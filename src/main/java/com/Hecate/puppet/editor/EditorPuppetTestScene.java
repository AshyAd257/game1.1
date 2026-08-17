package com.Hecate.puppet.editor;

// 使用编辑器专用的核心类
import com.Hecate.puppet.editor.core.EditorBone;
import com.Hecate.puppet.editor.core.EditorSkeleton;
import com.Hecate.puppet.editor.core.EditorPuppetRenderer;
import com.Hecate.puppet.editor.core.EditorPuppetPartRenderer;
import com.Hecate.puppet.config.PartConfig;
import com.jme3.app.SimpleApplication;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;

/**
 * 编辑器木偶测试场景
 * 创建一个简单的测试木偶并显示
 *
 * 注意：此类使用编辑器专用的EditorPuppetRenderer
 */
public class EditorPuppetTestScene {

    private final SimpleApplication app;
    private EditorPuppetRenderer puppetRenderer;
    private EditorSkeleton testSkeleton;

    public EditorPuppetTestScene(SimpleApplication app) {
        this.app = app;
    }

    /**
     * 创建测试木偶
     * 完整人形骨骼: 头部、身体、大臂左、小臂左、大腿左、小腿左
     */
    public void createTestPuppet(Node parentNode, Vector3f worldPosition) {
        // 创建骨架
        testSkeleton = new EditorSkeleton("TestPuppet");

        // 创建骨骼
        EditorBone body = new EditorBone("Body");
        EditorBone head = new EditorBone("Head");

        // 手臂
        EditorBone leftUpperArm = new EditorBone("LeftUpperArm");
        EditorBone leftLowerArm = new EditorBone("LeftLowerArm");
        EditorBone rightUpperArm = new EditorBone("RightUpperArm");
        EditorBone rightLowerArm = new EditorBone("RightLowerArm");

        // 腿部
        EditorBone leftUpperLeg = new EditorBone("LeftUpperLeg");
        EditorBone leftLowerLeg = new EditorBone("LeftLowerLeg");
        EditorBone rightUpperLeg = new EditorBone("RightUpperLeg");
        EditorBone rightLowerLeg = new EditorBone("RightLowerLeg");

        // 设置Rest姿势（相对于父骨骼的位置）
        // 所有部件使用统一的scale (1,1,1) 保持正方形
        // 身体在原点
        body.setRestPosition(new Vector3f(0, 0, 0));
        body.setRestRotation(new Quaternion());
        body.setRestScale(new Vector3f(1, 1, 1));

        // 头部在身体上方
        head.setRestPosition(new Vector3f(0, 1.8f, 0));
        head.setRestRotation(new Quaternion());
        head.setRestScale(new Vector3f(1, 1, 1));

        // 左大臂（从肩膀开始）
        leftUpperArm.setRestPosition(new Vector3f(-1.0f, 1.2f, 0));
        leftUpperArm.setRestRotation(new Quaternion());
        leftUpperArm.setRestScale(new Vector3f(1, 1, 1));

        // 左小臂（从肘部开始）
        leftLowerArm.setRestPosition(new Vector3f(0, -1.0f, 0));
        leftLowerArm.setRestRotation(new Quaternion());
        leftLowerArm.setRestScale(new Vector3f(1, 1, 1));

        // 右大臂
        rightUpperArm.setRestPosition(new Vector3f(1.0f, 1.2f, 0));
        rightUpperArm.setRestRotation(new Quaternion());
        rightUpperArm.setRestScale(new Vector3f(1, 1, 1));

        // 右小臂
        rightLowerArm.setRestPosition(new Vector3f(0, -1.0f, 0));
        rightLowerArm.setRestRotation(new Quaternion());
        rightLowerArm.setRestScale(new Vector3f(1, 1, 1));

        // 左大腿（从臀部开始）
        leftUpperLeg.setRestPosition(new Vector3f(-0.5f, -1.2f, 0));
        leftUpperLeg.setRestRotation(new Quaternion());
        leftUpperLeg.setRestScale(new Vector3f(1, 1, 1));

        // 左小腿（从膝盖开始）
        leftLowerLeg.setRestPosition(new Vector3f(0, -1.2f, 0));
        leftLowerLeg.setRestRotation(new Quaternion());
        leftLowerLeg.setRestScale(new Vector3f(1, 1, 1));

        // 右大腿
        rightUpperLeg.setRestPosition(new Vector3f(0.5f, -1.2f, 0));
        rightUpperLeg.setRestRotation(new Quaternion());
        rightUpperLeg.setRestScale(new Vector3f(1, 1, 1));

        // 右小腿
        rightLowerLeg.setRestPosition(new Vector3f(0, -1.2f, 0));
        rightLowerLeg.setRestRotation(new Quaternion());
        rightLowerLeg.setRestScale(new Vector3f(1, 1, 1));

        // 设置当前姿势等于Rest姿势
        body.resetToRestPose();
        head.resetToRestPose();
        leftUpperArm.resetToRestPose();
        leftLowerArm.resetToRestPose();
        rightUpperArm.resetToRestPose();
        rightLowerArm.resetToRestPose();
        leftUpperLeg.resetToRestPose();
        leftLowerLeg.resetToRestPose();
        rightUpperLeg.resetToRestPose();
        rightLowerLeg.resetToRestPose();

        // 构建层级关系
        body.addChild(head);
        body.addChild(leftUpperArm);
        leftUpperArm.addChild(leftLowerArm);
        body.addChild(rightUpperArm);
        rightUpperArm.addChild(rightLowerArm);
        body.addChild(leftUpperLeg);
        leftUpperLeg.addChild(leftLowerLeg);
        body.addChild(rightUpperLeg);
        rightUpperLeg.addChild(rightLowerLeg);

        // 设置根骨骼
        testSkeleton.setRootBone(body);

        // 创建渲染器（使用编辑器专用渲染器）
        puppetRenderer = new EditorPuppetRenderer(app, testSkeleton);
        // 设置默认部件尺寸为2x2正方形
        puppetRenderer.setDefaultPartSize(2.0f, 2.0f);
        puppetRenderer.initialize();

        // 设置不同颜色用于区分各部件
        setPartColors();

        // 设置默认模型的方向贴图（修复"纸片"效果）
        setupDefaultDirectionTextures();

        // 附加到场景
        puppetRenderer.attachToScene(parentNode);

        // 设置木偶的世界位置
        puppetRenderer.setWorldPosition(worldPosition);

    }

    /**
     * 设置各部件的调试颜色
     */
    private void setPartColors() {
        // 躯干和头部
        setPartColor("Body", ColorRGBA.Blue);
        setPartColor("Head", ColorRGBA.Yellow);

        // 左侧肢体（红色系）
        setPartColor("LeftUpperArm", new ColorRGBA(1.0f, 0.3f, 0.3f, 1.0f));  // 浅红
        setPartColor("LeftLowerArm", new ColorRGBA(0.8f, 0.2f, 0.2f, 1.0f));  // 深红
        setPartColor("LeftUpperLeg", new ColorRGBA(1.0f, 0.4f, 0.4f, 1.0f));  // 粉红
        setPartColor("LeftLowerLeg", new ColorRGBA(0.9f, 0.25f, 0.25f, 1.0f)); // 中红

        // 右侧肢体（绿色系）
        setPartColor("RightUpperArm", new ColorRGBA(0.3f, 1.0f, 0.3f, 1.0f));  // 浅绿
        setPartColor("RightLowerArm", new ColorRGBA(0.2f, 0.8f, 0.2f, 1.0f));  // 深绿
        setPartColor("RightUpperLeg", new ColorRGBA(0.4f, 1.0f, 0.4f, 1.0f));  // 淡绿
        setPartColor("RightLowerLeg", new ColorRGBA(0.25f, 0.9f, 0.25f, 1.0f)); // 中绿
    }

    /**
     * 辅助方法：设置单个部件颜色
     */
    private void setPartColor(String boneName, ColorRGBA color) {
        EditorPuppetPartRenderer renderer = puppetRenderer.getPartRenderer(boneName);
        if (renderer != null) {
            renderer.setDebugColor(color);
        }
    }

    /**
     * 更新测试木偶（每帧调用）
     */
    public void update(float tpf) {
        if (puppetRenderer != null && puppetRenderer.isInitialized()) {
            puppetRenderer.update(tpf);
        }
    }

    /**
     * 播放简单的动画测试
     * 演示层级骨骼动画：摆手、摆腿、头部摆动
     */
    public void playWaveAnimation(float time) {
        if (testSkeleton == null) {
            return;
        }

        // 头部轻微左右摆动
        EditorBone head = testSkeleton.findBone("Head");
        if (head != null) {
            float angle = (float) Math.sin(time * 1.5f) * 0.2f;
            Quaternion rotation = new Quaternion();
            rotation.fromAngleAxis(angle, Vector3f.UNIT_Z);
            head.setLocalRotation(rotation);
        }

        // 左大臂摆动（带动小臂）
        EditorBone leftUpperArm = testSkeleton.findBone("LeftUpperArm");
        if (leftUpperArm != null) {
            float angle = (float) Math.sin(time * 2.0f) * 0.6f;
            Quaternion rotation = new Quaternion();
            rotation.fromAngleAxis(angle, Vector3f.UNIT_Z);
            leftUpperArm.setLocalRotation(rotation);
        }

        // 左小臂额外弯曲
        EditorBone leftLowerArm = testSkeleton.findBone("LeftLowerArm");
        if (leftLowerArm != null) {
            float angle = (float) Math.sin(time * 2.5f) * 0.4f - 0.3f;
            Quaternion rotation = new Quaternion();
            rotation.fromAngleAxis(angle, Vector3f.UNIT_Z);
            leftLowerArm.setLocalRotation(rotation);
        }

        // 右大臂反向摆动
        EditorBone rightUpperArm = testSkeleton.findBone("RightUpperArm");
        if (rightUpperArm != null) {
            float angle = (float) Math.sin(time * 2.0f + Math.PI) * 0.6f;
            Quaternion rotation = new Quaternion();
            rotation.fromAngleAxis(angle, Vector3f.UNIT_Z);
            rightUpperArm.setLocalRotation(rotation);
        }

        // 右小臂额外弯曲
        EditorBone rightLowerArm = testSkeleton.findBone("RightLowerArm");
        if (rightLowerArm != null) {
            float angle = (float) Math.sin(time * 2.5f + Math.PI) * 0.4f - 0.3f;
            Quaternion rotation = new Quaternion();
            rotation.fromAngleAxis(angle, Vector3f.UNIT_Z);
            rightLowerArm.setLocalRotation(rotation);
        }

        // 左大腿轻微摆动（模拟走路）
        EditorBone leftUpperLeg = testSkeleton.findBone("LeftUpperLeg");
        if (leftUpperLeg != null) {
            float angle = (float) Math.sin(time * 1.8f) * 0.3f;
            Quaternion rotation = new Quaternion();
            rotation.fromAngleAxis(angle, Vector3f.UNIT_Z);
            leftUpperLeg.setLocalRotation(rotation);
        }

        // 左小腿弯曲
        EditorBone leftLowerLeg = testSkeleton.findBone("LeftLowerLeg");
        if (leftLowerLeg != null) {
            float angle = Math.max(0, (float) Math.sin(time * 1.8f) * 0.4f);
            Quaternion rotation = new Quaternion();
            rotation.fromAngleAxis(angle, Vector3f.UNIT_Z);
            leftLowerLeg.setLocalRotation(rotation);
        }

        // 右大腿反向摆动
        EditorBone rightUpperLeg = testSkeleton.findBone("RightUpperLeg");
        if (rightUpperLeg != null) {
            float angle = (float) Math.sin(time * 1.8f + Math.PI) * 0.3f;
            Quaternion rotation = new Quaternion();
            rotation.fromAngleAxis(angle, Vector3f.UNIT_Z);
            rightUpperLeg.setLocalRotation(rotation);
        }

        // 右小腿弯曲
        EditorBone rightLowerLeg = testSkeleton.findBone("RightLowerLeg");
        if (rightLowerLeg != null) {
            float angle = Math.max(0, (float) Math.sin(time * 1.8f + Math.PI) * 0.4f);
            Quaternion rotation = new Quaternion();
            rotation.fromAngleAxis(angle, Vector3f.UNIT_Z);
            rightLowerLeg.setLocalRotation(rotation);
        }
    }

    /**
     * 设置默认模型的方向贴图
     * 修复"纸片"效果 - 为所有骨骼启用多方向贴图
     */
    private void setupDefaultDirectionTextures() {
        if (testSkeleton == null) {
            return;
        }

        // 为所有骨骼启用多方向贴图
        for (EditorBone bone : testSkeleton.getAllBones()) {
            bone.setMultiDirectionTextureEnabled(true);

            // 设置所有6个方向的贴图路径
            // 注意：这里使用空字符串，因为默认模型使用调试颜色而不是实际贴图
            // 但必须设置directionTextures以启用方向切换功能
            bone.setDirectionTexture(EditorBone.Direction.FRONT, "");
            bone.setDirectionTexture(EditorBone.Direction.BACK, "");
            bone.setDirectionTexture(EditorBone.Direction.LEFT, "");
            bone.setDirectionTexture(EditorBone.Direction.RIGHT, "");
            bone.setDirectionTexture(EditorBone.Direction.UP, "");
            bone.setDirectionTexture(EditorBone.Direction.DOWN, "");
        }
    }

    /**
     * 清理资源
     */
    public void cleanup() {
        if (puppetRenderer != null) {
            puppetRenderer.cleanup();
        }
    }

    // ========== Getters ==========

    public EditorPuppetRenderer getPuppetRenderer() {
        return puppetRenderer;
    }

    public EditorSkeleton getTestSkeleton() {
        return testSkeleton;
    }

    // ========== Setters ==========

    /**
     * 设置新的Skeleton（用于加载模型时替换）
     */
    public void setTestSkeleton(EditorSkeleton skeleton) {
        this.testSkeleton = skeleton;
    }

    /**
     * 设置新的PuppetRenderer（用于加载模型时替换）
     */
    public void setPuppetRenderer(EditorPuppetRenderer renderer) {
        this.puppetRenderer = renderer;
    }
}
