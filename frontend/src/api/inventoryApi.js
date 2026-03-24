import axios from 'axios'

const api = axios.create({
  baseURL: '/api/inventory',
  headers: { 'Content-Type': 'application/json' },
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('uv_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

export const getInventory = async (productId) => {
  const res = await api.get(`/${productId}`)
  return res.data
}
