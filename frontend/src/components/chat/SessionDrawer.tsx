import { Button, List, Popup, SwipeAction } from 'antd-mobile';
import type { ChatSessionDto } from '../../types/bridge';

interface Props {
  visible: boolean;
  sessions: ChatSessionDto[];
  currentSessionId: string | null;
  onClose: () => void;
  onSelect: (sessionId: string) => void;
  onDelete: (sessionId: string) => void;
  onCreate: () => void;
}

export function SessionDrawer({
  visible,
  sessions,
  currentSessionId,
  onClose,
  onSelect,
  onDelete,
  onCreate,
}: Props) {
  return (
    <Popup
      visible={visible}
      onMaskClick={onClose}
      position="left"
      bodyStyle={{ width: '80%', maxWidth: 320, height: '100%' }}
    >
      <div className="session-drawer">
        <div className="session-drawer__header">
          <span>会话</span>
          <Button size="mini" color="primary" onClick={onCreate}>
            新建
          </Button>
        </div>
        <List>
          {sessions.map((s) => (
            <SwipeAction
              key={s.id}
              rightActions={[
                {
                  key: 'delete',
                  text: '删除',
                  color: 'danger',
                  onClick: () => onDelete(s.id),
                },
              ]}
            >
              <List.Item
                onClick={() => {
                  onSelect(s.id);
                  onClose();
                }}
                description={new Date(s.updatedAt).toLocaleString()}
                arrow={false}
                className={
                  s.id === currentSessionId
                    ? 'session-drawer__item session-drawer__item--active'
                    : 'session-drawer__item'
                }
              >
                {s.title || '未命名'}
              </List.Item>
            </SwipeAction>
          ))}
          {sessions.length === 0 ? (
            <List.Item>暂无会话</List.Item>
          ) : null}
        </List>
      </div>
    </Popup>
  );
}
