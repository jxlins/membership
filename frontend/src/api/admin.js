import request from './request'

export function adminLogin(data) {
  return request.post('/admin/login', data)
}

export function getAdminSession() {
  return request.get('/admin/session')
}

export function getMemberProfiles(params) {
  return request.get('/admin/member-profiles', { params })
}

export function exportMemberProfiles(params) {
  return request.get('/admin/member-profiles/export', {
    params,
    responseType: 'blob'
  })
}

export function getMemberProfileDetail(profileId) {
  return request.get(`/admin/member-profiles/${encodeURIComponent(profileId)}`)
}

export function updateMemberProfile(profileId, data) {
  return request.put(`/admin/member-profiles/${encodeURIComponent(profileId)}`, data)
}

export function deleteMemberProfile(profileId) {
  return request.delete(`/admin/member-profiles/${encodeURIComponent(profileId)}`)
}
