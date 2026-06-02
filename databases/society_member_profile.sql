CREATE TABLE society_member_profile (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    member_no VARCHAR(100) NOT NULL COMMENT '浼氬憳缂栧彿',

    member_name VARCHAR(100) DEFAULT NULL COMMENT '濮撳悕',
    email VARCHAR(150) DEFAULT NULL COMMENT '閭',
    phone VARCHAR(50) DEFAULT NULL COMMENT '鎵嬫満鍙?,

    gender VARCHAR(20) DEFAULT NULL COMMENT '鎬у埆',
    birth_year INT DEFAULT NULL COMMENT '鍑虹敓骞翠唤',
    country_region VARCHAR(100) DEFAULT NULL COMMENT '鍥藉鎴栧湴鍖?,

    organization VARCHAR(255) DEFAULT NULL COMMENT '鎵€鍦ㄥ崟浣?,
    department VARCHAR(255) DEFAULT NULL COMMENT '闄㈢郴鎴栭儴闂?,
    position_title VARCHAR(100) DEFAULT NULL COMMENT '鑱屽姟鎴栬亴绉?,
    highest_degree VARCHAR(100) DEFAULT NULL COMMENT '鏈€楂樺鍘?,
    professional_field VARCHAR(255) DEFAULT NULL COMMENT '涓撲笟棰嗗煙',

    research_direction TEXT DEFAULT NULL COMMENT '鐮旂┒鏂瑰悜',
    personal_bio TEXT DEFAULT NULL COMMENT '涓汉绠€浠?,
    representative_achievements TEXT DEFAULT NULL COMMENT '浠ｈ〃鎬ф垚鏋?,
    homepage VARCHAR(500) DEFAULT NULL COMMENT '涓汉涓婚〉',
    orcid VARCHAR(100) DEFAULT NULL COMMENT 'ORCID',
    scholar_profile VARCHAR(500) DEFAULT NULL COMMENT '瀛︽湳涓婚〉',

    member_type VARCHAR(50) DEFAULT 'REGULAR' COMMENT '浼氬憳绫诲瀷',
    member_status VARCHAR(50) DEFAULT 'DRAFT' COMMENT '浼氬憳鐘舵€?,
    join_date DATE DEFAULT NULL COMMENT '鍏ヤ細鏃ユ湡',

    resume_file_url VARCHAR(500) DEFAULT NULL COMMENT '褰撳墠绠€鍘嗘枃浠跺湴鍧€',
    resume_original_name VARCHAR(255) DEFAULT NULL COMMENT '褰撳墠绠€鍘嗗師濮嬫枃浠跺悕',
    resume_file_type VARCHAR(50) DEFAULT NULL COMMENT '褰撳墠绠€鍘嗘枃浠剁被鍨?,
    resume_file_size BIGINT DEFAULT NULL COMMENT '褰撳墠绠€鍘嗘枃浠跺ぇ灏?,
    resume_uploaded_at DATETIME DEFAULT NULL COMMENT '褰撳墠绠€鍘嗕笂浼犳椂闂?,

    resume_parse_status VARCHAR(50) DEFAULT 'NOT_PARSED' COMMENT '褰撳墠绠€鍘嗚В鏋愮姸鎬?,
    resume_parsed_at DATETIME DEFAULT NULL COMMENT '褰撳墠绠€鍘嗚В鏋愭椂闂?,

    remark TEXT DEFAULT NULL COMMENT '澶囨敞',

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_member_no (member_no),
    UNIQUE KEY uk_email (email)
) COMMENT='瀛︿細浼氬憳淇℃伅琛?;
