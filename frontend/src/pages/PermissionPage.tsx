import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button, List, NavBar, ProgressBar, Tag, Toast } from 'antd-mobile';
import type { PermissionStatusDto } from '../types/bridge';
import { LingxiPermission } from '../plugins/lingxi-permission';

const emptyStatus: PermissionStatusDto = {
  accessibility: false,
  overlay: false,
  screenCapture: false,
  appList: false,
  notification: false,
  apiConfigured: false,
  shizuku: false,
  allReady: false,
};

type RowKey =
  | 'accessibility'
  | 'overlay'
  | 'screenCapture'
  | 'appList'
  | 'apiConfigured';

interface RowDef {
  key: RowKey;
  title: string;
  description: string;
  actionLabel: string;
  optional?: boolean;
}

const ROWS: RowDef[] = [
  {
    key: 'accessibility',
    title: '无障碍服务',
    description: '用于自动化 UI 操作（点击、滑动、输入）',
    actionLabel: '去开启',
  },
  {
    key: 'overlay',
    title: '悬浮窗权限',
    description: '用于显示任务进度浮动窗口',
    actionLabel: '去授权',
  },
  {
    key: 'screenCapture',
    title: '屏幕录制',
    description: '用于捕获屏幕内容供 AI 分析',
    actionLabel: '授权',
  },
  {
    key: 'appList',
    title: '应用列表',
    description: '用于获取和启动其他应用',
    actionLabel: '刷新',
  },
  {
    key: 'apiConfigured',
    title: 'API 配置',
    description: '配置 AI 服务的密钥与模型',
    actionLabel: '去配置',
  },
];

/**
 * Cold-start permission gate. Navigates to chat when allReady.
 */
export function PermissionPage() {
  const navigate = useNavigate();
  const [status, setStatus] = useState<PermissionStatusDto>(emptyStatus);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState<string | null>(null);

  const applyStatus = useCallback((s: PermissionStatusDto) => {
    setStatus(s);
    if (s.allReady) {
      navigate('/tabs/chat', { replace: true });
    }
  }, [navigate]);

  const refresh = useCallback(async () => {
    try {
      const s = await LingxiPermission.refresh();
      applyStatus(s);
    } catch (e) {
      try {
        const s = await LingxiPermission.getStatus();
        applyStatus(s);
      } catch (err) {
        Toast.show({
          icon: 'fail',
          content: err instanceof Error ? err.message : '刷新失败',
        });
      }
    } finally {
      setLoading(false);
    }
  }, [applyStatus]);

  useEffect(() => {
    void refresh();
    let handle: { remove: () => Promise<void> } | undefined;
    void LingxiPermission.addListener('statusChanged', (s) => {
      applyStatus(s);
    }).then((h) => {
      handle = h;
    });

    const onVisible = () => {
      if (document.visibilityState === 'visible') {
        void refresh();
      }
    };
    document.addEventListener('visibilitychange', onVisible);
    window.addEventListener('focus', onVisible);

    return () => {
      void handle?.remove();
      document.removeEventListener('visibilitychange', onVisible);
      window.removeEventListener('focus', onVisible);
    };
  }, [refresh, applyStatus]);

  const onAction = async (row: RowDef) => {
    setBusy(row.key);
    try {
      switch (row.key) {
        case 'accessibility':
          await LingxiPermission.openAccessibilitySettings();
          break;
        case 'overlay':
          await LingxiPermission.requestOverlay();
          break;
        case 'screenCapture':
          await LingxiPermission.requestScreenCapture();
          break;
        case 'appList':
          await LingxiPermission.refresh();
          break;
        case 'apiConfigured':
          navigate('/api-config');
          break;
        default:
          break;
      }
      // Settings return: refresh after short delay
      if (row.key !== 'apiConfigured' && row.key !== 'screenCapture') {
        setTimeout(() => {
          void refresh();
        }, 400);
      } else if (row.key === 'screenCapture') {
        await refresh();
      }
    } catch (e) {
      Toast.show({
        icon: 'fail',
        content: e instanceof Error ? e.message : '操作失败',
      });
      void refresh();
    } finally {
      setBusy(null);
    }
  };

  const requiredKeys: RowKey[] = [
    'accessibility',
    'overlay',
    'screenCapture',
    'appList',
    'apiConfigured',
  ];
  const grantedCount = requiredKeys.filter((k) => status[k]).length;
  const totalCount = requiredKeys.length;
  const progress = totalCount === 0 ? 0 : grantedCount / totalCount;

  return (
    <div className="app-shell" style={{ height: '100%' }}>
      <NavBar back={null}>初始化设置</NavBar>
      <div className="page" style={{ overflow: 'auto' }}>
        <div className="page__card" style={{ marginBottom: 12 }}>
          <h1 className="page__title">欢迎使用灵犀</h1>
          <p className="page__meta">
            请完成以下设置以启用 AI 自动化功能。从系统设置返回后会自动刷新状态。
          </p>
        </div>

        <List header="必需权限" mode="card">
          {ROWS.map((row) => {
            const granted = status[row.key];
            return (
              <List.Item
                key={row.key}
                data-testid={`perm-${row.key}`}
                description={row.description}
                extra={
                  granted ? (
                    <Tag color="success" fill="outline">
                      已就绪
                    </Tag>
                  ) : (
                    <Button
                      size="mini"
                      color="primary"
                      loading={busy === row.key}
                      onClick={() => void onAction(row)}
                    >
                      {row.actionLabel}
                    </Button>
                  )
                }
              >
                {row.title}
              </List.Item>
            );
          })}
        </List>

        <div className="page__card" style={{ marginTop: 16 }}>
          <div style={{ marginBottom: 8, fontWeight: 600 }}>设置进度</div>
          <ProgressBar percent={Math.round(progress * 100)} />
          <p className="page__meta" style={{ marginTop: 8 }}>
            {loading
              ? '检测中…'
              : `${grantedCount} / ${totalCount} 项已完成`}
          </p>
          <Button
            block
            color="primary"
            disabled={!status.allReady}
            data-testid="perm-continue"
            style={{ marginTop: 12 }}
            onClick={() => navigate('/tabs/chat', { replace: true })}
          >
            进入主界面
          </Button>
        </div>
      </div>
    </div>
  );
}
