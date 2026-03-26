const axios = require('axios');

async function test() {
  try {
    const login = await axios.post('http://localhost:8082/api/auth/login', {
      email: 'admin@gmail.com',
      password: 'password'
    });
    console.log('Got token');
    const token = login.data.token;
    
    // First let's hit product-service directly
    try {
      const res1 = await axios.post('http://localhost:8083/api/products', 
        { name: 'Direct', description: 'desc', price: 10 },
        { headers: { Authorization: Bearer  } }
      );
      console.log('Direct 8083 success:', res1.data.id);
    } catch(e) {
      console.log('Direct 8083 failed:', e.response?.status, e.response?.data);
    }

    // Now let's hit the gateway
    try {
      const res2 = await axios.post('http://localhost:8080/api/products', 
        { name: 'Gateway', description: 'desc', price: 10 },
        { headers: { Authorization: Bearer  } }
      );
      console.log('Gateway 8080 success:', res2.data.id);
    } catch(e) {
      console.log('Gateway 8080 failed:', e.response?.status, e.response?.data);
    }
  } catch(e) {
    console.log('Login failed', e.message);
  }
}
test();
