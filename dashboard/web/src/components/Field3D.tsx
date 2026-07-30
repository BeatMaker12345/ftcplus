import { useEffect, useRef } from 'react'
import * as THREE from 'three'
import { GLTFLoader } from 'three/examples/jsm/loaders/GLTFLoader.js'
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js'
import type { FieldObject } from '../ws/DashboardClient'

interface Props {
  objects: FieldObject[]
}

// FTC field dimensions in inches
const FIELD_SIZE = 141.0

export default function Field3D({ objects }: Props) {
  const mountRef = useRef<HTMLDivElement>(null)
  const sceneRef = useRef<THREE.Scene | null>(null)
  const rendererRef = useRef<THREE.WebGLRenderer | null>(null)
  const cameraRef = useRef<THREE.PerspectiveCamera | null>(null)
  const controlsRef = useRef<OrbitControls | null>(null)
  const objectsGroupRef = useRef<THREE.Group | null>(null)
  const frameRef = useRef<number>(0)

  useEffect(() => {
    if (!mountRef.current) return

    const scene = new THREE.Scene()
    scene.background = new THREE.Color(0x1a1a1a)
    sceneRef.current = scene

    const camera = new THREE.PerspectiveCamera(
      50,
      mountRef.current.clientWidth / mountRef.current.clientHeight,
      0.1,
      10000
    )
    camera.position.set(0, 200, 200)
    camera.lookAt(0, 0, 0)
    cameraRef.current = camera

    const renderer = new THREE.WebGLRenderer({ antialias: true })
    renderer.setPixelRatio(window.devicePixelRatio)
    renderer.setSize(mountRef.current.clientWidth, mountRef.current.clientHeight)
    renderer.shadowMap.enabled = true
    mountRef.current.appendChild(renderer.domElement)
    rendererRef.current = renderer

    const controls = new OrbitControls(camera, renderer.domElement)
    controls.enableDamping = true
    controls.dampingFactor = 0.05
    controlsRef.current = controls

    scene.add(new THREE.AmbientLight(0xffffff, 0.6))
    const dirLight = new THREE.DirectionalLight(0xffffff, 0.8)
    dirLight.position.set(100, 200, 100)
    dirLight.castShadow = true
    scene.add(dirLight)

    const floorGeo = new THREE.PlaneGeometry(FIELD_SIZE, FIELD_SIZE)
    const floorMat = new THREE.MeshLambertMaterial({ color: 0x2a2a2a })
    const floor = new THREE.Mesh(floorGeo, floorMat)
    floor.rotation.x = -Math.PI / 2
    floor.receiveShadow = true
    scene.add(floor)

    // field border
    const borderGeo = new THREE.EdgesGeometry(
      new THREE.BoxGeometry(FIELD_SIZE, 2, FIELD_SIZE)
    )
    const borderMat = new THREE.LineBasicMaterial({ color: 0x444444 })
    const border = new THREE.LineSegments(borderGeo, borderMat)
    border.position.y = 1
    scene.add(border)

    // grid
    const grid = new THREE.GridHelper(FIELD_SIZE, 12, 0x333333, 0x333333)
    scene.add(grid)

    const loader = new GLTFLoader()
    loader.load(
      '/fields/decode-2025.glb',
      (gltf) => {
        const model = gltf.scene
        model.traverse((child) => {
          if (child instanceof THREE.Mesh) {
            child.receiveShadow = true
            child.castShadow = true
          }
        })
        scene.add(model)
      },
      undefined,
      () => {
        addPlaceholderField(scene)
      }
    )

    const objectsGroup = new THREE.Group()
    scene.add(objectsGroup)
    objectsGroupRef.current = objectsGroup

    const animate = () => {
      frameRef.current = requestAnimationFrame(animate)
      controls.update()
      renderer.render(scene, camera)
    }
    animate()

    const onResize = () => {
      if (!mountRef.current) return
      const w = mountRef.current.clientWidth
      const h = mountRef.current.clientHeight
      camera.aspect = w / h
      camera.updateProjectionMatrix()
      renderer.setSize(w, h)
    }
    window.addEventListener('resize', onResize)

    return () => {
      window.removeEventListener('resize', onResize)
      cancelAnimationFrame(frameRef.current)
      renderer.dispose()
      mountRef.current?.removeChild(renderer.domElement)
    }
  }, [])

  useEffect(() => {
    const group = objectsGroupRef.current
    if (!group) return

    while (group.children.length > 0) {
      group.remove(group.children[0])
    }

    for (const obj of objects) {
      switch (obj.kind) {
        case 'robot': {
          const geo = new THREE.BoxGeometry(18, 12, 18) // approximate robot size
          const mat = new THREE.MeshLambertMaterial({ color: 0x00aaff })
          const mesh = new THREE.Mesh(geo, mat)
          mesh.position.set(obj.x ?? 0, 6, -(obj.y ?? 0))
          mesh.castShadow = true

          const arrowDir = new THREE.Vector3(0, 0, -1)
          arrowDir.applyEuler(new THREE.Euler(0, -(obj.z ?? 0), 0))
          const arrow = new THREE.ArrowHelper(
            arrowDir.normalize(),
            mesh.position,
            20,
            0xffff00
          )
          group.add(mesh)
          group.add(arrow)
          break
        }

        case 'point': {
          const geo = new THREE.SphereGeometry(3, 8, 8)
          const mat = new THREE.MeshLambertMaterial({ color: 0xff6600 })
          const mesh = new THREE.Mesh(geo, mat)
          mesh.position.set(obj.x ?? 0, obj.z ?? 3, -(obj.y ?? 0))

          group.add(mesh)
          break
        }

        case 'axis': {
          const value = obj.value ?? 0
          const pos = new THREE.Vector3()
          if (obj.axis === 'X') pos.set(value, 1, 0)
          else if (obj.axis === 'Y') pos.set(0, 1, -value)
          else pos.set(0, value, 0)

          const geo = new THREE.SphereGeometry(4, 8, 8)
          const mat = new THREE.MeshLambertMaterial({ color: 0x00ff88 })
          const mesh = new THREE.Mesh(geo, mat)
          mesh.position.copy(pos)
          group.add(mesh)
          break
        }

        case 'line': {
          break
        }
      }
    }
  }, [objects])

  return (
    <div
      ref={mountRef}
      style={{ width: '100%', height: '100%', minHeight: 400 }}
    />
  )
}

function addPlaceholderField(scene: THREE.Scene) {
  const tileSize = FIELD_SIZE / 6
  const colors = [0x1a3a6a, 0x6a1a1a, 0x1a6a1a, 0x6a6a1a]

  for (let x = 0; x < 6; x++) {
    for (let z = 0; z < 6; z++) {
      const geo = new THREE.BoxGeometry(tileSize - 1, 0.5, tileSize - 1)
      const mat = new THREE.MeshLambertMaterial({
        color: colors[(x + z) % colors.length],
      })
      const mesh = new THREE.Mesh(geo, mat)
      mesh.position.set(
        (x - 2.5) * tileSize,
        0.25,
        (z - 2.5) * tileSize
      )
      mesh.receiveShadow = true
      scene.add(mesh)
    }
  }
}
