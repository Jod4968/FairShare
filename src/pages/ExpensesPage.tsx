import { FormEvent, useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { expensesApi, groupsApi, type Expense, type Group, type GroupMember } from '../api/client'
import { useAuth } from '../auth/AuthContext'

const categories = ['FOOD', 'GROCERIES', 'RENT', 'UTILITIES', 'TRANSPORT', 'ENTERTAINMENT', 'OTHER']
const money = (minor: number) => `₹${(minor / 100).toFixed(2)}`

export function ExpensesPage() {
  const { groupId } = useParams()
  const id = Number(groupId)
  const { token, signOut } = useAuth()
  const navigate = useNavigate()
  const [group, setGroup] = useState<Group | null>(null)
  const [members, setMembers] = useState<GroupMember[]>([])
  const [expenses, setExpenses] = useState<Expense[]>([])
  const [selected, setSelected] = useState<Expense | null>(null)
  const [description, setDescription] = useState('')
  const [amount, setAmount] = useState('')
  const [category, setCategory] = useState('FOOD')
  const [splitType, setSplitType] = useState<'EQUAL' | 'CUSTOM'>('EQUAL')
  const [participantIds, setParticipantIds] = useState<number[]>([])
  const [shares, setShares] = useState<Record<number, string>>({})
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)

  async function load() {
    if (!token || !id) return
    setLoading(true)
    try {
      const [groupResponse, memberResponse, expenseResponse] = await Promise.all([
        groupsApi.get(id, token), groupsApi.members(id, token), expensesApi.list(id, token),
      ])
      setGroup(groupResponse); setMembers(memberResponse); setExpenses(expenseResponse); setError('')
    } catch (loadError) { setError(loadError instanceof Error ? loadError.message : 'Unable to load expenses.') }
    finally { setLoading(false) }
  }
  useEffect(() => { void load() }, [token, id])

  function toggleParticipant(userId: number) {
    setParticipantIds(current => current.includes(userId) ? current.filter(idValue => idValue !== userId) : [...current, userId])
  }

  async function createExpense(event: FormEvent) {
    event.preventDefault()
    if (!token) return
    const amountMinor = Math.round(Number(amount) * 100)
    const request = splitType === 'EQUAL'
      ? { description, amountMinor, category, splitType, participantUserIds: participantIds }
      : { description, amountMinor, category, splitType, participants: participantIds.map(userId => ({ userId, shareMinor: Math.round(Number(shares[userId]) * 100) })) }
    try {
      await expensesApi.create(id, request, token)
      setDescription(''); setAmount(''); setParticipantIds([]); setShares({}); await load()
    } catch (createError) { setError(createError instanceof Error ? createError.message : 'Unable to create expense.') }
  }

  async function deleteExpense(expenseId: number) {
    if (!token) return
    try { await expensesApi.remove(id, expenseId, token); setSelected(null); await load() }
    catch (deleteError) { setError(deleteError instanceof Error ? deleteError.message : 'Unable to delete expense.') }
  }

  return (
    <main className="groups-layout">
      <header className="topbar"><div className="wordmark"><span>f</span> FairShare</div><button className="button-quiet" onClick={signOut}>Sign out</button></header>
      <div className="groups-header"><Link to="/">← Groups</Link><p className="eyebrow accent">Shared expenses</p><h1>{group?.name ?? 'Expenses'}</h1></div>
      {error && <p className="error" role="alert">{error}</p>}
      <section className="expense-layout">
        <form className="expense-form" onSubmit={createExpense}>
          <h2>Add expense</h2>
          <label>Description<input required maxLength={240} value={description} onChange={event => setDescription(event.target.value)} placeholder="Dinner" /></label>
          <label>Amount (₹)<input required min="0.01" step="0.01" type="number" value={amount} onChange={event => setAmount(event.target.value)} placeholder="900.00" /></label>
          <label>Category<select value={category} onChange={event => setCategory(event.target.value)}>{categories.map(value => <option key={value}>{value}</option>)}</select></label>
          <label>Split type<select value={splitType} onChange={event => setSplitType(event.target.value as 'EQUAL' | 'CUSTOM') }><option value="EQUAL">Equal</option><option value="CUSTOM">Custom amounts</option></select></label>
          <fieldset><legend>Participants</legend>{members.map(member => <label className="participant-option" key={member.userId}><input type="checkbox" checked={participantIds.includes(member.userId)} onChange={() => toggleParticipant(member.userId)} />{member.fullName}{splitType === 'CUSTOM' && participantIds.includes(member.userId) && <input required min="0.01" step="0.01" type="number" placeholder="Share ₹" value={shares[member.userId] ?? ''} onChange={event => setShares(current => ({ ...current, [member.userId]: event.target.value }))} />}</label>)}</fieldset>
          <button disabled={loading}>Add expense</button>
        </form>
        <div className="expense-list"><h2>Expenses</h2>{loading ? <p className="muted">Loading expenses…</p> : expenses.length === 0 ? <p className="muted">No expenses yet.</p> : expenses.map(expense => <button className={`expense-item ${selected?.id === expense.id ? 'selected' : ''}`} key={expense.id} onClick={() => setSelected(expense)}><span><strong>{expense.description}</strong><small>{expense.category} · paid by {expense.paidByName}</small></span><strong>{money(expense.amountMinor)}</strong></button>)}</div>
      </section>
      {selected && <section className="expense-detail"><div className="detail-heading"><div><p className="eyebrow accent">Expense details</p><h2>{selected.description}</h2></div><button className="button-danger" onClick={() => void deleteExpense(selected.id)}>Delete</button></div><p className="muted">{selected.category} · {selected.splitType} split · paid by {selected.paidByName}</p><h3>Participants</h3><ul className="member-list">{selected.participants.map(participant => <li key={participant.userId}><strong>{participant.fullName}</strong><span>{money(participant.shareMinor)}</span></li>)}</ul></section>}
      <button className="button-quiet" onClick={() => navigate('/')}>Back to groups</button>
    </main>
  )
}
