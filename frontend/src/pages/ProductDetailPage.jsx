import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { getProductById } from '../api/productApi.js'
import { useCart } from '../hooks/useCart.js'

function ProductDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const { addToCart } = useCart()
  const [product, setProduct] = useState(null)
  const [loading, setLoading] = useState(true)
  const [qty, setQty] = useState(1)
  const [added, setAdded] = useState(false)

  useEffect(() => {
    getProductById(id)
      .then(setProduct)
      .catch(() => setProduct(null))
      .finally(() => setLoading(false))
  }, [id])

  const handleAdd = () => {
    addToCart(product, qty)
    setAdded(true)
    setTimeout(() => setAdded(false), 2000)
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
        <button className="btn btn-primary" onClick={() => navigate('/products')} style={{ marginTop: '1rem' }}>
          Back to Shop
        </button>
      </div>
    )
  }

  return (
    <section className="section" style={{ maxWidth: '1100px' }}>
      <button className="btn btn-secondary" onClick={() => navigate('/products')} style={{ marginBottom: '2rem', fontSize: '0.85rem' }}>
        ← Back to Shop
      </button>
      <div style={{
        display: 'grid',
        gridTemplateColumns: '1fr 1fr',
        gap: '3rem',
        alignItems: 'start',
      }}>
        {/* Product Image */}
        <div style={{
          aspectRatio: '1',
          borderRadius: '16px',
          overflow: 'hidden',
          background: 'rgba(255,255,255,0.03)',
          border: '1px solid rgba(255,255,255,0.06)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
        }}>
          {product.imageUrl ? (
            <img
              src={product.imageUrl}
              alt={product.name}
              style={{ width: '100%', height: '100%', objectFit: 'cover' }}
              onError={(e) => {
                e.target.style.display = 'none'
                e.target.parentElement.innerHTML = '<div style="display:flex;flex-direction:column;align-items:center;gap:1rem;"><span style="font-size:4rem;opacity:0.4;">🛍️</span><span style="color:rgba(255,255,255,0.3);font-size:0.85rem;">No Image Available</span></div>'
              }}
            />
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '1rem' }}>
              <span style={{ fontSize: '4rem', opacity: 0.4 }}>🛍️</span>
              <span style={{ color: 'rgba(255,255,255,0.3)', fontSize: '0.85rem' }}>No Image Available</span>
            </div>
          )}
        </div>

        {/* Product Info */}
        <div>
          <h1 style={{
            fontFamily: "'Playfair Display', serif",
            fontSize: '2rem',
            color: 'var(--text-primary)',
            marginBottom: '0.5rem',
          }}>
            {product.name}
          </h1>
          <p style={{
            color: 'var(--text-secondary)',
            fontSize: '0.95rem',
            lineHeight: '1.7',
            marginBottom: '1.5rem',
          }}>
            {product.description}
          </p>
          <div style={{
            fontSize: '1.8rem',
            fontWeight: '700',
            color: 'var(--accent)',
            marginBottom: '2rem',
          }}>
            ₹{product.price?.toLocaleString('en-IN')}
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', marginBottom: '1.5rem' }}>
            <label style={{ color: 'var(--text-secondary)', fontSize: '0.88rem' }}>Qty:</label>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <button
                className="btn btn-secondary"
                style={{ width: '36px', height: '36px', padding: 0, fontSize: '1.1rem' }}
                onClick={() => setQty((q) => Math.max(1, q - 1))}
              >
                −
              </button>
              <span style={{ width: '2.5rem', textAlign: 'center', fontSize: '1.05rem', color: 'var(--text-primary)' }}>{qty}</span>
              <button
                className="btn btn-secondary"
                style={{ width: '36px', height: '36px', padding: 0, fontSize: '1.1rem' }}
                onClick={() => setQty((q) => q + 1)}
              >
                +
              </button>
            </div>
          </div>

          <button className="btn btn-primary" onClick={handleAdd} style={{ width: '100%', padding: '1rem' }}>
            {added ? '✓ Added to Cart!' : 'Add to Cart'}
          </button>

          {added && (
            <button
              className="btn btn-secondary"
              onClick={() => navigate('/cart')}
              style={{ width: '100%', marginTop: '0.8rem' }}
            >
              View Cart →
            </button>
          )}
        </div>
      </div>
    </section>
  )
}

export default ProductDetailPage
