import { useEffect, useState } from 'react';
import { NavBar, SpinLoading, Toast } from 'antd-mobile';
import {
  isNativePlatform,
  LingxiApp,
} from '../plugins/lingxi-app';
import type { AppVersionInfo } from '../types/bridge';

type BridgeState =
  | { kind: 'loading' }
  | { kind: 'ready'; version: AppVersionInfo; echo: string }
  | { kind: 'error'; message: string };

/**
 * Phase 1 home: NavBar + version via LingxiApp; TabBar shell only.
 * Chat business (send / sessions) is Task 3+.
 */
export function ChatPage() {
  const [bridge, setBridge] = useState<BridgeState>({ kind: 'loading' });

  useEffect(() => {
    let cancelled = false;

    async function probe() {
      try {
        const [version, echo] = await Promise.all([
          LingxiApp.getVersion(),
          LingxiApp.echo({ value: 'ok' }),
        ]);
        if (cancelled) return;
        setBridge({ kind: 'ready', version, echo: echo.value });
      } catch (e) {
        if (cancelled) return;
        const message = e instanceof Error ? e.message : String(e);
        setBridge({ kind: 'error', message });
        Toast.show({ icon: 'fail', content: `桥接失败: ${message}` });
      }
    }

    void probe();
    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <div className="app-shell" style={{ height: '100%' }}>
      <NavBar back={null}>灵犀</NavBar>
      <div className="page">
        <div className="page__card">
          <h1 className="page__title">Chat</h1>
          <p className="page__meta">Phase 1 壳工程 — 聊天业务尚未接入。</p>

          {bridge.kind === 'loading' && (
            <div style={{ marginTop: 16 }}>
              <SpinLoading color="primary" />
            </div>
          )}

          {bridge.kind === 'error' && (
            <div>
              <span className="bridge-status bridge-status--err">
                桥接错误
              </span>
              <p className="page__meta" style={{ marginTop: 8 }}>
                {bridge.message}
              </p>
            </div>
          )}

          {bridge.kind === 'ready' && (
            <>
              <p className="page__meta" style={{ marginTop: 12 }}>
                <strong>应用：</strong>
                {bridge.version.appName}
              </p>
              <p className="page__meta">
                <strong>版本：</strong>
                {bridge.version.versionName}
                {bridge.version.versionCode > 0
                  ? ` (${bridge.version.versionCode})`
                  : ''}
              </p>
              <p className="page__meta">
                <strong>包名：</strong>
                {bridge.version.packageName}
              </p>
              <p className="page__meta">
                <strong>echo：</strong>
                {bridge.echo}
              </p>
              <span
                className={
                  bridge.version.mock || !isNativePlatform()
                    ? 'bridge-status bridge-status--mock'
                    : 'bridge-status bridge-status--ok'
                }
                data-testid="bridge-status"
              >
                {bridge.version.mock || !isNativePlatform()
                  ? '浏览器 mock'
                  : '原生桥接 OK'}
              </span>
            </>
          )}

          <p className="page__hint">
            底栏：Chat / Logs / 我的。后续 Phase 接入权限门闸与 Agent。
          </p>
        </div>
      </div>
    </div>
  );
}
