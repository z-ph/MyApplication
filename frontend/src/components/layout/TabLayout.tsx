import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import { TabBar } from 'antd-mobile';
import {
  MessageOutline,
  UnorderedListOutline,
  UserOutline,
} from 'antd-mobile-icons';

const tabs = [
  { key: '/tabs/chat', title: 'Chat', icon: <MessageOutline /> },
  { key: '/tabs/logs', title: 'Logs', icon: <UnorderedListOutline /> },
  { key: '/tabs/profile', title: '我的', icon: <UserOutline /> },
] as const;

export function TabLayout() {
  const location = useLocation();
  const navigate = useNavigate();
  const activeKey =
    tabs.find((t) => location.pathname.startsWith(t.key))?.key ?? '/tabs/chat';

  return (
    <div className="app-shell">
      <div className="app-shell__content">
        <Outlet />
      </div>
      <TabBar
        activeKey={activeKey}
        onChange={(key) => navigate(key)}
        safeArea
      >
        {tabs.map((item) => (
          <TabBar.Item key={item.key} icon={item.icon} title={item.title} />
        ))}
      </TabBar>
    </div>
  );
}
