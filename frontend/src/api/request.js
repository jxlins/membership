import axios from 'axios'
import { ElMessage } from 'element-plus'

import { clearAdminAuth, getAdminToken } from '../utils/adminAuth'

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || '/member-api'

const request = axios.create({
  baseURL: apiBaseUrl,
  timeout: 300000
})

request.interceptors.request.use(config => {
  const token = getAdminToken()
  if (token) {
    config.headers = config.headers || {}
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  response => {
    const body = response.data
    if (body && typeof body.code !== 'undefined') {
      if (body.code === 200 || body.code === 0) {
        return body.data
      }
      const message = body.message || body.detail || '请求失败'
      ElMessage.error(message)
      return Promise.reject(new Error(message))
    }
    return body
  },
  error => {
    if (error.response?.status === 401) {
      clearAdminAuth()
    }
    const message = error.response?.data?.message || error.response?.data?.detail || error.message || '网络请求失败'
    ElMessage.error(message)
    return Promise.reject(new Error(message))
  }
)

export default request
