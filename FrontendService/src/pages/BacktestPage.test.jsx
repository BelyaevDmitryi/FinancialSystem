import React from 'react'
import { render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import BacktestPage from './BacktestPage'

vi.mock('../services/backtestApi', () => ({
  runBacktest: vi.fn(),
}))

describe('BacktestPage', () => {
  it('renders backtest form fields', () => {
    render(<BacktestPage />)

    expect(screen.getByTestId('backtest-figi')).toBeInTheDocument()
    expect(screen.getByTestId('backtest-from')).toBeInTheDocument()
    expect(screen.getByTestId('backtest-to')).toBeInTheDocument()
    expect(screen.getByTestId('backtest-sma-period')).toBeInTheDocument()
    expect(screen.getByTestId('backtest-submit')).toBeInTheDocument()
    expect(screen.getByDisplayValue('BBG004730N88')).toBeInTheDocument()
    expect(screen.getByDisplayValue('2026-01-01')).toBeInTheDocument()
    expect(screen.getByDisplayValue('2026-03-01')).toBeInTheDocument()
  })
})
