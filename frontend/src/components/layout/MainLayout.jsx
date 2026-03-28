import { useState } from 'react';
import { Outlet } from 'react-router-dom';
import Sidebar from './Sidebar';
import './MainLayout.css';

export default function MainLayout() {
  const [sidebarOpen, setSidebarOpen] = useState(false);

  return (
    <div className="main-layout">
      <Sidebar open={sidebarOpen} onClose={() => setSidebarOpen(false)} />
      {sidebarOpen && (
        <div className="main-layout__overlay" onClick={() => setSidebarOpen(false)} />
      )}
      <main className="main-layout__content">
        <button
          className="main-layout__hamburger"
          onClick={() => setSidebarOpen(true)}
          aria-label="메뉴 열기"
        >
          ☰
        </button>
        <Outlet />
      </main>
    </div>
  );
}
