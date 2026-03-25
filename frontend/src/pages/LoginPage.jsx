import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { loginUser } from '../api/authApi.js'
import { useAuth } from '../hooks/useAuth.js'
import InputField from '../components/InputField.jsx'

function LoginPage() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [errors, setErrors] = useState({})
  const [apiError, setApiError] = useState('')
  const [loading, setLoading] = useState(false)
  const { login } = useAuth()
  const navigate = useNavigate()

  const validate = () => {
    const errs = {}
    if (!email) errs.email = 'Email is required'
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) errs.email = 'Enter a valid email'
    if (!password) errs.password = 'Password is required'
    return errs
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    const errs = validate()
    if (Object.keys(errs).length > 0) { setErrors(errs); return }
    setErrors({}); setApiError(''); setLoading(true)
    try {
      const data = await loginUser(email, password)
      login(data.token)
      navigate('/dashboard')
    } catch (err) {
      const msg = err.response?.data || 'Invalid credentials. Please try again.'
      setApiError(typeof msg === 'string' ? msg : 'Login failed.')
    } finally { setLoading(false) }
  }

  return (
    <div className="auth-container">
      <div className="auth-card">
        <div className="auth-card-header">
          <div className="auth-icon">🔐</div>
          <h1 className="auth-title">Welcome Back</h1>
          <p className="auth-subtitle">Sign in to your UrbanVogue account</p>
        </div>
        {apiError && <div className="alert alert-error" role="alert"><span className="alert-icon">⚠</span>{apiError}</div>}
        <form onSubmit={handleSubmit} className="auth-form" noValidate>
          <InputField id="login-email" label="Email Address" type="email" value={email} onChange={(e) => setEmail(e.target.value)} error={errors.email} placeholder="you@example.com" required />
          <InputField id="login-password" label="Password" type="password" value={password} onChange={(e) => setPassword(e.target.value)} error={errors.password} placeholder="Enter your password" required />
          <button type="submit" className="btn btn-primary btn-full" id="login-submit-btn" disabled={loading}>
            {loading ? <span className="btn-loading"><span className="spinner" />Signing in…</span> : 'Sign In'}
          </button>
        </form>
        <p className="auth-switch">Don't have an account? <Link to="/register" className="auth-link">Create one</Link></p>
      </div>
      <div className="auth-visual">
        <div className="visual-content">
          <h2 className="visual-headline">Dress to<br />impress.</h2>
          <p className="visual-tagline">Your style, delivered.</p>
          <div className="visual-dots"><span className="dot dot-active" /><span className="dot" /><span className="dot" /></div>
        </div>
      </div>
    </div>
  )
}

export default LoginPage
