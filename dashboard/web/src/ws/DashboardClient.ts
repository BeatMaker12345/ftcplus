export type MessageType =
  | 'COMPONENT_TREE'
  | 'TELEMETRY'
  | 'SIGNAL'
  | 'POWER'
  | 'SETTINGS'
  | 'DIAGNOSTIC_RESULT'
  | 'CALIBRATION_RESULT'

export interface ComponentNode {
  path: string
  name: string
  kind: 'robot' | 'subsystem' | 'sensor' | 'hardware' | 'component'
  state?: string
  children: ComponentNode[]
}

export interface TelemetryEntry {
  kind: 'line' | 'divider' | 'panel' | 'graph' | 'table' | 'field'
  value?: string
  key?: string
  color?: string
  bold?: boolean
  italic?: boolean
  size?: number
  name?: string
  children?: TelemetryEntry[]
  rows?: { items: { label: string; value: string }[] }[]
  objects?: FieldObject[]
  min?: number
  max?: number
  unit?: string
}

export interface FieldObject {
  kind: 'robot' | 'point' | 'axis' | 'line'
  name?: string
  x?: number
  y?: number
  z?: number
  value?: number
  axis?: 'X' | 'Y' | 'Z'
}

export interface PowerEntry {
  path: string
  amps: number
}

export interface DashboardState {
  connected: boolean
  tree: ComponentNode | null
  telemetry: TelemetryEntry[]
  signals: { signalClass: string; timestamp: number }[]
  power: {
    totalAmps: number
    maxAmps: number
    utilizationPercent: number
    isOverBudget: boolean
    components: PowerEntry[]
  } | null
  settings: Record<string, unknown>
}

type Listener = (state: DashboardState) => void

class DashboardClient {
  private ws: WebSocket | null = null
  private listeners: Set<Listener> = new Set()
  private reconnectTimer: number | null = null

  state: DashboardState = {
    connected: false,
    tree: null,
    telemetry: [],
    signals: [],
    power: null,
    settings: {},
  }

  connect(url: string) {
    if (this.ws) {
      this.ws.close()
    }

    this.ws = new WebSocket(url)

    this.ws.onopen = () => {
      this.state = { ...this.state, connected: true }
      this.notify()
    }

    this.ws.onclose = () => {
      this.state = { ...this.state, connected: false }
      this.notify()
      // reconnect after 2s
      this.reconnectTimer = window.setTimeout(() => this.connect(url), 2000)
    }

    this.ws.onerror = () => {
      this.ws?.close()
    }

    this.ws.onmessage = (e) => {
      try {
        const msg = JSON.parse(e.data)
        this.handleMessage(msg)
      } catch {
        // ignore malformed messages
      }
    }
  }

  private handleMessage(msg: Record<string, unknown>) {
    switch (msg.type) {
      case 'COMPONENT_TREE':
        this.state = { ...this.state, tree: msg.root as ComponentNode }
        break

      case 'TELEMETRY':
        this.state = { ...this.state, telemetry: (msg.entries as TelemetryEntry[]) ?? [] }
        break

      case 'SIGNAL':
        this.state = {
          ...this.state,
          signals: [
            { signalClass: msg.signalClass as string, timestamp: msg.timestamp as number },
            ...this.state.signals.slice(0, 99),
          ],
        }
        break

      case 'POWER':
        this.state = {
          ...this.state,
          power: {
            totalAmps: msg.totalAmps as number,
            maxAmps: msg.maxAmps as number,
            utilizationPercent: msg.utilizationPercent as number,
            isOverBudget: msg.isOverBudget as boolean,
            components: msg.components as PowerEntry[],
          },
        }
        break

      case 'SETTINGS':
        this.state = { ...this.state, settings: msg.settings as Record<string, unknown> }
        break
    }

    this.notify()
  }

  send(msg: Record<string, unknown>) {
    if (this.ws?.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(msg))
    }
  }

  setSetting(className: string, field: string, value: unknown) {
    this.send({ type: 'SET_SETTING', class: className, field, value })
  }

  subscribe(listener: Listener) {
    this.listeners.add(listener)
    return () => this.listeners.delete(listener)
  }

  private notify() {
    this.listeners.forEach((l) => l(this.state))
  }

  disconnect() {
    if (this.reconnectTimer !== null) {
      clearTimeout(this.reconnectTimer)
    }
    this.ws?.close()
  }
}

export const dashboardClient = new DashboardClient()
