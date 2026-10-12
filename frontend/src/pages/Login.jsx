import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../AuthContext.jsx';
import { errMsg } from '../api';

export default function Login() {
  const { login } = useAuth();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    setError('');
    setBusy(true);
    try { await login(email, password); } catch (err) { setError(errMsg(err)); } finally { setBusy(false); }
  };

  return (
    <div className="auth-wrap">
      <div className="auth-card">
        <h1>🌾 AI Crop Emergency Predictor</h1>
        <p className="muted">Seeing the storm before the field does.</p>
        <form onSubmit={submit}>
          <label>Email</label>
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required autoFocus />
          <label>Password</label>
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
          {error && <div className="alert-box error">{error}</div>}
          <button className="btn btn-primary btn-block" disabled={busy}>{busy ? 'Signing in…' : 'Sign in'}</button>
        </form>
        <p className="muted center">New farmer? <Link to="/register">Create an account</Link></p>
        <div className="demo-box">
          <strong>Demo logins</strong><br />
          Farmer: farmer@agri.com / Farmer@123<br />
          Admin: admin@agri.com / Admin@123
        </div>
      </div>
    </div>
  );
}
