import React, { useMemo, useState } from 'react'
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
import {
  CartesianGrid,
  Legend,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import { runBacktest } from '../services/backtestApi'

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

const BacktestPage = () => {
  const [form, setForm] = useState({
    figi: DEFAULT_FIGI,
    from: DEFAULT_FROM,
    to: DEFAULT_TO,
    smaPeriod: 20,
  })
  const [result, setResult] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const chartData = useMemo(() => {
    if (!result?.equityCurve?.length) {
      return []
    }
    return result.equityCurve.map((point) => ({
      time: new Date(point.time).getTime(),
      equity: Number(point.equity),
    }))
  }, [result])

  const handleChange = (field) => (event) => {
    const value = field === 'smaPeriod' ? Number(event.target.value) : event.target.value
    setForm((prev) => ({ ...prev, [field]: value }))
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    setLoading(true)
    setError('')
    try {
      const data = await runBacktest({
        figi: form.figi.trim(),
        from: dateToInstant(form.from),
        to: dateToInstant(form.to),
        interval: 'DAY',
        smaPeriod: form.smaPeriod,
        initialCash: 100000,
        slippageBps: 0,
      })
      setResult(data)
    } catch (err) {
      const message =
        err.response?.data?.message ||
        err.response?.data?.error ||
        'Не удалось выполнить backtest'
      setError(message)
      setResult(null)
    } finally {
      setLoading(false)
    }
  }

  const metrics = result
    ? [
        { label: 'Total Return', value: formatPercent(result.totalReturn) },
        { label: 'Max Drawdown', value: formatPercent(result.maxDrawdown) },
        { label: 'Profit Factor', value: result.profitFactor?.toString() ?? '—' },
        { label: 'Final Equity', value: formatMoney(result.finalEquity) },
        { label: 'Trades', value: result.trades ?? 0 },
      ]
    : []

  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        Backtest (SMA)
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
              inputProps={{ 'data-testid': 'backtest-figi' }}
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
              inputProps={{ 'data-testid': 'backtest-from' }}
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
              inputProps={{ 'data-testid': 'backtest-to' }}
            />
          </Grid>
          <Grid item xs={12} sm={6} md={2}>
            <TextField
              label="SMA Period"
              type="number"
              fullWidth
              required
              inputProps={{ min: 1, 'data-testid': 'backtest-sma-period' }}
              value={form.smaPeriod}
              onChange={handleChange('smaPeriod')}
            />
          </Grid>
          <Grid item xs={12} md={3}>
            <Button
              type="submit"
              variant="contained"
              fullWidth
              disabled={loading}
              data-testid="backtest-submit"
            >
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

      {result && (
        <>
          <TableContainer component={Paper} sx={{ mb: 3 }}>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Метрика</TableCell>
                  <TableCell align="right">Значение</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {metrics.map((row) => (
                  <TableRow key={row.label}>
                    <TableCell>{row.label}</TableCell>
                    <TableCell align="right">{row.value}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>

          <Paper sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom>
              Equity curve
            </Typography>
            {chartData.length > 0 ? (
              <ResponsiveContainer width="100%" height={360}>
                <LineChart data={chartData}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis
                    dataKey="time"
                    type="number"
                    domain={['dataMin', 'dataMax']}
                    tickFormatter={(ts) =>
                      new Date(ts).toLocaleDateString('ru-RU', { timeZone: 'UTC' })
                    }
                  />
                  <YAxis tickFormatter={(v) => v.toLocaleString('ru-RU')} />
                  <Tooltip
                    labelFormatter={(ts) =>
                      new Date(ts).toLocaleString('ru-RU', { timeZone: 'UTC' })
                    }
                    formatter={(value) => [formatMoney(value), 'Equity']}
                  />
                  <Legend />
                  <Line
                    type="monotone"
                    dataKey="equity"
                    name="Equity"
                    stroke="#8884d8"
                    dot={false}
                  />
                </LineChart>
              </ResponsiveContainer>
            ) : (
              <Typography color="textSecondary" align="center">
                Нет точек equity curve
              </Typography>
            )}
          </Paper>
        </>
      )}
    </Box>
  )
}

export default BacktestPage
