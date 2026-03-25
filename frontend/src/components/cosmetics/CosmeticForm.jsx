import { useState } from 'react';
import './CosmeticForm.css';

const CATEGORIES = [
  { value: 'SKIN', label: '스킨/토너' }, { value: 'ESSENCE', label: '에센스/세럼' },
  { value: 'CREAM', label: '크림/로션' }, { value: 'SUNSCREEN', label: '선크림' },
  { value: 'CLEANSING', label: '클렌징' }, { value: 'ETC', label: '기타' },
];

export default function CosmeticForm({ initial, onSubmit, onCancel, loading }) {
  const [form, setForm] = useState({
    name: initial?.name ?? '',
    brand: initial?.brand ?? '',
    category: initial?.category ?? '',
    ingredients: initial?.ingredients ?? '',
  });
  const [error, setError] = useState('');

  const set = (k) => (e) => setForm((p) => ({ ...p, [k]: e.target.value }));

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!form.category) { setError('카테고리를 선택해주세요.'); return; }
    setError('');
    onSubmit(form);
  };

  return (
    <form className="cosmetic-form" onSubmit={handleSubmit}>
      <div className="cosmetic-form__field">
        <label>제품명 *</label>
        <input value={form.name} onChange={set('name')} placeholder="토너 이름" required />
      </div>
      <div className="cosmetic-form__field">
        <label>브랜드</label>
        <input value={form.brand} onChange={set('brand')} placeholder="브랜드명" />
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
        <textarea value={form.ingredients} onChange={set('ingredients')}
          placeholder="성분을 입력하세요 (선택)" rows={3} />
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
