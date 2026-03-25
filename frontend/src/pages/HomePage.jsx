import { useEffect, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { getCosmetics } from '../api/cosmetics';
import { getRoutines } from '../api/routines';
import { createSession, sendMessage, getMessages } from '../api/chat';
import ProfileCard from '../components/dashboard/ProfileCard';
import CosmeticSummary from '../components/dashboard/CosmeticSummary';
import RoutineSummary from '../components/dashboard/RoutineSummary';
import ChatMessage from '../components/chat/ChatMessage';
import ChatInput from '../components/chat/ChatInput';
import Spinner from '../components/common/Spinner';
import './HomePage.css';

export default function HomePage() {
  const { user } = useAuth();
  const { sessionId } = useParams();
  const navigate = useNavigate();
  const bottomRef = useRef(null);

  const [cosmetics, setCosmetics] = useState([]);
  const [routines, setRoutines] = useState([]);
  const [messages, setMessages] = useState([]);
  const [loadingChat, setLoadingChat] = useState(false);
  const [loadingData, setLoadingData] = useState(true);

  const isChatMode = Boolean(sessionId);

  useEffect(() => {
    Promise.all([
      getCosmetics().then((r) => setCosmetics(r.data)),
      getRoutines().then((r) => setRoutines(r.data)),
    ]).finally(() => setLoadingData(false));
  }, []);

  useEffect(() => {
    if (!sessionId) { setMessages([]); return; }
    getMessages(sessionId).then((r) => setMessages(r.data));
  }, [sessionId]);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const handleSend = async (text) => {
    let sid = sessionId;
    if (!sid) {
      const res = await createSession();
      sid = res.data.id;
      navigate(`/chat/${sid}`, { replace: true });
    }
    setMessages((prev) => [...prev, { role: 'USER', content: text, id: Date.now() }]);
    setLoadingChat(true);
    try {
      const res = await sendMessage(sid, text);
      setMessages((prev) => [...prev, { role: 'ASSISTANT', content: res.data.answer, id: Date.now() + 1 }]);
    } catch {
      setMessages((prev) => [...prev, { role: 'ASSISTANT', content: '죄송합니다, 오류가 발생했습니다.', id: Date.now() + 1 }]);
    } finally { setLoadingChat(false); }
  };

  if (loadingData) return <div className="home-page__loading"><Spinner size={28} /></div>;

  return (
    <div className="home-page">
      {!isChatMode && (
        <div className="home-page__dashboard">
          <ProfileCard user={user} />
          <div className="home-page__summary-grid">
            <CosmeticSummary cosmetics={cosmetics} />
            <RoutineSummary routines={routines} />
          </div>
        </div>
      )}
      {isChatMode && (
        <div className="home-page__messages">
          {messages.map((msg) => (
            <ChatMessage key={msg.id} role={msg.role} content={msg.content} />
          ))}
          {loadingChat && (
            <div className="home-page__typing">
              <div className="home-page__avatar">SR</div>
              <Spinner size={18} />
            </div>
          )}
          <div ref={bottomRef} />
        </div>
      )}
      <div className="home-page__input-wrap">
        <ChatInput
          onSubmit={handleSend}
          loading={loadingChat}
          placeholder={isChatMode ? '메시지를 입력하세요...' : '피부에 대해 무엇이든 물어보세요...'}
        />
      </div>
    </div>
  );
}
