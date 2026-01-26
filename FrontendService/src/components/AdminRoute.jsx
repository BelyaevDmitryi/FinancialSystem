import React from 'react'
import { Navigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { CircularProgress, Box, Alert } from '@mui/material'

const AdminRoute = ({ children }) => {
  const { user, loading } = useAuth()

  if (loading) {
    return (
      <Box
        display="flex"
        justifyContent="center"
        alignItems="center"
        minHeight="100vh"
      >
        <CircularProgress />
      </Box>
    )
  }

  // Проверяем наличие ролей ADMIN или OWNER
  const hasAdminAccess = user?.roles?.some(role => 
    role === 'ROLE_ADMIN' || role === 'ROLE_OWNER'
  )

  if (!hasAdminAccess) {
    return (
      <Box sx={{ p: 3 }}>
        <Alert severity="error">
          У вас нет доступа к админ панели
        </Alert>
      </Box>
    )
  }

  return children
}

export default AdminRoute
