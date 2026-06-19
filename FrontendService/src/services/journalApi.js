import api from './api'

export async function getTrades() {
  const { data } = await api.get('/api/journal/trades')
  return data
}

export async function getPositions() {
  const { data } = await api.get('/api/journal/positions')
  return data
}
