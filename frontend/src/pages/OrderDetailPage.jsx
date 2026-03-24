import { useState, useEffect } from 'react'
import { useParams, Link } from 'react-router-dom'
import { getOrderById } from '../api/orderApi.js'

function OrderDetailPage() {
  const { id } = useParams()
  const [order, setOrder] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    getOrderById(id)
      .then(setOrder)
      .catch(() => setOrder(null))
      .finally(() => setLoading(false))
  }, [id])

  const statusClass = (status) => {
    const s = (status || '').toLowerCase()
    if (['confirmed', 'paid', 'completed'].includes(s)) return 'status-confirmed'
    if (['cancelled', 'failed'].includes(s)) return 'status-cancelled'
    return 'status-pending'
  }

  if (loading) {
    return (
      <div className="page-loading" style={{ minHeight: 'calc(100vh - 72px)' }}>
        <div className="page-spinner" />
        <p>Loading order…</p>
      </div>
    )
  }

  if (!order) {
    return (
      <div className="cart-empty" style={{ paddingTop: '6rem' }}>
        <div className="cart-empty-icon">😕</div>
        <h2>Order Not Found</h2>
        <Link to="/orders" className="btn btn-primary" style={{ marginTop: '1rem' }}>Back to Orders</Link>
      </div>
    )
  }

  return (
    <section className="section" style={{ maxWidth: '800px' }}>
      <div className="detail-breadcrumb" style={{ marginBottom: '2rem' }}>
        <Link to="/">Home</Link>
        <span>/</span>
        <Link to="/orders">Orders</Link>
        <span>/</span>
        <span style={{ color: 'var(--text-secondary)' }}>#{order.id}</span>
      </div>

      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2rem' }}>
        <h1 className="section-title">Order #{order.id}</h1>
        <span className={`order-status-badge ${statusClass(order.status)}`}>
          {order.status || 'Pending'}
        </span>
      </div>

      <div className="info-card" style={{ marginBottom: '1.5rem' }}>
        <div className="info-card-header">
          <span className="info-icon">📋</span>
          <h3 className="info-card-title">Order Details</h3>
        </div>
        <ul className="info-list">
          <li className="info-item">
            <span className="info-key">Order ID</span>
            <span className="info-val">{order.id}</span>
          </li>
          <li className="info-item">
            <span className="info-key">Date</span>
            <span className="info-val">
              {order.createdAt ? new Date(order.createdAt).toLocaleString('en-IN') : '—'}
            </span>
          </li>
          <li className="info-item">
            <span className="info-key">Email</span>
            <span className="info-val">{order.userEmail || '—'}</span>
          </li>
          <li className="info-item">
            <span className="info-key">Total</span>
            <span className="info-val" style={{ color: 'var(--gold-light)', fontWeight: 700, fontSize: '1.1rem' }}>
              ₹{order.totalAmount?.toLocaleString('en-IN') || '0'}
            </span>
          </li>
        </ul>
      </div>

      <div className="info-card">
        <div className="info-card-header">
          <span className="info-icon">📦</span>
          <h3 className="info-card-title">Items</h3>
        </div>
        {order.items && order.items.length > 0 ? (
          <div className="checkout-items-list">
            {order.items.map((item, idx) => (
              <div key={idx} className="checkout-item">
                <div className="checkout-item-thumb"><span>👗</span></div>
                <div className="checkout-item-info">
                  <div className="checkout-item-name">Product #{item.productId}</div>
                  <div className="checkout-item-qty">Qty: {item.quantity}</div>
                </div>
                <div className="checkout-item-price">
                  ₹{item.price?.toLocaleString('en-IN') || '—'}
                </div>
              </div>
            ))}
          </div>
        ) : (
          <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>No items found</p>
        )}
      </div>
    </section>
  )
}

export default OrderDetailPage
