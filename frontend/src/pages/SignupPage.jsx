import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { signup } from '../api/auth';
import './AuthPage.css';

const SKIN_TYPES = [
  { value: 'DRY', label: '건성' }, { value: 'OILY', label: '지성' },
  { value: 'COMBINATION', label: '복합성' }, { value: 'SENSITIVE', label: '민감성' },
  { value: 'NORMAL', label: '중성' },
];

export default function SignupPage() {
  const navigate = useNavigate();
  const [form, setForm] = useState({ email: '', password: '', nickname: '', skinType: '', skinConcerns: '', allergyIngredients: '' });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const set = (k) => (e) => setForm((p) => ({ ...p, [k]: e.target.value }));

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.skinType) { setError('피부 타입을 선택해주세요.'); return; }
    setError(''); setLoading(true);
    try {
      await signup(form);
      navigate('/login');
    } catch (err) {
      setError(err.response?.data?.message || '회원가입에 실패했습니다.');
    } finally { setLoading(false); }
  };

  return (
    <div className="auth-page">
      <div className="auth-card auth-card--wide">
        <div className="auth-logo">
          <span className="auth-logo__icon">SR</span>
          <span className="auth-logo__text">Skin Recipe</span>
        </div>
        <form className="auth-form" onSubmit={handleSubmit}>
          <div className="auth-field"><label>이메일</label>
            <input type="email" value={form.email} onChange={set('email')} placeholder="email@example.com" required />
          </div>
          <div className="auth-field"><label>비밀번호</label>
            <input type="password" value={form.password} onChange={set('password')} placeholder="8자 이상 입력하세요" required />
          </div>
          <div className="auth-field"><label>닉네임</label>
            <input type="text" value={form.nickname} onChange={set('nickname')} placeholder="홍길동" required />
          </div>
          <div className="auth-field">
            <label>피부 타입</label>
            <div className="skin-type-group">
              {SKIN_TYPES.map((st) => (
                <button key={st.value} type="button"
                  className={`skin-type-pill ${form.skinType === st.value ? 'active' : ''}`}
                  onClick={() => setForm((p) => ({ ...p, skinType: st.value }))}>
                  {st.label}
                </button>
              ))}
            </div>
          </div>
          <div className="auth-field">
            <label>피부 고민 <span className="auth-hint">(쉼표로 구분)</span></label>
            <input type="text" value={form.skinConcerns} onChange={set('skinConcerns')} placeholder="건조함, 모공, 색소침착..." />
          </div>
          <div className="auth-field">
            <label>알레르기 성분 <span className="auth-hint">(쉼표로 구분)</span></label>
            <input type="text" value={form.allergyIngredients} onChange={set('allergyIngredients')} placeholder="파라벤, 향료, 알코올..." />
          </div>
          {error && <p className="auth-error">{error}</p>}
          <button className="auth-btn" type="submit" disabled={loading}>
            {loading ? '가입 중...' : '회원가입'}
          </button>
        </form>
        <p className="auth-link">이미 계정이 있으신가요? <Link to="/login">로그인 →</Link></p>
      </div>
    </div>
  );
}
