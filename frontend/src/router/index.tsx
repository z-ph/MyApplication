import { useEffect, useState } from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import { SpinLoading } from 'antd-mobile';
import { TabLayout } from '../components/layout/TabLayout';
import { ChatPage } from '../pages/ChatPage';
import { LogsPage } from '../pages/LogsPage';
import { ProfilePage } from '../pages/ProfilePage';
import { PermissionPage } from '../pages/PermissionPage';
import { ApiConfigPage } from '../pages/ApiConfigPage';
import { SettingsPage } from '../pages/SettingsPage';
import { LingxiPermission } from '../plugins/lingxi-permission';

/**
 * Cold-start gate: if permissions not ready, land on /permission.
 */
function RootRedirect() {
  const [target, setTarget] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    void (async () => {
      try {
        const s = await LingxiPermission.getStatus();
        if (!cancelled) {
          setTarget(s.allReady ? '/tabs/chat' : '/permission');
        }
      } catch {
        if (!cancelled) {
          // Fail open to chat so shell still usable if plugin missing
          setTarget('/tabs/chat');
        }
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  if (!target) {
    return (
      <div
        style={{
          height: '100%',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
        }}
      >
        <SpinLoading style={{ '--size': '36px' }} />
      </div>
    );
  }
  return <Navigate to={target} replace />;
}

export function AppRouter() {
  return (
    <Routes>
      <Route path="/" element={<RootRedirect />} />
      <Route path="/permission" element={<PermissionPage />} />
      <Route path="/api-config" element={<ApiConfigPage />} />
      <Route path="/settings" element={<SettingsPage />} />
      <Route path="/tabs" element={<TabLayout />}>
        <Route index element={<Navigate to="chat" replace />} />
        <Route path="chat" element={<ChatPage />} />
        <Route path="logs" element={<LogsPage />} />
        <Route path="profile" element={<ProfilePage />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
