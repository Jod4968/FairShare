import { FormEvent, useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { assistantApi, expensesApi, groupsApi, settlementsApi, type Balance, type Expense, type Group, type GroupMember, type Settlement, type Suggestion } from '../api/client'
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
  const [balances, setBalances] = useState<Balance[]>([])
  const [suggestions, setSuggestions] = useState<Suggestion[]>([])
  const [settlements, setSettlements] = useState<Settlement[]>([])
  const [settlementAmount, setSettlementAmount] = useState<Record<number, string>>({})
  const [assistantInput, setAssistantInput] = useState('')
  const [assistantMessages, setAssistantMessages] = useState<{ role: 'user' | 'assistant'; text: string }[]>([])
  const [assistantLoading, setAssistantLoading] = useState(false)

  async function load() {
    if (!token || !id) return
    setLoading(true)
    try {
      const [groupResponse, memberResponse, expenseResponse, balanceResponse, suggestionResponse, settlementResponse] = await Promise.all([
        groupsApi.get(id, token), groupsApi.members(id, token), expensesApi.list(id, token),
        settlementsApi.balances(id, token), settlementsApi.suggestions(id, token), settlementsApi.list(id, token),
      ])
      setGroup(groupResponse); setMembers(memberResponse); setExpenses(expenseResponse); setBalances(balanceResponse); setSuggestions(suggestionResponse); setSettlements(settlementResponse); setError('')
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

  async function recordSettlement(suggestion: Suggestion) {
    if (!token) return
    try {
      await settlementsApi.create(id, { toUserId: suggestion.toUserId, amountMinor: suggestion.amountMinor }, token)
      await load()
    } catch (settlementError) { setError(settlementError instanceof Error ? settlementError.message : 'Unable to record settlement.') }
  }

  async function completeSettlement(settlementId: number) {
    if (!token) return
    try { await settlementsApi.complete(id, settlementId, token); await load() }
    catch (completeError) { setError(completeError instanceof Error ? completeError.message : 'Unable to complete settlement.') }
  }

  async function sendAssistant(event: FormEvent) {
    event.preventDefault()
    if (!token || !assistantInput.trim()) return
    const text = assistantInput.trim()
    setAssistantInput('')
    setAssistantMessages(current => [...current, { role: 'user', text }])
    setAssistantLoading(true)
    try {
      const response = await assistantApi.message(id, text, token)
      setAssistantMessages(current => [...current, { role: 'assistant', text: response.message }])
      if (response.success) await load()
    } catch (assistantError) {
      setAssistantMessages(current => [...current, { role: 'assistant', text: assistantError instanceof Error ? assistantError.message : 'The assistant is unavailable.' }])
    } finally { setAssistantLoading(false) }
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
      <section className="settlements-section">
        <h2>Balances</h2>
        <div className="balance-grid">{balances.map(balance => <div className="balance-card" key={balance.userId}><strong>{balance.fullName}</strong><span>Paid {money(balance.amountPaidMinor)} · Owed {money(balance.amountOwedMinor)}</span><b className={balance.balanceMinor > 0 ? 'positive' : balance.balanceMinor < 0 ? 'negative' : ''}>{balance.balanceMinor > 0 ? `Receives ${money(balance.balanceMinor)}` : balance.balanceMinor < 0 ? `Owes ${money(-balance.balanceMinor)}` : 'Settled'}</b></div>)}</div>
        <h3>Suggested settlements</h3>
        {suggestions.length === 0 ? <p className="muted">Everyone is settled.</p> : suggestions.map(suggestion => <div className="settlement-row" key={`${suggestion.fromUserId}-${suggestion.toUserId}`}><span>{suggestion.fromUserName} pays {suggestion.toUserName} <strong>{money(suggestion.amountMinor)}</strong></span><button onClick={() => void recordSettlement(suggestion)}>Record payment</button></div>)}
        <h3>Settlement history</h3>
        {settlements.length === 0 ? <p className="muted">No settlements recorded.</p> : settlements.map(settlement => <div className="settlement-row" key={settlement.id}><span>{settlement.fromUserName} → {settlement.toUserName} <strong>{money(settlement.amountMinor)}</strong><small>{settlement.status}</small></span>{settlement.status === 'PENDING' && <button onClick={() => void completeSettlement(settlement.id)}>Mark completed</button>}</div>)}
      </section>
      <section className="assistant-section">
        <p className="eyebrow accent">FairShare assistant</p><h2>Ask about this group</h2>
        <div className="assistant-messages">{assistantMessages.length === 0 ? <p className="muted">Try “How much did we spend on food?”</p> : assistantMessages.map((message, index) => <p className={`assistant-message ${message.role}`} key={index}>{message.text}</p>)}</div>
        <form onSubmit={sendAssistant} className="assistant-form"><input value={assistantInput} onChange={event => setAssistantInput(event.target.value)} placeholder="I paid ₹900 for dinner for me and Rahul" /><button disabled={assistantLoading}>{assistantLoading ? 'Thinking…' : 'Send'}</button></form>
      </section>
      <button className="button-quiet" onClick={() => navigate('/')}>Back to groups</button>
    </main>
  )
}
