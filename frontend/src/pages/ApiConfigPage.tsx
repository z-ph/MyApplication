import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Button,
  Dialog,
  Form,
  Input,
  List,
  NavBar,
  Popup,
  Selector,
  Space,
  Tag,
  TextArea,
  Toast,
} from 'antd-mobile';
import { AddOutline } from 'antd-mobile-icons';
import type { ApiConfigDto, ProviderInfo } from '../types/bridge';
import { LingxiApiConfig } from '../plugins/lingxi-api-config';

const FALLBACK_PROVIDERS: ProviderInfo[] = [
  {
    id: 'OPENAI',
    displayName: 'OpenAI',
    defaultBaseUrl: 'https://api.openai.com/v1',
    defaultModel: 'gpt-4o',
  },
  {
    id: 'ZHIPU',
    displayName: '智谱 AI',
    defaultBaseUrl: 'https://open.bigmodel.cn/api/paas/v4',
    defaultModel: 'glm-4',
  },
  {
    id: 'DEEPSEEK',
    displayName: 'DeepSeek',
    defaultBaseUrl: 'https://api.deepseek.com/v1',
    defaultModel: 'deepseek-chat',
  },
  {
    id: 'CUSTOM',
    displayName: '自定义 API',
    defaultBaseUrl: '',
    defaultModel: '',
  },
];

interface FormState {
  name: string;
  provider: string;
  apiKey: string;
  baseUrl: string;
  modelId: string;
}

const emptyForm = (): FormState => ({
  name: '',
  provider: 'OPENAI',
  apiKey: '',
  baseUrl: FALLBACK_PROVIDERS[0].defaultBaseUrl,
  modelId: FALLBACK_PROVIDERS[0].defaultModel,
});

/**
 * Multi-provider API config management (list / create / edit / setActive / test).
 */
export function ApiConfigPage() {
  const navigate = useNavigate();
  const [configs, setConfigs] = useState<ApiConfigDto[]>([]);
  const [providers, setProviders] = useState<ProviderInfo[]>(FALLBACK_PROVIDERS);
  const [popupOpen, setPopupOpen] = useState(false);
  const [editing, setEditing] = useState<ApiConfigDto | null>(null);
  const [form, setForm] = useState<FormState>(emptyForm);
  const [saving, setSaving] = useState(false);
  const [testing, setTesting] = useState(false);
  const [fetchingModels, setFetchingModels] = useState(false);
  const [models, setModels] = useState<string[]>([]);

  const load = useCallback(async () => {
    try {
      const res = await LingxiApiConfig.list();
      setConfigs(res.configs ?? []);
    } catch (e) {
      Toast.show({
        icon: 'fail',
        content: e instanceof Error ? e.message : '加载失败',
      });
    }
  }, []);

  useEffect(() => {
    void load();
    void LingxiApiConfig.listProviders?.()
      .then((r) => {
        if (r.providers?.length) setProviders(r.providers);
      })
      .catch(() => undefined);

    let handle: { remove: () => Promise<void> } | undefined;
    void LingxiApiConfig.addListener('configsChanged', (data) => {
      setConfigs(data.configs ?? []);
    }).then((h) => {
      handle = h;
    });
    return () => {
      void handle?.remove();
    };
  }, [load]);

  const providerOptions = useMemo(
    () =>
      providers.map((p) => ({
        label: p.displayName,
        value: p.id,
      })),
    [providers],
  );

  const openCreate = () => {
    setEditing(null);
    const p = providers[0] ?? FALLBACK_PROVIDERS[0];
    setForm({
      name: '',
      provider: p.id,
      apiKey: '',
      baseUrl: p.defaultBaseUrl,
      modelId: p.defaultModel,
    });
    setModels([]);
    setPopupOpen(true);
  };

  const openEdit = (c: ApiConfigDto) => {
    setEditing(c);
    setForm({
      name: c.name,
      provider: c.provider,
      apiKey: '',
      baseUrl: c.baseUrl,
      modelId: c.modelId,
    });
    setModels([]);
    setPopupOpen(true);
  };

  const onProviderChange = (vals: string[]) => {
    const id = vals[0];
    if (!id) return;
    const p = providers.find((x) => x.id === id);
    setForm((f) => ({
      ...f,
      provider: id,
      baseUrl: p?.defaultBaseUrl ?? f.baseUrl,
      modelId: p?.defaultModel ?? f.modelId,
    }));
  };

  const onSave = async () => {
    if (!form.provider) {
      Toast.show({ content: '请选择 Provider' });
      return;
    }
    if (!editing && !form.apiKey.trim()) {
      Toast.show({ content: '请填写 API Key' });
      return;
    }
    setSaving(true);
    try {
      const result = editing
        ? await LingxiApiConfig.update({
            id: editing.id,
            name: form.name,
            provider: form.provider,
            apiKey: form.apiKey || undefined,
            baseUrl: form.baseUrl,
            modelId: form.modelId,
          })
        : await LingxiApiConfig.create({
            name: form.name || form.provider,
            provider: form.provider,
            apiKey: form.apiKey,
            baseUrl: form.baseUrl,
            modelId: form.modelId,
          });
      if (result.reconfigured === false) {
        Toast.show({
          icon: 'fail',
          content: '配置已保存，但 Agent 重建失败',
        });
      } else {
        Toast.show({ icon: 'success', content: '已保存' });
      }
      setPopupOpen(false);
      await load();
    } catch (e) {
      Toast.show({
        icon: 'fail',
        content: e instanceof Error ? e.message : '保存失败',
      });
    } finally {
      setSaving(false);
    }
  };

  const onDelete = async (c: ApiConfigDto) => {
    const ok = await Dialog.confirm({
      content: `确定删除配置「${c.name}」？`,
    });
    if (!ok) return;
    try {
      await LingxiApiConfig.delete({ id: c.id });
      Toast.show({ content: '已删除' });
      await load();
    } catch (e) {
      Toast.show({
        icon: 'fail',
        content: e instanceof Error ? e.message : '删除失败',
      });
    }
  };

  const onSetActive = async (c: ApiConfigDto) => {
    try {
      const r = await LingxiApiConfig.setActive({ id: c.id });
      if (r && 'reconfigured' in r && r.reconfigured === false) {
        Toast.show({
          icon: 'fail',
          content: '已设为活跃，但 Agent 重建失败',
        });
      } else {
        Toast.show({ icon: 'success', content: '已设为活跃' });
      }
      await load();
    } catch (e) {
      Toast.show({
        icon: 'fail',
        content: e instanceof Error ? e.message : '设置失败',
      });
    }
  };

  const onTest = async () => {
    if (!editing && !form.apiKey.trim()) {
      Toast.show({ content: '测连需要 API Key' });
      return;
    }
    setTesting(true);
    try {
      const r = await LingxiApiConfig.testConnection({
        provider: form.provider,
        apiKey: form.apiKey,
        baseUrl: form.baseUrl,
        modelId: form.modelId,
        // Edit form leaves key blank → native loads Room secret via configId
        configId: editing?.id,
      });
      Toast.show({
        icon: r.success ? 'success' : 'fail',
        content: r.message,
      });
    } catch (e) {
      Toast.show({
        icon: 'fail',
        content: e instanceof Error ? e.message : '测连失败',
      });
    } finally {
      setTesting(false);
    }
  };

  const onFetchModels = async () => {
    if (!form.apiKey.trim() && !editing) {
      Toast.show({ content: '请先填写 API Key' });
      return;
    }
    setFetchingModels(true);
    try {
      const r = await LingxiApiConfig.fetchModels({
        provider: form.provider,
        apiKey: form.apiKey,
        baseUrl: form.baseUrl,
        configId: editing?.id,
      });
      setModels(r.models ?? []);
      if (r.models?.length) {
        Toast.show({ content: `获取到 ${r.models.length} 个模型` });
      }
    } catch (e) {
      Toast.show({
        icon: 'fail',
        content: e instanceof Error ? e.message : '获取模型失败',
      });
    } finally {
      setFetchingModels(false);
    }
  };

  return (
    <div className="app-shell" style={{ height: '100%' }}>
      <NavBar
        onBack={() => navigate(-1)}
        right={
          <AddOutline
            fontSize={22}
            onClick={openCreate}
            data-testid="api-config-add"
          />
        }
      >
        API 配置
      </NavBar>
      <div className="page" style={{ overflow: 'auto' }}>
        <div className="page__card" style={{ marginBottom: 12 }}>
          <p className="page__meta">
            密钥仅在保存时传给原生，列表仅显示脱敏字段。切换活跃配置会重建
            Agent。
          </p>
        </div>

        {configs.length === 0 ? (
          <div className="page__card">
            <p className="page__meta">暂无配置，点击右上角添加。</p>
            <Button block color="primary" onClick={openCreate}>
              新建配置
            </Button>
          </div>
        ) : (
          <List mode="card">
            {configs.map((c) => (
              <List.Item
                key={c.id}
                data-testid={`api-config-${c.id}`}
                description={
                  <div>
                    <div>
                      {c.provider} · {c.modelId || '—'}
                    </div>
                    <div style={{ fontSize: 12, opacity: 0.7 }}>
                      {c.apiKeyMasked || '无密钥'} · {c.baseUrl || '默认 URL'}
                    </div>
                  </div>
                }
                extra={
                  c.isActive ? (
                    <Tag color="primary">活跃</Tag>
                  ) : (
                    <Button size="mini" onClick={() => void onSetActive(c)}>
                      启用
                    </Button>
                  )
                }
                onClick={() => openEdit(c)}
                arrow
              >
                {c.name}
              </List.Item>
            ))}
          </List>
        )}

        {configs.length > 0 && (
          <List header="操作" mode="card" style={{ marginTop: 12 }}>
            {configs.map((c) => (
              <List.Item
                key={`del-${c.id}`}
                onClick={() => void onDelete(c)}
              >
                <span style={{ color: 'var(--adm-color-danger)' }}>
                  删除「{c.name}」
                </span>
              </List.Item>
            ))}
          </List>
        )}
      </div>

      <Popup
        visible={popupOpen}
        onMaskClick={() => setPopupOpen(false)}
        bodyStyle={{
          borderTopLeftRadius: 12,
          borderTopRightRadius: 12,
          maxHeight: '90vh',
          overflow: 'auto',
        }}
      >
        <div style={{ padding: 16 }}>
          <h3 style={{ marginTop: 0 }}>
            {editing ? '编辑配置' : '新建配置'}
          </h3>
          <Form layout="vertical">
            <Form.Item label="名称">
              <Input
                placeholder="显示名称"
                value={form.name}
                onChange={(v) => setForm((f) => ({ ...f, name: v }))}
              />
            </Form.Item>
            <Form.Item label="Provider">
              <Selector
                options={providerOptions}
                value={[form.provider]}
                onChange={onProviderChange}
              />
            </Form.Item>
            <Form.Item
              label={
                editing ? 'API Key（留空则保留原密钥）' : 'API Key'
              }
            >
              <Input
                type="password"
                placeholder={editing ? '••••••••' : 'sk-…'}
                value={form.apiKey}
                onChange={(v) => setForm((f) => ({ ...f, apiKey: v }))}
                data-testid="api-key-input"
              />
            </Form.Item>
            <Form.Item label="Base URL">
              <TextArea
                rows={2}
                value={form.baseUrl}
                onChange={(v) => setForm((f) => ({ ...f, baseUrl: v }))}
              />
            </Form.Item>
            <Form.Item label="Model ID">
              <Input
                value={form.modelId}
                onChange={(v) => setForm((f) => ({ ...f, modelId: v }))}
              />
            </Form.Item>
            {models.length > 0 && (
              <Form.Item label="可用模型">
                <Selector
                  options={models.map((m) => ({ label: m, value: m }))}
                  value={form.modelId ? [form.modelId] : []}
                  onChange={(vals) => {
                    if (vals[0]) {
                      setForm((f) => ({ ...f, modelId: vals[0] }));
                    }
                  }}
                />
              </Form.Item>
            )}
          </Form>
          <Space direction="vertical" block style={{ marginTop: 8 }}>
            <Button
              block
              loading={fetchingModels}
              onClick={() => void onFetchModels()}
            >
              拉取模型列表
            </Button>
            <Button
              block
              loading={testing}
              onClick={() => void onTest()}
            >
              测试连接
            </Button>
            <Button
              block
              color="primary"
              loading={saving}
              data-testid="api-config-save"
              onClick={() => void onSave()}
            >
              保存
            </Button>
            <Button block fill="outline" onClick={() => setPopupOpen(false)}>
              取消
            </Button>
          </Space>
        </div>
      </Popup>
    </div>
  );
}
