package com.example.myapplication.accessibility

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.example.myapplication.utils.Logger

/**
 * Node Parser
 * Parses and traverses accessibility node info tree to extract UI element information
 */
class NodeParser {

    companion object {
        private const val TAG = "NodeParser"

        // Max depth to prevent infinite loops
        private const val MAX_DEPTH = 50

        // Max nodes to parse (for performance)
        private const val MAX_NODES = 1000
    }

    private val logger = Logger(TAG)
    private var parsedNodeCount = 0

    /**
     * Parse the accessibility node tree and return structured information
     *
     * @param rootNode Root accessibility node info
     * @return ParsedNode representing the UI tree
     */
    fun parseTree(rootNode: AccessibilityNodeInfo?): ParsedNode? {
        if (rootNode == null) {
            logger.w("Cannot parse null root node")
            return null
        }

        parsedNodeCount = 0
        logger.d("Starting node tree parse")

        val parsed = parseNodeRecursive(rootNode, depth = 0)

        logger.d("Parsed $parsedNodeCount nodes")
        return parsed
    }

    /**
     * Get all clickable nodes
     *
     * @param rootNode Root accessibility node info
     * @return List of clickable nodes with their bounds
     */
    fun getClickableNodes(rootNode: AccessibilityNodeInfo?): List<ClickableNode> {
        val clickableNodes = mutableListOf<ClickableNode>()
        if (rootNode == null) {
            return clickableNodes
        }

        findClickableNodesRecursive(rootNode, clickableNodes)
        logger.d("Found ${clickableNodes.size} clickable nodes")

        return clickableNodes
    }

    /**
     * Find nodes by text content
     *
     * @param rootNode Root accessibility node info
     * @param searchText Text to search for (case-insensitive)
     * @return List of matching nodes
     */
    fun findNodesByText(rootNode: AccessibilityNodeInfo?, searchText: String): List<AccessibilityNodeInfo> {
        val matchingNodes = mutableListOf<AccessibilityNodeInfo>()
        if (rootNode == null || searchText.isBlank()) {
            return matchingNodes
        }

        findNodesByTextRecursive(rootNode, searchText.lowercase(), matchingNodes)
        logger.d("Found ${matchingNodes.size} nodes matching '$searchText'")

        return matchingNodes
    }

    /**
     * Find nodes by content description
     *
     * @param rootNode Root accessibility node info
     * @param searchDescription Content description to search for
     * @return List of matching nodes
     */
    fun findNodesByContentDescription(
        rootNode: AccessibilityNodeInfo?,
        searchDescription: String
    ): List<AccessibilityNodeInfo> {
        val matchingNodes = mutableListOf<AccessibilityNodeInfo>()
        if (rootNode == null || searchDescription.isBlank()) {
            return matchingNodes
        }

        findNodesByDescriptionRecursive(rootNode, searchDescription.lowercase(), matchingNodes)
        logger.d("Found ${matchingNodes.size} nodes with description matching '$searchDescription'")

        return matchingNodes
    }

    /**
     * Find editable text fields
     *
     * @param rootNode Root accessibility node info
     * @return List of editable nodes
     */
    fun getEditableNodes(rootNode: AccessibilityNodeInfo?): List<AccessibilityNodeInfo> {
        val editableNodes = mutableListOf<AccessibilityNodeInfo>()
        if (rootNode == null) {
            return editableNodes
        }

        findEditableNodesRecursive(rootNode, editableNodes)
        logger.d("Found ${editableNodes.size} editable nodes")

        return editableNodes
    }

    /**
     * Get a tree representation as a formatted string
     *
     * @param rootNode Root accessibility node info
     * @return Formatted string representation of the UI tree
     */
    fun getTreeString(rootNode: AccessibilityNodeInfo?): String {
        if (rootNode == null) {
            return "No root node available"
        }

        val builder = StringBuilder()
        buildTreeStringRecursive(rootNode, builder, depth = 0)
        return builder.toString()
    }

    // Private helper methods

    private fun parseNodeRecursive(node: AccessibilityNodeInfo, depth: Int): ParsedNode? {
        if (depth > MAX_DEPTH || parsedNodeCount >= MAX_NODES) {
            return null
        }

        parsedNodeCount++

        val bounds = Rect()
        node.getBoundsInScreen(bounds)

        val children = mutableListOf<ParsedNode>()
        val childNodes = mutableListOf<AccessibilityNodeInfo>()
        
        try {
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                childNodes.add(child)
                try {
                    val parsedChild = parseNodeRecursive(child, depth + 1)
                    parsedChild?.let { children.add(it) }
                } catch (e: Exception) {
                    logger.e("Error parsing child node at index $i", e)
                    // Continue to next child
                }
            }
        } finally {
            // Critical: recycle all child nodes to prevent memory leak
            childNodes.forEach { it.recycle() }
        }

        return ParsedNode(
            className = node.className?.toString() ?: "Unknown",
            text = node.text?.toString(),
            contentDescription = node.contentDescription?.toString(),
            viewId = node.viewIdResourceName,
            isClickable = node.isClickable,
            isEditable = node.isEditable,
            isEnabled = node.isEnabled,
            isFocusable = node.isFocusable,
            isFocused = node.isFocused,
            isScrollable = node.isScrollable,
            isChecked = node.isChecked,
            bounds = bounds,
            children = children,
            depth = depth
        )
    }

    private fun findClickableNodesRecursive(
        node: AccessibilityNodeInfo,
        clickableNodes: MutableList<ClickableNode>
    ) {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)

        if (node.isClickable && bounds.width() > 0 && bounds.height() > 0) {
            clickableNodes.add(
                ClickableNode(
                    node = node,
                    text = node.text?.toString() ?: node.contentDescription?.toString(),
                    bounds = bounds,
                    viewId = node.viewIdResourceName
                )
            )
        }

        val childNodes = mutableListOf<AccessibilityNodeInfo>()
        try {
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                childNodes.add(child)
                findClickableNodesRecursive(child, clickableNodes)
            }
        } finally {
            // Critical: recycle all child nodes to prevent memory leak
            childNodes.forEach { it.recycle() }
        }
    }

    private fun findNodesByTextRecursive(
        node: AccessibilityNodeInfo,
        searchText: String,
        matchingNodes: MutableList<AccessibilityNodeInfo>
    ) {
        val text = node.text?.toString()
        if (text != null && text.lowercase().contains(searchText)) {
            matchingNodes.add(node)
        }

        val childNodes = mutableListOf<AccessibilityNodeInfo>()
        try {
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                childNodes.add(child)
                findNodesByTextRecursive(child, searchText, matchingNodes)
            }
        } finally {
            // Critical: recycle all child nodes to prevent memory leak
            childNodes.forEach { it.recycle() }
        }
    }

    private fun findNodesByDescriptionRecursive(
        node: AccessibilityNodeInfo,
        searchDescription: String,
        matchingNodes: MutableList<AccessibilityNodeInfo>
    ) {
        val description = node.contentDescription?.toString()
        if (description != null && description.lowercase().contains(searchDescription)) {
            matchingNodes.add(node)
        }

        val childNodes = mutableListOf<AccessibilityNodeInfo>()
        try {
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                childNodes.add(child)
                findNodesByDescriptionRecursive(child, searchDescription, matchingNodes)
            }
        } finally {
            // Critical: recycle all child nodes to prevent memory leak
            childNodes.forEach { it.recycle() }
        }
    }

    private fun findEditableNodesRecursive(
        node: AccessibilityNodeInfo,
        editableNodes: MutableList<AccessibilityNodeInfo>
    ) {
        if (node.isEditable) {
            editableNodes.add(node)
        }

        val childNodes = mutableListOf<AccessibilityNodeInfo>()
        try {
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                childNodes.add(child)
                findEditableNodesRecursive(child, editableNodes)
            }
        } finally {
            // Critical: recycle all child nodes to prevent memory leak
            childNodes.forEach { it.recycle() }
        }
    }

    private fun buildTreeStringRecursive(node: AccessibilityNodeInfo, builder: StringBuilder, depth: Int) {
        if (depth > MAX_DEPTH) {
            return
        }

        val indent = "  ".repeat(depth)
        val bounds = Rect()
        node.getBoundsInScreen(bounds)

        builder.appendLine("${indent}[${node.className}]")
        if (node.text?.isNotEmpty() == true) {
            builder.appendLine("${indent}  Text: \"${node.text}\"")
        }
        if (node.contentDescription?.isNotEmpty() == true) {
            builder.appendLine("${indent}  Description: \"${node.contentDescription}\"")
        }
        if (node.viewIdResourceName != null) {
            builder.appendLine("${indent}  ID: ${node.viewIdResourceName}")
        }
        builder.appendLine("${indent}  Bounds: $bounds")
        builder.appendLine("${indent}  Clickable: ${node.isClickable}, Editable: ${node.isEditable}")

        val childNodes = mutableListOf<AccessibilityNodeInfo>()
        try {
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                childNodes.add(child)
                buildTreeStringRecursive(child, builder, depth + 1)
            }
        } finally {
            // Critical: recycle all child nodes to prevent memory leak
            childNodes.forEach { it.recycle() }
        }
    }
}

/**
 * Parsed node representation
 */
data class ParsedNode(
    val className: String,
    val text: String?,
    val contentDescription: String?,
    val viewId: String?,
    val isClickable: Boolean,
    val isEditable: Boolean,
    val isEnabled: Boolean,
    val isFocusable: Boolean,
    val isFocused: Boolean,
    val isScrollable: Boolean,
    val isChecked: Boolean,
    val bounds: Rect,
    val children: List<ParsedNode>,
    val depth: Int
)

/**
 * Clickable node information
 */
data class ClickableNode(
    val node: AccessibilityNodeInfo,
    val text: String?,
    val bounds: Rect,
    val viewId: String?
)
