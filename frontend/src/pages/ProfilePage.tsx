import { NavBar } from 'antd-mobile';

/** Phase 1 empty shell — settings / API config in later tasks. */
export function ProfilePage() {
  return (
    <div className="app-shell" style={{ height: '100%' }}>
      <NavBar back={null}>我的</NavBar>
      <div className="page">
        <div className="page__card">
          <h1 className="page__title">我的</h1>
          <p className="page__meta">
            占位页。后续入口：设置、API 配置、调试工具。
          </p>
        </div>
      </div>
    </div>
  );
}
