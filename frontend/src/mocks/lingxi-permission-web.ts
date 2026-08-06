import type {
  LingxiPermissionPlugin,
  PermissionStatusDto,
} from '../types/bridge';

type Listener = (data: PermissionStatusDto) => void;

/**
 * Browser mock for LingxiPermission — all granted so dev can enter chat.
 */
export class LingxiPermissionWeb implements LingxiPermissionPlugin {
  private status: PermissionStatusDto = {
    accessibility: true,
    overlay: true,
    screenCapture: true,
    appList: true,
    notification: true,
    apiConfigured: true,
    shizuku: false,
    allReady: true,
  };
  private listeners = new Set<Listener>();

  async getStatus(): Promise<PermissionStatusDto> {
    return { ...this.status };
  }

  async openAccessibilitySettings(): Promise<void> {
    this.status = { ...this.status, accessibility: true };
    this.recompute();
    this.emit();
  }

  async requestOverlay(): Promise<void> {
    this.status = { ...this.status, overlay: true };
    this.recompute();
    this.emit();
  }

  async requestScreenCapture(): Promise<PermissionStatusDto> {
    this.status = { ...this.status, screenCapture: true };
    this.recompute();
    this.emit();
    return { ...this.status };
  }

  async refresh(): Promise<PermissionStatusDto> {
    this.recompute();
    this.emit();
    return { ...this.status };
  }

  async addListener(
    eventName: 'statusChanged',
    listener: Listener,
  ): Promise<{ remove: () => Promise<void> }> {
    if (eventName !== 'statusChanged') {
      return { remove: async () => undefined };
    }
    this.listeners.add(listener);
    listener({ ...this.status });
    return {
      remove: async () => {
        this.listeners.delete(listener);
      },
    };
  }

  async removeAllListeners(): Promise<void> {
    this.listeners.clear();
  }

  /** Test helper */
  setMockStatus(partial: Partial<PermissionStatusDto>) {
    this.status = { ...this.status, ...partial };
    this.recompute();
    this.emit();
  }

  private recompute() {
    const s = this.status;
    this.status = {
      ...s,
      allReady:
        s.accessibility &&
        s.overlay &&
        s.screenCapture &&
        s.appList &&
        s.apiConfigured,
    };
  }

  private emit() {
    const snap = { ...this.status };
    this.listeners.forEach((fn) => fn(snap));
  }
}
