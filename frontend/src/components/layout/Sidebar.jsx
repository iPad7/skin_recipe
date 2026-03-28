import { useEffect, useState } from 'react';
import { useNavigate, useLocation, Link } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { getSessions, deleteSession } from '../../api/chat';
import './Sidebar.css';

function groupByDate(sessions) {
  const today = new Date().toDateString();
  const yesterday = new Date(Date.now() - 86400000).toDateString();
  const groups = { 오늘: [], 어제: [], 이전: [] };
  for (const s of sessions) {
    const d = new Date(s.createdAt).toDateString();
    if (d === today) groups['오늘'].push(s);
    else if (d === yesterday) groups['어제'].push(s);
    else groups['이전'].push(s);
  }
  return groups;
}

export default function Sidebar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [sessions, setSessions] = useState([]);

  const currentSessionId = location.pathname.startsWith('/chat/')
    ? location.pathname.split('/chat/')[1]
    : null;

  useEffect(() => {
    getSessions().then((r) => setSessions(r.data)).catch(() => {});
  }, [location.pathname]);

  const handleDeleteSession = async (e, id) => {
    e.preventDefault();
    e.stopPropagation();
    if (!window.confirm('이 대화를 삭제할까요?')) return;
    await deleteSession(id);
    setSessions((prev) => prev.filter((s) => s.id !== id));
    if (currentSessionId === id) navigate('/');
  };

  const groups = groupByDate(sessions);

  return (
    <aside className="sidebar">
      <div className="sidebar__top">
        <Link to="/" className="sidebar__logo">
          <span className="sidebar__logo-icon">SR</span>
          <span className="sidebar__logo-text">Skin Recipe</span>
        </Link>
        <button className="sidebar__new-chat" onClick={() => navigate('/')}>
          + 새 채팅
        </button>
        <nav className="sidebar__menu">
          <Link to="/cosmetics" className={`sidebar__menu-item ${location.pathname === '/cosmetics' ? 'active' : ''}`}>
            <span className="sidebar__menu-icon">◻</span>내 화장품
          </Link>
          <Link to="/routines" className={`sidebar__menu-item ${location.pathname === '/routines' ? 'active' : ''}`}>
            <span className="sidebar__menu-icon">◎</span>내 루틴
          </Link>
        </nav>
      </div>

      <div className="sidebar__sessions">
        <p className="sidebar__section-label">채팅 기록</p>
        {Object.entries(groups).map(([label, items]) =>
          items.length > 0 ? (
            <div key={label} className="sidebar__group">
              <p className="sidebar__group-label">{label}</p>
              {items.map((s) => (
                <div
                  key={s.id}
                  className={`sidebar__session ${s.id === currentSessionId ? 'active' : ''}`}
                  onClick={() => navigate(`/chat/${s.id}`)}
                >
                  <span className="sidebar__session-text">
                    {new Date(s.createdAt).toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' })} 대화
                  </span>
                  <button className="sidebar__session-delete" onClick={(e) => handleDeleteSession(e, s.id)}>✕</button>
                </div>
              ))}
            </div>
          ) : null
        )}
        {sessions.length === 0 && <p className="sidebar__empty">채팅 기록이 없습니다</p>}
      </div>

      <div className="sidebar__bottom">
        <button className="sidebar__logout" onClick={logout}>
          {user?.nickname} · 로그아웃
        </button>
      </div>
    </aside>
  );
}
