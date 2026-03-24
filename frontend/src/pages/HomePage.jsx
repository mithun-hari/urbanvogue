import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { getAllProducts } from '../api/productApi.js'
import ProductCard from '../components/ProductCard.jsx'

function HomePage() {
  const [products, setProducts] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    getAllProducts()
      .then((data) => setProducts(data.slice(0, 8)))
      .catch(() => setProducts([]))
      .finally(() => setLoading(false))
  }, [])

  return (
    <div>
      {/* Hero */}
      <section className="hero">
        <div className="hero-ring" />
        <div className="hero-ring hero-ring-2" />
        <div className="hero-content">
          <span className="hero-badge">New Season 2026</span>
          <h1 className="hero-title">Elevate Your<br />Style Game</h1>
          <p className="hero-subtitle">
            Discover curated collections from top designers. Premium fashion,
            delivered to your doorstep.
          </p>
          <div className="hero-actions">
            <Link to="/products" className="btn btn-primary btn-lg">Shop Now</Link>
            <Link to="/register" className="btn btn-secondary btn-lg">Join Free</Link>
          </div>
        </div>
      </section>

      {/* Featured Products */}
      <section className="section">
        <div className="section-header">
          <div>
            <h2 className="section-title">Featured Collection</h2>
            <p className="section-subtitle">Handpicked styles for the modern wardrobe</p>
          </div>
          <Link to="/products" className="section-link">View All →</Link>
        </div>

        {loading ? (
          <div className="page-loading">
            <div className="page-spinner" />
            <p>Loading collection…</p>
          </div>
        ) : products.length > 0 ? (
          <div className="product-grid">
            {products.map((p, i) => (
              <ProductCard key={p.id} product={p} index={i} />
            ))}
          </div>
        ) : (
          <div className="cart-empty">
            <div className="cart-empty-icon">🛍️</div>
            <h2>No Products Yet</h2>
            <p>The store is being stocked. Check back soon!</p>
          </div>
        )}
      </section>

      {/* Trust Banner */}
      <section className="section" style={{ textAlign: 'center' }}>
        <div style={{ display: 'flex', justifyContent: 'center', gap: '3rem', flexWrap: 'wrap', opacity: 0.5 }}>
          <div><span style={{ fontSize: '2rem' }}>🚚</span><p style={{ fontSize: '0.8rem', marginTop: '0.5rem', color: 'var(--text-muted)' }}>Free Shipping</p></div>
          <div><span style={{ fontSize: '2rem' }}>🔒</span><p style={{ fontSize: '0.8rem', marginTop: '0.5rem', color: 'var(--text-muted)' }}>Secure Payment</p></div>
          <div><span style={{ fontSize: '2rem' }}>↩️</span><p style={{ fontSize: '0.8rem', marginTop: '0.5rem', color: 'var(--text-muted)' }}>Easy Returns</p></div>
          <div><span style={{ fontSize: '2rem' }}>💎</span><p style={{ fontSize: '0.8rem', marginTop: '0.5rem', color: 'var(--text-muted)' }}>Premium Quality</p></div>
        </div>
      </section>
    </div>
  )
}

export default HomePage
