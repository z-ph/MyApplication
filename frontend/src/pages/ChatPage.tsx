import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Dialog,
  NavBar,
  NoticeBar,
  SpinLoading,
  Tag,
  Toast,
} from 'antd-mobile';
import { UnorderedListOutline, SetOutline } from 'antd-mobile-icons';
import { MessageBubble } from '../components/chat/MessageBubble';
import { ChatInputBar } from '../components/chat/ChatInputBar';
import { SessionDrawer } from '../components/chat/SessionDrawer';
import { useChatStore } from '../stores/useChatStore';

/**
 * Chat main path: sessions / messages / send / cancel via LingxiChat + LingxiAgent.
 */
export function ChatPage() {
  const navigate = useNavigate();
  const listRef = useRef<HTMLDivElement>(null);
  const [drawerOpen, setDrawerOpen] = useState(false);

  const {
    sessions,
    currentSessionId,
    messages,
    agentState,
    sending,
    ready,
    error,
    init,
    createSession,
    selectSession,
    deleteSession,
    sendMessage,
    cancelTask,
  } = useChatStore();

  useEffect(() => {
    void init();
  }, [init]);

  useEffect(() => {
    const el = listRef.current;
    if (el) {
      el.scrollTop = el.scrollHeight;
    }
  }, [messages]);

  const agentTag = agentState?.state ?? '—';
  const isRunning = agentState?.state === 'RUNNING' || sending;

  const onSend = async (content: string) => {
    try {
      await sendMessage(content);
    } catch (e) {
      Toast.show({
        icon: 'fail',
        content: e instanceof Error ? e.message : '发送失败',
      });
    }
  };

  const onCancel = async () => {
    const ok = await Dialog.confirm({
      content: '确定取消当前任务？',
    });
    if (ok) {
      await cancelTask();
    }
  };

  const currentTitle =
    sessions.find((s) => s.id === currentSessionId)?.title ?? '灵犀';

  return (
    <div className="chat-page">
      <NavBar
        back={null}
        left={
          <UnorderedListOutline
            fontSize={22}
            onClick={() => setDrawerOpen(true)}
            data-testid="session-menu"
          />
        }
        right={
          <SetOutline
            fontSize={22}
            onClick={() => navigate('/tabs/profile')}
          />
        }
      >
        <span className="chat-page__title">{currentTitle}</span>
      </NavBar>

      {isRunning ? (
        <NoticeBar
          content={`Agent 运行中… ${agentState?.step || agentState?.action || ''}`}
          color="info"
          closeable={false}
        />
      ) : null}

      {error ? (
        <NoticeBar content={error} color="alert" closeable />
      ) : null}

      <div className="chat-page__status">
        <Tag color={isRunning ? 'primary' : 'default'} fill="outline">
          {agentTag}
        </Tag>
      </div>

      <div className="chat-page__messages" ref={listRef} data-testid="msg-list">
        {!ready ? (
          <div className="chat-page__loading">
            <SpinLoading color="primary" />
          </div>
        ) : messages.length === 0 ? (
          <div className="chat-page__empty">
            发送一条指令开始自动化任务
          </div>
        ) : (
          messages.map((m) => <MessageBubble key={m.id} message={m} />)
        )}
      </div>

      <ChatInputBar
        sending={isRunning}
        onSend={onSend}
        onCancel={onCancel}
      />

      <SessionDrawer
        visible={drawerOpen}
        sessions={sessions}
        currentSessionId={currentSessionId}
        onClose={() => setDrawerOpen(false)}
        onSelect={(id) => void selectSession(id)}
        onDelete={(id) => void deleteSession(id)}
        onCreate={() => {
          void createSession();
          setDrawerOpen(false);
        }}
      />
    </div>
  );
}
