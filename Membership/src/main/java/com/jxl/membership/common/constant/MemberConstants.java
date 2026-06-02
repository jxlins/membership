package com.jxl.membership.common.constant;

public class MemberConstants {

    private MemberConstants() {
    }

    public static final String MEMBER_STATUS_DRAFT = "DRAFT";
    public static final String MEMBER_STATUS_PENDING = "PENDING";
    public static final String MEMBER_STATUS_ACTIVE = "ACTIVE";
    public static final String MEMBER_STATUS_REJECTED = "REJECTED";
    public static final String MEMBER_STATUS_DISABLED = "DISABLED";

    public static final String MEMBER_TYPE_REGULAR = "REGULAR";
    public static final String MEMBER_TYPE_STUDENT = "STUDENT";
    public static final String MEMBER_TYPE_EXPERT = "EXPERT";
    public static final String MEMBER_TYPE_COUNCIL = "COUNCIL";

    public static final String RESUME_PARSE_NOT_PARSED = "NOT_PARSED";
    public static final String RESUME_PARSE_PARSING = "PARSING";
    public static final String RESUME_PARSE_SUCCESS = "SUCCESS";
    public static final String RESUME_PARSE_FAILED = "FAILED";

    public static final long MAX_RESUME_SIZE = 10 * 1024 * 1024;
    public static final int PDF_TEXT_MIN_LENGTH = 50;
}
