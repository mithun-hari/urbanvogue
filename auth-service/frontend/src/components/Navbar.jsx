import { Link, useNavigate, useLocation } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth.js'

function Navbar() {
  const { isAuthenticated, logout } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  return (
    <nav className="navbar">
      <div className="navbar-brand">
        <Link to="/" className="brand-link">
          <span className="brand-icon">UV</span>
          <span className="brand-name">UrbanVogue</span>
        </Link>
      </div>
      <div className="navbar-actions">
        {isAuthenticated ? (
          <button onClick={handleLogout} className="btn btn-outline">
            Logout
          </button>
        ) : (
          <div className="nav-links">
            {location.pathname !== '/login' && (
              <Link to="/login" className="nav-link">
                Sign In
              </Link>
            )}
            {location.pathname !== '/register' && (
              <Link to="/register" className="btn btn-primary">
                Register
              </Link>
            )}
          </div>
        )}
      </div>
    </nav>
  )
}

export default Navbar
