from typing import Optional
from pydantic import BaseModel


class MemberProfileForm(BaseModel):
    memberName: Optional[str] = None
    email: Optional[str] = None
    phone: Optional[str] = None

    gender: Optional[str] = None
    birthYear: Optional[int] = None
    countryRegion: Optional[str] = None

    organization: Optional[str] = None
    department: Optional[str] = None
    positionTitle: Optional[str] = None
    highestDegree: Optional[str] = None
    professionalField: Optional[str] = None

    researchDirection: Optional[str] = None
    personalBio: Optional[str] = None
    representativeAchievements: Optional[str] = None
    homepage: Optional[str] = None
    orcid: Optional[str] = None
    scholarProfile: Optional[str] = None

    memberType: Optional[str] = "REGULAR"
    remark: Optional[str] = None