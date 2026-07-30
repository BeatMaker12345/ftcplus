interface Props {
  signals: { signalClass: string; timestamp: number }[]
}

export default function SignalLog({ signals }: Props) {
  return (
    <div style={styles.container}>
      <div style={styles.title}>Signal Log</div>
      <div style={styles.list}>
        {signals.length === 0 && (
          <div style={styles.empty}>No signals yet.</div>
        )}
        {signals.map((s, i) => (
          <div key={i} style={styles.entry}>
            <span style={styles.time}>
              {new Date(s.timestamp).toLocaleTimeString('en-US', {
                hour12: false,
                hour: '2-digit',
                minute: '2-digit',
                second: '2-digit',
              })}
            </span>
            <span style={styles.signal}>{s.signalClass}</span>
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
  list: {
    display: 'flex',
    flexDirection: 'column',
    gap: 2,
  },
  empty: {
    color: '#555',
    fontSize: '0.8rem',
  },
  entry: {
    display: 'flex',
    gap: 12,
    padding: '2px 0',
    borderBottom: '1px solid #2a2a2a',
  },
  time: {
    color: '#555',
    fontSize: '0.75rem',
    minWidth: 80,
  },
  signal: {
    color: '#00aaff',
    fontSize: '0.8rem',
  },
}
