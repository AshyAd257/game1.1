package com.Hecate.puppet.editor.command;

import java.util.Stack;

/**
 * 命令管理器
 * 管理命令历史，提供撤销和重做功能
 */
public class CommandManager {

    private final Stack<Command> undoStack;
    private final Stack<Command> redoStack;
    private final int maxHistorySize;

    public CommandManager() {
        this(100); // 默认最多保存100条历史
    }

    public CommandManager(int maxHistorySize) {
        this.undoStack = new Stack<>();
        this.redoStack = new Stack<>();
        this.maxHistorySize = maxHistorySize;
    }

    /**
     * 执行命令并添加到历史中
     */
    public void executeCommand(Command command) {
        command.execute();
        undoStack.push(command);

        // 清空重做栈（执行新命令后，之前的重做历史失效）
        redoStack.clear();

        // 限制历史大小
        if (undoStack.size() > maxHistorySize) {
            undoStack.remove(0);
        }
    }

    /**
     * 撤销上一个命令
     */
    public void undo() {
        if (canUndo()) {
            Command command = undoStack.pop();
            command.undo();
            redoStack.push(command);
        }
    }

    /**
     * 重做上一个撤销的命令
     */
    public void redo() {
        if (canRedo()) {
            Command command = redoStack.pop();
            command.execute();
            undoStack.push(command);
        }
    }

    /**
     * 是否可以撤销
     */
    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    /**
     * 是否可以重做
     */
    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    /**
     * 清空所有历史
     */
    public void clear() {
        undoStack.clear();
        redoStack.clear();
    }

    /**
     * 获取撤销栈的大小
     */
    public int getUndoStackSize() {
        return undoStack.size();
    }

    /**
     * 获取重做栈的大小
     */
    public int getRedoStackSize() {
        return redoStack.size();
    }

    /**
     * 获取下一个可撤销命令的描述
     */
    public String getNextUndoDescription() {
        if (canUndo()) {
            return undoStack.peek().getDescription();
        }
        return null;
    }

    /**
     * 获取下一个可重做命令的描述
     */
    public String getNextRedoDescription() {
        if (canRedo()) {
            return redoStack.peek().getDescription();
        }
        return null;
    }
}
