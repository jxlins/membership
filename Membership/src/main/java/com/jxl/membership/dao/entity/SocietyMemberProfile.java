package com.jxl.membership.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("society_member_profile")
public class SocietyMemberProfile {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String memberNo;

    private String memberName;
    private String email;
    private String phone;

    private String gender;
    private Integer birthYear;
    private String countryRegion;

    private String organization;
    private String department;
    private String positionTitle;
    private String highestDegree;
    private String professionalField;

    private String researchDirection;
    private String personalBio;
    private String representativeAchievements;
    private String homepage;
    private String orcid;
    private String scholarProfile;

    private String memberType;
    private String memberStatus;
    private LocalDate joinDate;

    private String resumeFileUrl;
    private String resumeOriginalName;
    private String resumeFileType;
    private Long resumeFileSize;
    private LocalDateTime resumeUploadedAt;

    private String resumeParseStatus;
    private LocalDateTime resumeParsedAt;

    private String remark;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
