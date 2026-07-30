import type { ComponentNode } from '../ws/DashboardClient'
import { Bot, Settings, Eye, Wrench, Circle } from 'lucide-react'

interface Props {
  tree: ComponentNode | null
}

export default function ComponentTree({ tree }: Props) {
  return (
    <div style={styles.container}>
      <div style={styles.title}>Component Tree</div>
      {!tree ? (
        <div style={styles.empty}>Not connected.</div>
      ) : (
        <TreeNode node={tree} depth={0} />
      )}
    </div>
  )
}

function TreeNode({ node, depth }: { node: ComponentNode; depth: number }) {
  return (
    <div style={{ paddingLeft: depth * 16 }}>
      <div style={styles.node}>
        {kindIcon(node.kind)}
        <span style={styles.nodeName}>{node.name}</span>
        {node.state && (
            <span style={styles.nodeState}>{node.state}</span>
        )}
      </div>
      {node.children.map((child, i) => (
        <TreeNode key={i} node={child} depth={depth + 1} />
      ))}
    </div>
  )
}

function kindIcon(kind: string) {
  const size = 12
  switch (kind) {
    case 'robot':     return <Bot size={size} color={"#00aaff"} />
    case 'subsystem': return <Settings size={size} color={"#00cc66"} />
    case 'sensor':    return <Eye size={size} color={"#ffaa00"} />
    case 'hardware':  return <Wrench size={size} color={"#aa88ff"} />
    default:          return <Circle size={size} color={"#888"} />
  }
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
  node: {
    display: 'flex',
    alignItems: 'center',
    gap: 6,
    padding: '2px 0',
  },
  nodeName: {
    color: '#e0e0e0',
    fontSize: '0.8rem',
  },
  nodeState: {
    color: '#555',
    fontSize: '0.75rem',
    marginLeft: 4,
    background: '#2a2a2a',
    padding: '0 4px',
    borderRadius: 3,
  },
}
