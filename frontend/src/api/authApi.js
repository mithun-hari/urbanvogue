import axios from 'axios'

const api = axios.create({
  baseURL: '/api/auth',
  headers: { 'Content-Type': 'application/json' },
})

export const registerUser = async (username, email, password) => {
  const res = await api.post('/register', { username, email, password })
  return res.data
}

export const loginUser = async (email, password) => {
  const res = await api.post('/login', { email, password })
  return res.data
}
