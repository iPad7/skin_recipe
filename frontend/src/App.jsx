import { Routes, Route, Navigate } from 'react-router-dom';
import { useAuth } from './context/AuthContext';
import LoginPage from './pages/LoginPage';
import SignupPage from './pages/SignupPage';
import HomePage from './pages/HomePage';
import CosmeticsPage from './pages/CosmeticsPage';
import RoutinesPage from './pages/RoutinesPage';
import MainLayout from './components/layout/MainLayout';

function PrivateRoute({ children }) {
  const { user } = useAuth();
  return user ? children : <Navigate to="/login" replace />;
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/signup" element={<SignupPage />} />
      <Route
        path="/*"
        element={
          <PrivateRoute>
            <MainLayout />
          </PrivateRoute>
        }
      >
        <Route index element={<HomePage />} />
        <Route path="chat/:sessionId" element={<HomePage />} />
        <Route path="cosmetics" element={<CosmeticsPage />} />
        <Route path="routines" element={<RoutinesPage />} />
      </Route>
    </Routes>
  );
}
