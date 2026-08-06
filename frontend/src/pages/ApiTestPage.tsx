import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Button,
  Form,
  NavBar,
  Space,
  Tag,
  TextArea,
  Toast,
} from 'antd-mobile';
import type { AgentStateDto } from '../types/bridge';
import { LingxiAgent } from '../plugins/lingxi-agent';
import { LingxiChat } from '../plugins/lingxi-chat';

/**
 * Agent prompt smoke test (replaces Compose ApiTestScreen).
 * Sends via LingxiChat so Room + floating window stay consistent.
 */
export function ApiTestPage() {
  const navigate = useNavigate();
  const [prompt, setPrompt] = useState('你好，请介绍一下你自己');
  const [agent, setAgent] = useState<AgentStateDto | null>(null);
  const [result, setResult] = useState<string | null>(null);
  const [testing, setTesting] = useState(false);

  const refresh = useCallback(async () => {
    try {
      setAgent(await LingxiAgent.getState());
    } catch {
      /* ignore */
    }
  }, []);

  useEffect(() => {
    void refresh();
    let handle: { remove: () => Promise<void> } | undefined;
    void LingxiAgent.addListener('stateChanged', (s) => {
      setAgent(s);
      if (s.state === 'COMPLETED' && s.result) {
        setResult(`✅ ${s.result}`);
        setTesting(false);
      } else if (s.state === 'ERROR' && s.error) {
        setResult(`❌ ${s.error}`);
        setTesting(false);
      } else if (s.state === 'CANCELLED') {
        setResult('⏹️ 已取消');
        setTesting(false);
      }
    }).then((h) => {
      handle = h;
    });
    return () => {
      void handle?.remove();
    };
  }, [refresh]);

  const isRunning = agent?.state === 'RUNNING' || testing;

  const onTest = async () => {
    if (!prompt.trim()) {
      Toast.show({ content: '请输入提示词' });
      return;
    }
    setTesting(true);
    setResult(null);
    try {
      await LingxiChat.sendMessage({ content: prompt.trim() });
      Toast.show({ content: '已发送，见聊天页与悬浮窗' });
    } catch (e) {
      setTesting(false);
      setResult(`❌ ${e instanceof Error ? e.message : '发送失败'}`);
      Toast.show({
        icon: 'fail',
        content: e instanceof Error ? e.message : '测试失败',
      });
    }
  };

  const onStop = async () => {
    try {
      await LingxiChat.cancelTask();
      setTesting(false);
      Toast.show({ content: '已停止' });
    } catch (e) {
      Toast.show({
        icon: 'fail',
        content: e instanceof Error ? e.message : '停止失败',
      });
    }
  };

  const stateName = agent?.state ?? '—';
  const tagColor =
    agent?.state === 'RUNNING'
      ? 'primary'
      : agent?.state === 'READY'
        ? 'success'
        : agent?.state === 'ERROR'
          ? 'danger'
          : 'default';

  return (
    <div className="app-shell" style={{ height: '100%' }}>
      <NavBar onBack={() => navigate(-1)}>API / Agent 测试</NavBar>
      <div className="page" style={{ overflow: 'auto' }}>
        <div className="page__card" style={{ marginBottom: 12 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 8 }}>
            <span style={{ fontWeight: 600 }}>Agent 状态</span>
            <Tag color={tagColor} data-testid="api-test-agent-tag">
              {stateName}
            </Tag>
          </div>
          <p className="page__meta">
            通过 LingxiChat.sendMessage 走完整任务链路（会话、Agent、悬浮窗）。
            若未就绪，请先在 API 配置中添加 Key 并重建 Agent。
          </p>
        </div>

        <Form layout="vertical" footer={null}>
          <Form.Item label="测试提示词">
            <TextArea
              value={prompt}
              onChange={setPrompt}
              rows={3}
              placeholder="输入要测试的问题"
              data-testid="api-test-prompt"
            />
          </Form.Item>
        </Form>

        <Space direction="vertical" block>
          <Button
            block
            color="primary"
            loading={testing && isRunning}
            disabled={!prompt.trim() || (isRunning && testing)}
            onClick={() => void onTest()}
            data-testid="api-test-run"
          >
            发送测试
          </Button>
          <Button
            block
            color="danger"
            fill="outline"
            disabled={!isRunning}
            onClick={() => void onStop()}
            data-testid="api-test-stop"
          >
            停止
          </Button>
          <Button block fill="outline" onClick={() => navigate('/tabs/chat')}>
            打开聊天
          </Button>
        </Space>

        {result && (
          <div
            className="page__card"
            style={{ marginTop: 16, fontFamily: 'monospace', whiteSpace: 'pre-wrap' }}
            data-testid="api-test-result"
          >
            <div style={{ fontWeight: 600, marginBottom: 8 }}>结果</div>
            {result}
          </div>
        )}
      </div>
    </div>
  );
}
