package com.jxl.membership.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jxl.membership.common.constant.MemberConstants;
import com.jxl.membership.common.exception.BusinessException;
import com.jxl.membership.dao.entity.SocietyMemberProfile;
import com.jxl.membership.dao.mapper.SocietyMemberProfileMapper;
import com.jxl.membership.dto.FileSaveResult;
import com.jxl.membership.dto.ResumeParsedData;
import com.jxl.membership.dto.req.MemberProfileSubmitRequest;
import com.jxl.membership.dto.resp.ResumeUploadResp;
import com.jxl.membership.service.SocietyMemberProfileService;
import com.jxl.membership.toolkit.FileStorageToolkit;
import com.jxl.membership.toolkit.MemberNoGenerator;
import com.jxl.membership.toolkit.PdfResumeExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class SocietyMemberProfileServiceImpl implements SocietyMemberProfileService {

    private final SocietyMemberProfileMapper societyMemberProfileMapper;
    private final MemberNoGenerator memberNoGenerator;
    private final FileStorageToolkit fileStorageToolkit;
    private final PdfResumeExtractor pdfResumeExtractor;

    @Override
    @Transactional
    public ResumeUploadResp uploadResume(String memberNo, MultipartFile resumeFile, Boolean needParse) {
        boolean parse = Boolean.TRUE.equals(needParse);
        log.info("Resume upload start: memberNo={}, filename={}, size={}, needParse={}", memberNo, safeFilename(resumeFile),
                resumeFile == null ? null : resumeFile.getSize(), parse);
        FileSaveResult fileSaveResult = fileStorageToolkit.saveResume(resumeFile);
        SocietyMemberProfile profile = findOrCreateProfile(memberNo);
        String oldResumeFileUrl = profile.getResumeFileUrl();
        log.info("Resume saved: memberNo={}, fileUrl={}, size={}", profile.getMemberNo(), fileSaveResult.getFileUrl(),
                fileSaveResult.getFileSize());

        ResumeParsedData parsedData = null;
        String parseStatus = MemberConstants.RESUME_PARSE_NOT_PARSED;
        String message = "Resume uploaded. Parse was not executed.";

        if (parse) {
            log.info("Resume parsing started: memberNo={}, fileUrl={}", profile.getMemberNo(), fileSaveResult.getFileUrl());
            try {
                PdfResumeExtractor.ResumeExtractResult extractResult = pdfResumeExtractor.extract(new File(fileSaveResult.getAbsolutePath()));
                parsedData = extractResult.getParsedData();
                parseStatus = normalizeParseStatus(extractResult.getParseStatus());
                message = extractResult.getMessage();
                if (parsedData == null) {
                    parsedData = new ResumeParsedData();
                }
                int extractedLength = extractResult.getExtractedText() == null ? 0 : extractResult.getExtractedText().length();
                log.info("Resume parsing finished: memberNo={}, status={}, extractedLength={}, parsedFields={}", profile.getMemberNo(), parseStatus,
                        extractedLength, countParsedFields(parsedData));
            } catch (Exception exception) {
                parseStatus = MemberConstants.RESUME_PARSE_FAILED;
                message = "Resume uploaded, but parsing failed. Please fill in the form manually.";
                log.warn("Resume parsing failed: memberNo={}, fileUrl={}", profile.getMemberNo(), fileSaveResult.getFileUrl(), exception);
            }
        } else {
            log.info("Resume parsing skipped: memberNo={}, fileUrl={}", profile.getMemberNo(), fileSaveResult.getFileUrl());
        }

        profile.setResumeFileUrl(fileSaveResult.getFileUrl());
        profile.setResumeOriginalName(fileSaveResult.getOriginalName());
        profile.setResumeFileType(fileSaveResult.getFileType());
        profile.setResumeFileSize(fileSaveResult.getFileSize());
        profile.setResumeUploadedAt(LocalDateTime.now());
        profile.setResumeParseStatus(parseStatus);
        profile.setResumeParsedAt(parse ? LocalDateTime.now() : null);

        try {
            if (profile.getId() == null) {
                societyMemberProfileMapper.insert(profile);
            } else {
                societyMemberProfileMapper.updateById(profile);
            }
            log.info("Resume metadata persisted: memberNo={}, parseStatus={}", profile.getMemberNo(), parseStatus);
        } catch (RuntimeException exception) {
            log.error("Failed to persist resume metadata: memberNo={}, fileUrl={}", profile.getMemberNo(), fileSaveResult.getFileUrl(), exception);
            fileStorageToolkit.deleteQuietly(fileSaveResult.getFileUrl());
            throw exception;
        }
        deleteOldResumeAfterCommit(oldResumeFileUrl);

        ResumeUploadResp response = new ResumeUploadResp();
        response.setMemberNo(profile.getMemberNo());
        response.setResumeFileUrl(profile.getResumeFileUrl());
        response.setResumeOriginalName(profile.getResumeOriginalName());
        response.setResumeFileType(profile.getResumeFileType());
        response.setResumeFileSize(profile.getResumeFileSize());
        response.setNeedParse(parse);
        response.setParseStatus(parseStatus);
        response.setMessage(message);
        response.setParsedData(parsedData);
        return response;
    }

    @Override
    @Transactional
    public SocietyMemberProfile submitMemberProfile(MemberProfileSubmitRequest request) {
        validateSubmitRequest(request);
        SocietyMemberProfile profile = getMemberProfile(request.getMemberNo());
        if (isBlank(profile.getResumeFileUrl())) {
            throw new BusinessException("Please upload a resume before submitting member profile.");
        }
        ensureEmailAvailable(request.getEmail(), profile.getId());

        profile.setMemberName(request.getMemberName().trim());
        profile.setEmail(request.getEmail().trim());
        profile.setPhone(trimToNull(request.getPhone()));
        profile.setGender(trimToNull(request.getGender()));
        profile.setBirthYear(request.getBirthYear());
        profile.setCountryRegion(trimToNull(request.getCountryRegion()));
        profile.setOrganization(request.getOrganization().trim());
        profile.setDepartment(trimToNull(request.getDepartment()));
        profile.setPositionTitle(trimToNull(request.getPositionTitle()));
        profile.setHighestDegree(trimToNull(request.getHighestDegree()));
        profile.setProfessionalField(trimToNull(request.getProfessionalField()));
        profile.setResearchDirection(trimToNull(request.getResearchDirection()));
        profile.setPersonalBio(trimToNull(request.getPersonalBio()));
        profile.setRepresentativeAchievements(trimToNull(request.getRepresentativeAchievements()));
        profile.setHomepage(trimToNull(request.getHomepage()));
        profile.setOrcid(trimToNull(request.getOrcid()));
        profile.setScholarProfile(trimToNull(request.getScholarProfile()));
        profile.setMemberType(isBlank(request.getMemberType()) ? MemberConstants.MEMBER_TYPE_REGULAR : request.getMemberType().trim());
        profile.setJoinDate(request.getJoinDate());
        profile.setRemark(trimToNull(request.getRemark()));
        profile.setMemberStatus(MemberConstants.MEMBER_STATUS_PENDING);

        societyMemberProfileMapper.updateById(profile);
        return profile;
    }

    @Override
    public SocietyMemberProfile getMemberProfile(String memberNo) {
        if (isBlank(memberNo)) {
            throw new BusinessException("memberNo is required.");
        }
        SocietyMemberProfile profile = societyMemberProfileMapper.selectOne(new LambdaQueryWrapper<SocietyMemberProfile>()
                .eq(SocietyMemberProfile::getMemberNo, memberNo.trim())
                .last("LIMIT 1"));
        if (profile == null) {
            throw new BusinessException("Member profile does not exist.");
        }
        return profile;
    }

    private SocietyMemberProfile findOrCreateProfile(String memberNo) {
        if (!isBlank(memberNo)) {
            SocietyMemberProfile existing = societyMemberProfileMapper.selectOne(new LambdaQueryWrapper<SocietyMemberProfile>()
                    .eq(SocietyMemberProfile::getMemberNo, memberNo.trim())
                    .last("LIMIT 1"));
            if (existing != null) {
                return existing;
            }
        }
        SocietyMemberProfile profile = new SocietyMemberProfile();
        profile.setMemberNo(isBlank(memberNo) ? memberNoGenerator.generate() : memberNo.trim());
        profile.setMemberType(MemberConstants.MEMBER_TYPE_REGULAR);
        profile.setMemberStatus(MemberConstants.MEMBER_STATUS_DRAFT);
        profile.setResumeParseStatus(MemberConstants.RESUME_PARSE_NOT_PARSED);
        return profile;
    }

    private void validateSubmitRequest(MemberProfileSubmitRequest request) {
        if (request == null) {
            throw new BusinessException("Request body is required.");
        }
        if (isBlank(request.getMemberNo())) {
            throw new BusinessException("memberNo is required.");
        }
        if (isBlank(request.getMemberName())) {
            throw new BusinessException("Member name is required.");
        }
        if (isBlank(request.getEmail())) {
            throw new BusinessException("Email is required.");
        }
        if (!request.getEmail().matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new BusinessException("Email format is invalid.");
        }
        if (isBlank(request.getOrganization())) {
            throw new BusinessException("Organization is required.");
        }
    }

    private void ensureEmailAvailable(String email, Long currentId) {
        SocietyMemberProfile existing = societyMemberProfileMapper.selectOne(new LambdaQueryWrapper<SocietyMemberProfile>()
                .eq(SocietyMemberProfile::getEmail, email.trim())
                .last("LIMIT 1"));
        if (existing != null && !existing.getId().equals(currentId)) {
            throw new BusinessException("Email is already used by another member.");
        }
    }

    private void deleteOldResumeAfterCommit(String oldResumeFileUrl) {
        if (isBlank(oldResumeFileUrl)) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                fileStorageToolkit.deleteQuietly(oldResumeFileUrl);
            }
        });
    }

    private String trimToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String normalizeParseStatus(String status) {
        if (MemberConstants.RESUME_PARSE_SUCCESS.equals(status) || "SUCCESS".equalsIgnoreCase(status)) {
            return MemberConstants.RESUME_PARSE_SUCCESS;
        }
        if (MemberConstants.RESUME_PARSE_FAILED.equals(status) || "FAILED".equalsIgnoreCase(status)) {
            return MemberConstants.RESUME_PARSE_FAILED;
        }
        if (MemberConstants.RESUME_PARSE_PARSING.equals(status) || "PARSING".equalsIgnoreCase(status)) {
            return MemberConstants.RESUME_PARSE_PARSING;
        }
        if (MemberConstants.RESUME_PARSE_NOT_PARSED.equals(status) || "NOT_PARSED".equalsIgnoreCase(status)) {
            return MemberConstants.RESUME_PARSE_NOT_PARSED;
        }
        return MemberConstants.RESUME_PARSE_FAILED;
    }

    private String safeFilename(MultipartFile file) {
        if (file == null) {
            return null;
        }
        String name = file.getOriginalFilename();
        return name == null || name.trim().isEmpty() ? "unknown" : name.trim();
    }

    private int countParsedFields(ResumeParsedData data) {
        if (data == null) {
            return 0;
        }
        int count = 0;
        if (!isBlank(data.getMemberName())) {
            count++;
        }
        if (!isBlank(data.getEmail())) {
            count++;
        }
        if (!isBlank(data.getPhone())) {
            count++;
        }
        if (!isBlank(data.getGender())) {
            count++;
        }
        if (data.getBirthYear() != null) {
            count++;
        }
        if (!isBlank(data.getCountryRegion())) {
            count++;
        }
        if (!isBlank(data.getOrganization())) {
            count++;
        }
        if (!isBlank(data.getDepartment())) {
            count++;
        }
        if (!isBlank(data.getPositionTitle())) {
            count++;
        }
        if (!isBlank(data.getHighestDegree())) {
            count++;
        }
        if (!isBlank(data.getProfessionalField())) {
            count++;
        }
        if (!isBlank(data.getResearchDirection())) {
            count++;
        }
        if (!isBlank(data.getPersonalBio())) {
            count++;
        }
        if (!isBlank(data.getRepresentativeAchievements())) {
            count++;
        }
        if (!isBlank(data.getHomepage())) {
            count++;
        }
        if (!isBlank(data.getOrcid())) {
            count++;
        }
        if (!isBlank(data.getScholarProfile())) {
            count++;
        }
        return count;
    }
}
