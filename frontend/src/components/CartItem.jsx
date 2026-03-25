import { useCart } from '../hooks/useCart.js'

function CartItem({ item }) {
  const { updateQuantity, removeFromCart } = useCart()

  return (
    <div className="cart-item">
      <div className="cart-item-image">
        <span>👗</span>
      </div>
      <div className="cart-item-body">
        <div>
          <div className="cart-item-name">{item.name}</div>
          <div className="cart-item-price">₹{item.price?.toLocaleString('en-IN')} each</div>
        </div>
        <div className="cart-item-bottom">
          <div className="qty-controls">
            <button
              className="qty-btn"
              onClick={() => updateQuantity(item.id, item.quantity - 1)}
              disabled={item.quantity <= 1}
            >
              −
            </button>
            <span className="qty-value">{item.quantity}</span>
            <button
              className="qty-btn"
              onClick={() => updateQuantity(item.id, item.quantity + 1)}
            >
              +
            </button>
          </div>
          <div className="cart-item-total">
            ₹{(item.price * item.quantity).toLocaleString('en-IN')}
          </div>
          <button className="cart-remove-btn" onClick={() => removeFromCart(item.id)}>
            Remove
          </button>
        </div>
      </div>
    </div>
  )
}

export default CartItem
