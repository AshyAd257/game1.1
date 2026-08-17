package com.Hecate.puppet.editor;

import com.jme3.app.SimpleApplication;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;
import com.Hecate.puppet.editor.core.EditorBone;

/**
 * 多方向纹理管理面板
 * 用于管理骨骼的4个方向纹理（Front/Back/Left/Right）
 */
public class DirectionTexturePanel {

    private final SimpleApplication app;
    private final BitmapFont font;
    private final Node rootNode;

    private final int x, y, width, height;

    // 当前选中的骨骼
    private EditorBone currentBone;

    // UI元素
    private BitmapText titleText;
    private Button loadFrontTextureButton;
    private Button loadBackTextureButton;
    private Button loadLeftTextureButton;
    private Button loadRightTextureButton;
    private Button clearFrontTextureButton;
    private Button clearBackTextureButton;
    private Button clearLeftTextureButton;
    private Button clearRightTextureButton;
    private BitmapText frontTextureStatus;
    private BitmapText backTextureStatus;
    private BitmapText leftTextureStatus;
    private BitmapText rightTextureStatus;

    // 回调接口
    public interface DirectionTextureCallbacks {
        void onLoadDirectionTexture(String direction);
        void onClearDirectionTexture(String direction);
    }
    private DirectionTextureCallbacks callbacks;

    public DirectionTexturePanel(SimpleApplication app, BitmapFont font, int x, int y, int width, int height) {
        this.app = app;
        this.font = font;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        this.rootNode = new Node("DirectionTexturePanel");
        initializePanel();
    }

    /**
     * 初始化面板
     */
    private void initializePanel() {
        // 创建半透明背景
        Quad bgQuad = new Quad(width, height);
        Geometry background = new Geometry("DirectionTextureBackground", bgQuad);
        Material bgMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        bgMat.setColor("Color", new ColorRGBA(0.15f, 0.15f, 0.15f, 0.85f));
        bgMat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        background.setMaterial(bgMat);
        background.setLocalTranslation(x, y, 1);
        rootNode.attachChild(background);

        int currentY = y + height - 30;

        // 标题
        titleText = new BitmapText(font);
        titleText.setText("=== Directional Textures ===");
        titleText.setSize(font.getCharSet().getRenderedSize() * 1.8f);
        titleText.setColor(ColorRGBA.Yellow);
        titleText.setLocalTranslation(x + 15, currentY, 2);
        rootNode.attachChild(titleText);
        currentY -= 40;

        int dirButtonWidth = 120;
        int dirButtonHeight = 30;
        int buttonSpacing = 10;
        int statusTextSize = (int)(font.getCharSet().getRenderedSize() * 1.0f);

        // Front 方向
        loadFrontTextureButton = new Button(app, font, "Load Front",
            x + 15, currentY - dirButtonHeight, dirButtonWidth, dirButtonHeight);
        loadFrontTextureButton.setClickListener(new Button.ButtonClickListener() {
            @Override
            public void onClick() {
                if (callbacks != null) {
                    callbacks.onLoadDirectionTexture("front");
                }
            }
        });
        rootNode.attachChild(loadFrontTextureButton.getRootNode());

        clearFrontTextureButton = new Button(app, font, "Clear",
            x + 15 + dirButtonWidth + buttonSpacing, currentY - dirButtonHeight, dirButtonWidth, dirButtonHeight);
        clearFrontTextureButton.setClickListener(new Button.ButtonClickListener() {
            @Override
            public void onClick() {
                if (callbacks != null) {
                    callbacks.onClearDirectionTexture("front");
                }
            }
        });
        rootNode.attachChild(clearFrontTextureButton.getRootNode());

        frontTextureStatus = new BitmapText(font);
        frontTextureStatus.setText("Front: None");
        frontTextureStatus.setSize(statusTextSize);
        frontTextureStatus.setColor(ColorRGBA.Gray);
        frontTextureStatus.setLocalTranslation(x + 15, currentY - dirButtonHeight - 20, 2);
        rootNode.attachChild(frontTextureStatus);
        currentY -= 65;

        // Back 方向
        loadBackTextureButton = new Button(app, font, "Load Back",
            x + 15, currentY - dirButtonHeight, dirButtonWidth, dirButtonHeight);
        loadBackTextureButton.setClickListener(new Button.ButtonClickListener() {
            @Override
            public void onClick() {
                if (callbacks != null) {
                    callbacks.onLoadDirectionTexture("back");
                }
            }
        });
        rootNode.attachChild(loadBackTextureButton.getRootNode());

        clearBackTextureButton = new Button(app, font, "Clear",
            x + 15 + dirButtonWidth + buttonSpacing, currentY - dirButtonHeight, dirButtonWidth, dirButtonHeight);
        clearBackTextureButton.setClickListener(new Button.ButtonClickListener() {
            @Override
            public void onClick() {
                if (callbacks != null) {
                    callbacks.onClearDirectionTexture("back");
                }
            }
        });
        rootNode.attachChild(clearBackTextureButton.getRootNode());

        backTextureStatus = new BitmapText(font);
        backTextureStatus.setText("Back: None");
        backTextureStatus.setSize(statusTextSize);
        backTextureStatus.setColor(ColorRGBA.Gray);
        backTextureStatus.setLocalTranslation(x + 15, currentY - dirButtonHeight - 20, 2);
        rootNode.attachChild(backTextureStatus);
        currentY -= 65;

        // Left 方向
        loadLeftTextureButton = new Button(app, font, "Load Left",
            x + 15, currentY - dirButtonHeight, dirButtonWidth, dirButtonHeight);
        loadLeftTextureButton.setClickListener(new Button.ButtonClickListener() {
            @Override
            public void onClick() {
                if (callbacks != null) {
                    callbacks.onLoadDirectionTexture("left");
                }
            }
        });
        rootNode.attachChild(loadLeftTextureButton.getRootNode());

        clearLeftTextureButton = new Button(app, font, "Clear",
            x + 15 + dirButtonWidth + buttonSpacing, currentY - dirButtonHeight, dirButtonWidth, dirButtonHeight);
        clearLeftTextureButton.setClickListener(new Button.ButtonClickListener() {
            @Override
            public void onClick() {
                if (callbacks != null) {
                    callbacks.onClearDirectionTexture("left");
                }
            }
        });
        rootNode.attachChild(clearLeftTextureButton.getRootNode());

        leftTextureStatus = new BitmapText(font);
        leftTextureStatus.setText("Left: None");
        leftTextureStatus.setSize(statusTextSize);
        leftTextureStatus.setColor(ColorRGBA.Gray);
        leftTextureStatus.setLocalTranslation(x + 15, currentY - dirButtonHeight - 20, 2);
        rootNode.attachChild(leftTextureStatus);
        currentY -= 65;

        // Right 方向
        loadRightTextureButton = new Button(app, font, "Load Right",
            x + 15, currentY - dirButtonHeight, dirButtonWidth, dirButtonHeight);
        loadRightTextureButton.setClickListener(new Button.ButtonClickListener() {
            @Override
            public void onClick() {
                if (callbacks != null) {
                    callbacks.onLoadDirectionTexture("right");
                }
            }
        });
        rootNode.attachChild(loadRightTextureButton.getRootNode());

        clearRightTextureButton = new Button(app, font, "Clear",
            x + 15 + dirButtonWidth + buttonSpacing, currentY - dirButtonHeight, dirButtonWidth, dirButtonHeight);
        clearRightTextureButton.setClickListener(new Button.ButtonClickListener() {
            @Override
            public void onClick() {
                if (callbacks != null) {
                    callbacks.onClearDirectionTexture("right");
                }
            }
        });
        rootNode.attachChild(clearRightTextureButton.getRootNode());

        rightTextureStatus = new BitmapText(font);
        rightTextureStatus.setText("Right: None");
        rightTextureStatus.setSize(statusTextSize);
        rightTextureStatus.setColor(ColorRGBA.Gray);
        rightTextureStatus.setLocalTranslation(x + 15, currentY - dirButtonHeight - 20, 2);
        rootNode.attachChild(rightTextureStatus);
    }

    /**
     * 设置当前骨骼
     */
    public void setBone(EditorBone bone) {
        this.currentBone = bone;
        updateDisplay();
    }

    /**
     * 更新显示
     */
    public void updateDisplay() {
        if (currentBone == null) {
            frontTextureStatus.setText("Front: None");
            frontTextureStatus.setColor(ColorRGBA.Gray);
            backTextureStatus.setText("Back: None");
            backTextureStatus.setColor(ColorRGBA.Gray);
            leftTextureStatus.setText("Left: None");
            leftTextureStatus.setColor(ColorRGBA.Gray);
            rightTextureStatus.setText("Right: None");
            rightTextureStatus.setColor(ColorRGBA.Gray);
            return;
        }

        // Front
        String frontTexture = currentBone.getDirectionTexture("front");
        if (frontTexture != null && !frontTexture.isEmpty()) {
            String fileName = getFileName(frontTexture);
            frontTextureStatus.setText("Front: " + fileName);
            frontTextureStatus.setColor(ColorRGBA.Green);
        } else {
            frontTextureStatus.setText("Front: None");
            frontTextureStatus.setColor(ColorRGBA.Gray);
        }

        // Back
        String backTexture = currentBone.getDirectionTexture("back");
        if (backTexture != null && !backTexture.isEmpty()) {
            String fileName = getFileName(backTexture);
            backTextureStatus.setText("Back: " + fileName);
            backTextureStatus.setColor(ColorRGBA.Green);
        } else {
            backTextureStatus.setText("Back: None");
            backTextureStatus.setColor(ColorRGBA.Gray);
        }

        // Left
        String leftTexture = currentBone.getDirectionTexture("left");
        if (leftTexture != null && !leftTexture.isEmpty()) {
            String fileName = getFileName(leftTexture);
            leftTextureStatus.setText("Left: " + fileName);
            leftTextureStatus.setColor(ColorRGBA.Green);
        } else {
            leftTextureStatus.setText("Left: None");
            leftTextureStatus.setColor(ColorRGBA.Gray);
        }

        // Right
        String rightTexture = currentBone.getDirectionTexture("right");
        if (rightTexture != null && !rightTexture.isEmpty()) {
            String fileName = getFileName(rightTexture);
            rightTextureStatus.setText("Right: " + fileName);
            rightTextureStatus.setColor(ColorRGBA.Green);
        } else {
            rightTextureStatus.setText("Right: None");
            rightTextureStatus.setColor(ColorRGBA.Gray);
        }
    }

    /**
     * 从完整路径中提取文件名
     */
    private String getFileName(String path) {
        if (path == null) return "";
        int lastSlash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        if (lastSlash >= 0 && lastSlash < path.length() - 1) {
            String fileName = path.substring(lastSlash + 1);
            // 如果文件名太长，截断显示
            if (fileName.length() > 25) {
                return fileName.substring(0, 22) + "...";
            }
            return fileName;
        }
        return path;
    }

    /**
     * 更新所有UI组件
     */
    public void update(float tpf) {
        loadFrontTextureButton.update(tpf);
        loadBackTextureButton.update(tpf);
        loadLeftTextureButton.update(tpf);
        loadRightTextureButton.update(tpf);
        clearFrontTextureButton.update(tpf);
        clearBackTextureButton.update(tpf);
        clearLeftTextureButton.update(tpf);
        clearRightTextureButton.update(tpf);
    }

    /**
     * 处理鼠标点击
     */
    public boolean handleMouseClick(int mouseX, int mouseY) {
        if (loadFrontTextureButton.handleMouseClick(mouseX, mouseY)) {
            return true;
        }
        if (loadBackTextureButton.handleMouseClick(mouseX, mouseY)) {
            return true;
        }
        if (loadLeftTextureButton.handleMouseClick(mouseX, mouseY)) {
            return true;
        }
        if (loadRightTextureButton.handleMouseClick(mouseX, mouseY)) {
            return true;
        }
        if (clearFrontTextureButton.handleMouseClick(mouseX, mouseY)) {
            return true;
        }
        if (clearBackTextureButton.handleMouseClick(mouseX, mouseY)) {
            return true;
        }
        if (clearLeftTextureButton.handleMouseClick(mouseX, mouseY)) {
            return true;
        }
        if (clearRightTextureButton.handleMouseClick(mouseX, mouseY)) {
            return true;
        }
        return false;
    }

    // ========== Getters and Setters ==========

    public Node getRootNode() {
        return rootNode;
    }

    public void setCallbacks(DirectionTextureCallbacks callbacks) {
        this.callbacks = callbacks;
    }
}
