import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import api, { errMsg } from '../api';

const empty = { name: '', cropType: 'Rice', latitude: '', longitude: '', areaAcres: '' };

export default function Fields() {
  const [fields, setFields] = useState([]);
  const [crops, setCrops] = useState([]);
  const [form, setForm] = useState(empty);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  const load = () =>
    api.get('/fields').then((r) => setFields(r.data)).catch((e) => setError(errMsg(e))).finally(() => setLoading(false));

  useEffect(() => {
    load();
    api.get('/crops').then((r) => setCrops(r.data)).catch(() => {});
  }, []);

  const locate = () => {
    if (!navigator.geolocation) return setError('Geolocation is not supported by this browser');
    navigator.geolocation.getCurrentPosition(
      (p) => setForm((f) => ({ ...f, latitude: p.coords.latitude.toFixed(4), longitude: p.coords.longitude.toFixed(4) })),
      () => setError('Could not get your location - enter coordinates manually')
    );
  };

  const submit = async (e) => {
    e.preventDefault();
    setError('');
    try {
      await api.post('/fields', {
        name: form.name,
        cropType: form.cropType,
        latitude: parseFloat(form.latitude),
        longitude: parseFloat(form.longitude),
        areaAcres: form.areaAcres ? parseFloat(form.areaAcres) : null
      });
      setForm(empty);
      load();
    } catch (err) { setError(errMsg(err)); }
  };

  const remove = async (id) => {
    if (!window.confirm('Delete this field and all its data?')) return;
    try { await api.delete(`/fields/${id}`); load(); } catch (err) { setError(errMsg(err)); }
  };

  return (
    <>
      <div className="page-head"><h2>My Fields</h2></div>
      {error && <div className="alert-box error">{error}</div>}

      <div className="card">
        <h3>Add a field</h3>
        <form onSubmit={submit} className="form-grid">
          <div><label>Field name</label><input value={form.name} onChange={set('name')} required placeholder="e.g. East plot" /></div>
          <div>
            <label>Crop</label>
            <select value={form.cropType} onChange={set('cropType')}>
              {(crops.length ? crops : ['Rice']).map((c) => <option key={c}>{c}</option>)}
            </select>
          </div>
          <div><label>Latitude</label><input type="number" step="any" value={form.latitude} onChange={set('latitude')} required /></div>
          <div><label>Longitude</label><input type="number" step="any" value={form.longitude} onChange={set('longitude')} required /></div>
          <div><label>Area (acres)</label><input type="number" step="any" value={form.areaAcres} onChange={set('areaAcres')} /></div>
          <div className="form-actions">
            <button type="button" className="btn btn-ghost" onClick={locate}>📍 Use my location</button>
            <button className="btn btn-primary">Add field</button>
          </div>
        </form>
      </div>

      {loading ? <p className="muted">Loading…</p> : (
        <div className="grid">
          {fields.map((f) => (
            <div className="card" key={f.id}>
              <h3>{f.name}</h3>
              <p className="muted">{f.cropType} · {f.areaAcres ? `${f.areaAcres} acres` : 'area not set'}</p>
              <p className="muted small">📍 {f.latitude}, {f.longitude}</p>
              <div className="row-gap">
                <Link to={`/fields/${f.id}`} className="btn btn-primary btn-small">Open</Link>
                <button className="btn btn-danger btn-small" onClick={() => remove(f.id)}>Delete</button>
              </div>
            </div>
          ))}
          {fields.length === 0 && <p className="muted">No fields yet - add one above.</p>}
        </div>
      )}
    </>
  );
}
