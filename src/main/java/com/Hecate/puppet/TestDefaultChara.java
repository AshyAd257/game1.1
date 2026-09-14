package com.Hecate.puppet;

import com.Hecate.puppet.config.PuppetConfig;
import com.Hecate.puppet.config.PuppetIO;
import com.Hecate.puppet.core.PuppetRenderer;
import com.Hecate.puppet.core.Skeleton;
import com.jme3.app.SimpleApplication;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;

/**
 * 测试加载 defaultChara1 人物模型
 * 验证面部、身体和脖子的 puppet 部件是否正确加载和显示
 */
public class TestDefaultChara extends SimpleApplication {

    private PuppetRenderer puppetRenderer;
    private Node puppetNode;

    public static void main(String[] args) {
        TestDefaultChara app = new TestDefaultChara();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        // 设置背景颜色
        viewPort.setBackgroundColor(new ColorRGBA(0.5f, 0.7f, 1.0f, 1.0f));

        // 禁用默认的 FlyCam
        flyCam.setMoveSpeed(10f);

        // 设置相机位置
        cam.setLocation(new Vector3f(0, 2, 5));
        cam.lookAt(new Vector3f(0, 1.5f, 0), Vector3f.UNIT_Y);

        // 添加光照
        DirectionalLight sun = new DirectionalLight();
        sun.setDirection(new Vector3f(-0.5f, -1.0f, -0.5f).normalizeLocal());
        sun.setColor(ColorRGBA.White);
        rootNode.addLight(sun);

        // 加载人物 puppet
        loadCharacter();
    }

    /**
     * 加载 defaultChara1 人物模型
     */
    private void loadCharacter() {
        try {
            System.out.println("正在加载 defaultChara1 人物模型...");

            // 加载 puppet 配置
            String puppetPath = "puppets/defaultChara1/defaultChara1.puppet";
            PuppetConfig config = PuppetIO.loadFromResource(puppetPath);

            System.out.println("配置加载成功: " + config.getName());
            System.out.println("骨骼数量: " + config.getBones().size());

            // 创建骨架和渲染器
            Skeleton skeleton = new Skeleton(config.getName());
            puppetRenderer = new PuppetRenderer(this, skeleton);

            // 应用配置
            PuppetIO.applyConfig(config, skeleton, puppetRenderer);

            System.out.println("骨架创建成功，骨骼列表：");
            for (com.Hecate.puppet.core.Bone bone : skeleton.getAllBones()) {
                System.out.println("  - " + bone.getName() +
                    " (位置: " + bone.getLocalPosition() +
                    ", 父骨骼: " + (bone.getParent() != null ? bone.getParent().getName() : "无") + ")");
            }

            // 创建节点并添加到场景
            puppetNode = new Node("DefaultCharaNode");
            puppetNode.setLocalTranslation(0, 0, 0);
            puppetNode.attachChild(puppetRenderer.getPuppetNode());
            rootNode.attachChild(puppetNode);

            // 设置 Billboard 模式
            puppetRenderer.setBillboardMode(PuppetRenderer.BillboardMode.UNIFIED);

            System.out.println("人物模型加载完成！");
            System.out.println("\n操作说明：");
            System.out.println("- WASD: 移动相机");
            System.out.println("- 鼠标拖动: 旋转视角");
            System.out.println("- 滚轮: 缩放");

        } catch (Exception e) {
            System.err.println("加载人物模型失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void simpleUpdate(float tpf) {
        // 更新 puppet 渲染器
        if (puppetRenderer != null) {
            puppetRenderer.update(tpf);
        }
    }
}
