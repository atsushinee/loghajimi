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

    // 存储完整的、未经过滤的日志文本。必须是 var 才能追加新内容。
    private var originalText: String = initialText
    private val filterTextField = JBTextField()
    // Editor 组件用于显示日志，提供比 JTextArea 更好的性能和功能
    private val editor: Editor = createLogEditor(project, this.originalText)

    init {
        // 使用 panel DSL 构建顶部面板
        val topPanel = panel {
            row {
                label("Filter:")
                cell(filterTextField)
                    .resizableColumn()
                    .align(AlignX.FILL)
            }
        }
        // 关键修正：为顶部面板添加一个左右各 8 像素的空边框，以增加水平边距
        topPanel.border = BorderFactory.createEmptyBorder(0, 8, 0, 8)

        // 将顶部面板添加到主面板的北部（上方）
        add(topPanel, BorderLayout.NORTH)
        // 将编辑器组件添加到主面板的中心，它将占据剩余的所有空间
        add(editor.component, BorderLayout.CENTER)

        // 为过滤文本框添加文档监听器，实现“输入即搜索”的实时过滤功能
        filterTextField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) = filterLogs()
            override fun removeUpdate(e: DocumentEvent?) = filterLogs()
            override fun changedUpdate(e: DocumentEvent?) = filterLogs()
        })
    }

    /**
     * 公开方法，用于从外部（如 Action）向视图追加新的日志文本
     * @param newText 要追加的新日志
     */
    fun appendText(newText: String) {
        // 同步锁确保线程安全
        synchronized(this) {
            originalText += newText
        }
        filterLogs()
    }

    /**
     * 根据过滤框中的文本过滤日志并更新编辑器显示
     */
    private fun filterLogs() {
        // 在 UI 线程上执行
        ApplicationManager.getApplication().invokeLater {
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

            // 在写入操作中更新编辑器内容
            WriteCommandAction.runWriteCommandAction(project) {
                if (editor.isDisposed) return@runWriteCommandAction
                editor.document.setText(filteredText)
                editor.caretModel.moveToOffset(editor.document.textLength)
                editor.scrollingModel.scrollToCaret(ScrollType.MAKE_VISIBLE)
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
            isUseSoftWraps = false
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
