import api from './api'

/**
 * Grid-оптимизация параметров стратегии (Gateway /api/bot-optimization/grid).
 */
export async function runGridOptimization(payload) {
  const { data } = await api.post('/api/bot-optimization/grid', payload)
  return data
}
