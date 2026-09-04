import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';

import { AuthProvider } from './auth/AuthContext';
import ProtectedRoute from './auth/ProtectedRoute';
import Login from './auth/Login';
import ForgotPassword from './auth/ForgotPassword';

import AdminLayout from './admin/AdminLayout';
import AdminDashboard from './admin/pages/Dashboard';
import Users from './admin/pages/Users';
import AdminCredit from './admin/pages/Credit';
import Finance from './admin/pages/Finance';
import Disputes from './admin/pages/Disputes';
import Config from './admin/pages/Config';
import Audit from './admin/pages/Audit';
import Roles from './admin/pages/Roles';
import Support from './admin/pages/Support';
import Profile from './admin/pages/Profile';
import NotFound from './pages/NotFound';

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/" element={<Navigate to="/login" replace />} />
          <Route path="/login" element={<Login />} />
          <Route path="/forgot-password" element={<ForgotPassword />} />

          <Route element={<ProtectedRoute />}>
            <Route path="/admin" element={<AdminLayout />}>
              <Route index element={<Navigate to="dashboard" replace />} />
              <Route path="dashboard" element={<AdminDashboard />} />
              <Route path="users" element={<Users />} />
              <Route path="credit" element={<AdminCredit />} />
              <Route path="finance" element={<Finance />} />
              <Route path="disputes" element={<Disputes />} />
              <Route path="config" element={<Config />} />
              <Route path="audit" element={<Audit />} />
              <Route path="roles" element={<Roles />} />
              <Route path="support" element={<Support />} />
              <Route path="profile" element={<Profile />} />
            </Route>
          </Route>

          <Route path="*" element={<NotFound />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}
