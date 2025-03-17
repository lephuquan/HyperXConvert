package com.hyperxconvert.api.service;

import org.im4java.core.ConvertCmd;
import org.im4java.core.IMOperation;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class FileConversionService {

    public File convertFile(String inputFilePath, String targetFormat) {
        File input = new File(inputFilePath);

        // Kiểm tra xem tệp đầu vào có tồn tại không
        if (!input.exists()) {
            throw new IllegalArgumentException("Tệp đầu vào không tồn tại: " + inputFilePath);
        }

        // Đường dẫn tệp đầu ra
        File output = new File(input.getParent(), "converted_" + input.getName() + "." + targetFormat);

        try {
            // Cấu hình lệnh ImageMagick
            ConvertCmd cmd = new ConvertCmd();
            cmd.setCommand("magick"); // Sử dụng lệnh "magick" thay vì "convert"

            // Cấu hình các tham số chuyển đổi
            IMOperation op = new IMOperation();
            op.addImage(input.getAbsolutePath()); // Tệp đầu vào
            op.addImage(output.getAbsolutePath()); // Tệp đầu ra

            // Thực thi lệnh
            cmd.run(op);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Lỗi khi chuyển đổi tệp: " + e.getMessage());
        }

        return output;
    }
}