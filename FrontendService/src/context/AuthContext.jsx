import React, { createContext, useState, useContext, useEffect } from 'react'
import { flushSync } from 'react-dom'
import api from '../services/api'

const AuthContext = createContext(null)

export const useAuth = () => {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider')
  }
  return context
}

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null)
  const [token, setToken] = useState(localStorage.getItem('token'))
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (token) {
      const userData = localStorage.getItem('user')
      if (userData) {
        const parsed = JSON.parse(userData)
        // Убедимся, что роли есть в данных пользователя
        if (!parsed.roles) {
          parsed.roles = []
        }
        setUser(parsed)
      }
      // Authorization header добавляется автоматически через interceptor в api.js
    }
    setLoading(false)
  }, [token])

  const login = async (username, password) => {
    try {
      const response = await api.post('/api/auth/signin', {
        username,
        password,
      })
      const data = response.data || {}
      const newToken = data.token ?? data.accessToken
      const newRefreshToken = data.refreshToken
      const { id, name, roles } = data
      const userData = { id, name, username: data.username ?? username, roles: roles || [] }

      if (!newToken) {
        return {
          success: false,
          error: 'Ответ сервера не содержит access token',
        }
      }

      // Сначала localStorage, затем синхронный flush контекста — иначе ProtectedRoute/дашборд могут отработать до обновления token.
      localStorage.setItem('token', newToken)
      if (newRefreshToken) localStorage.setItem('refreshToken', newRefreshToken)
      localStorage.setItem('user', JSON.stringify(userData))
      flushSync(() => {
        setToken(newToken)
        setUser(userData)
      })

      return { success: true }
    } catch (error) {
      const errorMessage = error.response?.data?.error || 
                          error.response?.data?.message || 
                          error.message || 
                          'Ошибка входа'
      return {
        success: false,
        error: errorMessage,
      }
    }
  }

  const register = async (username, password, name) => {
    try {
      await api.post('/api/auth/signup', {
        username,
        password,
        name,
      })
      return { success: true }
    } catch (error) {
      console.error('Registration error:', error)
      
      // Обработка Network Error
      if (error.code === 'ERR_NETWORK' || error.message === 'Network Error') {
        return {
          success: false,
          error: 'Не удалось подключиться к серверу. Проверьте, что сервер запущен и доступен.',
        }
      }
      
      // Обработка ошибок с ответом от сервера
      if (error.response) {
        const errorMessage = error.response.data?.error || 
                            error.response.data?.message || 
                            `Ошибка сервера: ${error.response.status}`
        return {
          success: false,
          error: errorMessage,
        }
      }
      
      // Обработка других ошибок
      return {
        success: false,
        error: error.message || 'Ошибка регистрации',
      }
    }
  }

  const logout = () => {
    setToken(null)
    setUser(null)
    localStorage.removeItem('token')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('user')
  }

  return (
    <AuthContext.Provider value={{ user, token, login, register, logout, loading }}>
      {children}
    </AuthContext.Provider>
  )
}
