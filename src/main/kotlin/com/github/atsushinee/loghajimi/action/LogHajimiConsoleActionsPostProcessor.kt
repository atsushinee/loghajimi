package com.github.atsushinee.loghajimi.action

import com.intellij.execution.actions.ConsoleActionsPostProcessor
import com.intellij.execution.ui.ConsoleView
import com.intellij.openapi.actionSystem.AnAction

/**
 * 这是一个控制台动作的后处理器。
 * 它会在每次有 ConsoleView（例如 Run 或 Debug 控制台）被创建时调用。
 * 这是向动态创建的工具栏添加动作的最健壮、最可靠的方式。
 */
class LogHajimiConsoleActionsPostProcessor : ConsoleActionsPostProcessor() {
    /**
     * 在控制台的默认动作列表创建后，此方法被调用。
     * @param console 当前被创建的 ConsoleView 实例。
     * @param actions 一个包含将要显示在工具栏上的所有默认动作的数组。
     * @return 修改后的动作数组。
     */
    override fun postProcess(console: ConsoleView, actions: Array<AnAction>): Array<AnAction> {
        // 创建我们的 Action 实例
        val logHajimiAction = LogHajimiAction()
        // 关键修正：使用展开运算符(*)将 actions 数组的元素添加到新数组中。
        // 这是在 Kotlin 中将一个元素前置到一个现有数组的正确方法。
        return arrayOf(logHajimiAction, *actions)
    }
}
