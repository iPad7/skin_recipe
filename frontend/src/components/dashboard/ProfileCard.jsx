import { useState } from 'react';
import Badge from '../common/Badge';
import ProfileEditModal from './ProfileEditModal';
import './ProfileCard.css';

const SKIN_LABELS = { DRY: '건성', OILY: '지성', COMBINATION: '복합성', SENSITIVE: '민감성', NORMAL: '중성' };

export default function ProfileCard({ user }) {
  const [editing, setEditing] = useState(false);
  const skinLabel = SKIN_LABELS[user?.skinType] || user?.skinType;
  const concerns = user?.skinConcerns?.split(',').map((s) => s.trim()).filter(Boolean) || [];

  return (
    <>
      <div className="profile-card">
        <div className="profile-card__avatar">
          {user?.nickname?.slice(0, 2).toUpperCase()}
        </div>
        <div className="profile-card__info">
          <p className="profile-card__name">{user?.nickname}</p>
          <div className="profile-card__tags">
            {skinLabel && <Badge label={skinLabel} variant="green" />}
            {concerns.map((c) => <Badge key={c} label={c} />)}
          </div>
        </div>
        <button className="profile-card__edit-btn" onClick={() => setEditing(true)}>편집</button>
      </div>
      {editing && <ProfileEditModal onClose={() => setEditing(false)} />}
    </>
  );
}
