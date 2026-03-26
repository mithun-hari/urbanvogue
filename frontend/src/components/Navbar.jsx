import { Link, useNavigate, useLocation } from 'react-router-dom'
import { useState, useRef, useEffect } from 'react'
import { useAuth } from '../hooks/useAuth.js'
import { useCart } from '../hooks/useCart.js'

function Navbar() {
  const { isAuthenticated, user, logout } = useAuth()
  const { cartCount } = useCart()
  const navigate = useNavigate()
  const location = useLocation()
  const [dropdownOpen, setDropdownOpen] = useState(false)
  const dropdownRef = useRef(null)

  const handleLogout = () => {
    logout()
    setDropdownOpen(false)
    navigate('/login')
  }

  useEffect(() => {
    const handleClick = (e) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target)) {
        setDropdownOpen(false)
      }
    }
    document.addEventListener('mousedown', handleClick)
    return () => document.removeEventListener('mousedown', handleClick)
  }, [])

  const isActive = (path) => location.pathname === path || location.pathname.startsWith(path + '/')
  const username = user?.sub?.split('@')[0] || 'User'
  const isAdmin = user?.role === 'ADMIN'

  return (
    <nav className="navbar">
      <Link to="/" className="brand-link">
        <span className="brand-icon">UV</span>
        <span className="brand-name">UrbanVogue</span>
      </Link>

      <div className="nav-center">
        <Link to="/" className={`nav-center-link${isActive('/') && location.pathname === '/' ? ' active' : ''}`}>Home</Link>
        <Link to="/products" className={`nav-center-link${isActive('/products') ? ' active' : ''}`}>Shop</Link>
        {isAuthenticated && (
          <Link to="/orders" className={`nav-center-link${isActive('/orders') ? ' active' : ''}`}>Orders</Link>
        )}
        {isAuthenticated && isAdmin && (
          <Link to="/dashboard" className={`nav-center-link${isActive('/dashboard') ? ' active' : ''}`} style={{ color: 'var(--accent)' }}>Admin</Link>
        )}
      </div>

      <div className="nav-actions">
        <button className="cart-btn" onClick={() => navigate('/cart')} aria-label="Cart">
          🛒
          {cartCount > 0 && <span className="cart-badge">{cartCount}</span>}
        </button>

        {isAuthenticated ? (
          <div className="user-dropdown" ref={dropdownRef}>
            <button className="user-btn" onClick={() => setDropdownOpen(!dropdownOpen)}>
              <span className="user-avatar">{username[0].toUpperCase()}</span>
              {username}
            </button>
            {dropdownOpen && (
              <div className="dropdown-menu">
                <Link to="/dashboard" className="dropdown-item" onClick={() => setDropdownOpen(false)}>
                  Dashboard
                </Link>
                <Link to="/orders" className="dropdown-item" onClick={() => setDropdownOpen(false)}>
                  My Orders
                </Link>
                {isAdmin && (
                  <>
                    <div className="dropdown-divider" />
                    <Link to="/dashboard" className="dropdown-item" onClick={() => setDropdownOpen(false)} style={{ color: 'var(--accent)' }}>
                      ⚡ Admin Panel
                    </Link>
                  </>
                )}
                <div className="dropdown-divider" />
                <button className="dropdown-item danger" onClick={handleLogout}>
                  Sign Out
                </button>
              </div>
            )}
          </div>
        ) : (
          <div className="nav-actions">
            {location.pathname !== '/login' && (
              <Link to="/login" className="nav-link-text">Sign In</Link>
            )}
            {location.pathname !== '/register' && (
              <Link to="/register" className="btn btn-primary btn-sm">Register</Link>
            )}
          </div>
        )}
      </div>
    </nav>
  )
}

export default Navbar
