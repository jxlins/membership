from sqlalchemy import Column, BigInteger, String, Integer, Text, Date, DateTime
from sqlalchemy.sql import func

from database import Base


class SocietyMemberProfile(Base):
    __tablename__ = "society_member_profile"

    id = Column(BigInteger, primary_key=True, autoincrement=True)

    member_no = Column(String(100), nullable=False, unique=True)

    member_name = Column(String(100))
    email = Column(String(150), unique=True)
    phone = Column(String(50))

    gender = Column(String(20))
    birth_year = Column(Integer)
    country_region = Column(String(100))

    organization = Column(String(255))
    department = Column(String(255))
    position_title = Column(String(100))
    highest_degree = Column(String(100))
    professional_field = Column(String(255))

    research_direction = Column(Text)
    personal_bio = Column(Text)
    representative_achievements = Column(Text)
    homepage = Column(String(500))
    orcid = Column(String(100))
    scholar_profile = Column(String(500))

    member_type = Column(String(50), default="REGULAR")
    member_status = Column(String(50), default="DRAFT")
    join_date = Column(Date)

    resume_file_url = Column(String(500))
    resume_original_name = Column(String(255))
    resume_file_type = Column(String(50))
    resume_file_size = Column(BigInteger)
    resume_uploaded_at = Column(DateTime)

    resume_parse_status = Column(String(50), default="NOT_PARSED")
    resume_parsed_at = Column(DateTime)

    remark = Column(Text)

    created_at = Column(DateTime, server_default=func.now())
    updated_at = Column(DateTime, server_default=func.now(), onupdate=func.now())