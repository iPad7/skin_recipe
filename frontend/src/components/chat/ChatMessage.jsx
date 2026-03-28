import Markdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import rehypeRaw from 'rehype-raw';
import './ChatMessage.css';

export default function ChatMessage({ role, content }) {
  const isUser = role === 'USER';
  return (
    <div className={`chat-message chat-message--${isUser ? 'user' : 'assistant'}`}>
      {!isUser && <div className="chat-message__avatar">SR</div>}
      <div className="chat-message__bubble">
        {isUser ? content : <div className="chat-message__md"><Markdown remarkPlugins={[remarkGfm]} rehypePlugins={[rehypeRaw]}>{content}</Markdown></div>}
      </div>
    </div>
  );
}
