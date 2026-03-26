import { useNavigate } from 'react-router-dom'
import { useCart } from '../hooks/useCart.js'

function ProductCard({ product, index = 0 }) {
  const navigate = useNavigate()
  const { addToCart } = useCart()

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
        {product.imageUrl ? (
          <img
            src={product.imageUrl}
            alt={product.name}
            style={{
              width: '100%',
              height: '100%',
              objectFit: 'cover',
              borderRadius: 'inherit',
            }}
            onError={(e) => {
              e.target.style.display = 'none'
              e.target.nextSibling.style.display = 'flex'
            }}
          />
        ) : null}
        <div
          className="product-image-placeholder"
          style={{
            display: product.imageUrl ? 'none' : 'flex',
            width: '100%',
            height: '100%',
            alignItems: 'center',
            justifyContent: 'center',
            flexDirection: 'column',
            gap: '0.5rem',
            background: 'linear-gradient(135deg, rgba(212,175,55,0.15), rgba(212,175,55,0.05))',
            borderRadius: 'inherit',
          }}
        >
          <span style={{ fontSize: '2.5rem', opacity: 0.6 }}>🛍️</span>
          <span className="product-image-label">UrbanVogue</span>
        </div>
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
