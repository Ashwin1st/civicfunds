import { useEffect, useState } from 'react'

const API = import.meta.env.VITE_API_BASE ?? 'http://localhost:8080'

type Coverage = {
  rawRecords: number
  normalizedRecords: number
  recordsWithDeterministicKey: number
  sourceSystems: string[]
}

export default function App() {
  const [coverage, setCoverage] = useState<Coverage | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    fetch(`${API}/api/meta/coverage`)
      .then((r) => r.json())
      .then(setCoverage)
      .catch(() => setError('Backend not reachable yet.'))
  }, [])

  return (
    <main style={{ fontFamily: 'system-ui, sans-serif', padding: '3rem', maxWidth: 720 }}>
      <h1 style={{ color: '#0b3d69', marginBottom: 0 }}>CivicFunds</h1>
      <p style={{ color: '#5a6b7b', marginTop: '0.25rem' }}>
        Public Money. Real Projects. Clear Answers.
      </p>
      <hr style={{ margin: '2rem 0', border: 0, borderTop: '1px solid #e2e8f0' }} />
      <h2 style={{ fontSize: '1rem', color: '#0b3d69' }}>Phase 1 - data model online</h2>
      {error && <p style={{ color: '#b45309' }}>{error}</p>}
      {coverage && (
        <ul style={{ lineHeight: 1.8, color: '#334155' }}>
          <li>{coverage.rawRecords} raw records</li>
          <li>{coverage.normalizedRecords} normalized records</li>
          <li>{coverage.recordsWithDeterministicKey} with a deterministic key</li>
          <li>Sources: {coverage.sourceSystems.join(', ') || 'none ingested yet'}</li>
        </ul>
      )}
    </main>
  )
}
