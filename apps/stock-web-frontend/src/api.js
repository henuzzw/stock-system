import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 15000
})

export const getTechnicalTop = (date) => api.get('/ranks/technical', { params: { date } })
export const getValuationTop = (date) => api.get('/ranks/valuation', { params: { date } })

export const getCandidates = (date, side, opts = {}) =>
  api.get('/candidates', {
    params: {
      date,
      side,
      triggered: opts.triggered ? 1 : undefined,
      intersection: opts.intersection ? 1 : undefined,
      sort: opts.sort || 'rank',
      limit: opts.limit || 20
    }
  })

export const getSymbols = (q, limit = 200) => api.get('/symbols', { params: { q, limit } })
export const addSymbol = (payload) => api.post('/symbols', payload)
export const getSymbolDetail = (code, days = 180) => api.get(`/symbol/${code}`, { params: { days } })

export default api
