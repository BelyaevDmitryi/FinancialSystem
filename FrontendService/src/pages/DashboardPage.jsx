import React, { useEffect, useState } from 'react'
import {
  Box,
  Grid,
  Paper,
  Typography,
  Card,
  CardContent,
  CircularProgress,
  Alert,
} from '@mui/material'
import { PieChart, Pie, Cell, ResponsiveContainer, Legend, Tooltip } from 'recharts'
import api from '../services/api'
import { useAuth } from '../context/AuthContext'

const COLORS = ['#0088FE', '#00C49F', '#FFBB28', '#FF8042', '#8884d8']

const DashboardPage = () => {
  const { user } = useAuth()
  const [dashboard, setDashboard] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    if (user?.id) {
      fetchDashboard()
    }
  }, [user])

  const fetchDashboard = async () => {
    try {
      setLoading(true)
      // X-User-Id добавляется автоматически ApiGateway после валидации JWT
      const response = await api.get('/api/dashboard')
      setDashboard(response.data)
      setError('')
    } catch (err) {
      setError('Не удалось загрузить данные дашборда')
      console.error(err)
    } finally {
      setLoading(false)
    }
  }

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    )
  }

  if (error) {
    return <Alert severity="error">{error}</Alert>
  }

  if (!dashboard) {
    return <Alert severity="info">Нет данных для отображения</Alert>
  }

  const portfolioData = dashboard.portfolioDistribution?.map((item, index) => ({
    name: item.name || `Актив ${index + 1}`,
    value: item.value || 0,
  })) || []

  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        Дашборд портфеля
      </Typography>
      <Grid container spacing={3} sx={{ mt: 2 }}>
        <Grid item xs={12} md={4}>
          <Card>
            <CardContent>
              <Typography color="textSecondary" gutterBottom>
                Общая стоимость портфеля
              </Typography>
              <Typography variant="h4">
                {dashboard.totalValue?.toLocaleString('ru-RU', {
                  style: 'currency',
                  currency: 'RUB',
                }) || '0 ₽'}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} md={4}>
          <Card>
            <CardContent>
              <Typography color="textSecondary" gutterBottom>
                Количество позиций
              </Typography>
              <Typography variant="h4">
                {dashboard.positionsCount || 0}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} md={4}>
          <Card>
            <CardContent>
              <Typography color="textSecondary" gutterBottom>
                Прибыль/Убыток
              </Typography>
              <Typography
                variant="h4"
                color={dashboard.profitLoss >= 0 ? 'success.main' : 'error.main'}
              >
                {dashboard.profitLoss?.toLocaleString('ru-RU', {
                  style: 'currency',
                  currency: 'RUB',
                }) || '0 ₽'}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} md={6}>
          <Paper sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom>
              Распределение портфеля
            </Typography>
            {portfolioData.length > 0 ? (
              <ResponsiveContainer width="100%" height={300}>
                <PieChart>
                  <Pie
                    data={portfolioData}
                    cx="50%"
                    cy="50%"
                    labelLine={false}
                    label={({ name, percent }) =>
                      `${name}: ${(percent * 100).toFixed(0)}%`
                    }
                    outerRadius={80}
                    fill="#8884d8"
                    dataKey="value"
                  >
                    {portfolioData.map((entry, index) => (
                      <Cell
                        key={`cell-${index}`}
                        fill={COLORS[index % COLORS.length]}
                      />
                    ))}
                  </Pie>
                  <Tooltip />
                  <Legend />
                </PieChart>
              </ResponsiveContainer>
            ) : (
              <Typography color="textSecondary" align="center" sx={{ mt: 4 }}>
                Нет данных для графика
              </Typography>
            )}
          </Paper>
        </Grid>
        <Grid item xs={12} md={6}>
          <Paper sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom>
              Топ позиций
            </Typography>
            {dashboard.topPositions?.length > 0 ? (
              <Box sx={{ mt: 2 }}>
                {dashboard.topPositions.map((position, index) => (
                  <Box
                    key={index}
                    sx={{
                      display: 'flex',
                      justifyContent: 'space-between',
                      py: 1,
                      borderBottom: '1px solid #e0e0e0',
                    }}
                  >
                    <Typography>{position.name || position.ticker}</Typography>
                    <Typography fontWeight="bold">
                      {position.value?.toLocaleString('ru-RU', {
                        style: 'currency',
                        currency: 'RUB',
                      }) || '0 ₽'}
                    </Typography>
                  </Box>
                ))}
              </Box>
            ) : (
              <Typography color="textSecondary" align="center" sx={{ mt: 4 }}>
                Нет позиций
              </Typography>
            )}
          </Paper>
        </Grid>
      </Grid>
    </Box>
  )
}

export default DashboardPage
