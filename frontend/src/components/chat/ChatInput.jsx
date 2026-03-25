import { useState } from 'react';
import Spinner from '../common/Spinner';
import './ChatInput.css';

export default function ChatInput({ onSubmit, loading, placeholder }) {
  const [value, setValue] = useState('');

  const handleSubmit = () => {
    if (!value.trim() || loading) return;
    onSubmit(value.trim());
    setValue('');
  };

  return (
    <div className="chat-input">
      <textarea
        className="chat-input__textarea"
        value={value}
        onChange={(e) => setValue(e.target.value)}
        onKeyDown={(e) => { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); handleSubmit(); } }}
        placeholder={placeholder || '메시지를 입력하세요...'}
        rows={1}
        disabled={loading}
      />
      <button className="chat-input__send" onClick={handleSubmit} disabled={!value.trim() || loading}>
        {loading ? <Spinner size={16} /> : '↑'}
      </button>
    </div>
  );
}
