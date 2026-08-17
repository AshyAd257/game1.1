package com.Hecate.puppet.editor.command;

/**
 * 命令接口
 * 所有可撤销的操作都需要实现此接口
 */
public interface Command {

    /**
     * 执行命令
     */
    void execute();

    /**
     * 撤销命令
     */
    void undo();

    /**
     * 获取命令描述（用于调试和显示）
     */
    String getDescription();
}
