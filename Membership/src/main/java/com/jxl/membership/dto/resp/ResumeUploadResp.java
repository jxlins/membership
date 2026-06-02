package com.jxl.membership.dto.resp;

import com.jxl.membership.dto.ResumeParsedData;
import lombok.Data;

@Data
public class ResumeUploadResp {

    private String memberNo;

    private String resumeFileUrl;
    private String resumeOriginalName;
    private String resumeFileType;
    private Long resumeFileSize;

    private Boolean needParse;
    private String parseStatus;
    private String message;

    private ResumeParsedData parsedData;
}
