import type {
  LingxiShellPlugin,
  PackagesEnvelope,
  ShellCommandResult,
  ShellTestResultsEnvelope,
  ShizukuStatusDto,
} from '../types/bridge';

/**
 * Browser mock for LingxiShell — no real shell / accessibility.
 */
export class LingxiShellWeb implements LingxiShellPlugin {
  async getShizukuStatus(): Promise<ShizukuStatusDto> {
    return { ready: false, available: false, status: 'unavailable' };
  }

  async runCommand(options: {
    command: string;
  }): Promise<ShellCommandResult> {
    return {
      success: true,
      output: `[mock] runCommand: ${options.command}`,
      error: null,
      exitCode: 0,
    };
  }

  async listPackages(): Promise<PackagesEnvelope> {
    return {
      packages: [
        {
          packageName: 'com.android.settings',
          label: '设置',
          isSystem: true,
          hasLaunchIntent: true,
        },
        {
          packageName: 'com.example.myapplication',
          label: '灵犀',
          isSystem: false,
          hasLaunchIntent: true,
        },
      ],
    };
  }

  async launchApp(options: {
    nameOrPackage?: string;
    name?: string;
  }): Promise<ShellCommandResult> {
    const target = options.nameOrPackage ?? options.name ?? '';
    return {
      success: true,
      output: `[mock] launchApp: ${target}`,
      error: null,
      exitCode: 0,
    };
  }

  async inputText(options: { text: string }): Promise<ShellCommandResult> {
    return {
      success: true,
      output: `[mock] inputText: '${options.text}'`,
      error: null,
      exitCode: 0,
    };
  }

  async runPackageTests(): Promise<ShellTestResultsEnvelope> {
    return {
      results: [
        {
          name: '常用应用映射',
          success: true,
          message: '✓ 全部通过 (mock)',
          durationMs: 1,
        },
        {
          name: '包名解析',
          success: true,
          message: '✓ 全部通过 (mock)',
          durationMs: 1,
        },
      ],
    };
  }
}
