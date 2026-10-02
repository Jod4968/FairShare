import { useAuth } from '../auth/AuthContext'

export function HomePage() {
  const { signOut } = useAuth()
  return (
    <main className="home-layout">
      <header className="topbar"><div className="wordmark"><span>f</span> FairShare</div><button className="button-quiet" onClick={signOut}>Sign out</button></header>
      <section className="home-card"><div className="spark">✦</div><p className="eyebrow accent">You’re all set</p><h1>Your shared money space starts here.</h1><p className="muted">This is your FairShare home. More ways to manage shared costs are coming soon.</p></section>
    </main>
  )
}
