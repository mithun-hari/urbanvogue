import { useState, useEffect } from 'react'
import { useParams, Link } from 'react-router-dom'
import { getProductById } from '../api/productApi.js'
import { useCart } from '../hooks/useCart.js'

const FASHION_ICONS = ['👗', '👔', '👟', '👜', '🧥', '👕', '🩳', '👒', '🧣', '🕶️']

function ProductDetailPage() {
  const { id } = useParams()
  const [product, setProduct] = useState(null)
  const [loading, setLoading] = useState(true)
  const [qty, setQty] = useState(1)
  const [added, setAdded] = useState(false)
  const { addToCart } = useCart()

  useEffect(() => {
    setLoading(true)
    getProductById(id)
      .then(setProduct)
      .catch(() => setProduct(null))
      .finally(() => setLoading(false))
  }, [id])

  const handleAdd = () => {
    if (product) {
      addToCart(product, qty)
      setAdded(true)
      setTimeout(() => setAdded(false), 2000)
    }
  }

  if (loading) {
    return (
      <div className="page-loading" style={{ minHeight: 'calc(100vh - 72px)' }}>
        <div className="page-spinner" />
        <p>Loading product…</p>
      </div>
    )
  }

  if (!product) {
    return (
      <div className="cart-empty" style={{ paddingTop: '6rem' }}>
        <div className="cart-empty-icon">😕</div>
        <h2>Product Not Found</h2>
        <p>The product you're looking for doesn't exist.</p>
        <Link to="/products" className="btn btn-primary" style={{ marginTop: '1rem' }}>Back to Shop</Link>
      </div>
    )
  }

  const icon = FASHION_ICONS[product.id % FASHION_ICONS.length]

  return (
    <div className="product-detail">
      <div className="detail-image-box">
        <span className="detail-image-icon">{icon}</span>
      </div>
      <div className="detail-info">
        <div className="detail-breadcrumb">
          <Link to="/">Home</Link>
          <span>/</span>
          <Link to="/products">Shop</Link>
          <span>/</span>
          <span style={{ color: 'var(--text-secondary)' }}>{product.name}</span>
        </div>

        <h1 className="detail-name">{product.name}</h1>
        <div className="detail-price">₹{product.price?.toLocaleString('en-IN')}</div>

        <div className="detail-stock stock-in">● In Stock</div>

        <p className="detail-desc">{product.description}</p>

        <div className="qty-selector">
          <label>Quantity</label>
          <div className="qty-controls">
            <button className="qty-btn" onClick={() => setQty(Math.max(1, qty - 1))}>−</button>
            <span className="qty-value">{qty}</span>
            <button className="qty-btn" onClick={() => setQty(qty + 1)}>+</button>
          </div>
        </div>

        <div className="detail-actions">
          <button className="btn btn-primary btn-lg" onClick={handleAdd}>
            {added ? '✓ Added to Cart' : 'Add to Cart'}
          </button>
          <Link to="/cart" className="btn btn-secondary btn-lg">View Cart</Link>
        </div>
      </div>
    </div>
  )
}

export default ProductDetailPage
