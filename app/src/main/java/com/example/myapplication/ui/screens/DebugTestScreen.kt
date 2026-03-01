package com.example.myapplication.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.shell.ShellExecutor
import com.example.myapplication.shell.ShizukuHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugTestScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val shellExecutor = remember { ShellExecutor(context) }

    var testResults by remember { mutableStateOf<List<ShellTestResult>>(emptyList()) }
    var isRunning by remember { mutableStateOf(false) }
    var shizukuStatus by remember { mutableStateOf("检查中...") }

    LaunchedEffect(Unit) {
        shizukuStatus = if (ShizukuHelper.isReady()) {
            "✓ 已就绪"
        } else if (ShizukuHelper.isAvailable()) {
            "⚠ 可用但未授权"
        } else {
            "✗ 未安装"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("调试测试") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Shizuku Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Shizuku 状态",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = shizukuStatus,
                            style = MaterialTheme.typography.bodyMedium,
                            color = when {
                                shizukuStatus.startsWith("✓") -> Color(0xFF4CAF50)
                                shizukuStatus.startsWith("⚠") -> Color(0xFFFF9800)
                                else -> Color(0xFFF44336)
                            }
                        )
                    }
                    Icon(
                        imageVector = if (shizukuStatus.startsWith("✓")) Icons.Default.CheckCircle
                        else if (shizukuStatus.startsWith("⚠")) Icons.Default.Warning
                        else Icons.Default.Cancel,
                        contentDescription = null,
                        tint = when {
                            shizukuStatus.startsWith("✓") -> Color(0xFF4CAF50)
                            shizukuStatus.startsWith("⚠") -> Color(0xFFFF9800)
                            else -> Color(0xFFF44336)
                        }
                    )
                }
            }

            // Run Tests Button
            Button(
                onClick = {
                    scope.launch {
                        isRunning = true
                        testResults = emptyList()

                        val results = mutableListOf<ShellTestResult>()

                        // Test 1: Common apps mapping
                        results.add(runTest("常用应用映射") {
                            val map = shellExecutor.getCommonAppsMapForTest()
                            val tests = mapOf(
                                "微信" to "com.tencent.mm",
                                "抖音" to "com.ss.android.ugc.aweme",
                                "设置" to "com.android.settings"
                            )
                            val failed = tests.filter { (k, v) -> map[k.lowercase()] != v }
                            if (failed.isEmpty()) "✓ 全部通过 (${tests.size}个)" else "✗ 失败: $failed"
                        })

                        // Test 2: Resolve package name
                        results.add(runTest("包名解析") {
                            val tests = mapOf(
                                "微信" to "com.tencent.mm",
                                "com.tencent.mm" to "com.tencent.mm"
                            )
                            val failed = tests.filter { (input, expected) ->
                                val actual = shellExecutor.resolvePackageNameForTest(input)
                                actual != expected
                            }
                            if (failed.isEmpty()) "✓ 全部通过" else "✗ 失败: $failed"
                        })

                        // Test 3: PackageManager list
                        results.add(runTest("PackageManager 列表") {
                            val apps = context.packageManager.getInstalledApplications(0)
                                .filter { context.packageManager.getLaunchIntentForPackage(it.packageName) != null }
                            if (apps.isNotEmpty()) "✓ 找到 ${apps.size} 个应用" else "✗ 未找到应用"
                        })

                        // Test 4: Launch intent for common apps
                        results.add(runTest("启动Intent检查") {
                            val packages = listOf(
                                "com.tencent.mm" to "微信",
                                "com.android.settings" to "设置"
                            )
                            val results = packages.map { (pkg, name) ->
                                val intent = context.packageManager.getLaunchIntentForPackage(pkg)
                                "${if (intent != null) "✓" else "✗"} $name"
                            }
                            results.joinToString("\n")
                        })

                        // Test 5: Find app by name
                        results.add(runTest("应用名查找") {
                            val result = shellExecutor.findAppByName("微信")
                            if (result == "com.tencent.mm") "✓ 微信 -> com.tencent.mm" else "✗ 结果: $result"
                        })

                        testResults = results
                        isRunning = false
                    }
                },
                enabled = !isRunning,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isRunning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("测试中...")
                } else {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("运行测试")
                }
            }

            // Test Results
            if (testResults.isNotEmpty()) {
                Text(
                    text = "测试结果",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                testResults.forEach { result ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (result.success)
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            else
                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = result.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "${result.duration}ms",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = result.message,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = if (result.success)
                                    MaterialTheme.colorScheme.onSurface
                                else
                                    MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            // Quick Actions
            Text(
                text = "快捷测试",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        scope.launch {
                            val result = shellExecutor.launchApp("微信")
                            testResults = testResults + ShellTestResult(
                                "打开微信",
                                result.isSuccess,
                                result.getOrNull() ?: result.exceptionOrNull()?.message ?: "未知错误",
                                0
                            )
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("打开微信", fontSize = 12.sp)
                }
                Button(
                    onClick = {
                        scope.launch {
                            val result = shellExecutor.launchApp("设置")
                            testResults = testResults + ShellTestResult(
                                "打开设置",
                                result.isSuccess,
                                result.getOrNull() ?: result.exceptionOrNull()?.message ?: "未知错误",
                                0
                            )
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("打开设置", fontSize = 12.sp)
                }
                Button(
                    onClick = {
                        scope.launch {
                            val result = shellExecutor.listAllApps()
                            testResults = testResults + ShellTestResult(
                                "列出应用",
                                result.isSuccess,
                                result.getOrNull()?.take(5)?.joinToString("\n") { "${it.label} = ${it.packageName}" }
                                    ?: result.exceptionOrNull()?.message ?: "未知错误",
                                0
                            )
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("列出应用", fontSize = 12.sp)
                }
            }
        }
    }
}

data class ShellTestResult(
    val name: String,
    val success: Boolean,
    val message: String,
    val duration: Long
)

private suspend fun runTest(name: String, block: suspend () -> String): ShellTestResult {
    val start = System.currentTimeMillis()
    return try {
        val message = block()
        ShellTestResult(name, message.startsWith("✓"), message, System.currentTimeMillis() - start)
    } catch (e: Exception) {
        ShellTestResult(name, false, "异常: ${e.message}", System.currentTimeMillis() - start)
    }
}
