package com.example.myapplication.agent

import android.content.Context
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations

/**
 * Unit tests for ToolRegistry
 *
 * Tests tool definitions, OpenAI format generation, and tool retrieval.
 */
class ToolRegistryTest {

    @Mock
    private lateinit var mockContext: Context

    private lateinit var toolRegistry: ToolRegistry

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        toolRegistry = ToolRegistry.getInstance(mockContext)
    }

    // ========== toOpenAIToolsFormat Tests ==========

    @Test
    fun `toOpenAIToolsFormat should return valid tool definitions`() {
        val tools = toolRegistry.toOpenAIToolsFormat()

        assertThat(tools).isNotEmpty()
        tools.forEach { tool ->
            assertThat(tool["type"]).isEqualTo("function")
            @Suppress("UNCHECKED_CAST")
            val function = tool["function"] as Map<String, Any>
            assertThat(function.containsKey("name")).isTrue()
            assertThat(function.containsKey("description")).isTrue()
            assertThat(function.containsKey("parameters")).isTrue()
        }
    }

    @Test
    fun `toOpenAIToolsFormat click tool should have correct structure`() {
        val tools = toolRegistry.toOpenAIToolsFormat()
        val clickTool = tools.find {
            @Suppress("UNCHECKED_CAST")
            (it["function"] as Map<String, Any>)["name"] == "click"
        }

        assertThat(clickTool).isNotNull()
        @Suppress("UNCHECKED_CAST")
        val function = clickTool!!["function"] as Map<String, Any>
        assertThat(function["description"]).isNotNull()

        @Suppress("UNCHECKED_CAST")
        val parameters = function["parameters"] as Map<String, Any>
        assertThat(parameters["type"]).isEqualTo("object")
        @Suppress("UNCHECKED_CAST")
        val properties = parameters["properties"] as Map<String, Any>
        assertThat(properties).containsKey("x")
        assertThat(properties).containsKey("y")
    }

    @Test
    fun `toOpenAIToolsFormat swipe tool should have enum for direction`() {
        val tools = toolRegistry.toOpenAIToolsFormat()
        val swipeTool = tools.find {
            @Suppress("UNCHECKED_CAST")
            (it["function"] as Map<String, Any>)["name"] == "swipe"
        }

        assertThat(swipeTool).isNotNull()
        @Suppress("UNCHECKED_CAST")
        val function = swipeTool!!["function"] as Map<String, Any>
        @Suppress("UNCHECKED_CAST")
        val parameters = function["parameters"] as Map<String, Any>
        @Suppress("UNCHECKED_CAST")
        val properties = parameters["properties"] as Map<String, Any>
        @Suppress("UNCHECKED_CAST")
        val directionProp = properties["direction"] as Map<String, Any>

        assertThat(directionProp.containsKey("enum")).isTrue()
        @Suppress("UNCHECKED_CAST")
        val enumValues = directionProp["enum"] as List<String>
        assertThat(enumValues).containsExactly("up", "down", "left", "right")
    }

    // ========== getTool Tests ==========

    @Test
    fun `getTool should return click tool`() {
        val tool = toolRegistry.getTool("click")

        assertThat(tool).isNotNull()
        assertThat(tool!!.name).isEqualTo("click")
        assertThat(tool.category).isEqualTo(ToolCategory.GESTURE)
    }

    @Test
    fun `getTool should return swipe tool`() {
        val tool = toolRegistry.getTool("swipe")

        assertThat(tool).isNotNull()
        assertThat(tool!!.name).isEqualTo("swipe")
    }

    @Test
    fun `getTool should return back tool`() {
        val tool = toolRegistry.getTool("back")

        assertThat(tool).isNotNull()
        assertThat(tool!!.name).isEqualTo("back")
        assertThat(tool.category).isEqualTo(ToolCategory.NAVIGATION)
    }

    @Test
    fun `getTool should return home tool`() {
        val tool = toolRegistry.getTool("home")

        assertThat(tool).isNotNull()
        assertThat(tool!!.name).isEqualTo("home")
    }

    @Test
    fun `getTool should return finish tool`() {
        val tool = toolRegistry.getTool("finish")

        assertThat(tool).isNotNull()
        assertThat(tool!!.name).isEqualTo("finish")
        assertThat(tool.category).isEqualTo(ToolCategory.CONTROL)
    }

    @Test
    fun `getTool should return reply tool`() {
        val tool = toolRegistry.getTool("reply")

        assertThat(tool).isNotNull()
        assertThat(tool!!.name).isEqualTo("reply")
    }

    @Test
    fun `getTool should return null for unknown tool`() {
        val tool = toolRegistry.getTool("unknown_tool_xyz")

        assertThat(tool).isNull()
    }

    // ========== getAllTools Tests ==========

    @Test
    fun `getAllTools should return non-empty list`() {
        val tools = toolRegistry.getAllTools()

        assertThat(tools).isNotEmpty()
    }

    @Test
    fun `getAllTools should contain gesture tools`() {
        val tools = toolRegistry.getAllTools()
        val toolNames = tools.map { it.name }

        assertThat(toolNames).containsAtLeast("click", "swipe", "type", "drag")
    }

    @Test
    fun `getAllTools should contain navigation tools`() {
        val tools = toolRegistry.getAllTools()
        val toolNames = tools.map { it.name }

        assertThat(toolNames).containsAtLeast("back", "home", "recents")
    }

    @Test
    fun `getAllTools should contain control tools`() {
        val tools = toolRegistry.getAllTools()
        val toolNames = tools.map { it.name }

        assertThat(toolNames).containsAtLeast("wait", "finish", "open_app")
    }

    @Test
    fun `getAllTools should contain observation tools`() {
        val tools = toolRegistry.getAllTools()
        val toolNames = tools.map { it.name }

        assertThat(toolNames).containsAtLeast("capture_screen", "list_apps")
    }

    // ========== getToolsByCategory Tests ==========

    @Test
    fun `getToolsByCategory GESTURE should return gesture tools`() {
        val tools = toolRegistry.getToolsByCategory(ToolCategory.GESTURE)

        assertThat(tools).isNotEmpty()
        tools.forEach {
            assertThat(it.category).isEqualTo(ToolCategory.GESTURE)
        }
    }

    @Test
    fun `getToolsByCategory NAVIGATION should return navigation tools`() {
        val tools = toolRegistry.getToolsByCategory(ToolCategory.NAVIGATION)

        assertThat(tools).isNotEmpty()
        tools.forEach {
            assertThat(it.category).isEqualTo(ToolCategory.NAVIGATION)
        }
    }

    @Test
    fun `getToolsByCategory CONTROL should return control tools`() {
        val tools = toolRegistry.getToolsByCategory(ToolCategory.CONTROL)

        assertThat(tools).isNotEmpty()
        tools.forEach {
            assertThat(it.category).isEqualTo(ToolCategory.CONTROL)
        }
    }

    @Test
    fun `getToolsByCategory OBSERVATION should return observation tools`() {
        val tools = toolRegistry.getToolsByCategory(ToolCategory.OBSERVATION)

        assertThat(tools).isNotEmpty()
        tools.forEach {
            assertThat(it.category).isEqualTo(ToolCategory.OBSERVATION)
        }
    }

    // ========== Tool Parameter Tests ==========

    @Test
    fun `click tool should have x and y parameters`() {
        val tool = toolRegistry.getTool("click")!!

        val paramNames = tool.parameters.map { it.name }
        assertThat(paramNames).containsExactly("x", "y")

        tool.parameters.forEach { param ->
            assertThat(param.required).isTrue()
            assertThat(param.type).isEqualTo("integer")
        }
    }

    @Test
    fun `type tool should have text parameter`() {
        val tool = toolRegistry.getTool("type")!!

        assertThat(tool.parameters).hasSize(1)
        assertThat(tool.parameters[0].name).isEqualTo("text")
        assertThat(tool.parameters[0].type).isEqualTo("string")
    }

    @Test
    fun `wait tool should have ms parameter`() {
        val tool = toolRegistry.getTool("wait")!!

        assertThat(tool.parameters).hasSize(1)
        assertThat(tool.parameters[0].name).isEqualTo("ms")
        assertThat(tool.parameters[0].type).isEqualTo("integer")
    }

    @Test
    fun `open_app tool should have package_name parameter`() {
        val tool = toolRegistry.getTool("open_app")!!

        assertThat(tool.parameters).hasSize(1)
        assertThat(tool.parameters[0].name).isEqualTo("package_name")
        assertThat(tool.parameters[0].type).isEqualTo("string")
    }

    @Test
    fun `finish tool should have summary parameter`() {
        val tool = toolRegistry.getTool("finish")!!

        assertThat(tool.parameters).hasSize(1)
        assertThat(tool.parameters[0].name).isEqualTo("summary")
        assertThat(tool.parameters[0].type).isEqualTo("string")
    }

    @Test
    fun `back tool should have no parameters`() {
        val tool = toolRegistry.getTool("back")!!

        assertThat(tool.parameters).isEmpty()
    }

    @Test
    fun `home tool should have no parameters`() {
        val tool = toolRegistry.getTool("home")!!

        assertThat(tool.parameters).isEmpty()
    }

    @Test
    fun `drag tool should have four parameters`() {
        val tool = toolRegistry.getTool("drag")!!

        assertThat(tool.parameters).hasSize(4)
        val paramNames = tool.parameters.map { it.name }
        assertThat(paramNames).containsExactly("start_x", "start_y", "end_x", "end_y")
    }

    @Test
    fun `long_click should have optional duration parameter`() {
        val tool = toolRegistry.getTool("long_click")!!

        val durationParam = tool.parameters.find { it.name == "duration" }
        assertThat(durationParam).isNotNull()
        assertThat(durationParam!!.required).isFalse()
    }
}
