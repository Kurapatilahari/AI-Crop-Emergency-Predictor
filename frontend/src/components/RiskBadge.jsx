import { RISK_LABEL } from '../utils';

export default function RiskBadge({ level, big }) {
  const l = level || 'UNKNOWN';
  return <span className={`badge badge-${l.toLowerCase()} ${big ? 'badge-big' : ''}`}>{RISK_LABEL[l] || l}</span>;
}
