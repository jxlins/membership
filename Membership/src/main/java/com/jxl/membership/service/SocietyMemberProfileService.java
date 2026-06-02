package com.jxl.membership.service;

import com.jxl.membership.dao.entity.SocietyMemberProfile;
import com.jxl.membership.dto.req.MemberProfileSubmitRequest;
import com.jxl.membership.dto.resp.ResumeUploadResp;
import org.springframework.web.multipart.MultipartFile;

public interface SocietyMemberProfileService {

    ResumeUploadResp uploadResume(String memberNo, MultipartFile resumeFile, Boolean needParse);

    SocietyMemberProfile submitMemberProfile(MemberProfileSubmitRequest request);

    SocietyMemberProfile getMemberProfile(String memberNo);
}
