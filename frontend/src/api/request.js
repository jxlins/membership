import axios from 'axios'
import { ElMessage } from 'element-plus'

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || '/api'

const request = axios.create({
  baseURL: apiBaseUrl,
  timeout: 180000
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
    const message = error.response?.data?.message || error.response?.data?.detail || error.message || '网络请求失败'
    ElMessage.error(message)
    return Promise.reject(new Error(message))
  }
)

export default request
