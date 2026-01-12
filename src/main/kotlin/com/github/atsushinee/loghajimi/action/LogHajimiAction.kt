package com.github.atsushinee.loghajimi.action

import com.github.atsushinee.loghajimi.ui.LogHajimiView
import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys // 关键修正 1: 导入正确的 DataKey
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.util.Key
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.content.ContentManagerEvent
import com.intellij.ui.content.ContentManagerListener

class LogHajimiAction : AnAction() {

    companion object {
        private val LOG = Logger.getInstance(LogHajimiAction::class.java)
        private const val LOG_HAJIMI_CONTENT_NAME = "LogHajimi"
        // 创建一个唯一的 Key，用于在原始 Content 上查找我们关联的 LogHajimi Content
        private val LOG_HAJIMI_CONTENT_KEY = Key.create<Content>("LogHajimi.AssociatedContent")
    }

    init {
        // 设置动作的图标
        templatePresentation.icon = IconLoader.getIcon("/icons/hajimi.svg", javaClass)
    }

    // 指定 update 方法在后台线程执行，避免阻塞 UI
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val consoleView = e.getData(LangDataKeys.CONSOLE_VIEW) as? ConsoleViewImpl ?: return

        // 关键修正 2: 直接从事件上下文中获取当前活动的 ToolWindow。
        // 这可以确保无论用户点击的是 "Run" 还是 "Debug" 工具栏，我们都能在正确的窗口中操作。
        val toolWindow = e.getData(PlatformDataKeys.TOOL_WINDOW) ?: run {
            LOG.error("无法从 AnActionEvent 上下文中获取 ToolWindow。")
            return
        }

        val contentManager = toolWindow.contentManager

        // 直接使用当前选中的标签页作为原始标签页
        val originalContent = contentManager.selectedContent ?: return

        // 1. 检查此原始 Content 是否已经关联了一个 LogHajimi 标签页
        val existingHajimiContent = originalContent.getUserData(LOG_HAJIMI_CONTENT_KEY)
        if (existingHajimiContent != null && contentManager.getIndexOfContent(existingHajimiContent) != -1) {
            // 如果已存在且仍在管理器中，则直接选中它
            contentManager.setSelectedContent(existingHajimiContent, true)
            toolWindow.activate(null)
            LOG.warn("找到已存在的 LogHajimi 标签页，并切换到该页。")
            return
        }

        // 2. 如果不存在，则创建一个全新的视图和标签页
        val editor = consoleView.editor ?: run {
            LOG.error("无法获取 ConsoleView 的 Editor 实例。")
            return
        }
        val currentText = editor.document.text
        val logHajimiView = LogHajimiView(project, currentText)
        val newHajimiContent = ContentFactory.getInstance().createContent(logHajimiView, LOG_HAJIMI_CONTENT_NAME, true)

        // 3. 将新创建的 LogHajimi 标签页与原始标签页关联起来
        originalContent.putUserData(LOG_HAJIMI_CONTENT_KEY, newHajimiContent)

        // 4. 绑定生命周期：当 LogHajimi 标签页关闭时，自动销毁其内部视图
        Disposer.register(newHajimiContent, logHajimiView)

        // 5. 创建监听器，当原始标签页关闭时，自动关闭我们的 LogHajimi 标签页
        val lifecycleListener = object : ContentManagerListener {
            override fun contentRemoved(event: ContentManagerEvent) {
                // 检查被移除的是否是我们正在跟踪的原始标签页
                if (event.content == originalContent) {
                    ApplicationManager.getApplication().invokeLater {
                        if (!project.isDisposed && !Disposer.isDisposed(newHajimiContent)) {
                            contentManager.removeContent(newHajimiContent, true)
                            LOG.warn("原始控制台已关闭，自动关闭 LogHajimi 标签页。")
                        }
                    }
                } else if (event.content == newHajimiContent) {
                    // 如果我们的标签页被关闭，则移除此监听器以防止内存泄漏
                    contentManager.removeContentManagerListener(this)
                    LOG.warn("LogHajimi 标签页已关闭，移除生命周期监听器。")
                }
            }
        }
        // 将这个生命周期监听器添加到 ContentManager
        contentManager.addContentManagerListener(lifecycleListener)

        // 6. 创建文档监听器，以同步后续的日志
        val documentListener = object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                if (logHajimiView.isShowing) {
                    logHajimiView.appendText(event.newFragment.toString())
                }
            }
        }
        // 将文档监听器的生命周期与视图绑定
        editor.document.addDocumentListener(documentListener, logHajimiView)

        // 7. 将新创建的 Content 添加到管理器并选中它
        contentManager.addContent(newHajimiContent)
        contentManager.setSelectedContent(newHajimiContent, true)
        toolWindow.activate(null)
        LOG.warn("为当前会话创建并切换到新的 LogHajimi 标签页。")
    }

    /**
     * `update` 方法决定 Action 是否可用和可见。
     */
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null && e.getData(LangDataKeys.CONSOLE_VIEW) != null
    }
}