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
                throw new com.hyperxconvert.backend.exception.ApiException("UNSUPPORTED_FORMAT", "error.unsupported.format");
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
                throw new com.hyperxconvert.backend.exception.ApiException("UNSUPPORTED_FORMAT", "error.unsupported.conversion");
            }
        } finally {
            long durationMs = (System.nanoTime() - start) / 1_000_000;
            log.info("[MONITOR] Processing conversion in .......... {}s", String.format("%.1f", durationMs / 1000.0));
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
            log.error("pdf2docx failed. Output:\n{}", output.toString());
            throw new com.hyperxconvert.backend.exception.ApiException("CONVERSION_ERROR", "error.conversion.pdf2docx");
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
            throw new com.hyperxconvert.backend.exception.ApiException("CONVERSION_ERROR", "error.conversion.docx2pdf");
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
        log.info("[convertJpgToPng] Running command: {}", String.join(" ", cmd));
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
        log.info("[convertJpgToPng] Process exited with code: {}", exitCode);
        if (exitCode != 0) {
            log.error("[convertJpgToPng] JPG to PNG conversion failed. Output:\n{}", output.toString());
            throw new com.hyperxconvert.backend.exception.ApiException("CONVERSION_ERROR", "error.conversion.jpg2png");
        }
        log.info("[convertJpgToPng] Conversion output: {}", output.toString());
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
        log.info("[convertPngToJpg] Running command: {}", String.join(" ", cmd));
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
        log.info("[convertPngToJpg] Process exited with code: {}", exitCode);
        if (exitCode != 0) {
            log.error("[convertPngToJpg] PNG to JPG conversion failed. Output:\n{}", output.toString());
            throw new com.hyperxconvert.backend.exception.ApiException("CONVERSION_ERROR", "error.conversion.png2jpg");
        }
        log.info("[convertPngToJpg] Conversion output: {}", output.toString());
        return outputFile;
    }
    private File convertMp4ToMp3(File inputFile) throws Exception {
        File outputFile = Files.createTempFile("converted-", ".mp3").toFile();
        String[] cmd = (commandConfig.getFfmpegCmd() + " -y -i " + inputFile.getAbsolutePath() + " -vn -ar 44100 -ac 2 -b:a 192k " + outputFile.getAbsolutePath()).split(" ");
        log.info("[convertMp4ToMp3] Running command: {}", String.join(" ", cmd));
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
        log.info("[convertMp4ToMp3] Process exited with code: {}", exitCode);
        if (exitCode != 0) {
            log.error("[convertMp4ToMp3] MP4 to MP3 conversion failed. Output:\n{}", output.toString());
            throw new ApiException("CONVERSION_ERROR", "error.conversion.mp4tomp3");
        }
        log.info("[convertMp4ToMp3] Conversion output: {}", output.toString());
        return outputFile;
    }
    private File compressPdf(File inputFile) throws Exception {
        // Kiểm tra file đầu vào có phải PDF hợp lệ không
        if (!isPdfFile(inputFile)) {
            throw new com.hyperxconvert.backend.exception.ApiException("INVALID_INPUT", "error.input.notpdf");
        }
        
        // Sử dụng qpdf để nén PDF
        log.info("[compressPdf] Using qpdf for PDF compression");
        try {
            return compressPdfWithQpdf(inputFile);
        } catch (Exception e) {
            log.error("[compressPdf] qpdf failed: {}", e.getMessage());
            throw new com.hyperxconvert.backend.exception.ApiException("COMPRESSION_FAILED", "error.compression.qpdf_failed");
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

        log.info("[compressPdfWithQpdf] Running command: {}", String.join(" ", cmd));
        log.info("[compressPdfWithQpdf] Input file size: {} bytes", inputFile.length());
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
        
        log.info("[compressPdfWithQpdf] qpdf exit code: {}", exitCode);
        log.info("[compressPdfWithQpdf] qpdf output:\n{}", output.toString());
        
        // qpdf có thể thành công với warnings (exit code 3) hoặc thành công hoàn toàn (exit code 0)
        if (exitCode != 0 && exitCode != 3) {
            log.error("[compressPdfWithQpdf] qpdf failed with exit code {}. Output:\n{}", exitCode, output.toString());
            throw new com.hyperxconvert.backend.exception.ApiException("CONVERSION_ERROR", "error.conversion.qpdf");
        }
        
        // Log warnings nếu có
        if (output.toString().contains("WARNING") || output.toString().contains("warning")) {
            log.warn("[compressPdfWithQpdf] qpdf completed with warnings:\n{}", output.toString());
        }
        
        // Kiểm tra file đầu ra có tồn tại không
        if (!outputFile.exists()) {
            log.error("[compressPdfWithQpdf] Output file does not exist: {}", outputFile.getAbsolutePath());
            throw new com.hyperxconvert.backend.exception.ApiException("OUTPUT_INVALID", "error.output.file_not_created");
        }
        
        log.info("[compressPdfWithQpdf] Output file exists, size: {} bytes", outputFile.length());
        
        // Kiểm tra file đầu ra có phải PDF hợp lệ không
        if (!isPdfFile(outputFile)) {
            log.error("[compressPdfWithQpdf] Output file is not a valid PDF. Output:\n{}", output.toString());
            throw new com.hyperxconvert.backend.exception.ApiException("OUTPUT_INVALID", "error.output.notpdf");
        }
        
        log.info("[compressPdfWithQpdf] Compression completed successfully. Output: {}", output.toString());
        log.info("[compressPdfWithQpdf] Output file size: {} bytes", outputFile.length());
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
        log.info("[compressVideo] Running command: {}", String.join(" ", cmd));
        log.info("[compressVideo] Input file size: {} bytes", inputFile.length());
        
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        
        // Thêm timeout 10 phút cho process
        boolean finished = process.waitFor(10, java.util.concurrent.TimeUnit.MINUTES);
        if (!finished) {
            log.error("[compressVideo] Process timeout after 10 minutes. Destroying process...");
            process.destroyForcibly();
            throw new com.hyperxconvert.backend.exception.ApiException("CONVERSION_TIMEOUT", "error.conversion.timeout");
        }
        
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append(System.lineSeparator());
            }
        }
        
        int exitCode = process.exitValue();
        log.info("[compressVideo] Process exited with code: {}", exitCode);
        if (exitCode != 0) {
            log.error("[compressVideo] Video compression failed. Output:\n{}", output.toString());
            throw new com.hyperxconvert.backend.exception.ApiException("CONVERSION_ERROR", "error.conversion.compressvideo");
        }
        
        log.info("[compressVideo] Compression completed successfully. Output: {}", output.toString());
        log.info("[compressVideo] Output file size: {} bytes", outputFile.length());
        return outputFile;
    }
} 