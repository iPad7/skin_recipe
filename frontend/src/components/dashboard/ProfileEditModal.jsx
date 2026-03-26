import { useState } from 'react';
import Modal from '../common/Modal';
import { updateMe } from '../../api/auth';
import { useAuth } from '../../context/AuthContext';
import '../cosmetics/CosmeticForm.css';

const SKIN_TYPES = [
  { value: 'DRY', label: '건성' },
  { value: 'OILY', label: '지성' },
  { value: 'COMBINATION', label: '복합성' },
  { value: 'SENSITIVE', label: '민감성' },
  { value: 'NORMAL', label: '중성' },
];

export default function ProfileEditModal({ onClose }) {
  const { user, updateUser } = useAuth();
  const [form, setForm] = useState({
    nickname: user?.nickname || '',
    skinType: user?.skinType || 'NORMAL',
    skinConcerns: user?.skinConcerns || '',
    allergyIngredients: user?.allergyIngredients || '',
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(''); setLoading(true);
    try {
      const res = await updateMe(form);
      updateUser(res.data);
      onClose();
    } catch (err) {
      setError(err.response?.data?.message || '수정에 실패했습니다.');
    } finally { setLoading(false); }
  };

  return (
    <Modal title="내 정보 수정" onClose={onClose}>
      <form onSubmit={handleSubmit} className="cosmetic-form">
        <div className="cosmetic-form__field">
          <label>닉네임</label>
          <input
            value={form.nickname}
            onChange={(e) => setForm((p) => ({ ...p, nickname: e.target.value }))}
            required
          />
        </div>
        <div className="cosmetic-form__field">
          <label>피부 타입</label>
          <select
            value={form.skinType}
            onChange={(e) => setForm((p) => ({ ...p, skinType: e.target.value }))}
          >
            {SKIN_TYPES.map((t) => (
              <option key={t.value} value={t.value}>{t.label}</option>
            ))}
          </select>
        </div>
        <div className="cosmetic-form__field">
          <label>피부 고민 <span style={{ fontSize: '12px', color: '#888' }}>(쉼표로 구분)</span></label>
          <input
            value={form.skinConcerns}
            onChange={(e) => setForm((p) => ({ ...p, skinConcerns: e.target.value }))}
            placeholder="예: 여드름, 모공, 보습"
          />
        </div>
        <div className="cosmetic-form__field">
          <label>알레르기 성분 <span style={{ fontSize: '12px', color: '#888' }}>(쉼표로 구분)</span></label>
          <input
            value={form.allergyIngredients}
            onChange={(e) => setForm((p) => ({ ...p, allergyIngredients: e.target.value }))}
            placeholder="예: 향료, 알코올"
          />
        </div>
        {error && <p className="cosmetic-form__error">{error}</p>}
        <div className="cosmetic-form__footer">
          <button type="button" className="cosmetic-form__btn cosmetic-form__btn--ghost" onClick={onClose}>취소</button>
          <button type="submit" className="cosmetic-form__btn" disabled={loading}>
            {loading ? '저장 중...' : '저장'}
          </button>
        </div>
      </form>
    </Modal>
  );
}
