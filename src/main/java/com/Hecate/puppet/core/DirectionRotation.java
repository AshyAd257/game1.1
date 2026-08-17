package com.Hecate.puppet.core;

import com.Hecate.puppet.editor.core.EditorBone;
import java.util.HashMap;
import java.util.Map;

/**
 * 方向旋转映射工具类
 *
 * 处理骨骼的方向重映射，实现逻辑旋转效果。
 * 当骨骼组旋转时，其各个方向的贴图和属性会重新映射到新的方向。
 *
 * 例如，向左转90°：
 * - front → left（正面贴图移到左侧）
 * - left → back（左侧贴图移到背面）
 * - back → right（背面贴图移到右侧）
 * - right → front（右侧贴图移到正面）
 */
public class DirectionRotation {

    /**
     * 向左旋转90度的方向映射
     * Y轴逆时针旋转（俯视图）
     */
    private static final Map<String, String> LEFT_90_MAP = new HashMap<>();
    static {
        LEFT_90_MAP.put("front", "left");
        LEFT_90_MAP.put("left", "back");
        LEFT_90_MAP.put("back", "right");
        LEFT_90_MAP.put("right", "front");
        LEFT_90_MAP.put("up", "up");      // 上下方向不变
        LEFT_90_MAP.put("down", "down");
    }

    /**
     * 向右旋转90度的方向映射
     * Y轴顺时针旋转（俯视图）
     */
    private static final Map<String, String> RIGHT_90_MAP = new HashMap<>();
    static {
        RIGHT_90_MAP.put("front", "right");
        RIGHT_90_MAP.put("right", "back");
        RIGHT_90_MAP.put("back", "left");
        RIGHT_90_MAP.put("left", "front");
        RIGHT_90_MAP.put("up", "up");      // 上下方向不变
        RIGHT_90_MAP.put("down", "down");
    }

    /**
     * 转身180度的方向映射
     */
    private static final Map<String, String> ROTATE_180_MAP = new HashMap<>();
    static {
        ROTATE_180_MAP.put("front", "back");
        ROTATE_180_MAP.put("back", "front");
        ROTATE_180_MAP.put("left", "right");
        ROTATE_180_MAP.put("right", "left");
        ROTATE_180_MAP.put("up", "up");      // 上下方向不变
        ROTATE_180_MAP.put("down", "down");
    }

    /**
     * 向左旋转90度（逆时针）
     * 重映射骨骼的所有方向属性
     *
     * @param bone 要旋转的骨骼
     */
    public static void rotateDirectionsLeft90(Bone bone) {
        applyDirectionMapping(bone, LEFT_90_MAP);
    }

    /**
     * 向左旋转90度（逆时针）- EditorBone版本
     * 重映射骨骼的所有方向属性
     *
     * @param bone 要旋转的编辑器骨骼
     */
    public static void rotateDirectionsLeft90(EditorBone bone) {
        applyDirectionMappingEditor(bone, LEFT_90_MAP);
    }

    /**
     * 向右旋转90度（顺时针）
     * 重映射骨骼的所有方向属性
     *
     * @param bone 要旋转的骨骼
     */
    public static void rotateDirectionsRight90(Bone bone) {
        applyDirectionMapping(bone, RIGHT_90_MAP);
    }

    /**
     * 向右旋转90度（顺时针）- EditorBone版本
     * 重映射骨骼的所有方向属性
     *
     * @param bone 要旋转的编辑器骨骼
     */
    public static void rotateDirectionsRight90(EditorBone bone) {
        applyDirectionMappingEditor(bone, RIGHT_90_MAP);
    }

    /**
     * 转身180度
     * 重映射骨骼的所有方向属性
     *
     * @param bone 要旋转的骨骼
     */
    public static void rotateDirections180(Bone bone) {
        applyDirectionMapping(bone, ROTATE_180_MAP);
    }

    /**
     * 转身180度 - EditorBone版本
     * 重映射骨骼的所有方向属性
     *
     * @param bone 要旋转的编辑器骨骼
     */
    public static void rotateDirections180(EditorBone bone) {
        applyDirectionMappingEditor(bone, ROTATE_180_MAP);
    }

    /**
     * 应用方向映射
     * 将骨骼的所有方向相关属性根据映射表重新分配
     *
     * @param bone 目标骨骼
     * @param directionMap 方向映射表（旧方向 → 新方向）
     */
    private static void applyDirectionMapping(Bone bone, Map<String, String> directionMap) {
        if (bone == null || directionMap == null) {
            return;
        }

        // 1. 保存当前所有方向的属性
        Map<String, String> oldTextures = new HashMap<>(bone.getDirectionTextures());
        Map<String, float[]> oldUVs = new HashMap<>(bone.getDirectionUVs());
        Map<String, Integer> oldPriorities = new HashMap<>(bone.getDirectionPriorities());
        Map<String, Float> oldWidths = new HashMap<>(bone.getDirectionWidths());
        Map<String, Float> oldHeights = new HashMap<>(bone.getDirectionHeights());
        Map<String, float[]> oldOffsets = new HashMap<>(bone.getDirectionOffsets());
        Map<String, float[]> oldRotations = new HashMap<>(bone.getDirectionRotations());
        Map<String, float[]> oldContentCenters = new HashMap<>(bone.getDirectionContentCenters());
        Map<String, Float> oldTextureRotations = new HashMap<>(bone.getDirectionTextureRotations());

        // 2. 清空当前方向属性
        bone.getDirectionTextures().clear();
        bone.getDirectionUVs().clear();
        bone.getDirectionPriorities().clear();
        bone.getDirectionWidths().clear();
        bone.getDirectionHeights().clear();
        bone.getDirectionOffsets().clear();
        bone.getDirectionRotations().clear();
        bone.getDirectionContentCenters().clear();
        bone.getDirectionTextureRotations().clear();

        // 3. 根据映射重新分配属性
        for (Map.Entry<String, String> mapping : directionMap.entrySet()) {
            String oldDir = mapping.getKey();
            String newDir = mapping.getValue();

            // 贴图路径
            if (oldTextures.containsKey(oldDir)) {
                bone.setDirectionTexture(newDir, oldTextures.get(oldDir));
            }

            // UV坐标
            if (oldUVs.containsKey(oldDir)) {
                float[] uv = oldUVs.get(oldDir);
                bone.setDirectionUV(newDir, uv[0], uv[1], uv[2], uv[3]);
            }

            // 优先级
            if (oldPriorities.containsKey(oldDir)) {
                bone.setDirectionPriority(newDir, oldPriorities.get(oldDir));
            }

            // 宽度
            if (oldWidths.containsKey(oldDir)) {
                bone.setDirectionWidth(newDir, oldWidths.get(oldDir));
            }

            // 高度
            if (oldHeights.containsKey(oldDir)) {
                bone.setDirectionHeight(newDir, oldHeights.get(oldDir));
            }

            // 位置偏移
            if (oldOffsets.containsKey(oldDir)) {
                float[] offset = oldOffsets.get(oldDir);
                bone.setDirectionOffset(newDir, offset[0], offset[1], offset[2]);
            }

            // 旋转
            if (oldRotations.containsKey(oldDir)) {
                float[] rotation = oldRotations.get(oldDir);
                bone.setDirectionRotation(newDir, rotation[0], rotation[1], rotation[2]);
            }

            // 内容中心偏移
            if (oldContentCenters.containsKey(oldDir)) {
                float[] contentCenter = oldContentCenters.get(oldDir);
                bone.setDirectionContentCenter(newDir, contentCenter[0], contentCenter[1]);
            }

            // 贴图旋转
            if (oldTextureRotations.containsKey(oldDir)) {
                bone.setDirectionTextureRotation(newDir, oldTextureRotations.get(oldDir));
            }
        }

        // 4. 更新当前方向（如果当前方向被映射了）
        String currentDir = bone.getCurrentDirection();
        if (directionMap.containsKey(currentDir)) {
            bone.setCurrentDirection(directionMap.get(currentDir));
        }
    }

    /**
     * 获取方向经过旋转后的新方向
     *
     * @param direction 原方向
     * @param rotationType 旋转类型：0=左转90°, 1=右转90°, 2=转身180°
     * @return 新方向
     */
    public static String getRotatedDirection(String direction, int rotationType) {
        switch (rotationType) {
            case 0:  // 左转90°
                return LEFT_90_MAP.getOrDefault(direction, direction);
            case 1:  // 右转90°
                return RIGHT_90_MAP.getOrDefault(direction, direction);
            case 2:  // 转身180°
                return ROTATE_180_MAP.getOrDefault(direction, direction);
            default:
                return direction;
        }
    }

    /**
     * 应用方向映射 - EditorBone版本
     * 将编辑器骨骼的所有方向相关属性根据映射表重新分配
     *
     * @param bone 目标编辑器骨骼
     * @param directionMap 方向映射表（旧方向 → 新方向）
     */
    private static void applyDirectionMappingEditor(EditorBone bone, Map<String, String> directionMap) {
        if (bone == null || directionMap == null) {
            return;
        }

        // 1. 保存当前所有方向的属性
        Map<String, String> oldTextures = new HashMap<>(bone.getDirectionTextures());
        Map<String, float[]> oldUVs = new HashMap<>(bone.getDirectionUVs());
        Map<String, Integer> oldPriorities = new HashMap<>(bone.getDirectionPriorities());
        Map<String, Float> oldWidths = new HashMap<>(bone.getDirectionWidths());
        Map<String, Float> oldHeights = new HashMap<>(bone.getDirectionHeights());
        Map<String, float[]> oldOffsets = new HashMap<>(bone.getDirectionOffsets());
        Map<String, float[]> oldRotations = new HashMap<>(bone.getDirectionRotations());
        Map<String, float[]> oldContentCenters = new HashMap<>(bone.getDirectionContentCenters());
        Map<String, Float> oldTextureRotations = new HashMap<>(bone.getDirectionTextureRotations());

        // 2. 清空当前方向属性
        bone.getDirectionTextures().clear();
        bone.getDirectionUVs().clear();
        bone.getDirectionPriorities().clear();
        bone.getDirectionWidths().clear();
        bone.getDirectionHeights().clear();
        bone.getDirectionOffsets().clear();
        bone.getDirectionRotations().clear();
        bone.getDirectionContentCenters().clear();
        bone.getDirectionTextureRotations().clear();

        // 3. 根据映射重新分配属性
        for (Map.Entry<String, String> mapping : directionMap.entrySet()) {
            String oldDir = mapping.getKey();
            String newDir = mapping.getValue();

            // 贴图路径
            if (oldTextures.containsKey(oldDir)) {
                bone.setDirectionTexture(newDir, oldTextures.get(oldDir));
            }

            // UV坐标
            if (oldUVs.containsKey(oldDir)) {
                float[] uv = oldUVs.get(oldDir);
                bone.setDirectionUV(newDir, uv[0], uv[1], uv[2], uv[3]);
            }

            // 优先级
            if (oldPriorities.containsKey(oldDir)) {
                bone.setDirectionPriority(newDir, oldPriorities.get(oldDir));
            }

            // 宽度
            if (oldWidths.containsKey(oldDir)) {
                bone.setDirectionWidth(newDir, oldWidths.get(oldDir));
            }

            // 高度
            if (oldHeights.containsKey(oldDir)) {
                bone.setDirectionHeight(newDir, oldHeights.get(oldDir));
            }

            // 位置偏移
            if (oldOffsets.containsKey(oldDir)) {
                float[] offset = oldOffsets.get(oldDir);
                bone.setDirectionOffset(newDir, offset[0], offset[1], offset[2]);
            }

            // 旋转
            if (oldRotations.containsKey(oldDir)) {
                float[] rotation = oldRotations.get(oldDir);
                bone.setDirectionRotation(newDir, rotation[0], rotation[1], rotation[2]);
            }

            // 内容中心偏移
            if (oldContentCenters.containsKey(oldDir)) {
                float[] contentCenter = oldContentCenters.get(oldDir);
                bone.setDirectionContentCenter(newDir, contentCenter[0], contentCenter[1]);
            }

            // 贴图旋转
            if (oldTextureRotations.containsKey(oldDir)) {
                bone.setDirectionTextureRotation(newDir, oldTextureRotations.get(oldDir));
            }
        }

        // 4. 更新当前方向（如果当前方向被映射了）
        String currentDir = bone.getCurrentDirection();
        if (directionMap.containsKey(currentDir)) {
            bone.setCurrentDirection(directionMap.get(currentDir));
        }
    }

    /**
     * 获取逆向旋转映射
     * 用于撤销旋转操作
     *
     * @param directionMap 正向映射
     * @return 逆向映射
     */
    private static Map<String, String> getReverseMapping(Map<String, String> directionMap) {
        Map<String, String> reverse = new HashMap<>();
        for (Map.Entry<String, String> entry : directionMap.entrySet()) {
            reverse.put(entry.getValue(), entry.getKey());
        }
        return reverse;
    }
}
