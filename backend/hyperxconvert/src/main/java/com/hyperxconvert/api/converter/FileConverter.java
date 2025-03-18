package com.hyperxconvert.api.converter;

import java.io.File;
import java.io.IOException;

/**
 * Interface for file converters
 */
public interface FileConverter {
    /**
     * Convert a source file to the target format
     * 
     * @param source The source file to convert
     * @return The converted file
     * @throws IOException If an error occurs during conversion
     */
    File convert(File source) throws IOException;
    
    /**
     * Check if this converter supports the given source and target formats
     * 
     * @param sourceFormat The source file format
     * @param targetFormat The target file format
     * @return true if this converter supports the conversion, false otherwise
     */
    boolean supports(String sourceFormat, String targetFormat);
}
