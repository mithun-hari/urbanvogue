import axios from 'axios'

const api = axios.create({
  baseURL: '/api/products',
  headers: { 'Content-Type': 'application/json' },
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('uv_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

export const getAllProducts = async () => {
  const res = await api.get('/')
  return res.data
}

export const getProductById = async (id) => {
  const res = await api.get(`/${id}`)
  return res.data
}

export const createProduct = async (product) => {
  const res = await api.post('/', product)
  return res.data
}

export const updateProduct = async (id, product) => {
  const res = await api.put(`/${id}`, product)
  return res.data
}

export const deleteProduct = async (id) => {
  await api.delete(`/${id}`)
}
