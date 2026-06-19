import React, { useMemo, useRef, useState } from 'react'
import {
  Box,
  Paper,
  Typography,
  TextField,
  Button,
  Grid,
  Stack,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  Alert,
} from '@mui/material'
import html2canvas from 'html2canvas'
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from 'recharts'
import api from '../services/api'

const SERIES_COLORS = ['#8884d8', '#82ca9d', '#ff7300', '#0088fe', '#ff69b4', '#00c49f']
const PRICE_SERIES_KEY = 'price'

/** Единый ключ времени: ISO UTC, чтобы слияние серий не плодило дубликаты из-за разного формата строк. */
function toUtcIsoTimestamp(value) {
  const ms = new Date(value).getTime()
  if (Number.isNaN(ms)) {
    return String(value)
  }
  return new Date(ms).toISOString()
}

const AnalyticsPage = () => {
  const [formData, setFormData] = useState({
    figi: '',
    period: 20,
    indicator: 'SMA',
  })
  const [seriesMeta, setSeriesMeta] = useState([])
  const [lastResult, setLastResult] = useState(null)
  const [loading, setLoading] = useState(false)
  const [exporting, setExporting] = useState(false)
  const [error, setError] = useState('')
  const [chartRows, setChartRows] = useState([])
  const chartRef = useRef(null)

  const buildTimeRange = () => {
    const to = new Date()
    const from = new Date(to)
    from.setDate(from.getDate() - 547)
    return { from: from.toISOString(), to: to.toISOString() }
  }

  const formatTickDateTimeUtc = (utcMs) => {
    const date = new Date(utcMs)
    return Number.isNaN(date.getTime()) ? String(utcMs) : `${date.toLocaleString('ru-RU', { timeZone: 'UTC' })} UTC`
  }

  const tooltipLabelFormatter = (utcMs) => formatTickDateTimeUtc(utcMs)

  const tooltipFormatter = (value, name) => [typeof value === 'number' ? value.toFixed(4) : value, name]

  const fetchPriceData = async (figi) => {
    const { from, to } = buildTimeRange()
    const candlesResponse = await api.get('/api/market-history/candles', {
      params: {
        figi,
        from,
        to,
        interval: 'DAY',
      },
    })
    const candles = candlesResponse?.data?.candles ?? []
    return candles
      .filter((candle) => candle?.time != null && candle?.close != null)
      .map((candle) => {
        const iso = toUtcIsoTimestamp(candle.time)
        return {
          figi,
          price: typeof candle.close === 'string' ? Number.parseFloat(candle.close) : Number(candle.close),
          timestamp: iso,
        }
      })
      .filter((point) => Number.isFinite(point.price))
      .sort((a, b) => Date.parse(a.timestamp) - Date.parse(b.timestamp))
  }

  const mergeSeriesToRows = (baseRows, priceData, indicatorValues, period, indicator, seriesKey) => {
    const rowsByTime = new Map(
      baseRows.map((row) => {
        const t = toUtcIsoTimestamp(row.timestamp)
        return [t, { ...row, timestamp: t }]
      })
    )

    priceData.forEach((point) => {
      const t = point.timestamp
      const current = rowsByTime.get(t) ?? {
        timestamp: t,
        [PRICE_SERIES_KEY]: null,
      }
      current[PRICE_SERIES_KEY] = point.price
      rowsByTime.set(t, current)
    })

    const startIndex = indicator === 'SMA' ? Math.max(period - 1, 0) : 0
    indicatorValues.forEach((value, idx) => {
      const pointIndex = startIndex + idx
      if (pointIndex >= priceData.length) {
        return
      }
      const t = priceData[pointIndex].timestamp
      const current = rowsByTime.get(t) ?? {
        timestamp: t,
        [PRICE_SERIES_KEY]: null,
      }
      const num = typeof value === 'number' ? value : Number.parseFloat(value)
      current[seriesKey] = Number.isFinite(num) ? num : null
      rowsByTime.set(t, current)
    })

    return Array.from(rowsByTime.values()).sort((a, b) => Date.parse(a.timestamp) - Date.parse(b.timestamp))
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setLoading(true)
    setError('')

    try {
      const priceData = await fetchPriceData(formData.figi)
      if (priceData.length < formData.period) {
        throw new Error(`Недостаточно исторических данных для периода ${formData.period}`)
      }

      const endpoint = formData.indicator === 'SMA' ? '/api/analytics/sma' : '/api/analytics/ema'
      const response = await api.post(endpoint, {
        figi: formData.figi,
        period: formData.period,
        priceData,
      })

      const values = response?.data?.values ?? []
      if (values.length === 0) {
        throw new Error('Сервис аналитики не вернул значения для построения графика')
      }

      const seriesKey = `${formData.indicator}_${formData.period}`
      const label = `${formData.indicator}(${formData.period})`
      setChartRows((prevRows) =>
        mergeSeriesToRows(prevRows, priceData, values, formData.period, formData.indicator, seriesKey)
      )
      setSeriesMeta((prev) => {
        const withoutCurrent = prev.filter((series) => series.key !== seriesKey)
        const color = withoutCurrent.find((series) => series.key === seriesKey)?.color
          ?? SERIES_COLORS[withoutCurrent.length % SERIES_COLORS.length]
        return [...withoutCurrent, { key: seriesKey, label, color }]
      })
      setLastResult({
        indicator: formData.indicator,
        period: formData.period,
        value: Number(response?.data?.sma ?? response?.data?.ema),
      })
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || 'Не удалось получить аналитику')
      console.error(err)
    } finally {
      setLoading(false)
    }
  }

  const handleClearChart = () => {
    setChartRows([])
    setSeriesMeta([])
    setLastResult(null)
    setError('')
  }

  const handleSavePng = async () => {
    if (!chartRef.current) {
      return
    }
    setExporting(true)
    try {
      const canvas = await html2canvas(chartRef.current, {
        backgroundColor: '#ffffff',
        scale: 2,
      })
      const link = document.createElement('a')
      link.download = `analytics-${formData.figi || 'chart'}-${Date.now()}.png`
      link.href = canvas.toDataURL('image/png')
      link.click()
    } catch (err) {
      setError('Не удалось сохранить график в PNG')
      console.error(err)
    } finally {
      setExporting(false)
    }
  }

  const hasChartData = chartRows.length > 0
  const composedChartData = useMemo(
    () =>
      chartRows.map((row) => {
        const utcMs = Date.parse(row.timestamp)
        return {
          ...row,
          timeUtcMs: Number.isNaN(utcMs) ? 0 : utcMs,
        }
      }),
    [chartRows]
  )

  return (
    <Box sx={{ width: '100%', minWidth: 0 }}>
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
              <Stack spacing={1}>
                <Button type="submit" variant="contained" fullWidth sx={{ height: '56px' }} disabled={loading}>
                  {loading ? 'Загрузка...' : 'Добавить на график'}
                </Button>
                <Button variant="outlined" color="secondary" onClick={handleClearChart} fullWidth>
                  Очистить
                </Button>
              </Stack>
            </Grid>
          </Grid>
        </form>
      </Paper>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError('')}>
          {error}
        </Alert>
      )}

      <Paper sx={{ p: 3 }} ref={chartRef}>
        {lastResult && (
          <>
            <Typography variant="h6" gutterBottom>
              Последний расчёт: {lastResult.indicator}
            </Typography>
            {Number.isFinite(lastResult.value) && (
              <Typography variant="body1" sx={{ mb: 2 }}>
                {lastResult.indicator}({lastResult.period}) = {lastResult.value.toFixed(2)}
              </Typography>
            )}
          </>
        )}
        <Box sx={{ display: 'flex', justifyContent: 'flex-end', mb: 2 }}>
          <Button variant="outlined" onClick={handleSavePng} disabled={!hasChartData || exporting}>
            {exporting ? 'Сохранение...' : 'Сохранить PNG'}
          </Button>
        </Box>
        {hasChartData ? (
          /*
           * Числовая высота у ResponsiveContainer (как на дашборде): процент высоты внутри flex-лейаута MUI часто даёт 0 — график пустой.
           * Ось X — числовая шкала Unix-ms без scale time (стабильнее в Recharts 2.x для type="number").
           */
          <Box sx={{ width: '100%', minWidth: 0 }}>
            <ResponsiveContainer width="100%" height={420}>
              <LineChart data={composedChartData} margin={{ top: 8, right: 24, left: 8, bottom: 40 }}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis
                type="number"
                dataKey="timeUtcMs"
                domain={['dataMin', 'dataMax']}
                tickFormatter={formatTickDateTimeUtc}
                minTickGap={28}
                label={{
                  value: 'Дата и время (ось X)',
                  position: 'bottom',
                  offset: 26,
                  style: { textAnchor: 'middle', fill: '#666' },
                }}
              />
              <YAxis
                tickFormatter={(v) => (Number.isFinite(v) ? v.toFixed(2) : v)}
                width={72}
                label={{
                  value: 'Цена (ось Y)',
                  angle: -90,
                  position: 'insideLeft',
                  style: { textAnchor: 'middle', fill: '#666' },
                }}
              />
              <Tooltip labelFormatter={tooltipLabelFormatter} formatter={tooltipFormatter} />
              <Legend />
              <Line
                type="monotone"
                dataKey={PRICE_SERIES_KEY}
                stroke="#222222"
                name="Цена (close)"
                dot={false}
                strokeWidth={2}
              />
              {seriesMeta.map((series) => (
                <Line
                  key={series.key}
                  type="monotone"
                  connectNulls
                  dataKey={series.key}
                  stroke={series.color}
                  name={series.label}
                  dot={false}
                />
              ))}
              </LineChart>
            </ResponsiveContainer>
          </Box>
        ) : (
          <Typography color="textSecondary" align="center" sx={{ mt: 4 }}>
            Нет данных для графика
          </Typography>
        )}
      </Paper>
    </Box>
  )
}

export default AnalyticsPage
