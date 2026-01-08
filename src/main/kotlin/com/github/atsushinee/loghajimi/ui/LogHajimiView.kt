package com.github.atsushinee.loghajimi.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import java.awt.BorderLayout
import javax.swing.BorderFactory
import javax.swing.JPanel
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

// 实现 Disposable 接口，以便 IDE 能更好地管理此组件的生命周期和资源
class LogHajimiView(
    private val project: Project,
    initialText: String
) : JPanel(BorderLayout()), Disposable {

    private var originalText: String = initialText
    private val filterTextField = JBTextField()
    private val editor: Editor = createLogEditor(project, this.originalText)

    init {
        val topPanel = panel {
            row {
                label("Filter:")
                cell(filterTextField)
                    .resizableColumn()
                    .align(AlignX.FILL)
            }
        }
        topPanel.border = BorderFactory.createEmptyBorder(0, 8, 0, 8)
        add(topPanel, BorderLayout.NORTH)
        add(editor.component, BorderLayout.CENTER)

        filterTextField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) = filterLogs()
            override fun removeUpdate(e: DocumentEvent?) = filterLogs()
            override fun changedUpdate(e: DocumentEvent?) = filterLogs()
        })
    }

    /**
     * 公开方法，用于从外部（如 Action）向视图追加新的日志文本
     */
    fun appendText(newText: String) {
        synchronized(this) {
            originalText += newText
        }
        filterLogs()
    }

    /**
     * 根据过滤框中的文本过滤日志并更新编辑器显示
     */
    private fun filterLogs() {
        ApplicationManager.getApplication().invokeLater {
            // --- 智能滚动逻辑 ---
            // 1. 在更新文本前，检查滚动条是否在底部
            // 关键修正：必须通过 EditorEx 接口获取 JScrollPane，再从中获取 JScrollBar
            val scrollPane = (editor as? EditorEx)?.scrollPane
            val verticalScrollbar = scrollPane?.verticalScrollBar

            // 如果能获取到滚动条，则计算其是否在底部；如果获取不到，默认需要滚动，以保证新日志可见
            val isAtBottom = verticalScrollbar?.let {
                // 如果滚动条的当前位置 + 可见高度 约等于 滚动条的最大值，我们就认为它在底部
                it.value + it.visibleAmount >= it.maximum - 10 // 10像素的容差，以提高用户体验
            } ?: true

            val filterText = filterTextField.text
            val currentOriginalText: String
            synchronized(this) {
                currentOriginalText = originalText
            }

            val filteredText = if (filterText.isBlank()) {
                currentOriginalText
            } else {
                currentOriginalText.lineSequence()
                    .filter { it.contains(filterText, ignoreCase = true) }
                    .joinToString("\n")
            }

            WriteCommandAction.runWriteCommandAction(project) {
                if (editor.isDisposed) return@runWriteCommandAction
                editor.document.setText(filteredText)

                // 2. 只有当更新前滚动条就在底部时，才执行自动滚动
                if (isAtBottom) {
                    editor.caretModel.moveToOffset(editor.document.textLength)
                    editor.scrollingModel.scrollToCaret(ScrollType.MAKE_VISIBLE)
                }
            }
        }
    }

    /**
     * 创建并配置用于显示日志的 Editor
     */
    private fun createLogEditor(project: Project, text: String): Editor {
        val document = EditorFactory.getInstance().createDocument(text)
        val editor = EditorFactory.getInstance().createEditor(document, project)!!
        editor.settings.apply {
            isLineNumbersShown = true
            isWhitespacesShown = false
            isLineMarkerAreaShown = false
            isIndentGuidesShown = false
            isFoldingOutlineShown = false
            additionalColumnsCount = 0
            additionalLinesCount = 0
            isRightMarginShown = false
            // 关键修正：启用自动换行
            isUseSoftWraps = true
        }
        (editor as? EditorEx)?.setColorsScheme(EditorColorsManager.getInstance().globalScheme)
        return editor
    }

    /**
     * 实现 Disposable 接口的 dispose 方法，用于释放核心资源
     */
    override fun dispose() {
        if (!editor.isDisposed) {
            EditorFactory.getInstance().releaseEditor(editor)
        }
    }
}