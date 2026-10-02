import { FormEvent, useEffect, useState } from 'react'
import { groupsApi, type Group, type GroupMember } from '../api/client'
import { useAuth } from '../auth/AuthContext'

export function GroupsPage() {
  const { token, signOut } = useAuth()
  const [groups, setGroups] = useState<Group[]>([])
  const [selected, setSelected] = useState<Group | null>(null)
  const [members, setMembers] = useState<GroupMember[]>([])
  const [createName, setCreateName] = useState('')
  const [joinCode, setJoinCode] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)

  async function loadGroups() {
    if (!token) return
    setLoading(true)
    try { setGroups(await groupsApi.list(token)); setError('') }
    catch (loadError) { setError(loadError instanceof Error ? loadError.message : 'Unable to load groups.') }
    finally { setLoading(false) }
  }

  useEffect(() => { void loadGroups() }, [token])

  async function selectGroup(group: Group) {
    if (!token) return
    try {
      const [details, groupMembers] = await Promise.all([groupsApi.get(group.id, token), groupsApi.members(group.id, token)])
      setSelected(details); setMembers(groupMembers); setError('')
    } catch (loadError) { setError(loadError instanceof Error ? loadError.message : 'Unable to load group.') }
  }

  async function submitCreate(event: FormEvent) {
    event.preventDefault()
    if (!token) return
    try { const group = await groupsApi.create(createName, token); setCreateName(''); await loadGroups(); await selectGroup(group) }
    catch (createError) { setError(createError instanceof Error ? createError.message : 'Unable to create group.') }
  }

  async function submitJoin(event: FormEvent) {
    event.preventDefault()
    if (!token) return
    try { const group = await groupsApi.join(joinCode, token); setJoinCode(''); await loadGroups(); await selectGroup(group) }
    catch (joinError) { setError(joinError instanceof Error ? joinError.message : 'Unable to join group.') }
  }

  async function leaveGroup() {
    if (!token || !selected) return
    try { await groupsApi.leave(selected.id, token); setSelected(null); setMembers([]); await loadGroups() }
    catch (leaveError) { setError(leaveError instanceof Error ? leaveError.message : 'Unable to leave group.') }
  }

  return (
    <main className="groups-layout">
      <header className="topbar"><div className="wordmark"><span>f</span> FairShare</div><button className="button-quiet" onClick={signOut}>Sign out</button></header>
      <div className="groups-header"><div><p className="eyebrow accent">Your spaces</p><h1>Groups</h1></div></div>
      <section className="group-actions">
        <form onSubmit={submitCreate}><h2>Create a group</h2><label>Group name<input required maxLength={120} value={createName} onChange={event => setCreateName(event.target.value)} placeholder="PG Room 204" /></label><button>Create group</button></form>
        <form onSubmit={submitJoin}><h2>Join a group</h2><label>Join code<input required maxLength={32} value={joinCode} onChange={event => setJoinCode(event.target.value)} placeholder="ABC123" /></label><button>Join group</button></form>
      </section>
      {error && <p className="error" role="alert">{error}</p>}
      <section className="groups-content">
        <div className="group-list"><h2>Your groups</h2>{loading ? <p className="muted">Loading groups…</p> : groups.length === 0 ? <p className="muted">Create or join your first group.</p> : groups.map(group => <button className={`group-item ${selected?.id === group.id ? 'selected' : ''}`} key={group.id} onClick={() => void selectGroup(group)}>{group.name}<span>{group.joinCode}</span></button>)}</div>
        {selected && <div className="group-detail"><div className="detail-heading"><div><p className="eyebrow accent">Group details</p><h2>{selected.name}</h2></div><button className="button-danger" onClick={() => void leaveGroup()}>Leave group</button></div><p className="join-code">Join code <strong>{selected.joinCode}</strong></p><h3>Members</h3><ul className="member-list">{members.map(member => <li key={member.userId}><strong>{member.fullName}</strong><span>{member.email}</span></li>)}</ul></div>}
      </section>
    </main>
  )
}
