import csv
import io
import os
import re
import secrets
import shutil
from datetime import date, datetime, timedelta
from pathlib import Path

from fastapi import Depends, FastAPI, File, HTTPException, Response, UploadFile
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from fastapi.staticfiles import StaticFiles
from sqlalchemy import or_
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from database import Base, engine, get_db
from models import SocietyMemberProfile
from parser.pdf_parser import get_ocr_model
from parser.pdf_parser import parse_resume
from schemas import AdminLoginRequest, AdminMemberUpdateForm, MemberProfileForm


Base.metadata.create_all(bind=engine)

app = FastAPI(title="Society Member Profile System")

BASE_DIR = Path(__file__).resolve().parent
UPLOAD_ROOT = BASE_DIR / "uploads"
UPLOAD_DIR = UPLOAD_ROOT / "resumes"
UPLOAD_DIR.mkdir(parents=True, exist_ok=True)

ADMIN_USERNAME = os.getenv("MEMBER_ADMIN_USERNAME", "admin")
ADMIN_PASSWORD = os.getenv("MEMBER_ADMIN_PASSWORD", "admin123456")
ADMIN_TOKEN_TTL = timedelta(hours=12)
admin_bearer = HTTPBearer(auto_error=False)
admin_sessions: dict[str, dict[str, datetime | str]] = {}

app.mount("/uploads", StaticFiles(directory=str(UPLOAD_ROOT)), name="uploads")


@app.on_event("startup")
def warmup_ocr_model():
    get_ocr_model()


def save_resume_file(file: UploadFile) -> tuple[str, int]:
    suffix = Path(file.filename).suffix.lower()
    file_name = f"{datetime.now().strftime('%Y%m%d%H%M%S%f')}{suffix}"
    file_path = UPLOAD_DIR / file_name

    with open(file_path, "wb") as buffer:
        shutil.copyfileobj(file.file, buffer)

    size = os.path.getsize(file_path)
    return str(file_path), size


def build_resume_public_url(storage_path: str | None) -> str | None:
    if not storage_path:
        return None

    normalized_path = Path(storage_path)
    try:
        relative_path = normalized_path.relative_to(UPLOAD_ROOT)
    except ValueError:
        relative_path = Path(normalized_path.name)

    return "/" + str(Path("uploads") / relative_path).replace("\\", "/")


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


def extract_birth_date_from_text(text: str):
    if not text:
        return None

    patterns = [
        r"(?P<year>19\d{2}|20\d{2})[年\-.\/](?P<month>\d{1,2})[月\-.\/](?P<day>\d{1,2})日?",
        r"(?P<year>19\d{2}|20\d{2})[年\-.\/](?P<month>\d{1,2})月?",
    ]
    for pattern in patterns:
        match = re.search(pattern, text)
        if not match:
            continue
        year = int(match.group("year"))
        month = int(match.group("month"))
        day = int(match.groupdict().get("day") or 1)
        try:
            return date(year, month, day).isoformat()
        except ValueError:
            continue
    return None


def birth_year_from_date_value(value):
    if not value:
        return None

    text = str(value).strip()
    match = re.match(r"^(19\d{2}|20\d{2})", text)
    if not match:
        return None
    return int(match.group(1))


def parse_birth_date_value(value):
    if not value:
        return None

    text = str(value).strip()
    try:
        return date.fromisoformat(text[:10])
    except ValueError:
        return None


def achievements_to_text(value) -> str | None:
    if value is None:
        return None

    if isinstance(value, list):
        items = [str(item).strip() for item in value if str(item).strip()]
    else:
        raw_text = str(value).strip()
        if not raw_text:
            return None
        items = [item.strip() for item in re.split(r"[;；]\s*", raw_text) if item.strip()]

    if not items:
        return None
    return "；".join(items[:10])


def split_achievements_text(value: str | None) -> list[str]:
    if not value:
        return []
    return [item.strip() for item in re.split(r"[;；]\s*", value) if item.strip()]


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


def build_education_background(raw_result: dict) -> str:
    education_list = raw_result.get("教育背景", []) or []
    parts = []

    for item in education_list:
        school = item.get("学校") or ""
        major = item.get("专业") or ""
        degree = item.get("学历") or ""
        period = item.get("时间") or ""
        extra = item.get("补充信息") or ""

        line_parts = [period.strip(), school.strip(), major.strip(), degree.strip(), extra.strip()]
        line = " ".join(part for part in line_parts if part)
        if line:
            parts.append(line)

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

    birth_date = extract_birth_date_from_text(str(basic.get("出生日期") or ""))
    birth_year = birth_year_from_date_value(birth_date)
    if birth_year is None:
        birth_year = extract_birth_year_from_text(str(basic.get("出生日期") or ""))
    if birth_year is None:
        birth_year = parse_age_to_birth_year(basic.get("年龄"))
    if birth_date is None and birth_year is not None:
        birth_date = f"{birth_year}-01-01"

    skill_text = "；".join(
        f"{key}：{value}"
        for key, value in skills.items()
        if value
    )

    return {
        "memberName": basic.get("姓名"),
        "email": basic.get("电子邮箱"),
        "phone": basic.get("联系电话"),
        "gender": basic.get("性别"),
        "birthDate": birth_date,
        "birthYear": birth_year,
        "countryRegion": basic.get("国家或地区"),
        "organization": first_edu.get("学校"),
        "department": None,
        "positionTitle": basic.get("职务") or basic.get("职称"),
        "highestDegree": normalize_degree(first_edu.get("学历")),
        "professionalField": first_edu.get("专业"),
        "researchDirection": None,
        "educationBackground": build_education_background(raw_result),
        "representativeAchievements": build_representative_achievements(raw_result),
        "homepage": basic.get("个人主页"),
        "orcid": basic.get("ORCID"),
        "scholarProfile": basic.get("学术主页"),
        "memberType": "REGULAR",
        "remark": skill_text,
    }


def cleanup_admin_sessions():
    now = datetime.utcnow()
    expired_tokens = [
        token
        for token, session in admin_sessions.items()
        if session["expires_at"] <= now
    ]
    for token in expired_tokens:
        admin_sessions.pop(token, None)


def create_admin_token(username: str) -> tuple[str, datetime]:
    cleanup_admin_sessions()
    token = secrets.token_urlsafe(32)
    expires_at = datetime.utcnow() + ADMIN_TOKEN_TTL
    admin_sessions[token] = {
        "username": username,
        "expires_at": expires_at,
    }
    return token, expires_at


def require_admin(
    credentials: HTTPAuthorizationCredentials = Depends(admin_bearer),
):
    cleanup_admin_sessions()
    if not credentials or credentials.scheme.lower() != "bearer":
        raise HTTPException(status_code=401, detail="请先以管理员身份登录")

    session = admin_sessions.get(credentials.credentials)
    if not session:
        raise HTTPException(status_code=401, detail="管理员登录已失效，请重新登录")

    return session


def serialize_datetime(value: datetime | None) -> str | None:
    if not value:
        return None
    return value.isoformat()


def serialize_date(value: date | None) -> str | None:
    if not value:
        return None
    return value.isoformat()


def serialize_profile(profile: SocietyMemberProfile) -> dict:
    return {
        "profileId": profile.id,
        "memberName": profile.member_name,
        "email": profile.email,
        "phone": profile.phone,
        "gender": profile.gender,
        "birthDate": serialize_date(profile.birth_date),
        "countryRegion": profile.country_region,
        "organization": profile.organization,
        "department": profile.department,
        "positionTitle": profile.position_title,
        "highestDegree": profile.highest_degree,
        "professionalField": profile.professional_field,
        "researchDirection": profile.research_direction,
        "educationBackground": profile.education_background,
        "representativeAchievements": split_achievements_text(profile.representative_achievements),
        "homepage": profile.homepage,
        "orcid": profile.orcid,
        "scholarProfile": profile.scholar_profile,
        "memberType": profile.member_type or "REGULAR",
        "memberStatus": profile.member_status or "DRAFT",
        "joinDate": serialize_date(profile.join_date),
        "remark": profile.remark,
        "resumeFileUrl": build_resume_public_url(profile.resume_file_url),
        "resumeOriginalName": profile.resume_original_name,
        "resumeFileType": profile.resume_file_type,
        "resumeFileSize": profile.resume_file_size,
        "resumeUploadedAt": serialize_datetime(profile.resume_uploaded_at),
        "resumeParseStatus": profile.resume_parse_status or "NOT_PARSED",
        "resumeParsedAt": serialize_datetime(profile.resume_parsed_at),
        "createdAt": serialize_datetime(profile.created_at),
        "updatedAt": serialize_datetime(profile.updated_at),
    }


def apply_profile_form(
    profile: SocietyMemberProfile,
    form: MemberProfileForm,
    *,
    status_override: str | None = None,
    mark_submitted: bool = False,
):
    profile.member_name = form.memberName
    profile.email = form.email
    profile.phone = form.phone
    profile.gender = form.gender
    profile.birth_date = parse_birth_date_value(form.birthDate)
    if profile.birth_date is None and form.birthYear:
        profile.birth_date = date(form.birthYear, 1, 1)
    profile.country_region = form.countryRegion
    profile.organization = form.organization
    profile.department = form.department
    profile.position_title = form.positionTitle
    profile.highest_degree = form.highestDegree
    profile.professional_field = form.professionalField
    profile.research_direction = form.researchDirection
    profile.education_background = form.educationBackground
    profile.representative_achievements = achievements_to_text(form.representativeAchievements)
    profile.homepage = form.homepage
    profile.orcid = form.orcid
    profile.scholar_profile = form.scholarProfile
    profile.member_type = form.memberType or "REGULAR"
    profile.remark = form.remark

    if mark_submitted:
        profile.member_status = "SUBMITTED"
        profile.join_date = profile.join_date or date.today()
    elif status_override:
        profile.member_status = status_override
        if status_override == "SUBMITTED" and not profile.join_date:
            profile.join_date = date.today()


def build_member_query(db: Session, keyword: str | None, member_status: str | None):
    query = db.query(SocietyMemberProfile)

    if keyword:
        keyword = keyword.strip()
        if keyword:
            like_pattern = f"%{keyword}%"
            query = query.filter(
                or_(
                    SocietyMemberProfile.member_name.like(like_pattern),
                    SocietyMemberProfile.email.like(like_pattern),
                    SocietyMemberProfile.phone.like(like_pattern),
                    SocietyMemberProfile.organization.like(like_pattern),
                    SocietyMemberProfile.department.like(like_pattern),
                )
            )

    if member_status:
        query = query.filter(SocietyMemberProfile.member_status == member_status)

    return query


@app.post("/api/admin/login")
def admin_login(payload: AdminLoginRequest):
    if payload.username != ADMIN_USERNAME or payload.password != ADMIN_PASSWORD:
        raise HTTPException(status_code=401, detail="管理员账号或密码错误")

    token, expires_at = create_admin_token(payload.username)
    return {
        "code": 0,
        "message": "管理员登录成功",
        "data": {
            "token": token,
            "username": payload.username,
            "expiresAt": expires_at.isoformat(),
        },
    }


@app.get("/api/admin/session")
def get_admin_session(session=Depends(require_admin)):
    return {
        "code": 0,
        "message": "管理员会话有效",
        "data": {
            "username": session["username"],
            "expiresAt": session["expires_at"].isoformat(),
        },
    }


@app.get("/api/admin/member-profiles")
def list_member_profiles(
    keyword: str | None = None,
    memberStatus: str | None = None,
    page: int = 1,
    pageSize: int = 10,
    db: Session = Depends(get_db),
    _session=Depends(require_admin),
):
    page = max(page, 1)
    page_size = min(max(pageSize, 1), 100)

    query = build_member_query(db, keyword, memberStatus)
    total = query.count()
    items = (
        query.order_by(SocietyMemberProfile.updated_at.desc(), SocietyMemberProfile.id.desc())
        .offset((page - 1) * page_size)
        .limit(page_size)
        .all()
    )

    return {
        "code": 0,
        "message": "查询成功",
        "data": {
            "items": [serialize_profile(item) for item in items],
            "total": total,
            "page": page,
            "pageSize": page_size,
        },
    }


@app.get("/api/admin/member-profiles/export")
def export_member_profiles(
    keyword: str | None = None,
    memberStatus: str | None = None,
    db: Session = Depends(get_db),
    _session=Depends(require_admin),
):
    items = (
        build_member_query(db, keyword, memberStatus)
        .order_by(SocietyMemberProfile.updated_at.desc(), SocietyMemberProfile.id.desc())
        .all()
    )

    output = io.StringIO()
    writer = csv.writer(output)
    writer.writerow(
        [
            "档案ID",
            "姓名",
            "邮箱",
            "手机号",
            "性别",
            "出生日期",
            "国家或地区",
            "所在单位",
            "院系或部门",
            "职务或职称",
            "最高学历",
            "专业领域",
            "研究方向",
            "教育背景",
            "代表性成果",
            "个人主页",
            "ORCID",
            "学术主页",
            "会员类型",
            "会员状态",
            "入会日期",
            "简历原文件名",
            "简历解析状态",
            "备注",
            "创建时间",
            "更新时间",
        ]
    )

    for profile in items:
        writer.writerow(
            [
                profile.id,
                profile.member_name or "",
                profile.email or "",
                profile.phone or "",
                profile.gender or "",
                serialize_date(profile.birth_date) or "",
                profile.country_region or "",
                profile.organization or "",
                profile.department or "",
                profile.position_title or "",
                profile.highest_degree or "",
                profile.professional_field or "",
                profile.research_direction or "",
                profile.education_background or "",
                profile.representative_achievements or "",
                profile.homepage or "",
                profile.orcid or "",
                profile.scholar_profile or "",
                profile.member_type or "",
                profile.member_status or "",
                serialize_date(profile.join_date) or "",
                profile.resume_original_name or "",
                profile.resume_parse_status or "",
                profile.remark or "",
                serialize_datetime(profile.created_at) or "",
                serialize_datetime(profile.updated_at) or "",
            ]
        )

    csv_content = "\ufeff" + output.getvalue()
    file_name = f"member_profiles_{datetime.now().strftime('%Y%m%d%H%M%S')}.csv"
    return Response(
        content=csv_content,
        media_type="text/csv; charset=utf-8",
        headers={"Content-Disposition": f'attachment; filename="{file_name}"'},
    )


@app.get("/api/admin/member-profiles/{profile_id}")
def get_member_profile_detail(
    profile_id: int,
    db: Session = Depends(get_db),
    _session=Depends(require_admin),
):
    profile = db.query(SocietyMemberProfile).filter(SocietyMemberProfile.id == profile_id).first()
    if not profile:
        raise HTTPException(status_code=404, detail="会员信息不存在")

    return {
        "code": 0,
        "message": "查询成功",
        "data": serialize_profile(profile),
    }


@app.put("/api/admin/member-profiles/{profile_id}")
def update_member_profile(
    profile_id: int,
    form: AdminMemberUpdateForm,
    db: Session = Depends(get_db),
    _session=Depends(require_admin),
):
    profile = db.query(SocietyMemberProfile).filter(SocietyMemberProfile.id == profile_id).first()
    if not profile:
        raise HTTPException(status_code=404, detail="会员信息不存在")

    apply_profile_form(profile, form, status_override=form.memberStatus)

    try:
        db.commit()
        db.refresh(profile)
    except IntegrityError:
        db.rollback()
        raise HTTPException(status_code=400, detail="邮箱已存在，请检查会员邮箱")

    return {
        "code": 0,
        "message": "会员信息更新成功",
        "data": serialize_profile(profile),
    }


@app.delete("/api/admin/member-profiles/{profile_id}")
def delete_member_profile(
    profile_id: int,
    db: Session = Depends(get_db),
    _session=Depends(require_admin),
):
    profile = db.query(SocietyMemberProfile).filter(SocietyMemberProfile.id == profile_id).first()
    if not profile:
        raise HTTPException(status_code=404, detail="会员信息不存在")

    storage_path = profile.resume_file_url
    db.delete(profile)
    db.commit()

    if storage_path and os.path.exists(storage_path):
        try:
            os.remove(storage_path)
        except OSError:
            pass

    return {
        "code": 0,
        "message": "会员信息删除成功",
        "data": {"profileId": profile_id},
    }


@app.post("/api/member-profile/draft")
def create_member_profile_draft(db: Session = Depends(get_db)):
    profile = SocietyMemberProfile(
        member_status="DRAFT",
        resume_parse_status="NOT_PARSED",
    )

    db.add(profile)
    db.commit()
    db.refresh(profile)

    return {
        "code": 0,
        "message": "空白档案创建成功",
        "data": {
            "profileId": profile.id,
            "memberStatus": profile.member_status,
            "resumeParseStatus": profile.resume_parse_status,
        },
    }


@app.post("/api/member-profile/resume/upload")
async def upload_resume(
    file: UploadFile = File(...),
    db: Session = Depends(get_db),
):
    if not file.filename:
        raise HTTPException(status_code=400, detail="文件名不能为空")

    if not file.filename.lower().endswith(".pdf"):
        raise HTTPException(status_code=400, detail="只支持 PDF 文件")

    storage_path, file_size = save_resume_file(file)

    profile = SocietyMemberProfile(
        member_status="DRAFT",
        resume_file_url=storage_path,
        resume_original_name=file.filename,
        resume_file_type="PDF",
        resume_file_size=file_size,
        resume_uploaded_at=datetime.now(),
        resume_parse_status="NOT_PARSED",
    )

    db.add(profile)
    db.commit()
    db.refresh(profile)

    return {
        "code": 0,
        "message": "上传成功",
        "data": {
            "profileId": profile.id,
            "resumeFileUrl": build_resume_public_url(profile.resume_file_url),
            "resumeOriginalName": profile.resume_original_name,
            "resumeParseStatus": profile.resume_parse_status,
        },
    }


@app.post("/api/member-profile/{profile_id}/resume/skip-parse")
def skip_parse_resume(
    profile_id: int,
    db: Session = Depends(get_db),
):
    profile = db.query(SocietyMemberProfile).filter(SocietyMemberProfile.id == profile_id).first()
    if not profile:
        raise HTTPException(status_code=404, detail="会员草稿不存在")

    profile.resume_parse_status = "SKIPPED"
    db.commit()

    return {
        "code": 0,
        "message": "已跳过解析",
        "data": {
            "profileId": profile.id,
            "resumeParseStatus": profile.resume_parse_status,
        },
    }


@app.post("/api/member-profile/{profile_id}/resume/parse")
def parse_resume_file(
    profile_id: int,
    db: Session = Depends(get_db),
):
    profile = db.query(SocietyMemberProfile).filter(SocietyMemberProfile.id == profile_id).first()
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
                "resumeParseStatus": profile.resume_parse_status,
                "form": form,
            },
        }
    except Exception as exc:
        profile.resume_parse_status = "PARSE_FAILED"
        profile.remark = f"简历解析失败：{str(exc)}"
        db.commit()
        raise HTTPException(status_code=500, detail=f"简历解析失败：{str(exc)}")


@app.put("/api/member-profile/{profile_id}/submit")
def submit_member_profile(
    profile_id: int,
    form: MemberProfileForm,
    db: Session = Depends(get_db),
):
    profile = db.query(SocietyMemberProfile).filter(SocietyMemberProfile.id == profile_id).first()
    if not profile:
        raise HTTPException(status_code=404, detail="会员草稿不存在")

    apply_profile_form(profile, form, mark_submitted=True)

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
            "memberStatus": profile.member_status,
        },
    }
