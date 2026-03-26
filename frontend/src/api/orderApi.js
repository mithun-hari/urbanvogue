import axios from 'axios'

const api = axios.create({
  baseURL: '/orders',
  headers: { 'Content-Type': 'application/json' },
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('uv_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

export const createOrder = async (orderData) => {
  const res = await api.post('', orderData)
  return res.data
}

export const getOrderById = async (id) => {
  const res = await api.get(`/${id}`)
  return res.data
}

export const getOrdersByUser = async (userId) => {
  const res = await api.get(`/user/${userId}`)
  return res.data
}
