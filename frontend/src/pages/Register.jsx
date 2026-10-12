import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../AuthContext.jsx';
import { errMsg } from '../api';

export default function Register() {
  const { register } = useAuth();
  const [form, setForm] = useState({ name: '', email: '', password: '', phone: '', language: 'en' });
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  const submit = async (e) => {
    e.preventDefault();
    setError('');
    setBusy(true);
    try { await register(form); } catch (err) { setError(errMsg(err)); } finally { setBusy(false); }
  };

  return (
    <div className="auth-wrap">
      <div className="auth-card">
        <h1>Create your account</h1>
        <form onSubmit={submit}>
          <label>Full name</label>
          <input value={form.name} onChange={set('name')} required />
          <label>Email</label>
          <input type="email" value={form.email} onChange={set('email')} required />
          <label>Password (min 6 characters)</label>
          <input type="password" value={form.password} onChange={set('password')} minLength={6} required />
          <label>Mobile number (for SMS / voice alerts)</label>
          <input value={form.phone} onChange={set('phone')} placeholder="+91XXXXXXXXXX" />
          <label>Alert language</label>
          <select value={form.language} onChange={set('language')}>
            <option value="en">English</option>
            <option value="hi">हिन्दी (Hindi)</option>
            <option value="te">తెలుగు (Telugu)</option>
          </select>
          {error && <div className="alert-box error">{error}</div>}
          <button className="btn btn-primary btn-block" disabled={busy}>{busy ? 'Creating…' : 'Register'}</button>
        </form>
        <p className="muted center">Already registered? <Link to="/login">Sign in</Link></p>
      </div>
    </div>
  );
}
