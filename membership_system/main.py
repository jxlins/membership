import os
import re
import shutil
from pathlib import Path
from datetime import datetime, date

from fastapi import FastAPI, UploadFile, File, Depends, HTTPException
from sqlalchemy.orm import Session
from sqlalchemy.exc import IntegrityError

from database import Base, engine, get_db
from models import SocietyMemberProfile
from schemas import MemberProfileForm
from parser.pdf_parser import parse_resume
from parser.pdf_parser import get_ocr_model



Base.metadata.create_all(bind=engine)

app = FastAPI(title="Society Member Profile System")

UPLOAD_DIR = Path("uploads/resumes")
UPLOAD_DIR.mkdir(parents=True, exist_ok=True)

@app.on_event("startup")
def warmup_ocr_model():
    get_ocr_model()

def generate_member_no(db: Session) -> str:
    today = datetime.now().strftime("%Y%m%d")
    prefix = f"MB{today}"

    count = db.query(SocietyMemberProfile).filter(
        SocietyMemberProfile.member_no.like(f"{prefix}%")
    ).count()

    return f"{prefix}{count + 1:04d}"


def save_resume_file(file: UploadFile) -> tuple[str, int]:
    suffix = Path(file.filename).suffix.lower()
    file_name = f"{datetime.now().strftime('%Y%m%d%H%M%S%f')}{suffix}"
    file_path = UPLOAD_DIR / file_name

    size = 0
    with open(file_path, "wb") as buffer:
        shutil.copyfileobj(file.file, buffer)

    size = os.path.getsize(file_path)

    return str(file_path), size


def parse_age_to_birth_year(age_value):
    if age_value is None:
        return None

    text = str(age_value)
    digits = "".join(ch for ch in text if ch.isdigit())

    if not digits:
        return None

    age = int(digits)
    if 10 <= age <= 100:
        return datetime.now().year - age

    return None


def extract_birth_year_from_text(text: str):
    if not text:
        return None

    match = re.search(r"(19\d{2}|20\d{2})[年\-.]", text)
    if match:
        return int(match.group(1))

    return None


def normalize_degree(degree: str):
    if not degree:
        return None

    if "博士" in degree:
        return "博士"
    if "硕士" in degree:
        return "硕士"
    if "本科" in degree or "学士" in degree:
        return "本科"
    if "专科" in degree:
        return "专科"

    return degree


def build_personal_bio(raw_result: dict) -> str:
    parts = []

    basic = raw_result.get("基本信息", {}) or {}
    education_list = raw_result.get("教育背景", []) or []
    skills = raw_result.get("专业技能", {}) or {}
    projects = raw_result.get("项目经历", []) or []

    if basic.get("求职意向"):
        parts.append(f"求职意向：{basic.get('求职意向')}")

    if education_list:
        edu_texts = []
        for item in education_list:
            school = item.get("学校") or ""
            major = item.get("专业") or ""
            degree = item.get("学历") or ""
            edu_texts.append(f"{school} {major} {degree}".strip())
        parts.append("教育背景：" + "；".join(edu_texts))

    if skills:
        skill_texts = []
        for key, value in skills.items():
            skill_texts.append(f"{key}：{value}")
        parts.append("专业技能：" + "；".join(skill_texts))

    if projects:
        project_names = []
        for item in projects:
            if item.get("项目名称"):
                project_names.append(item.get("项目名称"))
        if project_names:
            parts.append("项目经历：" + "；".join(project_names))

    return "\n".join(parts)


def build_representative_achievements(raw_result: dict) -> str:
    projects = raw_result.get("项目经历", []) or []
    result = []

    for item in projects:
        name = item.get("项目名称")
        desc = item.get("项目描述")
        tech_stack = item.get("主要技术栈")
        content = item.get("项目内容")

        section = []

        if name:
            section.append(f"项目名称：{name}")
        if desc:
            section.append(f"项目描述：{desc}")
        if tech_stack:
            section.append(f"技术栈：{tech_stack}")

        if isinstance(content, list) and content:
            section.append("项目内容：")
            for index, line in enumerate(content, start=1):
                section.append(f"{index}. {line}")

        if section:
            result.append("\n".join(section))

    return "\n\n".join(result)


def map_resume_to_form(raw_result: dict) -> dict:
    basic = raw_result.get("基本信息", {}) or {}
    education_list = raw_result.get("教育背景", []) or {}
    skills = raw_result.get("专业技能", {}) or {}

    first_edu = education_list[0] if education_list else {}

    member_name = basic.get("姓名")
    email = basic.get("电子邮箱")
    phone = basic.get("联系电话")

    birth_year = None
    birth_year = extract_birth_year_from_text(str(basic.get("出生日期") or ""))
    if birth_year is None:
        birth_year = parse_age_to_birth_year(basic.get("年龄"))

    organization = first_edu.get("学校")
    highest_degree = normalize_degree(first_edu.get("学历"))
    professional_field = first_edu.get("专业")

    skill_text = "；".join(
        f"{key}：{value}"
        for key, value in skills.items()
        if value
    )

    return {
        "memberName": member_name,
        "email": email,
        "phone": phone,

        "gender": basic.get("性别"),
        "birthYear": birth_year,
        "countryRegion": basic.get("国家或地区"),

        "organization": organization,
        "department": None,
        "positionTitle": basic.get("职务") or basic.get("职称"),
        "highestDegree": highest_degree,
        "professionalField": professional_field,

        "researchDirection": None,
        "personalBio": build_personal_bio(raw_result),
        "representativeAchievements": build_representative_achievements(raw_result),
        "homepage": basic.get("个人主页"),
        "orcid": basic.get("ORCID"),
        "scholarProfile": basic.get("学术主页"),

        "memberType": "REGULAR",
        "remark": skill_text
    }


@app.post("/api/member-profile/resume/upload")
async def upload_resume(
    file: UploadFile = File(...),
    db: Session = Depends(get_db)
):
    if not file.filename:
        raise HTTPException(status_code=400, detail="文件名不能为空")

    if not file.filename.lower().endswith(".pdf"):
        raise HTTPException(status_code=400, detail="只支持 PDF 文件")

    storage_path, file_size = save_resume_file(file)

    profile = SocietyMemberProfile(
        member_no=generate_member_no(db),
        member_status="DRAFT",
        resume_file_url=storage_path,
        resume_original_name=file.filename,
        resume_file_type="PDF",
        resume_file_size=file_size,
        resume_uploaded_at=datetime.now(),
        resume_parse_status="NOT_PARSED"
    )

    db.add(profile)
    db.commit()
    db.refresh(profile)

    return {
        "code": 0,
        "message": "上传成功",
        "data": {
            "profileId": profile.id,
            "memberNo": profile.member_no,
            "resumeFileUrl": profile.resume_file_url,
            "resumeOriginalName": profile.resume_original_name,
            "resumeParseStatus": profile.resume_parse_status
        }
    }


@app.post("/api/member-profile/{profile_id}/resume/skip-parse")
def skip_parse_resume(
    profile_id: int,
    db: Session = Depends(get_db)
):
    profile = db.query(SocietyMemberProfile).filter(
        SocietyMemberProfile.id == profile_id
    ).first()

    if not profile:
        raise HTTPException(status_code=404, detail="会员草稿不存在")

    profile.resume_parse_status = "SKIPPED"
    db.commit()

    return {
        "code": 0,
        "message": "已跳过解析",
        "data": {
            "profileId": profile.id,
            "resumeParseStatus": profile.resume_parse_status
        }
    }


@app.post("/api/member-profile/{profile_id}/resume/parse")
def parse_resume_file(
    profile_id: int,
    db: Session = Depends(get_db)
):
    profile = db.query(SocietyMemberProfile).filter(
        SocietyMemberProfile.id == profile_id
    ).first()

    if not profile:
        raise HTTPException(status_code=404, detail="会员草稿不存在")

    if not profile.resume_file_url:
        raise HTTPException(status_code=400, detail="当前会员草稿没有上传简历")

    if not os.path.exists(profile.resume_file_url):
        raise HTTPException(status_code=404, detail="简历文件不存在")

    try:
        profile.resume_parse_status = "PARSING"
        db.commit()

        raw_result = parse_resume(profile.resume_file_url)
        form = map_resume_to_form(raw_result)

        profile.resume_parse_status = "PARSED"
        profile.resume_parsed_at = datetime.now()
        db.commit()

        return {
            "code": 0,
            "message": "解析成功",
            "data": {
                "profileId": profile.id,
                "memberNo": profile.member_no,
                "resumeParseStatus": profile.resume_parse_status,
                "form": form
            }
        }

    except Exception as e:
        profile.resume_parse_status = "PARSE_FAILED"
        profile.remark = f"简历解析失败：{str(e)}"
        db.commit()

        raise HTTPException(status_code=500, detail=f"简历解析失败：{str(e)}")


@app.put("/api/member-profile/{profile_id}/submit")
def submit_member_profile(
    profile_id: int,
    form: MemberProfileForm,
    db: Session = Depends(get_db)
):
    profile = db.query(SocietyMemberProfile).filter(
        SocietyMemberProfile.id == profile_id
    ).first()

    if not profile:
        raise HTTPException(status_code=404, detail="会员草稿不存在")

    profile.member_name = form.memberName
    profile.email = form.email
    profile.phone = form.phone

    profile.gender = form.gender
    profile.birth_year = form.birthYear
    profile.country_region = form.countryRegion

    profile.organization = form.organization
    profile.department = form.department
    profile.position_title = form.positionTitle
    profile.highest_degree = form.highestDegree
    profile.professional_field = form.professionalField

    profile.research_direction = form.researchDirection
    profile.personal_bio = form.personalBio
    profile.representative_achievements = form.representativeAchievements
    profile.homepage = form.homepage
    profile.orcid = form.orcid
    profile.scholar_profile = form.scholarProfile

    profile.member_type = form.memberType or "REGULAR"
    profile.member_status = "SUBMITTED"
    profile.join_date = date.today()
    profile.remark = form.remark

    try:
        db.commit()
        db.refresh(profile)

    except IntegrityError:
        db.rollback()
        raise HTTPException(status_code=400, detail="邮箱已存在，请检查会员邮箱")

    return {
        "code": 0,
        "message": "会员信息提交成功",
        "data": {
            "profileId": profile.id,
            "memberNo": profile.member_no,
            "memberStatus": profile.member_status
        }
    }