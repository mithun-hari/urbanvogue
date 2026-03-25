import { useNavigate } from 'react-router-dom'
import { useCart } from '../hooks/useCart.js'

const FASHION_ICONS = ['👗', '👔', '👟', '👜', '🧥', '👕', '🩳', '👒', '🧣', '🕶️']

function ProductCard({ product, index = 0 }) {
  const navigate = useNavigate()
  const { addToCart } = useCart()

  const icon = FASHION_ICONS[product.id ? product.id % FASHION_ICONS.length : index % FASHION_ICONS.length]

  const handleAdd = (e) => {
    e.stopPropagation()
    addToCart(product, 1)
  }

  return (
    <div
      className="product-card"
      onClick={() => navigate(`/products/${product.id}`)}
      style={{ animationDelay: `${index * 0.06}s` }}
    >
      <div className="product-image-box">
        <span className="product-image-icon">{icon}</span>
        <span className="product-image-label">UrbanVogue</span>
      </div>
      <div className="product-info">
        <div className="product-name">{product.name}</div>
        <div className="product-desc">{product.description}</div>
        <div className="product-footer">
          <span className="product-price">₹{product.price?.toLocaleString('en-IN')}</span>
          <button
            className="product-add-btn"
            onClick={handleAdd}
            aria-label="Add to cart"
            title="Add to cart"
          >
            +
          </button>
        </div>
      </div>
    </div>
  )
}

export default ProductCard
