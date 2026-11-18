package com.Hecate.puppet.editor.command;

import com.Hecate.puppet.editor.core.EditorBone;
import com.Hecate.puppet.volume.VolumeDefinition;

/** Copy-isolated entity edit, optionally including integer strip framing in the new mode. */
public final class SetVolumeDefinitionCommand implements Command {
    private final EditorBone bone;
    private final VolumeDefinition before, after;
    private final Runnable refresh;
    private final boolean resizeStrip;
    private final int oldFrameWidth, oldFrameHeight, newFrameWidth, newFrameHeight;
    private final float oldWidth, oldHeight;

    public SetVolumeDefinitionCommand(EditorBone bone, VolumeDefinition value, Runnable refresh) {
        this(bone, value, refresh, false);
    }
    public SetVolumeDefinitionCommand(EditorBone bone, VolumeDefinition value, Runnable refresh, boolean linkSize) {
        this.bone = bone;
        before = bone.getVolumeDefinition() == null ? null : bone.getVolumeDefinition().copy();
        after = value == null ? null : value.copy();
        this.refresh = refresh;
        oldFrameWidth = bone.getStripFrameWidthPx(); oldFrameHeight = bone.getStripFrameHeightPx();
        oldWidth = bone.getStripWidth(); oldHeight = bone.getStripHeight();
        resizeStrip = linkSize && bone.isRotationStripEnabled() && before != null && after != null
            && (before.getWidthPx() != after.getWidthPx() || before.getHeightPx() != after.getHeightPx()
                || before.getPixelsPerUnit() != after.getPixelsPerUnit());
        newFrameWidth = resizeStrip && before.getWidthPx() != after.getWidthPx() ? after.getWidthPx() : oldFrameWidth;
        newFrameHeight = resizeStrip && before.getHeightPx() != after.getHeightPx() ? after.getHeightPx() : oldFrameHeight;
    }
    public void execute() {
        bone.setVolumeDefinition(after);
        if (resizeStrip) { bone.setStripFrameWidthPx(newFrameWidth); bone.setStripFrameHeightPx(newFrameHeight); }
        refresh.run();
    }
    public void undo() {
        bone.setVolumeDefinition(before);
        if (resizeStrip) {
            bone.setStripFrameWidthPx(oldFrameWidth); bone.setStripFrameHeightPx(oldFrameHeight);
            bone.setStripWidth(oldWidth); bone.setStripHeight(oldHeight);
        }
        refresh.run();
    }
    public String getDescription() { return "修改部件实体与法线"; }
}
