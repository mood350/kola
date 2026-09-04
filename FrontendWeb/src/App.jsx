import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';

import Login from './auth/Login';

import KolaLayout from './kola/KolaLayout';
import Dashboard from './kola/pages/Dashboard';
import WalletPage from './kola/pages/Wallet';
import Vaults from './kola/pages/Vaults';
import Credit from './kola/pages/Credit';
import Kyc from './kola/pages/Kyc';
import AdminLite from './kola/pages/AdminLite';

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

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Navigate to="/app/dashboard" replace />} />

        <Route path="/app" element={<KolaLayout />}>
          <Route index element={<Navigate to="dashboard" replace />} />
          <Route path="dashboard" element={<Dashboard />} />
          <Route path="wallet" element={<WalletPage />} />
          <Route path="vaults" element={<Vaults />} />
          <Route path="credit" element={<Credit />} />
          <Route path="kyc" element={<Kyc />} />
          <Route path="admin" element={<AdminLite />} />
        </Route>

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
        </Route>

        <Route path="*" element={<Navigate to="/app/dashboard" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
