package com.jxl.membership.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileSaveResult {

    private String fileUrl;
    private String storagePath;
    private String originalName;
    private String fileType;
    private Long fileSize;
    private String absolutePath;
}
