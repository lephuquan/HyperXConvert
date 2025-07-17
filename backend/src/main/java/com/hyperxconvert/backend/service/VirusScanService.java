package com.hyperxconvert.backend.service;

import org.springframework.stereotype.Service;
import java.io.File;

@Service
public class VirusScanService {
    public void scanFile(File file) throws Exception {
        // Gọi lệnh clamscan để quét virus
        ProcessBuilder pb = new ProcessBuilder("clamscan", file.getAbsolutePath());
        pb.redirectErrorStream(true);
        Process process = pb.start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new Exception("File bị nghi ngờ nhiễm mã độc hoặc lỗi quét virus");
        }
    }
} 