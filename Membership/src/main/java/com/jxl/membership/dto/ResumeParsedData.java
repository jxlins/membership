package com.jxl.membership.dto;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class ResumeParsedData {

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

    private Map<String, String> confidence = new LinkedHashMap<>();
}
