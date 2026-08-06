package com.example.myapplication.data.mapper

import com.example.myapplication.config.ModelProvider
import com.example.myapplication.data.local.entities.ApiConfigEntity
import com.example.myapplication.data.mapper.BridgeDtoMapper.maskApiKey
import com.example.myapplication.data.mapper.BridgeDtoMapper.permissionStatus
import com.example.myapplication.data.mapper.BridgeDtoMapper.providerFromBridge
import com.example.myapplication.data.mapper.BridgeDtoMapper.providerIdToBridge
import com.example.myapplication.data.mapper.BridgeDtoMapper.toDto
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class ApiConfigBridgeMapperTest {

    @Test
    fun maskApiKey_empty_and_short() {
        assertThat(maskApiKey("")).isEqualTo("")
        assertThat(maskApiKey("ab")).isEqualTo("***")
        assertThat(maskApiKey("abcd")).isEqualTo("***")
    }

    @Test
    fun maskApiKey_long_keepsPrefix() {
        assertThat(maskApiKey("sk-abcdefgh")).isEqualTo("sk-***")
        assertThat(maskApiKey("12345")).isEqualTo("12***")
    }

    @Test
    fun providerIdToBridge_mapsKnownIds() {
        assertThat(providerIdToBridge("openai")).isEqualTo("OPENAI")
        assertThat(providerIdToBridge("zhipu")).isEqualTo("ZHIPU")
        assertThat(providerIdToBridge("azure-openai")).isEqualTo("AZURE_OPENAI")
        assertThat(providerIdToBridge("huggingface")).isEqualTo("HUGGING_FACE")
    }

    @Test
    fun providerFromBridge_acceptsEnumNameAndId() {
        assertThat(providerFromBridge("OPENAI")).isEqualTo(ModelProvider.OPENAI)
        assertThat(providerFromBridge("openai")).isEqualTo(ModelProvider.OPENAI)
        assertThat(providerFromBridge("azure-openai")).isEqualTo(ModelProvider.AZURE_OPENAI)
        assertThat(providerFromBridge("AZURE_OPENAI")).isEqualTo(ModelProvider.AZURE_OPENAI)
        assertThat(providerFromBridge("ZHIPU")).isEqualTo(ModelProvider.ZHIPU)
    }

    @Test
    fun providerFromBridge_unknown_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            providerFromBridge("not-a-real-provider")
        }
    }

    @Test
    fun entity_toDto_masksKey_andUppercasesProvider() {
        val entity = ApiConfigEntity(
            id = "c1",
            name = "Mine",
            providerId = "openai",
            apiKey = "sk-secret-value",
            baseUrl = "https://api.openai.com/v1",
            modelId = "gpt-4o",
            isActive = true,
            createdAt = 1L,
            updatedAt = 2L,
        )
        val dto = entity.toDto()
        assertThat(dto.id).isEqualTo("c1")
        assertThat(dto.provider).isEqualTo("OPENAI")
        assertThat(dto.apiKeyMasked).isEqualTo("sk-***")
        assertThat(dto.apiKeyMasked).doesNotContain("secret")
        assertThat(dto.isActive).isTrue()
        assertThat(dto.modelId).isEqualTo("gpt-4o")
    }

    @Test
    fun permissionStatus_allReady_requiresCorePlusApi() {
        val notReady = permissionStatus(
            accessibility = true,
            overlay = true,
            screenCapture = true,
            appList = true,
            notification = true,
            apiConfigured = false,
            shizuku = false,
        )
        assertThat(notReady.allReady).isFalse()

        val ready = permissionStatus(
            accessibility = true,
            overlay = true,
            screenCapture = true,
            appList = true,
            notification = false,
            apiConfigured = true,
            shizuku = false,
        )
        assertThat(ready.allReady).isTrue()
        assertThat(ready.notification).isFalse()
        assertThat(ready.shizuku).isFalse()
    }
}
