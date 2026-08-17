package com.Hecate.puppet.editor;

/**
 * 剪贴板管理器
 * 管理骨骼的复制和粘贴
 */
public class ClipboardManager {

    private static BoneClipboardData clipboard = null;

    /**
     * 复制骨骼数据到剪贴板
     */
    public static void copy(BoneClipboardData data) {
        clipboard = data;

    }

    /**
     * 从剪贴板粘贴
     */
    public static BoneClipboardData paste() {
        if (clipboard != null) {

            return new BoneClipboardData(clipboard.getRootBoneData().clone());
        }
        return null;
    }

    /**
     * 从剪贴板粘贴（镜像）
     */
    public static BoneClipboardData pasteMirrored() {
        if (clipboard != null) {
            return clipboard.getMirroredData();
        }
        return null;
    }

    /**
     * 检查剪贴板是否有数据
     */
    public static boolean hasData() {
        return clipboard != null;
    }

    /**
     * 清空剪贴板
     */
    public static void clear() {
        clipboard = null;

    }
}
