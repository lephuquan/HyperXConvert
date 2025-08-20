package com.hyperxconvert.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileStatusResponse {
    private String status;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String downloadUrl;
    
    private String message;
    private String originalFilename;
    private String formatFrom;
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String formatTo;
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String errorCode;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String convertedFilename;

}
