import './Badge.css';
export default function Badge({ label, children, variant = 'default' }) {
  return <span className={`badge badge--${variant}`}>{children ?? label}</span>;
}
