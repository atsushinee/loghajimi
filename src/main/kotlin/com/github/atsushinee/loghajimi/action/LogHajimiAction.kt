package com.github.atsushinee.loghajimi.action

import com.github.atsushinee.loghajimi.ui.LogHajimiView
import com.intellij.execution.impl.ConsoleViewImpl
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
import com.intellij.ui.content.ContentFactory

class LogHajimiAction : AnAction() {

    companion object {
        private val LOG = Logger.getInstance(LogHajimiAction::class.java)
        // 为我们的自定义标签页定义一个唯一的名称
        private const val LOG_HAJIMI_CONTENT_NAME = "LogHajimi"
    }

    init {
        // 设置动作的图标
        templatePresentation.icon = IconLoader.getIcon("/icons/hajimi.svg", javaClass)
    }

    // 指定 update 方法在后台线程执行，避免阻塞 UI
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        // 使用 LangDataKeys.CONSOLE_VIEW 获取 ConsoleView
        // 并将其安全地转换为 ConsoleViewImpl，以访问 getEditor() 等具体实现方法
        val consoleView = e.getData(LangDataKeys.CONSOLE_VIEW) as? ConsoleViewImpl ?: return

        // 通过 ToolWindowManager 获取当前活动的 Run/Debug 窗口和内容管理器
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.RUN)
            ?: ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.DEBUG)
            ?: return
        val contentManager = toolWindow.contentManager

        // --- 核心逻辑：查找或创建新的 "LogHajimi" 标签页 ---

        // 1. 查找是否已存在 LogHajimi 标签页
        val existingContent = contentManager.findContent(LOG_HAJIMI_CONTENT_NAME)

        if (existingContent != null) {
            // 2. 如果已存在，直接选中它并激活工具窗口，不执行任何其他操作
            contentManager.setSelectedContent(existingContent)
            toolWindow.activate(null)
            LOG.warn("找到已存在的 LogHajimi 标签页，并切换到该页。")
        } else {
            // 3. 如果不存在，则创建一个全新的视图和标签页
            val editor = consoleView.editor ?: run {
                LOG.error("无法获取 ConsoleView 的 Editor 实例。")
                return
            }
            val currentText = editor.document.text
            val logHajimiView = LogHajimiView(project, currentText)

            // 4. 使用 ContentFactory 创建一个新的、可关闭的 Content
            val newContent = ContentFactory.getInstance().createContent(logHajimiView, LOG_HAJIMI_CONTENT_NAME, true)

            // 5. 关键：将 LogHajimiView 的生命周期与新创建的 Content 绑定。
            // 当用户点击 "x" 关闭这个标签页时，Disposer 会自动调用 logHajimiView.dispose()，从而防止内存泄漏。
            Disposer.register(newContent, logHajimiView)

            // 6. 创建 DocumentListener 以便在原始控制台有新内容时，同步到我们的视图
            val documentListener = object : DocumentListener {
                override fun documentChanged(event: DocumentEvent) {
                    // 只有当我们的视图可见时才追加文本，以优化性能
                    if (logHajimiView.isShowing) {
                        logHajimiView.appendText(event.newFragment.toString())
                    }
                }
            }
            // 将监听器的生命周期与 logHajimiView（也就是 Content）绑定
            // 当 logHajimiView 被 dispose 时，这个 listener 会被自动移除
            editor.document.addDocumentListener(documentListener, logHajimiView)

            // 7. 将新创建的 Content 添加到管理器并选中它
            contentManager.addContent(newContent)
            contentManager.setSelectedContent(newContent)
            toolWindow.activate(null)
            LOG.warn("创建并切换到新的 LogHajimi 标签页。")
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