import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { login as loginApi } from '../api/auth';
import { useAuth } from '../context/AuthContext';
import './AuthPage.css';

export default function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ email: '', password: '' });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(''); setLoading(true);
    try {
      const res = await loginApi(form);
      await login(res.data.accessToken);
      navigate('/');
    } catch (err) {
      setError(err.response?.data?.message || '이메일 또는 비밀번호를 확인해주세요.');
    } finally { setLoading(false); }
  };

  return (
    <div className="auth-page">
      <div className="auth-card">
        <div className="auth-logo">
          <span className="auth-logo__icon">SR</span>
          <span className="auth-logo__text">Skin Recipe</span>
        </div>
        <p className="auth-subtitle">내 화장품을 스마트하게 관리하세요</p>
        <form className="auth-form" onSubmit={handleSubmit}>
          <div className="auth-field">
            <label>이메일</label>
            <input type="email" name="email" value={form.email}
              onChange={(e) => setForm((p) => ({ ...p, email: e.target.value }))}
              placeholder="email@example.com" required />
          </div>
          <div className="auth-field">
            <label>비밀번호</label>
            <input type="password" name="password" value={form.password}
              onChange={(e) => setForm((p) => ({ ...p, password: e.target.value }))}
              placeholder="비밀번호를 입력하세요" required />
          </div>
          {error && <p className="auth-error">{error}</p>}
          <button className="auth-btn" type="submit" disabled={loading}>
            {loading ? '로그인 중...' : '로그인'}
          </button>
        </form>
        <p className="auth-link">계정이 없으신가요? <Link to="/signup">회원가입 →</Link></p>
      </div>
    </div>
  );
}
