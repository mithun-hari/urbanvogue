import { createContext, useState, useCallback, useMemo } from 'react'

export const CartContext = createContext(null)

const STORAGE_KEY = 'uv_cart'

const loadCart = () => {
  try {
    const data = localStorage.getItem(STORAGE_KEY)
    return data ? JSON.parse(data) : []
  } catch {
    return []
  }
}

const saveCart = (items) => {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(items))
}

export const CartProvider = ({ children }) => {
  const [cartItems, setCartItems] = useState(loadCart)

  const addToCart = useCallback((product, qty = 1) => {
    setCartItems((prev) => {
      const existing = prev.find((item) => item.id === product.id)
      let next
      if (existing) {
        next = prev.map((item) =>
          item.id === product.id
            ? { ...item, quantity: item.quantity + qty }
            : item
        )
      } else {
        next = [...prev, { ...product, quantity: qty }]
      }
      saveCart(next)
      return next
    })
  }, [])

  const removeFromCart = useCallback((productId) => {
    setCartItems((prev) => {
      const next = prev.filter((item) => item.id !== productId)
      saveCart(next)
      return next
    })
  }, [])

  const updateQuantity = useCallback((productId, quantity) => {
    if (quantity < 1) return
    setCartItems((prev) => {
      const next = prev.map((item) =>
        item.id === productId ? { ...item, quantity } : item
      )
      saveCart(next)
      return next
    })
  }, [])

  const clearCart = useCallback(() => {
    setCartItems([])
    localStorage.removeItem(STORAGE_KEY)
  }, [])

  const cartCount = cartItems.reduce((sum, item) => sum + item.quantity, 0)
  const cartTotal = cartItems.reduce(
    (sum, item) => sum + item.price * item.quantity,
    0
  )

  const value = useMemo(
    () => ({
      cartItems,
      addToCart,
      removeFromCart,
      updateQuantity,
      clearCart,
      cartCount,
      cartTotal,
    }),
    [cartItems, addToCart, removeFromCart, updateQuantity, clearCart, cartCount, cartTotal]
  )

  return <CartContext.Provider value={value}>{children}</CartContext.Provider>
}
