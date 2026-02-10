package com.github.atsushinee.loghajimi.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
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
        // --- 创建清除按钮及其工具栏 ---
        val actionManager = ActionManager.getInstance()
        // 1. 创建一个动作组，用于容纳我们的清除按钮
        val actionGroup = DefaultActionGroup()
        // 2. 将我们自定义的 ClearLogAction 添加到组中
        actionGroup.add(ClearLogAction())
        // 3. 为这个动作组创建一个水平的工具栏
        val toolbar = actionManager.createActionToolbar("LogHajimiViewToolbar", actionGroup, true)
        // 4. 将工具栏的目标组件设置为当前视图，这对于正确的上下文处理很重要
        toolbar.targetComponent = this

        // --- 构建顶部面板 ---
        val topPanel = panel {
            row {
                label("Filter:")
                cell(filterTextField)
                    .resizableColumn()
                    .align(AlignX.FILL)
                // 5. 将工具栏组件添加到布局行中
                cell(toolbar.component)
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
     * 公开方法，用于清空视图中的所有日志
     */
    fun clear() {
        // 同步地将原始文本清空
        synchronized(this) {
            originalText = ""
        }
        // 重新应用过滤，此时因为原始文本为空，编辑器也会被清空
        filterLogs()
    }

    /**
     * 根据过滤框中的文本过滤日志并更新编辑器显示
     */
    private fun filterLogs() {
        ApplicationManager.getApplication().invokeLater {
            val scrollPane = (editor as? EditorEx)?.scrollPane
            val verticalScrollbar = scrollPane?.verticalScrollBar
            val isAtBottom = verticalScrollbar?.let {
                it.value + it.visibleAmount >= it.maximum - 10
            } ?: true

            val keywords = filterTextField.text.split(' ').filter { it.isNotBlank() }

            val currentOriginalText: String
            synchronized(this) {
                currentOriginalText = originalText
            }

            val filteredText = if (keywords.isEmpty()) {
                currentOriginalText
            } else {
                val result = currentOriginalText.lineSequence()
                    .filter { line ->
                        keywords.any { keyword -> line.contains(keyword, ignoreCase = true) }
                    }
                    .joinToString("\n")

                if (result.isNotEmpty()) {
                    result + "\n"
                } else {
                    result
                }
            }

            WriteCommandAction.runWriteCommandAction(project) {
                if (editor.isDisposed) return@runWriteCommandAction
                editor.document.setText(filteredText)

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

    /**
     * 定义一个私有内部类来处理清除动作。
     * 作为内部类，它可以方便地访问外部 LogHajimiView 的 `clear()` 方法。
     */
    private inner class ClearLogAction : AnAction("Clear All", "Clears the log content", AllIcons.Actions.GC) {
        override fun actionPerformed(e: AnActionEvent) {
            // 调用外部类的 clear 方法
            this@LogHajimiView.clear()
        }
    }
}
