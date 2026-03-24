import { createContext, useState, useCallback, useMemo } from 'react'

export const AuthContext = createContext(null)

const decodeJwt = (token) => {
  try {
    const base64Url = token.split('.')[1]
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/')
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    )
    return JSON.parse(jsonPayload)
  } catch {
    return null
  }
}

export const AuthProvider = ({ children }) => {
  const [token, setToken] = useState(() => localStorage.getItem('uv_token'))
  const [user, setUser] = useState(() => {
    const stored = localStorage.getItem('uv_token')
    return stored ? decodeJwt(stored) : null
  })

  const login = useCallback((jwt) => {
    localStorage.setItem('uv_token', jwt)
    setToken(jwt)
    setUser(decodeJwt(jwt))
  }, [])

  const logout = useCallback(() => {
    localStorage.removeItem('uv_token')
    setToken(null)
    setUser(null)
  }, [])

  const isAuthenticated = Boolean(token)

  const value = useMemo(
    () => ({ token, user, login, logout, isAuthenticated }),
    [token, user, login, logout, isAuthenticated]
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
