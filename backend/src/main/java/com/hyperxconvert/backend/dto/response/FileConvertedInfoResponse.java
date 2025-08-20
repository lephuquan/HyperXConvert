package com.hyperxconvert.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileConvertedInfoResponse {
    private Long convertedFileSize;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm yyyy-MM-dd")
    private LocalDateTime expiresAt;
    // Dự phòng cho các thuộc tính khác sẽ thêm sau


}
