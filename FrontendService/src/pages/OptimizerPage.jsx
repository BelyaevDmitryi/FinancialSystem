import React, { useState } from 'react'
import {
  Alert,
  Box,
  Button,
  CircularProgress,
  Grid,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material'
import { runGridOptimization } from '../services/optimizerApi'

const DEFAULT_FIGI = 'BBG004730N88'
const DEFAULT_FROM = '2026-01-01'
const DEFAULT_TO = '2026-03-01'

function dateToInstant(dateStr) {
  return `${dateStr}T00:00:00Z`
}

function formatPercent(value) {
  if (value == null || Number.isNaN(Number(value))) {
    return '—'
  }
  return `${(Number(value) * 100).toFixed(2)}%`
}

function formatMoney(value) {
  if (value == null || Number.isNaN(Number(value))) {
    return '—'
  }
  return Number(value).toLocaleString('ru-RU', {
    style: 'currency',
    currency: 'RUB',
    maximumFractionDigits: 2,
  })
}

function formatParameters(params) {
  if (!params || typeof params !== 'object') {
    return '—'
  }
  return Object.entries(params)
    .map(([key, val]) => `${key}=${val}`)
    .join(', ')
}

const OptimizerPage = () => {
  const [form, setForm] = useState({
    figi: DEFAULT_FIGI,
    from: DEFAULT_FROM,
    to: DEFAULT_TO,
    smaMin: 10,
    smaMax: 20,
    smaStep: 2,
    topK: 10,
  })
  const [response, setResponse] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const handleChange = (field) => (event) => {
    const numericFields = ['smaMin', 'smaMax', 'smaStep', 'topK']
    const value = numericFields.includes(field)
      ? Number(event.target.value)
      : event.target.value
    setForm((prev) => ({ ...prev, [field]: value }))
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    setLoading(true)
    setError('')
    try {
      const data = await runGridOptimization({
        figi: form.figi.trim(),
        from: dateToInstant(form.from),
        to: dateToInstant(form.to),
        interval: 'DAY',
        initialCash: 100000,
        slippageBps: 0,
        parameters: [
          {
            name: 'smaPeriod',
            min: form.smaMin,
            max: form.smaMax,
            step: form.smaStep,
            stepType: 'ABSOLUTE',
          },
        ],
        filters: {
          minProfitFactor: 0,
          maxDrawdown: 1,
          minTrades: 0,
        },
      })
      setResponse(data)
    } catch (err) {
      const message =
        err.response?.data?.message ||
        err.response?.data?.error ||
        'Не удалось выполнить grid-оптимизацию'
      setError(message)
      setResponse(null)
    } finally {
      setLoading(false)
    }
  }

  const topResults = (response?.results ?? []).slice(0, form.topK)

  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        Grid Optimizer
      </Typography>

      <Paper sx={{ p: 3, mb: 3 }} component="form" onSubmit={handleSubmit}>
        <Grid container spacing={2} alignItems="center">
          <Grid item xs={12} sm={6} md={3}>
            <TextField
              label="FIGI"
              fullWidth
              required
              value={form.figi}
              onChange={handleChange('figi')}
            />
          </Grid>
          <Grid item xs={12} sm={6} md={2}>
            <TextField
              label="From"
              type="date"
              fullWidth
              required
              InputLabelProps={{ shrink: true }}
              value={form.from}
              onChange={handleChange('from')}
            />
          </Grid>
          <Grid item xs={12} sm={6} md={2}>
            <TextField
              label="To"
              type="date"
              fullWidth
              required
              InputLabelProps={{ shrink: true }}
              value={form.to}
              onChange={handleChange('to')}
            />
          </Grid>
          <Grid item xs={6} sm={4} md={1}>
            <TextField
              label="SMA min"
              type="number"
              fullWidth
              inputProps={{ min: 1 }}
              value={form.smaMin}
              onChange={handleChange('smaMin')}
            />
          </Grid>
          <Grid item xs={6} sm={4} md={1}>
            <TextField
              label="SMA max"
              type="number"
              fullWidth
              inputProps={{ min: 1 }}
              value={form.smaMax}
              onChange={handleChange('smaMax')}
            />
          </Grid>
          <Grid item xs={6} sm={4} md={1}>
            <TextField
              label="SMA step"
              type="number"
              fullWidth
              inputProps={{ min: 1 }}
              value={form.smaStep}
              onChange={handleChange('smaStep')}
            />
          </Grid>
          <Grid item xs={6} sm={4} md={1}>
            <TextField
              label="Top-K"
              type="number"
              fullWidth
              inputProps={{ min: 1 }}
              value={form.topK}
              onChange={handleChange('topK')}
            />
          </Grid>
          <Grid item xs={12} md={1}>
            <Button type="submit" variant="contained" fullWidth disabled={loading}>
              {loading ? <CircularProgress size={24} color="inherit" /> : 'Запустить'}
            </Button>
          </Grid>
        </Grid>
      </Paper>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}

      {response && (
        <>
          <Typography variant="body2" color="textSecondary" sx={{ mb: 2 }}>
            Всего прогонов: {response.totalRuns ?? 0}, прошло фильтры:{' '}
            {response.passedFilters ?? 0}
          </Typography>
          <TableContainer component={Paper}>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Rank</TableCell>
                  <TableCell>Параметры</TableCell>
                  <TableCell align="right">Return</TableCell>
                  <TableCell align="right">Drawdown</TableCell>
                  <TableCell align="right">Profit Factor</TableCell>
                  <TableCell align="right">Final Equity</TableCell>
                  <TableCell align="right">Trades</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {topResults.length > 0 ? (
                  topResults.map((row) => (
                    <TableRow key={row.rank ?? formatParameters(row.parameters)}>
                      <TableCell>{row.rank ?? '—'}</TableCell>
                      <TableCell>{formatParameters(row.parameters)}</TableCell>
                      <TableCell align="right">{formatPercent(row.totalReturn)}</TableCell>
                      <TableCell align="right">{formatPercent(row.maxDrawdown)}</TableCell>
                      <TableCell align="right">{row.profitFactor?.toString() ?? '—'}</TableCell>
                      <TableCell align="right">{formatMoney(row.finalEquity)}</TableCell>
                      <TableCell align="right">{row.trades ?? 0}</TableCell>
                    </TableRow>
                  ))
                ) : (
                  <TableRow>
                    <TableCell colSpan={7} align="center">
                      Нет результатов
                    </TableCell>
                  </TableRow>
                )}
              </TableBody>
            </Table>
          </TableContainer>
        </>
      )}
    </Box>
  )
}

export default OptimizerPage
