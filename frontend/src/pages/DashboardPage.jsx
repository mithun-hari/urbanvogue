import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth.js'
import { getAllProducts, createProduct, updateProduct, deleteProduct } from '../api/productApi.js'
import { addStock } from '../api/inventoryApi.js'

const formatExpiry = (exp) => exp ? new Date(exp * 1000).toLocaleString() : 'N/A'
const roleLabel = (role) => role ? role.replace('ROLE_', '') : '—'

function DashboardPage() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const isAdmin = user?.role === 'ADMIN'

  const handleLogout = () => { logout(); navigate('/login') }
  const username = user?.sub?.split('@')[0] || 'Fashionista'

  // Admin product state
  const [products, setProducts] = useState([])
  const [prodLoading, setProdLoading] = useState(false)
  const [showForm, setShowForm] = useState(false)
  const [editingId, setEditingId] = useState(null)
  const [form, setForm] = useState({ name: '', description: '', price: '', imageUrl: '' })
  const [formError, setFormError] = useState('')
  const [formSuccess, setFormSuccess] = useState('')

  // Stock state
  const [stockForm, setStockForm] = useState({ productId: '', quantity: '' })
  const [stockMsg, setStockMsg] = useState('')

  const fetchProducts = async () => {
    setProdLoading(true)
    try {
      const data = await getAllProducts()
      setProducts(data)
    } catch { setProducts([]) }
    setProdLoading(false)
  }

  useEffect(() => {
    if (isAdmin) fetchProducts()
  }, [isAdmin])

  const resetForm = () => {
    setForm({ name: '', description: '', price: '', imageUrl: '' })
    setEditingId(null)
    setShowForm(false)
    setFormError('')
    setFormSuccess('')
  }

  const handleEdit = (p) => {
    setForm({ name: p.name, description: p.description, price: String(p.price), imageUrl: p.imageUrl || '' })
    setEditingId(p.id)
    setShowForm(true)
    setFormError('')
    setFormSuccess('')
  }

  const extractError = (err) => {
    const d = err.response?.data
    if (!d) return err.message || 'Network error.'
    if (typeof d === 'string') return d
    return d.message || d.error || JSON.stringify(d)
  }

  const handleDelete = async (id) => {
    if (!confirm('Delete this product permanently?')) return
    try {
      await deleteProduct(id)
      setProducts((prev) => prev.filter((p) => p.id !== id))
      setFormSuccess('Product deleted.')
    } catch (err) {
      setFormError(extractError(err))
    }
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setFormError(''); setFormSuccess('')
    if (!form.name || !form.description || !form.price) { setFormError('All fields are required.'); return }
    const payload = { name: form.name, description: form.description, price: parseFloat(form.price), imageUrl: form.imageUrl || null }
    try {
      if (editingId) {
        await updateProduct(editingId, payload)
        setFormSuccess('Product updated!')
      } else {
        await createProduct(payload)
        setFormSuccess('Product created!')
      }
      resetForm()
      fetchProducts()
    } catch (err) {
      setFormError(extractError(err))
    }
  }

  const handleAddStock = async (e) => {
    e.preventDefault()
    setStockMsg('')
    if (!stockForm.productId || !stockForm.quantity) { setStockMsg('⚠ Fill both fields.'); return }
    try {
      await addStock(parseInt(stockForm.productId), parseInt(stockForm.quantity))
      setStockMsg('✅ Stock added successfully!')
      setStockForm({ productId: '', quantity: '' })
    } catch (err) {
      setStockMsg('⚠ ' + extractError(err))
    }
  }

  return (
    <div className="dashboard-container">
      <div className="dashboard-hero">
        <span className="dashboard-badge">● Active Session</span>
        <h1 className="dashboard-title">
          Welcome back, <span className="highlight">{username}</span>
        </h1>
        <p className="dashboard-subtitle">Manage your account and explore the marketplace.</p>
      </div>

      <div className="dashboard-grid">
        <div className="info-card">
          <div className="info-card-header">
            <span className="info-icon">👤</span>
            <h3 className="info-card-title">Account Info</h3>
          </div>
          <ul className="info-list">
            <li className="info-item">
              <span className="info-key">Email</span>
              <span className="info-val">{user?.sub || '—'}</span>
            </li>
            <li className="info-item">
              <span className="info-key">Role</span>
              <span className={`role-badge role-${roleLabel(user?.role)?.toLowerCase()}`}>
                {roleLabel(user?.role)}
              </span>
            </li>
          </ul>
        </div>

        <div className="info-card">
          <div className="info-card-header">
            <span className="info-icon">🔑</span>
            <h3 className="info-card-title">Session</h3>
          </div>
          <ul className="info-list">
            <li className="info-item">
              <span className="info-key">Issued</span>
              <span className="info-val">{formatExpiry(user?.iat)}</span>
            </li>
            <li className="info-item">
              <span className="info-key">Expires</span>
              <span className="info-val">{formatExpiry(user?.exp)}</span>
            </li>
            <li className="info-item">
              <span className="info-key">Status</span>
              <span className="status-pill status-active">Valid</span>
            </li>
          </ul>
        </div>

        <div className="info-card" style={{ gridColumn: '1 / -1' }}>
          <div className="info-card-header">
            <span className="info-icon">🛍️</span>
            <h3 className="info-card-title">Quick Actions</h3>
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '1rem' }}>
            <button className="btn btn-secondary" onClick={() => navigate('/products')} style={{ flexDirection: 'column', padding: '1.2rem', gap: '0.4rem' }}>
              <span style={{ fontSize: '1.5rem' }}>🛒</span>
              <span style={{ fontSize: '0.78rem' }}>Shop</span>
            </button>
            <button className="btn btn-secondary" onClick={() => navigate('/orders')} style={{ flexDirection: 'column', padding: '1.2rem', gap: '0.4rem' }}>
              <span style={{ fontSize: '1.5rem' }}>📦</span>
              <span style={{ fontSize: '0.78rem' }}>Orders</span>
            </button>
            <button className="btn btn-secondary" onClick={() => navigate('/cart')} style={{ flexDirection: 'column', padding: '1.2rem', gap: '0.4rem' }}>
              <span style={{ fontSize: '1.5rem' }}>🛍️</span>
              <span style={{ fontSize: '0.78rem' }}>Cart</span>
            </button>
            <button className="btn btn-secondary" onClick={handleLogout} style={{ flexDirection: 'column', padding: '1.2rem', gap: '0.4rem', borderColor: 'rgba(224,82,82,0.2)' }}>
              <span style={{ fontSize: '1.5rem' }}>🚪</span>
              <span style={{ fontSize: '0.78rem' }}>Sign Out</span>
            </button>
          </div>
        </div>

        {/* ═══════════════ ADMIN PANEL ═══════════════ */}
        {isAdmin && (
          <>
            <div className="info-card" style={{ gridColumn: '1 / -1' }}>
              <div className="info-card-header" style={{ marginBottom: '1.2rem' }}>
                <span className="info-icon">⚡</span>
                <h3 className="info-card-title" style={{ color: 'var(--accent)' }}>Admin — Product Management</h3>
              </div>

              <button
                className="btn btn-primary"
                onClick={() => { resetForm(); setShowForm(true) }}
                style={{ marginBottom: '1.2rem' }}
              >
                + Add New Product
              </button>

              {formSuccess && <div className="alert alert-success" style={{ marginBottom: '1rem' }}><span className="alert-icon">✓</span>{formSuccess}</div>}
              {formError && <div className="alert alert-error" style={{ marginBottom: '1rem' }}><span className="alert-icon">⚠</span>{formError}</div>}

              {showForm && (
                <form onSubmit={handleSubmit} style={{
                  background: 'rgba(255,255,255,0.03)',
                  border: '1px solid rgba(212,175,55,0.15)',
                  borderRadius: '12px',
                  padding: '1.5rem',
                  marginBottom: '1.5rem',
                  display: 'grid',
                  gap: '1rem',
                }}>
                  <h4 style={{ color: 'var(--accent)', margin: 0 }}>{editingId ? 'Edit Product' : 'New Product'}</h4>
                  <input className="input-field" placeholder="Product Name" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required />
                  <input className="input-field" placeholder="Description" value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} required />
                  <input className="input-field" type="number" step="0.01" placeholder="Price (₹)" value={form.price} onChange={(e) => setForm({ ...form, price: e.target.value })} required />
                  <input className="input-field" placeholder="Image URL (optional)" value={form.imageUrl} onChange={(e) => setForm({ ...form, imageUrl: e.target.value })} />
                  {form.imageUrl && (
                    <div style={{ width: '80px', height: '80px', borderRadius: '8px', overflow: 'hidden', border: '1px solid rgba(212,175,55,0.2)' }}>
                      <img src={form.imageUrl} alt="Preview" style={{ width: '100%', height: '100%', objectFit: 'cover' }} onError={(e) => e.target.style.display = 'none'} />
                    </div>
                  )}
                  <div style={{ display: 'flex', gap: '0.8rem' }}>
                    <button type="submit" className="btn btn-primary">{editingId ? 'Update' : 'Create'}</button>
                    <button type="button" className="btn btn-secondary" onClick={resetForm}>Cancel</button>
                  </div>
                </form>
              )}

              {prodLoading ? (
                <div className="page-loading"><div className="page-spinner" /><p>Loading products…</p></div>
              ) : products.length > 0 ? (
                <div style={{ overflowX: 'auto' }}>
                  <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.88rem' }}>
                    <thead>
                      <tr style={{ borderBottom: '1px solid rgba(255,255,255,0.08)', textAlign: 'left' }}>
                        <th style={{ padding: '0.8rem 0.5rem', color: 'var(--text-secondary)' }}>ID</th>
                        <th style={{ padding: '0.8rem 0.5rem', color: 'var(--text-secondary)' }}>Image</th>
                        <th style={{ padding: '0.8rem 0.5rem', color: 'var(--text-secondary)' }}>Name</th>
                        <th style={{ padding: '0.8rem 0.5rem', color: 'var(--text-secondary)' }}>Price</th>
                        <th style={{ padding: '0.8rem 0.5rem', color: 'var(--text-secondary)' }}>Actions</th>
                      </tr>
                    </thead>
                    <tbody>
                      {products.map((p) => (
                        <tr key={p.id} style={{ borderBottom: '1px solid rgba(255,255,255,0.04)' }}>
                          <td style={{ padding: '0.7rem 0.5rem', color: 'var(--text-secondary)' }}>#{p.id}</td>
                          <td style={{ padding: '0.7rem 0.5rem' }}>
                            {p.imageUrl ? (
                              <img src={p.imageUrl} alt={p.name} style={{ width: '40px', height: '40px', objectFit: 'cover', borderRadius: '6px' }} />
                            ) : (
                              <span style={{ fontSize: '1.3rem' }}>🛍️</span>
                            )}
                          </td>
                          <td style={{ padding: '0.7rem 0.5rem', color: 'var(--text-primary)' }}>{p.name}</td>
                          <td style={{ padding: '0.7rem 0.5rem', color: 'var(--accent)' }}>₹{p.price?.toLocaleString('en-IN')}</td>
                          <td style={{ padding: '0.7rem 0.5rem' }}>
                            <div style={{ display: 'flex', gap: '0.5rem' }}>
                              <button className="btn btn-secondary" style={{ padding: '0.35rem 0.8rem', fontSize: '0.78rem' }} onClick={() => handleEdit(p)}>Edit</button>
                              <button className="btn btn-secondary" style={{ padding: '0.35rem 0.8rem', fontSize: '0.78rem', borderColor: 'rgba(224,82,82,0.3)', color: '#e05252' }} onClick={() => handleDelete(p.id)}>Delete</button>
                            </div>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : (
                <p style={{ color: 'var(--text-secondary)', textAlign: 'center', padding: '2rem 0' }}>No products yet. Create one above!</p>
              )}
            </div>

            {/* ── Inventory Stock Management ── */}
            <div className="info-card" style={{ gridColumn: '1 / -1' }}>
              <div className="info-card-header" style={{ marginBottom: '1rem' }}>
                <span className="info-icon">📦</span>
                <h3 className="info-card-title" style={{ color: 'var(--accent)' }}>Admin — Add Inventory Stock</h3>
              </div>
              {stockMsg && (
                <div className={`alert ${stockMsg.startsWith('✅') ? 'alert-success' : 'alert-error'}`} style={{ marginBottom: '1rem' }}>
                  {stockMsg}
                </div>
              )}
              <form onSubmit={handleAddStock} style={{ display: 'flex', gap: '1rem', alignItems: 'flex-end', flexWrap: 'wrap' }}>
                <div style={{ flex: 1, minWidth: '120px' }}>
                  <label className="input-label" style={{ fontSize: '0.78rem' }}>Product ID</label>
                  <input className="input-field" type="number" placeholder="e.g. 1" value={stockForm.productId} onChange={(e) => setStockForm({ ...stockForm, productId: e.target.value })} required />
                </div>
                <div style={{ flex: 1, minWidth: '120px' }}>
                  <label className="input-label" style={{ fontSize: '0.78rem' }}>Quantity</label>
                  <input className="input-field" type="number" placeholder="e.g. 50" value={stockForm.quantity} onChange={(e) => setStockForm({ ...stockForm, quantity: e.target.value })} required />
                </div>
                <button type="submit" className="btn btn-primary" style={{ height: '44px' }}>Add Stock</button>
              </form>
            </div>
          </>
        )}
      </div>
    </div>
  )
}

export default DashboardPage
