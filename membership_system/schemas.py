from typing import List, Optional, Union

from pydantic import BaseModel


class AdminLoginRequest(BaseModel):
    username: str
    password: str


class MemberProfileForm(BaseModel):
    memberName: Optional[str] = None
    email: Optional[str] = None
    phone: Optional[str] = None

    gender: Optional[str] = None
    birthDate: Optional[str] = None
    birthYear: Optional[int] = None
    countryRegion: Optional[str] = None

    organization: Optional[str] = None
    department: Optional[str] = None
    positionTitle: Optional[str] = None
    highestDegree: Optional[str] = None
    professionalField: Optional[str] = None

    researchDirection: Optional[str] = None
    educationBackground: Optional[str] = None
    representativeAchievements: Optional[Union[str, List[str]]] = None
    homepage: Optional[str] = None
    orcid: Optional[str] = None
    scholarProfile: Optional[str] = None

    memberType: Optional[str] = "REGULAR"
    remark: Optional[str] = None


class AdminMemberUpdateForm(MemberProfileForm):
    memberStatus: Optional[str] = None
