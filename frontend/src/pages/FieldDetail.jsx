import { useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { CartesianGrid, Legend, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import api, { errMsg } from '../api';
import RiskBadge from '../components/RiskBadge';
import { THREATS, fmtDate, fmtDay, pretty } from '../utils';

const SCENARIOS = [
  ['HEALTHY', 'Healthy field'], ['DROUGHT', 'Drought building'], ['HEAT', 'Heat wave'],
  ['WATERLOGGING', 'Waterlogging'], ['NUTRIENT', 'Nutrient deficiency'], ['PEST', 'Pest outbreak'],
  ['SALINITY', 'Salinity / pH drift']
];

const blank = {
  soilMoisture: '', temperature: '', humidity: '', ndvi: '', soilPh: '', salinity: '',
  nitrogen: '', phosphorus: '', potassium: '', rainfall: '', windSpeed: ''
};

const LABELS = {
  soilMoisture: 'Soil moisture (%) *', temperature: 'Temperature (°C) *', humidity: 'Humidity (%) *', ndvi: 'NDVI (0-1) *',
  soilPh: 'Soil pH', salinity: 'Salinity (dS/m)', nitrogen: 'Nitrogen (kg/ha)', phosphorus: 'Phosphorus (kg/ha)',
  potassium: 'Potassium (kg/ha)', rainfall: 'Rainfall (mm)', windSpeed: 'Wind (km/h)'
};

function FeedbackCell({ p, onSubmit }) {
  const [actual, setActual] = useState('');
  if (p.outcome !== 'PENDING') {
    return <span className={`pill ${p.outcome === 'PROBLEM_OCCURRED' ? 'pill-red' : 'pill-green'}`}>
      {p.outcome === 'PROBLEM_OCCURRED' ? 'Problem occurred' : 'No problem'}
    </span>;
  }
  return (
    <div className="row-gap wrap">
      {p.riskLevel === 'GREEN' && (
        <select value={actual} onChange={(e) => setActual(e.target.value)} className="mini-select">
          <option value="">What happened?</option>
          {THREATS.map((t) => <option key={t} value={t}>{pretty(t)}</option>)}
        </select>
      )}
      <button className="btn btn-small btn-danger" onClick={() => onSubmit(p.id, 'PROBLEM_OCCURRED', actual)}>Problem occurred</button>
      <button className="btn btn-small btn-ghost" onClick={() => onSubmit(p.id, 'NO_PROBLEM', '')}>No problem</button>
    </div>
  );
}

export default function FieldDetail() {
  const { id } = useParams();
  const [field, setField] = useState(null);
  const [readings, setReadings] = useState([]);
  const [predictions, setPredictions] = useState([]);
  const [weather, setWeather] = useState(null);
  const [scenario, setScenario] = useState('DROUGHT');
  const [form, setForm] = useState(blank);
  const [showForm, setShowForm] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [info, setInfo] = useState('');

  const load = useCallback(async () => {
    try {
      const [f, r, p] = await Promise.all([
        api.get(`/fields/${id}`), api.get(`/fields/${id}/readings?limit=30`), api.get(`/fields/${id}/predictions?limit=15`)
      ]);
      setField(f.data); setReadings(r.data); setPredictions(p.data);
    } catch (e) { setError(errMsg(e)); }
  }, [id]);

  useEffect(() => {
    load();
    api.get(`/fields/${id}/weather`).then((r) => setWeather(r.data)).catch(() => setWeather(null));
  }, [id, load]);

  const run = async () => {
    setBusy(true); setError(''); setInfo('');
    try { await api.post(`/fields/${id}/predict`); await load(); setInfo('AI prediction updated.'); }
    catch (e) { setError(errMsg(e)); } finally { setBusy(false); }
  };

  const simulate = async () => {
    setBusy(true); setError(''); setInfo('');
    try {
      await api.post(`/fields/${id}/simulate`, { scenario, days: 10 });
      await api.post(`/fields/${id}/predict`);
      await load();
      setInfo('Simulated 10 days of sensor data and ran the prediction engine.');
    } catch (e) { setError(errMsg(e)); } finally { setBusy(false); }
  };

  const addReading = async (e) => {
    e.preventDefault();
    setBusy(true); setError(''); setInfo('');
    const body = {};
    Object.entries(form).forEach(([k, v]) => { body[k] = v === '' ? null : parseFloat(v); });
    try {
      await api.post(`/fields/${id}/readings`, body);
      setForm(blank); setShowForm(false);
      await load();
      setInfo('Reading saved. Click "Run AI prediction" to refresh the forecast.');
    } catch (e2) { setError(errMsg(e2)); } finally { setBusy(false); }
  };

  const feedback = async (pid, outcome, actualThreat) => {
    setError('');
    try {
      await api.post(`/predictions/${pid}/feedback`, { outcome, actualThreat: actualThreat || null });
      await load();
      setInfo('Thanks! Your feedback was used to tune the model.');
    } catch (e) { setError(errMsg(e)); }
  };

  if (!field) return error ? <div className="alert-box error">{error}</div> : <p className="muted">Loading…</p>;

  const latest = predictions[0];
  const scores = latest?.scoresJson ? JSON.parse(latest.scoresJson) : null;
  const chartData = readings.map((r) => ({
    day: fmtDay(r.recordedAt), moisture: r.soilMoisture, temp: r.temperature, humidity: r.humidity, ndvi: r.ndvi
  }));

  return (
    <>
      <div className="page-head">
        <div>
          <Link to="/fields" className="muted small">← My fields</Link>
          <h2>{field.name} <span className="muted">· {field.cropType}</span></h2>
        </div>
        <button className="btn btn-primary" onClick={run} disabled={busy}>{busy ? 'Working…' : '⚡ Run AI prediction'}</button>
      </div>

      {error && <div className="alert-box error">{error}</div>}
      {info && <div className="alert-box ok">{info}</div>}

      {latest ? (
        <div className={`card hero risk-${latest.riskLevel.toLowerCase()}`}>
          <div className="row-between wrap">
            <div>
              <RiskBadge level={latest.riskLevel} big />
              <h3 className="hero-title">
                {latest.riskLevel === 'GREEN' ? 'No emergency expected' : pretty(latest.threat)}
              </h3>
              {latest.riskLevel !== 'GREEN' && (
                <p>Severity <b>{latest.severity}</b> · expected in about <b>{latest.daysToImpact}</b> day(s) · score <b>{Math.round(latest.riskScore)}</b>/100</p>
              )}
              <p className="reco">💡 {latest.recommendation}</p>
              <p className="muted small">Predicted {fmtDate(latest.createdAt)}</p>
            </div>
            <div className="indicators">
              <div><span>Moisture deficit</span><b>{(latest.moistureDeficit * 100).toFixed(0)}%</b></div>
              <div><span>Heat-stress index</span><b>{latest.heatStressIndex.toFixed(2)}</b></div>
              <div><span>NDVI decline / day</span><b>{latest.ndviDeclineRate.toFixed(3)}</b></div>
              {latest.forecastMaxTemp != null && <div><span>Forecast max temp</span><b>{latest.forecastMaxTemp}°C</b></div>}
              {latest.forecastRain3d != null && <div><span>Forecast rain (3d)</span><b>{latest.forecastRain3d} mm</b></div>}
            </div>
          </div>
          {scores && (
            <div className="bars">
              {Object.entries(scores).map(([k, v]) => (
                <div key={k} className="bar-row">
                  <span>{pretty(k)}</span>
                  <div className="bar"><div className={`bar-fill ${v >= 65 ? 'f-red' : v >= 35 ? 'f-amber' : 'f-green'}`} style={{ width: `${v}%` }} /></div>
                  <b>{Math.round(v)}</b>
                </div>
              ))}
            </div>
          )}
        </div>
      ) : (
        <div className="card center">
          <p>No prediction yet. Generate demo data below or add a reading, then run the AI engine.</p>
        </div>
      )}

      <div className="two-col">
        <div className="card">
          <h3>Data input</h3>
          <p className="muted small">No hardware? Simulate a realistic scenario to see the early-warning system in action.</p>
          <div className="row-gap wrap">
            <select value={scenario} onChange={(e) => setScenario(e.target.value)}>
              {SCENARIOS.map(([k, l]) => <option key={k} value={k}>{l}</option>)}
            </select>
            <button className="btn btn-ghost" onClick={simulate} disabled={busy}>Simulate &amp; predict</button>
            <button className="btn btn-ghost" onClick={() => setShowForm(!showForm)}>{showForm ? 'Close form' : '+ Add reading'}</button>
          </div>
        </div>
        <div className="card">
          <h3>3-day weather outlook</h3>
          {weather ? (
            <div className="indicators">
              <div><span>Max temp</span><b>{weather.maxTemp}°C</b></div>
              <div><span>Min temp</span><b>{weather.minTemp}°C</b></div>
              <div><span>Rain</span><b>{weather.rain3d} mm</b></div>
              <div><span>Max wind</span><b>{weather.maxWind} km/h</b></div>
              <div><span>Humidity</span><b>{weather.avgHumidity}%</b></div>
            </div>
          ) : <p className="muted">Forecast unavailable (offline or service down). Predictions still run on sensor data.</p>}
          <p className="muted small">Source: Open-Meteo</p>
        </div>
      </div>

      {showForm && (
        <form className="card form-grid" onSubmit={addReading}>
          {Object.keys(blank).map((k) => (
            <div key={k}>
              <label>{LABELS[k]}</label>
              <input type="number" step="any" value={form[k]} required={LABELS[k].endsWith('*')}
                     onChange={(e) => setForm({ ...form, [k]: e.target.value })} />
            </div>
          ))}
          <div className="form-actions"><button className="btn btn-primary" disabled={busy}>Save reading</button></div>
        </form>
      )}

      {chartData.length > 0 && (
        <div className="two-col">
          <div className="card">
            <h3>Soil moisture, temperature &amp; humidity</h3>
            <div className="chart">
              <ResponsiveContainer width="100%" height="100%">
                <LineChart data={chartData}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="day" fontSize={12} />
                  <YAxis fontSize={12} />
                  <Tooltip /><Legend />
                  <Line type="monotone" dataKey="moisture" name="Moisture %" stroke="#1d70b8" dot={false} strokeWidth={2} />
                  <Line type="monotone" dataKey="temp" name="Temp °C" stroke="#d93636" dot={false} strokeWidth={2} />
                  <Line type="monotone" dataKey="humidity" name="Humidity %" stroke="#7a5cc4" dot={false} strokeWidth={2} />
                </LineChart>
              </ResponsiveContainer>
            </div>
          </div>
          <div className="card">
            <h3>NDVI (vegetation health)</h3>
            <div className="chart">
              <ResponsiveContainer width="100%" height="100%">
                <LineChart data={chartData}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="day" fontSize={12} />
                  <YAxis domain={[0, 1]} fontSize={12} />
                  <Tooltip />
                  <Line type="monotone" dataKey="ndvi" name="NDVI" stroke="#2e9e5b" strokeWidth={3} dot={{ r: 3 }} />
                </LineChart>
              </ResponsiveContainer>
            </div>
          </div>
        </div>
      )}

      <div className="card">
        <h3>Prediction history &amp; feedback</h3>
        <p className="muted small">Tell the system what really happened - this feedback retrains its sensitivity so accuracy improves every season.</p>
        <div className="table-wrap">
          <table>
            <thead><tr><th>When</th><th>Risk</th><th>Threat</th><th>Score</th><th>In (days)</th><th>Actual outcome</th></tr></thead>
            <tbody>
              {predictions.map((p) => (
                <tr key={p.id}>
                  <td>{fmtDate(p.createdAt)}</td>
                  <td><RiskBadge level={p.riskLevel} /></td>
                  <td>{p.riskLevel === 'GREEN' ? '—' : pretty(p.threat)}</td>
                  <td>{Math.round(p.riskScore)}</td>
                  <td>{p.riskLevel === 'GREEN' ? '—' : p.daysToImpact}</td>
                  <td><FeedbackCell p={p} onSubmit={feedback} /></td>
                </tr>
              ))}
              {predictions.length === 0 && <tr><td colSpan="6" className="muted center">No predictions yet</td></tr>}
            </tbody>
          </table>
        </div>
      </div>
    </>
  );
}
