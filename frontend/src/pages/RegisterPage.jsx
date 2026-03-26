import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { registerUser } from '../api/authApi.js'
import InputField from '../components/InputField.jsx'

function RegisterPage() {
  const [form, setForm] = useState({ username: '', email: '', password: '', confirm: '', role: 'USER' })
  const [errors, setErrors] = useState({})
  const [apiError, setApiError] = useState('')
  const [success, setSuccess] = useState('')
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()
  const update = (field) => (e) => setForm((p) => ({ ...p, [field]: e.target.value }))

  const validate = () => {
    const errs = {}
    if (!form.username) errs.username = 'Username is required'
    else if (form.username.length < 3) errs.username = 'Min. 3 characters'
    if (!form.email) errs.email = 'Email is required'
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) errs.email = 'Enter a valid email'
    if (!form.password) errs.password = 'Password is required'
    else if (form.password.length < 6) errs.password = 'Min. 6 characters'
    if (!form.confirm) errs.confirm = 'Please confirm password'
    else if (form.password !== form.confirm) errs.confirm = 'Passwords do not match'
    return errs
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    const errs = validate()
    if (Object.keys(errs).length > 0) { setErrors(errs); return }
    setErrors({}); setApiError(''); setLoading(true)
    try {
      await registerUser(form.username, form.email, form.password, form.role)
      setSuccess('Account created! Redirecting…')
      setTimeout(() => navigate('/login'), 1500)
    } catch (err) {
      const msg = err.response?.data || 'Registration failed.'
      setApiError(typeof msg === 'string' ? msg : 'Registration failed.')
    } finally { setLoading(false) }
  }

  return (
    <div className="auth-container auth-container-reverse">
      <div className="auth-visual">
        <div className="visual-content">
          <h2 className="visual-headline">Join the<br />Collection.</h2>
          <p className="visual-tagline">Exclusive access. Premium style.</p>
          <div className="visual-dots"><span className="dot" /><span className="dot dot-active" /><span className="dot" /></div>
        </div>
      </div>
      <div className="auth-card">
        <div className="auth-card-header">
          <div className="auth-icon">✨</div>
          <h1 className="auth-title">Create Account</h1>
          <p className="auth-subtitle">Join UrbanVogue today</p>
        </div>
        {apiError && <div className="alert alert-error" role="alert"><span className="alert-icon">⚠</span>{apiError}</div>}
        {success && <div className="alert alert-success" role="status"><span className="alert-icon">✓</span>{success}</div>}
        <form onSubmit={handleSubmit} className="auth-form" noValidate>
          <InputField id="reg-username" label="Username" value={form.username} onChange={update('username')} error={errors.username} placeholder="johndoe" required />
          <InputField id="reg-email" label="Email Address" type="email" value={form.email} onChange={update('email')} error={errors.email} placeholder="you@example.com" required />
          <InputField id="reg-password" label="Password" type="password" value={form.password} onChange={update('password')} error={errors.password} placeholder="Min. 6 characters" required />
          <InputField id="reg-confirm" label="Confirm Password" type="password" value={form.confirm} onChange={update('confirm')} error={errors.confirm} placeholder="Re-enter password" required />


          <button type="submit" className="btn btn-primary btn-full" id="register-submit-btn" disabled={loading}>
            {loading ? <span className="btn-loading"><span className="spinner" />Creating…</span> : 'Create Account'}
          </button>
        </form>
        <p className="auth-switch">Already have an account? <Link to="/login" className="auth-link">Sign in</Link></p>
      </div>
    </div>
  )
}

export default RegisterPage
