package com.jxl.membership.controller;

import com.jxl.membership.common.Result;
import com.jxl.membership.dao.entity.SocietyMemberProfile;
import com.jxl.membership.dto.req.MemberProfileSubmitRequest;
import com.jxl.membership.dto.resp.ResumeUploadResp;
import com.jxl.membership.service.SocietyMemberProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/society-member")
public class SocietyMemberProfileController {

    private final SocietyMemberProfileService societyMemberProfileService;

    @PostMapping("/resume/upload")
    public Result<ResumeUploadResp> uploadResume(@RequestParam(required = false) String memberNo,
                                                 @RequestParam MultipartFile resumeFile,
                                                 @RequestParam Boolean needParse) {
        return Result.success(societyMemberProfileService.uploadResume(memberNo, resumeFile, needParse));
    }

    @PostMapping("/profile/submit")
    public Result<SocietyMemberProfile> submitMemberProfile(@RequestBody MemberProfileSubmitRequest request) {
        return Result.success("Member profile submitted successfully.", societyMemberProfileService.submitMemberProfile(request));
    }

    @GetMapping("/profile/{memberNo}")
    public Result<SocietyMemberProfile> getMemberProfile(@PathVariable String memberNo) {
        return Result.success(societyMemberProfileService.getMemberProfile(memberNo));
    }
}
