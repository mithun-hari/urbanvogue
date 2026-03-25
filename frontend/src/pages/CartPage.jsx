import { Link } from 'react-router-dom'
import { useCart } from '../hooks/useCart.js'
import { useAuth } from '../hooks/useAuth.js'
import CartItem from '../components/CartItem.jsx'

function CartPage() {
  const { cartItems, cartTotal, cartCount } = useCart()
  const { isAuthenticated } = useAuth()

  if (cartItems.length === 0) {
    return (
      <div className="cart-empty" style={{ paddingTop: '6rem' }}>
        <div className="cart-empty-icon">🛒</div>
        <h2>Your Cart is Empty</h2>
        <p>Discover our collection and add your favorites.</p>
        <Link to="/products" className="btn btn-primary" style={{ marginTop: '1rem' }}>
          Start Shopping
        </Link>
      </div>
    )
  }

  return (
    <>
      <div className="cart-header">
        <h1 className="cart-title">
          Shopping Cart
          <span className="cart-count">({cartCount} {cartCount === 1 ? 'item' : 'items'})</span>
        </h1>
      </div>
      <div className="cart-layout">
        <div className="cart-items">
          {cartItems.map((item) => (
            <CartItem key={item.id} item={item} />
          ))}
        </div>

        <div className="cart-summary">
          <div className="cart-summary-title">Order Summary</div>
          <div className="summary-row">
            <span className="summary-label">Subtotal</span>
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
          {isAuthenticated ? (
            <Link to="/checkout" className="btn btn-primary btn-full">
              Proceed to Checkout
            </Link>
          ) : (
            <Link to="/login" className="btn btn-primary btn-full">
              Sign In to Checkout
            </Link>
          )}
          <Link
            to="/products"
            className="btn btn-ghost btn-full"
            style={{ marginTop: '0.5rem' }}
          >
            ← Continue Shopping
          </Link>
        </div>
      </div>
    </>
  )
}

export default CartPage
