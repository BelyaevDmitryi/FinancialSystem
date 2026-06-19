import api from './api'

/**
 * Запуск SMA backtest через BotOptimizationService (Gateway /api/backtest/run).
 */
export async function runBacktest(payload) {
  const { data } = await api.post('/api/backtest/run', payload)
  return data
}
