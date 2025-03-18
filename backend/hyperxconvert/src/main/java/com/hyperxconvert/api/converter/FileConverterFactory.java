package com.hyperxconvert.api.converter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Factory for file converters
 */
@Component
@RequiredArgsConstructor
public class FileConverterFactory {
    
    private final List<FileConverter> converters;
    
    /**
     * Get a converter for the given source and target formats
     * 
     * @param sourceFormat The source file format
     * @param targetFormat The target file format
     * @return The converter that supports the conversion
     * @throws IllegalArgumentException If no converter supports the conversion
     */
    public FileConverter getConverter(String sourceFormat, String targetFormat) {
        return converters.stream()
                .filter(converter -> converter.supports(sourceFormat, targetFormat))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No converter found for " + sourceFormat + " to " + targetFormat));
    }
    
    /**
     * Check if a conversion is supported
     * 
     * @param sourceFormat The source file format
     * @param targetFormat The target file format
     * @return true if the conversion is supported, false otherwise
     */
    public boolean isConversionSupported(String sourceFormat, String targetFormat) {
        return converters.stream()
                .anyMatch(converter -> converter.supports(sourceFormat, targetFormat));
    }
    
    /**
     * Convert a file
     * 
     * @param sourceFile The source file
     * @param sourceFormat The source file format
     * @param targetFormat The target file format
     * @return The converted file
     * @throws IOException If an error occurs during conversion
     * @throws IllegalArgumentException If no converter supports the conversion
     */
    public File convert(File sourceFile, String sourceFormat, String targetFormat) throws IOException {
        FileConverter converter = getConverter(sourceFormat, targetFormat);
        return converter.convert(sourceFile);
    }
}
