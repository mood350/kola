import { Navigate, Outlet, useLocation } from 'react-router-dom';
import LoadingState from '../components/LoadingState';
import { useAuth } from './AuthContext';

export default function ProtectedRoute() {
  const { isAuthenticated, loading } = useAuth();
  const location = useLocation();

  if (loading) return <LoadingState label="Vérification de la session…" />;
  if (!isAuthenticated) return <Navigate to="/login" replace state={{ from: location }} />;
  return <Outlet />;
}
