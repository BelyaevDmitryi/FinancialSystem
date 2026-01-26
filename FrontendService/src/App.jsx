import React from 'react'
import { Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import Layout from './components/Layout'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import DashboardPage from './pages/DashboardPage'
import TradingPage from './pages/TradingPage'
import AnalyticsPage from './pages/AnalyticsPage'
import BotsPage from './pages/BotsPage'
import AdminPage from './pages/AdminPage'
import ProfilePage from './pages/ProfilePage'
import ProtectedRoute from './components/ProtectedRoute'
import AdminRoute from './components/AdminRoute'

function App() {
  return (
    <AuthProvider>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route
          path="/"
          element={
            <ProtectedRoute>
              <Layout />
            </ProtectedRoute>
          }
        >
          <Route index element={<Navigate to="/dashboard" replace />} />
          <Route path="dashboard" element={<DashboardPage />} />
          <Route path="trading" element={<TradingPage />} />
          <Route path="analytics" element={<AnalyticsPage />} />
          <Route path="bots" element={<BotsPage />} />
          <Route path="profile" element={<ProfilePage />} />
          <Route 
            path="admin" 
            element={
              <AdminRoute>
                <AdminPage />
              </AdminRoute>
            } 
          />
        </Route>
      </Routes>
    </AuthProvider>
  )
}

export default App
