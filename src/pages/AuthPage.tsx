import { FormEvent, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

export function AuthPage({ mode }: { mode: 'login' | 'register' }) {
  const isRegister = mode === 'register'
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const { signIn, register } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const destination = (location.state as { from?: { pathname?: string } } | null)?.from?.pathname ?? '/'

  async function submit(event: FormEvent) {
    event.preventDefault()
    setError('')
    setLoading(true)
    try {
      if (isRegister) await register(name, email, password)
      else await signIn(email, password)
      navigate(destination, { replace: true })
    } catch (submissionError) {
      setError(submissionError instanceof Error ? submissionError.message : 'Unable to authenticate.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <main className="auth-layout">
      <section className="brand-panel">
        <div className="brand-mark">f</div>
        <p className="eyebrow">Shared money, made simple</p>
        <h1>Keep every share fair.</h1>
        <p className="brand-copy">FairShare is a calm, clear place to stay on top of the things you share.</p>
      </section>
      <section className="form-panel">
        <div className="form-wrap">
          <p className="eyebrow accent">Welcome to FairShare</p>
          <h2>{isRegister ? 'Create your account' : 'Welcome back'}</h2>
          <p className="muted">{isRegister ? 'Start keeping shared costs simple.' : 'Sign in to continue.'}</p>
          <form onSubmit={submit}>
            {isRegister && <label>Name<input required value={name} onChange={(event) => setName(event.target.value)} autoComplete="name" /></label>}
            <label>Email<input required type="email" value={email} onChange={(event) => setEmail(event.target.value)} autoComplete="email" /></label>
            <label>Password<input required minLength={8} type="password" value={password} onChange={(event) => setPassword(event.target.value)} autoComplete={isRegister ? 'new-password' : 'current-password'} /></label>
            {error && <p className="error" role="alert">{error}</p>}
            <button disabled={loading}>{loading ? 'Please wait…' : isRegister ? 'Create account' : 'Sign in'}</button>
          </form>
          <p className="switch-auth">{isRegister ? 'Already have an account?' : 'New to FairShare?'} <Link to={isRegister ? '/login' : '/register'}>{isRegister ? 'Sign in' : 'Create an account'}</Link></p>
        </div>
      </section>
    </main>
  )
}
