import { useEffect, useState } from 'react';
import api, { errMsg } from '../api';
import { pretty } from '../utils';

export default function Admin() {
  const [users, setUsers] = useState([]);
  const [model, setModel] = useState([]);
  const [stats, setStats] = useState(null);
  const [error, setError] = useState('');

  const load = () => {
    Promise.all([api.get('/admin/users'), api.get('/admin/model'), api.get('/admin/stats')])
      .then(([u, m, s]) => { setUsers(u.data); setModel(m.data); setStats(s.data); })
      .catch((e) => setError(errMsg(e)));
  };
  useEffect(load, []);

  const del = async (u) => {
    if (!window.confirm(`Delete ${u.name} and all their fields?`)) return;
    try { await api.delete(`/admin/users/${u.id}`); load(); } catch (e) { setError(errMsg(e)); }
  };

  return (
    <>
      <div className="page-head"><h2>Admin Console</h2></div>
      {error && <div className="alert-box error">{error}</div>}

      {stats && (
        <div className="stats">
          <div className="stat"><span>Users</span><b>{stats.users}</b></div>
          <div className="stat"><span>Fields</span><b>{stats.fields}</b></div>
          <div className="stat"><span>Predictions</span><b>{stats.predictions}</b></div>
          <div className="stat"><span>Alerts</span><b>{stats.alerts}</b></div>
          <div className="stat"><span>Model accuracy</span><b>{stats.accuracy != null ? `${stats.accuracy}%` : '—'}</b><small>{stats.feedbackCount} feedback</small></div>
        </div>
      )}

      <div className="card">
        <h3>Learned model sensitivity</h3>
        <p className="muted small">Starts at 1.00 and moves with farmer feedback (confirmed ↑, false alarm ↓, missed ↑↑).</p>
        <div className="table-wrap"><table>
          <thead><tr><th>Threat</th><th>Sensitivity</th><th>Confirmed</th><th>False alarms</th><th>Missed</th></tr></thead>
          <tbody>{model.map((m) => (
            <tr key={m.threat}><td>{pretty(m.threat)}</td><td>{m.sensitivity.toFixed(2)}</td><td>{m.confirmedCount}</td><td>{m.falseAlarmCount}</td><td>{m.missedCount}</td></tr>
          ))}</tbody>
        </table></div>
      </div>

      <div className="card">
        <h3>Users</h3>
        <div className="table-wrap"><table>
          <thead><tr><th>Name</th><th>Email</th><th>Phone</th><th>Lang</th><th>Role</th><th></th></tr></thead>
          <tbody>{users.map((u) => (
            <tr key={u.id}>
              <td>{u.name}</td><td>{u.email}</td><td>{u.phone}</td><td>{u.language}</td><td>{u.role}</td>
              <td>{u.role !== 'ADMIN' && <button className="btn btn-small btn-danger" onClick={() => del(u)}>Delete</button>}</td>
            </tr>
          ))}</tbody>
        </table></div>
      </div>
    </>
  );
}
