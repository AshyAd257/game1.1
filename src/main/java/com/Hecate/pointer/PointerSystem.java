package com.Hecate.pointer;

import com.jme3.app.SimpleApplication;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;

import com.Hecate.player.PlayerController;

/**
 * 🎯 智能指针系统 - 与武器系统联动
 */
public class PointerSystem {
    private final SimpleApplication app;
    private final PlayerController player;
    private final Node rootNode;

    // 指针设置
    private final float POINTER_RANGE = 8.0f; // 指针检测范围
    private PointerState currentState = PointerState.NORMAL;
    private Geometry pointerIndicator;

    // UI 元素
    private BitmapText crosshair;
    private BitmapText infoText;
    private BitmapFont font;

    // 高亮系统
    private Geometry highlightBox;
    private Material highlightMaterial;
    private Vector3f currentTargetBlock = null;

    // 动画相关
    private float animationTime = 0f;
    private float chargingProgress = 0f;

    public PointerSystem(SimpleApplication app, PlayerController player) {
        this.app = app;
        this.player = player;
        this.rootNode = app.getRootNode();

        initializePointerSystem();
        System.out.println("🎯 指针系统已初始化");
        System.out.println("📏 指针射程: " + POINTER_RANGE + " 格");
    }

    /**
     * 🎨 初始化指针系统
     */
    private void initializePointerSystem() {
        // 获取默认字体
        font = app.getAssetManager().loadFont("Interface/Fonts/Default.fnt");

        // 创建十字线
        createCrosshair();

        // 创建信息显示
        createInfoDisplay();

        // 创建高亮材质
        createHighlightMaterial();

        System.out.println("✅ 指针UI界面已创建");
    }

    /**
     * ➕ 创建十字线
     */
    private void createCrosshair() {
        crosshair = new BitmapText(font);
        crosshair.setText("+");
        crosshair.setSize(24);
        crosshair.setColor(ColorRGBA.White);

        // 居中显示
        float screenWidth = app.getCamera().getWidth();
        float screenHeight = app.getCamera().getHeight();
        crosshair.setLocalTranslation(
                screenWidth/2 - crosshair.getLineWidth()/2,
                screenHeight/2 + crosshair.getLineHeight()/2,
                0
        );

        app.getGuiNode().attachChild(crosshair);
        System.out.println("➕ 十字线已创建");
    }

    /**
     * 📊 创建信息显示
     */
    private void createInfoDisplay() {
        infoText = new BitmapText(font);
        infoText.setText("");
        infoText.setSize(14);
        infoText.setColor(ColorRGBA.Green);
        infoText.setLocalTranslation(10, app.getCamera().getHeight() - 10, 0);

        app.getGuiNode().attachChild(infoText);
        System.out.println("📊 信息显示已创建");
    }

    private void createPointerIndicator() {
        Box pointerBox = new Box(0.1f, 0.1f, 0.1f);
        pointerIndicator = new Geometry("PointerIndicator", pointerBox);

        Material pointerMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        pointerMat.setColor("Color", ColorRGBA.Red);
        pointerIndicator.setMaterial(pointerMat);

        rootNode.attachChild(pointerIndicator);
    }

    private void updatePointerIndicator() {
        if (pointerIndicator != null) {
            Vector3f playerPos = player.getPlayerPosition();
            Vector3f facingDir = player.getPlayerFacingDirection();

            // 计算指针显示位置
            Vector3f pointerPos = playerPos.add(facingDir.mult(3.0f));
            pointerPos.y += 2.0f; // 🔥 这里控制指针高度！

            pointerIndicator.setLocalTranslation(pointerPos);
        }
    }

    /**
     * 创建高亮材质
     */
    private void createHighlightMaterial() {
        highlightMaterial = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        highlightMaterial.setColor("Color", ColorRGBA.Yellow);
        highlightMaterial.getAdditionalRenderState().setWireframe(true);
        highlightMaterial.getAdditionalRenderState().setLineWidth(2.0f); // 加粗线条
        System.out.println("🌟 高亮材质已创建");
    }

    /**
     * 🎯 更新指针系统
     */
    public void update(float tpf) {
        animationTime += tpf;

        // 🔍 执行射线检测
        performRaycast();

        // 🎨 更新指针样式
        updatePointerAppearance(tpf);

        // 📊 更新信息显示
        updateInfoDisplay();
    }

    /**
     * 🔍 执行射线检测
     */
    private void performRaycast() {
        // 获取玩家位置和朝向
        Vector3f origin = player.getPointerOrigin();
        Vector3f direction = player.getPointerDirection();

        // 创建射线
        Ray ray = new Ray(origin, direction);

        // 执行射线检测
        CollisionResults results = new CollisionResults();
        rootNode.collideWith(ray, results);

        // 处理检测结果
        Vector3f newTargetBlock = null;
        for (CollisionResult result : results) {
            // 跳过玩家自己
            if (result.getGeometry().getName().equals("PlayerBlock")) {
                continue;
            }

            // 检查距离
            if (result.getDistance() <= POINTER_RANGE) {
                Vector3f hitPoint = result.getContactPoint();
                Vector3f blockPos = new Vector3f(
                        FastMath.floor(hitPoint.x),
                        FastMath.floor(hitPoint.y),
                        FastMath.floor(hitPoint.z)
                );
                newTargetBlock = blockPos;
                break;
            }
        }

        // 更新目标方块
        if (newTargetBlock != null && !newTargetBlock.equals(currentTargetBlock)) {
            currentTargetBlock = newTargetBlock;
            updateHighlight();
        } else if (newTargetBlock == null && currentTargetBlock != null) {
            currentTargetBlock = null;
            removeHighlight();
        }
    }

    /**
     * 🌟 更新高亮显示（修复版本）
     */
    private void updateHighlight() {
        removeHighlight();

        if (currentTargetBlock != null) {
            // ✅ 使用标准的 Box 几何体，设置为线框模式
            Box wireBoxGeometry = new Box(0.51f, 0.51f, 0.51f);
            highlightBox = new Geometry("HighlightBox", wireBoxGeometry);
            highlightBox.setMaterial(highlightMaterial);
            highlightBox.setLocalTranslation(
                    currentTargetBlock.x + 0.5f,
                    currentTargetBlock.y + 0.5f,
                    currentTargetBlock.z + 0.5f
            );

            rootNode.attachChild(highlightBox);
        }
    }

    /**
     * 🗑️ 移除高亮显示
     */
    private void removeHighlight() {
        if (highlightBox != null) {
            highlightBox.removeFromParent();
            highlightBox = null;
        }
    }

    /**
     * 🎨 更新指针外观（根据状态）
     */
    private void updatePointerAppearance(float tpf) {
        switch (currentState) {
            case NORMAL:
                crosshair.setText("+");
                crosshair.setColor(ColorRGBA.White);
                break;

            case CHARGING:
                // 蓄力动画 - 旋转的十字线
                float rotation = (animationTime * 4) % (FastMath.TWO_PI);
                String chargingSymbol = getChargingSymbol(rotation);
                crosshair.setText(chargingSymbol);

                // 颜色从白色变为红色
                crosshair.setColor(new ColorRGBA(1, 1 - chargingProgress, 1 - chargingProgress, 1));
                break;

            case READY:
                crosshair.setText("◎");
                crosshair.setColor(ColorRGBA.Red);
                break;

            case COOLDOWN:
                crosshair.setText("+");
                float alpha = (FastMath.sin(animationTime * 6) + 1) * 0.5f;
                crosshair.setColor(new ColorRGBA(0.5f, 0.5f, 0.5f, alpha));
                break;
        }
    }

    /**
     * 🌀 获取蓄力旋转符号
     */
    private String getChargingSymbol(float rotation) {
        int frame = (int)((rotation / FastMath.TWO_PI) * 4) % 4;
        switch (frame) {
            case 0: return "|";
            case 1: return "/";
            case 2: return "—";
            case 3: return "\\";
            default: return "+";
        }
    }

    /**
     * 📊 更新信息显示
     */
    private void updateInfoDisplay() {
        StringBuilder info = new StringBuilder();

        // 玩家朝向信息
        float facingDegrees = player.getPlayerFacing() * FastMath.RAD_TO_DEG;
        String direction = getDirectionName(facingDegrees);
        info.append("🧭 朝向: ").append(direction).append(" (").append((int)facingDegrees).append("°)\n");

        // 目标方块信息
        if (currentTargetBlock != null) {
            Vector3f playerPos = player.getPlayerPosition();
            float distance = playerPos.distance(currentTargetBlock);

            info.append("🎯 目标: (")
                    .append((int)currentTargetBlock.x).append(", ")
                    .append((int)currentTargetBlock.y).append(", ")
                    .append((int)currentTargetBlock.z).append(")\n");
            info.append("📏 距离: ").append(String.format("%.1f", distance)).append(" 格\n");
        } else {
            info.append("🎯 目标: 无\n");
        }

        // 指针状态
        info.append("⚡ 状态: ").append(getStateDisplayName(currentState));

        infoText.setText(info.toString());
    }

    /**
     * 🧭 获取方向名称
     */
    private String getDirectionName(float degrees) {
        degrees = ((degrees % 360) + 360) % 360; // 标准化到 0-360

        if (degrees < 22.5f || degrees >= 337.5f) return "北";
        else if (degrees < 67.5f) return "东北";
        else if (degrees < 112.5f) return "东";
        else if (degrees < 157.5f) return "东南";
        else if (degrees < 202.5f) return "南";
        else if (degrees < 247.5f) return "西南";
        else if (degrees < 292.5f) return "西";
        else return "西北";
    }

    /**
     * 📊 获取状态显示名称
     */
    private String getStateDisplayName(PointerState state) {
        switch (state) {
            case NORMAL: return "正常";
            case CHARGING: return "蓄力中";
            case READY: return "就绪";
            case COOLDOWN: return "冷却中";
            default: return "未知";
        }
    }

    // 🔫 武器系统接口方法
    public void setPointerState(PointerState state) {
        this.currentState = state;
    }

    public void setChargingProgress(float progress) {
        this.chargingProgress = FastMath.clamp(progress, 0f, 1f);
    }

    public Vector3f getCurrentTarget() {
        return currentTargetBlock != null ? currentTargetBlock.clone() : null;
    }

    public PointerState getCurrentState() {
        return currentState;
    }

    /**
     * 🎯 获取指针射程
     */
    public float getPointerRange() {
        return POINTER_RANGE;
    }

    /**
     * 🎨 设置高亮颜色
     */
    public void setHighlightColor(ColorRGBA color) {
        if (highlightMaterial != null) {
            highlightMaterial.setColor("Color", color);
        }
    }

    /**
     * 🧹 清理资源
     */
    public void cleanup() {
        removeHighlight();

        if (crosshair != null) {
            crosshair.removeFromParent();
        }

        if (infoText != null) {
            infoText.removeFromParent();
        }

        System.out.println("🧹 指针系统资源已清理");
    }
}
