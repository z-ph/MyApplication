import { Navigate, Route, Routes } from 'react-router-dom';
import { TabLayout } from '../components/layout/TabLayout';
import { ChatPage } from '../pages/ChatPage';
import { LogsPage } from '../pages/LogsPage';
import { ProfilePage } from '../pages/ProfilePage';

export function AppRouter() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/tabs/chat" replace />} />
      <Route path="/tabs" element={<TabLayout />}>
        <Route index element={<Navigate to="chat" replace />} />
        <Route path="chat" element={<ChatPage />} />
        <Route path="logs" element={<LogsPage />} />
        <Route path="profile" element={<ProfilePage />} />
      </Route>
      <Route path="*" element={<Navigate to="/tabs/chat" replace />} />
    </Routes>
  );
}
