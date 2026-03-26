import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useCart } from '../hooks/useCart.js'
import { useAuth } from '../hooks/useAuth.js'
import { createOrder } from '../api/orderApi.js'

function CheckoutPage() {
  const { cartItems, cartTotal, clearCart } = useCart()
  const { user } = useAuth()
  const navigate = useNavigate()
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const handlePlaceOrder = async () => {
    if (cartItems.length === 0) return
    setLoading(true)
    setError('')
    try {
      const orderData = {
        items: cartItems.map((item) => ({
          productId: item.id,
          quantity: item.quantity,
        })),
      }
      const order = await createOrder(orderData)

      if (order.paymentUrl) {
        clearCart()
        window.location.href = order.paymentUrl
        return
      }

      clearCart()
      navigate(`/orders/${order.orderId}`)
    } catch (err) {
      const msg = err.response?.data?.message || err.response?.data || 'Failed to place order.'
      setError(typeof msg === 'string' ? msg : 'Order failed. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  if (cartItems.length === 0) {
    return (
      <div className="cart-empty" style={{ paddingTop: '6rem' }}>
        <div className="cart-empty-icon">📦</div>
        <h2>Nothing to Checkout</h2>
        <p>Add some items to your cart first.</p>
      </div>
    )
  }

  return (
    <>
      <div className="cart-header">
        <h1 className="cart-title">Checkout</h1>
      </div>
      <div className="checkout-layout">
        <div>
          <h3 style={{ fontFamily: "'Playfair Display', serif", fontSize: '1.1rem', color: 'var(--text-primary)', marginBottom: '1.2rem' }}>
            Order Items
          </h3>
          <div className="checkout-items-list">
            {cartItems.map((item) => (
              <div key={item.id} className="checkout-item">
                <div className="checkout-item-thumb">
                  {item.imageUrl ? (
                    <img src={item.imageUrl} alt={item.name} style={{ width: '100%', height: '100%', objectFit: 'cover', borderRadius: '8px' }} />
                  ) : (
                    <span>👗</span>
                  )}
                </div>
                <div className="checkout-item-info">
                  <div className="checkout-item-name">{item.name}</div>
                  <div className="checkout-item-qty">Qty: {item.quantity}</div>
                </div>
                <div className="checkout-item-price">
                  ₹{(item.price * item.quantity).toLocaleString('en-IN')}
                </div>
              </div>
            ))}
          </div>
        </div>

        <div className="cart-summary">
          <div className="cart-summary-title">Payment Summary</div>
          <div className="summary-row">
            <span className="summary-label">Items ({cartItems.length})</span>
            <span className="summary-value">₹{cartTotal.toLocaleString('en-IN')}</span>
          </div>
          <div className="summary-row">
            <span className="summary-label">Shipping</span>
            <span className="summary-value" style={{ color: 'var(--success)' }}>Free</span>
          </div>
          <div className="summary-row summary-total">
            <span className="summary-label">Total</span>
            <span className="summary-value">₹{cartTotal.toLocaleString('en-IN')}</span>
          </div>

          {error && (
            <div className="alert alert-error" style={{ marginTop: '1rem' }}>
              <span className="alert-icon">⚠</span>{error}
            </div>
          )}

          <button
            className="btn btn-primary btn-full"
            onClick={handlePlaceOrder}
            disabled={loading}
          >
            {loading ? (
              <span className="btn-loading"><span className="spinner" />Placing Order…</span>
            ) : (
              'Place Order & Pay'
            )}
          </button>
        </div>
      </div>
    </>
  )
}

export default CheckoutPage
