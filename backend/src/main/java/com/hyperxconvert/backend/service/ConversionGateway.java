package com.hyperxconvert.backend.service;

import com.hyperxconvert.backend.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.io.File;
import java.nio.file.Files;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import com.hyperxconvert.backend.enums.FileFormat;
import com.hyperxconvert.backend.config.ConversionCommandConfig;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ConversionGateway {
    private final ConversionCommandConfig commandConfig;

    @Autowired
    public ConversionGateway(ConversionCommandConfig commandConfig) {
        this.commandConfig = commandConfig;
    }

    public File convert(File inputFile, String fromFormat, String toFormat) throws Exception {
        long start = System.nanoTime();
        try {
            FileFormat from = FileFormat.fromString(fromFormat);
            FileFormat to = FileFormat.fromString(toFormat);
            if (from == null || to == null) {
                throw new ApiException("UNSUPPORTED_FORMAT", "error.unsupported.format");
            }
            // Tùy theo from/to gọi các hàm chuyển đổi tương ứng
            if (from == FileFormat.PDF && to == FileFormat.DOCX) {
                return convertPdfToDocx(inputFile);
            } else if (from == FileFormat.DOCX && to == FileFormat.PDF) {
                return convertDocxToPdf(inputFile);
            } else if (from == FileFormat.JPG && to == FileFormat.PNG) {
                return convertJpgToPng(inputFile);
            } else if (from == FileFormat.PNG && to == FileFormat.JPG) {
                return convertPngToJpg(inputFile);
            } else if (from == FileFormat.MP4 && to == FileFormat.MP3) {
                return convertMp4ToMp3(inputFile);
            } else if (from == FileFormat.PDF && to == FileFormat.COMPRESSED_PDF) {
                return compressPdf(inputFile);
            } else if (from == FileFormat.MP4 && to == FileFormat.COMPRESSED_VIDEO) {
                return compressVideo(inputFile);
            } else {
                throw new ApiException("UNSUPPORTED_FORMAT", "error.unsupported.conversion");
            }
        } finally {
            long durationMs = (System.nanoTime() - start) / 1_000_000;
            log.info("File conversion completed - from: {} to: {}, duration: {:.1f}s", 
                fromFormat, toFormat, durationMs / 1000.0);
        }
    }

    private File convertPdfToDocx(File inputFile) throws Exception {
        File outputFile = Files.createTempFile("converted-", ".docx").toFile();
        String[] cmd = (commandConfig.getPdf2docxCmd() + " convert " + inputFile.getAbsolutePath() + " " + outputFile.getAbsolutePath()).split(" ");
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        // Đọc toàn bộ output của lệnh chuyển đổi
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append(System.lineSeparator());
            }
        }
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            log.error("PDF to DOCX conversion failed - exitCode: {}, output: {}", exitCode, output.toString());
            throw new ApiException("CONVERSION_ERROR", "error.conversion.pdf2docx");
        }
        return outputFile;
    }
    private boolean isPdfFile(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] header = new byte[5];
            if (fis.read(header) != 5) return false;
            return new String(header).equals("%PDF-");
        }
    }
    private File convertDocxToPdf(File inputFile) throws Exception {
        File outputFile = Files.createTempFile("converted-", ".pdf").toFile();
        String officeCmd = "libreoffice";
        ProcessBuilder pb = new ProcessBuilder(officeCmd, "--headless", "--convert-to", "pdf", "--outdir", outputFile.getParent(), inputFile.getAbsolutePath());
        pb.redirectErrorStream(true);
        Process process = pb.start();

        // Đọc toàn bộ output của lệnh chuyển đổi
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append(System.lineSeparator());
            }
        }
        int exitCode = process.waitFor();
        String pdfName = inputFile.getName().replaceAll("\\.docx$", ".pdf");
        File result = new File(outputFile.getParent(), pdfName);

        if (exitCode != 0 || !result.exists() || !isPdfFile(result)) {
            log.error("DOCX to PDF conversion failed - exitCode: {}, resultExists: {}, isPdf: {}", 
                exitCode, result.exists(), result.exists() ? isPdfFile(result) : false);
            throw new ApiException("CONVERSION_ERROR", "error.conversion.docx2pdf");
        }
        return result;
    }
    private File convertJpgToPng(File inputFile) throws Exception {
        File outputFile = Files.createTempFile("converted-", ".png").toFile();
        File jpgInput = inputFile;
        if (!inputFile.getName().toLowerCase().endsWith(".jpg")) {
            jpgInput = Files.createTempFile("input-", ".jpg").toFile();
            java.nio.file.Files.copy(inputFile.toPath(), jpgInput.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
        String[] cmd = {commandConfig.getSharpCmd(), "-i", jpgInput.getAbsolutePath(), "-o", outputFile.getAbsolutePath()};
        log.debug("Running JPG to PNG conversion command: {}", String.join(" ", cmd));
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append(System.lineSeparator());
            }
        }
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            log.error("JPG to PNG conversion failed - exitCode: {}, output: {}", exitCode, output.toString());
            throw new ApiException("CONVERSION_ERROR", "error.conversion.jpg2png");
        }
        log.debug("JPG to PNG conversion completed successfully");
        return outputFile;
    }
    private File convertPngToJpg(File inputFile) throws Exception {
        File outputFile = Files.createTempFile("converted-", ".jpg").toFile();
        File pngInput = inputFile;
        if (!inputFile.getName().toLowerCase().endsWith(".png")) {
            pngInput = Files.createTempFile("input-", ".png").toFile();
            java.nio.file.Files.copy(inputFile.toPath(), pngInput.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
        String[] cmd = {commandConfig.getSharpCmd(), "-i", pngInput.getAbsolutePath(), "-o", outputFile.getAbsolutePath()};
        log.debug("Running PNG to JPG conversion command: {}", String.join(" ", cmd));
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append(System.lineSeparator());
            }
        }
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            log.error("PNG to JPG conversion failed - exitCode: {}, output: {}", exitCode, output.toString());
            throw new ApiException("CONVERSION_ERROR", "error.conversion.png2jpg");
        }
        log.debug("PNG to JPG conversion completed successfully");
        return outputFile;
    }
    private File convertMp4ToMp3(File inputFile) throws Exception {
        File outputFile = Files.createTempFile("converted-", ".mp3").toFile();
        String[] cmd = (commandConfig.getFfmpegCmd() + " -y -i " + inputFile.getAbsolutePath() + " -vn -ar 44100 -ac 2 -b:a 192k " + outputFile.getAbsolutePath()).split(" ");
        log.debug("Running MP4 to MP3 conversion command: {}", String.join(" ", cmd));
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append(System.lineSeparator());
            }
        }
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            log.error("MP4 to MP3 conversion failed - exitCode: {}, output: {}", exitCode, output.toString());
            throw new ApiException("CONVERSION_ERROR", "error.conversion.mp4tomp3");
        }
        log.debug("MP4 to MP3 conversion completed successfully");
        return outputFile;
    }
    private File compressPdf(File inputFile) throws Exception {
        // Kiểm tra file đầu vào có phải PDF hợp lệ không
        if (!isPdfFile(inputFile)) {
            throw new ApiException("INVALID_INPUT", "error.input.notpdf");
        }
        
        // Sử dụng qpdf để nén PDF
        log.debug("Using qpdf for PDF compression");
        try {
            return compressPdfWithQpdf(inputFile);
        } catch (Exception e) {
            log.error("PDF compression failed with qpdf", e);
            throw new ApiException("COMPRESSION_FAILED", "error.compression.qpdf_failed");
        }
    }
    
    private File compressPdfWithQpdf(File inputFile) throws Exception {
        File outputFile = Files.createTempFile("compressed-qpdf-", ".pdf").toFile();
        List<String> cmd = new ArrayList<>();
        cmd.add(commandConfig.getQpdfCmd());
        cmd.add("--linearize");
        cmd.add("--object-streams=generate");
        cmd.add("--compression-level=9");
        cmd.add(inputFile.getAbsolutePath());
        cmd.add(outputFile.getAbsolutePath());

        log.debug("Running PDF compression command: {}", String.join(" ", cmd));
        log.debug("Input PDF file size: {} bytes", inputFile.length());
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append(System.lineSeparator());
            }
        }
        int exitCode = process.waitFor();
        
        log.debug("qpdf exit code: {}", exitCode);
        log.debug("qpdf output: {}", output.toString());
        
        // qpdf có thể thành công với warnings (exit code 3) hoặc thành công hoàn toàn (exit code 0)
        if (exitCode != 0 && exitCode != 3) {
            log.error("PDF compression failed - exitCode: {}, output: {}", exitCode, output.toString());
            throw new ApiException("CONVERSION_ERROR", "error.conversion.qpdf");
        }
        
        // Log warnings nếu có
        if (output.toString().contains("WARNING") || output.toString().contains("warning")) {
            log.warn("PDF compression completed with warnings: {}", output.toString());
        }
        
        // Kiểm tra file đầu ra có tồn tại không
        if (!outputFile.exists()) {
            log.error("PDF compression output file does not exist: {}", outputFile.getAbsolutePath());
            throw new ApiException("OUTPUT_INVALID", "error.output.file_not_created");
        }
        
        log.debug("PDF compression output file size: {} bytes", outputFile.length());
        
        // Kiểm tra file đầu ra có phải PDF hợp lệ không
        if (!isPdfFile(outputFile)) {
            log.error("PDF compression output file is not a valid PDF: {}", output.toString());
            throw new ApiException("OUTPUT_INVALID", "error.output.notpdf");
        }
        
        log.debug("PDF compression completed successfully - output size: {} bytes", outputFile.length());
        return outputFile;
    }
    private File compressVideo(File inputFile) throws Exception {
        File outputFile = Files.createTempFile("compressed-", ".mp4").toFile();
        // Cải thiện command ffmpeg với các tham số tối ưu hóa tốc độ
        String[] cmd = (commandConfig.getFfmpegCmd() + 
            " -i " + inputFile.getAbsolutePath() + 
            " -vcodec libx264 -preset fast -crf 28" +
            " -acodec aac -b:a 128k" +
            " -movflags +faststart" +
            " -y " + outputFile.getAbsolutePath()).split(" ");
        log.debug("Running video compression command: {}", String.join(" ", cmd));
        log.debug("Input video file size: {} bytes", inputFile.length());
        
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        
        // Thêm timeout 10 phút cho process
        boolean finished = process.waitFor(10, java.util.concurrent.TimeUnit.MINUTES);
        if (!finished) {
            log.error("Video compression timeout after 10 minutes - destroying process");
            process.destroyForcibly();
            throw new ApiException("CONVERSION_TIMEOUT", "error.conversion.timeout");
        }
        
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append(System.lineSeparator());
            }
        }
        
        int exitCode = process.exitValue();
        if (exitCode != 0) {
            log.error("Video compression failed - exitCode: {}, output: {}", exitCode, output.toString());
            throw new ApiException("CONVERSION_ERROR", "error.conversion.compressvideo");
        }
        
        log.debug("Video compression completed successfully - output size: {} bytes", outputFile.length());
        return outputFile;
    }
} 