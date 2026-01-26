import React, { useEffect, useState } from 'react'
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
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  Alert,
  Chip,
  CircularProgress,
} from '@mui/material'
import api from '../services/api'
import { useAuth } from '../context/AuthContext'
import OrderBook from '../components/OrderBook'

const TradingPage = () => {
  const { user } = useAuth()
  const [orders, setOrders] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [openDialog, setOpenDialog] = useState(false)
  const [openOrderBookDialog, setOpenOrderBookDialog] = useState(false)
  const [orderBookFigi, setOrderBookFigi] = useState('')
  const [orderBookTicker, setOrderBookTicker] = useState('')
  const [formData, setFormData] = useState({
    ticker: '',
    quantity: '',
    orderType: 'BUY',
    price: '',
  })

  useEffect(() => {
    if (user?.id) {
      fetchOrders()
    }
  }, [user])

  const fetchOrders = async () => {
    try {
      setLoading(true)
      const response = await api.get('/api/orders')
      setOrders(response.data)
      setError('')
    } catch (err) {
      const errorMessage = err.response?.data?.message || 
                          err.response?.data?.error || 
                          'Не удалось загрузить ордера'
      setError(errorMessage)
      console.error('Ошибка при загрузке ордеров:', err)
    } finally {
      setLoading(false)
    }
  }

  const handleCreateOrder = async () => {
    try {
      // Валидация полей
      if (!formData.ticker || !formData.ticker.trim()) {
        setError('Введите тикер акции')
        return
      }
      if (!formData.quantity || parseFloat(formData.quantity) <= 0) {
        setError('Введите корректное количество (больше 0)')
        return
      }
      if (!formData.price || parseFloat(formData.price) <= 0) {
        setError('Введите корректную цену (больше 0)')
        return
      }

      setError('') // Очищаем предыдущие ошибки
      
      // Получаем figi по ticker
      const stockResponse = await api.get(`/api/stocks/${formData.ticker.trim().toUpperCase()}`)
      const figi = stockResponse.data?.figi
      
      if (!figi) {
        setError('Не удалось найти акцию с таким тикером')
        return
      }

      // Отправляем запрос с правильными полями
      await api.post('/api/orders', {
        figi: figi,
        type: formData.orderType, // BUY или SELL
        quantity: parseFloat(formData.quantity),
        price: parseFloat(formData.price),
      })
      setOpenDialog(false)
      setFormData({ ticker: '', quantity: '', orderType: 'BUY', price: '' })
      setError('')
      fetchOrders()
    } catch (err) {
      let errorMessage = 'Не удалось создать ордер'
      
      if (err.response?.status === 404) {
        errorMessage = 'Акция с таким тикером не найдена'
      } else if (err.response?.data?.message) {
        errorMessage = err.response.data.message
      } else if (err.response?.data?.error) {
        errorMessage = err.response.data.error
      } else if (err.message) {
        errorMessage = err.message
      }
      
      setError(errorMessage)
      console.error('Ошибка при создании ордера:', err)
    }
  }

  const getStatusColor = (status) => {
    switch (status) {
      case 'EXECUTED':
        return 'success'
      case 'PENDING':
        return 'warning'
      case 'CANCELLED':
        return 'default'
      case 'REJECTED':
        return 'error'
      default:
        return 'default'
    }
  }
  
  const getStatusLabel = (status) => {
    switch (status) {
      case 'EXECUTED':
        return 'Исполнен'
      case 'PENDING':
        return 'В ожидании'
      case 'CANCELLED':
        return 'Отменен'
      case 'REJECTED':
        return 'Отклонен'
      default:
        return status
    }
  }

  const handleOpenOrderBook = async () => {
    const ticker = prompt('Введите тикер акции для просмотра стакана:')
    if (!ticker || !ticker.trim()) {
      return
    }

    try {
      setError('')
      const stockResponse = await api.get(`/api/stocks/${ticker.trim().toUpperCase()}`)
      const figi = stockResponse.data?.figi
      
      if (!figi) {
        setError('Не удалось найти акцию с таким тикером')
        return
      }

      setOrderBookFigi(figi)
      setOrderBookTicker(ticker.trim().toUpperCase())
      setOpenOrderBookDialog(true)
    } catch (err) {
      let errorMessage = 'Не удалось загрузить информацию об акции'
      if (err.response?.status === 404) {
        errorMessage = 'Акция с таким тикером не найдена'
      } else if (err.response?.data?.message) {
        errorMessage = err.response.data.message
      }
      setError(errorMessage)
      console.error('Ошибка при загрузке акции:', err)
    }
  }

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    )
  }

  return (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4">Торговый терминал</Typography>
        <Box display="flex" gap={2}>
          <Button variant="outlined" onClick={handleOpenOrderBook}>
            Показать стакан
          </Button>
          <Button variant="contained" onClick={() => setOpenDialog(true)}>
            Создать ордер
          </Button>
        </Box>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError('')}>
          {error}
        </Alert>
      )}

      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Тикер</TableCell>
              <TableCell>Тип</TableCell>
              <TableCell>Количество</TableCell>
              <TableCell>Цена</TableCell>
              <TableCell>Статус</TableCell>
              <TableCell>Дата создания</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {orders.length === 0 ? (
              <TableRow>
                <TableCell colSpan={6} align="center">
                  <Typography color="textSecondary">
                    Нет ордеров
                  </Typography>
                </TableCell>
              </TableRow>
            ) : (
              orders.map((order) => (
                <TableRow key={order.id}>
                  <TableCell>{order.figi || 'N/A'}</TableCell>
                  <TableCell>{order.type === 'BUY' ? 'Покупка' : 'Продажа'}</TableCell>
                  <TableCell>{order.quantity}</TableCell>
                  <TableCell>{order.price?.toFixed(2)}</TableCell>
                  <TableCell>
                    <Chip
                      label={getStatusLabel(order.status)}
                      color={getStatusColor(order.status)}
                      size="small"
                    />
                  </TableCell>
                  <TableCell>
                    {order.createdAt ? new Date(order.createdAt).toLocaleString('ru-RU') : 'N/A'}
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </TableContainer>

      <Dialog open={openDialog} onClose={() => setOpenDialog(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Создать ордер</DialogTitle>
        <DialogContent>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mt: 1 }}>
            <TextField
              label="Тикер"
              value={formData.ticker}
              onChange={(e) => setFormData({ ...formData, ticker: e.target.value })}
              fullWidth
              required
            />
            <FormControl fullWidth>
              <InputLabel>Тип ордера</InputLabel>
              <Select
                value={formData.orderType}
                onChange={(e) => setFormData({ ...formData, orderType: e.target.value })}
                label="Тип ордера"
              >
                <MenuItem value="BUY">Покупка</MenuItem>
                <MenuItem value="SELL">Продажа</MenuItem>
              </Select>
            </FormControl>
            <TextField
              label="Количество"
              type="number"
              value={formData.quantity}
              onChange={(e) => setFormData({ ...formData, quantity: e.target.value })}
              fullWidth
              required
            />
            <TextField
              label="Цена"
              type="number"
              value={formData.price}
              onChange={(e) => setFormData({ ...formData, price: e.target.value })}
              fullWidth
              required
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenDialog(false)}>Отмена</Button>
          <Button onClick={handleCreateOrder} variant="contained">
            Создать
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog 
        open={openOrderBookDialog} 
        onClose={() => setOpenOrderBookDialog(false)} 
        maxWidth="lg" 
        fullWidth
      >
        <DialogTitle>
          Стакан заявок - {orderBookTicker}
        </DialogTitle>
        <DialogContent>
          <OrderBook 
            figi={orderBookFigi} 
            onClose={() => setOpenOrderBookDialog(false)} 
          />
        </DialogContent>
      </Dialog>
    </Box>
  )
}

export default TradingPage
