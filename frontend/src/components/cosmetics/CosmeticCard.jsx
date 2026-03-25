import Badge from '../common/Badge';
import './CosmeticCard.css';

const CATEGORY_LABELS = {
  SKIN: '스킨/토너', ESSENCE: '에센스/세럼', CREAM: '크림/로션',
  SUNSCREEN: '선크림', CLEANSING: '클렌징', ETC: '기타',
};

export default function CosmeticCard({ cosmetic, onEdit, onDelete }) {
  return (
    <div className="cosmetic-card">
      <div className="cosmetic-card__header">
        <Badge variant="category">{CATEGORY_LABELS[cosmetic.category] ?? cosmetic.category}</Badge>
        <div className="cosmetic-card__actions">
          <button className="cosmetic-card__btn" onClick={() => onEdit(cosmetic)}>수정</button>
          <button className="cosmetic-card__btn cosmetic-card__btn--danger" onClick={() => onDelete(cosmetic.id)}>삭제</button>
        </div>
      </div>
      <p className="cosmetic-card__name">{cosmetic.name}</p>
      {cosmetic.brand && <p className="cosmetic-card__brand">{cosmetic.brand}</p>}
      {cosmetic.ingredients && (
        <p className="cosmetic-card__ingredients">{cosmetic.ingredients}</p>
      )}
    </div>
  );
}
