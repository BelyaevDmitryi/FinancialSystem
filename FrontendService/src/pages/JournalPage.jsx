import React, { useCallback, useEffect, useState } from 'react'
import {
  Alert,
  Box,
  CircularProgress,
  Paper,
  Tab,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Tabs,
  Typography,
} from '@mui/material'
import { getPositions, getTrades } from '../services/journalApi'

function formatMoney(value) {
  if (value == null || Number.isNaN(Number(value))) {
    return '—'
  }
  return Number(value).toLocaleString('ru-RU', {
    style: 'currency',
    currency: 'RUB',
    maximumFractionDigits: 4,
  })
}

function formatDateTime(value) {
  if (!value) {
    return '—'
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return String(value)
  }
  return date.toLocaleString('ru-RU')
}

const JournalPage = () => {
  const [tab, setTab] = useState(0)
  const [trades, setTrades] = useState([])
  const [positions, setPositions] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const loadData = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const [tradesData, positionsData] = await Promise.all([getTrades(), getPositions()])
      setTrades(Array.isArray(tradesData) ? tradesData : [])
      setPositions(Array.isArray(positionsData) ? positionsData : [])
    } catch (err) {
      setError('Не удалось загрузить journal')
      console.error(err)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    loadData()
  }, [loadData])

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    )
  }

  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        Journal
      </Typography>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}

      <Paper sx={{ mb: 2 }}>
        <Tabs value={tab} onChange={(_, value) => setTab(value)}>
          <Tab label={`Сделки (${trades.length})`} />
          <Tab label={`Позиции (${positions.length})`} />
        </Tabs>
      </Paper>

      {tab === 0 && (
        <TableContainer component={Paper}>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>ID</TableCell>
                <TableCell>FIGI</TableCell>
                <TableCell>Side</TableCell>
                <TableCell align="right">Qty</TableCell>
                <TableCell align="right">Price</TableCell>
                <TableCell align="right">Realized PnL</TableCell>
                <TableCell align="right">Commission</TableCell>
                <TableCell>Executed At</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {trades.length > 0 ? (
                trades.map((trade) => (
                  <TableRow key={trade.id}>
                    <TableCell>{trade.id}</TableCell>
                    <TableCell>{trade.figi}</TableCell>
                    <TableCell>{trade.side}</TableCell>
                    <TableCell align="right">{trade.quantity}</TableCell>
                    <TableCell align="right">{formatMoney(trade.price)}</TableCell>
                    <TableCell align="right">{formatMoney(trade.realizedPnl)}</TableCell>
                    <TableCell align="right">{formatMoney(trade.commission)}</TableCell>
                    <TableCell>{formatDateTime(trade.executedAt)}</TableCell>
                  </TableRow>
                ))
              ) : (
                <TableRow>
                  <TableCell colSpan={8} align="center">
                    Нет сделок
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </TableContainer>
      )}

      {tab === 1 && (
        <TableContainer component={Paper}>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>FIGI</TableCell>
                <TableCell align="right">Quantity</TableCell>
                <TableCell align="right">Avg Price</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {positions.length > 0 ? (
                positions.map((position) => (
                  <TableRow key={position.figi}>
                    <TableCell>{position.figi}</TableCell>
                    <TableCell align="right">{position.quantity}</TableCell>
                    <TableCell align="right">{formatMoney(position.avgPrice)}</TableCell>
                  </TableRow>
                ))
              ) : (
                <TableRow>
                  <TableCell colSpan={3} align="center">
                    Нет открытых позиций
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </TableContainer>
      )}
    </Box>
  )
}

export default JournalPage
