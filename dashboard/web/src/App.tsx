import { useEffect, useState } from 'react'
import Field3D from './components/Field3D'
import TelemetryPanel from './components/TelemetryPanel'
import SignalLog from './components/SignalLog'
import PowerPanel from './components/PowerPanel'
import ComponentTree from './components/ComponentTree'
import { dashboardClient, type DashboardState } from './ws/DashboardClient'
import type { FieldObject, TelemetryEntry } from './ws/DashboardClient'

const WS_URL = (() => {
  const host = window.location.hostname === 'localhost' ? 'localhost' : window.location.hostname
  return `ws://${host}:7273/ws`
})()

type Panel = 'telemetry' | 'signals' | 'power' | 'tree'

export default function App() {
  const [state, setState] = useState<DashboardState>(dashboardClient.state)
  const [activePanel, setActivePanel] = useState<Panel>('telemetry')

  useEffect(() => {
    dashboardClient.connect(WS_URL)
    const unsub = dashboardClient.subscribe(setState)
    return () => {
      unsub()
      dashboardClient.disconnect()
    }
  }, [])

  const fieldObjects: FieldObject[] = state.telemetry
    .filter((e): e is TelemetryEntry & { kind: 'field' } => e.kind === 'field')
    .flatMap(e => e.objects ?? [])

  return (
    <div style={styles.root}>
      <div style={styles.header}>
        <div style={styles.logo}>FTC+</div>
        <div style={styles.status}>
          <div style={{
            ...styles.statusDot,
            background: state.connected ? '#00cc66' : '#ff4444',
          }} />
          <span style={styles.statusText}>
            {state.connected ? 'Connected' : 'Disconnected'}
          </span>
        </div>
        <div style={styles.tabs}>
          {(['telemetry', 'signals', 'power', 'tree'] as Panel[]).map(p => (
            <button
              key={p}
              style={{
                ...styles.tab,
                ...(activePanel === p ? styles.tabActive : {}),
              }}
              onClick={() => setActivePanel(p)}
            >
              {p}
            </button>
          ))}
        </div>
      </div>

      <div style={styles.main}>
        <div style={styles.field}>
          <Field3D objects={fieldObjects} />
        </div>

        <div style={styles.side}>
          {activePanel === 'telemetry' && (
            <TelemetryPanel entries={state.telemetry} />
          )}
          {activePanel === 'signals' && (
            <SignalLog signals={state.signals} />
          )}
          {activePanel === 'power' && (
            <PowerPanel power={state.power} />
          )}
          {activePanel === 'tree' && (
            <ComponentTree tree={state.tree} />
          )}
        </div>
      </div>
    </div>
  )
}

const styles: Record<string, React.CSSProperties> = {
  root: {
    display: 'flex',
    flexDirection: 'column',
    height: '100vh',
    background: '#121212',
    color: '#e0e0e0',
    fontFamily: 'system-ui, sans-serif',
    overflow: 'hidden',
  },
  header: {
    display: 'flex',
    alignItems: 'center',
    gap: 16,
    padding: '8px 16px',
    background: '#1a1a1a',
    borderBottom: '1px solid #2a2a2a',
    flexShrink: 0,
  },
  logo: {
    fontWeight: 'bold',
    fontSize: '1.1rem',
    color: '#00aaff',
    letterSpacing: '0.05em',
  },
  status: {
    display: 'flex',
    alignItems: 'center',
    gap: 6,
  },
  statusDot: {
    width: 8,
    height: 8,
    borderRadius: '50%',
  },
  statusText: {
    fontSize: '0.8rem',
    color: '#888',
  },
  tabs: {
    display: 'flex',
    gap: 4,
    marginLeft: 'auto',
  },
  tab: {
    background: 'none',
    border: '1px solid #333',
    borderRadius: 4,
    color: '#888',
    padding: '4px 12px',
    fontSize: '0.8rem',
    cursor: 'pointer',
    textTransform: 'capitalize',
  },
  tabActive: {
    background: '#00aaff22',
    borderColor: '#00aaff',
    color: '#00aaff',
  },
  main: {
    display: 'flex',
    flex: 1,
    overflow: 'hidden',
  },
  field: {
    flex: 1,
    overflow: 'hidden',
  },
  side: {
    width: 320,
    borderLeft: '1px solid #2a2a2a',
    overflow: 'hidden',
  },
}
