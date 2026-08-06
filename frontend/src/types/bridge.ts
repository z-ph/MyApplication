/** DTO / plugin types aligned with docs/bridge-api.md (LingxiApp Phase 1). */

export interface EchoOptions {
  value: string;
}

export interface EchoResult {
  value: string;
}

export interface AppVersionInfo {
  appName: string;
  packageName: string;
  versionName: string;
  versionCode: number;
  /** true when served by browser mock, false on native */
  mock?: boolean;
}

export interface OpenUrlOptions {
  url: string;
}

export interface LingxiAppPlugin {
  echo(options: EchoOptions): Promise<EchoResult>;
  getVersion(): Promise<AppVersionInfo>;
  openUrl(options: OpenUrlOptions): Promise<void>;
}
