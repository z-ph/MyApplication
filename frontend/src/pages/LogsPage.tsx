import { NavBar } from 'antd-mobile';

/** Phase 1 empty shell — LingxiLog plugin in a later task. */
export function LogsPage() {
  return (
    <div className="app-shell" style={{ height: '100%' }}>
      <NavBar back={null}>Logs</NavBar>
      <div className="page">
        <div className="page__card">
          <h1 className="page__title">运行日志</h1>
          <p className="page__meta">占位页。后续接入 LingxiLog.list / clear / export。</p>
        </div>
      </div>
    </div>
  );
}
