import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { App } from './App'

describe('Rightful Frontend Application', () => {
  it('renders application brand and platform overview', () => {
    render(<App />)
    expect(screen.getByText('Rightful')).toBeInTheDocument()
    expect(screen.getByText('Platform Overview')).toBeInTheDocument()
    expect(screen.getByText('Modular Monolith')).toBeInTheDocument()
  })
})

