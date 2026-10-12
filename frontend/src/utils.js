export const pretty = (s) =>
  !s ? '' : s.toLowerCase().split('_').map((w) => w[0].toUpperCase() + w.slice(1)).join(' ');

export const fmtDate = (d) =>
  d ? new Date(d).toLocaleString([], { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit' }) : '';

export const fmtDay = (d) => (d ? new Date(d).toLocaleDateString([], { day: '2-digit', month: 'short' }) : '');

export const THREATS = [
  'DROUGHT', 'HEAT_STRESS', 'WATERLOGGING', 'NUTRIENT_DEFICIENCY', 'PEST_OUTBREAK', 'SALINITY_STRESS'
];

export const RISK_LABEL = { GREEN: 'Safe', AMBER: 'Watch closely', RED: 'Act immediately', UNKNOWN: 'No data' };
