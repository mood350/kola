import { BrowserRouter, Link, Route, Routes } from 'react-router-dom';
import { isConfigured } from './lib/api';
import { SessionProvider } from './lib/session';
import Layout, { RequireModule } from './components/Layout';
import { PageHead } from './components/ui';
import Login from './pages/Login';
import Overview from './pages/Overview';
import Users from './pages/Users';
import UserDetail from './pages/UserDetail';
import Kyc from './pages/Kyc';
import Transactions from './pages/Transactions';
import Loans from './pages/Loans';
import Account from './pages/Account';

export default function App() {
  // Pas de mode démonstration : sans API, la console le dit au lieu d'inventer des données.
  if (!isConfigured()) {
    return (
      <div className="login">
        <div className="login-box notice error">
          VITE_API_BASE_URL n&apos;est pas défini. Renseignez-le (voir .env.example) puis relancez la console.
        </div>
      </div>
    );
  }

  return (
    <BrowserRouter>
      <SessionProvider>
        <Routes>
          <Route path="/connexion" element={<Login />} />
          <Route element={<Layout />}>
            <Route index element={<RequireModule module="dashboard"><Overview /></RequireModule>} />
            <Route path="utilisateurs" element={<RequireModule module="users"><Users /></RequireModule>} />
            <Route path="utilisateurs/:id" element={<RequireModule module="users"><UserDetail /></RequireModule>} />
            <Route path="kyc" element={<RequireModule module="users"><Kyc /></RequireModule>} />
            <Route path="transactions" element={<RequireModule module="users"><Transactions /></RequireModule>} />
            <Route path="prets" element={<RequireModule module="credit"><Loans /></RequireModule>} />
            <Route path="compte" element={<Account />} />
            <Route path="*" element={<PageHead title="Page introuvable" subtitle={<Link to="/">Retour à la vue d&apos;ensemble</Link>} />} />
          </Route>
        </Routes>
      </SessionProvider>
    </BrowserRouter>
  );
}
