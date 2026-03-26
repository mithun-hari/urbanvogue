import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { useCart } from '../hooks/useCart.js'

function PaymentSuccessPage() {
  const [searchParams] = useSearchParams()
  const sessionId = searchParams.get('session_id')
  const { clearCart } = useCart()
  const [showConfetti, setShowConfetti] = useState(true)

  useEffect(() => {
    // Clear the cart after successful payment
    clearCart()
    const timer = setTimeout(() => setShowConfetti(false), 4000)
    return () => clearTimeout(timer)
  }, [])

  return (
    <section className="section" style={{ maxWidth: '700px', textAlign: 'center', paddingTop: '4rem' }}>
      {/* Confetti Animation */}
      {showConfetti && (
        <div style={{
          position: 'fixed', top: 0, left: 0, width: '100%', height: '100%',
          pointerEvents: 'none', zIndex: 1000, overflow: 'hidden',
        }}>
          {[...Array(40)].map((_, i) => (
            <div key={i} style={{
              position: 'absolute',
              top: '-10px',
              left: `${Math.random() * 100}%`,
              width: `${6 + Math.random() * 8}px`,
              height: `${6 + Math.random() * 8}px`,
              background: ['#d4af37', '#e8c547', '#f0d060', '#4ade80', '#60a5fa', '#f472b6', '#a78bfa'][i % 7],
              borderRadius: Math.random() > 0.5 ? '50%' : '2px',
              animation: `confettiFall ${2 + Math.random() * 3}s ease-in forwards`,
              animationDelay: `${Math.random() * 1.5}s`,
              opacity: 0.9,
            }} />
          ))}
        </div>
      )}

      {/* Success Icon */}
      <div style={{
        width: '120px', height: '120px', margin: '0 auto 2rem',
        borderRadius: '50%',
        background: 'linear-gradient(135deg, rgba(74, 222, 128, 0.2), rgba(74, 222, 128, 0.05))',
        border: '3px solid rgba(74, 222, 128, 0.4)',
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        animation: 'successPulse 2s ease-in-out infinite',
      }}>
        <svg width="56" height="56" viewBox="0 0 24 24" fill="none" stroke="#4ade80" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
          <polyline points="20 6 9 17 4 12" />
        </svg>
      </div>

      {/* Title */}
      <h1 style={{
        fontSize: '2.2rem', fontWeight: 700,
        background: 'linear-gradient(135deg, #4ade80, #d4af37)',
        WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent',
        marginBottom: '0.8rem',
      }}>
        Payment Successful!
      </h1>

      <p style={{
        color: 'var(--text-secondary)', fontSize: '1.05rem',
        lineHeight: 1.6, maxWidth: '480px', margin: '0 auto 2rem',
      }}>
        Thank you for your order! Your payment has been processed securely through Stripe.
        You will receive a confirmation email shortly.
      </p>

      {/* Order Info Card */}
      <div style={{
        background: 'rgba(255,255,255,0.03)',
        border: '1px solid rgba(212,175,55,0.15)',
        borderRadius: '16px',
        padding: '1.5rem 2rem',
        marginBottom: '2.5rem',
      }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.8rem' }}>
          <span style={{ color: 'var(--text-secondary)', fontSize: '0.85rem' }}>Status</span>
          <span style={{
            background: 'rgba(74, 222, 128, 0.15)',
            color: '#4ade80',
            padding: '0.25rem 0.75rem',
            borderRadius: '20px',
            fontSize: '0.8rem',
            fontWeight: 600,
          }}>✓ Confirmed</span>
        </div>
        {sessionId && (
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span style={{ color: 'var(--text-secondary)', fontSize: '0.85rem' }}>Transaction</span>
            <span style={{ color: 'var(--text-primary)', fontSize: '0.8rem', fontFamily: 'monospace' }}>
              {sessionId.substring(0, 20)}…
            </span>
          </div>
        )}
      </div>

      {/* Action Buttons */}
      <div style={{ display: 'flex', gap: '1rem', justifyContent: 'center', flexWrap: 'wrap' }}>
        <Link to="/products" className="btn btn-primary" style={{ minWidth: '180px' }}>
          🛍️ Continue Shopping
        </Link>
        <Link to="/dashboard" className="btn btn-secondary" style={{ minWidth: '180px' }}>
          📋 Go to Dashboard
        </Link>
      </div>

      {/* CSS Animations */}
      <style>{`
        @keyframes confettiFall {
          0% { transform: translateY(0) rotate(0deg); opacity: 1; }
          100% { transform: translateY(100vh) rotate(720deg); opacity: 0; }
        }
        @keyframes successPulse {
          0%, 100% { box-shadow: 0 0 0 0 rgba(74, 222, 128, 0.3); }
          50% { box-shadow: 0 0 0 15px rgba(74, 222, 128, 0); }
        }
      `}</style>
    </section>
  )
}

export default PaymentSuccessPage
