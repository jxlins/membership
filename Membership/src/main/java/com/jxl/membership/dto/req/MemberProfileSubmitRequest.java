package com.jxl.membership.dto.req;

import lombok.Data;

import java.time.LocalDate;

@Data
public class MemberProfileSubmitRequest {

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
    private LocalDate joinDate;

    private String remark;
}