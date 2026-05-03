import axios from 'axios'

const getBaseURL = () => {
  if (import.meta.env.VITE_API_URL) {
    return import.meta.env.VITE_API_URL
  }
  return ''
}

const api = axios.create({
  baseURL: getBaseURL(),
  headers: {
    'Content-Type': 'application/json',
  },
})

// Добавляем access token в запросы. X-User-Id проставляет ApiGateway после валидации JWT.
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// При 401 пробуем обновить сессию через refresh token; при неудаче — выход и редирект на логин.
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config

    if (error.response?.status !== 401) {
      logError(error)
      return Promise.reject(error)
    }

    // Уже повторяли запрос после refresh — выходим
    if (originalRequest._retriedAfterRefresh) {
      clearAuthAndRedirect()
      return Promise.reject(error)
    }

    const refreshToken = localStorage.getItem('refreshToken')
    if (!refreshToken) {
      clearAuthAndRedirect()
      return Promise.reject(error)
    }

    try {
      const baseURL = getBaseURL()
      const { data } = await axios.post(
        `${baseURL}/api/auth/refresh`,
        { refreshToken },
        { headers: { 'Content-Type': 'application/json' } }
      )
      const newToken = data.token ?? data.accessToken
      const newRefresh = data.refreshToken
      if (newToken) {
        localStorage.setItem('token', newToken)
        if (newRefresh) localStorage.setItem('refreshToken', newRefresh)
        originalRequest.headers['Authorization'] = `Bearer ${newToken}`
        originalRequest._retriedAfterRefresh = true
        return api(originalRequest)
      }
    } catch (refreshError) {
      logError(refreshError)
    }

    clearAuthAndRedirect()
    return Promise.reject(error)
  }
)

function clearAuthAndRedirect() {
  localStorage.removeItem('token')
  localStorage.removeItem('refreshToken')
  localStorage.removeItem('user')
  window.location.href = '/login'
}

function logError(error) {
  if (error.code === 'ERR_NETWORK' || error.message === 'Network Error') {
    console.error('Network Error:', { message: error.message, code: error.code })
  } else {
    console.error('API Error:', {
      status: error.response?.status,
      data: error.response?.data,
      message: error.message,
    })
  }
}

export default api
