import { useState } from 'react';
import { confirmOcr } from '../../api/cosmetics';
import './CosmeticForm.css';

const CATEGORIES = [
  { value: 'SKIN', label: '스킨/토너' }, { value: 'ESSENCE', label: '에센스/세럼' },
  { value: 'CREAM', label: '크림/로션' }, { value: 'SUNSCREEN', label: '선크림' },
  { value: 'CLEANSING', label: '클렌징' }, { value: 'ETC', label: '기타' },
];

const CATEGORY_MAP = {
  skin: 'SKIN', essence: 'ESSENCE', cream: 'CREAM',
  sunscreen: 'SUNSCREEN', cleansing: 'CLEANSING', etc: 'ETC',
};

function normalizeCategory(raw) {
  if (!raw) return 'ETC';
  const upper = raw.toUpperCase().trim();
  if (CATEGORIES.some((c) => c.value === upper)) return upper;
  return CATEGORY_MAP[raw.toLowerCase().trim()] ?? 'ETC';
}

export default function OcrConfirmForm({ ocrResult, onSuccess, onCancel }) {
  const [form, setForm] = useState({
    name: ocrResult?.name ?? '',
    brand: ocrResult?.brand ?? '',
    category: normalizeCategory(ocrResult?.category),
    ingredients: ocrResult?.ingredients ?? '',
    imageUrl: ocrResult?.imageUrl ?? '',
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const set = (k) => (e) => setForm((p) => ({ ...p, [k]: e.target.value }));

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.category) { setError('카테고리를 선택해주세요.'); return; }
    setError(''); setLoading(true);
    try {
      const res = await confirmOcr(form);
      onSuccess(res.data);
    } catch {
      setError('저장에 실패했습니다.');
    } finally { setLoading(false); }
  };

  return (
    <form className="cosmetic-form" onSubmit={handleSubmit}>
      <p style={{ fontSize: 12, color: 'var(--color-text-mid)', marginBottom: 4 }}>
        분석 결과를 확인하고 필요시 수정해주세요.
      </p>
      <div className="cosmetic-form__field">
        <label>제품명 *</label>
        <input value={form.name} onChange={set('name')} required />
      </div>
      <div className="cosmetic-form__field">
        <label>브랜드</label>
        <input value={form.brand} onChange={set('brand')} />
      </div>
      <div className="cosmetic-form__field">
        <label>카테고리 *</label>
        <div className="cosmetic-form__pills">
          {CATEGORIES.map((c) => (
            <button key={c.value} type="button"
              className={`cosmetic-form__pill ${form.category === c.value ? 'active' : ''}`}
              onClick={() => setForm((p) => ({ ...p, category: c.value }))}>
              {c.label}
            </button>
          ))}
        </div>
      </div>
      <div className="cosmetic-form__field">
        <label>성분</label>
        <textarea value={form.ingredients} onChange={set('ingredients')} rows={3} />
      </div>
      {error && <p className="cosmetic-form__error">{error}</p>}
      <div className="cosmetic-form__footer">
        <button type="button" className="cosmetic-form__btn cosmetic-form__btn--ghost" onClick={onCancel}>취소</button>
        <button type="submit" className="cosmetic-form__btn" disabled={loading}>
          {loading ? '저장 중...' : '저장'}
        </button>
      </div>
    </form>
  );
}
