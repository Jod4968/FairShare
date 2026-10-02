import { Navigate, Route, Routes } from 'react-router-dom'
import { ProtectedRoute } from './components/ProtectedRoute'
import { AuthPage } from './pages/AuthPage'
import { GroupsPage } from './pages/GroupsPage'
import { ExpensesPage } from './pages/ExpensesPage'

export function App() {
  return <Routes>
    <Route path="/login" element={<AuthPage mode="login" />} />
    <Route path="/register" element={<AuthPage mode="register" />} />
    <Route element={<ProtectedRoute />}><Route path="/" element={<GroupsPage />} /><Route path="/groups/:groupId/expenses" element={<ExpensesPage />} /></Route>
    <Route path="*" element={<Navigate to="/" replace />} />
  </Routes>
}
