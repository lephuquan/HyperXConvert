package com.hyperxconvert.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.io.File;
import java.nio.file.Files;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import com.hyperxconvert.backend.enums.FileFormat;

@Slf4j
@Service
public class ConversionGateway {
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
            log.info("Processing conversion in .......... {}s", String.format("%.1f", durationMs / 1000.0));
        }
    }

    private File convertPdfToDocx(File inputFile) throws Exception {
        File outputFile = Files.createTempFile("converted-", ".docx").toFile();
        ProcessBuilder pb = new ProcessBuilder("pdf2docx", "convert", inputFile.getAbsolutePath(), outputFile.getAbsolutePath());
        pb.redirectErrorStream(true);
        Process process = pb.start();
        int exitCode = process.waitFor();
        if (exitCode != 0) throw new com.hyperxconvert.backend.exception.ApiException("CONVERSION_ERROR", "error.conversion.pdf2docx");
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
        String officeCmd = getLibreOfficeCmd();
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
        ProcessBuilder pb = new ProcessBuilder("sharp", inputFile.getAbsolutePath(), "--output", outputFile.getAbsolutePath());
        pb.redirectErrorStream(true);
        Process process = pb.start();
        int exitCode = process.waitFor();
        if (exitCode != 0) throw new com.hyperxconvert.backend.exception.ApiException("CONVERSION_ERROR", "error.conversion.jpg2png");
        return outputFile;
    }
    private File convertPngToJpg(File inputFile) throws Exception {
        File outputFile = Files.createTempFile("converted-", ".jpg").toFile();
        ProcessBuilder pb = new ProcessBuilder("sharp", inputFile.getAbsolutePath(), "--output", outputFile.getAbsolutePath());
        pb.redirectErrorStream(true);
        Process process = pb.start();
        int exitCode = process.waitFor();
        if (exitCode != 0) throw new com.hyperxconvert.backend.exception.ApiException("CONVERSION_ERROR", "error.conversion.png2jpg");
        return outputFile;
    }
    private File convertMp4ToMp3(File inputFile) throws Exception {
        File outputFile = Files.createTempFile("converted-", ".mp3").toFile();
        ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-i", inputFile.getAbsolutePath(), "-vn", "-ar", "44100", "-ac", "2", "-b:a", "192k", outputFile.getAbsolutePath());
        pb.redirectErrorStream(true);
        Process process = pb.start();
        int exitCode = process.waitFor();
        if (exitCode != 0) throw new com.hyperxconvert.backend.exception.ApiException("CONVERSION_ERROR", "error.conversion.mp4tomp3");
        return outputFile;
    }
    private File compressPdf(File inputFile) throws Exception {
        File outputFile = Files.createTempFile("compressed-", ".pdf").toFile();
        String gsCmd = getCommandForOS(
            System.getProperty("os.name").toLowerCase().contains("win") ? "gswin64c" : "gs",
            "gswin64c", "gswin32c", "gs"
        );
        ProcessBuilder pb = new ProcessBuilder(gsCmd, "-sDEVICE=pdfwrite", "-dCompatibilityLevel=1.4", "-dPDFSETTINGS=/ebook", "-dNOPAUSE", "-dQUIET", "-dBATCH", "-sOutputFile=" + outputFile.getAbsolutePath(), inputFile.getAbsolutePath());
        pb.redirectErrorStream(true);
        Process process = pb.start();
        int exitCode = process.waitFor();
        if (exitCode != 0) throw new com.hyperxconvert.backend.exception.ApiException("CONVERSION_ERROR", "error.conversion.compresspdf");
        return outputFile;
    }
    private File compressVideo(File inputFile) throws Exception {
        File outputFile = Files.createTempFile("compressed-", ".mp4").toFile();
        ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-i", inputFile.getAbsolutePath(), "-vcodec", "libx264", "-crf", "28", outputFile.getAbsolutePath());
        pb.redirectErrorStream(true);
        Process process = pb.start();
        int exitCode = process.waitFor();
        if (exitCode != 0) throw new com.hyperxconvert.backend.exception.ApiException("CONVERSION_ERROR", "error.conversion.compressvideo");
        return outputFile;
    }

    // Tiện ích: chọn tên lệnh phù hợp theo hệ điều hành, ưu tiên soffice.com trên Windows
    private String getLibreOfficeCmd() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            String[] candidates = {
                "soffice.com",
                "C:\\Program Files\\LibreOffice\\program\\soffice.com",
                "C:\\Program Files (x86)\\LibreOffice\\program\\soffice.com",
                "soffice",
                "C:\\Program Files\\LibreOffice\\program\\soffice.exe",
                "C:\\Program Files (x86)\\LibreOffice\\program\\soffice.exe"
            };
            for (String cmd : candidates) {
                try {
                    ProcessBuilder pb = new ProcessBuilder(cmd, "--version");
                    pb.redirectErrorStream(true);
                    Process process = pb.start();
                    int exitCode = process.waitFor();
                    if (exitCode == 0) return cmd;
                } catch (Exception ignored) {}
            }
            return "soffice.com";
        }
        return "libreoffice";
    }

    // Tiện ích: chọn tên lệnh phù hợp theo hệ điều hành
    private String getCommandForOS(String... candidates) {
        for (String cmd : candidates) {
            try {
                ProcessBuilder pb = new ProcessBuilder(cmd, "--version");
                pb.redirectErrorStream(true);
                Process process = pb.start();
                int exitCode = process.waitFor();
                if (exitCode == 0) return cmd;
            } catch (Exception ignored) {}
        }
        // Nếu không tìm thấy, trả về candidate đầu tiên (có thể sẽ lỗi, nhưng báo lỗi rõ ràng)
        return candidates[0];
    }
} 