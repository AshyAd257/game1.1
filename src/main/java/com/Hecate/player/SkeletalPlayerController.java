package com.Hecate.player;

import com.jme3.anim.AnimClip;
import com.jme3.anim.AnimComposer;
import com.jme3.anim.AnimTrack;
import com.jme3.anim.Armature;
import com.jme3.anim.Joint;
import com.jme3.anim.SkinningControl;
import com.jme3.anim.TransformTrack;
import com.jme3.anim.util.HasLocalTransform;
import com.jme3.app.SimpleApplication;
import com.jme3.asset.AssetManager;
import com.jme3.math.Vector3f;
import com.jme3.math.Quaternion;
import com.jme3.math.FastMath;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.Geometry;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.texture.Texture;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;
import java.nio.FloatBuffer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 基于3D骨骼模型的玩家控制器
 * 加载从Blender导出的glTF/glb模型和动画
 */
public class SkeletalPlayerController {

    private final SimpleApplication app;
    private final AssetManager assetManager;
    private final Node rootNode;

    // 模型和动画
    // jME 3.5.2的glTF/glb加载器（GltfLoader）用的是新动画系统（com.jme3.anim包：
    // AnimComposer+SkinningControl+Armature/Joint），不会挂旧系统的AnimControl/
    // SkeletonControl/Skeleton/Bone——用旧API去getControl()永远拿到null，这也是
    // 之前动画完全不播放、模型卡在A-pose的根本原因。
    private Node characterNode;           // 角色模型根节点
    private Spatial characterModel;       // 加载的模型
    private AnimComposer animComposer;    // 动画播放器（新API）
    private SkinningControl skinningControl; // 骨骼蒙皮控制器（新API）

    // 位置和状态
    private Vector3f position;
    private float yaw = 0f;               // 角色朝向（弧度）
    private boolean isWalking = false;
    private boolean isJumping = false;

    // 资源路径
    private static final String MODEL_PATH = "mesh/armlegmesh.glb";
    private static final String TEXTURE_PATH = "textures/armlegs/armlegs.png"; // 修复：使用小写路径（与jME AssetManager一致）

    // 动画路径
    private static final String ANIM_BREATHE = "movement/breathe.glb";
    private static final String ANIM_WALK = "movement/walk.glb";
    private static final String ANIM_IDLE_JUMP = "movement/idlejump.glb";
    private static final String ANIM_HOLD_GUN_IDLE = "movement/holdgunidle.glb";
    private static final String ANIM_HOLD_GUN_JUMP = "movement/holdgunjump.glb";

    // 已加载的动画名称集合（避免重复加载）
    private Set<String> loadedAnimations = new HashSet<>();

    // 缩放和偏移
    private static final float MODEL_SCALE = 0.1f;  // 缩小5倍（0.5 -> 0.1）
    private static final float MODEL_Y_OFFSET = 1.63f; // Y轴偏移：模型最低点-16.3174 * 0.1 = -1.63，需要上移1.63让脚底对齐地面
    private static final float MODEL_ROTATION_OFFSET = -FastMath.HALF_PI; // -90度旋转偏移，修正Blender坐标系

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

            System.out.println("[SkeletalPlayer] Model loaded: " + MODEL_PATH);

            // 应用贴图
            applyTexture(characterModel, TEXTURE_PATH);

            // 获取动画控制器（新API）。GltfLoader可能把控件挂在返回的根节点上，
            // 也可能挂在某个子节点上（取决于骨架/网格在glTF场景图里的具体结构），
            // 用递归查找而不是只查characterModel自身，避免因为挂载位置猜错而拿到null。
            animComposer = findControlRecursive(characterModel, AnimComposer.class);
            skinningControl = findControlRecursive(characterModel, SkinningControl.class);

            if (animComposer != null) {
                System.out.println("[SkeletalPlayer] AnimComposer found. Existing clips: "
                        + animComposer.getAnimClipsNames());
            } else {
                System.out.println("[SkeletalPlayer] No AnimComposer in base model (animations will not play)");
            }

            if (skinningControl != null) {
                System.out.println("[SkeletalPlayer] SkinningControl found, joint count: "
                        + skinningControl.getArmature().getJointCount());
            } else {
                System.out.println("[SkeletalPlayer] No SkinningControl in base model");
            }

            // 附加模型到角色节点
            characterNode.attachChild(characterModel);

            // 附加到场景
            rootNode.attachChild(characterNode);

            // 一次性预加载所有动画文件（避免updateAnimation()每帧重复加载），
            // 加载失败或动画数据为空的文件会被跳过，不影响其他动画正常播放。
            preloadAllAnimations();

            // 默认播放待机动画（如果预加载成功）
            if (loadedAnimations.contains(cleanAnimName(ANIM_BREATHE))) {
                playAnimation(cleanAnimName(ANIM_BREATHE), true);
            }

            System.out.println("[SkeletalPlayer] Character model initialized at " + position);

        } catch (Exception e) {
            System.err.println("[SkeletalPlayer] Failed to initialize character model: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 从动画文件路径生成清理后的动画名（用作AnimComposer内部key）
     */
    private String cleanAnimName(String animPath) {
        return animPath.replace("movement/", "").replace(".glb", "");
    }

    /**
     * 递归查找Spatial场景图里挂载的某种Control。GltfLoader不保证把Control
     * 挂在loadModel()直接返回的那个节点上，可能挂在更深的子节点上，所以不能
     * 只调用一次spatial.getControl()了事。
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

    /**
     * 一次性预加载所有动画文件。文件损坏/为空/加载失败的会跳过并记录日志，
     * 不影响其他动画的正常加载。成功加载的动画名会记录到loadedAnimations集合。
     */
    private void preloadAllAnimations() {
        String[] animPaths = {
            ANIM_BREATHE,
            ANIM_WALK,
            ANIM_IDLE_JUMP,
            ANIM_HOLD_GUN_IDLE,
            ANIM_HOLD_GUN_JUMP
        };

        System.out.println("[SkeletalPlayer] Preloading " + animPaths.length + " animation files...");

        for (String animPath : animPaths) {
            loadAnimationFile(animPath);
        }

        System.out.println("[SkeletalPlayer] Animation preload complete. Loaded: " + loadedAnimations);
    }

    /**
     * 从glb文件加载单个动画并注册到角色自己的AnimComposer上。加载失败/动画数据
     * 为空/骨骼名对不上时静默跳过（记录日志），不影响其他动画正常加载。
     *
     * 关键点：动画glb里的TransformTrack.target指向的是"这个动画文件自己
     * loadModel()出来的那一套Joint对象"，跟角色本体骨架的Joint是完全不同的
     * 实例（哪怕骨骼名字相同）。如果不做处理直接把track塞进角色的AnimComposer，
     * 动画会去驱动一套没人看得到的孤立骨架，角色本体骨架纹丝不动——这正是从
     * 旧版AnimControl（按骨骼名/索引在运行时应用）迁移到新版AnimComposer
     * （track直接持有目标对象引用）时最容易踩的坑。这里按骨骼名把每条轨道
     * 重新绑定（retarget）到角色自己的Joint上，再组成新的AnimClip。
     */
    private void loadAnimationFile(String animPath) {
        try {
            System.out.println("[SkeletalPlayer] Loading animation: " + animPath);

            Spatial animModel = assetManager.loadModel(animPath);
            AnimComposer sourceComposer = findControlRecursive(animModel, AnimComposer.class);

            if (sourceComposer == null) {
                System.err.println("[SkeletalPlayer] No AnimComposer in: " + animPath + " (skipping)");
                return;
            }

            Set<String> clipNames = sourceComposer.getAnimClipsNames();
            if (clipNames.isEmpty()) {
                System.err.println("[SkeletalPlayer] No animation clips in: " + animPath + " (file may be empty/corrupt, skipping)");
                return;
            }

            String sourceClipName = clipNames.iterator().next();
            AnimClip sourceClip = sourceComposer.getAnimClip(sourceClipName);
            System.out.println("[SkeletalPlayer] Found clip: '" + sourceClipName + "' in " + animPath);

            if (animComposer == null || skinningControl == null) {
                System.err.println("[SkeletalPlayer] Character AnimComposer/SkinningControl not initialized (skipping)");
                return;
            }

            Armature characterArmature = skinningControl.getArmature();

            List<AnimTrack> retargetedTracks = new ArrayList<>();
            int matchedCount = 0;
            int skippedCount = 0;
            for (AnimTrack track : sourceClip.getTracks()) {
                if (!(track instanceof TransformTrack)) {
                    // MorphTrack等以几何体为目标的轨道同样存在"目标是源文件自己的
                    // 几何体"问题，当前这批移动动画都不含形变轨道，直接跳过。
                    continue;
                }
                TransformTrack transformTrack = (TransformTrack) track;
                HasLocalTransform originalTarget = transformTrack.getTarget();
                if (!(originalTarget instanceof Joint)) {
                    continue;
                }
                String jointName = ((Joint) originalTarget).getName();
                Joint characterJoint = characterArmature.getJoint(jointName);
                if (characterJoint == null) {
                    skippedCount++;
                    continue;
                }
                retargetedTracks.add(new TransformTrack(
                        characterJoint,
                        transformTrack.getTimes(),
                        transformTrack.getTranslations(),
                        transformTrack.getRotations(),
                        transformTrack.getScales()));
                matchedCount++;
            }

            if (retargetedTracks.isEmpty()) {
                System.err.println("[SkeletalPlayer] No matching joints between " + animPath
                        + " and character armature (skipping)");
                return;
            }

            String cleanName = cleanAnimName(animPath);
            AnimClip retargetedClip = new AnimClip(cleanName);
            retargetedClip.setTracks(retargetedTracks.toArray(new AnimTrack[0]));

            animComposer.addAnimClip(retargetedClip);
            loadedAnimations.add(cleanName);

            System.out.println("[SkeletalPlayer] Animation loaded and registered: " + cleanName
                    + " (matched joints=" + matchedCount + ", skipped=" + skippedCount + ")");

        } catch (Exception e) {
            System.err.println("[SkeletalPlayer] Failed to load animation from " + animPath + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 应用贴图到模型
     */
    private void applyTexture(Spatial model, String texturePath) {
        try {
            System.out.println("[SkeletalPlayer] Loading texture from: " + texturePath);
            Texture texture = assetManager.loadTexture(texturePath);

            // 像素艺术风格的纹理设置
            texture.setMagFilter(Texture.MagFilter.Nearest);
            texture.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
            // Repeat模式下UV浮点误差（如0.9999->1.0001）会绕到贴图另一侧采样，
            // 在UV接缝处出现错误色块/透明块。这是单张贴图集，不需要平铺，改用EdgeClamp。
            texture.setWrap(Texture.WrapMode.EdgeClamp);

            System.out.println("[SkeletalPlayer] Texture loaded successfully");
            System.out.println("[SkeletalPlayer] Texture size: " + texture.getImage().getWidth() + "x" + texture.getImage().getHeight());

            // 递归遍历所有几何体
            applyTextureRecursive(model, texture);

            System.out.println("[SkeletalPlayer] Texture application completed");

        } catch (Exception e) {
            System.err.println("[SkeletalPlayer] Failed to load/apply texture: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 递归应用贴图到节点下所有几何体
     */
    private void applyTextureRecursive(Spatial spatial, Texture texture) {
        if (spatial instanceof Geometry) {
            Geometry geom = (Geometry) spatial;

            // 修复UV坐标（OpenGL的V轴与Blender相反）
            fixUVCoordinates(geom);

            // 低模方块拼接的模型，法线在拼接处不连续（部分三角形的存储法线和实际
            // 几何朝向对不上），用受光照影响的材质（Lighting.j3md）会在特定视角下
            // 让某些面算出接近全黑的光照结果。这个模型本来就没有做真正意义上的
            // 光影（贴图是手绘的、法线数据也不可靠），统一换成不受光照影响的
            // Unshaded材质，直接显示贴图颜色本身，从根源上跳过法线/光照计算。
            Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
            mat.setTexture("ColorMap", texture);
            // 贴图集里UV岛之间的填充区是透明黑色(RGBA全0)。不开alpha测试/混合的话，
            // GPU会忽略贴图的alpha通道只画RGB，把这些本该透明的填充区画成实心黑块——
            // 表现为UV接缝处的黑边、以及UV轻微越界踩到填充区时的"贴图丢失"变黑。
            mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
            mat.setFloat("AlphaDiscardThreshold", 0.1f);
            geom.setMaterial(mat);
            geom.setQueueBucket(com.jme3.renderer.queue.RenderQueue.Bucket.Transparent);
            System.out.println("[SkeletalPlayer] Unshaded material applied to: " + geom.getName());

        } else if (spatial instanceof Node) {
            Node node = (Node) spatial;
            for (Spatial child : node.getChildren()) {
                applyTextureRecursive(child, texture);
            }
        }
    }

    /**
     * 修复UV坐标（翻转V轴）
     * Blender使用的UV坐标系与OpenGL/jME不同，需要翻转V轴
     */
    private void fixUVCoordinates(Geometry geom) {
        if (geom.getMesh() == null) return;

        VertexBuffer uvBuffer = geom.getMesh().getBuffer(VertexBuffer.Type.TexCoord);
        if (uvBuffer == null) {
            System.out.println("[SkeletalPlayer] No UV buffer found for: " + geom.getName());
            return;
        }

        FloatBuffer uvData = (FloatBuffer) uvBuffer.getData();
        uvData.rewind();

        // 创建新的UV数据缓冲区
        FloatBuffer newUvData = BufferUtils.createFloatBuffer(uvData.capacity());

        // 翻转V坐标（1.0 - v）
        while (uvData.hasRemaining()) {
            float u = uvData.get();
            float v = uvData.get();
            newUvData.put(u);
            newUvData.put(1.0f - v);  // 翻转V轴
        }

        newUvData.flip();
        uvBuffer.updateData(newUvData);

        System.out.println("[SkeletalPlayer] UV coordinates flipped for: " + geom.getName());
    }

    /**
     * 播放指定动画（仅当该动画已成功预加载时才播放；未加载/加载失败的动画
     * 直接跳过，不会触发任何加载尝试——加载只在preloadAllAnimations()里做一次）。
     * 新API下没有单独的"循环模式"设置：AnimComposer的AnimLayer在
     * interpolate()里对超出片段长度的time做取模，效果等同旧API的LoopMode.Loop，
     * 所以loop参数目前对新API无效，只是保留调用方语义、暂不区分播一次/循环。
     */
    private void playAnimation(String animName, boolean loop) {
        if (animComposer == null) {
            return;
        }
        if (!loadedAnimations.contains(animName)) {
            // 动画未成功加载（文件损坏/为空/加载失败），不播放
            return;
        }
        if (animName.equals(currentAnimation)) {
            return;
        }
        animComposer.setCurrentAction(animName);
        currentAnimation = animName;
        System.out.println("[SkeletalPlayer] Playing animation: " + animName + " (loop=" + loop + ")");
    }

    /**
     * 更新（每帧调用）
     */
    public void update(float tpf) {
        // 更新位置
        if (characterNode != null) {
            characterNode.setLocalTranslation(position.x, position.y + MODEL_Y_OFFSET, position.z);

            // 设置模型旋转（绕Y轴），添加90度偏移修正Blender坐标系
            Quaternion rotation = new Quaternion();
            rotation.fromAngleAxis(yaw + MODEL_ROTATION_OFFSET, Vector3f.UNIT_Y);
            characterNode.setLocalRotation(rotation);

            // 设置统一缩放，不做左右翻转，保持左右手位置正确
            characterNode.setLocalScale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);

            // 调试输出
            if (frameCount % 60 == 0) {  // 每秒输出一次
                System.out.println("[SkeletalPlayer] 模型旋转角度 yaw=" + (yaw * FastMath.RAD_TO_DEG) +
                        "°, 实际旋转=" + ((yaw + MODEL_ROTATION_OFFSET) * FastMath.RAD_TO_DEG) + "°");
            }
        }

        // 根据状态切换动画
        updateAnimation();
        frameCount++;
    }

    private int frameCount = 0;

    /**
     * 根据当前状态切换动画。所有动画已在initializeCharacterModel()里通过
     * preloadAllAnimations()一次性加载完毕，这里只做切换，不做任何加载尝试——
     * 如果目标动画因文件损坏/为空未能加载成功，playAnimation()会直接跳过，
     * 保持当前动画继续播放，不会每帧反复尝试加载。
     */
    private void updateAnimation() {
        String targetAnim;
        if (isJumping) {
            targetAnim = cleanAnimName(ANIM_IDLE_JUMP);
        } else if (isWalking) {
            targetAnim = cleanAnimName(ANIM_WALK);
        } else {
            targetAnim = cleanAnimName(ANIM_BREATHE);
        }

        playAnimation(targetAnim, true);
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
     * 获取骨骼蒙皮控制器（用于挂载武器等）
     */
    public SkinningControl getSkinningControl() {
        return skinningControl;
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
