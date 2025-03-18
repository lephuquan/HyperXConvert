package com.hyperxconvert.api.converter;

import com.aspose.words.Document;
import com.aspose.words.SaveFormat;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
public class WordToPdfConverter implements FileConverter {

    private static final Set<String> SUPPORTED_SOURCE_FORMATS = new HashSet<>(
            Arrays.asList("doc", "docx", "rtf", "odt"));

    @Override
    public File convert(File source) throws IOException {
        try {
            // Create a temporary file for the output
            String outputFileName = UUID.randomUUID().toString() + ".pdf";
            File outputFile = Files.createTempFile("converted-", outputFileName).toFile();
            
            // Load the document
            Document doc = new Document(source.getAbsolutePath());
            
            // Save as PDF
            doc.save(outputFile.getAbsolutePath(), SaveFormat.PDF);
            
            log.info("Converted Word document to PDF: {}", outputFile.getAbsolutePath());
            return outputFile;
        } catch (Exception e) {
            log.error("Error converting Word document to PDF", e);
            throw new IOException("Failed to convert Word document to PDF", e);
        }
    }

    @Override
    public boolean supports(String sourceFormat, String targetFormat) {
        return SUPPORTED_SOURCE_FORMATS.contains(sourceFormat.toLowerCase()) && 
               "pdf".equalsIgnoreCase(targetFormat);
    }
}
