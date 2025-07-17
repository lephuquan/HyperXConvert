package com.hyperxconvert.backend.service;

import org.springframework.stereotype.Service;
import java.io.File;
import java.nio.file.Files;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;

@Service
public class FileConversionService {
    public File convert(File inputFile, String fromFormat, String toFormat) throws Exception {
        // Tùy theo fromFormat/toFormat gọi các hàm chuyển đổi tương ứng
        if (fromFormat.equals("PDF") && toFormat.equals("DOCX")) {
            return convertPdfToDocx(inputFile);
        } else if (fromFormat.equals("DOCX") && toFormat.equals("PDF")) {
            return convertDocxToPdf(inputFile);
        } else if (fromFormat.equals("JPG") && toFormat.equals("PNG")) {
            return convertJpgToPng(inputFile);
        } else if (fromFormat.equals("PNG") && toFormat.equals("JPG")) {
            return convertPngToJpg(inputFile);
        } else if (fromFormat.equals("MP4") && toFormat.equals("MP3")) {
            return convertMp4ToMp3(inputFile);
        } else if (fromFormat.equals("PDF") && toFormat.equals("COMPRESSED_PDF")) {
            return compressPdf(inputFile);
        } else if (fromFormat.equals("MP4") && toFormat.equals("COMPRESSED_VIDEO")) {
            return compressVideo(inputFile);
        } else {
            throw new Exception("Chuyển đổi định dạng không được hỗ trợ");
        }
    }

    private File convertPdfToDocx(File inputFile) throws Exception {
        File outputFile = Files.createTempFile("converted-", ".docx").toFile();
        ProcessBuilder pb = new ProcessBuilder("pdf2docx", "convert", inputFile.getAbsolutePath(), outputFile.getAbsolutePath());
        pb.redirectErrorStream(true);
        Process process = pb.start();
        int exitCode = process.waitFor();
        if (exitCode != 0) throw new Exception("CONVERSION_ERROR: pdf2docx");
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
            throw new Exception("CONVERSION_ERROR: docx2pdf output missing or not PDF. Output: " + output);
        }
        return result;
    }
    private File convertJpgToPng(File inputFile) throws Exception {
        File outputFile = Files.createTempFile("converted-", ".png").toFile();
        ProcessBuilder pb = new ProcessBuilder("sharp", inputFile.getAbsolutePath(), "--output", outputFile.getAbsolutePath());
        pb.redirectErrorStream(true);
        Process process = pb.start();
        int exitCode = process.waitFor();
        if (exitCode != 0) throw new Exception("CONVERSION_ERROR: sharp jpg2png");
        return outputFile;
    }
    private File convertPngToJpg(File inputFile) throws Exception {
        File outputFile = Files.createTempFile("converted-", ".jpg").toFile();
        ProcessBuilder pb = new ProcessBuilder("sharp", inputFile.getAbsolutePath(), "--output", outputFile.getAbsolutePath());
        pb.redirectErrorStream(true);
        Process process = pb.start();
        int exitCode = process.waitFor();
        if (exitCode != 0) throw new Exception("CONVERSION_ERROR: sharp png2jpg");
        return outputFile;
    }
    private File convertMp4ToMp3(File inputFile) throws Exception {
        File outputFile = Files.createTempFile("converted-", ".mp3").toFile();
        ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-i", inputFile.getAbsolutePath(), "-vn", "-ar", "44100", "-ac", "2", "-b:a", "192k", outputFile.getAbsolutePath());
        pb.redirectErrorStream(true);
        Process process = pb.start();
        int exitCode = process.waitFor();
        if (exitCode != 0) throw new Exception("CONVERSION_ERROR: ffmpeg mp4->mp3");
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
        if (exitCode != 0) throw new Exception("CONVERSION_ERROR: ghostscript compress pdf");
        return outputFile;
    }
    private File compressVideo(File inputFile) throws Exception {
        File outputFile = Files.createTempFile("compressed-", ".mp4").toFile();
        ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-i", inputFile.getAbsolutePath(), "-vcodec", "libx264", "-crf", "28", outputFile.getAbsolutePath());
        pb.redirectErrorStream(true);
        Process process = pb.start();
        int exitCode = process.waitFor();
        if (exitCode != 0) throw new Exception("CONVERSION_ERROR: ffmpeg compress video");
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