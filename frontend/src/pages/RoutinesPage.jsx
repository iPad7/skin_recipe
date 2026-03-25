import { useEffect, useState } from 'react';
import { getRoutines, deleteRoutine } from '../api/routines';
import RoutineCard from '../components/routines/RoutineCard';
import RoutineCreateModal from '../components/routines/RoutineCreateModal';
import Modal from '../components/common/Modal';
import Spinner from '../components/common/Spinner';
import './RoutinesPage.css';

export default function RoutinesPage() {
  const [routines, setRoutines] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showCreate, setShowCreate] = useState(false);

  const load = () => {
    setLoading(true);
    getRoutines().then((r) => setRoutines(r.data)).finally(() => setLoading(false));
  };

  useEffect(() => { load(); }, []);

  const handleDelete = async (id) => {
    if (!confirm('루틴을 삭제하시겠습니까?')) return;
    await deleteRoutine(id);
    setRoutines((p) => p.filter((r) => r.id !== id));
  };

  const handleCreated = (routine) => {
    setRoutines((p) => [...p, routine]);
    setShowCreate(false);
  };

  return (
    <div className="routines-page">
      <div className="routines-page__header">
        <h1 className="routines-page__title">내 루틴</h1>
        <button className="routines-page__btn" onClick={() => setShowCreate(true)}>
          + 루틴 만들기
        </button>
      </div>

      {loading ? (
        <div className="routines-page__loading"><Spinner size={24} /></div>
      ) : routines.length === 0 ? (
        <div className="routines-page__empty">
          <p>아직 루틴이 없습니다.</p>
          <p>화장품을 등록한 후 AI 루틴 추천을 받아보세요.</p>
        </div>
      ) : (
        <div className="routines-page__list">
          {routines.map((r) => (
            <RoutineCard key={r.id} routine={r} onDelete={handleDelete} />
          ))}
        </div>
      )}

      {showCreate && (
        <Modal title="루틴 만들기" onClose={() => setShowCreate(false)}>
          <RoutineCreateModal onSuccess={handleCreated} onCancel={() => setShowCreate(false)} />
        </Modal>
      )}
    </div>
  );
}
