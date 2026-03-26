import axios from 'axios';

async function test() {
  try {
    const login = await axios.post('http://localhost:5173/api/auth/login', {
      email: 'admintest@test.com',
      password: 'password123'
    });
    const token = login.data.token;
    
    try {
      const res1 = await axios.post('http://localhost:5173/api/products/', 
        { name: 'Vite Proxy Trailing Slash', description: 'desc', price: 99 },
        { headers: { Authorization: `Bearer ${token}` } }
      );
      console.log('Success POST via Vite! Product ID:', res1.data.id);
    } catch(e) {
      console.log('FAILED POST via Vite with Trailing Slash!! Status:', e.response?.status);
      if (e.response) console.log('Error Body:', e.response.data);
    }
  } catch(e) {
    console.log('Login failed', e.message);
  }
}
test();
