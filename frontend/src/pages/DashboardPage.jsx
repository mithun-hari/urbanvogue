import { useNavigate } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth.js'

const formatExpiry = (exp) => exp ? new Date(exp * 1000).toLocaleString() : 'N/A'
const roleLabel = (role) => role ? role.replace('ROLE_', '') : '—'

function DashboardPage() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = () => { logout(); navigate('/login') }
  const username = user?.sub?.split('@')[0] || 'Fashionista'

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
      </div>
    </div>
  )
}

export default DashboardPage
