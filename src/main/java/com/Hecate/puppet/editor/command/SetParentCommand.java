package com.Hecate.puppet.editor.command;

import com.Hecate.puppet.editor.core.EditorBone;
import com.jme3.math.Vector3f;
import com.jme3.math.Quaternion;

/**
 * 设置父骨骼的命令
 */
public class SetParentCommand implements Command {

    private final EditorBone childBone;
    private final EditorBone oldParent;
    private final EditorBone newParent;
    private final Vector3f oldPosition;
    private final Quaternion oldRotation;
    private final Vector3f oldScale;
    private final Vector3f newPosition;
    private final Quaternion newRotation;
    private final Vector3f newScale;
    private final String description;

    public SetParentCommand(EditorBone childBone, EditorBone newParent,
                           Vector3f oldPos, Quaternion oldRot, Vector3f oldScale,
                           Vector3f newPos, Quaternion newRot, Vector3f newScale) {
        this.childBone = childBone;
        this.oldParent = childBone.getParent();
        this.newParent = newParent;
        this.oldPosition = oldPos.clone();
        this.oldRotation = oldRot.clone();
        this.oldScale = oldScale.clone();
        this.newPosition = newPos.clone();
        this.newRotation = newRot.clone();
        this.newScale = newScale.clone();
        this.description = "Set Parent: " + childBone.getName() + " -> " + newParent.getName();
    }

    @Override
    public void execute() {
        // 从旧父骨骼移除
        if (oldParent != null) {
            oldParent.removeChild(childBone);
        }

        // 添加到新父骨骼
        newParent.addChild(childBone);

        // 设置新的局部变换
        childBone.setLocalPosition(newPosition.clone());
        childBone.setLocalRotation(newRotation.clone());
        childBone.setLocalScale(newScale.clone());
    }

    @Override
    public void undo() {
        // 从新父骨骼移除
        newParent.removeChild(childBone);

        // 恢复到旧父骨骼（或独立）
        if (oldParent != null) {
            oldParent.addChild(childBone);
        }

        // 恢复旧的局部变换
        childBone.setLocalPosition(oldPosition.clone());
        childBone.setLocalRotation(oldRotation.clone());
        childBone.setLocalScale(oldScale.clone());
    }

    @Override
    public String getDescription() {
        return description;
    }
}
