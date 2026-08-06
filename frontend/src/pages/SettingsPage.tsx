import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Button,
  List,
  NavBar,
  Space,
  Switch,
  Tag,
  Toast,
} from 'antd-mobile';
import type { AgentStateDto, PermissionStatusDto } from '../types/bridge';
import { LingxiAgent } from '../plugins/lingxi-agent';
import { LingxiChat } from '../plugins/lingxi-chat';
import { LingxiPermission } from '../plugins/lingxi-permission';

/**
 * Settings mapped from Compose MainScreen: agent status, permission shortcuts,
 * API config entry, reconfigure / test / cancel.
 */
export function SettingsPage() {
  const navigate = useNavigate();
  const [agent, setAgent] = useState<AgentStateDto | null>(null);
  const [perm, setPerm] = useState<PermissionStatusDto | null>(null);
  const [reconfiguring, setReconfiguring] = useState(false);
  const [testing, setTesting] = useState(false);
  const [stopping, setStopping] = useState(false);

  const refresh = useCallback(async () => {
    try {
      const [a, p] = await Promise.all([
        LingxiAgent.getState(),
        LingxiPermission.getStatus(),
      ]);
      setAgent(a);
      setPerm(p);
    } catch (e) {
      Toast.show({
        icon: 'fail',
        content: e instanceof Error ? e.message : '刷新失败',
      });
    }
  }, []);

  useEffect(() => {
    void refresh();
    let aHandle: { remove: () => Promise<void> } | undefined;
    let pHandle: { remove: () => Promise<void> } | undefined;
    void LingxiAgent.addListener('stateChanged', (s) => setAgent(s)).then(
      (h) => {
        aHandle = h;
      },
    );
    void LingxiPermission.addListener('statusChanged', (s) => setPerm(s)).then(
      (h) => {
        pHandle = h;
      },
    );
    const onVisible = () => {
      if (document.visibilityState === 'visible') void refresh();
    };
    document.addEventListener('visibilitychange', onVisible);
    return () => {
      void aHandle?.remove();
      void pHandle?.remove();
      document.removeEventListener('visibilitychange', onVisible);
    };
  }, [refresh]);

  const stateName = agent?.state ?? '—';
  const isRunning = agent?.state === 'RUNNING';
  const isReady = agent?.state === 'READY';

  const onReconfigure = async () => {
    setReconfiguring(true);
    try {
      await LingxiAgent.reconfigure();
      Toast.show({ icon: 'success', content: 'Agent 已重建' });
      await refresh();
    } catch (e) {
      Toast.show({
        icon: 'fail',
        content: e instanceof Error ? e.message : '重建失败',
      });
    } finally {
      setReconfiguring(false);
    }
  };

  const onTestAgent = async () => {
    setTesting(true);
    try {
      await LingxiChat.sendMessage({
        content: '分析一下当前屏幕，告诉我可以做什么',
      });
      Toast.show({ icon: 'success', content: '已发送测试任务' });
      navigate('/tabs/chat');
    } catch (e) {
      Toast.show({
        icon: 'fail',
        content: e instanceof Error ? e.message : '测试失败',
      });
    } finally {
      setTesting(false);
    }
  };

  const onStopTask = async () => {
    setStopping(true);
    try {
      await LingxiChat.cancelTask();
      Toast.show({ content: '已停止任务' });
      await refresh();
    } catch (e) {
      Toast.show({
        icon: 'fail',
        content: e instanceof Error ? e.message : '停止失败',
      });
    } finally {
      setStopping(false);
    }
  };

  return (
    <div className="app-shell" style={{ height: '100%' }}>
      <NavBar onBack={() => navigate(-1)}>Agent 设置</NavBar>
      <div className="page" style={{ overflow: 'auto' }}>
        <div className="page__card" style={{ marginBottom: 12 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <span style={{ fontWeight: 600 }}>Agent 状态</span>
            <Tag
              color={
                isRunning ? 'primary' : isReady ? 'success' : 'default'
              }
            >
              {stateName}
            </Tag>
          </div>
          {agent?.error && (
            <p className="page__meta" style={{ color: 'var(--adm-color-danger)' }}>
              {agent.error}
            </p>
          )}
          {agent?.result && (
            <p className="page__meta">结果：{agent.result}</p>
          )}
        </div>

        <List header="权限" mode="card">
          <List.Item
            extra={
              <Tag color={perm?.accessibility ? 'success' : 'warning'}>
                {perm?.accessibility ? '已开启' : '未开启'}
              </Tag>
            }
            onClick={() => void LingxiPermission.openAccessibilitySettings()}
          >
            无障碍服务
          </List.Item>
          <List.Item
            extra={
              <Tag color={perm?.screenCapture ? 'success' : 'warning'}>
                {perm?.screenCapture ? '已授权' : '未授权'}
              </Tag>
            }
            onClick={() =>
              void LingxiPermission.requestScreenCapture()
                .then(() => refresh())
                .catch((e: unknown) =>
                  Toast.show({
                    icon: 'fail',
                    content: e instanceof Error ? e.message : '授权失败',
                  }),
                )
            }
          >
            屏幕捕获
          </List.Item>
          <List.Item
            extra={
              <Tag color={perm?.overlay ? 'success' : 'warning'}>
                {perm?.overlay ? '已授权' : '未授权'}
              </Tag>
            }
            onClick={() => void LingxiPermission.requestOverlay()}
          >
            悬浮窗
          </List.Item>
          <List.Item
            description="打开完整权限引导页"
            onClick={() => navigate('/permission')}
            arrow
          >
            权限引导
          </List.Item>
        </List>

        <List header="配置" mode="card" style={{ marginTop: 12 }}>
          <List.Item
            description="多 Provider / 密钥 / 模型"
            onClick={() => navigate('/api-config')}
            arrow
            data-testid="settings-api-config"
          >
            API 配置管理
          </List.Item>
          <List.Item
            extra={
              <Tag color={perm?.apiConfigured ? 'success' : 'warning'}>
                {perm?.apiConfigured ? '已配置' : '未配置'}
              </Tag>
            }
          >
            活跃 API
          </List.Item>
        </List>

        <List header="Agent 控制" mode="card" style={{ marginTop: 12 }}>
          <List.Item
            extra={
              <Switch
                checked={isReady || isRunning}
                disabled
              />
            }
            description="由活跃 API 配置与 reconfigure 决定"
          >
            已就绪
          </List.Item>
        </List>

        <Space direction="vertical" block style={{ marginTop: 16 }}>
          <Button
            block
            color="primary"
            loading={reconfiguring}
            onClick={() => void onReconfigure()}
            data-testid="settings-reconfigure"
          >
            重建 Agent
          </Button>
          <Button
            block
            color="primary"
            fill="outline"
            loading={testing}
            disabled={!isReady && !isRunning}
            onClick={() => void onTestAgent()}
            data-testid="settings-test-agent"
          >
            测试 Agent
          </Button>
          <Button
            block
            color="danger"
            fill="outline"
            loading={stopping}
            disabled={!isRunning}
            onClick={() => void onStopTask()}
            data-testid="settings-stop-task"
          >
            停止任务
          </Button>
          <Button block fill="outline" onClick={() => void refresh()}>
            刷新状态
          </Button>
        </Space>
      </div>
    </div>
  );
}
