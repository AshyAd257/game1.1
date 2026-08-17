package com.Hecate.puppet.editor.command;

import com.Hecate.puppet.editor.core.EditorBone;
import com.jme3.math.Vector3f;
import com.jme3.math.Quaternion;

/**
 * 清除父骨骼的命令
 */
public class ClearParentCommand implements Command {

    private final EditorBone bone;
    private final EditorBone oldParent;
    private final Vector3f oldPosition;
    private final Quaternion oldRotation;
    private final Vector3f oldScale;
    private final Vector3f newPosition;
    private final Quaternion newRotation;
    private final Vector3f newScale;
    private final String description;

    public ClearParentCommand(EditorBone bone,
                             Vector3f oldPos, Quaternion oldRot, Vector3f oldScale,
                             Vector3f newPos, Quaternion newRot, Vector3f newScale) {
        this.bone = bone;
        this.oldParent = bone.getParent();
        this.oldPosition = oldPos.clone();
        this.oldRotation = oldRot.clone();
        this.oldScale = oldScale.clone();
        this.newPosition = newPos.clone();
        this.newRotation = newRot.clone();
        this.newScale = newScale.clone();
        this.description = "Clear Parent: " + bone.getName();
    }

    @Override
    public void execute() {
        // 从父骨骼移除
        if (oldParent != null) {
            oldParent.removeChild(bone);
        }

        // 设置为世界坐标（独立骨骼）
        bone.setLocalPosition(newPosition.clone());
        bone.setLocalRotation(newRotation.clone());
        bone.setLocalScale(newScale.clone());
    }

    @Override
    public void undo() {
        // 恢复父子关系
        if (oldParent != null) {
            oldParent.addChild(bone);
        }

        // 恢复旧的局部变换
        bone.setLocalPosition(oldPosition.clone());
        bone.setLocalRotation(oldRotation.clone());
        bone.setLocalScale(oldScale.clone());
    }

    @Override
    public String getDescription() {
        return description;
    }
}
