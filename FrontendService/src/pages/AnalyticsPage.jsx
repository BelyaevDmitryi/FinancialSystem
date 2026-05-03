import React, { useState } from 'react'
import {
  Box,
  Paper,
  Typography,
  TextField,
  Button,
  Grid,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  Alert,
} from '@mui/material'
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts'
import api from '../services/api'

const AnalyticsPage = () => {
  const [formData, setFormData] = useState({
    figi: '',
    period: 20,
    indicator: 'SMA',
  })
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const handleSubmit = async (e) => {
    e.preventDefault()
    setLoading(true)
    setError('')
    setData(null)

    try {
      const endpoint = formData.indicator === 'SMA' ? '/api/analytics/sma' : '/api/analytics/ema'
      const response = await api.post(endpoint, {
        figi: formData.figi,
        period: formData.period,
      })
      setData(response.data)
    } catch (err) {
      setError('Не удалось получить аналитику')
      console.error(err)
    } finally {
      setLoading(false)
    }
  }

  const chartData = (data?.values && data.values.length > 0)
    ? data.values.map((value, index) => ({
        name: `Точка ${index + 1}`,
        value: Number(value),
      }))
    : (data?.sma != null || data?.ema != null)
      ? [{ name: formData.indicator, value: Number(data.sma ?? data.ema) }]
      : []

  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        Технический анализ
      </Typography>

      <Paper sx={{ p: 3, mb: 3 }}>
        <form onSubmit={handleSubmit}>
          <Grid container spacing={2}>
            <Grid item xs={12} md={4}>
              <TextField
                label="FIGI"
                value={formData.figi}
                onChange={(e) => setFormData({ ...formData, figi: e.target.value })}
                fullWidth
                required
              />
            </Grid>
            <Grid item xs={12} md={4}>
              <FormControl fullWidth>
                <InputLabel>Индикатор</InputLabel>
                <Select
                  value={formData.indicator}
                  onChange={(e) => setFormData({ ...formData, indicator: e.target.value })}
                  label="Индикатор"
                >
                  <MenuItem value="SMA">SMA (Simple Moving Average)</MenuItem>
                  <MenuItem value="EMA">EMA (Exponential Moving Average)</MenuItem>
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12} md={2}>
              <TextField
                label="Период"
                type="number"
                value={formData.period}
                onChange={(e) => setFormData({ ...formData, period: parseInt(e.target.value) })}
                fullWidth
                required
              />
            </Grid>
            <Grid item xs={12} md={2}>
              <Button type="submit" variant="contained" fullWidth sx={{ height: '56px' }}>
                Рассчитать
              </Button>
            </Grid>
          </Grid>
        </form>
      </Paper>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError('')}>
          {error}
        </Alert>
      )}

      {data && (
        <Paper sx={{ p: 3 }}>
          <Typography variant="h6" gutterBottom>
            Результат: {formData.indicator}
          </Typography>
          {(data.sma != null || data.ema != null) && (
            <Typography variant="body1" sx={{ mb: 2 }}>
              {formData.indicator}({formData.period}) = {Number(data.sma ?? data.ema).toFixed(2)}
            </Typography>
          )}
          {chartData.length > 0 ? (
            <ResponsiveContainer width="100%" height={400}>
              <LineChart data={chartData}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="name" />
                <YAxis />
                <Tooltip />
                <Legend />
                <Line
                  type="monotone"
                  dataKey="value"
                  stroke="#8884d8"
                  name={formData.indicator}
                />
              </LineChart>
            </ResponsiveContainer>
          ) : (
            <Typography color="textSecondary" align="center" sx={{ mt: 4 }}>
              Нет данных для графика
            </Typography>
          )}
        </Paper>
      )}
    </Box>
  )
}

export default AnalyticsPage
