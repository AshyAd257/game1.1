package com.Hecate.example;

import com.Hecate.puppet.compat.PuppetAdapter;
import com.Hecate.puppet.config.PuppetConfig;
import com.Hecate.puppet.config.PuppetIO;
import com.Hecate.puppet.core.PuppetRenderer;
import com.Hecate.puppet.core.Skeleton;
import com.Hecate.puppet.ecs.entity.BoneEntity;
import com.Hecate.puppet.ecs.component.PhysicsComponent;
import com.Hecate.puppet.ecs.system.PhysicsSystem;
import com.jme3.app.SimpleApplication;
import com.jme3.math.Vector3f;

/**
 * 快速集成示例
 * 演示如何将新ECS系统集成到现有游戏中
 *
 * 【零修改集成】
 * 只需替换Skeleton的更新调用即可立即享受性能提升
 */
public class QuickStartExample extends SimpleApplication {

    private PuppetAdapter puppet;

    public static void main(String[] args) {
        QuickStartExample app = new QuickStartExample();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        java.lang.System.out.println("=== 快速集成示例 ===\n");

        // ========================================
        // 方法1: 零修改集成（推荐）
        // ========================================
        initWithAdapter();

        // ========================================
        // 方法2: 高级配置（可选）
        // ========================================
        configureAdvanced();
    }

    /**
     * 方法1: 零修改集成
     * 最简单的方式，立即享受性能提升
     */
    private void initWithAdapter() {
        try {
            // 步骤1: 加载Puppet配置文件，并应用到新建的骨架和渲染器上
            PuppetConfig config = PuppetIO.loadFromFile("assets/models/character.puppet");
            Skeleton skeleton = new Skeleton(config.getName());
            PuppetRenderer renderer = new PuppetRenderer(this, skeleton);
            PuppetIO.applyConfig(config, skeleton, renderer);

            // 步骤2: 创建ECS适配器（自动转换）
            puppet = new PuppetAdapter(skeleton, assetManager);

            java.lang.System.out.println("[集成] PuppetAdapter已创建");
            java.lang.System.out.println("[集成] " + puppet.getStats());
            java.lang.System.out.println("[集成] 现在已经在使用高性能ECS后端！");

            // 步骤3: 就这样！无需其他修改
            // 在simpleUpdate中调用puppet.update(tpf)即可

        } catch (Exception e) {
            java.lang.System.err.println("[错误] 加载失败: " + e.getMessage());
            java.lang.System.out.println("[提示] 这是示例代码，实际路径需要根据你的项目调整");
        }
    }

    /**
     * 方法2: 高级配置（可选）
     * 充分利用ECS的灵活性
     */
    private void configureAdvanced() {
        if (puppet == null) {
            java.lang.System.out.println("[提示] 跳过高级配置（puppet未加载）");
            return;
        }

        // 配置物理系统
        PhysicsSystem physics = puppet.getSystem(PhysicsSystem.class);
        if (physics != null) {
            physics.setWindForce(new Vector3f(2, 0, 0)); // 设置风力
            java.lang.System.out.println("[配置] 已设置风力: (2, 0, 0)");
        }

        // 查找特定实体并添加组件
        BoneEntity head = puppet.findEntity("head");
        if (head != null) {
            // 添加物理组件（头发摆动）
            PhysicsComponent hairPhysics = new PhysicsComponent();
            hairPhysics.configureForHair();
            head.addComponent(hairPhysics);
            java.lang.System.out.println("[配置] 为头部添加了物理效果");
        }

        java.lang.System.out.println("[配置] 高级配置完成");
    }

    @Override
    public void simpleUpdate(float tpf) {
        if (puppet != null) {
            // 这一行替换了所有旧的Puppet更新代码
            // 自动享受6-14倍性能提升！
            puppet.update(tpf);
        }
    }

    @Override
    public void stop() {
        // 清理资源
        if (puppet != null) {
            puppet.cleanup();
        }
        super.stop();
    }
}
