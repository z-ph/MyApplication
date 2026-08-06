import { useEffect, useState, type ReactNode } from 'react';
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

function LoadingGate() {
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

/**
 * Cold-start gate: if permissions not ready, land on /permission.
 * Fail closed → /permission when getStatus throws (tabs re-check also gates).
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
          // Fail closed: do not enter tabs without a known-ready status
          setTarget('/permission');
        }
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  if (!target) return <LoadingGate />;
  return <Navigate to={target} replace />;
}

/**
 * Hard gate for /tabs/* — re-check allReady so deep links cannot skip cold-start.
 * Web mock returns allReady:true so browser dev is unaffected.
 */
function TabsPermissionGate({ children }: { children: ReactNode }) {
  const [ready, setReady] = useState<boolean | null>(null);

  useEffect(() => {
    let cancelled = false;
    void (async () => {
      try {
        const s = await LingxiPermission.getStatus();
        if (!cancelled) setReady(!!s.allReady);
      } catch {
        if (!cancelled) setReady(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  if (ready === null) return <LoadingGate />;
  if (!ready) return <Navigate to="/permission" replace />;
  return <>{children}</>;
}

export function AppRouter() {
  return (
    <Routes>
      <Route path="/" element={<RootRedirect />} />
      <Route path="/permission" element={<PermissionPage />} />
      <Route path="/api-config" element={<ApiConfigPage />} />
      <Route path="/settings" element={<SettingsPage />} />
      <Route
        path="/tabs"
        element={
          <TabsPermissionGate>
            <TabLayout />
          </TabsPermissionGate>
        }
      >
        <Route index element={<Navigate to="chat" replace />} />
        <Route path="chat" element={<ChatPage />} />
        <Route path="logs" element={<LogsPage />} />
        <Route path="profile" element={<ProfilePage />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
