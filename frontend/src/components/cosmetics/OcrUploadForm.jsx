import { useRef, useState } from 'react';
import { uploadOcr } from '../../api/cosmetics';
import Spinner from '../common/Spinner';
import './OcrUploadForm.css';

function ImageZone({ label, file, preview, inputRef, onChange }) {
  return (
    <div className="ocr-upload__slot">
      <p className="ocr-upload__slot-label">{label}</p>
      <div className="ocr-upload__zone" onClick={() => inputRef.current?.click()}>
        {preview ? (
          <img src={preview} alt={label} className="ocr-upload__preview" />
        ) : (
          <div className="ocr-upload__placeholder">
            <span className="ocr-upload__icon">📸</span>
            <p className="ocr-upload__hint">클릭하여 업로드</p>
          </div>
        )}
      </div>
      <input ref={inputRef} type="file" accept="image/*" onChange={onChange} className="ocr-upload__input" />
    </div>
  );
}

export default function OcrUploadForm({ onSuccess, onCancel }) {
  const frontRef = useRef(null);
  const backRef = useRef(null);

  const [front, setFront] = useState({ file: null, preview: null });
  const [back, setBack] = useState({ file: null, preview: null });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleChange = (setter) => (e) => {
    const f = e.target.files?.[0];
    if (!f) return;
    setter({ file: f, preview: URL.createObjectURL(f) });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!front.file || !back.file) {
      setError('앞면과 뒷면 이미지를 모두 선택해주세요.');
      return;
    }
    setError(''); setLoading(true);
    try {
      const formData = new FormData();
      formData.append('frontImage', front.file);
      formData.append('backImage', back.file);
      const res = await uploadOcr(formData);
      onSuccess(res.data);
    } catch {
      setError('이미지 분석에 실패했습니다. 다시 시도해주세요.');
    } finally { setLoading(false); }
  };

  const canSubmit = front.file && back.file;

  return (
    <form className="ocr-upload" onSubmit={handleSubmit}>
      <div className="ocr-upload__grid">
        <ImageZone label="앞면" file={front.file} preview={front.preview}
          inputRef={frontRef} onChange={handleChange(setFront)} />
        <ImageZone label="뒷면" file={back.file} preview={back.preview}
          inputRef={backRef} onChange={handleChange(setBack)} />
      </div>
      {error && <p className="ocr-upload__error">{error}</p>}
      <div className="ocr-upload__footer">
        <button type="button" className="ocr-upload__btn ocr-upload__btn--ghost" onClick={onCancel}>취소</button>
        <button type="submit" className="ocr-upload__btn" disabled={loading || !canSubmit}>
          {loading ? <><Spinner size={14} /> 분석 중...</> : '이미지 분석'}
        </button>
      </div>
    </form>
  );
}
