import { useNavigate } from 'react-router-dom';
import Badge from '../common/Badge';
import './SummaryCard.css';

export default function RoutineSummary({ routines }) {
  const navigate = useNavigate();
  const display = [...routines.filter((r) => r.timeOfDay === 'AM'), ...routines.filter((r) => r.timeOfDay === 'PM')].slice(0, 2);

  return (
    <div className="summary-card">
      <div className="summary-card__header">
        <h3 className="summary-card__title">오늘의 루틴</h3>
        <button className="summary-card__more" onClick={() => navigate('/routines')}>전체보기 →</button>
      </div>
      {routines.length === 0 ? (
        <p className="summary-card__empty">루틴을 생성해보세요</p>
      ) : (
        <div className="routine-summary__list">
          {display.map((r) => (
            <div key={r.id} className="routine-summary__item">
              <div className="routine-summary__header">
                <Badge label={r.timeOfDay} variant={r.timeOfDay === 'AM' ? 'am' : 'pm'} />
                <span className="routine-summary__name">{r.name}</span>
              </div>
              <ol className="routine-summary__steps">
                {r.steps.slice(0, 3).map((s) => <li key={s.order}>{s.cosmeticName}</li>)}
                {r.steps.length > 3 && <li className="routine-summary__more">+{r.steps.length - 3}개 더</li>}
              </ol>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
