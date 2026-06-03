CREATE TABLE society_member_profile (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    member_no VARCHAR(100) NOT NULL COMMENT '会员编号',

    member_name VARCHAR(100) DEFAULT NULL COMMENT '姓名',
    email VARCHAR(150) DEFAULT NULL COMMENT '邮箱',
    phone VARCHAR(50) DEFAULT NULL COMMENT '手机号',

    gender VARCHAR(20) DEFAULT NULL COMMENT '性别',
    birth_year INT DEFAULT NULL COMMENT '出生年份',
    country_region VARCHAR(100) DEFAULT NULL COMMENT '国家或地区',

    organization VARCHAR(255) DEFAULT NULL COMMENT '所在单位',
    department VARCHAR(255) DEFAULT NULL COMMENT '院系或部门',
    position_title VARCHAR(100) DEFAULT NULL COMMENT '职务或职称',
    highest_degree VARCHAR(100) DEFAULT NULL COMMENT '最高学历',
    professional_field VARCHAR(255) DEFAULT NULL COMMENT '专业领域',

    research_direction TEXT DEFAULT NULL COMMENT '研究方向',
    personal_bio TEXT DEFAULT NULL COMMENT '个人简介',
    representative_achievements TEXT DEFAULT NULL COMMENT '代表性成果',
    homepage VARCHAR(500) DEFAULT NULL COMMENT '个人主页',
    orcid VARCHAR(100) DEFAULT NULL COMMENT 'ORCID',
    scholar_profile VARCHAR(500) DEFAULT NULL COMMENT '学术主页',

    member_type VARCHAR(50) DEFAULT 'REGULAR' COMMENT '会员类型',
    member_status VARCHAR(50) DEFAULT 'DRAFT' COMMENT '会员状态',
    join_date DATE DEFAULT NULL COMMENT '入会日期',

    resume_file_url VARCHAR(500) DEFAULT NULL COMMENT '当前简历文件地址',
    resume_original_name VARCHAR(255) DEFAULT NULL COMMENT '当前简历原始文件名',
    resume_file_type VARCHAR(50) DEFAULT NULL COMMENT '当前简历文件类型',
    resume_file_size BIGINT DEFAULT NULL COMMENT '当前简历文件大小',
    resume_uploaded_at DATETIME DEFAULT NULL COMMENT '当前简历上传时间',

    resume_parse_status VARCHAR(50) DEFAULT 'NOT_PARSED' COMMENT '当前简历解析状态',
    resume_parsed_at DATETIME DEFAULT NULL COMMENT '当前简历解析时间',

    remark TEXT DEFAULT NULL COMMENT '备注',

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_member_no (member_no),
    UNIQUE KEY uk_email (email)
) COMMENT='学会会员信息表';