import request from './request'

export function uploadResume(data) {
  return request.post('/api/society-member/resume/upload', data, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export function submitMemberProfile(data) {
  return request.post('/api/society-member/profile/submit', data)
}

export function getMemberProfile(memberNo) {
  return request.get(`/api/society-member/profile/${encodeURIComponent(memberNo)}`)
}
