import Badge from '../common/Badge';
import './RoutineCard.css';

export default function RoutineCard({ routine, onDelete }) {
  const isAm = routine.timeOfDay === 'AM';

  return (
    <div className="routine-card">
      <div className="routine-card__header">
        <div className="routine-card__title-row">
          <Badge variant={isAm ? 'am' : 'pm'}>{isAm ? 'AM' : 'PM'}</Badge>
          <h3 className="routine-card__name">{routine.name}</h3>
        </div>
        <button className="routine-card__delete" onClick={() => onDelete(routine.id)}>삭제</button>
      </div>
      {routine.description && (
        <p className="routine-card__desc">{routine.description}</p>
      )}
      {routine.steps?.length > 0 && (
        <ol className="routine-card__steps">
          {routine.steps.map((step) => (
            <li key={step.order} className="routine-card__step">
              <span className="routine-card__step-num">{step.order}</span>
              <span>{step.cosmeticName}</span>
            </li>
          ))}
        </ol>
      )}
    </div>
  );
}
