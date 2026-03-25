import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth.js'
import { getOrdersByUser } from '../api/orderApi.js'

function OrdersPage() {
  const { user } = useAuth()
  const [orders, setOrders] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const userId = user?.userId || 1
    getOrdersByUser(userId)
      .then(setOrders)
      .catch(() => setOrders([]))
      .finally(() => setLoading(false))
  }, [user])

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
        <p>Loading orders…</p>
      </div>
    )
  }

  return (
    <section className="section">
      <div className="section-header">
        <div>
          <h1 className="section-title">My Orders</h1>
          <p className="section-subtitle">Track and manage your purchases</p>
        </div>
      </div>

      {orders.length > 0 ? (
        <div className="orders-list">
          {orders.map((order) => (
            <Link to={`/orders/${order.id}`} key={order.id}>
              <div className="order-card">
                <div className="order-card-top">
                  <div>
                    <div className="order-id">Order #{order.id}</div>
                    <div className="order-date">
                      {order.createdAt
                        ? new Date(order.createdAt).toLocaleDateString('en-IN', {
                            day: 'numeric', month: 'short', year: 'numeric',
                          })
                        : '—'}
                    </div>
                  </div>
                  <span className={`order-status-badge ${statusClass(order.status)}`}>
                    {order.status || 'Pending'}
                  </span>
                </div>
                <div className="order-card-bottom">
                  <div className="order-items-count">
                    {order.items?.length || 0} item{(order.items?.length || 0) !== 1 ? 's' : ''}
                  </div>
                  <div className="order-total">₹{order.totalAmount?.toLocaleString('en-IN') || '0'}</div>
                </div>
              </div>
            </Link>
          ))}
        </div>
      ) : (
        <div className="cart-empty">
          <div className="cart-empty-icon">📦</div>
          <h2>No Orders Yet</h2>
          <p>Your order history will appear here after your first purchase.</p>
          <Link to="/products" className="btn btn-primary" style={{ marginTop: '1rem' }}>
            Start Shopping
          </Link>
        </div>
      )}
    </section>
  )
}

export default OrdersPage
