package com.github.atsushinee.loghajimi.action

import com.intellij.execution.actions.ConsoleActionsPostProcessor
import com.intellij.execution.ui.ConsoleView
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.diagnostic.Logger

/**
 * 这是一个控制台动作的后处理器。
 * 它会在每次有 ConsoleView（例如 Run 或 Debug 控制台）被创建时调用。
 */
class LogHajimiConsoleActionsPostProcessor : ConsoleActionsPostProcessor() {

    companion object {
        private val LOG = Logger.getInstance(LogHajimiConsoleActionsPostProcessor::class.java)
        private const val ACTION_ID = "com.github.atsushinee.loghajimi.action.LogHajimiAction"
    }

    /**
     * 在控制台的默认动作列表创建后，此方法被调用。
     * @param console 当前被创建的 ConsoleView 实例。
     * @param actions 一个包含将要显示在工具栏上的所有默认动作的数组。
     * @return 修改后的动作数组。
     */
    override fun postProcess(console: ConsoleView, actions: Array<AnAction>): Array<AnAction> {
        // 关键修正：永远不要在扩展点中直接 new 一个 Action 实例。
        // 必须通过 ActionManager 按 ID 获取，以确保在插件热重载时不会发生类加载器泄漏。
        val actionManager = ActionManager.getInstance()
        val logHajimiAction = actionManager.getAction(ACTION_ID)

        if (logHajimiAction == null) {
            // 如果由于某种原因找不到 Action，记录一个错误，并返回原始的动作列表，避免崩溃。
            LOG.error("无法通过 ID '$ACTION_ID' 找到 LogHajimiAction。请检查 plugin.xml。")
            return actions
        }

        // 使用展开运算符(*)将 actions 数组的元素添加到新数组中。
        return arrayOf(logHajimiAction, *actions)
    }
}