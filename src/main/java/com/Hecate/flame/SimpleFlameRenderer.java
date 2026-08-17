package com.Hecate.flame;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.control.BillboardControl;
import com.jme3.scene.shape.Quad;
import com.Hecate.utils.LogUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 简化版火焰渲染器(3D世界空间)
 * 在3D世界中渲染彩色圆形粒子(使用Billboard)
 */
public class SimpleFlameRenderer {

    private final SimpleApplication app;
    private final AssetManager assetManager;
    private final Node rootNode;

    // 粒子系统
    private FlameParticleSystem particleSystem;

    // 地面火焰管理器
    private GroundFireManager groundFireManager;

    // 粒子节点(放在世界中)
    private Node particleNode;

    // 粒子几何体池
    private List<Geometry> particleGeometries;
    private int maxVisibleParticles = 500;

    // 地面火焰节点(静态长方形)
    private Node groundFireNode;
    private Map<GroundFire, Geometry> groundFireGeometries;

    public SimpleFlameRenderer(SimpleApplication app) {
        this.app = app;
        this.assetManager = app.getAssetManager();
        this.rootNode = app.getRootNode();

        // 创建粒子系统
        particleSystem = new FlameParticleSystem();

        // 创建地面火焰管理器
        groundFireManager = new GroundFireManager(particleSystem);

        // 将地面火焰管理器设置到粒子系统(用于触地通知)
        particleSystem.setGroundFireManager(groundFireManager);

        // 创建粒子节点
        particleNode = new Node("FlameParticles");
        rootNode.attachChild(particleNode);

        // 创建粒子几何体池
        particleGeometries = new ArrayList<>();
        for (int i = 0; i < maxVisibleParticles; i++) {
            Geometry particleGeom = createParticleGeometry();
            particleGeometries.add(particleGeom);
        }

        // 创建地面火焰节点
        groundFireNode = new Node("GroundFires");
        rootNode.attachChild(groundFireNode);
        groundFireGeometries = new HashMap<>();
    }

    /**
     * 创建单个粒子几何体(3D Billboard)
     */
    private Geometry createParticleGeometry() {
        float size = 0.2f; // 粒子大小(3D单位)
        Quad quad = new Quad(size, size);
        Geometry geom = new Geometry("FlameParticle", quad);

        // 创建材质
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", new ColorRGBA(1.0f, 0.5f, 0.0f, 0.7f)); // 橙色
        mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        mat.setTransparent(true);
        mat.getAdditionalRenderState().setDepthWrite(false);

        geom.setMaterial(mat);

        // 添加Billboard控件,让粒子始终面向摄像机
        BillboardControl billboardControl = new BillboardControl();
        billboardControl.setAlignment(BillboardControl.Alignment.Screen);
        geom.addControl(billboardControl);

        // 设置渲染队列为透明
        geom.setQueueBucket(RenderQueue.Bucket.Transparent);

        return geom;
    }

    /**
     * 创建地面火焰的静态长方形几何体
     */
    private Geometry createGroundFireGeometry() {
        // 创建一个垂直的长方形(宽0.3, 高0.5)
        float width = 0.3f;
        float height = 0.5f;
        Quad quad = new Quad(width, height);
        Geometry geom = new Geometry("GroundFire", quad);

        // 创建白色材质
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", new ColorRGBA(1.0f, 1.0f, 1.0f, 0.8f)); // 白色,略透明
        mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        mat.setTransparent(true);
        mat.getAdditionalRenderState().setDepthWrite(false);

        geom.setMaterial(mat);

        // 添加Billboard控件,让长方形始终面向摄像机
        BillboardControl billboardControl = new BillboardControl();
        billboardControl.setAlignment(BillboardControl.Alignment.Screen);
        geom.addControl(billboardControl);

        // 设置渲染队列为透明
        geom.setQueueBucket(RenderQueue.Bucket.Transparent);

        return geom;
    }

    /**
     * 更新渲染器(3D世界空间)
     */
    public void update(float tpf) {
        // 更新粒子系统(会自动处理触地并创建地面火焰)
        particleSystem.update(tpf);

        // 更新地面火焰管理器(但不发射粒子)
        updateGroundFiresOnly(tpf);

        // 获取粒子列表
        List<FlameParticle> particles = particleSystem.getParticles();

        // 清空当前粒子节点上的所有粒子
        particleNode.detachAllChildren();

        // 渲染活跃粒子
        int renderCount = Math.min(particles.size(), maxVisibleParticles);

        for (int i = 0; i < renderCount; i++) {
            FlameParticle particle = particles.get(i);
            if (!particle.isAlive()) continue;

            Geometry geom = particleGeometries.get(i);
            Vector3f pos = particle.getPosition();

            // 粒子大小随生命周期缩小(模拟消散) 减小变化幅度让过渡更平滑
            float lifeRatio = particle.getLifeRatio();
            float sizeMultiplier = 0.7f + lifeRatio * 0.3f; // 从100%缩小到70%(更平滑的过渡)

            // 设置3D位置(Quad中心在左下角,需要调整)
            float size = particle.getRadius() * sizeMultiplier;
            geom.setLocalTranslation(pos.x - size/2, pos.y - size/2, pos.z);

            // 设置大小
            geom.setLocalScale(sizeMultiplier);

            // 根据生命周期和强度设置颜色
            float lifeProgress = 1.0f - particle.getLifeRatio();
            float intensity = particle.getIntensity();
            ColorRGBA color = getFlameColor(lifeProgress, intensity);
            Material mat = geom.getMaterial();
            mat.setColor("Color", color);

            // 添加到粒子节点
            particleNode.attachChild(geom);
        }
    }

    /**
     * 根据生命周期和强度获取火焰颜色(改进版)
     * @param lifeProgress 生命进度 0(新生) → 1(消亡)
     * @param intensity 粒子强度
     */
    private ColorRGBA getFlameColor(float lifeProgress, float intensity) {
        // 颜色随生命周期变化:
        // 0.0-0.15: 白黄色(最热)
        // 0.15-0.35: 亮橙色
        // 0.35-0.60: 橙红色
        // 0.60-0.85: 深红色
        // 0.85-1.0: 灰烟(冷却消散)

        float alpha = Math.max(0.0f, 1.0f - lifeProgress * 0.8f); // 透明度逐渐降低

        if (lifeProgress < 0.15f) {
            // 白黄色(最热)
            float t = lifeProgress / 0.15f;
            return new ColorRGBA(
                1.0f,
                1.0f - t * 0.2f,  // 1.0 → 0.8
                0.9f - t * 0.5f,  // 0.9 → 0.4
                alpha * 0.9f
            );
        } else if (lifeProgress < 0.35f) {
            // 亮橙色
            float t = (lifeProgress - 0.15f) / 0.2f;
            return new ColorRGBA(
                1.0f,
                0.8f - t * 0.3f,  // 0.8 → 0.5
                0.4f - t * 0.3f,  // 0.4 → 0.1
                alpha
            );
        } else if (lifeProgress < 0.60f) {
            // 橙红色
            float t = (lifeProgress - 0.35f) / 0.25f;
            return new ColorRGBA(
                1.0f - t * 0.15f,  // 1.0 → 0.85
                0.5f - t * 0.3f,   // 0.5 → 0.2
                0.1f - t * 0.1f,   // 0.1 → 0.0
                alpha
            );
        } else if (lifeProgress < 0.85f) {
            // 深红色
            float t = (lifeProgress - 0.60f) / 0.25f;
            return new ColorRGBA(
                0.85f - t * 0.45f, // 0.85 → 0.4
                0.2f - t * 0.2f,   // 0.2 → 0.0
                0.0f,
                alpha
            );
        } else {
            // 灰烟(消散)
            float t = (lifeProgress - 0.85f) / 0.15f;
            return new ColorRGBA(
                0.4f - t * 0.1f,   // 0.4 → 0.3 暗灰色
                0.3f - t * 0.1f,   // 0.3 → 0.2
                0.3f - t * 0.1f,   // 0.3 → 0.2 带点灰蓝
                alpha * (1.0f - t * 0.7f) // 快速消失
            );
        }
    }

    /**
     * 在指定位置发射火焰(3D世界空间)
     */
    public void emitFlame(Vector3f worldPosition, int particleCount) {
        emitFlame(worldPosition, particleCount, null);
    }

    /**
     * 在指定位置发射火焰,带目标速度(3D世界空间)
     * @param worldPosition 发射位置(3D世界坐标)
     * @param particleCount 粒子数量
     * @param targetVelocity 目标速度(3D),如果为null则使用默认速度
     */
    public void emitFlame(Vector3f worldPosition, int particleCount, Vector3f targetVelocity) {
        // 使用扩散半径和初始强度(3D单位)
        particleSystem.burst(worldPosition, particleCount, 0.5f, 0.8f, targetVelocity);
    }

    /**
     * 获取粒子系统
     */
    public FlameParticleSystem getParticleSystem() {
        return particleSystem;
    }

    /**
     * 清理资源
     */
    public void cleanup() {
        particleNode.detachAllChildren();
        particleNode.removeFromParent();
        particleSystem.clear();
        groundFireManager.clear();
    }

    /**
     * 获取地面火焰管理器
     */
    public GroundFireManager getGroundFireManager() {
        return groundFireManager;
    }

    /**
     * 只更新地面火焰的生命周期,渲染为静态白色长方形(不发射粒子)
     */
    private void updateGroundFiresOnly(float tpf) {
        List<GroundFire> fires = groundFireManager.getGroundFires();

        // 更新地面火焰生命周期
        for (int i = fires.size() - 1; i >= 0; i--) {
            GroundFire fire = fires.get(i);

            // 只更新生命周期,不发射粒子
            fire.update(tpf);

            // 如果火焰死亡,移除对应的几何体
            if (!fire.isAlive()) {
                Geometry geom = groundFireGeometries.remove(fire);
                if (geom != null) {
                    groundFireNode.detachChild(geom);
                }
                fires.remove(i);
                continue;
            }

            // 为新的地面火焰创建几何体
            if (!groundFireGeometries.containsKey(fire)) {
                Geometry geom = createGroundFireGeometry();
                groundFireGeometries.put(fire, geom);
                groundFireNode.attachChild(geom);
            }

            // 更新几何体位置
            Geometry geom = groundFireGeometries.get(fire);
            if (geom != null) {
                Vector3f pos = fire.getPosition();

                // 计算火焰所在的方块Y坐标
                int blockY = (int) Math.floor(pos.y - 0.1f); // 减0.1防止浮点误差

                // 长方形应该站在方块顶部
                float groundY = blockY + 1.0f; // 方块顶部

                // Quad的原点在左下角,宽度0.3需要居中偏移0.15
                // 长方形底部应该正好在方块表面(groundY)
                geom.setLocalTranslation(pos.x - 0.15f, groundY, pos.z);

                // 根据生命周期调整透明度(随时间淡出)
                float alpha = fire.getLifeRatio() * 0.8f;
                Material mat = geom.getMaterial();
                mat.setColor("Color", new ColorRGBA(1.0f, 1.0f, 1.0f, alpha));
            }
        }
    }
}
