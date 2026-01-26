import React, { useEffect, useState, useCallback } from 'react'
import {
  Box,
  Paper,
  Typography,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Button,
  Alert,
  CircularProgress,
  Grid,
} from '@mui/material'
import api from '../services/api'

const OrderBook = ({ figi, onClose }) => {
  const [orderBook, setOrderBook] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [depth, setDepth] = useState(20)
  const [autoRefresh, setAutoRefresh] = useState(false)

  const fetchOrderBook = useCallback(async () => {
    if (!figi) return
    
    try {
      setLoading(true)
      setError('')
      const response = await api.get(`/api/broker/orderbook/${figi}`, {
        params: { depth }
      })
      setOrderBook(response.data)
    } catch (err) {
      const errorMessage = err.response?.data?.message || 
                          err.response?.data?.error || 
                          'Не удалось загрузить стакан'
      setError(errorMessage)
      console.error('Ошибка при загрузке стакана:', err)
    } finally {
      setLoading(false)
    }
  }, [figi, depth])

  useEffect(() => {
    fetchOrderBook()
  }, [fetchOrderBook])

  useEffect(() => {
    let interval = null
    if (autoRefresh) {
      interval = setInterval(() => {
        fetchOrderBook()
      }, 2000) // Обновление каждые 2 секунды
    }
    return () => {
      if (interval) clearInterval(interval)
    }
  }, [autoRefresh, fetchOrderBook])

  const formatPrice = (price) => {
    if (!price) return '-'
    return price.toFixed(2)
  }

  const formatQuantity = (quantity) => {
    if (!quantity) return '-'
    return quantity.toLocaleString('ru-RU')
  }

  if (loading && !orderBook) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    )
  }

  return (
    <Paper sx={{ p: 3 }}>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
        <Typography variant="h6">Стакан заявок</Typography>
        {onClose && (
          <Button onClick={onClose} size="small">
            Закрыть
          </Button>
        )}
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError('')}>
          {error}
        </Alert>
      )}

      <Grid container spacing={2} sx={{ mb: 2 }}>
        <Grid item xs={6}>
          <TextField
            label="Глубина стакана"
            type="number"
            value={depth}
            onChange={(e) => setDepth(Math.max(1, Math.min(50, parseInt(e.target.value) || 20)))}
            size="small"
            inputProps={{ min: 1, max: 50 }}
            fullWidth
          />
        </Grid>
        <Grid item xs={6}>
          <Button
            variant={autoRefresh ? "contained" : "outlined"}
            onClick={() => setAutoRefresh(!autoRefresh)}
            fullWidth
            sx={{ height: '40px' }}
          >
            {autoRefresh ? 'Остановить автообновление' : 'Автообновление'}
          </Button>
        </Grid>
      </Grid>

      {orderBook && (
        <>
          {orderBook.lastPrice && (
            <Box sx={{ mb: 2, textAlign: 'center' }}>
              <Typography variant="h6" color="primary">
                Последняя цена: {formatPrice(orderBook.lastPrice)}
              </Typography>
            </Box>
          )}

          <Grid container spacing={2}>
            {/* Заявки на продажу (Asks) */}
            <Grid item xs={12} md={6}>
              <Typography variant="subtitle1" color="error" sx={{ mb: 1, fontWeight: 'bold' }}>
                Продажа (Asks)
              </Typography>
              <TableContainer sx={{ maxHeight: 400 }}>
                <Table size="small" stickyHeader>
                  <TableHead>
                    <TableRow>
                      <TableCell align="right">Цена</TableCell>
                      <TableCell align="right">Количество</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {orderBook.asks && orderBook.asks.length > 0 ? (
                      orderBook.asks.map((ask, index) => (
                        <TableRow key={index} sx={{ backgroundColor: 'rgba(244, 67, 54, 0.05)' }}>
                          <TableCell align="right" sx={{ color: 'error.main', fontWeight: 'bold' }}>
                            {formatPrice(ask.price)}
                          </TableCell>
                          <TableCell align="right">
                            {formatQuantity(ask.quantity)}
                          </TableCell>
                        </TableRow>
                      ))
                    ) : (
                      <TableRow>
                        <TableCell colSpan={2} align="center">
                          <Typography color="textSecondary">Нет заявок</Typography>
                        </TableCell>
                      </TableRow>
                    )}
                  </TableBody>
                </Table>
              </TableContainer>
            </Grid>

            {/* Заявки на покупку (Bids) */}
            <Grid item xs={12} md={6}>
              <Typography variant="subtitle1" color="success.main" sx={{ mb: 1, fontWeight: 'bold' }}>
                Покупка (Bids)
              </Typography>
              <TableContainer sx={{ maxHeight: 400 }}>
                <Table size="small" stickyHeader>
                  <TableHead>
                    <TableRow>
                      <TableCell align="right">Цена</TableCell>
                      <TableCell align="right">Количество</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {orderBook.bids && orderBook.bids.length > 0 ? (
                      orderBook.bids.map((bid, index) => (
                        <TableRow key={index} sx={{ backgroundColor: 'rgba(76, 175, 80, 0.05)' }}>
                          <TableCell align="right" sx={{ color: 'success.main', fontWeight: 'bold' }}>
                            {formatPrice(bid.price)}
                          </TableCell>
                          <TableCell align="right">
                            {formatQuantity(bid.quantity)}
                          </TableCell>
                        </TableRow>
                      ))
                    ) : (
                      <TableRow>
                        <TableCell colSpan={2} align="center">
                          <Typography color="textSecondary">Нет заявок</Typography>
                        </TableCell>
                      </TableRow>
                    )}
                  </TableBody>
                </Table>
              </TableContainer>
            </Grid>
          </Grid>

          {orderBook.timestamp && (
            <Typography variant="caption" color="textSecondary" sx={{ mt: 2, display: 'block' }}>
              Обновлено: {new Date(orderBook.timestamp).toLocaleString('ru-RU')}
            </Typography>
          )}
        </>
      )}

      <Box sx={{ mt: 2, display: 'flex', justifyContent: 'flex-end' }}>
        <Button onClick={fetchOrderBook} variant="outlined" size="small">
          Обновить
        </Button>
      </Box>
    </Paper>
  )
}

export default OrderBook
