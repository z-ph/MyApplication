import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Button,
  Form,
  Input,
  List,
  NavBar,
  Space,
  Tag,
  TextArea,
  Toast,
} from 'antd-mobile';
import type {
  ShellCommandResult,
  ShellTestResultDto,
  ShizukuStatusDto,
} from '../types/bridge';
import { LingxiShell } from '../plugins/lingxi-shell';

/**
 * Shell / package debug page (replaces Compose DebugTestScreen).
 */
export function DebugTestPage() {
  const navigate = useNavigate();
  const [shizuku, setShizuku] = useState<ShizukuStatusDto | null>(null);
  const [command, setCommand] = useState('pm list packages -3 | head -20');
  const [cmdOut, setCmdOut] = useState<string>('');
  const [tests, setTests] = useState<ShellTestResultDto[]>([]);
  const [running, setRunning] = useState(false);
  const [busy, setBusy] = useState(false);

  const refreshShizuku = useCallback(async () => {
    try {
      setShizuku(await LingxiShell.getShizukuStatus());
    } catch (e) {
      Toast.show({
        icon: 'fail',
        content: e instanceof Error ? e.message : 'Shizuku 状态失败',
      });
    }
  }, []);

  useEffect(() => {
    void refreshShizuku();
  }, [refreshShizuku]);

  const onRunTests = async () => {
    setRunning(true);
    try {
      const { results } = await LingxiShell.runPackageTests();
      setTests(results ?? []);
    } catch (e) {
      Toast.show({
        icon: 'fail',
        content: e instanceof Error ? e.message : '测试失败',
      });
    } finally {
      setRunning(false);
    }
  };

  const onRunCommand = async () => {
    if (!command.trim()) return;
    setBusy(true);
    try {
      const r = await LingxiShell.runCommand({ command: command.trim() });
      setCmdOut(formatResult(r));
    } catch (e) {
      setCmdOut(e instanceof Error ? e.message : String(e));
    } finally {
      setBusy(false);
    }
  };

  const onListPackages = async () => {
    setBusy(true);
    try {
      const { packages } = await LingxiShell.listPackages({ limit: 30 });
      const lines = (packages ?? [])
        .slice(0, 30)
        .map((p) => `${p.label} = ${p.packageName}`);
      setCmdOut(lines.join('\n') || '(empty)');
    } catch (e) {
      setCmdOut(e instanceof Error ? e.message : String(e));
    } finally {
      setBusy(false);
    }
  };

  const onLaunch = async (nameOrPackage: string) => {
    setBusy(true);
    try {
      const r = await LingxiShell.launchApp({ nameOrPackage });
      setCmdOut(formatResult(r));
      Toast.show({ content: r.success ? '已尝试启动' : r.error ?? '失败' });
    } catch (e) {
      Toast.show({
        icon: 'fail',
        content: e instanceof Error ? e.message : '启动失败',
      });
    } finally {
      setBusy(false);
    }
  };

  const shizukuLabel =
    shizuku?.status === 'ready'
      ? '已就绪'
      : shizuku?.status === 'available'
        ? '可用但未授权'
        : '未安装 / 不可用';
  const shizukuColor =
    shizuku?.status === 'ready'
      ? 'success'
      : shizuku?.status === 'available'
        ? 'warning'
        : 'danger';

  return (
    <div className="app-shell" style={{ height: '100%' }}>
      <NavBar onBack={() => navigate(-1)}>调试工具</NavBar>
      <div className="page" style={{ overflow: 'auto' }}>
        <div className="page__card" style={{ marginBottom: 12 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span style={{ fontWeight: 600 }}>Shizuku</span>
            <Tag color={shizukuColor} data-testid="debug-shizuku-tag">
              {shizukuLabel}
            </Tag>
          </div>
          <Button size="mini" fill="none" onClick={() => void refreshShizuku()} style={{ marginTop: 8 }}>
            刷新状态
          </Button>
        </div>

        <Space direction="vertical" block style={{ marginBottom: 12 }}>
          <Button
            block
            color="primary"
            loading={running}
            onClick={() => void onRunTests()}
            data-testid="debug-run-tests"
          >
            运行包名 / Shell 测试套件
          </Button>
        </Space>

        {tests.length > 0 && (
          <List header="测试结果" mode="card" style={{ marginBottom: 12 }}>
            {tests.map((t) => (
              <List.Item
                key={t.name}
                description={
                  <pre style={{ margin: 0, fontFamily: 'monospace', whiteSpace: 'pre-wrap' }}>
                    {t.message}
                  </pre>
                }
                extra={
                  <Tag color={t.success ? 'success' : 'danger'}>
                    {t.durationMs}ms
                  </Tag>
                }
              >
                {t.name}
              </List.Item>
            ))}
          </List>
        )}

        <div className="page__card" style={{ marginBottom: 12 }}>
          <div style={{ fontWeight: 600, marginBottom: 8 }}>快捷操作</div>
          <Space wrap>
            <Button size="small" loading={busy} onClick={() => void onLaunch('微信')}>
              打开微信
            </Button>
            <Button size="small" loading={busy} onClick={() => void onLaunch('设置')}>
              打开设置
            </Button>
            <Button size="small" loading={busy} onClick={() => void onListPackages()}>
              列出应用
            </Button>
          </Space>
        </div>

        <Form layout="vertical" footer={null}>
          <Form.Item label="Shell 命令">
            <Input
              value={command}
              onChange={setCommand}
              placeholder="pm list packages"
              data-testid="debug-command-input"
            />
          </Form.Item>
        </Form>
        <Button
          block
          color="primary"
          fill="outline"
          loading={busy}
          onClick={() => void onRunCommand()}
          data-testid="debug-run-command"
        >
          执行命令
        </Button>

        {cmdOut && (
          <div className="page__card" style={{ marginTop: 16 }}>
            <div style={{ fontWeight: 600, marginBottom: 8 }}>输出</div>
            <TextArea
              value={cmdOut}
              readOnly
              rows={10}
              style={{ fontFamily: 'monospace', fontSize: 12 }}
              data-testid="debug-output"
            />
          </div>
        )}
      </div>
    </div>
  );
}

function formatResult(r: ShellCommandResult): string {
  const parts = [
    `success=${r.success}`,
    r.exitCode != null ? `exitCode=${r.exitCode}` : null,
    r.output ? `--- output ---\n${r.output}` : null,
    r.error ? `--- error ---\n${r.error}` : null,
  ].filter(Boolean);
  return parts.join('\n');
}
