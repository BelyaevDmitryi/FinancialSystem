import React, { useEffect, useState } from 'react'
import {
  Box,
  Paper,
  Typography,
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
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Chip,
  IconButton,
  Alert,
  CircularProgress,
} from '@mui/material'
import { PlayArrow, Stop, Delete } from '@mui/icons-material'
import api from '../services/api'
import { useAuth } from '../context/AuthContext'

const BotsPage = () => {
  const { user } = useAuth()
  const [bots, setBots] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [openDialog, setOpenDialog] = useState(false)
  const [formData, setFormData] = useState({
    name: '',
    strategy: 'MACD_CROSSOVER',
    ticker: '',
    maxPositionSize: '',
  })

  useEffect(() => {
    if (user?.id) {
      fetchBots()
    }
  }, [user])

  const fetchBots = async () => {
    try {
      setLoading(true)
      const response = await api.get('/api/bots')
      setBots(response.data)
      setError('')
    } catch (err) {
      setError('Не удалось загрузить ботов')
      console.error(err)
    } finally {
      setLoading(false)
    }
  }

  const handleCreateBot = async () => {
    try {
      // Валидация полей
      if (!formData.name || !formData.name.trim()) {
        setError('Введите название бота')
        return
      }
      if (!formData.ticker || !formData.ticker.trim()) {
        setError('Введите тикер акции')
        return
      }
      if (!formData.maxPositionSize || parseFloat(formData.maxPositionSize) <= 0) {
        setError('Введите корректный максимальный размер позиции (больше 0)')
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
      await api.post('/api/bots', {
        name: formData.name.trim(),
        strategy: formData.strategy,
        figi: figi,
        maxPositionSize: parseFloat(formData.maxPositionSize),
      })
      setOpenDialog(false)
      setFormData({ name: '', strategy: 'MACD_CROSSOVER', ticker: '', maxPositionSize: '' })
      fetchBots()
    } catch (err) {
      let errorMessage = 'Не удалось создать бота'
      if (err.response?.data?.message) {
        errorMessage = err.response.data.message
      } else if (err.response?.status === 404) {
        errorMessage = 'Акция с таким тикером не найдена'
      } else if (err.message) {
        errorMessage = err.message
      }
      setError(errorMessage)
      console.error(err)
    }
  }

  const handleToggleBot = async (botId, currentStatus) => {
    try {
      const newStatus = currentStatus === 'ACTIVE' ? 'STOPPED' : 'ACTIVE'
      await api.put(`/api/bots/${botId}/status`, null, {
        params: { status: newStatus },
      })
      fetchBots()
    } catch (err) {
      setError('Не удалось изменить статус бота')
      console.error(err)
    }
  }

  const handleDeleteBot = async (botId) => {
    if (window.confirm('Вы уверены, что хотите удалить этого бота?')) {
      try {
        await api.delete(`/api/bots/${botId}`)
        fetchBots()
      } catch (err) {
        setError('Не удалось удалить бота')
        console.error(err)
      }
    }
  }

  const getStatusColor = (status) => {
    switch (status) {
      case 'ACTIVE':
        return 'success'
      case 'STOPPED':
        return 'default'
      case 'ERROR':
        return 'error'
      default:
        return 'default'
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
        <Typography variant="h4">Торговые боты</Typography>
        <Button variant="contained" onClick={() => setOpenDialog(true)}>
          Создать бота
        </Button>
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
              <TableCell>Название</TableCell>
              <TableCell>Стратегия</TableCell>
              <TableCell>FIGI</TableCell>
              <TableCell>Макс. размер позиции</TableCell>
              <TableCell>Прибыль</TableCell>
              <TableCell>Сделок</TableCell>
              <TableCell>Статус</TableCell>
              <TableCell>Действия</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {bots.length === 0 ? (
              <TableRow>
                <TableCell colSpan={8} align="center">
                  <Typography color="textSecondary">
                    Нет ботов
                  </Typography>
                </TableCell>
              </TableRow>
            ) : (
              bots.map((bot) => (
                <TableRow key={bot.id}>
                  <TableCell>{bot.name}</TableCell>
                  <TableCell>{bot.strategy}</TableCell>
                  <TableCell>{bot.figi || '-'}</TableCell>
                  <TableCell>{bot.maxPositionSize ? bot.maxPositionSize.toFixed(2) : '-'}</TableCell>
                  <TableCell>{bot.totalProfit ? bot.totalProfit.toFixed(2) : '0.00'}</TableCell>
                  <TableCell>{bot.totalTrades || 0}</TableCell>
                  <TableCell>
                    <Chip
                      label={bot.status}
                      color={getStatusColor(bot.status)}
                      size="small"
                    />
                  </TableCell>
                  <TableCell>
                    <IconButton
                      onClick={() => handleToggleBot(bot.id, bot.status)}
                      color={bot.status === 'ACTIVE' ? 'error' : 'success'}
                    >
                      {bot.status === 'ACTIVE' ? <Stop /> : <PlayArrow />}
                    </IconButton>
                    <IconButton
                      onClick={() => handleDeleteBot(bot.id)}
                      color="error"
                    >
                      <Delete />
                    </IconButton>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </TableContainer>

      <Dialog open={openDialog} onClose={() => setOpenDialog(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Создать торгового бота</DialogTitle>
        <DialogContent>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mt: 1 }}>
            <TextField
              label="Название"
              value={formData.name}
              onChange={(e) => setFormData({ ...formData, name: e.target.value })}
              fullWidth
              required
            />
            <FormControl fullWidth>
              <InputLabel>Стратегия</InputLabel>
              <Select
                value={formData.strategy}
                onChange={(e) => setFormData({ ...formData, strategy: e.target.value })}
                label="Стратегия"
              >
                <MenuItem value="MACD_CROSSOVER">MACD Crossover</MenuItem>
                <MenuItem value="SMA_CROSSOVER">SMA Crossover</MenuItem>
                <MenuItem value="VOLATILITY_BREAKOUT">Volatility Breakout</MenuItem>
                <MenuItem value="EMA_TREND">EMA Trend</MenuItem>
              </Select>
            </FormControl>
            <TextField
              label="Тикер"
              value={formData.ticker}
              onChange={(e) => setFormData({ ...formData, ticker: e.target.value })}
              fullWidth
              required
            />
            <TextField
              label="Максимальный размер позиции"
              type="number"
              value={formData.maxPositionSize}
              onChange={(e) => setFormData({ ...formData, maxPositionSize: e.target.value })}
              fullWidth
              required
              inputProps={{ min: 0, step: 0.01 }}
              helperText="Максимальная сумма для одной позиции"
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenDialog(false)}>Отмена</Button>
          <Button onClick={handleCreateBot} variant="contained">
            Создать
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}

export default BotsPage
