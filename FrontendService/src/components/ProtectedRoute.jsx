import React from 'react'
import { Navigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { CircularProgress, Box } from '@mui/material'

const ProtectedRoute = ({ children }) => {
  const { token, loading } = useAuth()
  const storedToken = typeof window !== 'undefined' ? localStorage.getItem('token') : null
  const effectiveToken = token || storedToken

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

  if (!effectiveToken) {
    return <Navigate to="/login" replace />
  }

  return children
}

export default ProtectedRoute
