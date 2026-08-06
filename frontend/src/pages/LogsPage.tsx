import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Button,
  Empty,
  List,
  NavBar,
  SearchBar,
  Selector,
  Space,
  Tag,
  Toast,
} from 'antd-mobile';
import type { LogEntryDto } from '../types/bridge';
import { LingxiLog } from '../plugins/lingxi-log';

const LEVEL_OPTIONS = [
  { label: '全部', value: 'ALL' },
  { label: '错误', value: 'ERROR' },
  { label: '警告', value: 'WARN' },
  { label: '信息', value: 'INFO' },
  { label: '调试', value: 'DEBUG' },
];

function levelColor(level: string): 'danger' | 'warning' | 'primary' | 'default' {
  switch (level) {
    case 'ERROR':
      return 'danger';
    case 'WARN':
      return 'warning';
    case 'INFO':
      return 'primary';
    default:
      return 'default';
  }
}

/** Runtime logs from native Logger buffer via LingxiLog. */
export function LogsPage() {
  const [logs, setLogs] = useState<LogEntryDto[]>([]);
  const [query, setQuery] = useState('');
  const [level, setLevel] = useState<string>('ALL');
  const [loading, setLoading] = useState(false);

  const refresh = useCallback(async () => {
    setLoading(true);
    try {
      const { logs: next } = await LingxiLog.list();
      setLogs(next ?? []);
    } catch (e) {
      Toast.show({
        icon: 'fail',
        content: e instanceof Error ? e.message : '加载日志失败',
      });
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void refresh();
    let handle: { remove: () => Promise<void> } | undefined;
    void LingxiLog.addListener('logAppended', (ev) => {
      setLogs(ev.logs ?? []);
    }).then((h) => {
      handle = h;
    });
    return () => {
      void handle?.remove();
    };
  }, [refresh]);

  const filtered = useMemo(() => {
    return logs.filter((log) => {
      const levelOk = level === 'ALL' || log.level === level;
      if (!levelOk) return false;
      if (!query.trim()) return true;
      const q = query.trim().toLowerCase();
      return (
        log.message.toLowerCase().includes(q) ||
        log.tag.toLowerCase().includes(q)
      );
    });
  }, [logs, level, query]);

  const onClear = async () => {
    try {
      await LingxiLog.clear();
      setLogs([]);
      Toast.show({ content: '已清空' });
    } catch (e) {
      Toast.show({
        icon: 'fail',
        content: e instanceof Error ? e.message : '清空失败',
      });
    }
  };

  const onExport = async () => {
    try {
      const { text } = await LingxiLog.export();
      if (navigator.clipboard?.writeText) {
        await navigator.clipboard.writeText(text);
        Toast.show({ icon: 'success', content: '已复制到剪贴板' });
      } else {
        Toast.show({ content: text.slice(0, 120) + (text.length > 120 ? '…' : '') });
      }
    } catch (e) {
      Toast.show({
        icon: 'fail',
        content: e instanceof Error ? e.message : '导出失败',
      });
    }
  };

  return (
    <div className="app-shell" style={{ height: '100%' }}>
      <NavBar
        back={null}
        right={
          <Space style={{ '--gap': '8px' }}>
            <Button size="mini" fill="none" onClick={() => void refresh()}>
              刷新
            </Button>
            <Button size="mini" fill="none" onClick={() => void onExport()}>
              导出
            </Button>
            <Button size="mini" fill="none" color="danger" onClick={() => void onClear()}>
              清空
            </Button>
          </Space>
        }
      >
        运行日志 ({logs.length})
      </NavBar>
      <div className="page" style={{ paddingTop: 8, overflow: 'auto', height: '100%' }}>
        <SearchBar
          placeholder="搜索 tag / 消息"
          value={query}
          onChange={setQuery}
          style={{ marginBottom: 8 }}
          data-testid="logs-search"
        />
        <Selector
          options={LEVEL_OPTIONS}
          value={[level]}
          onChange={(v) => {
            if (v.length) setLevel(v[0] as string);
          }}
          style={{ marginBottom: 12 }}
        />
        {filtered.length === 0 ? (
          <Empty
            description={
              loading
                ? '加载中…'
                : query || level !== 'ALL'
                  ? '没有匹配的日志'
                  : '暂无日志'
            }
          />
        ) : (
          <List mode="card" data-testid="logs-list">
            {filtered
              .slice()
              .reverse()
              .map((log) => (
                <List.Item
                  key={log.id}
                  description={
                    <div style={{ fontFamily: 'monospace', whiteSpace: 'pre-wrap', wordBreak: 'break-word' }}>
                      {log.message}
                      {log.throwable ? (
                        <div style={{ color: 'var(--adm-color-danger)', marginTop: 4 }}>
                          {log.throwable.slice(0, 400)}
                          {log.throwable.length > 400 ? '…' : ''}
                        </div>
                      ) : null}
                    </div>
                  }
                  prefix={
                    <Tag color={levelColor(log.level)} style={{ minWidth: 48, textAlign: 'center' }}>
                      {log.level}
                    </Tag>
                  }
                >
                  <span style={{ fontSize: 12, color: '#666' }}>
                    {log.timestamp} · {log.tag}
                  </span>
                </List.Item>
              ))}
          </List>
        )}
      </div>
    </div>
  );
}
