import type { DashboardState } from '../ws/DashboardClient'
import { AlertTriangle } from "lucide-react";

interface Props {
  power: DashboardState['power']
}

export default function PowerPanel({ power }: Props) {
  if (!power) {
    return (
      <div style={styles.container}>
        <div style={styles.title}>Power Budget</div>
        <div style={styles.empty}>No data yet.</div>
      </div>
    )
  }

  const pct = Math.min(100, power.utilizationPercent)
  const barColor = power.isOverBudget ? '#ff4444' : pct > 80 ? '#ffaa00' : '#00cc66'

  return (
    <div style={styles.container}>
      <div style={styles.title}>Power Budget</div>
      <div style={styles.total}>
        <span style={{ color: barColor }}>
          {power.totalAmps.toFixed(2)}A
        </span>
        <span style={styles.max}> / {power.maxAmps.toFixed(1)}A</span>
      </div>
      <div style={styles.bar}>
        <div style={{
          ...styles.barFill,
          width: `${pct}%`,
          background: barColor,
        }} />
      </div>
      {power.isOverBudget && (
          <div style={styles.warning}>
            <AlertTriangle size={14} /> Over budget
          </div>
      )}
      <div style={styles.components}>
        {power.components
          .filter(c => c.amps > 0)
          .sort((a, b) => b.amps - a.amps)
          .map((c, i) => (
            <div key={i} style={styles.component}>
              <span style={styles.componentPath}>{c.path}</span>
              <span style={styles.componentAmps}>{c.amps.toFixed(2)}A</span>
            </div>
          ))}
      </div>
    </div>
  )
}

const styles: Record<string, React.CSSProperties> = {
  container: {
    background: '#1e1e1e',
    borderRadius: 8,
    padding: 12,
    height: '100%',
    overflowY: 'auto',
    fontFamily: 'monospace',
  },
  title: {
    color: '#888',
    fontSize: '0.75rem',
    textTransform: 'uppercase',
    letterSpacing: '0.05em',
    marginBottom: 8,
  },
  empty: { color: '#555', fontSize: '0.8rem' },
  total: { fontSize: '1.5rem', fontWeight: 'bold', marginBottom: 8 },
  max: { color: '#666', fontSize: '1rem' },
  bar: {
    height: 8,
    background: '#333',
    borderRadius: 4,
    overflow: 'hidden',
    marginBottom: 8,
  },
  barFill: {
    height: '100%',
    borderRadius: 4,
    transition: 'width 0.2s, background 0.2s',
  },
  warning: {
    color: '#ff4444',
    fontSize: '0.8rem',
    marginBottom: 8,
  },
  components: {
    display: 'flex',
    flexDirection: 'column',
    gap: 2,
    marginTop: 8,
  },
  component: {
    display: 'flex',
    justifyContent: 'space-between',
    padding: '2px 0',
    borderBottom: '1px solid #2a2a2a',
  },
  componentPath: {
    color: '#aaa',
    fontSize: '0.75rem',
  },
  componentAmps: {
    color: '#e0e0e0',
    fontSize: '0.75rem',
  },
}
