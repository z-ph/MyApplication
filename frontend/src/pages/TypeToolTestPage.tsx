import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Button,
  Form,
  Input,
  List,
  NavBar,
  Space,
  TextArea,
  Toast,
} from 'antd-mobile';
import { LingxiShell } from '../plugins/lingxi-shell';

interface TypeResult {
  id: string;
  name: string;
  inputText: string;
  result: string;
  time: string;
}

/**
 * Type-tool test: focus an input, then call AutoService.inputText via LingxiShell.
 * Replaces Compose TypeToolTestScreen.
 */
export function TypeToolTestPage() {
  const navigate = useNavigate();
  const [field1, setField1] = useState('已有文本一');
  const [field2, setField2] = useState('已有文本二');
  const [field3, setField3] = useState('');
  const [testText, setTestText] = useState('测试输入');
  const [results, setResults] = useState<TypeResult[]>([]);
  const [busy, setBusy] = useState(false);

  const pushResult = (name: string, input: string, result: string) => {
    const now = new Date();
    const time = `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}:${String(now.getSeconds()).padStart(2, '0')}`;
    setResults((prev) => [
      {
        id: `${Date.now()}_${Math.random()}`,
        name,
        inputText: input,
        result,
        time,
      },
      ...prev,
    ]);
  };

  const onType = async () => {
    if (!testText) {
      Toast.show({ content: '请输入测试文本' });
      return;
    }
    setBusy(true);
    try {
      const r = await LingxiShell.inputText({ text: testText });
      const msg = r.success
        ? `✅ ${r.output || 'ok'}\n请检查上方输入框：覆盖还是追加？`
        : `❌ ${r.error ?? '失败'}`;
      pushResult('直接调用 type', testText, msg);
      if (!r.success) {
        Toast.show({ icon: 'fail', content: r.error ?? '失败' });
      }
    } catch (e) {
      pushResult(
        '直接调用 type',
        testText,
        `❌ ${e instanceof Error ? e.message : String(e)}`,
      );
    } finally {
      setBusy(false);
    }
  };

  const onClearThenType = async () => {
    setBusy(true);
    try {
      await LingxiShell.inputText({ text: '' });
      const r = await LingxiShell.inputText({ text: testText });
      pushResult(
        '先清除再输入',
        testText,
        r.success ? `✅ 先清除再输入: '${testText}'` : `❌ ${r.error ?? '失败'}`,
      );
    } catch (e) {
      pushResult(
        '先清除再输入',
        testText,
        `❌ ${e instanceof Error ? e.message : String(e)}`,
      );
    } finally {
      setBusy(false);
    }
  };

  const resetFields = () => {
    setField1('已有文本一');
    setField2('已有文本二');
    setField3('');
  };

  return (
    <div className="app-shell" style={{ height: '100%' }}>
      <NavBar onBack={() => navigate(-1)}>Type 工具测试</NavBar>
      <div className="page" style={{ overflow: 'auto' }}>
        <div className="page__card" style={{ marginBottom: 12 }}>
          <div style={{ fontWeight: 600, marginBottom: 8 }}>使用说明</div>
          <p className="page__meta" style={{ margin: 0 }}>
            1. 点击下方任一输入框，确保光标在框内<br />
            2. 填写要测试的文本<br />
            3. 点「直接执行 type」— 调用 AutoService.inputText<br />
            4. 观察是否【覆盖】或【追加】现有文本
          </p>
        </div>

        <Form layout="vertical" footer={null}>
          <Form.Item label="输入框 1（已有文本）">
            <Input value={field1} onChange={setField1} data-testid="type-field-1" />
          </Form.Item>
          <Form.Item label="输入框 2（已有文本）">
            <Input value={field2} onChange={setField2} data-testid="type-field-2" />
          </Form.Item>
          <Form.Item label="输入框 3（空）">
            <Input
              value={field3}
              onChange={setField3}
              placeholder="此输入框初始为空"
              data-testid="type-field-3"
            />
          </Form.Item>
          <Form.Item label="要输入的测试文本">
            <Input
              value={testText}
              onChange={setTestText}
              data-testid="type-test-text"
            />
          </Form.Item>
        </Form>

        <Space direction="vertical" block>
          <Button block fill="outline" onClick={resetFields}>
            重置输入框
          </Button>
          <Button
            block
            color="primary"
            loading={busy}
            disabled={!testText}
            onClick={() => void onType()}
            data-testid="type-run"
          >
            直接执行 type
          </Button>
          <Button
            block
            color="primary"
            fill="outline"
            loading={busy}
            disabled={!testText}
            onClick={() => void onClearThenType()}
            data-testid="type-clear-run"
          >
            清除 + 输入
          </Button>
          <Button
            block
            fill="outline"
            onClick={() => {
              setField1('');
              setField2('');
              setField3('');
              pushResult('清除所有输入框', '', '✅ 已清空 H5 输入框状态');
            }}
          >
            清空所有 H5 字段
          </Button>
        </Space>

        {results.length > 0 && (
          <List header={`测试结果 (${results.length})`} mode="card" style={{ marginTop: 16 }}>
            {results.map((r) => (
              <List.Item
                key={r.id}
                description={
                  <TextArea
                    value={r.result}
                    readOnly
                    rows={3}
                    style={{ fontFamily: 'monospace', fontSize: 12 }}
                  />
                }
                extra={<span style={{ fontSize: 12, color: '#999' }}>{r.time}</span>}
              >
                {r.name}
                {r.inputText ? ` · '${r.inputText}'` : ''}
              </List.Item>
            ))}
          </List>
        )}
      </div>
    </div>
  );
}
