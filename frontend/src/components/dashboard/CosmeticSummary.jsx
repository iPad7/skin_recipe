import { useNavigate } from 'react-router-dom';
import Badge from '../common/Badge';
import './SummaryCard.css';

const CAT = { SKIN: '스킨', ESSENCE: '에센스', CREAM: '크림', SUNSCREEN: '선크림', CLEANSING: '클렌징', ETC: '기타' };

export default function CosmeticSummary({ cosmetics }) {
  const navigate = useNavigate();
  return (
    <div className="summary-card">
      <div className="summary-card__header">
        <h3 className="summary-card__title">내 화장품 <span className="summary-card__count">{cosmetics.length}개</span></h3>
        <button className="summary-card__more" onClick={() => navigate('/cosmetics')}>전체보기 →</button>
      </div>
      {cosmetics.length === 0 ? (
        <p className="summary-card__empty">화장품을 등록해보세요</p>
      ) : (
        <div className="cosmetic-summary__grid">
          {cosmetics.slice(0, 4).map((c) => (
            <div key={c.id} className="cosmetic-summary__item">
              {c.imageUrl && !c.imageUrl.toLowerCase().endsWith('.heic') && !c.imageUrl.toLowerCase().endsWith('.heif')
                ? <img src={`${import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'}${c.imageUrl}`} alt={c.name} className="cosmetic-summary__img" />
                : <div className="cosmetic-summary__img cosmetic-summary__img--placeholder">📦</div>
              }
              <p className="cosmetic-summary__name">{c.name}</p>
              <p className="cosmetic-summary__brand">{c.brand}</p>
              <Badge label={CAT[c.category] || c.category} variant="category" />
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
