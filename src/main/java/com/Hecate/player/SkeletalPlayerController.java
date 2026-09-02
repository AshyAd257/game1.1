package com.Hecate.player;

import com.jme3.animation.*;
import com.jme3.app.SimpleApplication;
import com.jme3.asset.AssetManager;
import com.jme3.math.Vector3f;
import com.jme3.math.Quaternion;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.material.Material;
import com.jme3.texture.Texture;

import java.util.Collection;

/**
 * 基于3D骨骼模型的玩家控制器
 * 加载从Blender导出的glTF/glb模型和动画
 */
public class SkeletalPlayerController {

    private final SimpleApplication app;
    private final AssetManager assetManager;
    private final Node rootNode;

    // 模型和动画
    private Node characterNode;           // 角色模型根节点
    private Spatial characterModel;       // 加载的模型
    private AnimControl animControl;      // 动画控制器
    private AnimChannel animChannel;      // 动画通道
    private SkeletonControl skeletonControl; // 骨骼控制器

    // 位置和状态
    private Vector3f position;
    private float yaw = 0f;               // 角色朝向（弧度）
    private boolean isWalking = false;
    private boolean isJumping = false;

    // 资源路径
    private static final String MODEL_PATH = "mesh/armlegmesh.glb";
    private static final String TEXTURE_PATH = "textures/armlegs/armlegs.png";

    // 动画路径
    private static final String ANIM_BREATHE = "movement/breathe.glb";
    private static final String ANIM_WALK = "movement/walk.glb";
    private static final String ANIM_IDLE_JUMP = "movement/idlejump.glb";
    private static final String ANIM_HOLD_GUN_IDLE = "movement/holdgunidle.glb";
    private static final String ANIM_HOLD_GUN_JUMP = "movement/holdgunjump.glb";

    // 缩放和偏移
    private static final float MODEL_SCALE = 0.5f;  // 模型缩放
    private static final float MODEL_Y_OFFSET = 0.0f; // Y轴偏移

    // 当前动画名称
    private String currentAnimation = null;

    public SkeletalPlayerController(SimpleApplication app, Vector3f startPosition) {
        this.app = app;
        this.assetManager = app.getAssetManager();
        this.rootNode = app.getRootNode();
        this.position = startPosition.clone();

        initializeCharacterModel();
    }

    /**
     * 初始化角色模型
     */
    private void initializeCharacterModel() {
        try {
            // 创建角色根节点
            characterNode = new Node("SkeletalPlayer");
            characterNode.setLocalTranslation(position);
            characterNode.setLocalScale(MODEL_SCALE);

            // 加载基础模型（mesh + skeleton）
            characterModel = assetManager.loadModel(MODEL_PATH);

            // 应用贴图
            applyTexture(characterModel, TEXTURE_PATH);

            // 获取动画控制器
            animControl = characterModel.getControl(AnimControl.class);
            skeletonControl = characterModel.getControl(SkeletonControl.class);

            if (animControl != null) {
                animChannel = animControl.createChannel();
                System.out.println("[SkeletalPlayer] AnimControl found");

                // 打印模型自带的动画（通常mesh glb里没有动画，只有骨架）
                Collection<String> animNames = animControl.getAnimationNames();
                System.out.println("[SkeletalPlayer] Model animations: " + animNames);
            } else {
                System.out.println("[SkeletalPlayer] No AnimControl in base model");
            }

            if (skeletonControl != null) {
                System.out.println("[SkeletalPlayer] SkeletonControl found");
            }

            // 附加模型到角色节点
            characterNode.attachChild(characterModel);

            // 附加到场景
            rootNode.attachChild(characterNode);

            // 加载并播放默认动画（呼吸/待机）
            loadAndPlayAnimation(ANIM_BREATHE, true);

            System.out.println("[SkeletalPlayer] Character model initialized at " + position);

        } catch (Exception e) {
            System.err.println("[SkeletalPlayer] Failed to initialize character model: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 应用贴图到模型
     */
    private void applyTexture(Spatial model, String texturePath) {
        try {
            Texture texture = assetManager.loadTexture(texturePath);
            texture.setMagFilter(Texture.MagFilter.Nearest);
            texture.setMinFilter(Texture.MinFilter.NearestNoMipMaps);

            // 递归应用贴图到所有几何体
            model.depthFirstTraversal(spatial -> {
                if (spatial instanceof com.jme3.scene.Geometry) {
                    com.jme3.scene.Geometry geom = (com.jme3.scene.Geometry) spatial;
                    Material mat = geom.getMaterial();

                    // 如果没有材质，创建一个
                    if (mat == null) {
                        mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
                        geom.setMaterial(mat);
                    }

                    // 设置漫反射贴图
                    mat.setTexture("DiffuseMap", texture);

                    // 设置材质颜色为白色（让贴图原色显示）
                    mat.setBoolean("UseMaterialColors", true);
                    mat.setColor("Diffuse", com.jme3.math.ColorRGBA.White);
                    mat.setColor("Ambient", com.jme3.math.ColorRGBA.White);
                }
            });

            System.out.println("[SkeletalPlayer] Texture applied: " + texturePath);

        } catch (Exception e) {
            System.err.println("[SkeletalPlayer] Failed to apply texture: " + e.getMessage());
        }
    }

    /**
     * 加载并播放动画
     * @param animPath 动画文件路径
     * @param loop 是否循环播放
     */
    private void loadAndPlayAnimation(String animPath, boolean loop) {
        try {
            // 加载动画文件（包含动画数据的glb）
            Spatial animModel = assetManager.loadModel(animPath);
            AnimControl animAnimControl = animModel.getControl(AnimControl.class);

            if (animAnimControl == null) {
                System.err.println("[SkeletalPlayer] No AnimControl in: " + animPath);
                return;
            }

            // 获取动画名称（通常glb文件里只有一个动画）
            Collection<String> animNames = animAnimControl.getAnimationNames();
            if (animNames.isEmpty()) {
                System.err.println("[SkeletalPlayer] No animations in: " + animPath);
                return;
            }

            String animName = animNames.iterator().next();
            System.out.println("[SkeletalPlayer] Loading animation '" + animName + "' from " + animPath);

            // 获取动画数据
            Animation anim = animAnimControl.getAnim(animName);

            if (animControl == null) {
                System.err.println("[SkeletalPlayer] Character AnimControl not initialized");
                return;
            }

            // 将动画添加到角色的AnimControl中
            // 使用文件路径作为动画名称（避免乱码问题）
            String cleanAnimName = animPath.replace("movement/", "").replace(".glb", "");

            // 创建一个新的Animation对象，用clean的名称
            Animation renamedAnim = new Animation(cleanAnimName, anim.getLength());
            // 复制所有track
            for (int i = 0; i < anim.getTracks().length; i++) {
                renamedAnim.addTrack(anim.getTracks()[i]);
            }

            animControl.addAnim(renamedAnim);

            // 播放动画
            if (animChannel != null) {
                animChannel.setAnim(cleanAnimName);
                animChannel.setLoopMode(loop ? LoopMode.Loop : LoopMode.DontLoop);
                animChannel.setSpeed(1.0f);
                currentAnimation = cleanAnimName;
                System.out.println("[SkeletalPlayer] Playing animation: " + cleanAnimName + " (loop=" + loop + ")");
            }

        } catch (Exception e) {
            System.err.println("[SkeletalPlayer] Failed to load animation from " + animPath + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 播放指定动画（如果已加载）
     */
    private void playAnimation(String animName, boolean loop) {
        if (animChannel != null && animControl != null) {
            if (animControl.getAnimationNames().contains(animName)) {
                animChannel.setAnim(animName);
                animChannel.setLoopMode(loop ? LoopMode.Loop : LoopMode.DontLoop);
                currentAnimation = animName;
            }
        }
    }

    /**
     * 更新（每帧调用）
     */
    public void update(float tpf) {
        // 更新位置
        if (characterNode != null) {
            characterNode.setLocalTranslation(position.x, position.y + MODEL_Y_OFFSET, position.z);

            // 更新旋转（绕Y轴）
            Quaternion rotation = new Quaternion();
            rotation.fromAngleAxis(yaw, Vector3f.UNIT_Y);
            characterNode.setLocalRotation(rotation);
        }

        // 根据状态切换动画
        updateAnimation();
    }

    /**
     * 根据当前状态更新动画
     */
    private void updateAnimation() {
        String targetAnim = null;

        if (isJumping) {
            targetAnim = "idlejump";
        } else if (isWalking) {
            targetAnim = "walk";
        } else {
            targetAnim = "breathe";  // 默认待机动画
        }

        // 只在动画改变时切换
        if (targetAnim != null && !targetAnim.equals(currentAnimation)) {
            if (animControl != null && animControl.getAnimationNames().contains(targetAnim)) {
                playAnimation(targetAnim, true);
            } else {
                // 如果目标动画还没加载，尝试加载
                String animPath = "movement/" + targetAnim + ".glb";
                loadAndPlayAnimation(animPath, true);
            }
        }
    }

    /**
     * 设置位置
     */
    public void setPosition(Vector3f position) {
        this.position.set(position);
    }

    /**
     * 获取位置
     */
    public Vector3f getPosition() {
        return position.clone();
    }

    /**
     * 设置旋转（弧度）
     */
    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    /**
     * 获取旋转（弧度）
     */
    public float getYaw() {
        return yaw;
    }

    /**
     * 设置行走状态
     */
    public void setWalking(boolean walking) {
        this.isWalking = walking;
    }

    /**
     * 设置跳跃状态
     */
    public void setJumping(boolean jumping) {
        this.isJumping = jumping;
    }

    /**
     * 获取角色节点（用于相机跟随等）
     */
    public Node getCharacterNode() {
        return characterNode;
    }

    /**
     * 获取骨骼控制器（用于挂载武器等）
     */
    public SkeletonControl getSkeletonControl() {
        return skeletonControl;
    }

    /**
     * 清理资源
     */
    public void cleanup() {
        if (characterNode != null) {
            characterNode.removeFromParent();
        }
    }
}
