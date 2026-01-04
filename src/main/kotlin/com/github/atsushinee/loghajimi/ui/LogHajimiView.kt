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
import com.intellij.ui.dsl.builder.panel
import java.awt.BorderLayout
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
        val topPanel = panel {
            row {
                label("过滤:")
                cell(filterTextField).resizableColumn()
            }
        }
        add(topPanel, BorderLayout.NORTH)
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
        // 同步锁确保线程安全，防止多个线程（如UI线程和后台日志线程）同时修改 originalText
        synchronized(this) {
            originalText += newText
        }
        // 追加文本后，立即重新应用过滤规则
        filterLogs()
    }

    /**
     * 根据过滤框中的文本过滤日志并更新编辑器显示
     */
    private fun filterLogs() {
        // 使用 invokeLater 确保 UI 更新在事件分发线程（EDT）上执行，这是 Swing 的基本规则
        ApplicationManager.getApplication().invokeLater {
            val filterText = filterTextField.text
            val currentOriginalText: String
            // 同步访问，安全地获取当前完整的日志文本
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

            // 使用 WriteCommandAction 来安全地修改编辑器文档，这是 IntelliJ Platform 的要求
            WriteCommandAction.runWriteCommandAction(project) {
                // 检查 editor 是否已被释放，防止在组件销毁后进行操作
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
        // 创建一个只读的编辑器实例
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
        // 关键修正：必须将 Editor 转换为 EditorEx 接口，然后调用 setColorsScheme 方法。
        // 不能直接对 editor.colorsScheme 属性赋值，因为它在 API 中是 val（只读）的。
        (editor as? EditorEx)?.setColorsScheme(EditorColorsManager.getInstance().globalScheme)
        return editor
    }

    /**
     * 实现 Disposable 接口的 dispose 方法，用于释放核心资源
     */
    override fun dispose() {
        // 释放 Editor 对象是必须的，否则会造成严重的内存泄漏
        if (!editor.isDisposed) {
            EditorFactory.getInstance().releaseEditor(editor)
        }
    }
}
