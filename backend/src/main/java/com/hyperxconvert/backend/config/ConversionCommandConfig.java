package com.hyperxconvert.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "conversion")
public class ConversionCommandConfig {
    private String pdf2docxCmd;
    private String sharpCmd;
    private String ffmpegCmd;
    private String qpdfCmd;

    public String getPdf2docxCmd() { return pdf2docxCmd; }
    public void setPdf2docxCmd(String pdf2docxCmd) { this.pdf2docxCmd = pdf2docxCmd; }

    public String getSharpCmd() { return sharpCmd; }
    public void setSharpCmd(String sharpCmd) { this.sharpCmd = sharpCmd; }

    public String getFfmpegCmd() { return ffmpegCmd; }
    public void setFfmpegCmd(String ffmpegCmd) { this.ffmpegCmd = ffmpegCmd; }

    public String getQpdfCmd() { return qpdfCmd; }
    public void setQpdfCmd(String qpdfCmd) { this.qpdfCmd = qpdfCmd; }
} 