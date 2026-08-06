import { useState } from 'react';
import { Button, DotLoading, TextArea } from 'antd-mobile';

interface Props {
  disabled?: boolean;
  sending?: boolean;
  onSend: (content: string) => void | Promise<void>;
  onCancel?: () => void | Promise<void>;
}

export function ChatInputBar({
  disabled,
  sending,
  onSend,
  onCancel,
}: Props) {
  const [text, setText] = useState('');

  const submit = async () => {
    const v = text.trim();
    if (!v || sending || disabled) return;
    setText('');
    await onSend(v);
  };

  return (
    <div className="chat-input">
      <TextArea
        data-testid="chat-input"
        placeholder="输入指令…"
        value={text}
        onChange={setText}
        rows={1}
        autoSize={{ minRows: 1, maxRows: 4 }}
        disabled={disabled || sending}
      />
      {sending ? (
        <Button
          data-testid="cancel-btn"
          color="warning"
          size="small"
          onClick={() => void onCancel?.()}
        >
          取消
        </Button>
      ) : (
        <Button
          data-testid="send-btn"
          color="primary"
          size="small"
          disabled={!text.trim() || disabled}
          onClick={() => void submit()}
        >
          发送
        </Button>
      )}
      {sending ? (
        <span className="chat-input__loading" aria-hidden>
          <DotLoading color="primary" />
        </span>
      ) : null}
    </div>
  );
}
