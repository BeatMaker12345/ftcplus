import { WebSocketServer } from 'ws'
import { createServer } from 'http'

const server = createServer((req, res) => {
    res.writeHead(426, { 'Content-Type': 'text/plain' })
    res.end('Upgrade Required')
})

const wss = new WebSocketServer({ server, path: '/ws' })

wss.on('connection', (ws) => {
    console.log('client connected')

    ws.send(JSON.stringify({
        type: 'COMPONENT_TREE',
        root: {
            path: 'robot', name: 'Robot', kind: 'robot', state: null,
            children: [
                {
                    path: 'robot.drive', name: 'Drive', kind: 'subsystem', state: 'DRIVING',
                    children: []
                },
                {
                    path: 'robot.intake', name: 'Intake', kind: 'subsystem', state: 'IDLE',
                    children: [
                        { path: 'robot.intake.motor', name: 'IntakeMotor', kind: 'hardware', children: [] }
                    ]
                },
                {
                    path: 'robot.flywheel', name: 'Flywheel', kind: 'subsystem', state: 'SPINNING_UP',
                    children: []
                }
            ]
        }
    }))

    let t = 0
    const interval = setInterval(() => {
        t += 0.05
        ws.send(JSON.stringify({
            type: 'TELEMETRY',
            entries: [
                {
                    kind: 'panel', name: 'Drive',
                    children: [
                        { kind: 'line', value: 'State: DRIVING' },
                        { kind: 'graph', key: 'Velocity', value: Math.abs(Math.sin(t)) * 45, min: 0, max: 50, unit: 'cm/s', color: '#00aaff' }
                    ]
                },
                {
                    kind: 'panel', name: 'Flywheel',
                    children: [
                        { kind: 'line', value: 'State: SPINNING_UP' },
                        { kind: 'graph', key: 'RPM', value: 800 + Math.sin(t * 2) * 200, min: 0, max: 2000, color: '#ff6600' }
                    ]
                },
                {
                    kind: 'field',
                    objects: [
                        { kind: 'robot', x: Math.sin(t) * 30, y: Math.cos(t) * 30, z: t }
                    ]
                }
            ]
        }))

        ws.send(JSON.stringify({
            type: 'POWER',
            totalAmps: 8 + Math.sin(t) * 3,
            maxAmps: 20,
            utilizationPercent: (8 + Math.sin(t) * 3) / 20 * 100,
            isOverBudget: false,
            components: [
                { path: 'robot.drive.frontLeft', amps: 2.1 },
                { path: 'robot.drive.frontRight', amps: 1.9 },
                { path: 'robot.flywheel', amps: 3.5 }
            ]
        }))

        if (Math.random() < 0.05) {
            ws.send(JSON.stringify({
                type: 'SIGNAL',
                signalClass: ['IntakeRequested', 'ShootRequested', 'ForceStop', 'FlywheelReady'][Math.floor(Math.random() * 4)],
                timestamp: Date.now()
            }))
        }
    }, 100)

    ws.on('close', () => clearInterval(interval))
})

server.listen(7273, () => {
    console.log('Mock FTC+ server running on ws://localhost:7273')
})