import { useEffect, useState } from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../AuthContext.jsx';
import api from '../api';

export default function Navbar() {
  const { user, logout, isAdmin } = useAuth();
  const nav = useNavigate();
  const [unread, setUnread] = useState(0);

  useEffect(() => {
    let alive = true;
    const load = () =>
      api.get('/alerts/unread-count').then((r) => alive && setUnread(r.data.count)).catch(() => {});
    load();
    const t = setInterval(load, 30000);
    return () => { alive = false; clearInterval(t); };
  }, []);

  return (
    <header className="navbar">
      <div className="nav-inner">
        <div className="brand">🌾 Crop Emergency Predictor</div>
        <nav className="nav-links">
          <NavLink to="/" end>Dashboard</NavLink>
          <NavLink to="/fields">My Fields</NavLink>
          <NavLink to="/alerts">
            Alerts {unread > 0 && <span className="count">{unread}</span>}
          </NavLink>
          {isAdmin && <NavLink to="/admin">Admin</NavLink>}
        </nav>
        <div className="nav-user">
          <span>{user.name}</span>
          <button className="btn btn-small btn-ghost" onClick={() => { logout(); nav('/login'); }}>Logout</button>
        </div>
      </div>
    </header>
  );
}
