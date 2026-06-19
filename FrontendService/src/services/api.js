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

/**
 * Axios 1.x использует AxiosHeaders: присвоение через ['Authorization'] не всегда попадает в исходящий запрос.
 * Единая точка гарантирует наличие Bearer в wire-формате.
 */
function setAuthorizationHeader(config, rawToken) {
  if (!rawToken) {
    return
  }
  const value = `Bearer ${rawToken}`
  const headers = config.headers
  if (!headers) {
    config.headers = { Authorization: value }
    return
  }
  if (typeof headers.set === 'function') {
    headers.set('Authorization', value)
  }
  // Дублируем присвоением поля: в Axios 1.x иногда в wire уходит только одно из представлений заголовков.
  headers.Authorization = value
}

api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    setAuthorizationHeader(config, token)
    return config
  },
  (error) => Promise.reject(error)
)

let refreshInFlight = null

async function refreshAccessToken() {
  const refreshToken = localStorage.getItem('refreshToken')
  if (!refreshToken) {
    throw new Error('Refresh token отсутствует')
  }
  const baseURL = getBaseURL()
  const { data } = await axios.post(
    `${baseURL}/api/auth/refresh`,
    { refreshToken },
    { headers: { 'Content-Type': 'application/json' } }
  )
  const newToken = data.token ?? data.accessToken
  const newRefresh = data.refreshToken
  if (!newToken) {
    throw new Error('Ответ refresh не содержит access token')
  }
  localStorage.setItem('token', newToken)
  if (newRefresh) {
    localStorage.setItem('refreshToken', newRefresh)
  }
  return newToken
}

function isAuthEndpoint401(config) {
  const url = typeof config?.url === 'string' ? config.url : ''
  return (
    url.includes('/api/auth/signin') ||
    url.includes('/api/auth/signup') ||
    url.includes('/api/auth/refresh')
  )
}

// При 401 пробуем обновить сессию через refresh token; при неудаче — выход и редирект на логин.
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config

    if (error.response?.status !== 401) {
      logError(error)
      return Promise.reject(error)
    }

    // Неверный пароль при логине и прочие 401 на публичных auth — не пускаем в цикл refresh.
    if (isAuthEndpoint401(originalRequest)) {
      logError(error)
      return Promise.reject(error)
    }

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
      if (!refreshInFlight) {
        refreshInFlight = refreshAccessToken().finally(() => {
          refreshInFlight = null
        })
      }
      await refreshInFlight
      const token = localStorage.getItem('token')
      setAuthorizationHeader(originalRequest, token)
      originalRequest._retriedAfterRefresh = true
      return api(originalRequest)
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
