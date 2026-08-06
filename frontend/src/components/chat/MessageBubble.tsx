import type { ChatMessageDto } from '../../types/bridge';

export function MessageBubble({ message }: { message: ChatMessageDto }) {
  const type = message.type;

  if (type === 'user') {
    return (
      <div className="msg msg--user" data-testid="msg-user">
        <div className="msg__bubble msg__bubble--user">{message.content}</div>
      </div>
    );
  }

  if (type === 'ai') {
    const failed = message.isSuccess === false;
    return (
      <div className="msg msg--ai" data-testid="msg-ai">
        <div
          className={
            failed
              ? 'msg__bubble msg__bubble--ai msg__bubble--err'
              : 'msg__bubble msg__bubble--ai'
          }
        >
          {message.content}
          {message.errorMessage && failed ? (
            <div className="msg__error">{message.errorMessage}</div>
          ) : null}
        </div>
      </div>
    );
  }

  if (type === 'toolCall') {
    return (
      <div className="msg msg--tool" data-testid="msg-tool">
        <div className="msg__bubble msg__bubble--tool">
          <strong>🔧 {message.toolName ?? message.content}</strong>
          {message.result ? (
            <div className="msg__meta">{message.result}</div>
          ) : null}
        </div>
      </div>
    );
  }

  if (type === 'screenshot') {
    return (
      <div className="msg msg--shot" data-testid="msg-screenshot">
        <div className="msg__bubble msg__bubble--shot">
          {message.content || '截图'}
          {message.imageBase64 ? (
            <img
              className="msg__img"
              src={
                message.imageBase64.startsWith('data:')
                  ? message.imageBase64
                  : `data:image/jpeg;base64,${message.imageBase64}`
              }
              alt={message.content || 'screenshot'}
            />
          ) : null}
        </div>
      </div>
    );
  }

  // status
  return (
    <div className="msg msg--status" data-testid="msg-status">
      <div className="msg__status">
        {message.isRunning ? '⏳ ' : ''}
        {message.content}
      </div>
    </div>
  );
}
