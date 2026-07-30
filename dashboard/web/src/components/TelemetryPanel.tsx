import type { TelemetryEntry } from '../ws/DashboardClient'

interface Props {
  entries: TelemetryEntry[]
}

export default function TelemetryPanel({ entries }: Props) {
  return (
    <div style={styles.container}>
      <div style={styles.title}>Telemetry</div>
      <div style={styles.entries}>
        {entries.map((entry, i) => (
          <TelemetryEntryView key={i} entry={entry} depth={0} />
        ))}
      </div>
    </div>
  )
}

function TelemetryEntryView({ entry, depth }: { entry: TelemetryEntry; depth: number }) {
  switch (entry.kind) {
    case 'line':
      return (
        <div style={{
          ...styles.line,
          color: entry.color ?? '#e0e0e0',
          fontWeight: entry.bold ? 'bold' : 'normal',
          fontStyle: entry.italic ? 'italic' : 'normal',
          fontSize: entry.size ? `${entry.size}em` : '0.875rem',
          paddingLeft: depth * 12,
        }}>
          {entry.value}
        </div>
      )

    case 'divider':
      return <div style={styles.divider} />

    case 'panel':
      return (
        <div style={styles.panel}>
          <div style={styles.panelTitle}>{entry.name}</div>
          {entry.children?.map((child, i) => (
            <TelemetryEntryView key={i} entry={child} depth={depth + 1} />
          ))}
        </div>
      )

    case 'graph':
      return (
        <div style={styles.graphRow}>
          <span style={styles.graphKey}>{entry.key}</span>
          <div style={styles.graphBar}>
            <div style={{
              ...styles.graphFill,
              width: `${Math.min(100, ((Number(entry.value) - (entry.min ?? 0)) / ((entry.max ?? 100) - (entry.min ?? 0))) * 100)}%`,
              background: entry.color ?? '#00aaff',
            }} />
          </div>
          <span style={styles.graphValue}>
            {Number(entry.value).toFixed(1)}{entry.unit ? ` ${entry.unit}` : ''}
          </span>
        </div>
      )

    case 'table':
      return (
        <div style={styles.table}>
          <div style={styles.tableName}>{entry.name}</div>
          {entry.rows?.map((row, ri) => (
            <div key={ri} style={styles.tableRow}>
              {row.items.map((item, ii) => (
                <div key={ii} style={styles.tableCell}>
                  <span style={styles.tableCellLabel}>{item.label}</span>
                  <span style={styles.tableCellValue}>{item.value}</span>
                </div>
              ))}
            </div>
          ))}
        </div>
      )

    default:
      return null
  }
}

const styles: Record<string, React.CSSProperties> = {
  container: {
    background: '#1e1e1e',
    borderRadius: 8,
    padding: '12px',
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
  entries: {
    display: 'flex',
    flexDirection: 'column',
    gap: 2,
  },
  line: {
    padding: '2px 0',
    whiteSpace: 'pre',
  },
  divider: {
    height: 1,
    background: '#333',
    margin: '4px 0',
  },
  panel: {
    border: '1px solid #333',
    borderRadius: 4,
    padding: '8px',
    marginBottom: 4,
  },
  panelTitle: {
    color: '#aaa',
    fontSize: '0.75rem',
    fontWeight: 'bold',
    textTransform: 'uppercase',
    marginBottom: 4,
  },
  graphRow: {
    display: 'flex',
    alignItems: 'center',
    gap: 8,
    padding: '2px 0',
  },
  graphKey: {
    color: '#aaa',
    fontSize: '0.8rem',
    minWidth: 80,
  },
  graphBar: {
    flex: 1,
    height: 8,
    background: '#333',
    borderRadius: 4,
    overflow: 'hidden',
  },
  graphFill: {
    height: '100%',
    borderRadius: 4,
    transition: 'width 0.1s',
  },
  graphValue: {
    color: '#e0e0e0',
    fontSize: '0.8rem',
    minWidth: 60,
    textAlign: 'right',
  },
  table: {
    border: '1px solid #333',
    borderRadius: 4,
    padding: 8,
    marginBottom: 4,
  },
  tableName: {
    color: '#aaa',
    fontSize: '0.75rem',
    marginBottom: 4,
  },
  tableRow: {
    display: 'flex',
    gap: 16,
    padding: '2px 0',
    borderTop: '1px solid #2a2a2a',
  },
  tableCell: {
    display: 'flex',
    gap: 8,
    flex: 1,
  },
  tableCellLabel: {
    color: '#888',
    fontSize: '0.8rem',
  },
  tableCellValue: {
    color: '#e0e0e0',
    fontSize: '0.8rem',
  },
}
