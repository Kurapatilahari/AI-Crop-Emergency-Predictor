import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import api, { errMsg } from '../api';
import RiskBadge from '../components/RiskBadge';
import { fmtDate } from '../utils';

export default function Alerts() {
  const [alerts, setAlerts] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  const load = () =>
    api.get('/alerts').then((r) => setAlerts(r.data)).catch((e) => setError(errMsg(e))).finally(() => setLoading(false));
  useEffect(() => { load(); }, []);

  const read = async (id) => { await api.post(`/alerts/${id}/read`).catch(() => {}); load(); };
  const readAll = async () => { await api.post('/alerts/read-all').catch(() => {}); load(); };

  return (
    <>
      <div className="page-head">
        <h2>Alerts</h2>
        <button className="btn btn-ghost" onClick={readAll}>Mark all as read</button>
      </div>
      {error && <div className="alert-box error">{error}</div>}
      {loading && <p className="muted">Loading…</p>}
      {!loading && alerts.length === 0 && <div className="card center">🎉 No alerts. Your fields look safe.</div>}
      {alerts.map((a) => (
        <div key={a.id} className={`card alert-card risk-${a.riskLevel.toLowerCase()} ${a.seen ? 'seen' : ''}`}>
          <div className="row-between wrap">
            <div className="row-gap"><RiskBadge level={a.riskLevel} /><Link to={`/fields/${a.fieldId}`}><b>{a.fieldName}</b></Link></div>
            <span className="muted small">{fmtDate(a.createdAt)} · via {a.channels.split(',').join(' + ')}</span>
          </div>
          <p>{a.message}</p>
          {!a.seen && <button className="btn btn-small btn-ghost" onClick={() => read(a.id)}>Mark as read</button>}
        </div>
      ))}
    </>
  );
}
