import request from './request'

export function uploadResume(data) {
  return request.post('/member-profile/resume/upload', data, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export function parseResume(profileId) {
  return request.post(`/member-profile/${encodeURIComponent(profileId)}/resume/parse`)
}

export function skipParseResume(profileId) {
  return request.post(`/member-profile/${encodeURIComponent(profileId)}/resume/skip-parse`)
}

export function submitMemberProfile(data) {
  const { profileId, ...form } = data
  if (!profileId) {
    throw new Error('profileId is required')
  }
  return request.put(`/member-profile/${encodeURIComponent(profileId)}/submit`, form)
}
