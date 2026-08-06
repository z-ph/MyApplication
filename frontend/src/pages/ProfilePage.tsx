import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { List, NavBar, Tag } from 'antd-mobile';
import type { AgentStateDto, AppVersionInfo } from '../types/bridge';
import { LingxiAgent } from '../plugins/lingxi-agent';
import { LingxiApp } from '../plugins/lingxi-app';

/**
 * Profile menu + agent status tag. Deep-links to settings / api-config.
 */
export function ProfilePage() {
  const navigate = useNavigate();
  const [agent, setAgent] = useState<AgentStateDto | null>(null);
  const [version, setVersion] = useState<AppVersionInfo | null>(null);

  useEffect(() => {
    void LingxiAgent.getState()
      .then(setAgent)
      .catch(() => undefined);
    void LingxiApp.getVersion()
      .then(setVersion)
      .catch(() => undefined);

    let handle: { remove: () => Promise<void> } | undefined;
    void LingxiAgent.addListener('stateChanged', setAgent).then((h) => {
      handle = h;
    });
    return () => {
      void handle?.remove();
    };
  }, []);

  const stateName = agent?.state ?? '—';
  const tagColor =
    stateName === 'RUNNING'
      ? 'primary'
      : stateName === 'READY'
        ? 'success'
        : stateName === 'ERROR'
          ? 'danger'
          : 'default';

  return (
    <div className="app-shell" style={{ height: '100%' }}>
      <NavBar back={null}>我的</NavBar>
      <div className="page" style={{ overflow: 'auto' }}>
        <div className="page__card" style={{ marginBottom: 12 }}>
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
            }}
          >
            <div>
              <h1 className="page__title" style={{ margin: 0 }}>
                灵犀
              </h1>
              <p className="page__meta">
                {version
                  ? `${version.versionName} (${version.versionCode})`
                  : '…'}
              </p>
            </div>
            <Tag color={tagColor} data-testid="profile-agent-tag">
              {stateName}
            </Tag>
          </div>
        </div>

        <List header="设置与配置" mode="card">
          <List.Item
            onClick={() => navigate('/settings')}
            arrow
            data-testid="menu-settings"
          >
            Agent 设置
          </List.Item>
          <List.Item
            onClick={() => navigate('/api-config')}
            arrow
            data-testid="menu-api-config"
            description="多模型 Provider 与密钥"
          >
            API 配置
          </List.Item>
          <List.Item
            onClick={() => navigate('/permission')}
            arrow
            data-testid="menu-permission"
          >
            权限状态
          </List.Item>
        </List>

        <List header="调试" mode="card" style={{ marginTop: 12 }}>
          <List.Item
            onClick={() => navigate('/api-test')}
            arrow
            data-testid="menu-api-test"
            description="Agent 提示词冒烟"
          >
            API 测试
          </List.Item>
          <List.Item
            onClick={() => navigate('/debug')}
            arrow
            data-testid="menu-debug"
            description="Shizuku / Shell / 包名"
          >
            调试工具
          </List.Item>
          <List.Item
            onClick={() => navigate('/type-tool-test')}
            arrow
            data-testid="menu-type-tool"
            description="AutoService.inputText 覆盖/追加"
          >
            Type 工具测试
          </List.Item>
        </List>
      </div>
    </div>
  );
}
