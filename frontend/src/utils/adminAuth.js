const ADMIN_TOKEN_KEY = 'memberAdminToken'
const ADMIN_USERNAME_KEY = 'memberAdminUsername'

export function getAdminToken() {
  if (typeof window === 'undefined') {
    return ''
  }
  return window.localStorage.getItem(ADMIN_TOKEN_KEY) || ''
}

export function getAdminUsername() {
  if (typeof window === 'undefined') {
    return ''
  }
  return window.localStorage.getItem(ADMIN_USERNAME_KEY) || ''
}

export function setAdminAuth(token, username) {
  if (typeof window === 'undefined') {
    return
  }
  window.localStorage.setItem(ADMIN_TOKEN_KEY, token || '')
  window.localStorage.setItem(ADMIN_USERNAME_KEY, username || '')
}

export function clearAdminAuth() {
  if (typeof window === 'undefined') {
    return
  }
  window.localStorage.removeItem(ADMIN_TOKEN_KEY)
  window.localStorage.removeItem(ADMIN_USERNAME_KEY)
}

export function hasAdminAuth() {
  return Boolean(getAdminToken())
}
