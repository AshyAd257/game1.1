package com.Hecate.puppet.editor;

import com.jme3.app.SimpleApplication;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.input.RawInputListener;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.renderer.RenderManager;
import com.jme3.system.AppSettings;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.shape.Sphere;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
// import com.Hecate.puppet.PuppetTestScene; // 使用编辑器专用版本
import com.Hecate.puppet.editor.core.EditorPuppetPartRenderer;
import com.Hecate.puppet.editor.core.EditorBone;
import com.Hecate.puppet.editor.core.EditorBoneGroup;
import com.Hecate.puppet.editor.core.EditorGroupManager;
import com.Hecate.puppet.editor.core.EditorPuppetRenderer;
import com.Hecate.puppet.editor.core.EditorSkeleton;
import com.Hecate.puppet.editor.animation.EditorAnimationPlayer;
import com.Hecate.puppet.animation.AnimationClip;
import com.Hecate.puppet.animation.Keyframe;
import com.Hecate.puppet.config.PuppetConfig;
import com.Hecate.puppet.config.PuppetIO;
import com.Hecate.puppet.editor.command.*;
import com.Hecate.puppet.export.*;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.DisplayMode;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.util.List;

/**
 * 独立的木偶编辑器窗口
 * 在独立窗口中显示木偶和编辑器UI，支持实时编辑
 */
public class PuppetEditorApp extends SimpleApplication {

    private boolean volumeMode;
    private VolumeViewportTools volumeTools;
    private boolean volumeBonesVisible=true;
    public boolean isVolumeMode() { return volumeMode; }
    private VolumeWorkbench volumeWorkbench;
    private boolean openVolumeWorkbenchOnStart;
    public void setOpenVolumeWorkbenchOnStart(boolean value) { openVolumeWorkbenchOnStart=value; }
    private EditorPuppetTestScene puppetTestScene;
    private PuppetEditorUI editorUI;
    private float animationTime = 0f;

    // 动画系统
    private EditorAnimationPlayer animationPlayer;
    private AnimationClip currentClip;
    private java.util.List<Keyframe> copiedKeyframes = new java.util.ArrayList<>();  // 关键帧剪贴板

    // 当前选中的骨骼
    private int selectedBoneIndex = 0;
    private EditorBone[] allBones;
    private EditorBone selectedBone;  // 当前选中的骨骼引用

    // 调整步长
    private final float SIZE_ADJUST_STEP = 0.1f;

    // 鼠标输入监听器
    private RawInputListener mouseListener;

    // 相机控制
    private boolean volumeOrthographic;
    private float cameraDistance = 15f;
    private float cameraAngleX = 0f;
    private float cameraAngleY = 0f;
    private boolean isDraggingCamera = false;
    private boolean isShiftPanningCamera;
    private int shiftPanStartX,shiftPanStartY;
    private float shiftPanDepth;
    private Vector3f shiftPanStartPivot;
    private com.jme3.renderer.Camera shiftPanCamera;
    private boolean isPanningCamera = false;  // 右键平移模式
    private int lastMouseX = 0;
    private int lastMouseY = 0;
    private int rightClickStartX = 0;
    private int rightClickStartY = 0;
    private boolean hasMouseMoved = false;

    // 相机旋转中心点（默认为(0, 5, 0)）
    private Vector3f cameraPivotPoint = new Vector3f(0, 5, 0);

    // 方向指示球体
    private Geometry directionSphere;

    // 当前摄像机方向
    private EditorBone.Direction currentCameraDirection = EditorBone.Direction.FRONT;

    // 隐藏模式
    private boolean isHideModeEnabled = false;
    private boolean isDeleteModeEnabled = false;
    private java.util.Set<String> hiddenParts = new java.util.HashSet<>();

    // 当前文件路径
    private String currentFilePath = null;

    // 父子关系设置模式
    private boolean waitingForParentSelect = false;  // 等待选择新的父节点
    private EditorBone pendingChildBone = null;  // 等待设置父节点的子骨骼
    private boolean isAddingFreeBone = false;  // 是否正在添加自由骨骼

    // 初始化标志 - 防止启动时的意外鼠标事件
    private boolean fullyInitialized = false;
    private float initializationDelay = 0f;

    // 命令管理器（撤销/重做）
    private CommandManager commandManager;

    // Ctrl键状态追踪（用于快捷键）
    private boolean isCtrlPressed = false;

    // Shift和Tab键状态追踪（用于多选和镜像功能）
    private boolean isShiftPressed = false;
    private boolean isTabPressed = false;

    // 多选功能
    private java.util.Set<EditorBone> selectedBones = new java.util.LinkedHashSet<>();

    // 镜像管理器
    private MirrorManager mirrorManager;

    // 镜像配对模式
    private boolean mirrorPairingMode = false;  // Shift+Tab 镜像配对模式
    private EditorBone firstMirrorBone = null;  // 第一个选中的镜像骨骼

    // 禁用wave动画标志（当从编辑器返回时）
    private boolean disableWaveAnimation = false;

    // 关键帧类型选择（默认为插值关键帧）
    private com.Hecate.puppet.animation.Keyframe.KeyframeType currentKeyframeType =
            com.Hecate.puppet.animation.Keyframe.KeyframeType.INTERPOLATED;

    public static void main(String[] args) {
        PuppetEditorApp app = createEditor();
        app.start();
    }

    public PuppetEditorApp() {
        super();
        // Keep the 3D preview live while the modeless workbench has keyboard focus.
        setPauseOnLostFocus(false);
    }

    @Override
    public void simpleInitApp() {
        // 注册每个磁盘根目录（C:\、D:\...）为FileLocator，支持加载resources目录之外
        // 任意位置的贴图文件。convertToResourcePath()会把绝对路径转成"去掉盘符"的
        // 相对片段（如"Users/xxx/Downloads/foo.png"），配合这里注册的盘符根定位器即可解析。
        for (File root : File.listRoots()) {
            String rootPath = root.getAbsolutePath().replace('\\', '/');
            assetManager.registerLocator(rootPath, com.jme3.asset.plugins.FileLocator.class);
        }

        // 设置背景颜色
        viewPort.setBackgroundColor(new ColorRGBA(0.3f, 0.3f, 0.35f, 1.0f));

        // 禁用调试信息显示
        setDisplayFps(false);
        setDisplayStatView(false);

        // 禁用默认的FlyCam（WASD控制）
        flyCam.setEnabled(false);

        // 设置相机初始位置
        updateCameraPosition();

        // 添加光照（已禁用 - 避免光影影响贴图查看）
        // setupLights();

        // 创建木偶测试场景
        puppetTestScene = new EditorPuppetTestScene(this);
        puppetTestScene.createTestPuppet(rootNode, new Vector3f(0, 5, 0));

        // 设置Billboard渲染模式
        // DISABLED - 部件保持固定朝向，适合3D立体模型（立方体等）
        // UNIFIED - 整个木偶像纸人一样整体朝向摄像机（适合2D精灵）- 默认模式
        // INDEPENDENT - 每个部件独立朝向摄像机（不推荐，会导致诡异效果）
        puppetTestScene.getPuppetRenderer().setBillboardMode(EditorPuppetRenderer.BillboardMode.UNIFIED);

        if (volumeMode) initializeVolumeModeParts(true);
        // 收集所有骨骼
        collectAllBones();

        // 初始化命令管理器
        commandManager = new CommandManager();

        // 初始化镜像管理器
        mirrorManager = new MirrorManager();

        // 创建编辑器UI（必须先创建，因为动画系统需要访问Timeline）
        editorUI = new PuppetEditorUI(this);
        editorUI.setSkeleton(puppetTestScene.getTestSkeleton());
        if(volumeMode)volumeTools=new VolumeViewportTools(this);

        // 设置 PartListPanel 的 MirrorManager 引用
        editorUI.getPartListPanel().setMirrorManager(mirrorManager);

        // 设置 InspectorPanel 的 selectedBones 引用（用于多选编辑）
        // TODO: InspectorPanel需要被添加到PuppetEditorUI中
        // editorUI.getInspectorPanel().setSelectedBones(selectedBones);

        // 设置编辑器回调（在打开时重新设置按钮和滑条回调）
        editorUI.setEditorCallbacks(new PuppetEditorUI.EditorCallbacks() {
            @Override
            public void onEditorOpened() {
                setupButtonCallbacks();
                setupSliderCallbacks();
                setupPartListCallbacks();
                setupGroupControlCallbacks();
                setupTimelineCallbacks();
            }

            @Override
            public void onBackButtonClicked() {
                // 关闭编辑器窗口，返回到主游戏窗口
                stop();  // 关闭编辑器应用程序（独立窗口）
            }

            @Override
            public void onExitButtonClicked() {
                // 退出整个应用程序（包括主游戏）

                // 关闭编辑器窗口
                stop();

                // 通过System.exit强制退出整个JVM（包括主游戏窗口）
                System.exit(0);
            }
        });

        // 初始设置回调
        setupButtonCallbacks();
        setupSliderCallbacks();
        setupPartListCallbacks();
        setupGroupControlCallbacks();
        setupTimelineCallbacks();

        editorUI.setVisible(true);

        // 初始化动画系统（在UI创建之后）
        initializeAnimationSystem();

        // 选择第一个骨骼（Body）
        if(allBones.length>0)selectBone(0);

        // 添加键盘控制
        setupKeyBindings();

        // 添加鼠标输入监听
        setupMouseListener();

        // 创建线框球体（位于屏幕左上方）
        createWireframeSphere();
        if (openVolumeWorkbenchOnStart) openVolumeWorkbench();
    }

    /**
     * 初始化动画系统
     */
    private void initializeAnimationSystem() {
        if (puppetTestScene == null || puppetTestScene.getTestSkeleton() == null) {
            return;
        }

        // 创建动画片段
        currentClip = new AnimationClip("TestClip");
        currentClip.setLooping(true);

        // 创建动画播放器
        animationPlayer = new EditorAnimationPlayer(
            puppetTestScene.getTestSkeleton(),
            puppetTestScene.getPuppetRenderer()
        );

        // 设置动画片段但不自动播放（默认暂停状态）
        animationPlayer.setCurrentClip(currentClip);
        animationPlayer.pause();

        // 将动画片段设置到Timeline
        if (editorUI != null && editorUI.getTimelinePanel() != null) {
            editorUI.getTimelinePanel().setAnimationClip(currentClip);
        } else {
        }

        // 将动画播放器设置到AnimationLayerPanel
        if (editorUI != null && editorUI.getAnimationLayerPanel() != null) {
            editorUI.getAnimationLayerPanel().setAnimationPlayer(animationPlayer);
        }
    }

    /**
     * 收集所有骨骼
     */
    private void collectAllBones() {
        if (puppetTestScene == null || puppetTestScene.getTestSkeleton() == null) {
            return;
        }

        java.util.List<EditorBone> boneList = puppetTestScene.getTestSkeleton().getAllBones();
        allBones = boneList.toArray(new EditorBone[0]);
    }

    /**
     * 创建彩色方向球体
     * 位于屏幕左上方，距离左边约1/6屏幕宽度
     * 添加到guiNode，使其不受相机旋转缩放影响
     * 不同方向显示不同颜色：前蓝/后深蓝、右红/左深红、上绿/下深绿
     */
    private void createWireframeSphere() {
        // 创建带顶点颜色的球体mesh
        Mesh sphereMesh = createColoredSphereMesh(50.0f, 32, 32);
        directionSphere = new Geometry("DirectionSphere", sphereMesh);

        // 使用支持顶点颜色的材质
        Material colorMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        colorMat.setBoolean("VertexColor", true);  // 启用顶点颜色
        colorMat.getAdditionalRenderState().setWireframe(true);  // 设置为线框模式
        colorMat.getAdditionalRenderState().setLineWidth(2.0f);  // 设置线宽
        colorMat.getAdditionalRenderState().setDepthTest(false);  // 禁用深度测试，始终显示在最前面
        colorMat.getAdditionalRenderState().setDepthWrite(false);  // 禁用深度写入

        directionSphere.setMaterial(colorMat);

        // 计算球体位置（使用屏幕坐标）
        float viewWidth = cam.getWidth();
        float viewHeight = cam.getHeight();
        float screenX = volumeMode ? 345 : viewWidth / 6.0f;  // 左边1/6位置
        float screenY = volumeMode ? viewHeight - 140 : viewHeight * 0.85f;  // 顶部附近（从底部算起85%高度处）

        directionSphere.setLocalTranslation(screenX, screenY, 0);

        // 添加到GUI节点（不受相机旋转缩放影响）
        guiNode.attachChild(directionSphere);
    }

    /**
     * 创建带顶点颜色的球体mesh
     * 根据顶点的方向分配颜色：
     * +X(右)=红色, -X(左)=深红色
     * +Y(上)=绿色, -Y(下)=深绿色
     * +Z(前)=蓝色, -Z(后)=深蓝色
     */
    private Mesh createColoredSphereMesh(float radius, int zSamples, int radialSamples) {
        Mesh mesh = new Mesh();

        int vertCount = (zSamples - 2) * radialSamples + 2;
        Vector3f[] vertices = new Vector3f[vertCount];
        ColorRGBA[] colors = new ColorRGBA[vertCount];

        // 生成顶点和颜色
        int vertIndex = 0;

        // 顶部顶点
        vertices[vertIndex] = new Vector3f(0, radius, 0);
        colors[vertIndex] = ColorRGBA.Green;  // 上=绿色
        vertIndex++;

        // 中间层顶点
        for (int z = 1; z < zSamples - 1; z++) {
            float theta = (float) (Math.PI * z / (zSamples - 1));
            float y = radius * (float) Math.cos(theta);
            float ringRadius = radius * (float) Math.sin(theta);

            for (int r = 0; r < radialSamples; r++) {
                float phi = (float) (2 * Math.PI * r / radialSamples);
                float x = ringRadius * (float) Math.cos(phi);
                float zCoord = ringRadius * (float) Math.sin(phi);

                vertices[vertIndex] = new Vector3f(x, y, zCoord);

                // 根据方向分配颜色（选择最主要的方向）
                float absX = Math.abs(x);
                float absY = Math.abs(y);
                float absZ = Math.abs(zCoord);

                if (absX >= absY && absX >= absZ) {
                    // X轴主导
                    colors[vertIndex] = x > 0 ? ColorRGBA.Red : new ColorRGBA(0.5f, 0, 0, 1);
                } else if (absY >= absX && absY >= absZ) {
                    // Y轴主导
                    colors[vertIndex] = y > 0 ? ColorRGBA.Green : new ColorRGBA(0, 0.5f, 0, 1);
                } else {
                    // Z轴主导
                    colors[vertIndex] = zCoord > 0 ? ColorRGBA.Blue : new ColorRGBA(0, 0, 0.5f, 1);
                }

                vertIndex++;
            }
        }

        // 底部顶点
        vertices[vertIndex] = new Vector3f(0, -radius, 0);
        colors[vertIndex] = new ColorRGBA(0, 0.5f, 0, 1);  // 下=深绿色

        // 设置顶点位置
        float[] posArray = new float[vertCount * 3];
        float[] colorArray = new float[vertCount * 4];
        for (int i = 0; i < vertCount; i++) {
            posArray[i * 3] = vertices[i].x;
            posArray[i * 3 + 1] = vertices[i].y;
            posArray[i * 3 + 2] = vertices[i].z;

            colorArray[i * 4] = colors[i].r;
            colorArray[i * 4 + 1] = colors[i].g;
            colorArray[i * 4 + 2] = colors[i].b;
            colorArray[i * 4 + 3] = colors[i].a;
        }

        mesh.setBuffer(com.jme3.scene.VertexBuffer.Type.Position, 3, posArray);
        mesh.setBuffer(com.jme3.scene.VertexBuffer.Type.Color, 4, colorArray);

        // 生成索引
        java.util.ArrayList<Integer> indices = new java.util.ArrayList<>();

        // 顶部三角形扇
        for (int r = 0; r < radialSamples; r++) {
            indices.add(0);
            indices.add(1 + r);
            indices.add(1 + (r + 1) % radialSamples);
        }

        // 中间四边形条带
        for (int z = 0; z < zSamples - 3; z++) {
            int row = 1 + z * radialSamples;
            int nextRow = row + radialSamples;

            for (int r = 0; r < radialSamples; r++) {
                int nextR = (r + 1) % radialSamples;

                indices.add(row + r);
                indices.add(nextRow + r);
                indices.add(row + nextR);

                indices.add(row + nextR);
                indices.add(nextRow + r);
                indices.add(nextRow + nextR);
            }
        }

        // 底部三角形扇
        int lastVertex = vertCount - 1;
        int lastRow = 1 + (zSamples - 3) * radialSamples;
        for (int r = 0; r < radialSamples; r++) {
            indices.add(lastVertex);
            indices.add(lastRow + (r + 1) % radialSamples);
            indices.add(lastRow + r);
        }

        // 转换为int数组
        int[] indexArray = new int[indices.size()];
        for (int i = 0; i < indices.size(); i++) {
            indexArray[i] = indices.get(i);
        }

        mesh.setBuffer(com.jme3.scene.VertexBuffer.Type.Index, 3, indexArray);
        mesh.updateBound();

        return mesh;
    }

    /**
     * 设置键盘绑定
     */
    private void setupKeyBindings() {
        // In volume mode Esc is handled here so an in-progress drag can consume it before exit.
        if(volumeMode&&inputManager.hasMapping(com.jme3.app.SimpleApplication.INPUT_MAPPING_EXIT))inputManager.deleteMapping(com.jme3.app.SimpleApplication.INPUT_MAPPING_EXIT);
        // ESC键关闭编辑器
        inputManager.addMapping("CloseEditor", new KeyTrigger(KeyInput.KEY_ESCAPE));

        // 动画控制
        inputManager.addMapping("RecordKeyframe", new KeyTrigger(KeyInput.KEY_K));
        inputManager.addMapping("TogglePlayback", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping("ToggleKeyframeType", new KeyTrigger(KeyInput.KEY_T));  // T键切换关键帧类型

        // 时间轴缩放（A放大，D缩小）
        inputManager.addMapping("ZoomInTimeline", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("ZoomOutTimeline", new KeyTrigger(KeyInput.KEY_D));

        // 全屏切换
        inputManager.addMapping("ToggleFullscreen", new KeyTrigger(KeyInput.KEY_F11));

        // Ctrl键追踪
        inputManager.addMapping("CtrlKey", new KeyTrigger(KeyInput.KEY_LCONTROL), new KeyTrigger(KeyInput.KEY_RCONTROL));

        // Shift和Tab键追踪（用于多选和镜像功能）
        inputManager.addMapping("ShiftKey", new KeyTrigger(KeyInput.KEY_LSHIFT), new KeyTrigger(KeyInput.KEY_RSHIFT));
        inputManager.addMapping("TabKey", new KeyTrigger(KeyInput.KEY_TAB));

        // 镜像轴选择（Shift+Tab+X/Y/Z）
        inputManager.addMapping("MirrorAxisX", new KeyTrigger(KeyInput.KEY_X));
        inputManager.addMapping("MirrorAxisY", new KeyTrigger(KeyInput.KEY_Y));
        inputManager.addMapping("MirrorAxisZ", new KeyTrigger(KeyInput.KEY_Z));

        // 撤销/重做
        inputManager.addMapping("Undo", new KeyTrigger(KeyInput.KEY_Z));
        inputManager.addMapping("Redo", new KeyTrigger(KeyInput.KEY_Y));

        // 复制/粘贴
        inputManager.addMapping("Copy", new KeyTrigger(KeyInput.KEY_C));
        inputManager.addMapping("Paste", new KeyTrigger(KeyInput.KEY_V));

        // 相机视角快捷键（Blender风格：Numpad 1/3/5/7 或方向键）
        inputManager.addMapping("ViewFront", new KeyTrigger(KeyInput.KEY_NUMPAD1), new KeyTrigger(KeyInput.KEY_UP));
        inputManager.addMapping("ViewLeft", new KeyTrigger(KeyInput.KEY_NUMPAD3), new KeyTrigger(KeyInput.KEY_LEFT));
        if(volumeMode)inputManager.addMapping("ViewRight",new KeyTrigger(KeyInput.KEY_RIGHT));
        else inputManager.addMapping("ViewRight",new KeyTrigger(KeyInput.KEY_NUMPAD5),new KeyTrigger(KeyInput.KEY_RIGHT));
        inputManager.addMapping("ToggleProjection",new KeyTrigger(KeyInput.KEY_NUMPAD5));
        inputManager.addMapping("ViewTop", new KeyTrigger(KeyInput.KEY_NUMPAD7), new KeyTrigger(KeyInput.KEY_DOWN));

        // Billboard模式切换（B键）
        inputManager.addMapping("ToggleBillboard", new KeyTrigger(KeyInput.KEY_B));

        // 修饰键状态追踪监听器（Ctrl, Shift, Tab）
        ActionListener modifierListener = new ActionListener() {
            @Override
            public void onAction(String name, boolean keyPressed, float tpf) {
                if (name.equals("CtrlKey")) {
                    isCtrlPressed = keyPressed;
                } else if (name.equals("ShiftKey")) {
                    isShiftPressed = keyPressed;
                    // 更新 PartListPanel 的 Shift 状态
                    editorUI.getPartListPanel().setShiftPressed(keyPressed);
                } else if (name.equals("TabKey")) {
                    isTabPressed = keyPressed;

                    // 检测 Shift+Tab 组合键进入/退出镜像配对模式
                    if (isShiftPressed && isTabPressed) {
                        if (keyPressed) {
                            enterMirrorPairingMode();
                        }
                    } else if (!isTabPressed && mirrorPairingMode) {
                        // Tab 释放时退出镜像配对模式
                        exitMirrorPairingMode();
                    }
                }
            }
        };

        // 普通按键监听器
        ActionListener actionListener = new ActionListener() {
            @Override
            public void onAction(String name, boolean keyPressed, float tpf) {
                if(isShiftPanningCamera){
                    if(name.equals("CloseEditor")&&!keyPressed){isShiftPanningCamera=false;cameraPivotPoint.set(shiftPanStartPivot);shiftPanCamera=null;updateCameraPosition();}
                    return;
                }
                if(volumeTools!=null&&volumeTools.isDragging()){
                    if(name.equals("CloseEditor")&&!keyPressed)volumeTools.cancel();
                    return;
                }
                // 撤销/重做在按下时响应
                if (name.equals("Undo")) {
                    if (keyPressed && isCtrlPressed) {
                        performUndo();
                    }
                    return;
                } else if (name.equals("Redo")) {
                    if (keyPressed && isCtrlPressed) {
                        performRedo();
                    }
                    return;
                } else if (name.equals("Copy")) {
                    if (keyPressed && isCtrlPressed) {
                        copyBone();
                    }
                    return;
                } else if (name.equals("Paste")) {
                    if (keyPressed && isCtrlPressed) {
                        pasteBone();
                    }
                    return;
                }

                // 镜像轴选择（只在镜像配对模式下响应，且在按键释放时）
                if (mirrorPairingMode && !keyPressed) {
                    if (name.equals("MirrorAxisX")) {
                        setMirrorAxis(MirrorManager.MirrorAxis.X);
                        return;
                    } else if (name.equals("MirrorAxisY")) {
                        setMirrorAxis(MirrorManager.MirrorAxis.Y);
                        return;
                    } else if (name.equals("MirrorAxisZ")) {
                        setMirrorAxis(MirrorManager.MirrorAxis.Z);
                        return;
                    }
                }

                // 相机视角快捷键（在按键释放时响应）
                if (!keyPressed) {
                    if(volumeMode&&name.equals("ToggleProjection")){setVolumeCameraView(5);return;}
                    if(volumeMode&&name.equals("ViewFront")){setVolumeCameraView(1);return;}
                    if(volumeMode&&name.equals("ViewLeft")){setVolumeCameraView(3);return;}
                    if(volumeMode&&name.equals("ViewTop")){setVolumeCameraView(7);return;}
                    if (name.equals("ViewFront")) {
                        setCameraToFrontView();
                        return;
                    } else if (name.equals("ViewLeft")) {
                        setCameraToLeftView();
                        return;
                    } else if (name.equals("ViewRight")) {
                        setCameraToRightView();
                        return;
                    } else if (name.equals("ViewTop")) {
                        setCameraToTopView();
                        return;
                    } else if (name.equals("ToggleBillboard")) {
                        toggleBillboardMode();
                        return;
                    }
                }

                // 其他命令只在按键释放时响应
                if (keyPressed) return;

                if (name.equals("CloseEditor")) {
                    stop();
                } else if (name.equals("RecordKeyframe")) {
                    recordKeyframeAtCurrentTime();
                } else if (name.equals("TogglePlayback")) {
                    togglePlayback();
                } else if (name.equals("ToggleKeyframeType")) {
                    toggleKeyframeType();
                } else if (name.equals("ZoomInTimeline")) {
                    zoomTimeline(true);  // A键放大
                } else if (name.equals("ZoomOutTimeline")) {
                    zoomTimeline(false);  // D键缩小
                } else if (name.equals("ToggleFullscreen")) {
                    toggleFullscreen();
                }
            }
        };

        // 注册修饰键监听器（Ctrl, Shift, Tab）
        inputManager.addListener(modifierListener, "CtrlKey", "ShiftKey", "TabKey");

        // 注册所有其他映射
        String[] mappings = new String[]{
            "CloseEditor",
            "RecordKeyframe",
            "TogglePlayback",
            "ToggleKeyframeType",
            "ZoomInTimeline",
            "ZoomOutTimeline",
            "ToggleFullscreen",
            "Undo",
            "Redo",
            "Copy",
            "Paste",
            "MirrorAxisX",
            "MirrorAxisY",
            "MirrorAxisZ",
            "ToggleProjection",
            "ViewFront",
            "ViewLeft",
            "ViewRight",
            "ViewTop",
            "ToggleBillboard"
        };

        inputManager.addListener(actionListener, mappings);
    }

    /**
     * 选择骨骼（支持 Shift 多选和镜像配对）
     */
    private void selectBone(int index) {
        if (allBones == null || index < 0 || index >= allBones.length) {
            return;
        }

        EditorBone bone = allBones[index];

        // 检查是否在等待设置父骨骼模式
        if (waitingForParentSelect) {
            if (isAddingFreeBone) {
                completeAddFreeBone(bone);
            } else {
                completeSetParent(bone);
            }
            return;
        }

        // 镜像配对模式
        if (mirrorPairingMode) {
            if (firstMirrorBone == null) {
                // 选择第一个骨骼
                firstMirrorBone = bone;
            } else if (firstMirrorBone != bone) {
                // 选择第二个骨骼，建立镜像关系（默认 X 轴）
                boolean success = mirrorManager.addMirrorPair(firstMirrorBone, bone, MirrorManager.MirrorAxis.X);
                if (success) {
                    // 刷新 PartListPanel 显示镜像彩色边框
                    editorUI.getPartListPanel().refreshDisplay();
                } else {
                }
                firstMirrorBone = null;  // 重置，等待下一次配对
            } else {
            }
            return;
        }

        // Shift 多选模式
        if (isShiftPressed) {
            if (selectedBones.contains(bone)) {
                // 已选中，取消选中
                selectedBones.remove(bone);
                EditorPuppetPartRenderer renderer = puppetTestScene.getPuppetRenderer().getPartRenderer(bone.getName());
                if (renderer != null) {
                    renderer.setSelected(false);
                }
            } else {
                // 未选中，添加到选中集合
                selectedBones.add(bone);
                EditorPuppetPartRenderer renderer = puppetTestScene.getPuppetRenderer().getPartRenderer(bone.getName());
                if (renderer != null) {
                    renderer.setSelected(true);
                }
            }

            // 更新主选中骨骼为最后添加的
            selectedBoneIndex = index;
            selectedBone = bone;

            // 更新UI（显示最后选中的骨骼信息）
            EditorPuppetPartRenderer partRenderer = puppetTestScene.getPuppetRenderer().getPartRenderer(bone.getName());
            editorUI.selectBone(bone, partRenderer);
            return;
        }

        // 正常单选模式
        // 取消之前所有选中的骨骼高光
        if (!selectedBones.isEmpty()) {
            for (EditorBone prevBone : selectedBones) {
                EditorPuppetPartRenderer prevRenderer = puppetTestScene.getPuppetRenderer()
                    .getPartRenderer(prevBone.getName());
                if (prevRenderer != null) {
                    prevRenderer.setSelected(false);
                }
            }
            selectedBones.clear();
        }

        // 单独处理之前的单选骨骼（兼容旧逻辑）
        if (selectedBoneIndex >= 0 && selectedBoneIndex < allBones.length) {
            EditorBone prevBone = allBones[selectedBoneIndex];
            EditorPuppetPartRenderer prevRenderer = puppetTestScene.getPuppetRenderer()
                .getPartRenderer(prevBone.getName());
            if (prevRenderer != null) {
                prevRenderer.setSelected(false);
            }
        }

        selectedBoneIndex = index;
        selectedBone = bone;  // 设置选中的骨骼引用
        selectedBones.add(bone);  // 同时加入多选集合

        // 获取对应的渲染器
        EditorPuppetPartRenderer partRenderer = puppetTestScene.getPuppetRenderer().getPartRenderer(bone.getName());

        // 设置新选中的骨骼高光
        if (partRenderer != null) {
            partRenderer.setSelected(true);
        }

        // 更新UI
        editorUI.selectBone(bone, partRenderer);
    }

    /**
     * 调整当前骨骼的宽度
     */
    private void adjustCurrentBoneWidth(float delta) {
        if (allBones == null || selectedBoneIndex >= allBones.length) {
            return;
        }

        EditorBone bone = allBones[selectedBoneIndex];
        EditorPuppetPartRenderer partRenderer = puppetTestScene.getPuppetRenderer().getPartRenderer(bone.getName());

        if (partRenderer != null) {
            partRenderer.adjustWidth(delta);
            // 更新UI显示
            editorUI.updateInspector();
        }
    }

    /**
     * 调整当前骨骼的高度
     */
    private void adjustCurrentBoneHeight(float delta) {
        if (allBones == null || selectedBoneIndex >= allBones.length) {
            return;
        }

        EditorBone bone = allBones[selectedBoneIndex];
        EditorPuppetPartRenderer partRenderer = puppetTestScene.getPuppetRenderer().getPartRenderer(bone.getName());

        if (partRenderer != null) {
            partRenderer.adjustHeight(delta);
            // 更新UI显示
            editorUI.updateInspector();
        }
    }

    /**
     * 设置鼠标监听器
     */
    private void setupMouseListener() {
        mouseListener = new RawInputListener() {
            @Override
            public void beginInput() {}

            @Override
            public void endInput() {}

            @Override
            public void onJoyAxisEvent(com.jme3.input.event.JoyAxisEvent evt) {}

            @Override
            public void onJoyButtonEvent(com.jme3.input.event.JoyButtonEvent evt) {}

            @Override
            public void onMouseMotionEvent(MouseMotionEvent evt) {
                int mouseX = evt.getX();
                int mouseY = evt.getY();

                if(isShiftPanningCamera){updateShiftCameraPan(mouseX,mouseY);lastMouseX=mouseX;lastMouseY=mouseY;evt.setConsumed();return;}
                if(volumeTools!=null&&volumeTools.isDragging()){volumeTools.drag(mouseX,mouseY);evt.setConsumed();return;}
                // 检查UV详览面板是否可见，如果可见则优先处理并阻止背景操作
                if (editorUI != null && editorUI.getSliderColumnPanel() != null &&
                    editorUI.getSliderColumnPanel().getEnlargedPreviewPanel() != null &&
                    editorUI.getSliderColumnPanel().getEnlargedPreviewPanel().isVisible()) {

                    editorUI.getSliderColumnPanel().getEnlargedPreviewPanel().handleMouseDrag(mouseX, mouseY);
                    return; // 阻止所有背景操作
                }

                // 如果正在拖动相机（左键旋转）
                if (isDraggingCamera) {
                    int deltaX = mouseX - lastMouseX;
                    int deltaY = mouseY - lastMouseY;

                    // 检测是否有明显的移动（距离起始位置超过5像素）
                    int totalDeltaX = mouseX - rightClickStartX;
                    int totalDeltaY = mouseY - rightClickStartY;
                    if (Math.abs(totalDeltaX) > 5 || Math.abs(totalDeltaY) > 5) {
                        hasMouseMoved = true;
                    }

                    cameraAngleY += deltaX * 0.005f;
                    cameraAngleX -= deltaY * 0.005f;

                    // 限制垂直角度
                    cameraAngleX = Math.max(volumeMode?-FastMath.HALF_PI:-1.5f, Math.min(volumeMode?FastMath.HALF_PI:1.5f, cameraAngleX));

                    updateCameraPosition();
                }

                // 如果正在平移相机（右键平移）
                if (isPanningCamera) {
                    int deltaX = mouseX - lastMouseX;
                    int deltaY = mouseY - lastMouseY;

                    // 检测是否有明显的移动
                    int totalDeltaX = mouseX - rightClickStartX;
                    int totalDeltaY = mouseY - rightClickStartY;
                    if (Math.abs(totalDeltaX) > 5 || Math.abs(totalDeltaY) > 5) {
                        hasMouseMoved = true;
                    }

                    // 计算相机的右方向和上方向
                    Vector3f cameraDir = cam.getDirection().normalize();
                    Vector3f cameraUp = cam.getUp().normalize();
                    Vector3f cameraRight = cameraDir.cross(cameraUp).normalize();

                    // 根据鼠标移动平移相机pivot point
                    // deltaX 控制左右移动（沿相机右方向）
                    // deltaY 控制上下移动（沿相机上方向）
                    float panSpeed = 0.003f * cameraDistance;  // 降低平移速度，原来是0.01f
                    cameraPivotPoint.addLocal(cameraRight.mult(-deltaX * panSpeed));
                    cameraPivotPoint.addLocal(cameraUp.mult(deltaY * panSpeed));

                    updateCameraPosition();
                }

                // 更新TimelinePanel拖动（播放头和面板拖动）
                if (!isDraggingCamera && !isPanningCamera && editorUI != null && editorUI.getTimelinePanel() != null) {
                    editorUI.getTimelinePanel().handleMouseDrag(mouseX, mouseY);
                    editorUI.getTimelinePanel().handlePanelDrag(mouseX, mouseY);
                }

                // 更新滑条（如果不在拖动相机）
                if (!isDraggingCamera && !isPanningCamera && editorUI != null && editorUI.getSliderColumnPanel() != null) {
                    editorUI.getSliderColumnPanel().handleMouseDrag(mouseX, mouseY);
                }

                // 更新部件列表面板拖动
                if (!isDraggingCamera && !isPanningCamera && editorUI != null && editorUI.getPartListPanel() != null) {
                    editorUI.getPartListPanel().handleMouseDrag(mouseX, mouseY);
                }

                // 更新动画层面板拖动
                if (!isDraggingCamera && !isPanningCamera && editorUI != null && editorUI.getAnimationLayerPanel() != null) {
                    editorUI.getAnimationLayerPanel().handleMouseDrag(mouseX, mouseY);
                }

                lastMouseX = mouseX;
                lastMouseY = mouseY;
            }

            @Override
            public void onMouseButtonEvent(MouseButtonEvent evt) {
                // 如果还没完全初始化，忽略所有鼠标事件
                if (!fullyInitialized) {
                    return;
                }

                int mouseX = evt.getX();
                int mouseY = evt.getY();

                // 检查UV详览面板是否可见，如果可见则优先处理并阻止背景操作
                if (editorUI != null && editorUI.getSliderColumnPanel() != null &&
                    editorUI.getSliderColumnPanel().getEnlargedPreviewPanel() != null &&
                    editorUI.getSliderColumnPanel().getEnlargedPreviewPanel().isVisible()) {

                    EnlargedTexturePreviewPanel preview = editorUI.getSliderColumnPanel().getEnlargedPreviewPanel();
                    int button = evt.getButtonIndex();

                    if (evt.isPressed()) {
                        preview.handleMousePress(mouseX, mouseY, button);
                    } else {
                        preview.handleMouseRelease(mouseX, mouseY);
                    }
                    return; // 阻止所有背景操作
                }

                if(isShiftPanningCamera){
                    if(evt.getButtonIndex()==MouseInput.BUTTON_LEFT&&evt.isReleased()){updateShiftCameraPan(mouseX,mouseY);isShiftPanningCamera=false;shiftPanCamera=null;}
                    evt.setConsumed();return;
                }
                // Capture Shift + left before axis handles and part selection, only inside the viewport.
                if(volumeMode&&isShiftPressed&&evt.getButtonIndex()==MouseInput.BUTTON_LEFT&&evt.isPressed()
                    &&volumeViewportAllows(mouseX,mouseY)&&(volumeTools==null||!volumeTools.isDragging())){
                    isShiftPanningCamera=true;shiftPanStartX=mouseX;shiftPanStartY=mouseY;
                    shiftPanStartPivot=cameraPivotPoint.clone();shiftPanCamera=cam.clone();shiftPanDepth=cam.getScreenCoordinates(cameraPivotPoint).z;
                    isDraggingCamera=false;isPanningCamera=false;lastMouseX=mouseX;lastMouseY=mouseY;evt.setConsumed();return;
                }
                if(volumeTools!=null){
                    if(volumeTools.isDragging()){
                        if(evt.getButtonIndex()==MouseInput.BUTTON_LEFT&&evt.isReleased()){volumeTools.drag(mouseX,mouseY);volumeTools.finish();}
                        evt.setConsumed();return;
                    }
                    if(evt.getButtonIndex()==MouseInput.BUTTON_LEFT&&evt.isPressed()&&volumeViewportAllows(mouseX,mouseY)&&volumeTools.press(mouseX,mouseY)){
                        isDraggingCamera=false;isPanningCamera=false;evt.setConsumed();return;
                    }
                }
                // 左键设置旋转中心点 / 拖动面板 / 拖动视角
                if (evt.getButtonIndex() == MouseInput.BUTTON_LEFT) {
                    if (evt.isPressed()) {
                        // 检查是否点击在按钮或滑条上
                        boolean clickedOnUI = false;

                        // 检查顶栏按钮（Back/Exit）- 优先级最高
                        if (editorUI != null) {
                            clickedOnUI = editorUI.handleMouseClick(mouseX, mouseY);
                        }

                        // 检查TimelinePanel（优先级第二，因为在底部）
                        if (!clickedOnUI && editorUI != null && editorUI.getTimelinePanel() != null) {
                            // 先检查标题栏点击（用于拖动面板）
                            clickedOnUI = editorUI.getTimelinePanel().handleTitleBarClick(mouseX, mouseY);
                            // 如果不是标题栏，再检查时间轴按钮和播放头
                            if (!clickedOnUI) {
                                clickedOnUI = editorUI.getTimelinePanel().handleMouseClick(mouseX, mouseY);
                            }
                        }

                        if (!clickedOnUI && editorUI != null && editorUI.getButtonColumnPanel() != null) {
                            clickedOnUI = editorUI.getButtonColumnPanel().handleMouseClick(mouseX, mouseY);
                        }

                        // 检查SliderColumnPanel（先检查边缘调整大小，再检查标题栏拖动）
                        if (!clickedOnUI && editorUI != null && editorUI.getSliderColumnPanel() != null) {
                            // 先检查边缘点击（用于调整大小）
                            clickedOnUI = editorUI.getSliderColumnPanel().handleResizeBorderClick(mouseX, mouseY);
                            // 如果不是边缘，再检查标题栏点击（用于拖动）
                            if (!clickedOnUI) {
                                clickedOnUI = editorUI.getSliderColumnPanel().handleTitleBarClick(mouseX, mouseY);
                            }
                            // 如果不是标题栏，再检查滑条
                            if (!clickedOnUI) {
                                clickedOnUI = editorUI.getSliderColumnPanel().handleMouseClick(mouseX, mouseY);
                            }
                        }

                        // 检查是否点击在部件列表面板上（先检查标题栏以启用拖动）
                        if (!clickedOnUI && editorUI != null && editorUI.getPartListPanel() != null) {
                            // 先检查标题栏点击（用于拖动）
                            clickedOnUI = editorUI.getPartListPanel().handleTitleBarClick(mouseX, mouseY);
                            // 如果不是标题栏，再检查是否点击在部件列表项上（含拖拽分组按下检测）
                            if (!clickedOnUI) {
                                clickedOnUI = editorUI.getPartListPanel().handleMousePress(mouseX, mouseY);
                            }
                        }

                        // 检查是否点击在骨骼分组控制面板上
                        if (!clickedOnUI && editorUI != null && editorUI.getGroupControlPanel() != null) {
                            clickedOnUI = editorUI.getGroupControlPanel().handleMouseClick(mouseX, mouseY);
                        }

                        // 如果没有点击UI，检查是否点击在部件上
                        if (!clickedOnUI) {
                            // 记录起始位置
                            rightClickStartX = mouseX;
                            rightClickStartY = mouseY;
                            hasMouseMoved = false;

                            // 检查是否点击在部件上（射线检测）
                            boolean clickedOnPart = checkIfClickedOnPart(mouseX, mouseY);

                            // 如果没有点击在部件上，启动相机拖动模式（Unity/Blender风格）
                            if (!clickedOnPart || volumeMode) {
                                isDraggingCamera = true;
                                lastMouseX = mouseX;
                                lastMouseY = mouseY;
                            }
                        }
                    } else {
                        // 释放左键
                        if (editorUI != null && editorUI.getTimelinePanel() != null) {
                            editorUI.getTimelinePanel().handleMouseRelease();
                            editorUI.getTimelinePanel().handlePanelDragRelease();
                        }
                        if (editorUI != null && editorUI.getSliderColumnPanel() != null) {
                            editorUI.getSliderColumnPanel().handleMouseRelease();
                        }
                        if (editorUI != null && editorUI.getPartListPanel() != null) {
                            editorUI.getPartListPanel().handleMouseRelease();
                        }
                        if (editorUI != null && editorUI.getAnimationLayerPanel() != null) {
                            editorUI.getAnimationLayerPanel().handleMouseRelease();
                        }

                        // 如果正在拖动相机，停止拖动
                        if (isDraggingCamera) {
                            isDraggingCamera = false;

                            // 如果没有移动，说明是点击（设置旋转中心点）
                            if (!hasMouseMoved) {
                                handleSetPivotPoint(mouseX, mouseY);
                            }
                        }
                    }
                }

                if (volumeMode && evt.getButtonIndex() != MouseInput.BUTTON_LEFT && editorUI.blocksVolumeViewport(mouseX, mouseY)) {
                    isPanningCamera=false;evt.setConsumed();return;
                }

                // 右键拖动平移视角 / 点击选择部件
                if (evt.getButtonIndex() == MouseInput.BUTTON_RIGHT) {
                    if (evt.isPressed()) {
                        // 记录起始位置
                        rightClickStartX = mouseX;
                        rightClickStartY = mouseY;
                        hasMouseMoved = false;

                        // 开始平移模式
                        isPanningCamera = true;
                        lastMouseX = mouseX;
                        lastMouseY = mouseY;
                    } else {
                        // 释放时，判断是点击还是拖动
                        isPanningCamera = false;

                        // 如果没有移动，说明是点击，执行选择
                        if (!hasMouseMoved) {
                            // 检查是否点击在UI上
                            boolean clickedOnUI = false;

                            // 检查顶栏按钮（Back/Exit）
                            if (editorUI != null) {
                                clickedOnUI = editorUI.handleMouseClick(mouseX, mouseY);
                            }

                            // 检查TimelinePanel
                            if (!clickedOnUI && editorUI != null && editorUI.getTimelinePanel() != null) {
                                clickedOnUI = editorUI.getTimelinePanel().handleMouseClick(mouseX, mouseY);
                            }

                            if (!clickedOnUI && editorUI != null && editorUI.getButtonColumnPanel() != null) {
                                clickedOnUI = editorUI.getButtonColumnPanel().handleMouseClick(mouseX, mouseY);
                            }
                            if (!clickedOnUI && editorUI != null && editorUI.getSliderColumnPanel() != null) {
                                clickedOnUI = editorUI.getSliderColumnPanel().handleMouseClick(mouseX, mouseY);
                            }
                            // 检查是否点击在部件列表面板上
                            if (!clickedOnUI && editorUI != null && editorUI.getPartListPanel() != null) {
                                clickedOnUI = editorUI.getPartListPanel().handleMouseClick(mouseX, mouseY);
                            }

                            // 如果没有点击UI，尝试选择部件
                            if (!clickedOnUI) {
                                handleSelectPart(mouseX, mouseY);
                            }
                        }
                    }
                }
            }

            @Override
            public void onKeyEvent(com.jme3.input.event.KeyInputEvent evt) {}

            @Override
            public void onTouchEvent(com.jme3.input.event.TouchEvent evt) {}
        };

        inputManager.addRawInputListener(mouseListener);

        // 添加鼠标滚轮监听（监听两个方向）
        inputManager.addMapping("CameraZoomUp", new com.jme3.input.controls.MouseAxisTrigger(MouseInput.AXIS_WHEEL, false));
        inputManager.addMapping("CameraZoomDown", new com.jme3.input.controls.MouseAxisTrigger(MouseInput.AXIS_WHEEL, true));

        com.jme3.input.controls.AnalogListener zoomListener = new com.jme3.input.controls.AnalogListener() {
            @Override
            public void onAnalog(String name, float value, float tpf) {
                if(isShiftPanningCamera||(volumeTools!=null&&volumeTools.isDragging()))return;
                // 获取当前鼠标位置
                int mouseX = inputManager.getCursorPosition().getX() != 0 ? (int) inputManager.getCursorPosition().getX() : lastMouseX;
                int mouseY = inputManager.getCursorPosition().getY() != 0 ? (int) inputManager.getCursorPosition().getY() : lastMouseY;

                // 计算滚轮方向：向上滚动（缩小）为负值，向下滚动（放大）为正值
                int scrollAmount = name.equals("CameraZoomUp") ? -1 : 1;

                // 优先检查Timeline是否处理滚轮事件
                if (editorUI != null && editorUI.getTimelinePanel() != null) {
                    if (editorUI.getTimelinePanel().handleMouseScroll(mouseX, mouseY, scrollAmount)) {
                        return; // Timeline处理了滚动，不处理其他缩放
                    }
                }

                // 检查旋转条选区面板是否可见并处理滚轮缩放（整数倍缩放）
                if (editorUI != null && editorUI.getSliderColumnPanel() != null &&
                    editorUI.getSliderColumnPanel().getRotationStripSelectorPanel() != null &&
                    editorUI.getSliderColumnPanel().getRotationStripSelectorPanel().isVisible()) {

                    float stripScrollAmount = name.equals("CameraZoomUp") ? -1.0f : 1.0f;
                    if (editorUI.getSliderColumnPanel().getRotationStripSelectorPanel().handleMouseScroll(mouseX, mouseY, stripScrollAmount)) {
                        return; // 旋转条选区面板处理了滚动，不处理相机缩放
                    }
                }

                // 检查UV详览面板是否可见并处理滚轮缩放
                if (editorUI != null && editorUI.getSliderColumnPanel() != null &&
                    editorUI.getSliderColumnPanel().getEnlargedPreviewPanel() != null &&
                    editorUI.getSliderColumnPanel().getEnlargedPreviewPanel().isVisible()) {

                    float uvScrollAmount = name.equals("CameraZoomUp") ? -1.0f : 1.0f;
                    if (editorUI.getSliderColumnPanel().getEnlargedPreviewPanel().handleMouseScroll(mouseX, mouseY, uvScrollAmount)) {
                        return; // UV预览处理了滚动，不处理相机缩放
                    }
                }

                // 尝试将滚轮事件发送给部件列表面板
                if (editorUI != null && editorUI.getPartListPanel() != null) {
                    if (editorUI.getPartListPanel().handleMouseScroll(mouseX, mouseY, scrollAmount)) {
                        return; // 部件列表处理了滚动，不处理相机缩放
                    }
                }

                // 如果不在任何UI区域，处理相机缩放
                if (name.equals("CameraZoomUp")) {
                    // 向上滚动：拉近
                    cameraDistance = Math.max(volumeMode?.2f:2f, cameraDistance - value * 5f);
                    updateCameraPosition();
                } else if (name.equals("CameraZoomDown")) {
                    // 向下滚动：拉远
                    cameraDistance = Math.min(100f, cameraDistance + value * 5f);
                    updateCameraPosition();
                }
            }
        };

        inputManager.addListener(zoomListener, "CameraZoomUp", "CameraZoomDown");
    }

    /**
     * 更新相机位置
     */
    private void updateShiftCameraPan(int mouseX,int mouseY){
        // Unproject on the view plane through the orbit target. This gives smooth, one-to-one
        // screen dragging in both projection modes without changing distance, FOV, or part data.
        Vector3f start=shiftPanCamera.getWorldCoordinates(new com.jme3.math.Vector2f(shiftPanStartX,shiftPanStartY),shiftPanDepth);
        Vector3f current=shiftPanCamera.getWorldCoordinates(new com.jme3.math.Vector2f(mouseX,mouseY),shiftPanDepth);
        Vector3f delta=start.subtract(current);
        if(!Vector3f.isValidVector(delta))return;
        cameraPivotPoint.set(shiftPanStartPivot).addLocal(delta);
        updateCameraPosition();
    }
    private void updateCameraPosition() {
        float x = (float) (cameraDistance * Math.cos(cameraAngleX) * Math.sin(cameraAngleY));
        float y = (float) (cameraDistance * Math.sin(cameraAngleX));
        float z = (float) (cameraDistance * Math.cos(cameraAngleX) * Math.cos(cameraAngleY));

        // 相机位置 = 旋转中心点 + 相机偏移
        Vector3f cameraPos = new Vector3f(
            cameraPivotPoint.x + x,
            cameraPivotPoint.y + y,
            cameraPivotPoint.z + z
        );

        cam.setLocation(cameraPos);
        Vector3f cameraUp=volumeMode?new Vector3f(-FastMath.sin(cameraAngleY)*FastMath.sin(cameraAngleX),FastMath.cos(cameraAngleX),-FastMath.cos(cameraAngleY)*FastMath.sin(cameraAngleX)):Vector3f.UNIT_Y;
        cam.lookAt(cameraPivotPoint,cameraUp);
        if(volumeMode)updateVolumeCameraProjection();

        // 同步方向指示球体的旋转
        updateDirectionSphereRotation();
    }

    /**
     * 更新方向指示球体的旋转，使其与相机旋转同步
     */
    private void updateDirectionSphereRotation() {
        if (directionSphere == null) {
            return;
        }

        // 创建旋转四元数，基于相机角度
        // 注意：球体的旋转需要与相机相反，这样才能正确显示方向
        Quaternion rotation = new Quaternion();

        // 先绕Y轴旋转（水平角度）
        Quaternion yRotation = new Quaternion();
        yRotation.fromAngleAxis(cameraAngleY, Vector3f.UNIT_Y);

        // 再绕X轴旋转（垂直角度）
        Quaternion xRotation = new Quaternion();
        xRotation.fromAngleAxis(cameraAngleX, Vector3f.UNIT_X);

        // 组合旋转：先Y后X
        rotation = yRotation.mult(xRotation);

        // 应用旋转到球体
        directionSphere.setLocalRotation(volumeMode?cam.getRotation().inverse():rotation);
    }

    /**
     * 更新摄像机方向检测
     * 根据cameraAngleX和cameraAngleY判断当前查看的是哪个方向
     * 支持6个方向：前后左右上下
     */
    private void updateCameraDirection() {
        if(volumeMode){if(editorUI!=null)editorUI.updateCurrentDirection(currentCameraDirection);return;}
        EditorBone.Direction newDirection;

        // 首先检查垂直角度（优先级更高）
        // cameraAngleX > 0 表示从上往下看，< 0 表示从下往上看
        float verticalThreshold = (float) (Math.PI / 6);  // 30度阈值

        if (cameraAngleX > verticalThreshold) {
            // 从上往下看 → 看到木偶的顶部
            newDirection = EditorBone.Direction.UP;
        } else if (cameraAngleX < -verticalThreshold) {
            // 从下往上看 → 看到木偶的底部
            newDirection = EditorBone.Direction.DOWN;
        } else {
            // 水平视角：检查水平角度
            // 将cameraAngleY归一化到0-2π范围
            float angle = cameraAngleY;
            while (angle < 0) angle += Math.PI * 2;
            while (angle >= Math.PI * 2) angle -= Math.PI * 2;

            // 根据角度判断方向
            // 0° (0) = FRONT
            // 90° (π/2) = RIGHT
            // 180° (π) = BACK
            // 270° (3π/2) = LEFT
            if (angle < Math.PI / 4 || angle >= 7 * Math.PI / 4) {
                // 0-45° 或 315-360°: FRONT
                newDirection = EditorBone.Direction.FRONT;
            } else if (angle >= Math.PI / 4 && angle < 3 * Math.PI / 4) {
                // 45-135°: RIGHT
                newDirection = EditorBone.Direction.RIGHT;
            } else if (angle >= 3 * Math.PI / 4 && angle < 5 * Math.PI / 4) {
                // 135-225°: BACK
                newDirection = EditorBone.Direction.BACK;
            } else {
                // 225-315°: LEFT
                newDirection = EditorBone.Direction.LEFT;
            }
        }

        // 如果方向改变，更新并切换贴图
        if (newDirection != currentCameraDirection) {
            currentCameraDirection = newDirection;
            updateDirectionalTextures();

            // 更新UI显示
            if (editorUI != null) {
                editorUI.updateCurrentDirection(currentCameraDirection);

                // 更新TimelinePanel的方向按钮
                // if (editorUI.getTimelinePanel() != null) { // COMMENTED OUT - OLD UI
                    // editorUI.getTimelinePanel().updateDirectionButtons(currentCameraDirection.getKey()); // COMMENTED OUT - OLD UI
                // } // COMMENTED OUT - OLD UI
            }
        }
    }

    /**
     * 切换到正面视角 (Numpad 1 / Up Arrow)
     */
    public void setVolumeCameraView(int key) {
        if(!volumeMode)return;
        if(key==5){volumeOrthographic=!volumeOrthographic;updateVolumeCameraProjection();return;}
        if(key==1){cameraAngleY=0;cameraAngleX=0;}
        else if(key==3){cameraAngleY=FastMath.HALF_PI;cameraAngleX=0;}
        else if(key==7){cameraAngleY=0;cameraAngleX=FastMath.HALF_PI;}
        else return;
        volumeOrthographic=true;updateCameraPosition();updateCameraDirection();
    }
    public void setPixelCameraElevation(int degrees){
        if(!volumeMode)return;
        cameraAngleX=FastMath.clamp(degrees,-90,90)*FastMath.DEG_TO_RAD;
        updateCameraPosition();updateCameraDirection();
    }
    public String getPixelCameraViewLabel(){
        float pitch=cameraAngleX*FastMath.RAD_TO_DEG;
        String name=Math.abs(pitch-90)<.05f?"Top":Math.abs(pitch+90)<.05f?"Bottom":pitch>.05f?"Upper slant":pitch<-.05f?"Lower slant":"Level";
        return name+" "+Math.round(pitch)+" deg";
    }
    private void updateVolumeCameraProjection(){
        float aspect=(float)cam.getWidth()/Math.max(1,cam.getHeight());
        if(volumeOrthographic){float half=cameraDistance*FastMath.tan(22.5f*FastMath.DEG_TO_RAD);cam.setParallelProjection(true);cam.setFrustum(.1f,1000,-half*aspect,half*aspect,half,-half);}
        else cam.setFrustumPerspective(45,aspect,.1f,1000);
    }
    private void setCameraToFrontView() {
        cameraAngleY = 0f;  // 0° = FRONT
        cameraAngleX = 0f;  // 水平视角
        updateCameraPosition();
        updateCameraDirection();
    }

    /**
     * 切换到左侧视角 (Numpad 3 / Left Arrow)
     */
    private void setCameraToLeftView() {
        cameraAngleY = (float) (Math.PI * 3 / 2);  // 270° = LEFT
        cameraAngleX = 0f;  // 水平视角
        updateCameraPosition();
        updateCameraDirection();
    }

    /**
     * 切换到右侧视角 (Numpad 5 / Right Arrow)
     */
    private void setCameraToRightView() {
        cameraAngleY = (float) (Math.PI / 2);  // 90° = RIGHT
        cameraAngleX = 0f;  // 水平视角
        updateCameraPosition();
        updateCameraDirection();
    }

    /**
     * 切换到顶部视角 (Numpad 7 / Down Arrow)
     */
    private void setCameraToTopView() {
        cameraAngleY = 0f;  // 保持朝向
        cameraAngleX = (float) (Math.PI / 2 - 0.1);  // 接近90°俯视 (稍微偏移以避免gimbal lock)
        updateCameraPosition();
        updateCameraDirection();
    }

    /**
     * 切换Billboard渲染模式 (B键)
     * 循环切换: DISABLED -> UNIFIED -> INDEPENDENT -> DISABLED...
     */
    private void toggleBillboardMode() {
        if (puppetTestScene == null || puppetTestScene.getPuppetRenderer() == null) {
            return;
        }

        EditorPuppetRenderer renderer = puppetTestScene.getPuppetRenderer();
        EditorPuppetRenderer.BillboardMode currentMode = renderer.getBillboardMode();

        // 循环切换模式
        EditorPuppetRenderer.BillboardMode nextMode;
        String modeName;

        switch (currentMode) {
            case DISABLED:
                nextMode = EditorPuppetRenderer.BillboardMode.UNIFIED;
                modeName = "统一Billboard (纸人模式)";
                break;
            case UNIFIED:
                nextMode = EditorPuppetRenderer.BillboardMode.INDEPENDENT;
                modeName = "独立Billboard (不推荐)";
                break;
            case INDEPENDENT:
            default:
                nextMode = EditorPuppetRenderer.BillboardMode.DISABLED;
                modeName = "禁用Billboard (3D立体模式)";
                break;
        }

        renderer.setBillboardMode(nextMode);

    }

    /**
     * 循环切换重力方向
     * DOWN -> UP -> LEFT -> RIGHT -> FRONT -> BACK -> DOWN...
     */
    private void cycleGravityDirection() {
        if (selectedBone == null) {
            return;
        }

        EditorBone.GravityDirection currentGravity = selectedBone.getGravityDirection();
        EditorBone.GravityDirection nextGravity;
        String gravityName;

        switch (currentGravity) {
            case DOWN:
                nextGravity = EditorBone.GravityDirection.UP;
                gravityName = "上";
                break;
            case UP:
                nextGravity = EditorBone.GravityDirection.LEFT;
                gravityName = "左";
                break;
            case LEFT:
                nextGravity = EditorBone.GravityDirection.RIGHT;
                gravityName = "右";
                break;
            case RIGHT:
                nextGravity = EditorBone.GravityDirection.FRONT;
                gravityName = "前";
                break;
            case FRONT:
                nextGravity = EditorBone.GravityDirection.BACK;
                gravityName = "后";
                break;
            case BACK:
            default:
                nextGravity = EditorBone.GravityDirection.DOWN;
                gravityName = "下";
                break;
        }

        selectedBone.setGravityDirection(nextGravity);

        // 更新按钮文本
        if (editorUI != null && editorUI.getButtonColumnPanel() != null) {
            Button gravityButton = editorUI.getButtonColumnPanel().getGravityDirectionButton();
            if (gravityButton != null) {
                gravityButton.setText("重力:" + gravityName);
            }
        }
    }

    /**
     * 打开相机跟随设置对话框
     */
    private void openCameraFollowDialog() {
        if (selectedBone == null) {
            return;
        }

        // 创建并显示对话框
        CameraFollowDialog dialog = new CameraFollowDialog(
            null,
            selectedBone.getCameraFollowFreedomX(),
            selectedBone.getCameraFollowFreedomY()
        );
        dialog.setVisible(true);

        // 如果用户确认了输入
        if (dialog.isConfirmed()) {
            selectedBone.setCameraFollowFreedomX(dialog.getCameraFollowX());
            selectedBone.setCameraFollowFreedomY(dialog.getCameraFollowY());

            // 更新按钮文本
            if (editorUI != null && editorUI.getButtonColumnPanel() != null) {
                editorUI.getButtonColumnPanel().updateCameraFollowButtonText(
                    dialog.getCameraFollowX(),
                    dialog.getCameraFollowY()
                );
            }
        }
    }

    /**
     * 根据当前方向更新所有部件的贴图
     */
    private void updateDirectionalTextures() {
        if (puppetTestScene == null || puppetTestScene.getPuppetRenderer() == null ||
            puppetTestScene.getTestSkeleton() == null) {
            return;
        }

        // 获取所有骨骼
        java.util.List<EditorBone> allBones = puppetTestScene.getTestSkeleton().getAllBones();

        for (EditorBone bone : allBones) {
            // 设置骨骼的当前方向
            bone.setCurrentDirection(currentCameraDirection.getKey());

            // 获取部件渲染器
            EditorPuppetPartRenderer partRenderer = puppetTestScene.getPuppetRenderer().getPartRenderer(bone.getName());
            if (partRenderer != null) {
                // 更新渲染器的贴图（使用fallback策略）
                partRenderer.updateTextureFromBone();
            }
        }
    }

    /**
     * 设置光照（已禁用 - 避免光影影响贴图查看）
     */
    /*
    private void setupLights() {
        // 主光源
        DirectionalLight sun = new DirectionalLight();
        sun.setDirection(new Vector3f(-0.5f, -1.0f, -0.5f).normalizeLocal());
        sun.setColor(ColorRGBA.White);
        rootNode.addLight(sun);

        // 辅助光源
        DirectionalLight fill = new DirectionalLight();
        fill.setDirection(new Vector3f(0.5f, -0.5f, 0.5f).normalizeLocal());
        fill.setColor(new ColorRGBA(0.6f, 0.6f, 0.7f, 1.0f));
        rootNode.addLight(fill);
    }
    */

    @Override
    public void simpleUpdate(float tpf) {
        // 处理初始化延迟 - 防止启动时的意外鼠标事件
        if (!fullyInitialized) {
            initializationDelay += tpf;
            if (initializationDelay >= 0.5f) {  // 延迟0.5秒
                fullyInitialized = true;
            }
        }

        animationTime += tpf;

        // 更新动画播放器
        if (animationPlayer != null) {
            animationPlayer.update(tpf);

            // 同步动画播放器的时间到Timeline
            if (animationPlayer.isPlaying() && editorUI != null && editorUI.getTimelinePanel() != null) {
                editorUI.getTimelinePanel().setTime(animationPlayer.getCurrentTime());
            }
        }

        // 更新木偶场景
        if (puppetTestScene != null) {
            // 只在编辑器未打开且没有播放动画时播放wave动画
            // 并且wave动画未被禁用
            if ((editorUI == null || !editorUI.isVisible()) &&
                (animationPlayer == null || !animationPlayer.isPlaying()) &&
                !disableWaveAnimation) {
                puppetTestScene.playWaveAnimation(animationTime);
            }
            // 始终更新场景（保持交互功能）
            puppetTestScene.update(tpf);
        }

        if(volumeTools!=null)volumeTools.update();
        // 始终更新按钮状态
        if (editorUI != null) {
            editorUI.updateButtons(tpf);
        }

        // 更新摄像机方向检测和自动切换贴图
        updateCameraDirection();

        // 不再自动更新编辑器UI的时间
        // 时间由AnimationPlayer控制，暂停时不应该移动
    }

    @Override
    public void simpleRender(RenderManager rm) {
        // 可选：添加自定义渲染逻辑
    }

    /**
     * 录制当前时间点的关键帧
     */
    private void recordKeyframeAtCurrentTime() {
        if (animationPlayer == null || currentClip == null) {
            return;
        }

        // 使用TimelinePanel的当前时间（用户在时间轴上选择的时间）
        float time = 0f;
        if (editorUI != null && editorUI.getTimelinePanel() != null) {
            time = editorUI.getTimelinePanel().getCurrentTime();
        } else {
            // 如果没有TimelinePanel，使用animationPlayer的当前时间
            time = animationPlayer.getCurrentTime();
        }

        // 【重要】在录制关键帧之前，进入编辑模式，防止动画覆盖用户的手动编辑
        boolean wasInEditMode = animationPlayer.isEditMode();
        animationPlayer.setEditMode(true);

        // 录制所有骨骼的关键帧
        animationPlayer.recordAllKeyframes(currentClip, time);

        // 【修复】录制完成后，保持编辑模式，让用户可以继续调整并录制下一个关键帧
        // 不恢复之前的状态，避免时间轴移动时自动应用动画覆盖用户的手动编辑
        // animationPlayer.setEditMode(wasInEditMode);  // 注释掉，保持编辑模式

        int keyframeCount = currentClip.getAllKeyframes().size();

        // 打印所有关键帧信息

        for (String boneName : currentClip.getBoneNames()) {

            for (com.Hecate.puppet.animation.Keyframe kf : currentClip.getKeyframes(boneName)) {

            }
        }

        // 更新Timeline显示
        if (editorUI != null && editorUI.getTimelinePanel() != null) {
            editorUI.getTimelinePanel().updateKeyframeMarkers();
        }
    }

    /**
     * 切换播放/暂停
     */
    private void togglePlayback() {
        if (animationPlayer == null || currentClip == null) {
            return;
        }

        if (animationPlayer.isPlaying()) {
            animationPlayer.pause();
        } else {
            // 如果没有关键帧，先提示用户
            if (currentClip.getAllKeyframes().isEmpty()) {
                return;
            }

            // 退出编辑模式，允许动画播放
            exitEditMode();
            animationPlayer.play(currentClip);
        }
    }

    /**
     * 切换关键帧类型（插值/快照）
     */
    private void toggleKeyframeType() {
        if (currentKeyframeType == com.Hecate.puppet.animation.Keyframe.KeyframeType.INTERPOLATED) {
            currentKeyframeType = com.Hecate.puppet.animation.Keyframe.KeyframeType.SNAPSHOT;

        } else {
            currentKeyframeType = com.Hecate.puppet.animation.Keyframe.KeyframeType.INTERPOLATED;

        }

        // 更新UI显示当前模式
        if (editorUI != null) {
            editorUI.updateKeyframeTypeDisplay(currentKeyframeType);
        }
    }

    /**
     * 缩放时间轴（以当前时间为中心）
     * @param zoomIn true为放大（A键），false为缩小（D键）
     */
    private void zoomTimeline(boolean zoomIn) {
        if (editorUI == null || editorUI.getTimelinePanel() == null) {
            return;
        }

        // 获取当前时间
        float currentTime = editorUI.getCurrentTime();

        // 调用TimelinePanel的缩放方法
        editorUI.getTimelinePanel().zoomTimelineAtTime(currentTime, zoomIn);

    }

    /**
     * 切换全屏
     */
    private void toggleFullscreen() {
        GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        boolean isCurrentlyFullscreen = settings.isFullscreen();

        if (!isCurrentlyFullscreen) {
            // 切换到全屏
            // 获取屏幕分辨率
            DisplayMode displayMode = device.getDisplayMode();
            int screenWidth = displayMode.getWidth();
            int screenHeight = displayMode.getHeight();

            // 更新设置
            settings.setFullscreen(true);
            settings.setResolution(screenWidth, screenHeight);

            // 重新创建显示
            context.getSettings().copyFrom(settings);
            context.restart();
        } else {
            // 切换到窗口模式
            settings.setFullscreen(false);
            settings.setResolution(1200, 800);

            // 重新创建显示
            context.getSettings().copyFrom(settings);
            context.restart();
        }
    }

    /**
     * 窗口大小变化时调用
     */
    @Override
    public void reshape(int width, int height) {
        super.reshape(width, height);

        // 延迟一帧更新UI，确保camera已更新
        enqueue(() -> {
            if(directionSphere!=null)directionSphere.setLocalTranslation(volumeMode?345:width/6f,volumeMode?height-140:height*.85f,0);
            if(volumeMode)updateVolumeCameraProjection();
            if (editorUI != null && editorUI.isVisible()) {
                editorUI.reshapeUI(width, height);
            }
            return null;
        });
    }

    @Override
    public void destroy() {
        java.awt.EventQueue.invokeLater(() -> { if (volumeWorkbench != null) volumeWorkbench.close(); });
        if (puppetTestScene != null) {
            puppetTestScene.cleanup();
        }
        if (editorUI != null) {
            editorUI.cleanup();
        }
        super.destroy();
    }

    /**
     * 设置Inspector面板回调
     */
    private void setupButtonCallbacks() {
        if (editorUI != null && editorUI.getButtonColumnPanel() != null) {
            editorUI.getButtonColumnPanel().setCallbacks(new ButtonColumnPanel.ButtonCallbacks() {
                @Override
                public void onHideModeToggle(boolean enabled) {
                    isHideModeEnabled = enabled;
                }

                @Override
                public void onShowAllParts() {
                    showAllParts();
                }

                @Override
                public void onLoadPuppet() {
                    loadPuppet();
                }

                @Override
                public void onAddPuppet() {
                    addPuppet();
                }

                @Override
                public void onSavePuppet() {
                    savePuppet();
                }

                public void onExportAnimation() {
                    exportAnimation();
                }

                public void onImportAnimation() {
                    importAnimation();
                }

                @Override
                public void onLoadTexture() {
                    loadTexture();
                }

                @Override
                public void onSetParent() {
                    setParentBone();
                }

                @Override
                public void onAddFreeBone() {
                    addFreeBone();
                }

                @Override
                public void onClearParent() {
                    clearParentBone();
                }

                @Override
                public void onDeletePart() {
                    deletePart();
                }

                @Override
                public void onToggleBoneLines(boolean enabled) {
                    toggleBoneLines(enabled);
                }

                @Override
                public void onAddKeyframe() {
                    addKeyframe();
                }

                @Override
                public void onAddSnapshot() {
                    addSnapshot();
                }

                @Override
                public void onDeleteKeyframe() {
                    deleteKeyframe();
                }

                @Override
                public void onCopyKeyframe() {
                    copyKeyframe();
                }

                @Override
                public void onPasteKeyframe() {
                    pasteKeyframe();
                }

                @Override
                public void onUndo() {
                    performUndo();
                }

                @Override
                public void onRedo() {
                    performRedo();
                }

                @Override
                public void onCopyBone() {
                    copyBone();
                }

                @Override
                public void onPasteBone() {
                    pasteBone();
                }

                @Override
                public void onPasteBoneMirrored() {
                    pasteBoneMirrored();
                }

                @Override
                public void onGridSnapToggle(boolean enabled) {
                }

                @Override
                public void onPreviewToggle(boolean enabled) {
                }

                @Override
                public void onBillboardToggle(boolean enabled) {
                    // 切换当前选中部件的billboard状态
                    if (selectedBone != null) {
                        selectedBone.setBillboardEnabled(enabled);
                    }
                }

                @Override
                public void onTextureModeToggle(boolean multiTextureEnabled) {
                    // 切换当前选中部件的纹理模式（单贴图/多向贴图）
                    if (selectedBone != null) {
                        selectedBone.setMultiDirectionTextureEnabled(multiTextureEnabled);

                        // 刷新贴图显示
                        if (puppetTestScene != null && puppetTestScene.getPuppetRenderer() != null) {
                            EditorPuppetPartRenderer partRenderer = puppetTestScene.getPuppetRenderer()
                                .getPartRenderer(selectedBone.getName());
                            if (partRenderer != null) {
                                partRenderer.updateTextureFromBone();
                            }
                        }
                    }
                }

                @Override
                public void onRotationStripToggle(boolean enabled) {
                    // 切换当前选中部件的旋转条状贴图模式（与6方向系统互斥）
                    if (selectedBone != null) {
                        selectedBone.setRotationStripEnabled(enabled);

                        // 刷新贴图显示
                        if (puppetTestScene != null && puppetTestScene.getPuppetRenderer() != null) {
                            EditorPuppetPartRenderer partRenderer = puppetTestScene.getPuppetRenderer()
                                .getPartRenderer(selectedBone.getName());
                            if (partRenderer != null) {
                                partRenderer.updateTextureFromBone();
                            }
                        }
                    }
                }

                @Override
                public void onLoadStripTexture() {
                    loadStripTexture();
                }

                @Override
                public void onImportModel() {
                    importModelPart();
                }

                @Override
                public void onPlayPauseToggle(boolean playing) {
                    // 播放/暂停动画
                    if (playing) {
                        // 播放时退出编辑模式，允许动画应用
                        exitEditMode();
                        if (animationPlayer != null && currentClip != null) {
                            animationPlayer.resume();
                        }
                    } else {
                        if (animationPlayer != null) {
                            animationPlayer.pause();
                        }
                        // 暂停后进入编辑模式（可选）
                        // enterEditMode();
                    }
                }

                @Override
                public void onReset() {
                    // 重置动画到起始位置
                    if (animationPlayer != null) {
                        animationPlayer.setCurrentTime(0f);
                    }
                    if (editorUI != null && editorUI.getTimelinePanel() != null) {
                        editorUI.getTimelinePanel().setTime(0f);
                    }
                    // 重置按钮状态
                    if (editorUI != null && editorUI.getButtonColumnPanel() != null) {
                        editorUI.getButtonColumnPanel().setPlaying(false);
                    }
                }

                @Override
                public void onDirectionChanged(String direction) {
                    // 根据方向字符串设置相机方向
                    switch (direction.toLowerCase()) {
                        case "front":
                            currentCameraDirection = EditorBone.Direction.FRONT;
                            break;
                        case "back":
                            currentCameraDirection = EditorBone.Direction.BACK;
                            break;
                        case "left":
                            currentCameraDirection = EditorBone.Direction.LEFT;
                            break;
                        case "right":
                            currentCameraDirection = EditorBone.Direction.RIGHT;
                            break;
                        case "up":
                            currentCameraDirection = EditorBone.Direction.UP;
                            break;
                        case "down":
                            currentCameraDirection = EditorBone.Direction.DOWN;
                            break;
                    }
                    // 更新方向显示
                    editorUI.updateCurrentDirection(currentCameraDirection);
                }

                @Override
                public void onGravityDirectionChanged() {
                    cycleGravityDirection();
                }

                @Override
                public void onCameraFollowClicked() {
                    openCameraFollowDialog();
                }

                @Override
                public void onSwingEnableToggle(boolean enabled) {
                    if (selectedBone != null) {
                        selectedBone.setSwingEnabled(enabled);
                    }
                }

                @Override
                public void onSwingAxisChanged() {
                    if (selectedBone != null) {
                        Vector3f currentAxis = selectedBone.getSwingAxis();
                        Vector3f newAxis;
                        String axisName;

                        // 循环切换：Z轴（左右摆） -> X轴（前后摆） -> Y轴（扭转） -> Z轴
                        if (Math.abs(currentAxis.z - 1f) < 0.01f) {
                            newAxis = new Vector3f(1, 0, 0);
                            axisName = "X";
                        } else if (Math.abs(currentAxis.x - 1f) < 0.01f) {
                            newAxis = new Vector3f(0, 1, 0);
                            axisName = "Y";
                        } else {
                            newAxis = new Vector3f(0, 0, 1);
                            axisName = "Z";
                        }

                        selectedBone.setSwingAxis(newAxis);
                        if (editorUI != null && editorUI.getButtonColumnPanel() != null) {
                            editorUI.getButtonColumnPanel().updateSwingAxisButtonText(axisName);
                        }
                    }
                }

                @Override
                public void onAddPart() {
                    addPart();
                }

                @Override
                public void onAddPrismPart() {
                    addPrismPart();
                }
            });
        }
    }

    private void setupSliderCallbacks() {
        if (editorUI != null && editorUI.getSliderColumnPanel() != null) {
            editorUI.getSliderColumnPanel().setCallbacks(new SliderColumnPanel.SliderCallbacks() {
                @Override
                public void onWidthChanged(float value) {
                    enterEditMode();  // 进入编辑模式
                    if (selectedBone != null && editorUI.getCurrentPartRenderer() != null) {
                        editorUI.getCurrentPartRenderer().setWidth(value);
                    }
                }

                @Override
                public void onHeightChanged(float value) {
                    enterEditMode();  // 进入编辑模式
                    if (selectedBone != null && editorUI.getCurrentPartRenderer() != null) {
                        editorUI.getCurrentPartRenderer().setHeight(value);
                    }
                }

                @Override
                public void onPriorityChanged(int value) {
                    enterEditMode();  // 进入编辑模式
                    if (selectedBone != null) {
                        // 同时更新全局优先度和所有方向的优先度
                        selectedBone.setPriority(value);
                        // 更新所有方向的优先度，确保不会被后备机制覆盖
                        for (String dir : new String[]{"front", "back", "left", "right", "up", "down"}) {
                            selectedBone.setDirectionPriority(dir, value);
                        }
                        // 立即重新排序，让用户立即看到效果
                        if (puppetTestScene != null && puppetTestScene.getPuppetRenderer() != null) {
                            puppetTestScene.getPuppetRenderer().updateRenderOrder();
                        }
                    }
                }

                @Override
                public void onPosXChanged(float value) {
                    enterEditMode();  // 进入编辑模式
                    if (selectedBone != null) {
                        Vector3f pos = selectedBone.getLocalPosition();
                        selectedBone.setLocalPosition(new Vector3f(value, pos.y, pos.z));
                        // 同时更新RestPosition以确保修改被持久化
                        selectedBone.setRestPosition(new Vector3f(value, pos.y, pos.z));
                    }
                }

                @Override
                public void onPosYChanged(float value) {
                    enterEditMode();  // 进入编辑模式
                    if (selectedBone != null) {
                        Vector3f pos = selectedBone.getLocalPosition();
                        selectedBone.setLocalPosition(new Vector3f(pos.x, value, pos.z));
                        // 同时更新RestPosition以确保修改被持久化
                        selectedBone.setRestPosition(new Vector3f(pos.x, value, pos.z));
                    }
                }

                @Override
                public void onPosZChanged(float value) {
                    enterEditMode();  // 进入编辑模式
                    if (selectedBone != null) {
                        Vector3f pos = selectedBone.getLocalPosition();
                        selectedBone.setLocalPosition(new Vector3f(pos.x, pos.y, value));
                        // 同时更新RestPosition以确保修改被持久化
                        selectedBone.setRestPosition(new Vector3f(pos.x, pos.y, value));
                    }
                }

                @Override
                public void onRotXChanged(float value) {
                    enterEditMode();  // 进入编辑模式
                    if (selectedBone != null && editorUI.getCurrentPartRenderer() != null) {
                        EditorPuppetPartRenderer renderer = editorUI.getCurrentPartRenderer();
                        renderer.setCustomRotationX(value);
                    }
                }

                @Override
                public void onRotYChanged(float value) {
                    enterEditMode();  // 进入编辑模式
                    if (selectedBone != null && editorUI.getCurrentPartRenderer() != null) {
                        EditorPuppetPartRenderer renderer = editorUI.getCurrentPartRenderer();
                        renderer.setCustomRotationY(value);
                    }
                }

                @Override
                public void onRotZChanged(float value) {
                    enterEditMode();  // 进入编辑模式
                    if (selectedBone != null && editorUI.getCurrentPartRenderer() != null) {
                        EditorPuppetPartRenderer renderer = editorUI.getCurrentPartRenderer();
                        renderer.setCustomRotationZ(value);
                    }
                }

                @Override
                public void onTextureRotationChanged(float value) {
                    enterEditMode();  // 进入编辑模式
                    if (selectedBone != null && editorUI.getCurrentPartRenderer() != null) {
                        // 保存贴图旋转角度到当前方向
                        String currentDirection = selectedBone.getCurrentDirection();
                        selectedBone.setDirectionTextureRotation(currentDirection, value);

                        // 更新渲染器的UV坐标（触发重新计算旋转后的UV）
                        EditorPuppetPartRenderer renderer = editorUI.getCurrentPartRenderer();
                        renderer.updateTextureFromBone();
                    }
                }

                @Override
                public void onGridSizeChanged(float value) {
                }

                @Override
                public void onFreedomValueChanged(float value) {
                    enterEditMode();  // 进入编辑模式
                    if (selectedBone != null) {
                        selectedBone.setFreedomValue(value);
                    }
                }

                @Override
                public void onSwingAmplitudeChanged(float value) {
                    enterEditMode();  // 进入编辑模式
                    if (selectedBone != null) {
                        selectedBone.setSwingAmplitude(value);
                    }
                }

                @Override
                public void onSwingFrequencyChanged(float value) {
                    enterEditMode();  // 进入编辑模式
                    if (selectedBone != null) {
                        selectedBone.setSwingFrequency(value);
                    }
                }

                @Override
                public void onSwingPhaseChanged(float value) {
                    enterEditMode();  // 进入编辑模式
                    if (selectedBone != null) {
                        selectedBone.setSwingPhaseOffset(value);
                    }
                }

                @Override
                public void onUVChanged(float uvOffsetX, float uvOffsetY, float uvScaleX, float uvScaleY) {
                    if (selectedBone != null && editorUI.getCurrentPartRenderer() != null) {
                        EditorPuppetPartRenderer renderer = editorUI.getCurrentPartRenderer();
                        renderer.setUvOffsetX(uvOffsetX);
                        renderer.setUvOffsetY(uvOffsetY);
                        renderer.setUvScaleX(uvScaleX);
                        renderer.setUvScaleY(uvScaleY);
                        //                  ") Scale: (" + uvScaleX + ", " + uvScaleY + ")");
                    }
                }

                @Override
                public void onRotationStripSelectionChanged(int pixelX, int pixelY, int pixelWidth, int pixelHeight) {
                    if (selectedBone != null) {
                        // setStripFrameWidthPx/Height内部会按固定像素/单位比例同步重算stripWidth/stripHeight，
                        // 保证部件显示形状始终匹配取景框的像素比例，不会再出现忽宽忽窄的拉伸
                        selectedBone.setStripFrameWidthPx(pixelWidth);
                        selectedBone.setStripFrameHeightPx(pixelHeight);

                        // 立即刷新宽高滑条显示，避免数值和实际渲染尺寸不同步
                        if (editorUI != null) {
                            editorUI.updateInspector();
                        }
                    }
                }

                @Override
                public void onRotationStripCalibrated(int calibrationOffsetPx) {
                    // 把"当前摄像机朝向 <-> 当前取景框位置"的对应关系写入部件，
                    // 存在Bone上而不是贴图文件里，所以以后换贴图这个对应关系依然保留
                    if (selectedBone != null) {
                        selectedBone.setStripCalibrationOffsetPx(calibrationOffsetPx);
                    }
                }
            });
        }
    }

    /**
     * 设置部件列表面板回调
     */
    private void setupPartListCallbacks() {
        if (editorUI != null && editorUI.getPartListPanel() != null) {
            editorUI.getPartListPanel().setCallbacks(new PartListPanel.PartListCallbacks() {
                @Override
                public void onPartSelected(EditorBone bone) {
                    // 查找骨骼索引并选中（无Shift，单选模式）
                    EditorBone[] allBones = puppetTestScene.getTestSkeleton().getAllBones().toArray(new EditorBone[0]);
                    for (int i = 0; i < allBones.length; i++) {
                        if (allBones[i] == bone) {
                            selectBone(i);
                            break;
                        }
                    }
                }

                @Override
                public void onPartSelected(EditorBone bone, boolean shiftPressed) {
                    // 查找骨骼索引
                    EditorBone[] allBones = puppetTestScene.getTestSkeleton().getAllBones().toArray(new EditorBone[0]);
                    for (int i = 0; i < allBones.length; i++) {
                        if (allBones[i] == bone) {
                            // 如果是隐藏模式，切换可见性
                            if (isHideModeEnabled) {
                                toggleBoneVisibility(i);
                            } else {
                                // 否则选中部件（带Shift状态）
                                boolean originalShift = isShiftPressed;
                                isShiftPressed = shiftPressed;
                                selectBone(i);
                                isShiftPressed = originalShift;
                            }
                            break;
                        }
                    }
                }

                @Override
                public void onPartRenamed(EditorBone bone, String newName) {
                    // 调用renamePart方法来正确处理重命名（包括清理旧渲染器）
                    renamePart(bone, newName);
                }

                @Override
                public void onGroupSelected(String groupId) {
                    // 拖拽成组/点击组标题行时，同步选中状态到GroupControlPanel，
                    // 这样用户可以立即在GroupControlPanel里用XYZ按钮整体移动这个组
                    if (editorUI != null && editorUI.getGroupControlPanel() != null) {
                        editorUI.getGroupControlPanel().setSelectedGroup(groupId);
                    }
                }
            });
        }
    }

    /**
     * 设置骨骼分组控制面板回调
     * 分组功能之前完全没有接入编辑器（GroupControlPanel存在但从未被初始化/接收事件），
     * 这里补上：设置GroupManager引用，并在分组操作后刷新PartListPanel显示
     */
    private void setupGroupControlCallbacks() {
        if (editorUI == null || editorUI.getGroupControlPanel() == null) {
            return;
        }

        editorUI.getGroupControlPanel().setActionListener(new GroupControlPanel.GroupActionListener() {
            @Override
            public void onGroupCreated(com.Hecate.puppet.editor.core.EditorBoneGroup group) {
                refreshPartListPanel();
            }

            @Override
            public void onGroupDeleted(String groupId) {
                refreshPartListPanel();
            }

            @Override
            public void onBoneAddedToGroup(EditorBone bone, com.Hecate.puppet.editor.core.EditorBoneGroup group) {
                refreshPartListPanel();
            }

            @Override
            public void onBoneRemovedFromGroup(EditorBone bone, com.Hecate.puppet.editor.core.EditorBoneGroup group) {
                refreshPartListPanel();
            }

            @Override
            public void onGroupRotated(com.Hecate.puppet.editor.core.EditorBoneGroup group, int degrees) {
                // 旋转会改变部件贴图/朝向，重新收集骨骼数据供UI同步
                if (editorUI != null) {
                    editorUI.updateInspector();
                }
            }

            @Override
            public void onGroupMoved(com.Hecate.puppet.editor.core.EditorBoneGroup group, float dx, float dy, float dz) {
                // 移动组内所有成员的位置后，同步更新Inspector里显示的XYZ滑条（如果当前选中的骨骼恰好是组成员）
                if (editorUI != null) {
                    editorUI.updateInspector();
                }
            }
        });
    }

    /**
     * 刷新部件列表面板（分组结构发生变化后调用，保证白色分组边框和折叠状态及时更新）
     */
    private void refreshPartListPanel() {
        if (editorUI != null && editorUI.getPartListPanel() != null) {
            editorUI.getPartListPanel().refreshPartList();
        }
    }

    /**
     * 设置Timeline面板回调
     */
    private void setupTimelineCallbacks() {
        if (editorUI != null && editorUI.getTimelinePanel() != null) {
            editorUI.getTimelinePanel().setCallbacks(new TimelinePanel.TimelineCallbacks() {
                @Override
                public void onTimeChanged(float newTime) {
                    // 更新PuppetEditorUI的currentTime，以便addKeyframe()可以使用正确的时间
                    if (editorUI != null) {
                        editorUI.setCurrentTime(newTime);
                    }
                    if (animationPlayer != null) {
                        // 只有在非编辑模式下才应用动画预览
                        // 如果用户已经手动编辑了骨骼，保持编辑模式，不覆盖手动编辑
                        if (!animationPlayer.isEditMode()) {
                            // 非编辑模式：允许时间轴预览动画姿势
                            animationPlayer.setCurrentTime(newTime);
                            animationPlayer.seek(newTime);
                        } else {
                            // 编辑模式：只更新时间，不应用动画，保留手动编辑
                            animationPlayer.setCurrentTime(newTime);
                        }
                    }
                }

                @Override
                public void onKeyframeSelected(float keyframeTime) {
                    // 跳转到关键帧时间
                    if (animationPlayer != null) {
                        animationPlayer.setCurrentTime(keyframeTime);
                        animationPlayer.seek(keyframeTime);
                    }
                    if (editorUI != null) {
                        editorUI.setCurrentTime(keyframeTime);
                    }
                }

                @Override
                public void onScrubbingStarted() {
                    // 开始拖拽时，暂停动画播放
                    if (animationPlayer != null && animationPlayer.isPlaying()) {
                        animationPlayer.pause();
                        // 更新UI播放按钮状态
                        if (editorUI != null && editorUI.getButtonColumnPanel() != null) {
                            editorUI.getButtonColumnPanel().setPlaying(false);
                        }
                    }
                }

                @Override
                public void onScrubbingEnded() {
                    // 拖拽结束后，不自动恢复播放（让用户手动控制）
                    // 如果需要自动恢复，可以在这里添加逻辑
                }
            });
        }
    }

    /**
     * 进入编辑模式（用户手动编辑部件时调用）
     * 防止动画系统覆盖用户的编辑
     */
    private void enterEditMode() {
        if (animationPlayer != null) {
            animationPlayer.setEditMode(true);
        }
    }

    /**
     * 退出编辑模式（录制关键帧或播放动画时调用）
     */
    private void exitEditMode() {
        if (animationPlayer != null) {
            animationPlayer.setEditMode(false);
        }
    }

    /**
     * 显示所有被隐藏的部件
     */
    private void showAllParts() {
        if (puppetTestScene == null || puppetTestScene.getPuppetRenderer() == null) {
            return;
        }

        int count = hiddenParts.size();
        for (String boneName : hiddenParts) {
            EditorPuppetPartRenderer partRenderer = puppetTestScene.getPuppetRenderer()
                .getPartRenderer(boneName);
            if (partRenderer != null) {
                partRenderer.setVisible(true);
            }
        }
        hiddenParts.clear();
    }

    /**
     * 切换指定骨骼的可见性（隐藏模式下按1-9键）
     */
    private void toggleBoneVisibility(int boneIndex) {
        if (allBones == null || boneIndex < 0 || boneIndex >= allBones.length) {
            return;
        }

        if (puppetTestScene == null || puppetTestScene.getPuppetRenderer() == null) {
            return;
        }

        EditorBone bone = allBones[boneIndex];
        String boneName = bone.getName();

        EditorPuppetPartRenderer partRenderer = puppetTestScene.getPuppetRenderer()
            .getPartRenderer(boneName);

        if (partRenderer == null) {
            return;
        }

        // 切换可见性
        if (hiddenParts.contains(boneName)) {
            // 当前是隐藏的，显示它
            partRenderer.setVisible(true);
            hiddenParts.remove(boneName);
        } else {
            // 当前是显示的，隐藏它
            partRenderer.setVisible(false);
            hiddenParts.add(boneName);
        }
    }

    /**
     * 处理右键点击 - 选择部件
     */
    private void handleSelectPart(int mouseX, int mouseY) {
        if (puppetTestScene == null || puppetTestScene.getPuppetRenderer() == null) {
            return;
        }

        // 将屏幕坐标转换为3D世界射线
        com.jme3.math.Vector2f screenCoords = new com.jme3.math.Vector2f(mouseX, mouseY);
        com.jme3.math.Vector3f worldCoords = cam.getWorldCoordinates(screenCoords, 0f);
        com.jme3.math.Vector3f worldDir = cam.getWorldCoordinates(screenCoords, 1f);
        worldDir.subtractLocal(worldCoords).normalizeLocal();

        // 创建射线
        com.jme3.math.Ray ray = new com.jme3.math.Ray(worldCoords, worldDir);

        // 对所有部件进行射线投射
        float closestDistance = Float.MAX_VALUE;
        EditorBone closestBone = null;

        for (int i = 0; i < allBones.length; i++) {
            EditorBone bone = allBones[i];

            // 如果不在隐藏模式，跳过已经被隐藏的部件
            if (!isHideModeEnabled && hiddenParts.contains(bone.getName())) {
                continue;
            }

            EditorPuppetPartRenderer partRenderer = puppetTestScene.getPuppetRenderer()
                .getPartRenderer(bone.getName());

            if (partRenderer != null && partRenderer.getGeometry() != null) {
                com.jme3.collision.CollisionResults results = new com.jme3.collision.CollisionResults();
                partRenderer.getGeometry().collideWith(ray, results);

                if (results.size() > 0) {
                    float distance = results.getClosestCollision().getDistance();
                    if (distance < closestDistance) {
                        closestDistance = distance;
                        closestBone = bone;
                    }
                }
            }
        }

        // 如果找到了被点击的部件
        if (closestBone != null) {
            // 找到骨骼的索引
            for (int i = 0; i < allBones.length; i++) {
                if (allBones[i] == closestBone) {
                    // 如果是删除模式，删除该部件
                    if (isDeleteModeEnabled) {
                        executeDeletePart(i);
                    } else if (isHideModeEnabled) {
                        // 如果是隐藏模式，隐藏该部件
                        toggleBoneVisibility(i);
                    } else {
                        // 否则选择该部件
                        selectBone(i);
                    }
                    break;
                }
            }
        }
    }

    /**
     * 检查鼠标是否点击在部件上（用于判断是否启动相机拖动）
     * @return true if clicked on a part, false otherwise
     */
    private boolean checkIfClickedOnPart(int mouseX, int mouseY) {
        if (puppetTestScene == null || puppetTestScene.getPuppetRenderer() == null) {
            return false;
        }

        // 将屏幕坐标转换为3D世界射线
        com.jme3.math.Vector2f screenCoords = new com.jme3.math.Vector2f(mouseX, mouseY);
        com.jme3.math.Vector3f worldCoords = cam.getWorldCoordinates(screenCoords, 0f);
        com.jme3.math.Vector3f worldDir = cam.getWorldCoordinates(screenCoords, 1f);
        worldDir.subtractLocal(worldCoords).normalizeLocal();

        // 创建射线
        com.jme3.math.Ray ray = new com.jme3.math.Ray(worldCoords, worldDir);

        // 对所有部件进行射线投射
        for (int i = 0; i < allBones.length; i++) {
            EditorBone bone = allBones[i];

            // 跳过已经被隐藏的部件
            if (hiddenParts.contains(bone.getName())) {
                continue;
            }

            EditorPuppetPartRenderer partRenderer = puppetTestScene.getPuppetRenderer()
                .getPartRenderer(bone.getName());

            if (partRenderer != null && partRenderer.getGeometry() != null) {
                com.jme3.collision.CollisionResults results = new com.jme3.collision.CollisionResults();
                partRenderer.getGeometry().collideWith(ray, results);

                if (results.size() > 0) {
                    return true; // 点击在部件上
                }
            }
        }

        return false; // 没有点击在任何部件上
    }

    /**
     * 处理左键点击 - 设置旋转中心点
     */
    private void handleSetPivotPoint(int mouseX, int mouseY) {
        if (puppetTestScene == null || puppetTestScene.getPuppetRenderer() == null) {
            return;
        }

        // 将屏幕坐标转换为3D世界射线
        com.jme3.math.Vector2f screenCoords = new com.jme3.math.Vector2f(mouseX, mouseY);
        com.jme3.math.Vector3f worldCoords = cam.getWorldCoordinates(screenCoords, 0f);
        com.jme3.math.Vector3f worldDir = cam.getWorldCoordinates(screenCoords, 1f);
        worldDir.subtractLocal(worldCoords).normalizeLocal();

        // 创建射线
        com.jme3.math.Ray ray = new com.jme3.math.Ray(worldCoords, worldDir);

        // 对当前选中的部件进行射线投射
        if (selectedBoneIndex >= 0 && selectedBoneIndex < allBones.length) {
            EditorBone selectedBone = allBones[selectedBoneIndex];
            EditorPuppetPartRenderer partRenderer = puppetTestScene.getPuppetRenderer()
                .getPartRenderer(selectedBone.getName());

            if (partRenderer != null && partRenderer.getGeometry() != null) {
                // 进行射线投射
                com.jme3.collision.CollisionResults results = new com.jme3.collision.CollisionResults();
                partRenderer.getGeometry().collideWith(ray, results);

                if (results.size() > 0) {
                    // 获取碰撞点
                    com.jme3.collision.CollisionResult closest = results.getClosestCollision();
                    com.jme3.math.Vector3f contactPoint = closest.getContactPoint();

                    // 转换为部件的局部坐标
                    com.jme3.math.Vector3f localPoint = partRenderer.getGeometry()
                        .worldToLocal(contactPoint, new com.jme3.math.Vector3f());

                    // 设置中心点（相对于部件中心的偏移）
                    partRenderer.setPivotPoint(localPoint);

                    // 显示中心点标记
                    partRenderer.setShowPivotMarker(true);

                    // 更新Inspector显示
                    editorUI.updateInspector();
                }
            }
        }
    }

    /**
     * 保存木偶配置
     */
    private void savePuppet() {
        // 在独立线程中显示文件对话框（避免阻塞渲染线程）
        java.awt.EventQueue.invokeLater(() -> {
            // 创建置顶的父窗口
            JFrame parentFrame = createTopMostFrame();

            // 第一步：选择导出格式
            ExportFormat[] formats = ExportManager.getSupportedFormats();
            String[] formatNames = new String[formats.length];
            for (int i = 0; i < formats.length; i++) {
                formatNames[i] = formats[i].getDisplayName();
            }

            String selectedFormatName = (String) JOptionPane.showInputDialog(
                    parentFrame,
                    "请选择导出格式：",
                    "选择导出格式",
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    formatNames,
                    formatNames[0]
            );

            if (selectedFormatName == null) {
                // 用户取消了
                parentFrame.dispose();
                return;
            }

            // 找到选中的格式
            ExportFormat selectedFormat = null;
            for (ExportFormat format : formats) {
                if (format.getDisplayName().equals(selectedFormatName)) {
                    selectedFormat = format;
                    break;
                }
            }

            if (selectedFormat == null) {
                parentFrame.dispose();
                return;
            }

            final ExportFormat exportFormat = selectedFormat;

            // 第二步：选择保存位置
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("保存/导出木偶 - " + exportFormat.getDisplayName());
            fileChooser.setFileFilter(new FileNameExtensionFilter(
                    exportFormat.getDescription(),
                    exportFormat.getExtension()
            ));

            // 如果有当前文件，默认使用该路径（但改变扩展名）
            if (currentFilePath != null) {
                File currentFile = new File(currentFilePath);
                String baseName = currentFile.getName().replaceAll("\\.[^.]+$", "");
                fileChooser.setSelectedFile(new File(
                        currentFile.getParent(),
                        baseName + "." + exportFormat.getExtension()
                ));
            } else {
                fileChooser.setSelectedFile(new File("puppet." + exportFormat.getExtension()));
            }

            int result = fileChooser.showSaveDialog(parentFrame);

            // 关闭临时父窗口
            parentFrame.dispose();

            if (result == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                String path = file.getAbsolutePath();

                // 确保有正确的扩展名
                String extension = "." + exportFormat.getExtension();
                if (!path.toLowerCase().endsWith(extension)) {
                    path += extension;
                    file = new File(path);
                }

                // 只有在保存为puppet格式时才更新currentFilePath


                // 在渲染线程中执行导出
                final String finalPath = path;
                enqueue(() -> {
                    try {

                        ExportManager.export(
                                exportFormat,
                                puppetTestScene.getTestSkeleton(),
                                puppetTestScene.getPuppetRenderer(),
                                finalPath
                        );
                        if(exportFormat==ExportFormat.PUPPET||exportFormat==ExportFormat.PUPPET_PACKAGE)currentFilePath=finalPath;
                        java.awt.EventQueue.invokeLater(()->EditorDialogs.message("导出完成：\n"+finalPath));
                    } catch (Exception e) {
                        e.printStackTrace();
                        java.awt.EventQueue.invokeLater(()->EditorDialogs.message("导出失败：\n"+e.getMessage()));
                    }
                    return null;
                });
            }
        });
    }

    /**
     * 加载木偶配置
     */
    private void loadPuppet() {
        // 在独立线程中显示文件对话框
        java.awt.EventQueue.invokeLater(() -> {
            // 创建置顶的父窗口
            JFrame parentFrame = createTopMostFrame();

            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("加载木偶配置");

            // 支持三种文件格式
            fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("Puppet打包文件 (*.ppkg)", "ppkg"));
            fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("Puppet文件 (*.puppet)", "puppet"));
            fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("JSON文件 (*.json)", "json"));
            fileChooser.setFileFilter(new FileNameExtensionFilter("所有支持的文件 (*.ppkg, *.puppet, *.json)", "ppkg", "puppet", "json"));

            // 如果有当前文件，默认使用该目录
            if (currentFilePath != null) {
                fileChooser.setCurrentDirectory(new File(currentFilePath).getParentFile());
            }

            int result = fileChooser.showOpenDialog(parentFrame);

            // 关闭临时父窗口
            parentFrame.dispose();

            if (result == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                String path = file.getAbsolutePath();



                // 在渲染线程中执行加载
                final String finalPath = path;
                enqueue(() -> {
                    try {
                        PuppetConfig config = PuppetIO.loadFromFile(finalPath);
                        EditorSkeleton newSkeleton = new EditorSkeleton(config.getName() != null ? config.getName() : "LoadedPuppet");
                        EditorPuppetRenderer newRenderer = new EditorPuppetRenderer(this, newSkeleton);
                        PuppetIO.applyConfig(config, newSkeleton, newRenderer);
                        if (puppetTestScene.getPuppetRenderer() != null) puppetTestScene.getPuppetRenderer().cleanup();
                        newRenderer.attachToScene(rootNode);
                        newRenderer.setWorldPosition(new Vector3f(0, 5, 0));
                        puppetTestScene.setTestSkeleton(newSkeleton);
                        puppetTestScene.setPuppetRenderer(newRenderer);
                        commandManager.clear();
                        selectedBones.clear();selectedBone=null;selectedBoneIndex=-1;hiddenParts.clear();
                        if (volumeMode) {initializeVolumeModeParts(false);if(newSkeleton.getAllBones().stream().anyMatch(b->b.getVolumeDefinition()!=null&&b.getVolumeDefinition().isStrip3DEnabled()))newRenderer.setVolumeViewMode(com.Hecate.puppet.volume.VolumeReferenceView.Mode.APPEARANCE);}
                        initializeAnimationSystem();

                        // 【关键修复】更新UI的skeleton引用，否则UI组件仍然引用旧的skeleton
                        if (editorUI != null) {
                            editorUI.setSkeleton(puppetTestScene.getTestSkeleton());
                        }

                        // 重新获取骨骼数组（骨架已被重建）
                        List<EditorBone> newBonesList = puppetTestScene.getTestSkeleton().getAllBones();
                        allBones = newBonesList.toArray(new EditorBone[0]);

                        // 刷新 PartListPanel（重建骨骼列表）
                        if (editorUI != null && editorUI.getPartListPanel() != null) {
                            editorUI.getPartListPanel().refreshPartList();
                        }

                        // 选择第一个骨骼
                        if (allBones.length > 0) {
                            selectBone(0);
                        }
                        currentFilePath=finalPath;
                    } catch (Exception e) {
                        e.printStackTrace();

                        // 显示错误对话框
                        java.awt.EventQueue.invokeLater(()->EditorDialogs.message("加载木偶失败：\n"+e.getMessage()));
                    }
                    return null;
                });
            }
        });
    }

    /**
     * 添加木偶配置（不清除现有模型）
     */
    private void addPuppet() {
        // 在独立线程中显示文件对话框
        new Thread(() -> {
            // 创建置顶的父窗口
            JFrame parentFrame = createTopMostFrame();

            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("添加木偶配置");
            fileChooser.setFileFilter(new FileNameExtensionFilter("Puppet Files (*.puppet)", "puppet"));

            // 如果有当前文件，默认使用该目录
            if (currentFilePath != null) {
                fileChooser.setCurrentDirectory(new File(currentFilePath).getParentFile());
            }

            int result = fileChooser.showOpenDialog(parentFrame);

            // 关闭临时父窗口
            parentFrame.dispose();

            if (result == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                String path = file.getAbsolutePath();

                // 在渲染线程中执行添加
                final String finalPath = path;
                enqueue(() -> {
                    try {
                        PuppetConfig config = PuppetIO.loadFromFile(finalPath);

                        // 使用addConfig方法添加而不清除现有模型
                        PuppetIO.addConfig(
                            config,
                            puppetTestScene.getTestSkeleton(),
                            puppetTestScene.getPuppetRenderer()
                        );

                        if(volumeMode){var mode=puppetTestScene.getPuppetRenderer().getVolumeViewMode();initializeVolumeModeParts(false);puppetTestScene.getPuppetRenderer().setVolumeViewMode(mode);}
                        // 重新获取骨骼数组（新骨骼已添加）
                        List<EditorBone> newBonesList = puppetTestScene.getTestSkeleton().getAllBones();
                        allBones = newBonesList.toArray(new EditorBone[0]);

                        // 刷新 PartListPanel（更新骨骼列表）
                        if (editorUI != null && editorUI.getPartListPanel() != null) {
                            editorUI.getPartListPanel().refreshPartList();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();

                        // 显示错误对话框
                        javax.swing.JOptionPane.showMessageDialog(
                            null,
                            "添加木偶失败：\n" + e.getMessage(),
                            "添加错误",
                            javax.swing.JOptionPane.ERROR_MESSAGE
                        );
                    }
                    return null;
                });
            }
        }).start();
    }

    /**
     * 导出动画
     */
    private void exportAnimation() {
        new Thread(() -> {
            JFrame parentFrame = createTopMostFrame();

            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("导出动画");
            fileChooser.setFileFilter(new FileNameExtensionFilter("Animation Files (*.anim)", "anim"));

            if (currentFilePath != null) {
                File currentFile = new File(currentFilePath);
                String baseName = currentFile.getName().replaceAll("\\.[^.]+$", "");
                fileChooser.setSelectedFile(new File(
                        currentFile.getParent(),
                        baseName + ".anim"
                ));
            } else {
                fileChooser.setSelectedFile(new File("animation.anim"));
            }

            int result = fileChooser.showSaveDialog(parentFrame);
            parentFrame.dispose();

            if (result == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                String path = file.getAbsolutePath();

                if (!path.toLowerCase().endsWith(".anim")) {
                    path += ".anim";
                    file = new File(path);
                }

                final String finalPath = path;
                enqueue(() -> {
                    try {
                        // 导出动画配置
                        com.Hecate.puppet.config.AnimationConfig animConfig =
                            com.Hecate.puppet.config.AnimationIO.exportAnimation(
                                currentClip,
                                puppetTestScene.getTestSkeleton().getBaseSkeleton()
                            );

                        // 保存到文件
                        com.Hecate.puppet.config.AnimationIO.saveAnimation(animConfig, finalPath);

                        javax.swing.JOptionPane.showMessageDialog(
                            null,
                            "动画导出成功！\n文件: " + finalPath,
                            "导出成功",
                            javax.swing.JOptionPane.INFORMATION_MESSAGE
                        );
                    } catch (Exception e) {
                        e.printStackTrace();
                        javax.swing.JOptionPane.showMessageDialog(
                            null,
                            "导出动画失败：\n" + e.getMessage(),
                            "导出错误",
                            javax.swing.JOptionPane.ERROR_MESSAGE
                        );
                    }
                    return null;
                });
            }
        }).start();
    }

    /**
     * 导入动画
     */
    private void importAnimation() {
        new Thread(() -> {
            JFrame parentFrame = createTopMostFrame();

            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("导入动画");
            fileChooser.setFileFilter(new FileNameExtensionFilter("Animation Files (*.anim)", "anim"));

            if (currentFilePath != null) {
                fileChooser.setCurrentDirectory(new File(currentFilePath).getParentFile());
            }

            int result = fileChooser.showOpenDialog(parentFrame);
            parentFrame.dispose();

            if (result == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                String path = file.getAbsolutePath();

                final String finalPath = path;
                enqueue(() -> {
                    try {
                        // 加载动画配置
                        com.Hecate.puppet.config.AnimationConfig animConfig =
                            com.Hecate.puppet.config.AnimationIO.loadAnimation(finalPath);

                        // 获取动画骨骼名称集合
                        java.util.Set<String> animBoneNames = animConfig.getAllBoneNames();

                        // 获取当前木偶骨骼列表
                        java.util.List<EditorBone> puppetBones = puppetTestScene.getTestSkeleton().getAllBones();

                        // 自动匹配骨骼
                        com.Hecate.puppet.config.BoneMappingConfig mapping =
                            com.Hecate.puppet.config.BoneMatcher.autoMatchEditorBones(animBoneNames, puppetBones);

                        // 验证映射
                        boolean isValid = com.Hecate.puppet.config.AnimationIO.validateMapping(
                            animConfig, mapping, puppetTestScene.getTestSkeleton().getBaseSkeleton()
                        );

                        if (!isValid) {
                            javax.swing.JOptionPane.showMessageDialog(
                                null,
                                "骨骼映射验证失败！\n某些骨骼无法匹配到当前木偶。\n请检查骨骼结构。",
                                "映射错误",
                                javax.swing.JOptionPane.WARNING_MESSAGE
                            );
                            return null;
                        }

                        // 获取映射覆盖率
                        float coverage = mapping.getMappingCoverage(animBoneNames);

                        // 如果覆盖率不是100%，显示映射编辑器让用户手动调整
                        com.Hecate.puppet.config.BoneMappingConfig finalMapping = mapping;
                        if (coverage < 100.0f) {
                            // 在EDT线程中显示对话框
                            final com.Hecate.puppet.config.BoneMappingConfig[] dialogResult =
                                new com.Hecate.puppet.config.BoneMappingConfig[1];
                            final java.util.concurrent.CountDownLatch latch =
                                new java.util.concurrent.CountDownLatch(1);

                            javax.swing.SwingUtilities.invokeLater(() -> {
                                try {
                                    dialogResult[0] = BoneMappingDialog.showDialog(
                                        null,
                                        animConfig,
                                        mapping,
                                        puppetBones
                                    );
                                } finally {
                                    latch.countDown();
                                }
                            });

                            // 等待对话框关闭
                            try {
                                latch.await();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                            // 如果用户取消了对话框，返回
                            if (dialogResult[0] == null) {
                                return null;
                            }

                            finalMapping = dialogResult[0];
                            coverage = finalMapping.getMappingCoverage(animBoneNames);
                        }

                        // 获取最终映射统计信息
                        String stats = com.Hecate.puppet.config.AnimationIO.getMappingStatistics(
                            animConfig, finalMapping
                        );

                        // 询问用户是否继续
                        int confirm = javax.swing.JOptionPane.showConfirmDialog(
                            null,
                            "动画导入信息：\n" +
                            "动画名称: " + animConfig.getName() + "\n" +
                            "动画时长: " + animConfig.getDuration() + "秒\n" +
                            "关键帧数: " + animConfig.getKeyframes().size() + "\n" +
                            stats + "\n\n" +
                            "是否导入此动画？",
                            "确认导入",
                            javax.swing.JOptionPane.YES_NO_OPTION,
                            javax.swing.JOptionPane.QUESTION_MESSAGE
                        );

                        // 使用 finalMapping 引用（下面的代码会使用）
                        final com.Hecate.puppet.config.BoneMappingConfig mappingToUse = finalMapping;

                        if (confirm == javax.swing.JOptionPane.YES_OPTION) {
                            // 应用动画（使用用户调整后的映射）
                            AnimationClip importedClip = com.Hecate.puppet.config.AnimationIO.applyAnimation(
                                animConfig, mappingToUse
                            );

                            // 替换当前动画
                            currentClip = importedClip;
                            animationPlayer.setCurrentClip(currentClip);

                            // 更新Timeline
                            if (editorUI != null && editorUI.getTimelinePanel() != null) {
                                editorUI.getTimelinePanel().setAnimationClip(currentClip);
                            }

                            javax.swing.JOptionPane.showMessageDialog(
                                null,
                                "动画导入成功！\n" + stats,
                                "导入成功",
                                javax.swing.JOptionPane.INFORMATION_MESSAGE
                            );
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        javax.swing.JOptionPane.showMessageDialog(
                            null,
                            "导入动画失败：\n" + e.getMessage(),
                            "导入错误",
                            javax.swing.JOptionPane.ERROR_MESSAGE
                        );
                    }
                    return null;
                });
            }
        }).start();
    }

    /**
     * 加载纹理到当前选中的部件
     */
    private void loadTexture() {
        // 检查是否有选中的骨骼
        if (selectedBoneIndex < 0 || selectedBoneIndex >= allBones.length) {
            return;
        }

        EditorBone selectedBone = allBones[selectedBoneIndex];
        final EditorPuppetPartRenderer partRenderer = puppetTestScene.getPuppetRenderer()
            .getPartRenderer(selectedBone.getName());

        if (partRenderer == null) {
            return;
        }

        // 在独立线程中显示文件对话框
        new Thread(() -> {
            // 创建置顶的父窗口
            JFrame parentFrame = createTopMostFrame();

            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("选择纹理文件");
            fileChooser.setFileFilter(new FileNameExtensionFilter(
                "Image Files (*.png, *.jpg, *.jpeg)", "png", "jpg", "jpeg"));

            // 设置默认目录为资源目录
            // 优先使用工作目录下的Textures文件夹（适用于独立安装包）
            File defaultDir = new File("Textures");
            if (!defaultDir.exists()) {
                // 如果不存在，尝试开发环境路径
                defaultDir = new File("src/main/resources/Textures");
            }
            if (defaultDir.exists()) {
                fileChooser.setCurrentDirectory(defaultDir);
            }

            int result = fileChooser.showOpenDialog(parentFrame);

            // 关闭临时父窗口
            parentFrame.dispose();

            if (result == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                String absolutePath = file.getAbsolutePath();

                // 尝试转换为相对于 resources 的路径
                String texturePath = convertToResourcePath(absolutePath);

                // 在渲染线程中加载纹理
                final String finalPath = texturePath;
                enqueue(() -> {
                    // 加载纹理到渲染器
                    partRenderer.loadTexture(finalPath);

                    // 保存贴图路径到当前方向
                    String currentDirection = selectedBone.getCurrentDirection();
                    selectedBone.setDirectionTexture(currentDirection, finalPath);

                    // 更新纹理预览面板
                    if (editorUI != null && editorUI.getSliderColumnPanel() != null &&
                        editorUI.getSliderColumnPanel().getTexturePreviewPanel() != null) {
                        editorUI.getSliderColumnPanel().getTexturePreviewPanel().setTexture(partRenderer.getTexture());
                        editorUI.getSliderColumnPanel().getTexturePreviewPanel().setUV(
                            partRenderer.getUvOffsetX(),
                            partRenderer.getUvOffsetY(),
                            partRenderer.getUvScaleX(),
                            partRenderer.getUvScaleY()
                        );
                    }

                    return null;
                });
            }
        }).start();
    }

    /**
     * 加载旋转条状贴图到当前选中的部件（伪3D棱柱效果）
     * 与loadTexture()不同：贴图路径存到bone.stripTexturePath，而不是方向贴图map
     */
    private void loadStripTexture() {
        if (selectedBoneIndex < 0 || selectedBoneIndex >= allBones.length) {
            return;
        }

        EditorBone selectedBone = allBones[selectedBoneIndex];
        final EditorPuppetPartRenderer partRenderer = puppetTestScene.getPuppetRenderer()
            .getPartRenderer(selectedBone.getName());

        if (partRenderer == null) {
            return;
        }

        new Thread(() -> {
            JFrame parentFrame = createTopMostFrame();

            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("选择旋转条状贴图文件");
            fileChooser.setFileFilter(new FileNameExtensionFilter(
                "Image Files (*.png, *.jpg, *.jpeg)", "png", "jpg", "jpeg"));

            File defaultDir = new File("Textures");
            if (!defaultDir.exists()) {
                defaultDir = new File("src/main/resources/Textures");
            }
            if (defaultDir.exists()) {
                fileChooser.setCurrentDirectory(defaultDir);
            }

            int result = fileChooser.showOpenDialog(parentFrame);
            parentFrame.dispose();

            if (result == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                String absolutePath = file.getAbsolutePath();
                String texturePath = convertToResourcePath(absolutePath);

                final String finalPath = texturePath;
                enqueue(() -> {
                    selectedBone.setStripTexturePath(finalPath);
                    // 加载条状贴图后自动开启旋转条状贴图模式，否则贴图会被6方向系统忽略而不显示
                    selectedBone.setRotationStripEnabled(true);
                    partRenderer.updateTextureFromBone();

                    // 同步左侧"旋转条:开/关"按钮的显示状态
                    if (editorUI != null && editorUI.getButtonColumnPanel() != null) {
                        editorUI.getButtonColumnPanel().updateRotationStripButton(true);
                    }

                    // 打开选区框面板并自动加载新贴图，方便直接框选
                    if (editorUI != null && editorUI.getSliderColumnPanel() != null &&
                        editorUI.getSliderColumnPanel().getRotationStripSelectorPanel() != null) {
                        RotationStripSelectorPanel panel = editorUI.getSliderColumnPanel().getRotationStripSelectorPanel();
                        panel.setTexture(partRenderer.getTexture());
                        panel.setSelection(
                            0, 0,
                            selectedBone.getStripFrameWidthPx(),
                            selectedBone.getStripFrameHeightPx()
                        );
                        panel.setCalibrationOffsetPx(selectedBone.getStripCalibrationOffsetPx());
                        panel.setLivePreviewTarget(partRenderer);
                        panel.show();
                    }

                    return null;
                });
            }
        }).start();
    }

    /**
     * 尝试将绝对路径转换为 jME3 资源路径
     * 支持三种路径格式：
     * 1. resources目录下的相对路径（如 "Textures/blocks/grass.png"）
     * 2. 项目外部的绝对路径（返回原始路径，由loadTexture使用BufferedImage加载）
     * 3. 项目内部但非resources的路径（去掉盘符，配合FileLocator解析）
     *
     * 修复说明：当路径包含特殊字符（如括号）时，保留绝对路径让loadTexture()
     * 使用BufferedImage直接加载，绕过AssetManager的路径解析问题
     */
    private String convertToResourcePath(String absolutePath) {
        // 将路径标准化（统一使用正斜杠）
        String normalized = absolutePath.replace('\\', '/');

        // 优先检查 resources 目录（标准资源路径）
        int resourcesIndex = normalized.indexOf("/resources/");
        if (resourcesIndex != -1) {
            // 提取 resources 之后的路径
            String resourcePath = normalized.substring(resourcesIndex + "/resources/".length());
            return resourcePath;
        }

        // 检查文件是否存在且为绝对路径
        java.io.File file = new java.io.File(absolutePath);
        if (file.exists() && file.isAbsolute()) {
            // 检查路径是否包含特殊字符（括号、空格等）
            // 这些字符可能导致 AssetManager 的 FileLocator 解析失败
            if (normalized.contains("(") || normalized.contains(")") ||
                normalized.contains(" ") || normalized.contains("%")) {
                // 返回原始绝对路径，让 loadTexture() 使用 BufferedImage 直接加载
                return absolutePath;
            }
        }

        // 找不到 resources 目录且无特殊字符：去掉盘符/根目录前缀
        // 配合simpleInitApp()中为每个盘符（Windows）或"/"（Unix）注册的FileLocator解析
        if (normalized.matches("^[A-Za-z]:/.*")) {
            // Windows路径："C:/Users/xxx/foo.png" -> "Users/xxx/foo.png"
            return normalized.substring(3);
        } else if (normalized.startsWith("/")) {
            // Unix绝对路径："/home/xxx/foo.png" -> "home/xxx/foo.png"
            return normalized.substring(1);
        } else {
            // 相对路径（没有盘符前缀），转换为绝对路径后再去掉盘符
            String absPath = file.getAbsolutePath().replace('\\', '/');
            if (absPath.matches("^[A-Za-z]:/.*")) {
                return absPath.substring(3);
            } else if (absPath.startsWith("/")) {
                return absPath.substring(1);
            }
            return absPath;
        }
    }

    /**
     * 加载指定方向的纹理
     */
    private void loadDirectionTexture(String direction) {
        // 检查是否有选中的骨骼
        if (selectedBoneIndex < 0 || selectedBoneIndex >= allBones.length) {
            return;
        }

        EditorBone selectedBone = allBones[selectedBoneIndex];
        final EditorPuppetPartRenderer partRenderer = puppetTestScene.getPuppetRenderer()
            .getPartRenderer(selectedBone.getName());

        if (partRenderer == null) {
            return;
        }

        // 在独立线程中显示文件对话框
        new Thread(() -> {
            // 创建置顶的父窗口
            JFrame parentFrame = createTopMostFrame();

            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("选择 " + direction.toUpperCase() + " 方向纹理");
            fileChooser.setFileFilter(new FileNameExtensionFilter(
                "Image Files (*.png, *.jpg, *.jpeg)", "png", "jpg", "jpeg"));

            // 设置默认目录为资源目录
            // 优先使用工作目录下的Textures文件夹（适用于独立安装包）
            File defaultDir = new File("Textures");
            if (!defaultDir.exists()) {
                // 如果不存在，尝试开发环境路径
                defaultDir = new File("src/main/resources/Textures");
            }
            if (defaultDir.exists()) {
                fileChooser.setCurrentDirectory(defaultDir);
            }

            int result = fileChooser.showOpenDialog(parentFrame);

            // 关闭临时父窗口
            parentFrame.dispose();

            if (result == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                String absolutePath = file.getAbsolutePath();

                // 尝试转换为相对于 resources 的路径
                String texturePath = convertToResourcePath(absolutePath);

                // 在渲染线程中加载纹理
                final String finalPath = texturePath;
                enqueue(() -> {
                    // 保存贴图路径到指定方向
                    selectedBone.setDirectionTexture(direction, finalPath);

                    // 如果当前方向就是这个方向，立即更新渲染器
                    if (selectedBone.getCurrentDirection().equals(direction)) {
                        partRenderer.loadTexture(finalPath);
                    }

                    // 更新Inspector面板和DirectionTexturePanel显示
                    // if (editorUI != null && editorUI.getInspectorPanel() != null) { // COMMENTED OUT - OLD UI
                        // editorUI.getInspectorPanel().updateDisplay(); // COMMENTED OUT - OLD UI
                    // } // COMMENTED OUT - OLD UI
                    // if (editorUI != null && editorUI.getDirectionTexturePanel() != null) { // COMMENTED OUT - OLD UI
                        // editorUI.getDirectionTexturePanel().updateDisplay(); // COMMENTED OUT - OLD UI
                    // } // COMMENTED OUT - OLD UI

                    return null;
                });
            }
        }).start();
    }

    /**
     * 清除指定方向的纹理
     */
    private void clearDirectionTexture(String direction) {
        // 检查是否有选中的骨骼
        if (selectedBoneIndex < 0 || selectedBoneIndex >= allBones.length) {
            return;
        }

        EditorBone selectedBone = allBones[selectedBoneIndex];
        final EditorPuppetPartRenderer partRenderer = puppetTestScene.getPuppetRenderer()
            .getPartRenderer(selectedBone.getName());

        if (partRenderer == null) {
            return;
        }

        // 清除指定方向的纹理
        selectedBone.setDirectionTexture(direction, null);

        // 如果当前方向就是这个方向，需要重新加载纹理（使用fallback）
        if (selectedBone.getCurrentDirection().equals(direction)) {
            String fallbackTexture = selectedBone.getCurrentDirectionTexture();
            if (fallbackTexture != null && !fallbackTexture.isEmpty()) {
                partRenderer.loadTexture(fallbackTexture);
            }
            // 如果没有fallback纹理，保持当前状态不变
        }

        // 更新Inspector面板显示
        // if (editorUI != null && editorUI.getInspectorPanel() != null) { // COMMENTED OUT - OLD UI
            // editorUI.getInspectorPanel().updateDisplay(); // COMMENTED OUT - OLD UI
        // } // COMMENTED OUT - OLD UI
    }

    /**
     * 切换骨骼连线显示
     */
    private void toggleBoneLines(boolean enabled) {
        if (puppetTestScene == null || puppetTestScene.getPuppetRenderer() == null) {
            return;
        }

        if(volumeMode){volumeBonesVisible=enabled;puppetTestScene.getPuppetRenderer().setShowBoneConnections(false);}
        else puppetTestScene.getPuppetRenderer().setShowBoneConnections(enabled);
    }

    /**
     * 添加新的部件
     */
    private void addPart() {
        if (volumeMode) { requestVolumePart(); return; }
        if (puppetTestScene == null || puppetTestScene.getTestSkeleton() == null) {
            return;
        }

        // 生成新部件名称
        int partCount = puppetTestScene.getTestSkeleton().getBoneCount();
        String newPartName = "Part_" + (partCount + 1);

        // 检查名称是否已存在
        int suffix = 1;
        while (puppetTestScene.getTestSkeleton().findBone(newPartName) != null) {
            suffix++;
            newPartName = "Part_" + (partCount + suffix);
        }

        // 创建新骨骼
        EditorBone newBone = new EditorBone(newPartName);

        // 【修复】显式启用billboard，确保新模型在旋转时正确面向摄像机
        newBone.setBillboardEnabled(true);

        // 【修复】只初始化front方向，其他方向保持null以支持继承
        // Only initialize front direction, other directions will inherit from it
        // This allows the inheritance system to work: when user modifies front to 3.0f,
        // other directions (back/left/right/up/down) will automatically inherit 3.0f
        // because they have null values and will use getSourceDirectionForAttribute()
        String frontKey = EditorBone.Direction.FRONT.getKey();
        // 设置默认UV坐标（全图）
        newBone.setDirectionUV(frontKey, 0f, 0f, 1f, 1f);
        // 设置默认尺寸（2.0 x 2.0）
        newBone.setDirectionWidth(frontKey, 2.0f);
        newBone.setDirectionHeight(frontKey, 2.0f);
        // 设置默认优先级
        newBone.setDirectionPriority(frontKey, 0);
        // 设置默认偏移（0, 0, 0）
        newBone.setDirectionOffset(frontKey, 0f, 0f, 0f);
        // 设置默认旋转（0, 0, 0）
        newBone.setDirectionRotation(frontKey, 0f, 0f, 0f);

        // 设置默认变换
        newBone.setRestPosition(new Vector3f(0, 0, 0));
        newBone.setRestRotation(new com.jme3.math.Quaternion());
        newBone.setRestScale(new Vector3f(1, 1, 1));
        newBone.resetToRestPose();

        // 检查是否有根骨骼
        EditorBone rootBone = puppetTestScene.getTestSkeleton().getRootBone();
        if (rootBone == null) {
            // 如果骨架为空，将新骨骼设为根骨骼
            puppetTestScene.getTestSkeleton().setRootBone(newBone);
        } else {
            // 否则，将新骨骼添加到根骨骼作为子骨骼
            rootBone.addChild(newBone);
        }

        // 添加到骨架
        puppetTestScene.getTestSkeleton().addBone(newBone);

        // 创建部件渲染器
        puppetTestScene.getPuppetRenderer().addPartRenderer(newBone);

        // 设置为正方形尺寸（2.0 x 2.0）
        EditorPuppetPartRenderer newPartRenderer = puppetTestScene.getPuppetRenderer().getPartRenderer(newPartName);
        if (newPartRenderer != null) {
            newPartRenderer.setSize(2.0f, 2.0f);
        }

        // 重新收集所有骨骼
        collectAllBones();

        // 刷新骨骼连线显示
        if (puppetTestScene != null && puppetTestScene.getPuppetRenderer() != null) {
            puppetTestScene.getPuppetRenderer().refreshBoneConnections();
        }

        // 刷新部件列表面板
        if (editorUI != null && editorUI.getPartListPanel() != null) {
            editorUI.getPartListPanel().refreshPartList();
        }

        // 自动选择新创建的部件
        for (int i = 0; i < allBones.length; i++) {
            if (allBones[i].getName().equals(newPartName)) {
                selectBone(i);
                break;
            }
        }
    }

    /**
     * 导入OBJ模型，创建一个新的"3D模型骨骼"部件（与普通Quad部件是完全不同的一条渲染路径，
     * 详见Bone.isModelEnabled()）。这个新部件默认挂在当前骨架根骨骼下（没有根骨骼则自己
     * 成为根骨骼），跟普通"添加部件"的挂载规则一致。
     *
     * 目前只支持挂"整个模型固定跟随一个变换"——如果以后想让脸贴在模型上会动的某个部位，
     * 需要模型自带命名锚点/子节点，那是后续扩展，不在这次范围内（见Bone.java里
     * modelEnabled字段上方的扩展点注释）。
     *
     * 导入完成后，用户可以用现有的"设置父骨骼"功能（两步选择模式），把一个普通的Quad部件
     * （比如脸）挂到这个新模型骨骼下面成为子骨骼——子骨骼的世界变换由Bone.getWorldTransform()
     * 统一递归计算，不需要为模型骨骼做任何特殊处理，父子关系机制是完全通用的。
     */
    private void importModelPart() {
        if (puppetTestScene == null || puppetTestScene.getTestSkeleton() == null) {
            return;
        }

        new Thread(() -> {
            JFrame parentFrame = createTopMostFrame();

            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("选择OBJ模型文件");
            fileChooser.setFileFilter(new FileNameExtensionFilter("OBJ Model Files (*.obj)", "obj"));

            File defaultDir = new File("Models");
            if (!defaultDir.exists()) {
                defaultDir = new File("src/main/resources/Models");
            }
            if (defaultDir.exists()) {
                fileChooser.setCurrentDirectory(defaultDir);
            }

            int result = fileChooser.showOpenDialog(parentFrame);
            parentFrame.dispose();

            if (result == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                String absolutePath = file.getAbsolutePath();
                String modelPath = convertToResourcePath(absolutePath);

                final String finalPath = modelPath;
                enqueue(() -> {
                    createModelBone(finalPath);
                    return null;
                });
            }
        }).start();
    }

    /**
     * 创建一个新的3D模型骨骼，加载指定路径的OBJ模型
     */
    private void createModelBone(String modelPath) {
        // 生成新部件名称（与addPart()同款命名/去重规则）
        int partCount = puppetTestScene.getTestSkeleton().getBoneCount();
        String newPartName = "Model_" + (partCount + 1);
        int suffix = 1;
        while (puppetTestScene.getTestSkeleton().findBone(newPartName) != null) {
            suffix++;
            newPartName = "Model_" + (partCount + suffix);
        }

        EditorBone newBone = new EditorBone(newPartName);

        // 模型骨骼固定朝向，不跟随摄像机旋转——模型本身有正反面，billboard会导致穿模/朝向错乱
        newBone.setBillboardEnabled(false);

        newBone.setModelEnabled(true);
        newBone.setModelFilePath(modelPath);

        // 设置默认变换
        newBone.setRestPosition(new Vector3f(0, 0, 0));
        newBone.setRestRotation(new com.jme3.math.Quaternion());
        newBone.setRestScale(new Vector3f(1, 1, 1));
        newBone.resetToRestPose();

        // 检查是否有根骨骼（挂载规则与addPart()一致）
        EditorBone rootBone = puppetTestScene.getTestSkeleton().getRootBone();
        if (rootBone == null) {
            puppetTestScene.getTestSkeleton().setRootBone(newBone);
        } else {
            rootBone.addChild(newBone);
        }

        puppetTestScene.getTestSkeleton().addBone(newBone);

        // 创建部件渲染器（内部会因为bone.isModelEnabled()走模型加载路径，不创建Quad）
        puppetTestScene.getPuppetRenderer().addPartRenderer(newBone);

        collectAllBones();

        if (puppetTestScene.getPuppetRenderer() != null) {
            puppetTestScene.getPuppetRenderer().refreshBoneConnections();
        }

        if (editorUI != null && editorUI.getPartListPanel() != null) {
            editorUI.getPartListPanel().refreshPartList();
        }

        // 自动选择新创建的部件
        for (int i = 0; i < allBones.length; i++) {
            if (allBones[i].getName().equals(newPartName)) {
                selectBone(i);
                break;
            }
        }
    }

    /**
     * 添加新的棱柱（由多个独立普通部件组成，打包成一个组/包）
     * 弹窗让用户选择棱柱边数（10/14）和长宽高，生成N个侧面+1个顶+1个底，
     * 共N+2个独立的普通QUAD部件（billboard关闭，固定朝向，不跟随镜头旋转），
     * 每个面都可以单独选中、贴图、调UV，然后把它们打包进一个组（复用拖拽分组功能）。
     * 整体移动只能通过GroupControlPanel的批量XYZ按钮。
     */
    private void addPrismPart() {
        if (puppetTestScene == null || puppetTestScene.getTestSkeleton() == null) {
            return;
        }

        AddPrismDialog dialog = new AddPrismDialog(null);
        dialog.setVisible(true);

        if (!dialog.isConfirmed()) {
            return;
        }

        int sideCount = dialog.getSideCount();
        float prismW = dialog.getPrismWidth();
        float prismD = dialog.getPrismDepth();
        float prismH = dialog.getPrismHeight();

        EditorSkeleton skeleton = puppetTestScene.getTestSkeleton();
        EditorPuppetRenderer renderer = puppetTestScene.getPuppetRenderer();
        String namePrefix = "Prism" + sideCount + "_" + (skeleton.getBoneCount() + 1) + "_";

        // parentBone为null时表示骨架为空，第一个生成的面会被设为根骨骼，
        // 之后所有面都挂在这第一个面下面（避免每个面都想当根骨骼互相覆盖）
        EditorBone parentBone = skeleton.getRootBone();

        java.util.List<EditorBone> faceBones = new java.util.ArrayList<>();

        // 生成N个侧面：每个面是一个标准QUAD部件，围绕中心摆成正N边形棱柱的侧壁
        // 侧面宽度按弦长近似（正N边形边长），高度=用户输入的高
        float halfW = prismW / 2f;
        float halfD = prismD / 2f;
        double angleStep = Math.PI * 2.0 / sideCount;
        for (int i = 0; i < sideCount; i++) {
            double angle0 = i * angleStep;
            double angle1 = (i + 1) * angleStep;
            float x0 = (float) (halfW * Math.sin(angle0));
            float z0 = (float) (halfD * Math.cos(angle0));
            float x1 = (float) (halfW * Math.sin(angle1));
            float z1 = (float) (halfD * Math.cos(angle1));

            float centerX = (x0 + x1) / 2f;
            float centerZ = (z0 + z1) / 2f;
            float sideWidth = (float) Math.sqrt((x1 - x0) * (x1 - x0) + (z1 - z0) * (z1 - z0));

            // 面朝外的水平旋转角度（Y轴），使贴图正对法线方向
            double midAngle = (angle0 + angle1) / 2.0;
            float faceYawDegrees = (float) Math.toDegrees(midAngle);

            EditorBone faceBone = createPrismFaceBone(namePrefix + "side" + i, skeleton, parentBone,
                centerX, 0f, centerZ, faceYawDegrees, sideWidth, prismH);
            faceBones.add(faceBone);

            // 骨架原本为空时，第一个面成为根骨骼，后续面挂在它下面
            if (parentBone == null) {
                parentBone = faceBone;
            }
        }

        // 顶面和底面：也是标准QUAD部件，水平放置（绕X轴转90度），billboard关闭，不受镜头影响
        float capSize = Math.max(prismW, prismD);
        EditorBone topBone = createPrismFaceBone(namePrefix + "top", skeleton, parentBone,
            0f, prismH / 2f, 0f, 0f, capSize, capSize);
        topBone.setLocalRotation(new com.jme3.math.Quaternion().fromAngles(
            (float) Math.toRadians(90), 0f, 0f));
        topBone.setRestRotation(topBone.getLocalRotation().clone());
        faceBones.add(topBone);

        EditorBone bottomBone = createPrismFaceBone(namePrefix + "bottom", skeleton, parentBone,
            0f, -prismH / 2f, 0f, 0f, capSize, capSize);
        bottomBone.setLocalRotation(new com.jme3.math.Quaternion().fromAngles(
            (float) Math.toRadians(-90), 0f, 0f));
        bottomBone.setRestRotation(bottomBone.getLocalRotation().clone());
        faceBones.add(bottomBone);

        // 为每个面创建部件渲染器
        for (EditorBone faceBone : faceBones) {
            renderer.addPartRenderer(faceBone, faceBone == topBone || faceBone == bottomBone ? capSize : 1.0f,
                faceBone == topBone || faceBone == bottomBone ? capSize : prismH);
        }

        // 打包成一个组（复用拖拽分组/包功能）：折叠后只占一行，展开能看到所有面
        EditorGroupManager groupManager = skeleton.getGroupManager();
        String groupName = "Prism" + sideCount + "_" + (skeleton.getBoneCount());
        EditorBoneGroup group = groupManager.createGroup(groupName);
        if (group != null) {
            String groupId = groupManager.getGroupId(group);
            groupManager.addBonesToGroup(groupId, faceBones);
        }

        // 重新收集所有骨骼
        collectAllBones();

        // 刷新骨骼连线显示
        if (renderer != null) {
            renderer.refreshBoneConnections();
        }

        // 刷新部件列表面板（让新组的白色边框和折叠状态显示出来）
        if (editorUI != null && editorUI.getPartListPanel() != null) {
            editorUI.getPartListPanel().refreshPartList();
        }
    }

    /**
     * 创建棱柱的一个面（侧面/顶/底），作为一个标准QUAD部件
     * billboard关闭，使其固定朝向骨骼自身旋转，不跟随镜头转动
     */
    private EditorBone createPrismFaceBone(String name, EditorSkeleton skeleton, EditorBone parentBone,
                                            float posX, float posY, float posZ, float yawDegrees,
                                            float width, float height) {
        // 确保名称不冲突
        String finalName = name;
        int suffix = 1;
        while (skeleton.findBone(finalName) != null) {
            suffix++;
            finalName = name + "_" + suffix;
        }

        EditorBone bone = new EditorBone(finalName);

        // 关闭billboard：固定朝向，不跟随镜头旋转（棱柱面要保持立体结构）
        bone.setBillboardEnabled(false);

        String frontKey = EditorBone.Direction.FRONT.getKey();
        bone.setDirectionUV(frontKey, 0f, 0f, 1f, 1f);
        bone.setDirectionWidth(frontKey, width);
        bone.setDirectionHeight(frontKey, height);
        bone.setDirectionPriority(frontKey, 0);
        bone.setDirectionOffset(frontKey, 0f, 0f, 0f);
        bone.setDirectionRotation(frontKey, 0f, 0f, 0f);

        Quaternion yawRotation = new Quaternion().fromAngles(0f, (float) Math.toRadians(yawDegrees), 0f);

        bone.setRestPosition(new Vector3f(posX, posY, posZ));
        bone.setRestRotation(yawRotation);
        bone.setRestScale(new Vector3f(1, 1, 1));
        bone.resetToRestPose();

        if (parentBone == null) {
            skeleton.setRootBone(bone);
        } else {
            parentBone.addChild(bone);
        }
        skeleton.addBone(bone);

        return bone;
    }

    /**
     * 删除当前选中的部件
     */
    private void deletePart() {
        if (puppetTestScene == null || puppetTestScene.getTestSkeleton() == null) {
            return;
        }

        // 检查是否有选中的骨骼
        if (selectedBoneIndex < 0 || selectedBoneIndex >= allBones.length) {
            return;
        }

        EditorBone boneToDelete = allBones[selectedBoneIndex];
        String boneName = boneToDelete.getName();

        // 如果删除的是根骨骼，需要提升一个子骨骼为新根骨骼
        boolean wasRootBone = (boneToDelete == puppetTestScene.getTestSkeleton().getRootBone());

        if (wasRootBone) {
            // 复制一份子骨骼列表（因为在转移过程中会修改原列表）
            java.util.List<EditorBone> children = new java.util.ArrayList<>(boneToDelete.getChildren());
            if (!children.isEmpty()) {
                // 将第一个子骨骼提升为新的根骨骼
                EditorBone newRoot = children.get(0);
                boneToDelete.removeChild(newRoot);  // 断开与旧根骨骼的父子关系

                // 将其他子骨骼转移到新根骨骼下
                for (int i = 1; i < children.size(); i++) {
                    EditorBone child = children.get(i);
                    newRoot.addChild(child);  // addChild会自动处理父子关系的转移
                }

                // 先移除旧根骨骼，再手动更新根骨骼引用（不触发rebuildBoneList）
                puppetTestScene.getTestSkeleton().removeBone(boneName);
                puppetTestScene.getTestSkeleton().updateRootBoneReference(newRoot);
            } else {
                // 没有子骨骼，需要检查是否有其他独立骨骼
                java.util.List<EditorBone> allBonesInSkeleton = puppetTestScene.getTestSkeleton().getAllBones();
                EditorBone newRootCandidate = null;

                // 找到第一个不是当前要删除的骨骼的独立骨骼
                for (EditorBone bone : allBonesInSkeleton) {
                    if (bone != boneToDelete && bone.getParent() == null) {
                        newRootCandidate = bone;
                        break;
                    }
                }

                // 先移除旧根骨骼
                puppetTestScene.getTestSkeleton().removeBone(boneName);

                if (newRootCandidate != null) {
                    // 有其他独立骨骼，提升为新根骨骼
                    puppetTestScene.getTestSkeleton().updateRootBoneReference(newRootCandidate);
                } else {
                    // 真的没有骨骼了，骨架变为空
                    puppetTestScene.getTestSkeleton().updateRootBoneReference(null);
                }
            }
        } else {
            // 非根骨骼，正常删除
            puppetTestScene.getTestSkeleton().removeBone(boneName);
        }

        // 清理渲染器资源（包括高光）
        EditorPuppetPartRenderer renderer = puppetTestScene.getPuppetRenderer().getPartRenderer(boneName);
        if (renderer != null) {
            renderer.cleanup();
        }

        // 从渲染器移除
        puppetTestScene.getPuppetRenderer().removePartRenderer(boneName);

        // 从隐藏列表移除（如果存在）
        hiddenParts.remove(boneName);

        // 重新收集所有骨骼
        collectAllBones();

        // 刷新骨骼连线显示
        if (puppetTestScene != null && puppetTestScene.getPuppetRenderer() != null) {
            puppetTestScene.getPuppetRenderer().refreshBoneConnections();
        }

        // 刷新部件列表面板
        if (editorUI != null && editorUI.getPartListPanel() != null) {
            editorUI.getPartListPanel().refreshPartList();
        }

        // 选择第一个骨骼（或者选择前一个）
        if (allBones.length > 0) {
            int newIndex = Math.max(0, Math.min(selectedBoneIndex, allBones.length - 1));
            selectBone(newIndex);
        } else {
            editorUI.selectBone(null, null);
        }
    }

    /**
     * 执行删除部件（在右键点击时调用）
     */
    private void executeDeletePart(int boneIndex) {
        if (puppetTestScene == null || puppetTestScene.getTestSkeleton() == null) {
            return;
        }

        if (boneIndex < 0 || boneIndex >= allBones.length) {
            return;
        }

        EditorBone boneToDelete = allBones[boneIndex];
        String boneName = boneToDelete.getName();

        // 如果删除的是根骨骼，需要提升一个子骨骼为新根骨骼
        boolean wasRootBone = (boneToDelete == puppetTestScene.getTestSkeleton().getRootBone());

        if (wasRootBone) {
            // 复制一份子骨骼列表（因为在转移过程中会修改原列表）
            java.util.List<EditorBone> children = new java.util.ArrayList<>(boneToDelete.getChildren());
            if (!children.isEmpty()) {
                // 将第一个子骨骼提升为新的根骨骼
                EditorBone newRoot = children.get(0);
                boneToDelete.removeChild(newRoot);  // 断开与旧根骨骼的父子关系

                // 将其他子骨骼转移到新根骨骼下
                for (int i = 1; i < children.size(); i++) {
                    EditorBone child = children.get(i);
                    newRoot.addChild(child);  // addChild会自动处理父子关系的转移
                }

                // 先移除旧根骨骼，再手动更新根骨骼引用（不触发rebuildBoneList）
                puppetTestScene.getTestSkeleton().removeBone(boneName);
                puppetTestScene.getTestSkeleton().updateRootBoneReference(newRoot);
            } else {
                // 没有子骨骼，需要检查是否有其他独立骨骼
                java.util.List<EditorBone> allBonesInSkeleton = puppetTestScene.getTestSkeleton().getAllBones();
                EditorBone newRootCandidate = null;

                // 找到第一个不是当前要删除的骨骼的独立骨骼
                for (EditorBone bone : allBonesInSkeleton) {
                    if (bone != boneToDelete && bone.getParent() == null) {
                        newRootCandidate = bone;
                        break;
                    }
                }

                // 先移除旧根骨骼
                puppetTestScene.getTestSkeleton().removeBone(boneName);

                if (newRootCandidate != null) {
                    // 有其他独立骨骼，提升为新根骨骼
                    puppetTestScene.getTestSkeleton().updateRootBoneReference(newRootCandidate);
                } else {
                    // 真的没有骨骼了，骨架变为空
                    puppetTestScene.getTestSkeleton().updateRootBoneReference(null);
                }
            }
        } else {
            // 非根骨骼，正常删除
            puppetTestScene.getTestSkeleton().removeBone(boneName);
        }

        // 清理渲染器资源（包括高光）
        EditorPuppetPartRenderer renderer = puppetTestScene.getPuppetRenderer().getPartRenderer(boneName);
        if (renderer != null) {
            renderer.cleanup();
        }

        // 从渲染器移除
        puppetTestScene.getPuppetRenderer().removePartRenderer(boneName);

        // 从隐藏列表移除（如果存在）
        hiddenParts.remove(boneName);

        // 重新收集所有骨骼
        collectAllBones();

        // 刷新骨骼连线显示
        if (puppetTestScene != null && puppetTestScene.getPuppetRenderer() != null) {
            puppetTestScene.getPuppetRenderer().refreshBoneConnections();
        }

        // 刷新部件列表面板
        // if (editorUI != null && editorUI.getPartListPanel() != null) { // COMMENTED OUT - OLD UI
            // editorUI.getPartListPanel().refreshPartList(); // COMMENTED OUT - OLD UI
        // } // COMMENTED OUT - OLD UI

        // 退出删除模式
        isDeleteModeEnabled = false;

        // 选择第一个骨骼
        if (allBones.length > 0) {
            selectBone(0);
        } else {
            editorUI.selectBone(null, null);
        }
    }

    /**
     * 设置父骨骼（两步选择模式）
     * 第一次点击：选择要设置父节点的子骨骼
     * 第二次点击部件：选择父骨骼
     */
    private void setParentBone() {
        if (puppetTestScene == null || puppetTestScene.getTestSkeleton() == null) {
            return;
        }

        // 检查是否已经在等待模式，如果是，则取消
        if (waitingForParentSelect) {
            cancelParentMode();
            return;
        }

        if (selectedBoneIndex < 0 || selectedBoneIndex >= allBones.length) {
            return;
        }

        EditorBone selectedBone = allBones[selectedBoneIndex];

        // 进入等待选择父节点模式
        waitingForParentSelect = true;
        pendingChildBone = selectedBone;

        // 激活Set Parent按钮
        // editorUI.getInspectorPanel().getSetParentButton().setActive(true); // COMMENTED OUT - OLD UI
    }

    /**
     * 完成设置父骨骼（在选择第二个骨骼时调用）
     */
    private void completeSetParent(EditorBone newParentBone) {
        if (pendingChildBone == null) {
            return;
        }

        // 检查是否试图选择自己
        if (newParentBone == pendingChildBone) {
            cancelParentMode();
            return;
        }

        // 检查是否会形成循环
        if (isDescendant(newParentBone, pendingChildBone)) {
            cancelParentMode();
            return;
        }

        // 保存旧的父骨骼和局部变换
        EditorBone oldParent = pendingChildBone.getParent();
        com.jme3.math.Vector3f oldPosition = pendingChildBone.getLocalPosition().clone();
        com.jme3.math.Quaternion oldRotation = pendingChildBone.getLocalRotation().clone();
        com.jme3.math.Vector3f oldScale = pendingChildBone.getLocalScale().clone();

        // 保存当前世界变换
        com.jme3.math.Vector3f worldPos = new com.jme3.math.Vector3f();
        com.jme3.math.Quaternion worldRot = new com.jme3.math.Quaternion();
        com.jme3.math.Vector3f worldScale = new com.jme3.math.Vector3f();
        pendingChildBone.getWorldTransform(worldPos, worldRot, worldScale);

        // 计算新的局部变换，使世界变换保持不变
        com.jme3.math.Vector3f newParentWorldPos = new com.jme3.math.Vector3f();
        com.jme3.math.Quaternion newParentWorldRot = new com.jme3.math.Quaternion();
        com.jme3.math.Vector3f newParentWorldScale = new com.jme3.math.Vector3f();
        newParentBone.getWorldTransform(newParentWorldPos, newParentWorldRot, newParentWorldScale);

        // 计算新的局部位置
        com.jme3.math.Vector3f relativePos = worldPos.subtract(newParentWorldPos);
        com.jme3.math.Quaternion invParentRot = newParentWorldRot.inverse();
        com.jme3.math.Vector3f newLocalPos = invParentRot.mult(relativePos);
        newLocalPos.divideLocal(newParentWorldScale);

        // 计算新的局部旋转
        com.jme3.math.Quaternion newLocalRot = invParentRot.mult(worldRot);

        // 计算新的局部缩放
        com.jme3.math.Vector3f newLocalScale = worldScale.divide(newParentWorldScale);

        // 创建并执行命令
        Command cmd = new SetParentCommand(
            pendingChildBone, newParentBone,
            oldPosition, oldRotation, oldScale,
            newLocalPos, newLocalRot, newLocalScale
        );
        commandManager.executeCommand(cmd);

        // 刷新骨骼连线显示
        if (puppetTestScene != null && puppetTestScene.getPuppetRenderer() != null) {
            puppetTestScene.getPuppetRenderer().refreshBoneConnections();
        }

        cancelParentMode();
        editorUI.updateInspector();
    }

    /**
     * 取消父子关系设置模式
     */
    private void cancelParentMode() {
        waitingForParentSelect = false;
        pendingChildBone = null;
        isAddingFreeBone = false;

        // 熄灭Set Parent按钮
        // editorUI.getInspectorPanel().getSetParentButton().setActive(false); // COMMENTED OUT - OLD UI
    }

    /**
     * 清除父骨骼（一步操作）
     * 直接清除当前选中骨骼的父子关系
     */
    private void clearParentBone() {
        if (puppetTestScene == null || puppetTestScene.getTestSkeleton() == null) {
            return;
        }

        if (selectedBoneIndex < 0 || selectedBoneIndex >= allBones.length) {
            return;
        }

        EditorBone selectedBone = allBones[selectedBoneIndex];
        EditorBone currentParent = selectedBone.getParent();

        // 检查是否有父骨骼
        if (currentParent == null) {
            return;
        }

        // 保存旧的局部变换
        com.jme3.math.Vector3f oldPosition = selectedBone.getLocalPosition().clone();
        com.jme3.math.Quaternion oldRotation = selectedBone.getLocalRotation().clone();
        com.jme3.math.Vector3f oldScale = selectedBone.getLocalScale().clone();

        // 保存当前世界变换
        com.jme3.math.Vector3f worldPos = new com.jme3.math.Vector3f();
        com.jme3.math.Quaternion worldRot = new com.jme3.math.Quaternion();
        com.jme3.math.Vector3f worldScale = new com.jme3.math.Vector3f();
        selectedBone.getWorldTransform(worldPos, worldRot, worldScale);

        // 创建并执行命令
        Command cmd = new ClearParentCommand(
            selectedBone,
            oldPosition, oldRotation, oldScale,
            worldPos, worldRot, worldScale
        );
        commandManager.executeCommand(cmd);

        // 刷新骨骼连线显示
        if (puppetTestScene != null && puppetTestScene.getPuppetRenderer() != null) {
            puppetTestScene.getPuppetRenderer().refreshBoneConnections();
        }

        editorUI.updateInspector();
    }

    /**
     * 添加自由骨骼（与添加刚性骨骼类似，但骨骼类型为FREE）
     */
    private void addFreeBone() {
        if (puppetTestScene == null || puppetTestScene.getTestSkeleton() == null) {
            return;
        }

        // 检查是否已经在等待模式，如果是，则取消
        if (waitingForParentSelect) {
            cancelParentMode();
            return;
        }

        if (selectedBoneIndex < 0 || selectedBoneIndex >= allBones.length) {
            return;
        }

        EditorBone selectedBone = allBones[selectedBoneIndex];

        // 进入等待选择父节点模式（与setParentBone相同）
        waitingForParentSelect = true;
        pendingChildBone = selectedBone;
        isAddingFreeBone = true;  // 标记为添加自由骨骼模式
    }

    /**
     * 完成添加自由骨骼（在选择第二个骨骼时调用）
     */
    private void completeAddFreeBone(EditorBone newParentBone) {
        if (pendingChildBone == null) {
            return;
        }

        // 检查是否试图选择自己
        if (newParentBone == pendingChildBone) {
            cancelParentMode();
            return;
        }

        // 检查是否会形成循环
        if (isDescendant(newParentBone, pendingChildBone)) {
            cancelParentMode();
            return;
        }

        // 保存旧的父骨骼和局部变换
        EditorBone oldParent = pendingChildBone.getParent();
        com.jme3.math.Vector3f oldPosition = pendingChildBone.getLocalPosition().clone();
        com.jme3.math.Quaternion oldRotation = pendingChildBone.getLocalRotation().clone();
        com.jme3.math.Vector3f oldScale = pendingChildBone.getLocalScale().clone();

        // 保存当前世界变换
        com.jme3.math.Vector3f worldPos = new com.jme3.math.Vector3f();
        com.jme3.math.Quaternion worldRot = new com.jme3.math.Quaternion();
        com.jme3.math.Vector3f worldScale = new com.jme3.math.Vector3f();
        pendingChildBone.getWorldTransform(worldPos, worldRot, worldScale);

        // 计算新的局部变换，使世界变换保持不变
        com.jme3.math.Vector3f newParentWorldPos = new com.jme3.math.Vector3f();
        com.jme3.math.Quaternion newParentWorldRot = new com.jme3.math.Quaternion();
        com.jme3.math.Vector3f newParentWorldScale = new com.jme3.math.Vector3f();
        newParentBone.getWorldTransform(newParentWorldPos, newParentWorldRot, newParentWorldScale);

        // 计算新的局部位置
        com.jme3.math.Vector3f relativePos = worldPos.subtract(newParentWorldPos);
        com.jme3.math.Quaternion invParentRot = newParentWorldRot.inverse();
        com.jme3.math.Vector3f newLocalPos = invParentRot.mult(relativePos);
        newLocalPos.divideLocal(newParentWorldScale);

        // 计算新的局部旋转
        com.jme3.math.Quaternion newLocalRot = invParentRot.mult(worldRot);

        // 计算新的局部缩放
        com.jme3.math.Vector3f newLocalScale = worldScale.divide(newParentWorldScale);

        // 设置为自由骨骼类型
        pendingChildBone.setBoneType(EditorBone.BoneType.FREE);

        // 创建并执行命令
        Command cmd = new SetParentCommand(
            pendingChildBone, newParentBone,
            oldPosition, oldRotation, oldScale,
            newLocalPos, newLocalRot, newLocalScale
        );
        commandManager.executeCommand(cmd);

        // 为自由骨骼添加物理系统
        if (puppetTestScene != null && puppetTestScene.getPuppetRenderer() != null) {
            puppetTestScene.getPuppetRenderer().addFreeBonePhysics(pendingChildBone);
        }

        // 刷新骨骼连线显示
        if (puppetTestScene != null && puppetTestScene.getPuppetRenderer() != null) {
            puppetTestScene.getPuppetRenderer().refreshBoneConnections();
        }

        cancelParentMode();
        editorUI.updateInspector();
    }

    /**
     * 添加关键帧
     * 在当前时间点录制所有部件的变换
     */
    private void addKeyframe() {
        if (animationPlayer == null || currentClip == null) {
            return;
        }

        // 【重要】录制关键帧时，退出编辑模式，记录当前的编辑状态
        exitEditMode();

        float currentTime = (editorUI != null) ? editorUI.getCurrentTime() : animationPlayer.getCurrentTime();

        // 【修改】录制所有骨骼的关键帧，而不是只录制选中的骨骼
        animationPlayer.recordAllKeyframes(currentClip, currentTime, currentKeyframeType);

        // 更新时间轴显示
        if (editorUI != null) {
            TimelinePanel timelinePanel = editorUI.getTimelinePanel();
            if (timelinePanel != null) {
                timelinePanel.setAnimationClip(currentClip);
            }
        }

        String typeStr = (currentKeyframeType == com.Hecate.puppet.animation.Keyframe.KeyframeType.SNAPSHOT)
                ? "快照" : "插值";
        int boneCount = (animationPlayer.getSkeleton() != null) ? animationPlayer.getSkeleton().getAllBones().size() : 0;

    }

    /**
     * 添加快照关键帧
     * 直接录制快照类型的关键帧，无需切换模式
     */
    private void addSnapshot() {
        if (animationPlayer == null || currentClip == null) {
            return;
        }

        // 【重要】录制关键帧时，退出编辑模式，记录当前的编辑状态
        exitEditMode();

        float currentTime = (editorUI != null) ? editorUI.getCurrentTime() : animationPlayer.getCurrentTime();

        // 【修改】录制所有骨骼的快照关键帧
        animationPlayer.recordAllKeyframes(currentClip, currentTime,
            com.Hecate.puppet.animation.Keyframe.KeyframeType.SNAPSHOT);

        // 更新时间轴显示
        if (editorUI != null) {
            TimelinePanel timelinePanel = editorUI.getTimelinePanel();
            if (timelinePanel != null) {
                timelinePanel.setAnimationClip(currentClip);
            }
        }
    }

    /**
     * 删除当前时间的关键帧
     */
    private void deleteKeyframe() {
        if (animationPlayer == null || currentClip == null) {
            return;
        }

        float currentTime = editorUI != null ? editorUI.getCurrentTime() : animationPlayer.getCurrentTime();

        // 删除当前时间的所有骨骼关键帧
        int removedCount = currentClip.removeKeyframesAtTime(currentTime);

        if (removedCount > 0) {
            // 【修复】重置所有部件的动画旋转标志，让UI重新获得控制权
            if (puppetTestScene != null && puppetTestScene.getPuppetRenderer() != null && animationPlayer.getSkeleton() != null) {
                EditorPuppetRenderer puppetRenderer = puppetTestScene.getPuppetRenderer();
                for (EditorBone bone : animationPlayer.getSkeleton().getAllBones()) {
                    EditorPuppetPartRenderer partRenderer = puppetRenderer.getPartRenderer(bone.getName());
                    if (partRenderer != null) {
                        partRenderer.resetAnimationRotation();
                    }
                }
            }

            // 更新时间轴显示
            if (editorUI != null) {
                TimelinePanel timelinePanel = editorUI.getTimelinePanel();
                if (timelinePanel != null) {
                    timelinePanel.setAnimationClip(currentClip);
                }
            }
        }
    }

    /**
     * 复制选中的关键帧
     */
    private void copyKeyframe() {
        if (animationPlayer == null || currentClip == null) {
            return;
        }

        // 获取选中的关键帧时间
        Float selectedTime = null;
        if (editorUI != null) {
            TimelinePanel timelinePanel = editorUI.getTimelinePanel();
            if (timelinePanel != null) {
                selectedTime = timelinePanel.getSelectedKeyframeTime();
            }
        }

        if (selectedTime == null) {
            return;
        }

        // 清空剪贴板
        copiedKeyframes.clear();

        // 复制该时间点的所有关键帧
        java.util.List<Keyframe> allKeyframes = currentClip.getAllKeyframes();
        for (Keyframe kf : allKeyframes) {
            if (Math.abs(kf.getTime() - selectedTime) < 0.001f) {
                // 克隆关键帧（创建副本）
                Keyframe copy = new Keyframe(kf.getTime(), kf.getBoneName(), kf.getType());
                copy.setPosition(kf.getPosition().clone());
                copy.setRotation(kf.getRotation().clone());
                copy.setScale(kf.getScale().clone());
                copy.setWidth(kf.getWidth());
                copy.setHeight(kf.getHeight());
                copy.setCustomRotationX(kf.getCustomRotationX());
                copy.setCustomRotationY(kf.getCustomRotationY());
                copy.setCustomRotationZ(kf.getCustomRotationZ());
                copy.setTextureRotation(kf.getTextureRotation());
                if (kf.getTexturePath() != null) {
                    copy.setTexturePath(kf.getTexturePath());
                }
                copiedKeyframes.add(copy);
            }
        }
    }

    /**
     * 粘贴关键帧到当前时间
     */
    private void pasteKeyframe() {
        if (animationPlayer == null || currentClip == null) {
            return;
        }

        if (copiedKeyframes.isEmpty()) {
            return;
        }

        // 获取当前时间
        float currentTime = editorUI != null ? editorUI.getCurrentTime() : animationPlayer.getCurrentTime();

        // 粘贴所有复制的关键帧到当前时间
        int pastedCount = 0;
        for (Keyframe copiedKf : copiedKeyframes) {
            // 创建新的关键帧，时间设置为当前时间
            Keyframe newKf = new Keyframe(currentTime, copiedKf.getBoneName(), copiedKf.getType());
            newKf.setPosition(copiedKf.getPosition().clone());
            newKf.setRotation(copiedKf.getRotation().clone());
            newKf.setScale(copiedKf.getScale().clone());
            newKf.setWidth(copiedKf.getWidth());
            newKf.setHeight(copiedKf.getHeight());
            newKf.setCustomRotationX(copiedKf.getCustomRotationX());
            newKf.setCustomRotationY(copiedKf.getCustomRotationY());
            newKf.setCustomRotationZ(copiedKf.getCustomRotationZ());
            if (copiedKf.getTexturePath() != null) {
                newKf.setTexturePath(copiedKf.getTexturePath());
            }

            // 添加到动画片段
            currentClip.addKeyframe(newKf);
            pastedCount++;
        }

        

        // 更新时间轴显示
        if (editorUI != null) {
            TimelinePanel timelinePanel = editorUI.getTimelinePanel();
            if (timelinePanel != null) {
                timelinePanel.setAnimationClip(currentClip);
            }
        }

        // 应用动画到当前时间
        if (animationPlayer != null) {
            animationPlayer.seek(currentTime);
        }
    }

    /**
     * 检查 candidate 是否是 bone 的子孙骨骼
     */
    private boolean isDescendant(EditorBone candidate, EditorBone bone) {
        for (EditorBone child : bone.getChildren()) {
            if (child == candidate) {
                return true;
            }
            if (isDescendant(candidate, child)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 复制当前选中的骨骼（包括子骨骼）
     */
    private void copyBone() {
        if (selectedBoneIndex < 0 || selectedBoneIndex >= allBones.length) {
            return;
        }

        EditorBone selectedBone = allBones[selectedBoneIndex];
        BoneClipboardData.BoneData boneData = copyBoneRecursive(selectedBone);
        ClipboardManager.copy(new BoneClipboardData(boneData));

    }

    /**
     * 递归复制骨骼及其子骨骼
     */
    private BoneClipboardData.BoneData copyBoneRecursive(EditorBone bone) {
        BoneClipboardData.BoneData data = new BoneClipboardData.BoneData();

        // 复制骨骼属性
        data.name = bone.getName();
        data.volumeDefinition = bone.getVolumeDefinition() == null ? null : bone.getVolumeDefinition().copy();
        data.rotationStripEnabled=bone.isRotationStripEnabled();data.stripTexturePath=bone.getStripTexturePath();data.stripSteps=bone.getStripSteps();
        data.stripFrameWidthPx=bone.getStripFrameWidthPx();data.stripFrameHeightPx=bone.getStripFrameHeightPx();data.stripCalibrationOffsetPx=bone.getStripCalibrationOffsetPx();
        data.stripPriority=bone.getStripPriority();data.stripOffset=bone.getStripOffset();data.stripRotation=new Vector3f(bone.getStripRotationX(),bone.getStripRotationY(),bone.getStripRotationZ());
        data.localPosition = bone.getLocalPosition().clone();
        data.localRotation = bone.getLocalRotation().clone();
        data.localScale = bone.getLocalScale().clone();
        data.priority = bone.getPriority();

        // 复制渲染器属性
        EditorPuppetPartRenderer renderer = puppetTestScene.getPuppetRenderer().getPartRenderer(bone.getName());
        if (renderer != null) {
            data.width = renderer.getWidth();
            data.height = renderer.getHeight();
            data.offset = renderer.getOffset().clone();
            data.customRotationX = renderer.getCustomRotationX();
            data.customRotationY = renderer.getCustomRotationY();
            data.customRotationZ = renderer.getCustomRotationZ();
            data.texturePath = renderer.getTexturePath();

            // 复制UV坐标数据
            data.uvOffsetX = renderer.getUvOffsetX();
            data.uvOffsetY = renderer.getUvOffsetY();
            data.uvScaleX = renderer.getUvScaleX();
            data.uvScaleY = renderer.getUvScaleY();
        }

        // 复制所有方向的纹理映射
        if (bone.getDirectionTextures() != null) {
            data.directionTextures = new java.util.HashMap<>(bone.getDirectionTextures());
        }

        // 复制所有方向的尺寸映射
        if (bone.getDirectionWidths() != null) {
            data.directionWidths = new java.util.HashMap<>(bone.getDirectionWidths());
        }
        if (bone.getDirectionHeights() != null) {
            data.directionHeights = new java.util.HashMap<>(bone.getDirectionHeights());
        }

        // 递归复制子骨骼
        for (EditorBone child : bone.getChildren()) {
            data.children.add(copyBoneRecursive(child));
        }

        return data;
    }

    /**
     * 粘贴骨骼
     */
    private void pasteBone() {
        pasteBoneInternal(false);
    }

    /**
     * 镜像粘贴骨骼
     */
    private void pasteBoneMirrored() {
        pasteBoneInternal(true);
    }

    /**
     * 切换当前骨骼的显示方向
     */
    private void changeDirection(String direction) {
        if (selectedBoneIndex < 0 || selectedBoneIndex >= allBones.length) {
            return;
        }

        EditorBone selectedBone = allBones[selectedBoneIndex];
        EditorPuppetPartRenderer renderer = puppetTestScene.getPuppetRenderer().getPartRenderer(selectedBone.getName());

        if (renderer == null) {
            return;
        }

        // 更新骨骼的当前方向
        selectedBone.setCurrentDirection(direction);

        // 更新渲染器的贴图和所有属性（宽度、高度、偏移、旋转、优先级）
        renderer.updateTextureFromBone();

        // 更新UI显示（关键修复：同步UI滑条值与当前方向的属性值）
        if (editorUI != null) {
            editorUI.updateInspector();
        }

        // 更新Timeline面板的方向按钮状态
        // if (editorUI != null && editorUI.getTimelinePanel() != null) { // COMMENTED OUT - OLD UI
            // editorUI.getTimelinePanel().updateDirectionButtons(direction); // COMMENTED OUT - OLD UI
        // } // COMMENTED OUT - OLD UI
    }

    /**
     * 内部粘贴实现
     */
    private void pasteBoneInternal(boolean mirrored) {
        if (!ClipboardManager.hasData()) {
            return;
        }

        BoneClipboardData clipboardData = mirrored ?
            ClipboardManager.pasteMirrored() :
            ClipboardManager.paste();

        if (clipboardData == null) {
            return;
        }

        // 创建新骨骼
        EditorBone newBone = createBoneFromData(clipboardData.getRootBoneData());

        // 将新骨骼添加到根骨骼作为子骨骼（确保它在骨骼树中，而不是孤立的）
        EditorBone rootBone = puppetTestScene.getTestSkeleton().getRootBone();
        if (rootBone != null) {
            rootBone.addChild(newBone);  // 作为根骨骼的子骨骼
        } else {
            // 如果没有根骨骼，将新骨骼设为根骨骼
            puppetTestScene.getTestSkeleton().setRootBone(newBone);
        }

        // 添加到骨架索引
        puppetTestScene.getTestSkeleton().addBone(newBone);

        // 刷新骨骼列表
        collectAllBones();

        // 刷新骨骼连线显示
        if (puppetTestScene != null && puppetTestScene.getPuppetRenderer() != null) {
            puppetTestScene.getPuppetRenderer().refreshBoneConnections();
        }

        // 刷新部件列表面板
        if (editorUI != null && editorUI.getPartListPanel() != null) {
            editorUI.getPartListPanel().refreshPartList();
        }

        // 自动选择新创建的骨骼
        for (int i = 0; i < allBones.length; i++) {
            if (allBones[i] == newBone) {
                selectBone(i);
                break;
            }
        }

    }

    /**
     * 从剪贴板数据创建骨骼
     */
    private EditorBone createBoneFromData(BoneClipboardData.BoneData data) {
        // 生成唯一名称
        String uniqueName = generateUniqueBoneName(data.name);

        // 创建骨骼
        EditorBone bone = new EditorBone(uniqueName);
        bone.setVolumeDefinition(data.volumeDefinition);
        if(volumeMode && bone.getVolumeDefinition()==null){var d=new com.Hecate.puppet.volume.VolumeDefinition();d.setDimensions(Math.max(1,Math.min(4096,Math.round(data.width*32))),Math.max(1,Math.min(4096,Math.round(data.height*32))),32);bone.setVolumeDefinition(d);}
        bone.setRotationStripEnabled(data.rotationStripEnabled);bone.setStripTexturePath(data.stripTexturePath);bone.setStripSteps(data.stripSteps);
        bone.setStripFrameWidthPx(data.stripFrameWidthPx);bone.setStripFrameHeightPx(data.stripFrameHeightPx);bone.setStripCalibrationOffsetPx(data.stripCalibrationOffsetPx);
        bone.setStripPriority(data.stripPriority);bone.setStripOffset(data.stripOffset.x,data.stripOffset.y,data.stripOffset.z);
        bone.setStripRotation(data.stripRotation.x,data.stripRotation.y,data.stripRotation.z);

        // 【修复】显式启用billboard，确保粘贴的模型在旋转时正确面向摄像机
        bone.setBillboardEnabled(true);

        bone.setLocalPosition(data.localPosition.clone());
        bone.setLocalRotation(data.localRotation.clone());
        bone.setLocalScale(data.localScale.clone());
        bone.setRestPosition(data.localPosition.clone());
        bone.setRestRotation(data.localRotation.clone());
        bone.setRestScale(data.localScale.clone());
        bone.setPriority(data.priority);  // 恢复优先级

        // 恢复所有方向的纹理映射
        if (data.directionTextures != null && !data.directionTextures.isEmpty()) {
            bone.setDirectionTextures(new java.util.HashMap<>(data.directionTextures));
        }

        // 恢复所有方向的尺寸映射
        if (data.directionWidths != null && !data.directionWidths.isEmpty()) {
            bone.setDirectionWidths(new java.util.HashMap<>(data.directionWidths));
        }
        if (data.directionHeights != null && !data.directionHeights.isEmpty()) {
            bone.setDirectionHeights(new java.util.HashMap<>(data.directionHeights));
        }

        // 创建渲染器（使用正确的尺寸，而不是默认尺寸）
        EditorPuppetPartRenderer renderer = puppetTestScene.getPuppetRenderer().addPartRenderer(bone, data.width, data.height);
        if (renderer != null) {
            // 优先使用当前方向的纹理，如果没有则使用旧的texturePath
            String currentDirection = bone.getCurrentDirection();
            String textureToLoad = bone.getDirectionTexture(currentDirection);
            if (textureToLoad == null || textureToLoad.isEmpty()) {
                textureToLoad = data.texturePath;
            }

            // 先加载纹理
            if (textureToLoad != null && !textureToLoad.isEmpty()) {
                renderer.loadTexture(textureToLoad);
            }

            // 然后设置所有属性（在loadTexture之后，防止loadTexture重置属性）
            renderer.setSize(data.width, data.height);
            renderer.setOffset(data.offset.x, data.offset.y);
            renderer.setCustomRotationX(data.customRotationX);
            renderer.setCustomRotationY(data.customRotationY);
            renderer.setCustomRotationZ(data.customRotationZ);

            // 恢复UV坐标数据
            renderer.setUV(data.uvOffsetX, data.uvOffsetY, data.uvScaleX, data.uvScaleY);
        }

        if(data.rotationStripEnabled){bone.setStripWidth(data.width);bone.setStripHeight(data.height);
            bone.setStripOffset(data.stripOffset.x,data.stripOffset.y,data.stripOffset.z);bone.setStripRotation(data.stripRotation.x,data.stripRotation.y,data.stripRotation.z);
            if(renderer!=null)renderer.updateTextureFromBone();}

        // 递归创建子骨骼
        for (BoneClipboardData.BoneData childData : data.children) {
            EditorBone childBone = createBoneFromData(childData);
            bone.addChild(childBone);
            puppetTestScene.getTestSkeleton().addBone(childBone);
        }

        return bone;
    }

    /**
     * 生成唯一的骨骼名称
     */
    private String generateUniqueBoneName(String baseName) {
        // 移除已有的后缀（如_Copy, _Copy2等）
        String cleanName = baseName.replaceAll("_Copy\\d*$", "");

        String uniqueName = cleanName;
        int suffix = 1;

        // 检查名称是否已存在
        while (puppetTestScene.getTestSkeleton().findBone(uniqueName) != null) {
            suffix++;
            uniqueName = cleanName + "_Copy" + (suffix > 1 ? suffix : "");
        }

        return uniqueName;
    }

    /**
     * 创建一个置顶的临时父窗口用于文件对话框
     */
    private static JFrame createTopMostFrame() {
        JFrame frame = new JFrame();
        frame.setAlwaysOnTop(true);
        frame.setUndecorated(true);
        frame.setSize(0, 0);
        frame.setVisible(false);
        return frame;
    }

    /**
     * 进入镜像配对模式
     */
    private void enterMirrorPairingMode() {
        mirrorPairingMode = true;
        firstMirrorBone = null;
        // TODO: Add visual feedback in UI (e.g., status message in title bar)
    }

    /**
     * 退出镜像配对模式
     */
    private void exitMirrorPairingMode() {
        mirrorPairingMode = false;
        firstMirrorBone = null;
        // TODO: Clear visual feedback in UI
    }

    /**
     * 设置当前选中镜像对的镜像轴
     */
    private void setMirrorAxis(MirrorManager.MirrorAxis axis) {
        if (selectedBone != null && mirrorManager.hasMirror(selectedBone)) {
            mirrorManager.setMirrorAxis(selectedBone, axis);
            // TODO: Update UI to reflect axis change (show in inspector panel)
        } else {
        }
    }

    /**
     * 获取命令管理器
     */
    public CommandManager getCommandManager() {
        return commandManager;
    }

    /**
     * 执行撤销操作
     */
    private void performUndo() {

        if (commandManager != null) {

            if (commandManager.canUndo()) {
                commandManager.undo();

                // 刷新UI显示
                // if (editorUI != null && editorUI.getInspectorPanel() != null) { // COMMENTED OUT - OLD UI
                    editorUI.updateInspector();
                // } // COMMENTED OUT - OLD UI

                // 刷新骨骼连线显示
                if (puppetTestScene != null && puppetTestScene.getPuppetRenderer() != null) {
                    puppetTestScene.getPuppetRenderer().refreshBoneConnections();
                }

                // 刷新部件列表面板
                // if (editorUI != null && editorUI.getPartListPanel() != null) { // COMMENTED OUT - OLD UI
                    // editorUI.getPartListPanel().refreshPartList(); // COMMENTED OUT - OLD UI
                // } // COMMENTED OUT - OLD UI

            } else {
            }
        } else {
        }
    }

    /**
     * 执行重做操作
     */
    private void performRedo() {

        if (commandManager != null) {

            if (commandManager.canRedo()) {
                commandManager.redo();

                // 刷新UI显示
                // if (editorUI != null && editorUI.getInspectorPanel() != null) { // COMMENTED OUT - OLD UI
                    editorUI.updateInspector();
                // } // COMMENTED OUT - OLD UI

                // 刷新骨骼连线显示
                if (puppetTestScene != null && puppetTestScene.getPuppetRenderer() != null) {
                    puppetTestScene.getPuppetRenderer().refreshBoneConnections();
                }

                // 刷新部件列表面板
                // if (editorUI != null && editorUI.getPartListPanel() != null) { // COMMENTED OUT - OLD UI
                    // editorUI.getPartListPanel().refreshPartList(); // COMMENTED OUT - OLD UI
                // } // COMMENTED OUT - OLD UI

            } else {
            }
        } else {
        }
    }

    /**
     * 重命名部件
     */
    private void renamePart(EditorBone oldBone, String newName) {
        if (puppetTestScene == null || puppetTestScene.getTestSkeleton() == null) {
            return;
        }

        String oldName = oldBone.getName();

        // 打印重命名前的所有骨骼（已禁用）
        // System.out.print("[renamePart] 重命名前骨骼列表: ");
        // for (EditorBone b : puppetTestScene.getTestSkeleton().getAllBones()) {
        //     System.out.print(b.getName() + " ");
        // }

        // 打印oldBone的父骨骼和子骨骼
        EditorBone parent = oldBone.getParent();
        for (EditorBone child : oldBone.getChildren()) {
        }

        // 创建新的骨骼对象（因为name是final的）
        EditorBone newBone = new EditorBone(newName);
        newBone.setVolumeDefinition(oldBone.getVolumeDefinition());

        // 复制所有属性
        newBone.setRestPosition(oldBone.getRestPosition().clone());
        newBone.setRestRotation(oldBone.getRestRotation().clone());
        newBone.setRestScale(oldBone.getRestScale().clone());
        newBone.setLocalPosition(oldBone.getLocalPosition().clone());
        newBone.setLocalRotation(oldBone.getLocalRotation().clone());
        newBone.setLocalScale(oldBone.getLocalScale().clone());
        newBone.setZOffset(oldBone.getZOffset());
        newBone.setPriority(oldBone.getPriority());

        // 复制纹理信息
        if (oldBone.getTexturePath() != null) {
            newBone.setTexturePath(oldBone.getTexturePath());
        }
        newBone.setDirectionTextures(oldBone.getDirectionTextures());
        newBone.setCurrentDirection(oldBone.getCurrentDirection());

        // 复制方向尺寸信息（修复重命名后尺寸重置为2x2的bug）
        newBone.setDirectionWidths(oldBone.getDirectionWidths());
        newBone.setDirectionHeights(oldBone.getDirectionHeights());

        // 获取父骨骼和子骨骼
        EditorBone parentBone = oldBone.getParent();
        java.util.List<EditorBone> children = new java.util.ArrayList<>(oldBone.getChildren());

        // 从父骨骼移除旧骨骼
        if (parentBone != null) {
            parentBone.removeChild(oldBone);
        }

        // 将新骨骼添加到父骨骼
        if (parentBone != null) {
            parentBone.addChild(newBone);
        }

        // 转移所有子骨骼到新骨骼
        for (EditorBone child : children) {
            newBone.addChild(child);
        }

        // *** 重要：在修改骨架之前先获取旧的渲染器 ***
        EditorPuppetPartRenderer oldRenderer = puppetTestScene.getPuppetRenderer().getPartRenderer(oldName);

        // *** 在cleanup之前保存渲染器属性（因为cleanup会清空这些字段）***
        float savedWidth = 0, savedHeight = 0;
        Vector3f savedOffset = new Vector3f();
        float savedCustomRotX = 0, savedCustomRotZ = 0;
        Vector3f savedPivotPoint = new Vector3f();
        // 保存UV坐标数据
        float savedUvOffsetX = 0, savedUvOffsetY = 0;
        float savedUvScaleX = 1.0f, savedUvScaleY = 1.0f;

        if (oldRenderer != null) {
            savedWidth = oldRenderer.getWidth();
            savedHeight = oldRenderer.getHeight();
            savedOffset = oldRenderer.getOffset().clone();
            savedCustomRotX = oldRenderer.getCustomRotationX();
            savedCustomRotZ = oldRenderer.getCustomRotationZ();
            savedPivotPoint = oldRenderer.getPivotPoint().clone();
            // 保存UV坐标
            savedUvOffsetX = oldRenderer.getUvOffsetX();
            savedUvOffsetY = oldRenderer.getUvOffsetY();
            savedUvScaleX = oldRenderer.getUvScaleX();
            savedUvScaleY = oldRenderer.getUvScaleY();
        }

        // 清理旧渲染器的资源（包括高光）- 在移除骨架之前就清理
        if (oldRenderer != null) {
            oldRenderer.cleanup();
        } else {
        }

        // 从渲染器移除旧部件
        puppetTestScene.getPuppetRenderer().removePartRenderer(oldName);

        // 更新骨架索引
        // 注意：不能使用setRootBone()，因为它会调用rebuildBoneList()清空所有索引
        // 这会导致不在骨骼树中的独立骨骼丢失
        puppetTestScene.getTestSkeleton().removeBone(oldName);

        puppetTestScene.getTestSkeleton().addBone(newBone);
        for(var part:puppetTestScene.getTestSkeleton().getAllBones()){
            var value=part.getVolumeDefinition();if(value!=null&&oldName.equals(value.getCoverTarget())){var copy=value.copy();copy.setCoverTarget(newName);part.setVolumeDefinition(copy);}
        }

        // 如果是根骨骼，需要手动更新根骨骼引用（不调用setRootBone以避免rebuildBoneList）
        boolean isRootBone = (oldBone == puppetTestScene.getTestSkeleton().getRootBone());
        if (isRootBone) {
            // 使用反射或者添加一个不触发rebuild的方法
            // 临时解决方案：直接通过Skeleton的内部方法更新
            puppetTestScene.getTestSkeleton().updateRootBoneReference(newBone);
        }

        // 添加新部件渲染器
        puppetTestScene.getPuppetRenderer().addPartRenderer(newBone);

        // 复制渲染器属性（使用保存的值）
        EditorPuppetPartRenderer newRenderer = puppetTestScene.getPuppetRenderer().getPartRenderer(newName);
        if (newRenderer != null) {
            newRenderer.setSize(savedWidth, savedHeight);
            newRenderer.setOffset(savedOffset.x, savedOffset.y);
            newRenderer.setOffsetZ(savedOffset.z);
            newRenderer.setCustomRotationX(savedCustomRotX);
            newRenderer.setCustomRotationZ(savedCustomRotZ);
            newRenderer.setPivotPoint(savedPivotPoint);

            // 恢复UV坐标
            newRenderer.setUV(savedUvOffsetX, savedUvOffsetY, savedUvScaleX, savedUvScaleY);

            // 更新纹理
            newRenderer.updateTextureFromBone();
        }

        // 重新收集所有骨骼
        collectAllBones();

        // 打印所有骨骼名称（已禁用）
        // System.out.print("[renamePart] 当前骨骼列表: ");
        // for (EditorBone b : allBones) {
        //     System.out.print(b.getName() + " ");
        // }

        // 刷新骨骼连线显示
        if (puppetTestScene != null && puppetTestScene.getPuppetRenderer() != null) {
            puppetTestScene.getPuppetRenderer().refreshBoneConnections();

            // 强制更新所有部件的变换（防止其他部件错位）
            // 这是必需的，因为重命名操作改变了骨骼对象的引用，虽然父子关系被正确转移，
            // 但渲染器需要重新计算所有部件的世界坐标
            puppetTestScene.getPuppetRenderer().update(0);
        }

        // 自动选择重命名后的部件（先选择，再刷新列表以确保高亮正确）
        for (int i = 0; i < allBones.length; i++) {
            if (allBones[i] == newBone) {
                selectBone(i);
                break;
            }
        }

        // 刷新部件列表面板（在选择之后刷新，确保高亮正确显示）
        if (editorUI != null && editorUI.getPartListPanel() != null) {
            editorUI.getPartListPanel().refreshPartList();
            // 刷新后重新设置选中高亮
            editorUI.getPartListPanel().setSelectedBone(newBone);
        }

    }

    /**
     * 创建并配置编辑器窗口设置
     */
    public static PuppetEditorApp createEditor() {
        PuppetEditorApp app = new PuppetEditorApp();

        AppSettings settings = new AppSettings(true);
        settings.setTitle("Puppet Editor - 木偶编辑器");

        // 获取屏幕尺寸并使用最大化窗口而非全屏
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        DisplayMode dm = gd.getDisplayMode();
        settings.setWidth(dm.getWidth());
        settings.setHeight(dm.getHeight());
        settings.setFullscreen(false);  // 不使用全屏模式，避免黑屏
        settings.setResizable(true);
        settings.setVSync(true);
        settings.setSamples(4); // 抗锯齿

        app.setSettings(settings);
        app.setShowSettings(false); // 不显示设置对话框

        return app;
    }

    /** Opens a modeless UI; every controller operation is marshalled to the scene thread. */
    public void openVolumeWorkbench() {
        java.awt.EventQueue.invokeLater(() -> {
            if (volumeWorkbench == null) volumeWorkbench = new VolumeWorkbench(new VolumeWorkbench.Controller() {
                public void requestSnapshot(java.util.function.Consumer<VolumeWorkbench.Snapshot> receiver) {
                    enqueue(() -> {
                        EditorBone b = selectedBone;
                        EditorPuppetRenderer renderer = puppetTestScene.getPuppetRenderer();
                        boolean live = b != null && puppetTestScene.getTestSkeleton().findBone(b.getName()) == b;
                        com.Hecate.puppet.volume.VolumeDefinition d = live ? b.getVolumeDefinition() : null;
                        EditorPuppetPartRenderer part = live ? renderer.getPartRenderer(b.getName()) : null;
                        receiver.accept(new VolumeWorkbench.Snapshot(
                            puppetTestScene.getTestSkeleton().getAllBones().stream().map(EditorBone::getName).toArray(String[]::new),
                            live ? b.getName() : null, live ? b : null, d, d == null ? null : d.copy(),
                            renderer.getVolumeViewMode(), live && !b.isModelEnabled(), part == null ? null : part.getVolumeError()));
                    });
                }
                public void select(String name) { enqueue(() -> { collectAllBones(); for(int i=0;i<allBones.length;i++)if(allBones[i].getName().equals(name)){selectBone(i);break;} }); }
                public void create(com.Hecate.puppet.volume.VolumeDefinition d) { enqueue(() -> createVolumePart(d)); }
                public void mode(com.Hecate.puppet.volume.VolumeReferenceView.Mode mode) { enqueue(() -> puppetTestScene.getPuppetRenderer().setVolumeViewMode(mode)); }
                public void apply(Object token, com.Hecate.puppet.volume.VolumeDefinition d) {
                    enqueue(() -> {
                        if (!isCurrentVolumeBone(token)) return;
                        EditorBone b = (EditorBone)token;
                        if (b.isModelEnabled()) return;
                        commandManager.executeCommand(new SetVolumeDefinitionCommand(b,d,() -> {
                            EditorPuppetPartRenderer p = puppetTestScene.getPuppetRenderer().getPartRenderer(b.getName());
                            if(p != null)p.refreshVolumeAppearance();
                        }));
                    });
                }
                public void action(String action, Object token) {
                    enqueue(() -> {
                        switch(action) {
                            case "undo": performUndo(); break;
                            case "redo": performRedo(); break;
                            case "play": togglePlayback(); break;
                            case "key": addKeyframe(); break;
                            case "save": savePuppet(); break;
                            default:
                                if(!isCurrentVolumeBone(token))return;
                                switch(action) {
                                    case "color": loadTexture(); break;
                                    case "strip": loadStripTexture(); break;
                                    case "crop": editorUI.getSliderColumnPanel().openRotationStripSelector(); break;
                                    case "refresh": puppetTestScene.getPuppetRenderer().getPartRenderer(((EditorBone)token).getName()).refreshVolumeAppearance(); break;
                                }
                        }
                    });
                }
            });
            volumeWorkbench.show();
        });
    }
    private boolean isCurrentVolumeBone(Object token) {
        return token instanceof EditorBone && token == selectedBone
            && puppetTestScene.getTestSkeleton().findBone(((EditorBone)token).getName()) == token;
    }
    private void createVolumePart(com.Hecate.puppet.volume.VolumeDefinition definition) {
        EditorSkeleton skeleton = puppetTestScene.getTestSkeleton();
        EditorPuppetRenderer renderer = puppetTestScene.getPuppetRenderer();
        int suffix=1; while(skeleton.findBone("Volume_"+suffix)!=null)suffix++;
        EditorBone bone=new EditorBone("Volume_"+suffix);
        if(volumeMode){definition.getStrip3D().setEnabled(true);definition.getBakedPixels().setEnabled(true);}
        bone.setVolumeDefinition(definition); bone.setBillboardEnabled(true);
        if (volumeMode) {
            bone.setRotationStripEnabled(true); bone.setStripWidth(definition.width()); bone.setStripHeight(definition.height());
            bone.setStripFrameWidthPx(definition.getWidthPx());bone.setStripFrameHeightPx(definition.getHeightPx());
        }
        bone.setDirectionUV("front",0,0,1,1);
        bone.setDirectionWidth("front",definition.width()); bone.setDirectionHeight("front",definition.height());
        bone.setDirectionPriority("front",0); bone.setDirectionOffset("front",0,0,0); bone.setDirectionRotation("front",0,0,0);
        bone.setRestPosition(new Vector3f());bone.setRestRotation(new Quaternion());bone.setRestScale(new Vector3f(1,1,1));bone.resetToRestPose();
        EditorBone parent = isCurrentVolumeBone(selectedBone) ? selectedBone : skeleton.getRootBone();
        commandManager.executeCommand(new Command() {
            public void execute() {
                if(parent==null)skeleton.setRootBone(bone);else parent.addChild(bone);
                skeleton.addBone(bone);renderer.addPartRenderer(bone);
                renderer.getPartRenderer(bone.getName()).setWidth(definition.width());renderer.getPartRenderer(bone.getName()).setHeight(definition.height());
                refreshVolumeStructure(bone);
            }
            public void undo() {
                renderer.removePartRenderer(bone.getName());skeleton.removeBone(bone.getName());
                if(skeleton.getRootBone()==bone)skeleton.updateRootBoneReference(null);
                selectedBones.remove(bone);refreshVolumeStructure(parent);
            }
            public String getDescription(){return "新建实体部件";}
        });
        if (!volumeMode) renderer.setVolumeViewMode(com.Hecate.puppet.volume.VolumeReferenceView.Mode.OVERLAY);
    }
    private void refreshVolumeStructure(EditorBone focus) {
        collectAllBones();puppetTestScene.getPuppetRenderer().refreshBoneConnections();
        editorUI.getPartListPanel().refreshPartList();
        for(int i=0;i<allBones.length;i++)if(allBones[i]==focus){selectBone(i);return;}
        if(allBones.length>0)selectBone(0);
        else {selectedBone=null;selectedBoneIndex=-1;editorUI.setSkeleton(puppetTestScene.getTestSkeleton());}
    }

    public static PuppetEditorApp createPixelEditor() {
        PuppetEditorApp app=createEditor();app.volumeMode=true;app.settings.setTitle("木偶编辑器 · 预生成像素模式");return app;
    }
    public static PuppetEditorApp createVolumeEditor() { return createPixelEditor(); }
    public void bindVolumeAnimationPanel(AnimationLayerPanel panel) { if(animationPlayer!=null)panel.setAnimationPlayer(animationPlayer); }
    public EditorBone getVolumeSelectedBone() { return isCurrentVolumeBone(selectedBone) ? selectedBone : null; }
    public boolean isVolumeModelDisplay() { return puppetTestScene.getPuppetRenderer().getVolumeViewMode()==com.Hecate.puppet.volume.VolumeReferenceView.Mode.SOLID; }
    public boolean isVolumeXrayEnabled(){return puppetTestScene.getPuppetRenderer().isVolumeXrayEnabled();}
    public void toggleVolumeXray(){
        var renderer=puppetTestScene.getPuppetRenderer();
        if(!isVolumeModelDisplay()){
            renderer.setVolumeViewMode(com.Hecate.puppet.volume.VolumeReferenceView.Mode.SOLID);
            renderer.setVolumeXrayEnabled(true);
        }else renderer.setVolumeXrayEnabled(!renderer.isVolumeXrayEnabled());
    }
    public void toggleVolumeDisplay() {
        puppetTestScene.getPuppetRenderer().setVolumeViewMode(isVolumeModelDisplay()
            ? com.Hecate.puppet.volume.VolumeReferenceView.Mode.APPEARANCE : com.Hecate.puppet.volume.VolumeReferenceView.Mode.SOLID);
    }
    private void initializeVolumeModeParts(boolean sample) {
        if(sample){var skeleton=puppetTestScene.getTestSkeleton();var renderer=puppetTestScene.getPuppetRenderer();
            for(var bone:new java.util.ArrayList<>(skeleton.getAllBones()))renderer.removePartRenderer(bone.getName());skeleton.clear();renderer.setVolumeViewMode(com.Hecate.puppet.volume.VolumeReferenceView.Mode.SOLID);return;}

        EditorPuppetRenderer renderer=puppetTestScene.getPuppetRenderer();
        for(EditorBone bone:puppetTestScene.getTestSkeleton().getAllBones()) {
            if(bone.isModelEnabled()||bone.getVolumeDefinition()!=null&&bone.getVolumeDefinition().isSkeletonOnly())continue;
            if(bone.getVolumeDefinition()!=null){var existing=bone.getVolumeDefinition().copy();existing.getBakedPixels().setEnabled(true);existing.getStrip3D().setEnabled(true);bone.setVolumeDefinition(existing);var existingPart=renderer.getPartRenderer(bone.getName());if(existingPart!=null)existingPart.refreshVolumeAppearance();continue;}
            var part=renderer.getPartRenderer(bone.getName());
            var d=new com.Hecate.puppet.volume.VolumeDefinition();d.getBakedPixels().setEnabled(true);d.getStrip3D().setEnabled(true);
            if(sample) {
                d.getStrip3D().setEnabled(true);
                if(bone.getName().equals("Body"))d.setDimensions(40,56,24);
                else if(bone.getName().equals("Head"))d.setDimensions(32,32,32);
                else d.setDimensions(16,32,16);
                bone.setRotationStripEnabled(true);bone.setStripFrameWidthPx(d.getWidthPx());bone.setStripFrameHeightPx(d.getHeightPx());
                if(part!=null){part.setWidth(d.width());part.setHeight(d.height());}
            } else if(part!=null) d.setDimensions(Math.max(1,Math.min(4096,Math.round(part.getWidth()*32))),Math.max(1,Math.min(4096,Math.round(part.getHeight()*32))),32);
            bone.setVolumeDefinition(d);
        }
        renderer.setVolumeViewMode(com.Hecate.puppet.volume.VolumeReferenceView.Mode.SOLID);
    }
    public void requestVolumePart() {
        if(volumeMode){requestBlenderModel();return;}
        EditorSkeleton skeleton=puppetTestScene.getTestSkeleton();
        EditorBone parent=getVolumeSelectedBone();
        java.awt.EventQueue.invokeLater(() -> {
            var definition=VolumePartDialog.show(new com.Hecate.puppet.volume.VolumeDefinition());
            if(definition!=null)enqueue(() -> {
                if(puppetTestScene.getTestSkeleton()!=skeleton)return;
                if(parent!=null && skeleton.findBone(parent.getName())==parent){collectAllBones();for(int i=0;i<allBones.length;i++)if(allBones[i]==parent)selectBone(i);}
                createVolumePart(definition);
            });
        });
    }
    public void requestBlenderModel(){
        EditorSkeleton expected=puppetTestScene.getTestSkeleton();EditorBone parent=getVolumeSelectedBone();
        java.awt.EventQueue.invokeLater(()->{javax.swing.JFileChooser chooser=new javax.swing.JFileChooser();chooser.setDialogTitle("导入 Blender 模型 · 1 米 = 25 像素");chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Blender 导出的 glTF 2.0（GLB / glTF）","glb","gltf"));
            if(chooser.showOpenDialog(null)!=javax.swing.JFileChooser.APPROVE_OPTION)return;java.nio.file.Path path=chooser.getSelectedFile().toPath();
            enqueue(()->{if(puppetTestScene.getTestSkeleton()!=expected)return;try{applyBlenderParts(com.Hecate.puppet.pixel.BlenderPixelImporter.load(path),parent);}catch(Exception e){java.awt.EventQueue.invokeLater(()->EditorDialogs.message("导入模型失败："+e.getMessage()));}});
        });
    }
    private void applyBlenderParts(java.util.List<com.Hecate.puppet.pixel.BlenderPixelImporter.Part> parts,EditorBone requestedParent){
        EditorSkeleton skeleton=puppetTestScene.getTestSkeleton();EditorPuppetRenderer renderer=puppetTestScene.getPuppetRenderer();
        EditorBone oldRoot=skeleton.getRootBone();EditorBone attachment=requestedParent!=null&&skeleton.findBone(requestedParent.getName())==requestedParent?requestedParent:oldRoot;
        boolean createRoot=attachment==null;
        EditorBone root=createRoot?new EditorBone("ModelRoot"):attachment;
        if(createRoot){var rootDefinition=new com.Hecate.puppet.volume.VolumeDefinition();rootDefinition.setSkeletonOnly(true);rootDefinition.setPixelsPerUnit(com.Hecate.puppet.pixel.PixelMetrics.PIXELS_PER_METRE);root.setVolumeDefinition(rootDefinition);root.setRestPosition(new Vector3f());root.setRestRotation(new Quaternion());root.setRestScale(Vector3f.UNIT_XYZ.clone());root.resetToRestPose();}
        var bones=new java.util.ArrayList<EditorBone>();java.util.Set<String> names=new java.util.HashSet<>();for(var existing:skeleton.getAllBones())names.add(existing.getName());names.add(root.getName());
        // Preflight all captures before applying a command, so failed imports cannot leave half a model.
        var leases=new java.util.ArrayList<com.Hecate.puppet.pixel.PixelAtlasCache.Lease>();
        try{
            for(var part:parts){leases.add(com.Hecate.puppet.pixel.PixelAtlasCache.SHARED.acquire(part.definition()));String name=part.name();int index=2;while(!names.add(name))name=part.name()+"_"+(index++);EditorBone bone=new EditorBone(name);bone.setVolumeDefinition(part.definition());bone.setRestPosition(part.position().clone());bone.setRestRotation(part.rotation().clone());bone.setRestScale(Vector3f.UNIT_XYZ.clone());bone.resetToRestPose();bone.setRotationStripEnabled(true);bone.setStripFrameWidthPx(part.definition().getWidthPx());bone.setStripFrameHeightPx(part.definition().getHeightPx());bone.setStripWidth(part.definition().width());bone.setStripHeight(part.definition().height());bones.add(bone);}
            commandManager.executeCommand(new Command(){
                public void execute(){if(createRoot)skeleton.setRootBone(root);for(int i=0;i<bones.size();i++){EditorBone parent=parts.get(i).parent()<0?root:bones.get(parts.get(i).parent());parent.addChild(bones.get(i));skeleton.addBone(bones.get(i));renderer.addPartRenderer(bones.get(i));}renderer.setVolumeViewMode(com.Hecate.puppet.volume.VolumeReferenceView.Mode.APPEARANCE);refreshVolumeStructure(bones.get(0));if(createRoot)framePixelModel();}
                public void undo(){for(int i=bones.size()-1;i>=0;i--){renderer.removePartRenderer(bones.get(i).getName());skeleton.removeBone(bones.get(i).getName());selectedBones.remove(bones.get(i));}if(createRoot){skeleton.removeBone(root.getName());skeleton.updateRootBoneReference(oldRoot);}refreshVolumeStructure(oldRoot);}
                public String getDescription(){return "导入 Blender 刚性部件 · 1 米 = 25 像素";}
            });
        }finally{for(var lease:leases)lease.close();}
    }
    private void framePixelModel(){
        com.jme3.bounding.BoundingBox bounds=null;
        for(EditorBone b:puppetTestScene.getTestSkeleton().getAllBones()){var d=b.getVolumeDefinition();if(d==null||d.isSkeletonOnly())continue;var mesh=com.Hecate.puppet.volume.VolumeMeshFactory.create(d);var position=new Vector3f();var rotation=new Quaternion();var scale=new Vector3f();b.getWorldTransform(position,rotation,scale);var transform=new com.jme3.math.Transform(position,rotation,Vector3f.UNIT_XYZ).combineWithParent(puppetTestScene.getPuppetRenderer().getPuppetNode().getWorldTransform());var box=(com.jme3.bounding.BoundingBox)mesh.getBound().transform(transform);bounds=bounds==null?box:(com.jme3.bounding.BoundingBox)bounds.merge(box);}
        if(bounds==null)return;cameraPivotPoint.set(bounds.getCenter());float radius=new Vector3f(bounds.getXExtent(),bounds.getYExtent(),bounds.getZExtent()).length();cameraDistance=Math.max(.2f,radius/FastMath.tan(22.5f*FastMath.DEG_TO_RAD)*1.2f);cameraAngleX=0;cameraAngleY=0;volumeOrthographic=true;updateCameraPosition();updateCameraDirection();
    }
    public boolean isVolumeBonesVisible(){return volumeBonesVisible;}
    public void toggleVolumeBones(){toggleBoneLines(!volumeBonesVisible);}
    boolean volumeBoneVisible(EditorBone bone){return bone!=null&&puppetTestScene!=null&&puppetTestScene.getTestSkeleton().findBone(bone.getName())==bone&&!hiddenParts.contains(bone.getName());}
    void commitVolumeTransform(EditorBone bone,Quaternion rotation,Vector3f scale){
        if(!isCurrentVolumeBone(bone))return;enterEditMode();
        Vector3f p=bone.getLocalPosition().clone(),oldP=bone.getRestPosition().clone(),oldS=bone.getRestScale().clone();Quaternion oldR=bone.getRestRotation().clone();
        Quaternion targetRotation=rotation.clone();Vector3f targetScale=scale.clone();
        SetBoneTransformCommand pose=new SetBoneTransformCommand(bone,p,bone.getLocalRotation(),bone.getLocalScale(),p,targetRotation,targetScale);
        commandManager.executeCommand(new Command(){public void execute(){pose.execute();bone.setRestPosition(p.clone());bone.setRestRotation(targetRotation.clone());bone.setRestScale(targetScale.clone());}
            public void undo(){pose.undo();bone.setRestPosition(oldP.clone());bone.setRestRotation(oldR.clone());bone.setRestScale(oldS.clone());}public String getDescription(){return "拖动旋转／缩放骨骼";}});
    }
    public void setVolumeTool(int mode){if(volumeTools!=null)volumeTools.setMode(mode);}
    public int getVolumeTool(){return volumeTools==null?0:volumeTools.getMode();}
    public void toggleVolumeGrid(){if(volumeTools!=null)volumeTools.toggleGrid();}
    public boolean isVolumeGridVisible(){return volumeTools==null||volumeTools.isGridVisible();}
    boolean volumeViewportAllows(int x,int y){return editorUI!=null&&editorUI.isVisible()&&!editorUI.blocksVolumeViewport(x,y)&&!editorUI.getSliderColumnPanel().getRotationStripSelectorPanel().isVisible()&&!editorUI.getSliderColumnPanel().getEnlargedPreviewPanel().isVisible();}
    EditorPuppetRenderer volumeSceneRenderer(){return puppetTestScene==null?null:puppetTestScene.getPuppetRenderer();}
    Vector3f volumeViewPivot(){return cameraPivotPoint.clone();}
    com.jme3.scene.Node createVolumeHint(String text){return editorUI.createVolumeHint(text);}
    boolean volumeToolsVisible(){return editorUI!=null&&editorUI.isVisible();}
    boolean volumeBoneEditable(EditorBone bone){return bone!=null&&isCurrentVolumeBone(bone)&&!hiddenParts.contains(bone.getName())&&!bone.isModelEnabled()&&bone.getVolumeDefinition()!=null;}
    void beginVolumeDrag(){enterEditMode();}
    void commitVolumeMove(EditorBone bone,int axis,float value){applyVolumePose(bone,"p"+"xyz".charAt(axis),value);}
    void commitVolumeSize(EditorBone bone,com.Hecate.puppet.volume.VolumeDefinition d){applyVolumeDefinition(bone,d);}
    @Override public void loseFocus(){isShiftPanningCamera=false;shiftPanCamera=null;isDraggingCamera=false;isPanningCamera=false;isShiftPressed=false;if(editorUI!=null&&editorUI.getPartListPanel()!=null)editorUI.getPartListPanel().setShiftPressed(false);if(volumeTools!=null)volumeTools.cancel();super.loseFocus();}
    public void attachVolumeToSelected() { if(getVolumeSelectedBone()!=null){var d=new com.Hecate.puppet.volume.VolumeDefinition();d.getBakedPixels().setEnabled(volumeMode);d.getStrip3D().setEnabled(volumeMode);applyVolumeDefinition(selectedBone,d);} }
    public void setVolumeShape(com.Hecate.puppet.volume.VolumeDefinition.Shape shape) {
        EditorBone b=getVolumeSelectedBone();if(b==null||b.getVolumeDefinition()==null)return;
        var d=b.getVolumeDefinition().copy();d.setShape(shape);applyVolumeDefinition(b,d);
    }
    private void applyVolumeDefinition(EditorBone bone,com.Hecate.puppet.volume.VolumeDefinition d) {
        if(!isCurrentVolumeBone(bone)||bone.isModelEnabled())return;
        commandManager.executeCommand(new SetVolumeDefinitionCommand(bone,d,()->{
            var part=puppetTestScene.getPuppetRenderer().getPartRenderer(bone.getName());if(part!=null)part.refreshVolumeAppearance();
        },volumeMode));
    }
    public void editVolumeDimensions(){
        EditorBone bone=getVolumeSelectedBone();if(bone==null||bone.getVolumeDefinition()==null)return;var snapshot=bone.getVolumeDefinition().copy();
        java.awt.EventQueue.invokeLater(()->{int[] size=VolumeDimensionsDialog.show(snapshot);if(size!=null)enqueue(()->{if(!isCurrentVolumeBone(bone)||bone.getVolumeDefinition()==null)return;var next=bone.getVolumeDefinition().copy();next.setDimensions(size[0],size[1],size[2]);applyVolumeDefinition(bone,next);});});
    }
    public void editVolumeCoverTarget(){
        EditorBone bone=getVolumeSelectedBone();if(bone==null||bone.getVolumeDefinition()==null)return;
        if(!bone.getVolumeDefinition().isStrip3DEnabled()){java.awt.EventQueue.invokeLater(()->EditorDialogs.message("请先在图像面板启用三维条带，再设置包覆目标。"));return;}
        var names=new java.util.ArrayList<String>();
        for(var candidate:puppetTestScene.getTestSkeleton().getAllBones())if(candidate!=bone&&!candidate.isModelEnabled()&&candidate.getVolumeDefinition()!=null&&candidate.getVolumeDefinition().isStrip3DEnabled())names.add(candidate.getName());
        names.sort(String::compareTo);String current=bone.getVolumeDefinition().getCoverTarget();
        java.awt.EventQueue.invokeLater(()->{
            javax.swing.JComboBox<String> choice=new javax.swing.JComboBox<>();choice.setName("coverTarget");choice.addItem("无（正常空间遮挡）");for(String name:names)choice.addItem(name);
            int selected=current==null?-1:names.indexOf(current);choice.setSelectedIndex(selected+1);
            javax.swing.JPanel panel=new javax.swing.JPanel(new java.awt.GridLayout(0,1,6,8));panel.add(new javax.swing.JLabel("外层部件："+bone.getName()));panel.add(choice);
            panel.add(new javax.swing.JLabel("外层有色像素覆盖目标；透明孔洞露出内层。"));panel.add(new javax.swing.JLabel("可逐层指定，单个部件最多接受 8 个外层；不允许循环。"));
            if(current!=null&&selected<0)panel.add(new javax.swing.JLabel("原目标不可用："+current+"，可重新选择或清除。"));
            if(EditorDialogs.confirm(panel,"设置包覆目标")==javax.swing.JOptionPane.OK_OPTION){String target=choice.getSelectedIndex()==0?null:names.get(choice.getSelectedIndex()-1);
                enqueue(()->{if(!isCurrentVolumeBone(bone)||bone.getVolumeDefinition()==null)return;
                    try{applyVolumeCoverTarget(bone,target);}catch(IllegalArgumentException e){java.awt.EventQueue.invokeLater(()->EditorDialogs.message(e.getMessage()));}
                });
            }
        });
    }
    private void applyVolumeCoverTarget(EditorBone bone,String target){
        java.util.Map<String,String> targets=new java.util.LinkedHashMap<>();
        for(var part:puppetTestScene.getTestSkeleton().getAllBones())if(!part.isModelEnabled()&&part.getVolumeDefinition()!=null&&part.getVolumeDefinition().isStrip3DEnabled())targets.put(part.getName(),part.getVolumeDefinition().getCoverTarget());
        if(target!=null&&!targets.containsKey(target))throw new IllegalArgumentException("目标已被删除或不再是三维条带部件，请重新选择。");
        targets.put(bone.getName(),target);com.Hecate.puppet.volume.VolumeOcclusion.resolve(targets);
        var next=bone.getVolumeDefinition().copy();next.setCoverTarget(target);applyVolumeDefinition(bone,next);
    }
    public void editSelectionWidth(){
        EditorBone bone=getVolumeSelectedBone();if(bone==null||bone.getVolumeDefinition()==null)return;
        if(bone.getVolumeDefinition().isBakedPixelsEnabled()){editSelectionShape();return;}
        var snapshot=bone.getVolumeDefinition().getStrip3D().copy();
        java.awt.EventQueue.invokeLater(()->{Integer width=SelectionWidthDialog.show(snapshot,assetManager);if(width!=null)enqueue(()->{
            if(!isCurrentVolumeBone(bone)||bone.getVolumeDefinition()==null)return;var next=bone.getVolumeDefinition().copy();
            if(next.getStrip3D().getFrameWidth()!=snapshot.getFrameWidth()||next.getStrip3D().getFrameHeight()!=snapshot.getFrameHeight()){
                java.awt.EventQueue.invokeLater(()->EditorDialogs.message("贴图尺寸已改变，请重新打开选框宽度设置。"));return;
            }
            next.getStrip3D().setSampleWidth(width);applyVolumeDefinition(bone,next);
        });});
    }
    public void regeneratePixelSilhouettes(){
        EditorBone bone=getVolumeSelectedBone();if(bone==null||bone.getVolumeDefinition()==null)return;
        var d=bone.getVolumeDefinition().copy();d.getBakedPixels().setEnabled(true);d.getStrip3D().setEnabled(true);applyVolumeDefinition(bone,d);
    }
    public String getPixelCaptureStatus(){
        var cache=com.Hecate.puppet.pixel.PixelAtlasCache.SHARED;
        return "剪影："+String.format(java.util.Locale.ROOT,"%.1f",cache.bytes()/1048576.0)+" / 64 MB";
    }
    public com.Hecate.puppet.pixel.PixelRingTexture.Requirement getPixelTextureRequirement(){
        var bone=getVolumeSelectedBone();if(bone==null)return null;
        var part=puppetTestScene.getPuppetRenderer().getPartRenderer(bone.getName());
        var strip=part==null?null:part.getStrip3DView();var view=strip==null?null:strip.getBakedView();
        return view==null||view.atlas()==null?null:com.Hecate.puppet.pixel.PixelRingTexture.requirement(view.atlas());
    }
    public String getPixelTextureSize(){var r=getPixelTextureRequirement();return r==null?"贴图尺寸：等待剪影":"宽度自由 · 建议高"+r.recommendedHeight()+" px";}
    public String getPixelTextureStep(){var r=getPixelTextureRequirement();return r==null?"一圈 20 张剪影":"20 步整列移动 · 360° 循环";}
    public String getPixelCaptureCount(){return "生成次数："+com.Hecate.puppet.pixel.PixelAtlasCache.SHARED.builds()+"（动画不增加）";}
    public void editPixelCaptureSettings(){
        EditorBone bone=getVolumeSelectedBone();if(bone==null||bone.getVolumeDefinition()==null)return;var snapshot=bone.getVolumeDefinition().copy();
        java.awt.EventQueue.invokeLater(()->{
            var options=snapshot.getBakedPixels();javax.swing.JPanel panel=new javax.swing.JPanel(new java.awt.GridLayout(0,2,8,8));
            javax.swing.JSpinner lower=new javax.swing.JSpinner(new javax.swing.SpinnerNumberModel((double)options.elevation(0),-89.0,-1.0,1.0));
            javax.swing.JSpinner upper=new javax.swing.JSpinner(new javax.swing.SpinnerNumberModel((double)options.elevation(2),1.0,89.0,1.0));
            javax.swing.JSpinner transition=new javax.swing.JSpinner(new javax.swing.SpinnerNumberModel((double)options.getTransitionDegrees(),1.0,20.0,1.0));
            javax.swing.JSpinner ox=new javax.swing.JSpinner(new javax.swing.SpinnerNumberModel(options.getTextureOriginX(),-65536,65536,1)),oy=new javax.swing.JSpinner(new javax.swing.SpinnerNumberModel(options.getTextureOriginY(),-65536,65536,1));
            javax.swing.JSpinner priority=new javax.swing.JSpinner(new javax.swing.SpinnerNumberModel(options.getPriority(),-100,100,1));
            javax.swing.JSpinner perspective=new javax.swing.JSpinner(new javax.swing.SpinnerNumberModel((double)Math.round(options.getPerspectiveStrength()*100),0.0,20.0,1.0));
            String[] labels={"透视强度（%，0 为正交）","下斜视仰角（度）","上斜视仰角（度）","逐行过渡范围（度）","条带起点 X（整列）","条带起点 Y（整行）","关节接缝优先级"};javax.swing.JSpinner[] fields={perspective,lower,upper,transition,ox,oy,priority};
            for(int i=0;i<labels.length;i++){panel.add(new javax.swing.JLabel(labels[i]));panel.add(fields[i]);}
            if(EditorDialogs.confirm(panel,"五组剪影 · 平视 0°、顶底 ±90°固定，每组 20 张")!=javax.swing.JOptionPane.OK_OPTION)return;
            enqueue(()->{if(!isCurrentVolumeBone(bone)||bone.getVolumeDefinition()==null)return;var next=bone.getVolumeDefinition().copy();var value=next.getBakedPixels();
                value.setPerspectiveStrength(((Number)perspective.getValue()).floatValue()/100);value.setElevations(((Number)lower.getValue()).floatValue(),((Number)upper.getValue()).floatValue());value.setTransitionDegrees(((Number)transition.getValue()).floatValue());
                value.setTextureOrigin((int)ox.getValue(),(int)oy.getValue());value.setPriority((int)priority.getValue());applyVolumeDefinition(bone,next);
            });
        });
    }
    public void exportPixelAssemblyCheck(){
        var parts=new java.util.ArrayList<com.Hecate.puppet.pixel.PixelAssemblyPreview.Part>();var leases=new java.util.ArrayList<com.Hecate.puppet.pixel.PixelAtlasCache.Lease>();
        try{for(EditorBone b:puppetTestScene.getTestSkeleton().getAllBones()){var d=b.getVolumeDefinition();if(d==null||d.isSkeletonOnly()||!d.isBakedPixelsEnabled())continue;var lease=com.Hecate.puppet.pixel.PixelAtlasCache.SHARED.acquire(d);leases.add(lease);var position=new Vector3f();var rotation=new Quaternion();var scale=new Vector3f();b.getWorldTransform(position,rotation,scale);parts.add(new com.Hecate.puppet.pixel.PixelAssemblyPreview.Part(d.copy(),lease.atlas(),new com.jme3.math.Transform(position,rotation,scale),b.getPriority(),BakedSilhouetteDialog.loadSources(d,assetManager)));}}
        catch(Exception e){leases.forEach(com.Hecate.puppet.pixel.PixelAtlasCache.Lease::close);java.awt.EventQueue.invokeLater(()->EditorDialogs.message(e.getMessage()));return;}
        java.awt.EventQueue.invokeLater(()->{try{var chooser=new javax.swing.JFileChooser();chooser.setDialogTitle("导出当前姿势的整人 20 方向检查图");chooser.setSelectedFile(new java.io.File("人物-20方向.png"));if(chooser.showSaveDialog(null)!=javax.swing.JFileChooser.APPROVE_OPTION)return;java.nio.file.Path path=chooser.getSelectedFile().toPath();if(!path.toString().toLowerCase(java.util.Locale.ROOT).endsWith(".png"))path=java.nio.file.Path.of(path+".png");if(java.nio.file.Files.exists(path)&&EditorDialogs.confirm("将覆盖同名检查图。","保存检查图")!=javax.swing.JOptionPane.OK_OPTION)return;javax.imageio.ImageIO.write(com.Hecate.puppet.pixel.PixelAssemblyPreview.contactSheet(parts),"png",path.toFile());EditorDialogs.message("已导出当前姿势的 20 方向检查图；部件剪影没有重新生成。");}catch(Exception e){EditorDialogs.message(e.getMessage());}finally{leases.forEach(com.Hecate.puppet.pixel.PixelAtlasCache.Lease::close);}});
    }
    public void editPixelPalette(){
        EditorBone bone=getVolumeSelectedBone();if(bone==null||bone.getVolumeDefinition()==null)return;var snapshot=bone.getVolumeDefinition().getBakedPixels().copy();
        java.awt.EventQueue.invokeLater(()->{var result=PixelPaletteDialog.show(snapshot);if(result!=null)enqueue(()->{if(!isCurrentVolumeBone(bone)||bone.getVolumeDefinition()==null)return;var next=bone.getVolumeDefinition().copy();next.getBakedPixels().setPalette(result);applyVolumeDefinition(bone,next);});});
    }
    public void editPixelTint(){
        EditorBone bone=getVolumeSelectedBone();if(bone==null||bone.getVolumeDefinition()==null)return;float[] tint=bone.getVolumeDefinition().getBakedPixels().getTint();
        java.awt.EventQueue.invokeLater(()->{java.awt.Color result=javax.swing.JColorChooser.showDialog(null,"部件颜色倍率（白色恢复原色）",new java.awt.Color(tint[0],tint[1],tint[2]));
            if(result!=null)enqueue(()->{if(!isCurrentVolumeBone(bone)||bone.getVolumeDefinition()==null)return;var next=bone.getVolumeDefinition().copy();next.getBakedPixels().setTint(result.getRed()/255f,result.getGreen()/255f,result.getBlue()/255f);applyVolumeDefinition(bone,next);});
        });
    }
    public void editBakedSilhouette(){
        EditorBone bone=getVolumeSelectedBone();if(bone==null||bone.getVolumeDefinition()==null)return;var snapshot=bone.getVolumeDefinition().copy();
        // Keep an extra lease while the modal painter is open; no animation-dependent capture.
        final com.Hecate.puppet.pixel.PixelAtlasCache.Lease lease;
        try{lease=com.Hecate.puppet.pixel.PixelAtlasCache.SHARED.acquire(snapshot);}catch(Exception e){java.awt.EventQueue.invokeLater(()->EditorDialogs.message(e.getMessage()));return;}
        java.awt.EventQueue.invokeLater(()->{try{
            var result=BakedSilhouetteDialog.show(snapshot,lease.atlas(),BakedSilhouetteDialog.loadSources(snapshot,assetManager));
            if(result!=null)enqueue(()->{if(!isCurrentVolumeBone(bone)||bone.getVolumeDefinition()==null)return;var next=bone.getVolumeDefinition().copy();
                if(!com.Hecate.puppet.pixel.PixelAtlasBaker.key(next).equals(com.Hecate.puppet.pixel.PixelAtlasBaker.key(snapshot))){java.awt.EventQueue.invokeLater(()->EditorDialogs.message("形状已改变，请重新打开剪影画板。"));return;}
                for(int i=0;i<com.Hecate.puppet.pixel.PixelAtlas.VIEWS;i++)next.getBakedPixels().setEdit(i,result.getBakedPixels().getEdit(i));applyVolumeDefinition(bone,next);
            });
        }catch(Exception e){EditorDialogs.message(e.getMessage());}finally{lease.close();}});
    }
    private com.Hecate.puppet.volume.Strip3DDefinition showPixelSelection(com.Hecate.puppet.volume.VolumeDefinition d){try(var lease=com.Hecate.puppet.pixel.PixelAtlasCache.SHARED.acquire(d)){return SelectionShapeDialog.showBaked(d,lease.atlas(),assetManager);}catch(Exception e){EditorDialogs.message(e.getMessage());return null;}}
    public void editSelectionShape(){
        EditorBone bone=getVolumeSelectedBone();if(bone==null||bone.getVolumeDefinition()==null)return;var definition=bone.getVolumeDefinition().copy();var snapshot=definition.getStrip3D().copy();
        java.awt.EventQueue.invokeLater(()->{var result=definition.isBakedPixelsEnabled()?showPixelSelection(definition):SelectionShapeDialog.show(snapshot,assetManager);if(result!=null)enqueue(()->{
            if(!isCurrentVolumeBone(bone)||bone.getVolumeDefinition()==null)return;var next=bone.getVolumeDefinition().copy();
            if(next.getStrip3D().getFrameWidth()!=result.getFrameWidth()||next.getStrip3D().getFrameHeight()!=result.getFrameHeight()){
                java.awt.EventQueue.invokeLater(()->EditorDialogs.message("贴图尺寸已改变，请重新打开选框形状画板。"));return;
            }
            next.getStrip3D().copyWindowFrom(result);applyVolumeDefinition(bone,next);
        });});
    }
    public void editRowImportance(){
        EditorBone bone=getVolumeSelectedBone();if(bone==null||bone.getVolumeDefinition()==null)return;if(bone.getVolumeDefinition().isBakedPixelsEnabled()){editStrip3DTextures();return;}var snapshot=bone.getVolumeDefinition().getStrip3D().copy();
        java.awt.EventQueue.invokeLater(()->{var result=RowImportanceDialog.show(snapshot,assetManager);if(result!=null)enqueue(()->{if(!isCurrentVolumeBone(bone)||bone.getVolumeDefinition()==null)return;var next=bone.getVolumeDefinition().copy();
            if(next.getStrip3D().getFrameHeight()!=result.getFrameHeight()){java.awt.EventQueue.invokeLater(()->EditorDialogs.message("贴图高度已改变，请重新打开行重要度编辑器。"));return;}
            next.getStrip3D().setImportance(result.getImportance());next.getStrip3D().setImportanceEnabled(result.isImportanceEnabled());applyVolumeDefinition(bone,next);});});
    }
    public void editVolumeNumber(String key) {
        if(key.equals("width")||key.equals("height")||key.equals("depth")){editVolumeDimensions();return;}
        if(volumeMode&&key.equals("density")){java.awt.EventQueue.invokeLater(()->EditorDialogs.message("预生成像素模式固定为 1 米 = 25 像素。"));return;}
        EditorBone bone=getVolumeSelectedBone();if(bone==null||bone.getVolumeDefinition()==null)return;
        var d=bone.getVolumeDefinition();float value=switch(key){case "width"->d.getWidthPx();case "height"->d.getHeightPx();case "depth"->d.getDepthPx();case "ratio"->d.getTopRatio();case "density"->d.getPixelsPerUnit();case "yaw"->d.getLightYaw();case "elevation"->d.getLightElevation();default->d.getLightStrength();};
        boolean integer=key.equals("width")||key.equals("height")||key.equals("depth");
        askVolumeNumber(integer?"尺寸（1–4096 整数像素）":"修改参数",value,integer,n->{
            if(!isCurrentVolumeBone(bone)||bone.getVolumeDefinition()==null)return;
            var next=bone.getVolumeDefinition().copy();
            switch(key){case "width":next.setDimensions(n.intValue(),next.getHeightPx(),next.getDepthPx());break;
                case "height":next.setDimensions(next.getWidthPx(),n.intValue(),next.getDepthPx());break;
                case "depth":next.setDimensions(next.getWidthPx(),next.getHeightPx(),n.intValue());break;
                case "ratio":next.setTopRatio(n);break;case "density":next.setPixelsPerUnit(n);break;
                case "yaw":next.setLight(n,next.getLightElevation(),next.getLightStrength());break;
                case "elevation":next.setLight(next.getLightYaw(),n,next.getLightStrength());break;
                case "strength":next.setLight(next.getLightYaw(),next.getLightElevation(),n);break;}
            applyVolumeDefinition(bone,next);
        });
    }
    private void askVolumeNumber(String title,float initial,boolean integer,java.util.function.Consumer<Float> change) {
        java.awt.EventQueue.invokeLater(()->{
            String input=EditorDialogs.input(title,integer?Integer.toString((int)initial):Float.toString(initial));if(input==null)return;
            try {float value=Float.parseFloat(input.trim());if(!Float.isFinite(value)||(integer&&value!=Math.rint(value)))throw new IllegalArgumentException("请输入有效数值；像素尺寸必须为整数");
                enqueue(()->{try{change.accept(value);}catch(IllegalArgumentException e){java.awt.EventQueue.invokeLater(()->EditorDialogs.message(e.getMessage()));}});
            }catch(IllegalArgumentException e){EditorDialogs.message(e.getMessage());}
        });
    }
    public void editVolumePose(String key) {
        EditorBone bone=getVolumeSelectedBone();if(bone==null)return;int axis="xyz".indexOf(key.charAt(1));
        float initial=key.charAt(0)=='p'?bone.getLocalPosition().get(axis):key.charAt(0)=='s'?bone.getLocalScale().get(axis):bone.getLocalRotation().toAngles(null)[axis]*com.jme3.math.FastMath.RAD_TO_DEG;
        askVolumeNumber("局部骨骼姿态（旋转单位为度）",initial,false,n->applyVolumePose(bone,key,n));
    }
    private void applyVolumePose(EditorBone bone,String key,float value) {
        if(!isCurrentVolumeBone(bone))return;enterEditMode();int axis="xyz".indexOf(key.charAt(1));
        Vector3f p=bone.getLocalPosition().clone(),s=bone.getLocalScale().clone();Quaternion r=bone.getLocalRotation().clone();
        Vector3f oldP=bone.getRestPosition().clone(),oldS=bone.getRestScale().clone();Quaternion oldR=bone.getRestRotation().clone();
        switch(key.charAt(0)){case 'p':p.set(axis,value);break;case 's':if(Math.abs(value)<.001f)throw new IllegalArgumentException("缩放不能为零");s.set(axis,value);break;
            case 'r':float[] angles=r.toAngles(null);angles[axis]=value*com.jme3.math.FastMath.DEG_TO_RAD;r.fromAngles(angles);break;}
        SetBoneTransformCommand pose=new SetBoneTransformCommand(bone,bone.getLocalPosition(),bone.getLocalRotation(),bone.getLocalScale(),p,r,s);
        commandManager.executeCommand(new Command(){public void execute(){pose.execute();bone.setRestPosition(p.clone());bone.setRestRotation(r.clone());bone.setRestScale(s.clone());}
            public void undo(){pose.undo();bone.setRestPosition(oldP.clone());bone.setRestRotation(oldR.clone());bone.setRestScale(oldS.clone());}public String getDescription(){return "修改实体骨骼姿态";}});
    }
    public String getVolumeImageError(){String coverError=puppetTestScene.getPuppetRenderer().getVolumeOcclusionError();if(coverError!=null)return coverError;EditorBone b=getVolumeSelectedBone();if(b==null)return null;var part=puppetTestScene.getPuppetRenderer().getPartRenderer(b.getName());return part==null?null:part.getVolumeError();}
    static void copyPixelTextureSettings(com.Hecate.puppet.volume.VolumeDefinition target,com.Hecate.puppet.volume.VolumeDefinition edit){
        target.getBakedPixels().setTextureOrigin(edit.getBakedPixels().getTextureOriginX(),edit.getBakedPixels().getTextureOriginY());target.getBakedPixels().copyTextureRowsFrom(edit.getBakedPixels());
        target.getStrip3D().setEnabled(edit.isStrip3DEnabled());target.getStrip3D().copyOptionsFrom(edit.getStrip3D());
        for(var surface:com.Hecate.puppet.volume.Strip3DDefinition.Surface.values()){
            var from=edit.getStrip3D().layer(surface);var to=target.getStrip3D().layer(surface);
            to.setColorPath(from.getColorPath());to.setNormalPath(from.getNormalPath());to.setSampling(from.getOffsetX(),from.getOffsetY(),from.getTravelX(),from.getTravelY());
        }
    }
    /** Render-thread preview. The closing action records a single undoable edit. */
    void previewPixelTextureSettings(EditorBone bone,com.Hecate.puppet.volume.VolumeDefinition edit){
        if(!isCurrentVolumeBone(bone)||bone.getVolumeDefinition()==null||bone.isModelEnabled())return;
        var current=bone.getVolumeDefinition().copy();copyPixelTextureSettings(current,edit);bone.setVolumeDefinition(current);
        var renderer=puppetTestScene.getPuppetRenderer();var part=renderer.getPartRenderer(bone.getName());if(part!=null)part.refreshVolumeAppearance();
        renderer.setVolumeViewMode(com.Hecate.puppet.volume.VolumeReferenceView.Mode.APPEARANCE);
    }
    void finishPixelTextureSettings(EditorBone bone,com.Hecate.puppet.volume.VolumeDefinition original,com.Hecate.puppet.volume.VolumeDefinition edit){
        if(!isCurrentVolumeBone(bone)||bone.getVolumeDefinition()==null||bone.isModelEnabled())return;
        var baseline=bone.getVolumeDefinition().copy();copyPixelTextureSettings(baseline,original);bone.setVolumeDefinition(baseline);
        if(edit!=null){var after=baseline.copy();copyPixelTextureSettings(after,edit);after.getStrip3D().setEnabled(true);applyVolumeDefinition(bone,after);}
        else{var part=puppetTestScene.getPuppetRenderer().getPartRenderer(bone.getName());if(part!=null)part.refreshVolumeAppearance();}
    }
    public void editStrip3DTextures(){
        EditorBone bone=getVolumeSelectedBone();if(bone==null||bone.getVolumeDefinition()==null)return;
        var snapshot=bone.getVolumeDefinition().copy();
        java.awt.EventQueue.invokeLater(()->{
            if(snapshot.isBakedPixelsEnabled()){
                var result=PixelStripTextureDialog.show(snapshot,assetManager,edit->enqueue(()->previewPixelTextureSettings(bone,edit)));
                enqueue(()->finishPixelTextureSettings(bone,snapshot,result));
            }else{
                var result=Strip3DTextureDialog.show(snapshot,assetManager);
                if(result!=null)enqueue(()->{if(!isCurrentVolumeBone(bone)||bone.getVolumeDefinition()==null)return;var current=bone.getVolumeDefinition().copy();copyPixelTextureSettings(current,result);current.getStrip3D().setEnabled(true);applyVolumeDefinition(bone,current);});
            }
        });
    }
    public void importPixelWholeColor(){
        EditorBone bone=getVolumeSelectedBone();if(bone==null||bone.getVolumeDefinition()==null||!bone.getVolumeDefinition().isBakedPixelsEnabled())return;
        String title="导入整部件条带 PNG · "+getPixelTextureSize();
        java.awt.EventQueue.invokeLater(()->textureImportDialogs.choose(title,path->enqueue(()->{
            if(!isCurrentVolumeBone(bone)||bone.getVolumeDefinition()==null)return;
            try{var d=bone.getVolumeDefinition().copy();var image=com.Hecate.puppet.pixel.PixelRingTexture.readImage(assetManager,path);
                com.Hecate.puppet.pixel.PixelRingTexture.assign(d,com.Hecate.puppet.volume.Strip3DDefinition.Surface.SIDE,path,image);
                for(var surface:com.Hecate.puppet.volume.Strip3DDefinition.Surface.values())d.getStrip3D().layer(surface).setColorPath(path);
                applyVolumeDefinition(bone,d);puppetTestScene.getPuppetRenderer().setVolumeViewMode(com.Hecate.puppet.volume.VolumeReferenceView.Mode.APPEARANCE);
            }catch(Exception error){java.awt.EventQueue.invokeLater(()->textureImportDialogs.error("无法导入："+error.getMessage()));}
        })));
    }
    private final TextureImportDialogs textureImportDialogs=new TextureImportDialogs();
    public void importStrip3DColor(com.Hecate.puppet.volume.Strip3DDefinition.Surface surface){
        EditorBone bone=getVolumeSelectedBone();if(bone==null||bone.getVolumeDefinition()==null)return;
        String title="导入"+new String[]{"顶面","侧面","底面"}[surface.ordinal()]+"条带 PNG · "+getPixelTextureSize();
        java.awt.EventQueue.invokeLater(()->textureImportDialogs.choose(title,path->
            enqueue(()->{if(!isCurrentVolumeBone(bone)||bone.getVolumeDefinition()==null)return;try{var d=bone.getVolumeDefinition().copy();d.getStrip3D().layer(surface).setColorPath(path);if(d.isBakedPixelsEnabled()){var image=com.Hecate.puppet.pixel.PixelRingTexture.readImage(assetManager,path);com.Hecate.puppet.pixel.PixelRingTexture.assign(d,surface,path,image);}else com.Hecate.puppet.volume.Strip3DTextures.validateSources(d.getStrip3D(),assetManager);d.getStrip3D().setEnabled(true);applyVolumeDefinition(bone,d);
                if(isVolumeModelDisplay())toggleVolumeDisplay();
            }catch(Exception e){java.awt.EventQueue.invokeLater(()->textureImportDialogs.error("无法导入："+e.getMessage()));}})));
    }
    public void volumeImageAction(String action) {
        EditorBone bone=getVolumeSelectedBone();if(bone==null||bone.getVolumeDefinition()==null)return;
        switch(action){
            case "strip3d":var next=bone.getVolumeDefinition().copy();next.getStrip3D().setEnabled(!next.isStrip3DEnabled());applyVolumeDefinition(bone,next);break;
            case "strip3dSettings":editStrip3DTextures();break;
            case "surfaceContour":var contour=bone.getVolumeDefinition().copy();contour.setSurfaceContourSnapped(!contour.isSurfaceContourSnapped());applyVolumeDefinition(bone,contour);break;
            case "surfaceDepth":var depth=bone.getVolumeDefinition().copy();depth.setSurfaceDepthEnabled(!depth.isSurfaceDepthEnabled());applyVolumeDefinition(bone,depth);break;
            case "vertical":case "horizontal":case "importance":case "outline":
                var options=bone.getVolumeDefinition().copy();var sampling=options.getStrip3D();
                var slide=com.Hecate.puppet.volume.Strip3DDefinition.Sampling.SLIDE;var perspective=com.Hecate.puppet.volume.Strip3DDefinition.Sampling.PERSPECTIVE;
                if(action.equals("vertical"))sampling.setVertical(sampling.getVertical()==slide?perspective:slide);
                else if(action.equals("horizontal"))sampling.setHorizontal(sampling.getHorizontal()==slide?perspective:slide);
                else if(action.equals("outline"))sampling.setOutlineEnabled(!sampling.isOutlineEnabled());
                else sampling.setImportanceEnabled(!sampling.isImportanceEnabled());applyVolumeDefinition(bone,options);break;
            case "status":String error=getVolumeImageError();boolean captured=bone.getVolumeDefinition().isBakedPixelsEnabled();java.awt.EventQueue.invokeLater(()->JOptionPane.showMessageDialog(null,error==null?(captured?"剪影和条带正常；未导入贴图的层显示浅色剪影。":"贴图正常；未导入图片的层保持透明。"):error));break;
            case "strip":loadStripTexture();break;case "crop":editorUI.getSliderColumnPanel().openRotationStripSelector();break;
            case "refresh":puppetTestScene.getPuppetRenderer().getPartRenderer(bone.getName()).refreshVolumeAppearance();break;
            case "normal":java.awt.EventQueue.invokeLater(()->{JFileChooser picker=new JFileChooser();picker.setFileFilter(new FileNameExtensionFilter("法线 PNG","png"));
                if(picker.showOpenDialog(null)==JFileChooser.APPROVE_OPTION){String path=picker.getSelectedFile().getAbsolutePath();enqueue(()->{if(isCurrentVolumeBone(bone)&&bone.getVolumeDefinition()!=null){var d=bone.getVolumeDefinition().copy();d.setNormalTexturePath(path);applyVolumeDefinition(bone,d);}});}});break;
            default:var d=bone.getVolumeDefinition().copy();if(action.equals("lighting"))d.setLightingEnabled(!d.isLightingEnabled());else if(action.equals("clearNormal"))d.setNormalTexturePath(null);applyVolumeDefinition(bone,d);}
    }
}

