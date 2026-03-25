import './ChatMessage.css';

export default function ChatMessage({ role, content }) {
  const isUser = role === 'USER';
  return (
    <div className={`chat-message chat-message--${isUser ? 'user' : 'assistant'}`}>
      {!isUser && <div className="chat-message__avatar">SR</div>}
      <div className="chat-message__bubble">
        {content.split('\n').map((line, i, arr) => (
          <span key={i}>{line}{i < arr.length - 1 && <br />}</span>
        ))}
      </div>
    </div>
  );
}
