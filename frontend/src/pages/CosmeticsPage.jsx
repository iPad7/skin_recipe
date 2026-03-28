import { useEffect, useState } from 'react';
import { getCosmetics, createCosmetic, updateCosmetic, deleteCosmetic } from '../api/cosmetics';
import CosmeticCard from '../components/cosmetics/CosmeticCard';
import CosmeticForm from '../components/cosmetics/CosmeticForm';
import OcrUploadForm from '../components/cosmetics/OcrUploadForm';
import OcrConfirmForm from '../components/cosmetics/OcrConfirmForm';
import Modal from '../components/common/Modal';
import Spinner from '../components/common/Spinner';
import './CosmeticsPage.css';

export default function CosmeticsPage() {
  const [cosmetics, setCosmetics] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  const [modal, setModal] = useState(null); // null | 'add' | 'edit' | 'ocr' | 'ocr-confirm'
  const [editing, setEditing] = useState(null);
  const [ocrResult, setOcrResult] = useState(null);

  const load = () => {
    setLoading(true);
    getCosmetics().then((r) => setCosmetics(r.data)).finally(() => setLoading(false));
  };

  useEffect(() => { load(); }, []);

  const closeModal = () => { setModal(null); setEditing(null); setOcrResult(null); };

  const handleAdd = async (form) => {
    setSaving(true);
    try { const r = await createCosmetic(form); setCosmetics((p) => [...p, r.data]); closeModal(); }
    finally { setSaving(false); }
  };

  const handleEdit = async (form) => {
    setSaving(true);
    try { const r = await updateCosmetic(editing.id, form); setCosmetics((p) => p.map((c) => c.id === editing.id ? r.data : c)); closeModal(); }
    finally { setSaving(false); }
  };

  const handleDelete = async (id) => {
    if (!confirm('정말 삭제하시겠습니까?')) return;
    try {
      await deleteCosmetic(id);
      setCosmetics((p) => p.filter((c) => c.id !== id));
    } catch (e) {
      alert(e.response?.data?.message || '삭제에 실패했습니다.');
    }
  };

  const handleOcrSuccess = (result) => {
    if (result.id) {
      // high confidence: 백엔드가 이미 저장한 CosmeticResponse
      setCosmetics((p) => [...p, result]);
      closeModal();
    } else {
      // low confidence: 저장 전 OcrParseResult → confirm 폼
      setOcrResult(result);
      setModal('ocr-confirm');
    }
  };
  const handleOcrConfirm = (cosmetic) => { setCosmetics((p) => [...p, cosmetic]); closeModal(); };

  const modalTitle = modal === 'add' ? '화장품 추가' : modal === 'edit' ? '화장품 수정' : modal === 'ocr' ? '이미지로 추가' : '이미지 분석 결과';

  return (
    <div className="cosmetics-page">
      <div className="cosmetics-page__header">
        <h1 className="cosmetics-page__title">내 화장품</h1>
        <div className="cosmetics-page__actions">
          <button className="cosmetics-page__btn cosmetics-page__btn--ghost" onClick={() => setModal('ocr')}>
            이미지로 추가
          </button>
          <button className="cosmetics-page__btn" onClick={() => setModal('add')}>
            + 직접 추가
          </button>
        </div>
      </div>

      {loading ? (
        <div className="cosmetics-page__loading"><Spinner size={24} /></div>
      ) : cosmetics.length === 0 ? (
        <div className="cosmetics-page__empty">
          <p>등록된 화장품이 없습니다.</p>
          <p>화장품을 추가해서 루틴을 만들어보세요.</p>
        </div>
      ) : (
        <div className="cosmetics-page__grid">
          {cosmetics.map((c) => (
            <CosmeticCard key={c.id} cosmetic={c}
              onEdit={(item) => { setEditing(item); setModal('edit'); }}
              onDelete={handleDelete} />
          ))}
        </div>
      )}

      {modal && (
        <Modal title={modalTitle} onClose={closeModal}>
          {modal === 'add' && <CosmeticForm onSubmit={handleAdd} onCancel={closeModal} loading={saving} />}
          {modal === 'edit' && <CosmeticForm initial={editing} onSubmit={handleEdit} onCancel={closeModal} loading={saving} />}
          {modal === 'ocr' && <OcrUploadForm onSuccess={handleOcrSuccess} onCancel={closeModal} />}
          {modal === 'ocr-confirm' && <OcrConfirmForm ocrResult={ocrResult} onSuccess={handleOcrConfirm} onCancel={closeModal} />}
        </Modal>
      )}
    </div>
  );
}
