import { useState } from 'react';
import { createRoutine } from '../../api/routines';
import Spinner from '../common/Spinner';
import './RoutineCreateModal.css';

export default function RoutineCreateModal({ onSuccess, onCancel }) {
  const [timeOfDay, setTimeOfDay] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleCreate = async () => {
    if (!timeOfDay) { setError('AM 또는 PM을 선택해주세요.'); return; }
    setError(''); setLoading(true);
    try {
      const res = await createRoutine({ timeOfDay });
      onSuccess(res.data);
    } catch {
      setError('루틴 생성에 실패했습니다. 화장품을 먼저 등록해주세요.');
    } finally { setLoading(false); }
  };

  return (
    <div className="routine-create">
      <p className="routine-create__desc">
        등록된 화장품을 기반으로 AI가 최적의 루틴을 추천해드립니다.
      </p>
      <div className="routine-create__options">
        <button
          className={`routine-create__option ${timeOfDay === 'AM' ? 'active am' : ''}`}
          onClick={() => setTimeOfDay('AM')} disabled={loading}>
          <span className="routine-create__option-label">AM</span>
          <span className="routine-create__option-desc">아침 루틴</span>
        </button>
        <button
          className={`routine-create__option ${timeOfDay === 'PM' ? 'active pm' : ''}`}
          onClick={() => setTimeOfDay('PM')} disabled={loading}>
          <span className="routine-create__option-label">PM</span>
          <span className="routine-create__option-desc">저녁 루틴</span>
        </button>
      </div>
      {error && <p className="routine-create__error">{error}</p>}
      {loading && (
        <div className="routine-create__loading">
          <Spinner size={18} />
          <span>AI가 루틴을 생성하고 있습니다...</span>
        </div>
      )}
      <div className="routine-create__footer">
        <button className="routine-create__btn routine-create__btn--ghost" onClick={onCancel} disabled={loading}>
          취소
        </button>
        <button className="routine-create__btn" onClick={handleCreate} disabled={loading || !timeOfDay}>
          루틴 생성
        </button>
      </div>
    </div>
  );
}
