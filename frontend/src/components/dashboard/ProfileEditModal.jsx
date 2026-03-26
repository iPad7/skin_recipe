import { useState } from 'react';
import Modal from '../common/Modal';
import { updateMe, deleteMe } from '../../api/auth';
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
  const { user, updateUser, logout } = useAuth();
  const [form, setForm] = useState({
    nickname: user?.nickname || '',
    skinType: user?.skinType || 'NORMAL',
    skinConcerns: user?.skinConcerns || '',
    allergyIngredients: user?.allergyIngredients || '',
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [deleteInput, setDeleteInput] = useState('');
  const [deleteLoading, setDeleteLoading] = useState(false);

  const handleDelete = async () => {
    setDeleteLoading(true);
    try {
      await deleteMe();
      logout();
    } catch (err) {
      setError(err.response?.data?.message || '탈퇴에 실패했습니다.');
      setShowDeleteConfirm(false);
    } finally {
      setDeleteLoading(false);
    }
  };

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

        {!showDeleteConfirm ? (
          <div style={{ marginTop: '24px', borderTop: '1px solid #f0f0f0', paddingTop: '16px' }}>
            <button
              type="button"
              style={{ background: 'none', border: 'none', color: '#aaa', fontSize: '13px', cursor: 'pointer', padding: 0 }}
              onClick={() => setShowDeleteConfirm(true)}
            >
              회원 탈퇴
            </button>
          </div>
        ) : (
          <div style={{ marginTop: '24px', borderTop: '1px solid #fee2e2', paddingTop: '16px' }}>
            <p style={{ fontSize: '13px', color: '#ef4444', marginBottom: '8px' }}>
              탈퇴하면 모든 데이터가 삭제되며 복구할 수 없습니다.<br />
              확인하려면 아래에 <strong>회원탈퇴</strong>를 입력하세요.
            </p>
            <input
              value={deleteInput}
              onChange={(e) => setDeleteInput(e.target.value)}
              placeholder="회원탈퇴"
              style={{ width: '100%', padding: '8px', border: '1px solid #fca5a5', borderRadius: '6px', fontSize: '14px', boxSizing: 'border-box' }}
            />
            <div style={{ display: 'flex', gap: '8px', marginTop: '8px' }}>
              <button
                type="button"
                style={{ flex: 1, padding: '8px', background: 'none', border: '1px solid #ddd', borderRadius: '6px', cursor: 'pointer', fontSize: '13px' }}
                onClick={() => { setShowDeleteConfirm(false); setDeleteInput(''); }}
              >
                취소
              </button>
              <button
                type="button"
                disabled={deleteInput !== '회원탈퇴' || deleteLoading}
                onClick={handleDelete}
                style={{ flex: 1, padding: '8px', background: deleteInput === '회원탈퇴' ? '#ef4444' : '#fca5a5', color: '#fff', border: 'none', borderRadius: '6px', cursor: deleteInput === '회원탈퇴' ? 'pointer' : 'not-allowed', fontSize: '13px' }}
              >
                {deleteLoading ? '탈퇴 중...' : '탈퇴하기'}
              </button>
            </div>
          </div>
        )}
      </form>
    </Modal>
  );
}
