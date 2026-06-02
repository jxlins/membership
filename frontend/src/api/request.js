import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '',
  timeout: 30000
})

request.interceptors.response.use(
  response => {
    const body = response.data
    if (body && typeof body.code !== 'undefined') {
      if (body.code === 200) {
        return body.data
      }
      const message = body.message || '请求失败'
      ElMessage.error(message)
      return Promise.reject(new Error(message))
    }
    return body
  },
  error => {
    const message = error.response?.data?.message || error.message || '网络请求失败'
    ElMessage.error(message)
    return Promise.reject(new Error(message))
  }
)

export default request
