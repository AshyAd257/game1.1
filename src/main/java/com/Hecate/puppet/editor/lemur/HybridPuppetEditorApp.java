package com.Hecate.puppet.editor.lemur;

import com.jme3.app.SimpleApplication;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.input.RawInputListener;
import com.jme3.input.event.*;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.system.AppSettings;
import com.Hecate.puppet.PuppetTestScene;
import com.Hecate.puppet.core.PuppetRenderer;
import com.Hecate.puppet.core.PuppetPartRenderer;
import com.Hecate.puppet.core.Bone;
import com.Hecate.puppet.core.Skeleton;
import com.Hecate.puppet.config.PuppetConfig;
import com.Hecate.puppet.config.PuppetIO;
import com.Hecate.puppet.config.BoneConfig;
import com.Hecate.puppet.config.PartConfig;
import com.Hecate.puppet.config.Vec3Config;
import com.Hecate.puppet.config.QuatConfig;
import com.Hecate.puppet.editor.TimelinePanel;
import com.Hecate.puppet.editor.TTFontLoader;
import com.Hecate.puppet.editor.command.CommandManager;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;

/**
 * 混合版木偶编辑器
 * 使用 Lemur GUI 面板 + 保留原有的 TimelinePanel
 *
 * 这是方案 C 的实现：混合使用新旧 UI 系统
 */
public class HybridPuppetEditorApp extends SimpleApplication {

    // Lemur UI 管理器
    private LemurUIManager lemurUI;

    // 保留原有的 Timeline（太特殊，不用 Lemur 重写）
    private TimelinePanel timelinePanel;

    // 木偶场景
    private PuppetTestScene puppetTestScene;
    private Skeleton skeleton;
    private Bone[] allBones;
    private int selectedBoneIndex = 0;
    private Bone selectedBone;
    private PuppetPartRenderer selectedPartRenderer;  // 当前选中的部件渲染器

    // 相机控制
    private float cameraDistance = 15f;
    private float cameraAngleX = 0f;
    private float cameraAngleY = 0f;
    private boolean isDraggingCamera = false;
    private int lastMouseX = 0;
    private int lastMouseY = 0;

    // 当前方向
    private Bone.Direction currentDirection = Bone.Direction.FRONT;

    // 文件操作
    private String currentFilePath = null;
    private CommandManager commandManager;

    // 隐藏模式
    private boolean isHideModeEnabled = false;
    private java.util.Set<String> hiddenParts = new java.util.HashSet<>();

    public static void main(String[] args) {
        HybridPuppetEditorApp app = createEditor();
        app.start();
    }

    /**
     * 创建编辑器实例（供外部调用）
     * @return 配置好的编辑器实例
     */
    public static HybridPuppetEditorApp createEditor() {
        HybridPuppetEditorApp app = new HybridPuppetEditorApp();

        AppSettings settings = new AppSettings(true);
        settings.setTitle("Puppet Editor - 木偶编辑器 (Lemur UI)");
        settings.setWidth(1600);
        settings.setHeight(900);
        settings.setVSync(true);
        settings.setSamples(4);

        app.setSettings(settings);
        app.setShowSettings(false);
        return app;
    }

    @Override
    public void simpleInitApp() {
        // 设置背景
        viewPort.setBackgroundColor(new ColorRGBA(0.25f, 0.25f, 0.3f, 1f));

        // 禁用 FlyCam
        flyCam.setEnabled(false);
        inputManager.setCursorVisible(true);

        // 设置相机
        updateCameraPosition();

        // 添加光照
        setupLights();

        // 创建木偶场景
        createPuppetScene();

        // === 初始化 Lemur UI ===
        lemurUI = new LemurUIManager(this);
        lemurUI.initialize();
        lemurUI.createPanels();

        // 设置 Lemur UI 回调
        setupLemurCallbacks();

        // 更新部件列表
        if (skeleton != null) {
            lemurUI.updateBoneList(skeleton);
        }

        // === 创建原有的 TimelinePanel（保留） ===
        createTimelinePanel();

        // 设置输入
        setupInputs();

        // 选择第一个骨骼
        selectBone(0);

    }

    private void setupLights() {
        AmbientLight ambient = new AmbientLight();
        ambient.setColor(ColorRGBA.White.mult(0.4f));
        rootNode.addLight(ambient);

        DirectionalLight sun = new DirectionalLight();
        sun.setDirection(new Vector3f(-0.5f, -1f, -0.5f).normalizeLocal());
        sun.setColor(ColorRGBA.White.mult(0.8f));
        rootNode.addLight(sun);
    }

    private void createPuppetScene() {
        puppetTestScene = new PuppetTestScene(this);
        puppetTestScene.createTestPuppet(rootNode, new Vector3f(0, 5, 0));

        skeleton = puppetTestScene.getTestSkeleton();
        if (skeleton != null) {
            allBones = skeleton.getAllBones().toArray(new Bone[0]);
        }
    }

    private void createTimelinePanel() {
        // 使用原有的 TimelinePanel
        try {
            com.jme3.font.BitmapFont guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
            int screenWidth = cam.getWidth();
            int timelineHeight = 120;

            timelinePanel = new TimelinePanel(this, guiFont, 0, 0, screenWidth, timelineHeight);
            guiNode.attachChild(timelinePanel.getRootNode());

        } catch (Exception e) {

        }
    }

    private void setupLemurCallbacks() {
        lemurUI.setCallbacks(new LemurUIManager.EditorUICallbacks() {
            @Override
            public void onWidthChanged(float value) {
                if (selectedPartRenderer != null) {
                    selectedPartRenderer.setWidth(value);

                }
            }

            @Override
            public void onHeightChanged(float value) {
                if (selectedPartRenderer != null) {
                    selectedPartRenderer.setHeight(value);
                }
            }

            @Override
            public void onPriorityChanged(float value) {
                if (selectedBone != null) {
                    selectedBone.setPriority((int) value);
                    // 通知渲染器更新排序
                    if (puppetTestScene != null && puppetTestScene.getPuppetRenderer() != null) {
                        puppetTestScene.getPuppetRenderer().updateRenderOrder();
                    }
                }
            }

            @Override
            public void onPosXChanged(float value) {
                if (selectedPartRenderer != null) {
                    selectedPartRenderer.setOffsetX(value);
                }
            }

            @Override
            public void onPosYChanged(float value) {
                if (selectedPartRenderer != null) {
                    selectedPartRenderer.setOffsetY(value);
                }
            }

            @Override
            public void onPosZChanged(float value) {
                if (selectedPartRenderer != null) {
                    selectedPartRenderer.setOffsetZ(value);
                }
            }

            @Override
            public void onRotXChanged(float value) {
                if (selectedPartRenderer != null) {
                    selectedPartRenderer.setCustomRotationX(value);
                }
            }

            @Override
            public void onRotZChanged(float value) {
                if (selectedPartRenderer != null) {
                    selectedPartRenderer.setCustomRotationZ(value);
                }
            }

            @Override
            public void onGridSizeChanged(float value) {
            }

            @Override
            public void onNewFile() {
                doNewFile();
            }

            @Override
            public void onOpenFile() {
                doOpenFile();
            }

            @Override
            public void onSaveFile() {
                doSaveFile();
            }

            @Override
            public void onSaveAsFile() {
                doSaveAsFile();
            }

            @Override
            public void onExportFile() {
            }

            @Override
            public void onUndo() {
                if (commandManager != null && commandManager.canUndo()) {
                    commandManager.undo();
                    refreshUI();
                }
            }

            @Override
            public void onRedo() {
                if (commandManager != null && commandManager.canRedo()) {
                    commandManager.redo();
                    refreshUI();
                }
            }

            @Override
            public void onCopy() {
            }

            @Override
            public void onPaste() {
            }

            @Override
            public void onDelete() {
                doRemoveBone();
            }

            @Override
            public void onAddBone() {
                doAddBone();
            }

            @Override
            public void onRemoveBone() {
                doRemoveBone();
            }

            @Override
            public void onSetParent() {
                doSetParent();
            }

            @Override
            public void onClearParent() {
                doClearParent();
            }

            @Override
            public void onViewFront() {
                currentDirection = Bone.Direction.FRONT;
                cameraAngleX = 0;
                cameraAngleY = 0;
                updateCameraPosition();
            }

            @Override
            public void onViewBack() {
                currentDirection = Bone.Direction.BACK;
                cameraAngleX = 0;
                cameraAngleY = 180;
                updateCameraPosition();
            }

            @Override
            public void onViewLeft() {
                currentDirection = Bone.Direction.LEFT;
                cameraAngleX = 0;
                cameraAngleY = -90;
                updateCameraPosition();
            }

            @Override
            public void onViewRight() {
                currentDirection = Bone.Direction.RIGHT;
                cameraAngleX = 0;
                cameraAngleY = 90;
                updateCameraPosition();
            }

            @Override
            public void onHideMode() {
            }

            @Override
            public void onShowAll() {
            }

            @Override
            public void onPlay() {
            }

            @Override
            public void onStop() {
            }

            @Override
            public void onAddKeyframe() {
            }

            @Override
            public void onPartSelected(int boneIndex, String boneName) {
                selectBone(boneIndex);
            }
        });
    }

    private void setupInputs() {
        // 键盘快捷键
        inputManager.addMapping("ViewFront", new KeyTrigger(KeyInput.KEY_1));
        inputManager.addMapping("ViewBack", new KeyTrigger(KeyInput.KEY_2));
        inputManager.addMapping("ViewLeft", new KeyTrigger(KeyInput.KEY_3));
        inputManager.addMapping("ViewRight", new KeyTrigger(KeyInput.KEY_4));

        inputManager.addListener((ActionListener) (name, isPressed, tpf) -> {
            if (!isPressed) return;
            switch (name) {
                case "ViewFront" -> { cameraAngleY = 0; updateCameraPosition(); }
                case "ViewBack" -> { cameraAngleY = 180; updateCameraPosition(); }
                case "ViewLeft" -> { cameraAngleY = -90; updateCameraPosition(); }
                case "ViewRight" -> { cameraAngleY = 90; updateCameraPosition(); }
            }
        }, "ViewFront", "ViewBack", "ViewLeft", "ViewRight");

        // 鼠标输入（相机控制）
        inputManager.addRawInputListener(new RawInputListener() {
            @Override
            public void beginInput() {}

            @Override
            public void endInput() {}

            @Override
            public void onJoyAxisEvent(JoyAxisEvent evt) {}

            @Override
            public void onJoyButtonEvent(JoyButtonEvent evt) {}

            @Override
            public void onMouseMotionEvent(MouseMotionEvent evt) {
                if (isDraggingCamera) {
                    int deltaX = evt.getX() - lastMouseX;
                    int deltaY = evt.getY() - lastMouseY;

                    cameraAngleY += deltaX * 0.5f;
                    cameraAngleX -= deltaY * 0.5f;
                    cameraAngleX = Math.max(-89, Math.min(89, cameraAngleX));

                    updateCameraPosition();
                }
                lastMouseX = evt.getX();
                lastMouseY = evt.getY();

                // 鼠标滚轮缩放
                if (evt.getDeltaWheel() != 0) {
                    cameraDistance -= evt.getDeltaWheel() * 0.01f;
                    cameraDistance = Math.max(5, Math.min(50, cameraDistance));
                    updateCameraPosition();
                }
            }

            @Override
            public void onMouseButtonEvent(MouseButtonEvent evt) {
                if (evt.getButtonIndex() == MouseInput.BUTTON_RIGHT) {
                    isDraggingCamera = evt.isPressed();
                }
            }

            @Override
            public void onKeyEvent(KeyInputEvent evt) {}

            @Override
            public void onTouchEvent(TouchEvent evt) {}
        });
    }

    private void updateCameraPosition() {
        float radX = (float) Math.toRadians(cameraAngleX);
        float radY = (float) Math.toRadians(cameraAngleY);

        float x = (float) (cameraDistance * Math.cos(radX) * Math.sin(radY));
        float y = (float) (cameraDistance * Math.sin(radX)) + 5;
        float z = (float) (cameraDistance * Math.cos(radX) * Math.cos(radY));

        cam.setLocation(new Vector3f(x, y, z));
        cam.lookAt(new Vector3f(0, 5, 0), Vector3f.UNIT_Y);
    }

    private void selectBone(int index) {
        if (allBones == null || index < 0 || index >= allBones.length) return;

        selectedBoneIndex = index;
        selectedBone = allBones[index];

        // 获取对应的 PartRenderer
        if (puppetTestScene != null && puppetTestScene.getPuppetRenderer() != null) {
            selectedPartRenderer = puppetTestScene.getPuppetRenderer().getPartRenderer(selectedBone.getName());
        }

        // 更新 UI
        lemurUI.setSelectedBone(index);
        if (selectedPartRenderer != null) {
            lemurUI.updateSliderValues(selectedPartRenderer, selectedBone);
        }

    }

    @Override
    public void simpleUpdate(float tpf) {
        // 更新 Lemur UI
        if (lemurUI != null) {
            lemurUI.update(tpf);
        }

        // 更新木偶渲染
        if (puppetTestScene != null) {
            puppetTestScene.update(tpf);
        }
    }

    @Override
    public void destroy() {
        if (lemurUI != null) {
            lemurUI.cleanup();
        }
        super.destroy();
    }

    // ==================== 文件操作 ====================

    /**
     * 新建木偶
     */
    private void doNewFile() {
        int result = JOptionPane.showConfirmDialog(
            null,
            "新建木偶将清除当前内容，是否继续？",
            "新建确认",
            JOptionPane.YES_NO_OPTION
        );

        if (result == JOptionPane.YES_OPTION) {
            // 清除当前木偶
            if (puppetTestScene != null && puppetTestScene.getPuppetRenderer() != null) {
                puppetTestScene.getPuppetRenderer().cleanup();
            }

            // 创建新的骨骼
            skeleton = new Skeleton("NewPuppet");
            Bone rootBone = new Bone("Body");
            skeleton.addBone(rootBone);
            skeleton.setRootBone(rootBone);
            allBones = skeleton.getAllBones().toArray(new Bone[0]);

            // 更新UI
            currentFilePath = null;
            refreshUI();
            selectBone(0);

        }
    }

    /**
     * 打开文件
     */
    private void doOpenFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("打开木偶文件");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Puppet JSON (*.json)", "json"));

        // 设置默认目录
        File defaultDir = new File("puppets");
        if (defaultDir.exists()) {
            fileChooser.setCurrentDirectory(defaultDir);
        }

        int result = fileChooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                // 使用 PuppetIO 加载
                PuppetConfig config = PuppetIO.loadFromFile(selectedFile.getAbsolutePath());
                if (config != null) {
                    // 应用配置到当前木偶
                    applyPuppetConfig(config);
                    currentFilePath = selectedFile.getAbsolutePath();
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(
                    null,
                    "加载文件失败: " + e.getMessage(),
                    "错误",
                    JOptionPane.ERROR_MESSAGE
                );
                e.printStackTrace();
            }
        }
    }

    /**
     * 保存文件
     */
    private void doSaveFile() {
        if (currentFilePath == null) {
            doSaveAsFile();
        } else {
            saveToFile(currentFilePath);
        }
    }

    /**
     * 另存为
     */
    private void doSaveAsFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("保存木偶文件");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Puppet JSON (*.json)", "json"));

        // 设置默认目录
        File defaultDir = new File("puppets");
        if (!defaultDir.exists()) {
            defaultDir.mkdirs();
        }
        fileChooser.setCurrentDirectory(defaultDir);

        int result = fileChooser.showSaveDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            String path = selectedFile.getAbsolutePath();

            // 添加 .json 扩展名
            if (!path.toLowerCase().endsWith(".json")) {
                path += ".json";
            }

            saveToFile(path);
            currentFilePath = path;
        }
    }

    /**
     * 保存到指定文件
     */
    private void saveToFile(String filePath) {
        try {
            // 创建配置对象
            PuppetConfig config = createPuppetConfig();
            PuppetIO.saveToFile(config, filePath);

            JOptionPane.showMessageDialog(
                null,
                "保存成功!",
                "提示",
                JOptionPane.INFORMATION_MESSAGE
            );
        } catch (Exception e) {
            JOptionPane.showMessageDialog(
                null,
                "保存失败: " + e.getMessage(),
                "错误",
                JOptionPane.ERROR_MESSAGE
            );
            e.printStackTrace();
        }
    }

    /**
     * 创建木偶配置
     */
    private PuppetConfig createPuppetConfig() {
        PuppetConfig config = new PuppetConfig();
        config.setName(skeleton != null ? skeleton.getName() : "Puppet");

        if (skeleton != null) {
            for (Bone bone : skeleton.getAllBones()) {
                BoneConfig boneConfig = new BoneConfig(bone.getName());

                // 设置父骨骼名称
                if (bone.getParent() != null) {
                    boneConfig.setParentName(bone.getParent().getName());
                }

                // 设置 Rest Pose
                boneConfig.setRestPosition(new Vec3Config(
                    bone.getRestPosition().x,
                    bone.getRestPosition().y,
                    bone.getRestPosition().z
                ));
                boneConfig.setRestRotation(new QuatConfig(
                    bone.getRestRotation().getX(),
                    bone.getRestRotation().getY(),
                    bone.getRestRotation().getZ(),
                    bone.getRestRotation().getW()
                ));
                boneConfig.setRestScale(new Vec3Config(
                    bone.getRestScale().x,
                    bone.getRestScale().y,
                    bone.getRestScale().z
                ));

                // 获取渲染器配置
                PuppetPartRenderer renderer = null;
                if (puppetTestScene != null && puppetTestScene.getPuppetRenderer() != null) {
                    renderer = puppetTestScene.getPuppetRenderer().getPartRenderer(bone.getName());
                }

                if (renderer != null) {
                    PartConfig partConfig = new PartConfig();
                    partConfig.setWidth(renderer.getWidth());
                    partConfig.setHeight(renderer.getHeight());
                    partConfig.setOffset(new Vec3Config(renderer.getOffset()));
                    partConfig.setCustomRotationX(renderer.getCustomRotationX());
                    partConfig.setCustomRotationZ(renderer.getCustomRotationZ());
                    partConfig.setPriority(bone.getPriority());
                    boneConfig.setPartConfig(partConfig);
                }

                config.addBone(boneConfig);
            }
        }

        return config;
    }

    /**
     * 应用木偶配置
     */
    private void applyPuppetConfig(PuppetConfig config) {
        // TODO: 实现完整的配置应用逻辑
        // 这需要根据 PuppetConfig 的结构来实现
        refreshUI();
    }

    // ==================== 骨骼操作 ====================

    /**
     * 添加骨骼
     */
    private void doAddBone() {
        String boneName = JOptionPane.showInputDialog(
            null,
            "请输入新骨骼名称:",
            "添加骨骼",
            JOptionPane.PLAIN_MESSAGE
        );

        if (boneName != null && !boneName.trim().isEmpty()) {
            boneName = boneName.trim();

            // 检查名称是否已存在
            if (skeleton != null) {
                for (Bone bone : skeleton.getAllBones()) {
                    if (bone.getName().equals(boneName)) {
                        JOptionPane.showMessageDialog(
                            null,
                            "骨骼名称已存在!",
                            "错误",
                            JOptionPane.ERROR_MESSAGE
                        );
                        return;
                    }
                }
            }

            // 创建新骨骼
            Bone newBone = new Bone(boneName);

            // 如果有选中的骨骼，设为父节点
            if (selectedBone != null) {
                selectedBone.addChild(newBone);
            }

            // 添加到骨骼系统
            if (skeleton != null) {
                skeleton.addBone(newBone);
                // 如果没有根骨骼且新骨骼没有父节点，设为根
                if (skeleton.getRootBone() == null && newBone.getParent() == null) {
                    skeleton.setRootBone(newBone);
                }
                allBones = skeleton.getAllBones().toArray(new Bone[0]);
            }

            // 创建对应的渲染器
            if (puppetTestScene != null && puppetTestScene.getPuppetRenderer() != null) {
                puppetTestScene.getPuppetRenderer().addPartRenderer(newBone);
            }

            // 更新UI
            refreshUI();

            // 选中新骨骼
            for (int i = 0; i < allBones.length; i++) {
                if (allBones[i].getName().equals(boneName)) {
                    selectBone(i);
                    break;
                }
            }

        }
    }

    /**
     * 删除骨骼
     */
    private void doRemoveBone() {
        if (selectedBone == null) {
            JOptionPane.showMessageDialog(
                null,
                "请先选择要删除的骨骼",
                "提示",
                JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        // 不能删除根骨骼
        if (selectedBone.getParent() == null && skeleton.getAllBones().size() == 1) {
            JOptionPane.showMessageDialog(
                null,
                "不能删除最后一个骨骼!",
                "错误",
                JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        int result = JOptionPane.showConfirmDialog(
            null,
            "确定删除骨骼 \"" + selectedBone.getName() + "\"?\n(子骨骼将被重新挂载到父节点)",
            "删除确认",
            JOptionPane.YES_NO_OPTION
        );

        if (result == JOptionPane.YES_OPTION) {
            String removedName = selectedBone.getName();

            // 从渲染器移除
            if (puppetTestScene != null && puppetTestScene.getPuppetRenderer() != null) {
                puppetTestScene.getPuppetRenderer().removePartRenderer(removedName);
            }

            // 从骨骼系统移除
            if (skeleton != null) {
                skeleton.removeBone(removedName);
                allBones = skeleton.getAllBones().toArray(new Bone[0]);
            }

            // 更新UI
            refreshUI();

            // 选择第一个骨骼
            if (allBones.length > 0) {
                selectBone(0);
            }

        }
    }

    /**
     * 设置父节点
     */
    private void doSetParent() {
        if (selectedBone == null || allBones == null || allBones.length < 2) {
            JOptionPane.showMessageDialog(
                null,
                "请先选择骨骼，并确保有多个骨骼可选",
                "提示",
                JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        // 构建可选父节点列表（排除自己和子节点）
        java.util.List<String> validParents = new java.util.ArrayList<>();
        for (Bone bone : allBones) {
            if (bone != selectedBone && !isDescendant(bone, selectedBone)) {
                validParents.add(bone.getName());
            }
        }

        if (validParents.isEmpty()) {
            JOptionPane.showMessageDialog(
                null,
                "没有可用的父节点",
                "提示",
                JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        String[] options = validParents.toArray(new String[0]);
        String selected = (String) JOptionPane.showInputDialog(
            null,
            "选择父节点:",
            "设置父节点",
            JOptionPane.PLAIN_MESSAGE,
            null,
            options,
            options[0]
        );

        if (selected != null) {
            // 找到选中的骨骼
            for (Bone bone : allBones) {
                if (bone.getName().equals(selected)) {
                    // 先从旧父节点移除
                    Bone oldParent = selectedBone.getParent();
                    if (oldParent != null) {
                        oldParent.removeChild(selectedBone);
                    }
                    // 添加到新父节点
                    bone.addChild(selectedBone);
                    refreshUI();
                    break;
                }
            }
        }
    }

    /**
     * 清除父节点
     */
    private void doClearParent() {
        if (selectedBone == null) {
            JOptionPane.showMessageDialog(
                null,
                "请先选择骨骼",
                "提示",
                JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        if (selectedBone.getParent() == null) {
            JOptionPane.showMessageDialog(
                null,
                "该骨骼没有父节点",
                "提示",
                JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        // 从父节点移除
        Bone parent = selectedBone.getParent();
        if (parent != null) {
            parent.removeChild(selectedBone);
        }
        refreshUI();
    }

    /**
     * 检查 bone 是否是 potentialAncestor 的后代
     */
    private boolean isDescendant(Bone bone, Bone potentialAncestor) {
        Bone parent = bone.getParent();
        while (parent != null) {
            if (parent == potentialAncestor) {
                return true;
            }
            parent = parent.getParent();
        }
        return false;
    }

    // ==================== UI 刷新 ====================

    /**
     * 刷新 UI
     */
    private void refreshUI() {
        // 更新骨骼列表
        if (lemurUI != null && skeleton != null) {
            lemurUI.updateBoneList(skeleton);
        }

        // 更新滑条值
        if (selectedPartRenderer != null && selectedBone != null) {
            lemurUI.updateSliderValues(selectedPartRenderer, selectedBone);
        }
    }

    // ==================== Getters ====================

    public Skeleton getSkeleton() {
        return skeleton;
    }

    public Bone getSelectedBone() {
        return selectedBone;
    }

    public PuppetPartRenderer getSelectedPartRenderer() {
        return selectedPartRenderer;
    }

    public PuppetTestScene getPuppetTestScene() {
        return puppetTestScene;
    }
}
