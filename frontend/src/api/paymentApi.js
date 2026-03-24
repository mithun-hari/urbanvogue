import axios from 'axios'

const api = axios.create({
  baseURL: '/payments',
  headers: { 'Content-Type': 'application/json' },
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('uv_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

export const processPayment = async (paymentData) => {
  const res = await api.post('/', paymentData)
  return res.data
}

export const getPaymentByOrder = async (orderId) => {
  const res = await api.get(`/order/${orderId}`)
  return res.data
}
