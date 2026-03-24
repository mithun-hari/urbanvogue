import { Link } from 'react-router-dom'

function Footer() {
  return (
    <footer className="footer">
      <div className="footer-grid">
        <div>
          <div className="footer-brand-name">UrbanVogue</div>
          <p className="footer-brand-desc">
            Your premium destination for curated fashion. Discover collections
            that define your style, delivered to your door.
          </p>
        </div>
        <div className="footer-col">
          <div className="footer-col-title">Shop</div>
          <ul>
            <li><Link to="/products">All Products</Link></li>
            <li><a href="#">New Arrivals</a></li>
            <li><a href="#">Best Sellers</a></li>
            <li><a href="#">Sale</a></li>
          </ul>
        </div>
        <div className="footer-col">
          <div className="footer-col-title">Account</div>
          <ul>
            <li><Link to="/login">Sign In</Link></li>
            <li><Link to="/register">Register</Link></li>
            <li><Link to="/orders">Order History</Link></li>
            <li><Link to="/dashboard">Dashboard</Link></li>
          </ul>
        </div>
        <div className="footer-col">
          <div className="footer-col-title">Support</div>
          <ul>
            <li><a href="#">Contact Us</a></li>
            <li><a href="#">Shipping Info</a></li>
            <li><a href="#">Return Policy</a></li>
            <li><a href="#">FAQ</a></li>
          </ul>
        </div>
      </div>
      <div className="footer-bottom">
        &copy; {new Date().getFullYear()} UrbanVogue. All rights reserved.
      </div>
    </footer>
  )
}

export default Footer
