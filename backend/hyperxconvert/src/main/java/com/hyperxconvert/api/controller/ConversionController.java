package com.hyperxconvert.api.controller;

import com.hyperxconvert.api.entity.Conversion;
import com.hyperxconvert.api.entity.User;
import com.hyperxconvert.api.repository.ConversionRepository;
import com.hyperxconvert.api.service.ConversionService;
import com.hyperxconvert.api.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/conversions")
@RequiredArgsConstructor
public class ConversionController {

    private final ConversionService conversionService;
    private final ConversionRepository conversionRepository;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<?> createConversion(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("file") MultipartFile file,
            @RequestParam("targetFormat") String targetFormat) {
        
        User user = userService.findByEmail(userDetails.getUsername());
        
        try {
            Conversion conversion = conversionService.createConversion(user, file, targetFormat);
            return ResponseEntity.ok(Map.of(
                "id", conversion.getId(),
                "status", conversion.getStatus(),
                "message", "Conversion request submitted successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getConversionStatus(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        
        User user = userService.findByEmail(userDetails.getUsername());
        
        Conversion conversion = conversionRepository.findById(id)
                .orElse(null);
        
        if (conversion == null) {
            return ResponseEntity.notFound().build();
        }
        
        // Check if the conversion belongs to the user
        if (!conversion.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body(Map.of(
                "error", "You don't have permission to access this conversion"
            ));
        }
        
        return ResponseEntity.ok(conversion);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<?> downloadConvertedFile(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        
        User user = userService.findByEmail(userDetails.getUsername());
        
        try {
            Conversion conversion = conversionRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Conversion not found"));
            
            // Check if the conversion belongs to the user
            if (!conversion.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(403).body(Map.of(
                    "error", "You don't have permission to access this conversion"
                ));
            }
            
            // Check if the conversion is completed
            if (conversion.getStatus() != Conversion.ConversionStatus.COMPLETED) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Conversion is not completed yet",
                    "status", conversion.getStatus()
                ));
            }
            
            Resource fileResource = conversionService.getConvertedFile(conversion);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + 
                            conversion.getOriginalFileName() + "." + conversion.getTargetFormat() + "\"")
                    .body(fileResource);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage()
            ));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteConversion(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        
        User user = userService.findByEmail(userDetails.getUsername());
        
        try {
            Conversion conversion = conversionRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Conversion not found"));
            
            // Check if the conversion belongs to the user
            if (!conversion.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(403).body(Map.of(
                    "error", "You don't have permission to access this conversion"
                ));
            }
            
            conversionService.deleteConversion(conversion);
            
            return ResponseEntity.ok(Map.of(
                "message", "Conversion deleted successfully"
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage()
            ));
        }
    }
}
