package com.Hecate.tools;

import com.jme3.anim.Armature;
import com.jme3.anim.Joint;
import com.jme3.anim.SkinningControl;
import com.jme3.app.SimpleApplication;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

/**
 * 工具：列出 3D 模型的所有骨骼名称
 * 用于查看 armlegmesh.glb 的骨骼结构
 */
public class ListModelBones extends SimpleApplication {

    public static void main(String[] args) {
        ListModelBones app = new ListModelBones();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        System.out.println("========================================");
        System.out.println("加载模型并列出骨骼结构");
        System.out.println("========================================\n");

        try {
            // 加载模型
            String modelPath = "mesh/armlegnew.glb";
            System.out.println("正在加载模型: " + modelPath);
            Spatial model = assetManager.loadModel(modelPath);
            System.out.println("模型加载成功！\n");

            // 递归查找 SkinningControl
            SkinningControl skinningControl = findControlRecursive(model, SkinningControl.class);

            if (skinningControl == null) {
                System.out.println("错误：模型中没有找到 SkinningControl（骨骼系统）");
                stop();
                return;
            }

            // 获取骨架
            Armature armature = skinningControl.getArmature();
            System.out.println("找到骨架！");
            System.out.println("骨骼总数: " + armature.getJointCount());
            System.out.println("\n========================================");
            System.out.println("骨骼列表（按层级显示）：");
            System.out.println("========================================\n");

            // 列出所有根骨骼
            for (Joint root : armature.getRoots()) {
                printJointHierarchy(root, 0);
            }

            System.out.println("\n========================================");
            System.out.println("建议的绑定方案：");
            System.out.println("========================================");
            System.out.println("找到以下骨骼后，可以将 puppet 部件附加到它们上：");
            System.out.println("1. 查找名为 'Head' 或 'head' 的骨骼 → 绑定面部 puppet");
            System.out.println("2. 查找名为 'Body' 或 'Spine' 的骨骼 → 绑定身体 puppet");
            System.out.println("3. 查找名为 'Neck' 的骨骼 → 绑定脖子 puppet");
            System.out.println("\n程序将在 3 秒后退出...");

            // 等待 3 秒后退出
            new Thread(() -> {
                try {
                    Thread.sleep(3000);
                    stop();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();

        } catch (Exception e) {
            System.err.println("加载模型失败: " + e.getMessage());
            e.printStackTrace();
            stop();
        }
    }

    /**
     * 递归打印骨骼层级结构
     */
    private void printJointHierarchy(Joint joint, int level) {
        // 打印缩进
        for (int i = 0; i < level; i++) {
            System.out.print("  ");
        }

        // 打印骨骼信息：同时输出localTransform（相对父骨骼）和modelTransform（相对骨架根），
        // 用于判断骨骼本身在Blender里是否正常、还是父子层级/坐标空间被曲解了。
        System.out.println("├─ " + joint.getName() +
            " (索引: " + joint.getId() +
            ", 子骨骼: " + joint.getChildren().size() + ")"
            + " | localTransform.translation(相对父骨骼): " + joint.getLocalTransform().getTranslation()
            + " | modelTransform.translation(相对骨架根): " + joint.getModelTransform().getTranslation());

        // 递归打印子骨骼
        for (Joint child : joint.getChildren()) {
            printJointHierarchy(child, level + 1);
        }
    }

    /**
     * 递归查找 Control
     */
    @SuppressWarnings("unchecked")
    private <T> T findControlRecursive(Spatial spatial, Class<T> controlClass) {
        Object control = spatial.getControl((Class) controlClass);
        if (control != null) {
            return (T) control;
        }
        if (spatial instanceof Node) {
            for (Spatial child : ((Node) spatial).getChildren()) {
                T found = findControlRecursive(child, controlClass);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
