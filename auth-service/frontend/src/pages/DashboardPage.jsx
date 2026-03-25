import { useNavigate } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth.js'

const formatExpiry = (exp) => {
  if (!exp) return 'N/A'
  return new Date(exp * 1000).toLocaleString()
}

const roleLabel = (role) => {
  if (!role) return '—'
  return role.replace('ROLE_', '')
}

function DashboardPage() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  return (
    <div className="dashboard-container">
      <div className="dashboard-hero">
        <div className="dashboard-hero-content">
          <span className="dashboard-badge">● Active Session</span>
          <h1 className="dashboard-title">
            Welcome back,{' '}
            <span className="highlight">{user?.sub?.split('@')[0] || 'Fashionista'}</span>
          </h1>
          <p className="dashboard-subtitle">
            You're signed in to your UrbanVogue account. Explore the marketplace.
          </p>
        </div>
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
            <h3 className="info-card-title">Session Token</h3>
          </div>
          <ul className="info-list">
            <li className="info-item">
              <span className="info-key">Issued At</span>
              <span className="info-val">{formatExpiry(user?.iat)}</span>
            </li>
            <li className="info-item">
              <span className="info-key">Expires At</span>
              <span className="info-val">{formatExpiry(user?.exp)}</span>
            </li>
            <li className="info-item">
              <span className="info-key">Status</span>
              <span className="status-pill status-active">Valid</span>
            </li>
          </ul>
        </div>

        <div className="info-card info-card-actions">
          <div className="info-card-header">
            <span className="info-icon">🛍️</span>
            <h3 className="info-card-title">Quick Actions</h3>
          </div>
          <div className="action-grid">
            <button className="action-btn" disabled>
              <span>🛒</span>
              <span>Shop</span>
            </button>
            <button className="action-btn" disabled>
              <span>📦</span>
              <span>Orders</span>
            </button>
            <button className="action-btn" disabled>
              <span>💳</span>
              <span>Payments</span>
            </button>
            <button className="action-btn action-btn-danger" onClick={handleLogout}>
              <span>🚪</span>
              <span>Sign Out</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}

export default DashboardPage
