import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import api, { errMsg } from '../api';
import RiskBadge from '../components/RiskBadge';
import { pretty } from '../utils';

export default function Dashboard() {
  const [data, setData] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    api.get('/dashboard').then((r) => setData(r.data)).catch((e) => setError(errMsg(e)));
  }, []);

  if (error) return <div className="alert-box error">{error}</div>;
  if (!data) return <p className="muted">Loading dashboard…</p>;

  return (
    <>
      <div className="page-head">
        <h2>Field Risk Dashboard</h2>
        <Link to="/fields" className="btn btn-primary">Manage fields</Link>
      </div>

      <div className="stats">
        <div className="stat"><span>Total fields</span><b>{data.totalFields}</b></div>
        <div className="stat stat-green"><span>Safe</span><b>{data.green}</b></div>
        <div className="stat stat-amber"><span>Watch closely</span><b>{data.amber}</b></div>
        <div className="stat stat-red"><span>Act immediately</span><b>{data.red}</b></div>
        <div className="stat"><span>Unread alerts</span><b>{data.unreadAlerts}</b></div>
        <div className="stat">
          <span>Prediction accuracy</span>
          <b>{data.accuracy != null ? `${data.accuracy}%` : '—'}</b>
          <small>{data.feedbackCount} feedback record(s)</small>
        </div>
      </div>

      {data.fields.length === 0 && (
        <div className="card center">
          <p>You have no fields yet.</p>
          <Link to="/fields" className="btn btn-primary">Add your first field</Link>
        </div>
      )}

      <div className="grid">
        {data.fields.map((f) => (
          <Link to={`/fields/${f.fieldId}`} key={f.fieldId} className={`card field-card risk-${f.riskLevel.toLowerCase()}`}>
            <div className="row-between">
              <h3>{f.name}</h3>
              <RiskBadge level={f.riskLevel} />
            </div>
            <p className="muted">{f.cropType}</p>
            {f.riskLevel !== 'UNKNOWN' ? (
              <>
                <p className="threat">
                  {f.riskLevel === 'GREEN' ? 'No threat detected' : pretty(f.threat)}
                  {f.score != null && <span className="score"> · score {Math.round(f.score)}/100</span>}
                </p>
                {f.riskLevel !== 'GREEN' && <p className="muted">Expected in about <b>{f.daysToImpact}</b> day(s)</p>}
              </>
            ) : null}
            <p className="reco">{f.recommendation}</p>
          </Link>
        ))}
      </div>
    </>
  );
}
