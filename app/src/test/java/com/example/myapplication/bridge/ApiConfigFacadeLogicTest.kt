package com.example.myapplication.bridge

import com.example.myapplication.agent.LangChainAgentEngine
import com.example.myapplication.api.ModelFetchResult
import com.example.myapplication.api.ModelFetcher
import com.example.myapplication.api.ModelInfo
import com.example.myapplication.config.ModelProvider
import com.example.myapplication.data.local.entities.ApiConfigEntity
import com.example.myapplication.data.repository.ApiConfigRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ApiConfigFacadeLogicTest {

    private lateinit var repository: ApiConfigRepository
    private lateinit var modelFetcher: ModelFetcher
    private lateinit var agent: LangChainAgentEngine

    @Before
    fun setUp() {
        repository = mockk(relaxed = true)
        modelFetcher = mockk(relaxed = true)
        agent = mockk(relaxed = true)
        every { agent.reconfigure() } returns Result.success(Unit)
        every { repository.allConfigs } returns flowOf(emptyList())
    }

    private fun facade() = ApiConfigFacade(repository, modelFetcher, agent)

    private fun entity(
        id: String = "c1",
        isActive: Boolean = true,
        apiKey: String = "sk-real-key",
    ) = ApiConfigEntity(
        id = id,
        name = "Default",
        providerId = "openai",
        apiKey = apiKey,
        baseUrl = "https://api.openai.com/v1",
        modelId = "gpt-4o",
        isActive = isActive,
        createdAt = 1L,
        updatedAt = 2L,
    )

    @Test
    fun create_active_reconfiguresAgent() = runTest {
        val created = entity(isActive = true)
        coEvery {
            repository.createConfig(any(), any(), any(), any(), any())
        } returns Result.success(created)

        val (dto, reconfigured) = facade().create("Default", "OPENAI", "sk-real-key", "", "gpt-4o")
        assertThat(dto.provider).isEqualTo("OPENAI")
        assertThat(dto.apiKeyMasked).doesNotContain("real")
        assertThat(reconfigured).isTrue()
        verify { agent.reconfigure() }
    }

    @Test
    fun create_inactive_skipsReconfigure() = runTest {
        val created = entity(isActive = false)
        coEvery {
            repository.createConfig(any(), any(), any(), any(), any())
        } returns Result.success(created)

        val (_, reconfigured) = facade().create("Other", "OPENAI", "sk-x", "", "gpt-4o")
        assertThat(reconfigured).isNull()
        verify(exactly = 0) { agent.reconfigure() }
    }

    @Test
    fun setActive_reconfigures() = runTest {
        coEvery { repository.setActiveConfig("c1") } returns Result.success(Unit)
        val ok = facade().setActive("c1")
        assertThat(ok).isTrue()
        verify { agent.reconfigure() }
    }

    @Test
    fun update_blankApiKey_keepsExisting() = runTest {
        val existing = entity(apiKey = "sk-keep-me")
        coEvery { repository.getConfigById("c1") } returns existing andThen existing.copy(
            name = "Renamed",
            apiKey = "sk-keep-me",
        )
        coEvery {
            repository.updateConfig(
                configId = "c1",
                name = "Renamed",
                provider = ModelProvider.OPENAI,
                apiKey = "sk-keep-me",
                baseUrl = existing.baseUrl,
                modelId = existing.modelId,
            )
        } returns Result.success(Unit)

        val (dto, reconfigured) = facade().update(
            id = "c1",
            name = "Renamed",
            provider = "OPENAI",
            apiKey = "",
            baseUrl = existing.baseUrl,
            modelId = existing.modelId,
        )
        assertThat(dto.name).isEqualTo("Renamed")
        assertThat(reconfigured).isTrue()
        verify { agent.reconfigure() }
    }

    @Test
    fun testConnection_success_shape() = runTest {
        coEvery {
            modelFetcher.fetchModels(ModelProvider.OPENAI, "k", "u")
        } returns ModelFetchResult(
            isSuccess = true,
            models = listOf(ModelInfo("gpt-4o", "gpt-4o")),
        )
        val r = facade().testConnection("OPENAI", "k", "u", "gpt-4o")
        assertThat(r.success).isTrue()
        assertThat(r.message).contains("1")
    }

    @Test
    fun fetchModels_returnsIds() = runTest {
        coEvery {
            modelFetcher.fetchModels(any(), any(), any())
        } returns ModelFetchResult(
            isSuccess = true,
            models = listOf(
                ModelInfo("a", "A"),
                ModelInfo("b", "B"),
            ),
        )
        val models = facade().fetchModels("ZHIPU", "k", "")
        assertThat(models).containsExactly("a", "b").inOrder()
    }

    @Test
    fun testConnection_blankKey_usesStoredFromConfigId() = runTest {
        coEvery { repository.getConfigById("c1") } returns entity(apiKey = "sk-stored")
        coEvery {
            modelFetcher.fetchModels(ModelProvider.OPENAI, "sk-stored", "https://api.openai.com/v1")
        } returns ModelFetchResult(
            isSuccess = true,
            models = listOf(ModelInfo("gpt-4o", "gpt-4o")),
        )
        val r = facade().testConnection(
            provider = "OPENAI",
            apiKey = "",
            baseUrl = "https://api.openai.com/v1",
            modelId = "gpt-4o",
            configId = "c1",
        )
        assertThat(r.success).isTrue()
        coVerify {
            modelFetcher.fetchModels(ModelProvider.OPENAI, "sk-stored", "https://api.openai.com/v1")
        }
    }

    @Test
    fun fetchModels_blankKey_usesStoredFromConfigId() = runTest {
        coEvery { repository.getConfigById("c1") } returns entity(apiKey = "sk-stored")
        coEvery {
            modelFetcher.fetchModels(ModelProvider.OPENAI, "sk-stored", any())
        } returns ModelFetchResult(
            isSuccess = true,
            models = listOf(ModelInfo("gpt-4o", "gpt-4o")),
        )
        val models = facade().fetchModels(
            provider = "OPENAI",
            apiKey = "",
            baseUrl = "https://api.openai.com/v1",
            configId = "c1",
        )
        assertThat(models).containsExactly("gpt-4o")
    }

    @Test
    fun fetchModels_blankKey_withoutConfigId_fails() = runTest {
        try {
            facade().fetchModels("OPENAI", "", "https://x", configId = null)
            throw AssertionError("expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertThat(e.message).contains("apiKey is required")
        }
        coVerify(exactly = 0) { modelFetcher.fetchModels(any(), any(), any()) }
    }

    @Test
    fun testConnection_blankKey_withoutConfigId_returnsFailure() = runTest {
        val r = facade().testConnection("OPENAI", "", "https://x", "m", configId = null)
        assertThat(r.success).isFalse()
        assertThat(r.message).contains("apiKey")
        coVerify(exactly = 0) { modelFetcher.fetchModels(any(), any(), any()) }
    }

    @Test
    fun listProviders_includesOpenAi() {
        val providers = facade().listProviders()
        assertThat(providers.any { it["id"] == "OPENAI" }).isTrue()
        assertThat(providers.any { it["id"] == "ZHIPU" }).isTrue()
    }
}
