package com.github.atsushinee.loghajimi.action

import com.github.atsushinee.loghajimi.ui.LogHajimiView
import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.execution.ui.ConsoleView
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.wm.ToolWindowId
import com.intellij.openapi.wm.ToolWindowManager
import javax.swing.JComponent
import java.util.WeakHashMap

class LogHajimiAction : AnAction() {

    companion object {
        private val LOG = Logger.getInstance(LogHajimiAction::class.java)
        private val originalComponents = WeakHashMap<LogHajimiView, JComponent>()
        private val documentListeners = WeakHashMap<LogHajimiView, DocumentListener>()
    }

    init {
        templatePresentation.icon = IconLoader.getIcon("/icons/hajimi.svg", javaClass)
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val consoleView = e.getData(LangDataKeys.CONSOLE_VIEW) as? ConsoleViewImpl ?: return

        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.RUN)
            ?: ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.DEBUG)
            ?: return
        val contentManager = toolWindow.contentManager
        val selectedContent = contentManager.selectedContent ?: return

        val existingLogHajimiView = selectedContent.component as? LogHajimiView

        if (existingLogHajimiView != null) {
            // --- 恢复原始控制台视图 ---
            originalComponents[existingLogHajimiView]?.let { originalComponent ->
                val originalConsole = (originalComponent as? ConsoleViewImpl)
                selectedContent.component = originalComponent

                documentListeners[existingLogHajimiView]?.let { listener ->
                    // 安全地移除监听器，即使 editor 为 null 也不会出错
                    originalConsole?.editor?.document?.removeDocumentListener(listener)
                    documentListeners.remove(existingLogHajimiView)
                }
                originalComponents.remove(existingLogHajimiView)
                LOG.warn("恢复原始控制台视图。")
            }
        } else {
            // --- 切换到 LogHajimi 自定义视图 ---
            // 关键修正：getEditor() 可能返回 null，必须进行检查。如果为 null，则无法继续。
            val editor = consoleView.editor ?: run {
                LOG.error("无法获取 ConsoleView 的 Editor 实例。")
                return
            }

            val currentText = editor.document.text
            val logHajimiView = LogHajimiView(project, currentText)

            // 将 LogHajimiView 的生命周期与 Content 绑定，以便自动销毁
            Disposer.register(selectedContent, logHajimiView)

            // 创建 DocumentListener 以监听新产生的日志
            val documentListener = object : DocumentListener {
                override fun documentChanged(event: DocumentEvent) {
                    logHajimiView.appendText(event.newFragment.toString())
                }
            }
            // 此处 editor 已被确认为非 null
            editor.document.addDocumentListener(documentListener)

            // 存储原始组件和监听器，以便之后恢复
            originalComponents[logHajimiView] = consoleView.component
            documentListeners[logHajimiView] = documentListener

            // 将 Content 的组件替换为我们的自定义视图
            selectedContent.component = logHajimiView
            LOG.warn("切换到 LogHajimi 视图。")
        }
    }

    /**
     * `update` 方法决定 Action 是否可用和可见。
     */
    override fun update(e: AnActionEvent) {
        // 只要能从上下文中获取到 ConsoleView，就启用按钮
        e.presentation.isEnabledAndVisible = e.project != null && e.getData(LangDataKeys.CONSOLE_VIEW) != null
    }
}