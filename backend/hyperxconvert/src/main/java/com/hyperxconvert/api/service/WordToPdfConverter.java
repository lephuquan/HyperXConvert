package com.hyperxconvert.api.service;

import com.aspose.words.Document;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class WordToPdfConverter {

    public File convertDocxToPdf(String inputFilePath) {
        try {
            // Tải tài liệu Word
            Document doc = new Document(inputFilePath);

            // Đường dẫn tệp PDF đầu ra
            String outputFilePath = inputFilePath.replace(".docx", ".pdf");
            File outputFile = new File(outputFilePath);

            // Lưu tài liệu dưới dạng PDF
            doc.save(outputFilePath);

            return outputFile;
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi chuyển đổi DOCX sang PDF: " + e.getMessage(), e);
        }
    }
}