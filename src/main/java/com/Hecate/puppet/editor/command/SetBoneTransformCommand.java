package com.Hecate.puppet.editor.command;

import com.Hecate.puppet.editor.core.EditorBone;
import com.jme3.math.Vector3f;
import com.jme3.math.Quaternion;

/**
 * 设置骨骼变换的命令
 * 记录骨骼位置、旋转、缩放的变化
 */
public class SetBoneTransformCommand implements Command {

    private final EditorBone bone;
    private final Vector3f oldPosition;
    private final Quaternion oldRotation;
    private final Vector3f oldScale;
    private final Vector3f newPosition;
    private final Quaternion newRotation;
    private final Vector3f newScale;
    private final String description;

    public SetBoneTransformCommand(EditorBone bone,
                                   Vector3f oldPosition, Quaternion oldRotation, Vector3f oldScale,
                                   Vector3f newPosition, Quaternion newRotation, Vector3f newScale) {
        this.bone = bone;
        this.oldPosition = oldPosition.clone();
        this.oldRotation = oldRotation.clone();
        this.oldScale = oldScale.clone();
        this.newPosition = newPosition.clone();
        this.newRotation = newRotation.clone();
        this.newScale = newScale.clone();
        this.description = "Transform " + bone.getName();
    }

    @Override
    public void execute() {
        bone.setLocalPosition(newPosition.clone());
        bone.setLocalRotation(newRotation.clone());
        bone.setLocalScale(newScale.clone());
    }

    @Override
    public void undo() {
        bone.setLocalPosition(oldPosition.clone());
        bone.setLocalRotation(oldRotation.clone());
        bone.setLocalScale(oldScale.clone());
    }

    @Override
    public String getDescription() {
        return description;
    }
}
