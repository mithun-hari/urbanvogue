import { useState, useEffect } from 'react'
import { getAllProducts } from '../api/productApi.js'
import ProductCard from '../components/ProductCard.jsx'

function ProductsPage() {
  const [products, setProducts] = useState([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')

  useEffect(() => {
    getAllProducts()
      .then(setProducts)
      .catch(() => setProducts([]))
      .finally(() => setLoading(false))
  }, [])

  const filtered = products.filter(
    (p) =>
      p.name?.toLowerCase().includes(search.toLowerCase()) ||
      p.description?.toLowerCase().includes(search.toLowerCase())
  )

  return (
    <section className="section" style={{ maxWidth: '1300px' }}>
      <div className="section-header" style={{ flexDirection: 'column', alignItems: 'flex-start', gap: '1rem' }}>
        <div>
          <h1 className="section-title">Shop All</h1>
          <p className="section-subtitle">Browse our complete collection</p>
        </div>
        <div style={{ width: '100%', maxWidth: '400px' }}>
          <input
            type="text"
            className="input-field"
            placeholder="Search products…"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>
      </div>

      {loading ? (
        <div className="page-loading">
          <div className="page-spinner" />
          <p>Loading products…</p>
        </div>
      ) : filtered.length > 0 ? (
        <div className="product-grid">
          {filtered.map((p, i) => (
            <ProductCard key={p.id} product={p} index={i} />
          ))}
        </div>
      ) : (
        <div className="cart-empty">
          <div className="cart-empty-icon">🔍</div>
          <h2>{search ? 'No matches found' : 'No Products Available'}</h2>
          <p>{search ? 'Try a different search term' : 'Products will appear here once added'}</p>
        </div>
      )}
    </section>
  )
}

export default ProductsPage
