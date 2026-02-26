package com.example.myapplication.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.myapplication.data.local.entities.ApiConfigEntity
import com.example.myapplication.ui.screens.*
import com.example.myapplication.utils.ApiProvider
import com.example.myapplication.utils.ApiProviders
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Compose UI tests for ApiConfigScreen
 *
 * Tests API configuration management UI including adding, editing, and deleting configs.
 */
class ApiConfigScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var fakeViewModel: FakeApiConfigViewModel

    @Before
    fun setup() {
        fakeViewModel = FakeApiConfigViewModel()
    }

    // ========== Empty State Tests ==========

    @Test
    fun `apiConfigScreen shows empty state when no configs`() {
        composeTestRule.setContent {
            ApiConfigScreen(
                onNavigateBack = {},
                viewModel = fakeViewModel
            )
        }

        composeTestRule.onNodeWithText("暂无 API 配置").assertExists()
        composeTestRule.onNodeWithText("点击下方按钮添加您的第一个 API 配置").assertExists()
    }

    @Test
    fun `apiConfigScreen shows add button in empty state`() {
        composeTestRule.setContent {
            ApiConfigScreen(
                onNavigateBack = {},
                viewModel = fakeViewModel
            )
        }

        composeTestRule.onNodeWithText("添加配置").assertExists()
    }

    // ========== Config Display Tests ==========

    @Test
    fun `apiConfigScreen displays configs when available`() {
        fakeViewModel.setConfigs(listOf(
            ApiConfigEntity(
                id = "1",
                name = "Test Config",
                providerId = "zhipu",
                apiKey = "test-api-key-12345",
                baseUrl = "",
                modelId = "glm-4v",
                isActive = true,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        ))

        composeTestRule.setContent {
            ApiConfigScreen(
                onNavigateBack = {},
                viewModel = fakeViewModel
            )
        }

        composeTestRule.onNodeWithText("Test Config").assertExists()
        composeTestRule.onNodeWithText("glm-4v").assertExists()
    }

    @Test
    fun `apiConfigScreen displays active indicator for active config`() {
        fakeViewModel.setConfigs(listOf(
            ApiConfigEntity(
                id = "1",
                name = "Active Config",
                providerId = "zhipu",
                apiKey = "test-key",
                baseUrl = "",
                modelId = "glm-4v",
                isActive = true,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        ))

        composeTestRule.setContent {
            ApiConfigScreen(
                onNavigateBack = {},
                viewModel = fakeViewModel
            )
        }

        composeTestRule.onNodeWithText("当前使用").assertExists()
    }

    @Test
    fun `apiConfigScreen displays multiple configs`() {
        fakeViewModel.setConfigs(listOf(
            ApiConfigEntity(
                id = "1",
                name = "Config One",
                providerId = "zhipu",
                apiKey = "key1",
                baseUrl = "",
                modelId = "model1",
                isActive = true,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            ),
            ApiConfigEntity(
                id = "2",
                name = "Config Two",
                providerId = "openai",
                apiKey = "key2",
                baseUrl = "",
                modelId = "model2",
                isActive = false,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        ))

        composeTestRule.setContent {
            ApiConfigScreen(
                onNavigateBack = {},
                viewModel = fakeViewModel
            )
        }

        composeTestRule.onNodeWithText("Config One").assertExists()
        composeTestRule.onNodeWithText("Config Two").assertExists()
    }

    // ========== Add Config Tests ==========

    @Test
    fun `addConfigButton opens dialog`() {
        composeTestRule.setContent {
            ApiConfigScreen(
                onNavigateBack = {},
                viewModel = fakeViewModel
            )
        }

        // Click the add button in top bar
        composeTestRule.onNodeWithContentDescription("添加配置")
            .performClick()

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("添加配置").assertExists()
    }

    @Test
    fun `addConfig dialog shows provider selection`() {
        composeTestRule.setContent {
            ApiConfigScreen(
                onNavigateBack = {},
                viewModel = fakeViewModel
            )
        }

        composeTestRule.onNodeWithContentDescription("添加配置")
            .performClick()

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("选择提供商").assertExists()
    }

    @Test
    fun `addConfig dialog shows API key input`() {
        composeTestRule.setContent {
            ApiConfigScreen(
                onNavigateBack = {},
                viewModel = fakeViewModel
            )
        }

        composeTestRule.onNodeWithContentDescription("添加配置")
            .performClick()

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("API Key *").assertExists()
    }

    // ========== Config Card Tests ==========

    @Test
    fun `configCard shows edit and delete buttons`() {
        fakeViewModel.setConfigs(listOf(
            ApiConfigEntity(
                id = "1",
                name = "Test Config",
                providerId = "zhipu",
                apiKey = "test-key",
                baseUrl = "",
                modelId = "glm-4v",
                isActive = true,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        ))

        composeTestRule.setContent {
            ApiConfigScreen(
                onNavigateBack = {},
                viewModel = fakeViewModel
            )
        }

        composeTestRule.onNodeWithContentDescription("编辑").assertExists()
        composeTestRule.onNodeWithContentDescription("删除").assertExists()
    }

    @Test
    fun `configCard click delete shows confirmation dialog`() {
        fakeViewModel.setConfigs(listOf(
            ApiConfigEntity(
                id = "1",
                name = "To Delete",
                providerId = "zhipu",
                apiKey = "key",
                baseUrl = "",
                modelId = "model",
                isActive = true,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        ))

        composeTestRule.setContent {
            ApiConfigScreen(
                onNavigateBack = {},
                viewModel = fakeViewModel
            )
        }

        composeTestRule.onNodeWithContentDescription("删除")
            .performClick()

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("确认删除").assertExists()
    }

    @Test
    fun `delete confirmation dialog shows config name`() {
        fakeViewModel.setConfigs(listOf(
            ApiConfigEntity(
                id = "1",
                name = "My API Config",
                providerId = "zhipu",
                apiKey = "key",
                baseUrl = "",
                modelId = "model",
                isActive = true,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        ))

        composeTestRule.setContent {
            ApiConfigScreen(
                onNavigateBack = {},
                viewModel = fakeViewModel
            )
        }

        composeTestRule.onNodeWithContentDescription("删除")
            .performClick()

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("确定要删除配置 \"My API Config\" 吗？").assertExists()
    }

    // ========== Edit Config Tests ==========

    @Test
    fun `clicking edit opens dialog with existing values`() {
        fakeViewModel.setConfigs(listOf(
            ApiConfigEntity(
                id = "1",
                name = "Existing Config",
                providerId = "zhipu",
                apiKey = "existing-key",
                baseUrl = "",
                modelId = "glm-4v",
                isActive = true,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        ))

        composeTestRule.setContent {
            ApiConfigScreen(
                onNavigateBack = {},
                viewModel = fakeViewModel
            )
        }

        composeTestRule.onNodeWithContentDescription("编辑")
            .performClick()

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("编辑配置").assertExists()
    }

    // ========== Activation Tests ==========

    @Test
    fun `clicking inactive config activates it`() {
        var activatedConfigId: String? = null
        fakeViewModel.onSetActiveConfig = { id -> activatedConfigId = id }

        fakeViewModel.setConfigs(listOf(
            ApiConfigEntity(
                id = "1",
                name = "Config 1",
                providerId = "zhipu",
                apiKey = "key1",
                baseUrl = "",
                modelId = "model1",
                isActive = true,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            ),
            ApiConfigEntity(
                id = "2",
                name = "Config 2",
                providerId = "openai",
                apiKey = "key2",
                baseUrl = "",
                modelId = "model2",
                isActive = false,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        ))

        composeTestRule.setContent {
            ApiConfigScreen(
                onNavigateBack = {},
                viewModel = fakeViewModel
            )
        }

        // Click on inactive config to activate
        composeTestRule.onAllNodesWithContentDescription("未选中")[0]
            .performClick()

        assert(activatedConfigId == "2")
    }

    // ========== Info Card Tests ==========

    @Test
    fun `infoCard displays helpful message`() {
        composeTestRule.setContent {
            ApiConfigScreen(
                onNavigateBack = {},
                viewModel = fakeViewModel
            )
        }

        composeTestRule.onNodeWithText("管理您的 API 配置，可以添加多个配置并在它们之间切换")
            .assertExists()
    }

    // ========== Navigation Tests ==========

    @Test
    fun `back button exists in top bar`() {
        composeTestRule.setContent {
            ApiConfigScreen(
                onNavigateBack = {},
                viewModel = fakeViewModel
            )
        }

        composeTestRule.onNodeWithContentDescription("返回").assertExists()
    }

    @Test
    fun `back button triggers navigation`() {
        var navigatedBack = false
        composeTestRule.setContent {
            ApiConfigScreen(
                onNavigateBack = { navigatedBack = true },
                viewModel = fakeViewModel
            )
        }

        composeTestRule.onNodeWithContentDescription("返回")
            .performClick()

        assert(navigatedBack)
    }

    // ========== API Key Display Tests ==========

    @Test
    fun `configCard masks API key`() {
        fakeViewModel.setConfigs(listOf(
            ApiConfigEntity(
                id = "1",
                name = "Test",
                providerId = "zhipu",
                apiKey = "sk-1234567890abcdefghijklmnopqrstuvwxyz",
                baseUrl = "",
                modelId = "model",
                isActive = true,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        ))

        composeTestRule.setContent {
            ApiConfigScreen(
                onNavigateBack = {},
                viewModel = fakeViewModel
            )
        }

        // Should show truncated key with ellipsis
        composeTestRule.onNodeWithText("sk-12345...", substring = true).assertExists()
    }

    @Test
    fun `configCard shows warning when no API key`() {
        fakeViewModel.setConfigs(listOf(
            ApiConfigEntity(
                id = "1",
                name = "No Key Config",
                providerId = "zhipu",
                apiKey = "",
                baseUrl = "",
                modelId = "model",
                isActive = false,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        ))

        composeTestRule.setContent {
            ApiConfigScreen(
                onNavigateBack = {},
                viewModel = fakeViewModel
            )
        }

        composeTestRule.onNodeWithText("未设置").assertExists()
    }
}

/**
 * Fake implementation of ApiConfigViewModel for testing
 */
class FakeApiConfigViewModel : ApiConfigViewModel(android.app.Application()) {
    private val _configs = MutableStateFlow<List<ApiConfigEntity>>(emptyList())
    override val configs: StateFlow<List<ApiConfigEntity>> = _configs

    private val _activeConfig = MutableStateFlow<ApiConfigEntity?>(null)
    override val activeConfig: StateFlow<ApiConfigEntity?> = _activeConfig

    private val _uiState = MutableStateFlow(ApiConfigUiState())
    override val uiState: StateFlow<ApiConfigUiState> = _uiState

    private val _availableModels = MutableStateFlow<List<com.example.myapplication.api.ModelInfo>>(emptyList())
    override val availableModels: StateFlow<List<com.example.myapplication.api.ModelInfo>> = _availableModels

    private val _testResult = MutableStateFlow<TestConnectionResult?>(null)
    override val testResult: StateFlow<TestConnectionResult?> = _testResult

    var onSetActiveConfig: ((String) -> Unit)? = null

    fun setConfigs(configs: List<ApiConfigEntity>) {
        _configs.value = configs
        _activeConfig.value = configs.find { it.isActive }
    }

    override fun setActiveConfig(configId: String) {
        onSetActiveConfig?.invoke(configId)
        val configs = _configs.value.map {
            it.copy(isActive = it.id == configId)
        }
        _configs.value = configs
        _activeConfig.value = configs.find { it.isActive }
    }

    override fun showEditDialog(config: ApiConfigEntity?) {
        _uiState.value = _uiState.value.copy(
            showEditDialog = true,
            editingConfig = config
        )
    }

    override fun hideEditDialog() {
        _uiState.value = _uiState.value.copy(
            showEditDialog = false,
            editingConfig = null
        )
    }

    override fun showDeleteConfirm(config: ApiConfigEntity) {
        _uiState.value = _uiState.value.copy(
            showDeleteConfirm = true,
            configToDelete = config
        )
    }

    override fun hideDeleteConfirm() {
        _uiState.value = _uiState.value.copy(
            showDeleteConfirm = false,
            configToDelete = null
        )
    }

    override fun createConfig(name: String, provider: ApiProvider, apiKey: String, baseUrl: String, modelId: String) {
        val newConfig = ApiConfigEntity(
            id = System.currentTimeMillis().toString(),
            name = name,
            providerId = provider.id,
            apiKey = apiKey,
            baseUrl = baseUrl,
            modelId = modelId,
            isActive = _configs.value.isEmpty(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        _configs.value = _configs.value + newConfig
    }

    override fun deleteConfig(configId: String) {
        _configs.value = _configs.value.filter { it.id != configId }
        hideDeleteConfirm()
    }
}
