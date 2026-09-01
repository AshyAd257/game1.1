package com.Hecate.ui;

import com.jme3.app.SimpleApplication;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;
import com.Hecate.ui.common.TTFontLoader;
import com.Hecate.ui.common.TextField;

/**
 * 背包UI系统
 * 按G键切换显示/隐藏
 */
public class InventoryUI implements ActionListener {
    private final SimpleApplication app;
    private Geometry inventoryBackground;

    // 制造界面按钮
    private Geometry forgeButton;
    private Geometry forgeButtonPressed;
    private Geometry craftSearchBox;
    private Geometry searchButton;
    private Geometry searchButtonPressed;
    private Geometry buttonNormal;
    private Geometry buttonPressed;

    // 制造界面底部面板
    private Geometry craftPreviewPanel;      // craft_preview_panel
    private Geometry craftSliderBg;          // slider_background_craft
    private Geometry craftSliderPreview;     // slider_craft_preview
    private Geometry craftBottomBar;         // 底部制造栏

    // 生命/个人界面元素
    private Geometry lifeChip;               // 个人界面芯片

    // 搜索框文本输入
    private TTFontLoader fontLoader;         // TTF字体加载器
    private TextField searchTextField;       // 搜索框TextField组件
    private static final float SEARCH_BOX_X = 82.5f;      // 搜索框左边界
    private static final float SEARCH_BOX_Y = 32.0f;      // 搜索框上边界（36.75-9/2）
    private static final float SEARCH_BOX_WIDTH = 51.75f; // 搜索框宽度（134.25-82.5）
    private static final float SEARCH_BOX_HEIGHT = 9f;    // 搜索框高度

    // 背包格子面板（Lemur版本，通用格子容器UI的接入点）。背包数据(PlayerStateManager.getBackpack())
    // 在PlayerController构造完成之后才可用（见PlayerController.setPlayerStateManager），
    // 所以这里不能在构造函数里直接创建，只能提供一个setter延后接入。
    private com.Hecate.ui.inventory.InventoryGridPanel backpackGridPanel;
    // 4列 = backpack.png的4x4格布局；单格显示尺寸12px*4(与背包主界面贴图同一套缩放倍数scale=4.0，
    // 见createInventoryUI)=48px，让格子交互层与backpack.png背景贴图的格子线精确重合
    private static final int BACKPACK_COLUMNS = 4;
    private static final float BACKPACK_SLOT_SIZE = 48f;
    private static final String BACKPACK_BACKGROUND_TEXTURE = "textures/ui/backpack.png";
    private static final String BACKPACK_HIGHLIGHT_TEXTURE = "textures/ui/blockhighlight.png";

    // 面板管理器（鼠标悬停背包物品图标时用来显示/隐藏说明面板）。可能在setBackpack()
    // 调用之前或之后设置，setBackpack()里若backpackGridPanel已存在会重新注入。
    private PanelManager panelManager;

    private boolean isVisible = false;  // 默认隐藏

    // 当前显示的界面类型
    private enum InterfaceType {
        INVENTORY,  // 背包界面
        LIFE,       // 生命/个人界面
        CRAFT       // 制造界面
    }
    private InterfaceType currentInterface = InterfaceType.INVENTORY;

    // 界面位置和尺寸
    private float uiX, uiY;
    private float uiWidth, uiHeight;
    private float scale;
    private int textureWidth, textureHeight;

    // 侧边栏按钮区域（相对于贴图的像素坐标，从左上角开始）
    // 30x30正方形区域，以实际点击位置为中心
    private static final float SIDEBAR_BUTTON_WIDTH = 30f;
    private static final float SIDEBAR_BUTTON_HEIGHT = 30f;

    // 按钮中心点坐标（根据实际点击测试，从上到下）
    // 背包按钮（最上）: 点击(206.25, 47.0) → 区域[191.25, 32.0, 30, 30]
    private static final float INVENTORY_BUTTON_X = 191.25f;
    private static final float INVENTORY_BUTTON_Y = 32.0f;

    // 生命按钮（第2个）: 点击(202.5, 79.0) → 区域[187.5, 64.0, 30, 30]
    private static final float LIFE_BUTTON_X = 187.5f;
    private static final float LIFE_BUTTON_Y = 64.0f;

    // 制造按钮（第3个）: 点击(202.75, 114.5) → 区域[187.75, 99.5, 30, 30]
    private static final float CRAFT_BUTTON_X = 187.75f;
    private static final float CRAFT_BUTTON_Y = 99.5f;

    // 学习按钮（第4个）: 点击(202.25, 146.25) → 区域[187.25, 131.25, 30, 30]
    private static final float LEARN_BUTTON_X = 187.25f;
    private static final float LEARN_BUTTON_Y = 131.25f;

    public InventoryUI(SimpleApplication app) {
        this.app = app;
        createInventoryUI();
        setupMouseInput();
    }

    private void createInventoryUI() {
        try {
            // 获取屏幕尺寸
            int screenWidth = app.getCamera().getWidth();
            int screenHeight = app.getCamera().getHeight();

            // 加载纹理
            Texture backgroundTexture = loadCurrentTexture();
            backgroundTexture.setWrap(Texture.WrapMode.EdgeClamp);

            // 像素画专用：使用最近邻过滤，保持像素清晰锐利
            backgroundTexture.setMagFilter(Texture.MagFilter.Nearest);
            backgroundTexture.setMinFilter(Texture.MinFilter.NearestNoMipMaps);

            // 获取纹理的原始尺寸
            textureWidth = backgroundTexture.getImage().getWidth();
            textureHeight = backgroundTexture.getImage().getHeight();

            // 缩放因子（可以调整这个值来改变整体大小）
            scale = 4.0f;

            // 根据纹理原始尺寸计算显示尺寸，保持原始比例
            uiWidth = textureWidth * scale;
            uiHeight = textureHeight * scale;

            // 创建四边形
            Quad quad = new Quad(uiWidth, uiHeight);
            inventoryBackground = new Geometry("InventoryBackground", quad);

            // 创建材质并加载背景图片
            Material material = new Material(app.getAssetManager(),
                    "Common/MatDefs/Misc/Unshaded.j3md");

            material.setTexture("ColorMap", backgroundTexture);

            // 设置透明度
            material.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
            material.setTransparent(true);
            material.setColor("Color", ColorRGBA.White);

            inventoryBackground.setMaterial(material);

            // 放置在屏幕中央
            uiX = (screenWidth - uiWidth) / 2;
            uiY = (screenHeight - uiHeight) / 2;

            inventoryBackground.setLocalTranslation(uiX, uiY, 1001);

            // 默认隐藏
            inventoryBackground.setCullHint(com.jme3.scene.Spatial.CullHint.Always);

            // 添加到GUI节点
            app.getGuiNode().attachChild(inventoryBackground);

            // 创建其他UI元素
            createAdditionalUIElements();

            // 创建搜索框文字显示
            createSearchTextDisplay();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 创建搜索框文字显示
     */
    private void createSearchTextDisplay() {
        try {
            // 加载TTF字体（字体大小需要匹配UI缩放）
            float fontSize = 30f;  // 调整为30px
            fontLoader = TTFontLoader.loadFontFromResource(
                app.getAssetManager(),
                "Interface/Fonts/ZLabsBitmap_12px_CN（简体中文）.ttf",
                fontSize
            );

            if (fontLoader == null) {
                System.err.println("无法加载TTF字体");
                return;
            }

            // 计算TextField位置和尺寸
            int textFieldX = (int)(uiX + (SEARCH_BOX_X * scale));
            int textFieldY = (int)(uiY + (uiHeight - (SEARCH_BOX_Y * scale)) - (SEARCH_BOX_HEIGHT * scale)) - 8;  // 下移8像素
            int textFieldWidth = (int)(SEARCH_BOX_WIDTH * scale);
            int textFieldHeight = (int)(SEARCH_BOX_HEIGHT * scale);

            // 创建TextField
            searchTextField = new TextField(
                app,
                fontLoader,
                "",  // 初始文本为空
                textFieldX,
                textFieldY,
                textFieldWidth,
                textFieldHeight
            );

            // 设置TextField样式
            searchTextField.setTextColor(ColorRGBA.White);  // 白色文字
            searchTextField.setBackgroundColor(new ColorRGBA(0, 0, 0, 0)); // 透明背景

            // 设置最大宽度（搜索按钮左边10像素）
            // 搜索按钮在 (146.5, 36.5)，宽度8px，所以左边界是 146.5 - 4 = 142.5
            // 搜索框左边界是 82.5，所以最大宽度是 (142.5 - 10 - 82.5) * scale = 50 * scale = 200px
            int maxTextWidth = (int)(50f * scale);
            searchTextField.setMaxWidth(maxTextWidth);

            // 添加到GUI
            app.getGuiNode().attachChild(searchTextField.getRootNode());

            // 默认隐藏
            searchTextField.getRootNode().setCullHint(com.jme3.scene.Spatial.CullHint.Always);

        } catch (Exception e) {
            System.err.println("无法创建搜索框TextField: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 创建额外的UI元素（按钮、搜索框等）
     */
    private void createAdditionalUIElements() {
        // 使用相同的缩放系数
        float uiScale = scale;

        // 1. 锻造按钮 (forge_button_normal)
        forgeButton = createUIElement("textures/ui/forge_button_normal.png", uiScale);
        if (forgeButton != null) {
            forgeButton.setLocalTranslation(uiX, uiY, 1002);
            app.getGuiNode().attachChild(forgeButton);
        }

        // 1b. 锻造按钮按下状态
        forgeButtonPressed = createUIElement("textures/ui/forge_button_pressed.png", uiScale);
        if (forgeButtonPressed != null) {
            forgeButtonPressed.setLocalTranslation(uiX, uiY, 1003);
            forgeButtonPressed.setCullHint(com.jme3.scene.Spatial.CullHint.Always);
            app.getGuiNode().attachChild(forgeButtonPressed);
        }

        // 2. 制造搜索框 (craft_search_box)
        craftSearchBox = createUIElement("textures/ui/craft_search_box.png", uiScale);
        if (craftSearchBox != null) {
            craftSearchBox.setLocalTranslation(uiX, uiY, 1002);
            app.getGuiNode().attachChild(craftSearchBox);
        }

        // 3. 搜索按钮 (search_button_normal)
        searchButton = createUIElement("textures/ui/search_button_normal.png", uiScale);
        if (searchButton != null) {
            searchButton.setLocalTranslation(uiX, uiY, 1002);
            app.getGuiNode().attachChild(searchButton);
        }

        // 3b. 搜索按钮按下状态
        searchButtonPressed = createUIElement("textures/ui/search_button_pressed.png", uiScale);
        if (searchButtonPressed != null) {
            searchButtonPressed.setLocalTranslation(uiX, uiY, 1003);
            searchButtonPressed.setCullHint(com.jme3.scene.Spatial.CullHint.Always);
            app.getGuiNode().attachChild(searchButtonPressed);
        }

        // 4. 普通按钮 (button_normal)
        buttonNormal = createUIElement("textures/ui/button_normal.png", uiScale);
        if (buttonNormal != null) {
            buttonNormal.setLocalTranslation(uiX, uiY, 1002);
            app.getGuiNode().attachChild(buttonNormal);
        }

        // 4b. 普通按钮按下状态
        buttonPressed = createUIElement("textures/ui/button_pressed.png", uiScale);
        if (buttonPressed != null) {
            buttonPressed.setLocalTranslation(uiX, uiY, 1003);
            buttonPressed.setCullHint(com.jme3.scene.Spatial.CullHint.Always);
            app.getGuiNode().attachChild(buttonPressed);
        }

        // 5. 制造预览面板 (craft_preview_panel)
        craftPreviewPanel = createUIElement("textures/ui/craft_preview_panel.png", uiScale);
        if (craftPreviewPanel != null) {
            craftPreviewPanel.setLocalTranslation(uiX, uiY, 1002);
            app.getGuiNode().attachChild(craftPreviewPanel);
        }

        // 6. 制造滑块背景 (slider_background_craft)
        craftSliderBg = createUIElement("textures/ui/slider_background_craft.png", uiScale);
        if (craftSliderBg != null) {
            craftSliderBg.setLocalTranslation(uiX, uiY, 1002);
            app.getGuiNode().attachChild(craftSliderBg);
        }

        // 7. 制造滑块预览 (slider_craft_preview)
        craftSliderPreview = createUIElement("textures/ui/slider_craft_preview.png", uiScale);
        if (craftSliderPreview != null) {
            craftSliderPreview.setLocalTranslation(uiX, uiY, 1002);
            app.getGuiNode().attachChild(craftSliderPreview);
        }

        // 8. 底部制造栏
        craftBottomBar = createUIElement("textures/ui/craft_bottom_bar.png", uiScale);
        if (craftBottomBar != null) {
            craftBottomBar.setLocalTranslation(uiX, uiY, 1002);
            app.getGuiNode().attachChild(craftBottomBar);
        }

        // 9. 生命界面芯片
        lifeChip = createUIElement("textures/ui/life_chip.png", uiScale);
        if (lifeChip != null) {
            lifeChip.setLocalTranslation(uiX, uiY, 1002);
            app.getGuiNode().attachChild(lifeChip);
        }
    }

    /**
     * 创建UI元素的通用方法
     */
    private Geometry createUIElement(String texturePath, float scale) {
        try {
            Texture texture = app.getAssetManager().loadTexture(texturePath);
            texture.setWrap(Texture.WrapMode.EdgeClamp);
            texture.setMagFilter(Texture.MagFilter.Nearest);
            texture.setMinFilter(Texture.MinFilter.NearestNoMipMaps);

            int width = texture.getImage().getWidth();
            int height = texture.getImage().getHeight();

            Quad quad = new Quad(width * scale, height * scale);
            Geometry geometry = new Geometry("UIElement_" + texturePath, quad);

            Material material = new Material(app.getAssetManager(),
                    "Common/MatDefs/Misc/Unshaded.j3md");
            material.setTexture("ColorMap", texture);
            material.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
            material.setTransparent(true);
            material.setColor("Color", ColorRGBA.White);

            geometry.setMaterial(material);
            geometry.setCullHint(com.jme3.scene.Spatial.CullHint.Always);

            return geometry;
        } catch (Exception e) {
            System.err.println("无法加载UI元素: " + texturePath);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 加载当前界面的纹理
     */
    private Texture loadCurrentTexture() {
        String texturePath;
        switch (currentInterface) {
            case CRAFT:
                texturePath = "textures/ui/craft_interface.png";
                break;
            case LIFE:
                texturePath = "textures/ui/life_interface.png";
                break;
            case INVENTORY:
            default:
                texturePath = "textures/ui/inventory_interface.png";
                break;
        }
        return app.getAssetManager().loadTexture(texturePath);
    }

    /**
     * 设置鼠标输入
     */
    private void setupMouseInput() {
        app.getInputManager().addMapping("InventoryClick",
                new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        app.getInputManager().addListener(this, "InventoryClick");

        // 添加键盘输入监听
        app.getInputManager().addRawInputListener(new com.jme3.input.RawInputListener() {
            @Override
            public void beginInput() {}

            @Override
            public void endInput() {}

            @Override
            public void onJoyAxisEvent(com.jme3.input.event.JoyAxisEvent evt) {}

            @Override
            public void onJoyButtonEvent(com.jme3.input.event.JoyButtonEvent evt) {}

            @Override
            public void onMouseMotionEvent(com.jme3.input.event.MouseMotionEvent evt) {}

            @Override
            public void onMouseButtonEvent(com.jme3.input.event.MouseButtonEvent evt) {}

            @Override
            public void onKeyEvent(com.jme3.input.event.KeyInputEvent evt) {
                // TextField自动处理键盘输入
            }

            @Override
            public void onTouchEvent(com.jme3.input.event.TouchEvent evt) {}
        });
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (name.equals("InventoryClick") && isPressed && isVisible) {
            handleMouseClick();
        }
    }

    /**
     * 处理鼠标点击
     */
    private void handleMouseClick() {
        // 获取鼠标位置
        Vector2f cursorPos = app.getInputManager().getCursorPosition();
        float mouseX = cursorPos.x;
        float mouseY = cursorPos.y;

        // 检查是否点击在UI区域内
        if (mouseX < uiX || mouseX > uiX + uiWidth ||
            mouseY < uiY || mouseY > uiY + uiHeight) {
            return;  // 点击在UI外部
        }

        // 转换为相对于UI的坐标
        float relativeX = mouseX - uiX;
        float relativeY = mouseY - uiY;

        // 转换为纹理坐标（考虑缩放）
        float textureX = relativeX / scale;
        float textureY = (uiHeight - relativeY) / scale;  // Y轴翻转

        // 检查是否点击在制造界面的按钮上
        if (currentInterface == InterfaceType.CRAFT) {
            checkCraftButtonClick(textureX, textureY);
        }

        // 检查侧边栏按钮（使用30x30正方形区域）

        // 按从上到下的顺序检查：背包 → 生命 → 制造 → 学习

        // 检查背包按钮（最上）
        if (isInButtonArea(textureX, textureY, INVENTORY_BUTTON_X, INVENTORY_BUTTON_Y)) {
            switchToInventory();
            return;
        }

        // 检查生命按钮（第2个）
        if (isInButtonArea(textureX, textureY, LIFE_BUTTON_X, LIFE_BUTTON_Y)) {
            switchToLife();
            return;
        }

        // 检查制造按钮（第3个）
        if (isInButtonArea(textureX, textureY, CRAFT_BUTTON_X, CRAFT_BUTTON_Y)) {
            switchToCraft();
            return;
        }

        // 检查学习按钮（第4个）
        if (isInButtonArea(textureX, textureY, LEARN_BUTTON_X, LEARN_BUTTON_Y)) {
            return;
        }
    }

    /**
     * 检查点击是否在按钮区域内（30x30正方形）
     */
    private boolean isInButtonArea(float clickX, float clickY, float buttonX, float buttonY) {
        return clickX >= buttonX && clickX <= buttonX + SIDEBAR_BUTTON_WIDTH &&
               clickY >= buttonY && clickY <= buttonY + SIDEBAR_BUTTON_HEIGHT;
    }

    // 按钮点击区域定义（纹理坐标）
    // 根据实际点击测试数据配置
    private static class ButtonArea {
        float x, y, width, height;
        ButtonArea(float x, float y, float width, float height) {
            this.x = x; this.y = y; this.width = width; this.height = height;
        }
        boolean contains(float px, float py) {
            return px >= x && px <= x + width && py >= y && py <= y + height;
        }
    }

    // 制造界面按钮区域（8x8正方形）
    // forge_button: 点击坐标 (135.0, 152.5) - 以点击点为中心的8x8区域
    private ButtonArea forgeButtonArea = new ButtonArea(131.0f, 148.5f, 8f, 8f);

    // search_button: 点击坐标 (146.5, 36.5) - 以点击点为中心的8x8区域
    private ButtonArea searchButtonArea = new ButtonArea(142.5f, 32.5f, 8f, 8f);

    // button_normal: 点击坐标 (91.75, 153.25) - 以点击点为中心的8x8区域
    private ButtonArea buttonNormalArea = new ButtonArea(87.75f, 149.25f, 8f, 8f);

    /**
     * 检查制造界面按钮点击
     */
    private void checkCraftButtonClick(float textureX, float textureY) {

        // 检查是否点击搜索框
        if (textureX >= SEARCH_BOX_X && textureX <= SEARCH_BOX_X + SEARCH_BOX_WIDTH &&
            textureY >= SEARCH_BOX_Y && textureY <= SEARCH_BOX_Y + SEARCH_BOX_HEIGHT) {
            activateSearchBox();
            return;
        }

        // 检测forge_button
        if (forgeButtonArea.contains(textureX, textureY)) {
            showButtonPressed(forgeButton, forgeButtonPressed);
            return;
        }

        // 检测search_button
        if (searchButtonArea.contains(textureX, textureY)) {
            showButtonPressed(searchButton, searchButtonPressed);
            toggleSearchBox();  // 切换搜索框状态
            return;
        }

        // 检测button_normal
        if (buttonNormalArea.contains(textureX, textureY)) {
            showButtonPressed(buttonNormal, buttonPressed);
            return;
        }

        // 点击其他地方取消搜索框激活
        deactivateSearchBox();
    }

    /**
     * 切换搜索框状态
     * 第一次点击：激活输入
     * 第二次点击：终止输入，保留文字
     * 第三次点击：清空文字，重新开始输入
     */
    private void toggleSearchBox() {

        if (searchTextField == null) {
            System.err.println("错误：searchTextField 为 null！");
            return;
        }

        boolean isFocused = searchTextField.isFocused();
        String currentText = searchTextField.getText();

        if (!isFocused && currentText.isEmpty()) {
            // 第一次点击：激活输入

            activateSearchBox();
        } else if (isFocused) {
            // 第二次点击：终止输入，保留文字

            deactivateSearchBox();
        } else {
            // 第三次点击：清空文字，重新开始输入

            searchTextField.setText("");
            activateSearchBox();
        }
    }

    /**
     * 激活搜索框
     */
    private void activateSearchBox() {

        if (searchTextField != null) {
            searchTextField.setFocused(true);
            searchTextField.getRootNode().setCullHint(com.jme3.scene.Spatial.CullHint.Never);

        } else {

        }
    }

    /**
     * 取消激活搜索框（失去焦点但保持可见）
     */
    private void deactivateSearchBox() {
        if (searchTextField != null) {
            searchTextField.setFocused(false);
            // 不隐藏TextField，让文字保持可见

        }
    }

    /**
     * 显示按钮按下状态
     */
    private void showButtonPressed(Geometry normalButton, Geometry pressedButton) {
        if (normalButton != null && pressedButton != null) {
            normalButton.setCullHint(com.jme3.scene.Spatial.CullHint.Always);
            pressedButton.setCullHint(com.jme3.scene.Spatial.CullHint.Never);

            // 0.1秒后恢复正常状态
            new Thread(() -> {
                try {
                    Thread.sleep(100);
                    app.enqueue(() -> {
                        normalButton.setCullHint(com.jme3.scene.Spatial.CullHint.Never);
                        pressedButton.setCullHint(com.jme3.scene.Spatial.CullHint.Always);
                        return null;
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    /**
     * 切换到背包界面
     */
    private void switchToInventory() {
        if (currentInterface != InterfaceType.INVENTORY) {
            currentInterface = InterfaceType.INVENTORY;
            updateInterface();
        }
    }

    /**
     * 切换到生命界面
     */
    private void switchToLife() {
        if (currentInterface != InterfaceType.LIFE) {
            currentInterface = InterfaceType.LIFE;
            updateInterface();
        }
    }

    /**
     * 切换到制造界面
     */
    private void switchToCraft() {
        if (currentInterface != InterfaceType.CRAFT) {
            currentInterface = InterfaceType.CRAFT;
            updateInterface();
        }
    }

    /**
     * 更新界面显示
     */
    private void updateInterface() {
        if (inventoryBackground != null) {
            // 加载新纹理
            Texture newTexture = loadCurrentTexture();
            newTexture.setWrap(Texture.WrapMode.EdgeClamp);
            newTexture.setMagFilter(Texture.MagFilter.Nearest);
            newTexture.setMinFilter(Texture.MinFilter.NearestNoMipMaps);

            // 获取新纹理的尺寸
            int newTextureWidth = newTexture.getImage().getWidth();
            int newTextureHeight = newTexture.getImage().getHeight();

            // 计算新的显示尺寸
            float newUiWidth = newTextureWidth * scale;
            float newUiHeight = newTextureHeight * scale;

            // 更新Quad的尺寸
            Quad newQuad = new Quad(newUiWidth, newUiHeight);
            inventoryBackground.setMesh(newQuad);

            // 更新材质
            Material material = inventoryBackground.getMaterial();
            material.setTexture("ColorMap", newTexture);

            // 更新存储的尺寸
            textureWidth = newTextureWidth;
            textureHeight = newTextureHeight;
            uiWidth = newUiWidth;
            uiHeight = newUiHeight;

            // 重新居中
            int screenWidth = app.getCamera().getWidth();
            int screenHeight = app.getCamera().getHeight();
            uiX = (screenWidth - uiWidth) / 2;
            uiY = (screenHeight - uiHeight) / 2;
            inventoryBackground.setLocalTranslation(uiX, uiY, 1001);

            // 更新所有子UI元素的位置
            updateChildUIPositions();
        }

        // 更新按钮可见性
        updateVisibility();
    }

    /**
     * 更新所有子UI元素的位置
     */
    private void updateChildUIPositions() {
        if (forgeButton != null) forgeButton.setLocalTranslation(uiX, uiY, 1002);
        if (forgeButtonPressed != null) forgeButtonPressed.setLocalTranslation(uiX, uiY, 1003);
        if (craftSearchBox != null) craftSearchBox.setLocalTranslation(uiX, uiY, 1002);
        if (searchButton != null) searchButton.setLocalTranslation(uiX, uiY, 1002);
        if (searchButtonPressed != null) searchButtonPressed.setLocalTranslation(uiX, uiY, 1003);
        if (buttonNormal != null) buttonNormal.setLocalTranslation(uiX, uiY, 1002);
        if (buttonPressed != null) buttonPressed.setLocalTranslation(uiX, uiY, 1003);
        if (craftPreviewPanel != null) craftPreviewPanel.setLocalTranslation(uiX, uiY, 1002);
        if (craftSliderBg != null) craftSliderBg.setLocalTranslation(uiX, uiY, 1002);
        if (craftSliderPreview != null) craftSliderPreview.setLocalTranslation(uiX, uiY, 1002);
        if (craftBottomBar != null) craftBottomBar.setLocalTranslation(uiX, uiY, 1002);
        if (lifeChip != null) lifeChip.setLocalTranslation(uiX, uiY, 1002);

        // 更新搜索框位置
        if (searchTextField != null) {
            int textFieldX = (int)(uiX + (SEARCH_BOX_X * scale));
            int textFieldY = (int)(uiY + (uiHeight - (SEARCH_BOX_Y * scale)) - (SEARCH_BOX_HEIGHT * scale));
            searchTextField.setPosition(textFieldX, textFieldY);
        }
    }

    /**
     * 切换背包显示/隐藏
     */
    public void toggle() {
        isVisible = !isVisible;
        if (isVisible && backpackGridPanel != null) {
            // 背包可能在隐藏期间被/giveitem等命令修改过，显示前刷新一次保证不显示旧内容
            backpackGridPanel.refreshAll();
        }
        updateVisibility();
        updateMouseCursor();
    }

    /**
     * 显示背包
     */
    public void show() {
        isVisible = true;
        if (backpackGridPanel != null) {
            backpackGridPanel.refreshAll();
        }
        updateVisibility();
        updateMouseCursor();
    }

    /**
     * 隐藏背包
     */
    public void hide() {
        isVisible = false;
        updateVisibility();
        updateMouseCursor();
    }

    private void updateVisibility() {
        if (inventoryBackground != null) {
            inventoryBackground.setCullHint(isVisible ?
                    com.jme3.scene.Spatial.CullHint.Never :
                    com.jme3.scene.Spatial.CullHint.Always);
        }

        // 背包格子面板只在背包界面显示
        if (backpackGridPanel != null) {
            boolean showBackpackGrid = isVisible && currentInterface == InterfaceType.INVENTORY;
            backpackGridPanel.getContainer().setCullHint(showBackpackGrid ?
                    com.jme3.scene.Spatial.CullHint.Never :
                    com.jme3.scene.Spatial.CullHint.Always);
        }

        // 这些按钮和面板只在制造界面显示
        boolean showCraftElements = isVisible && currentInterface == InterfaceType.CRAFT;

        if (forgeButton != null) {
            forgeButton.setCullHint(showCraftElements ?
                    com.jme3.scene.Spatial.CullHint.Never :
                    com.jme3.scene.Spatial.CullHint.Always);
        }
        if (craftSearchBox != null) {
            craftSearchBox.setCullHint(showCraftElements ?
                    com.jme3.scene.Spatial.CullHint.Never :
                    com.jme3.scene.Spatial.CullHint.Always);
        }
        if (searchButton != null) {
            searchButton.setCullHint(showCraftElements ?
                    com.jme3.scene.Spatial.CullHint.Never :
                    com.jme3.scene.Spatial.CullHint.Always);
        }
        if (buttonNormal != null) {
            buttonNormal.setCullHint(showCraftElements ?
                    com.jme3.scene.Spatial.CullHint.Never :
                    com.jme3.scene.Spatial.CullHint.Always);
        }

        // 制造界面底部面板
        if (craftPreviewPanel != null) {
            craftPreviewPanel.setCullHint(showCraftElements ?
                    com.jme3.scene.Spatial.CullHint.Never :
                    com.jme3.scene.Spatial.CullHint.Always);
        }
        if (craftSliderBg != null) {
            craftSliderBg.setCullHint(showCraftElements ?
                    com.jme3.scene.Spatial.CullHint.Never :
                    com.jme3.scene.Spatial.CullHint.Always);
        }
        if (craftSliderPreview != null) {
            craftSliderPreview.setCullHint(showCraftElements ?
                    com.jme3.scene.Spatial.CullHint.Never :
                    com.jme3.scene.Spatial.CullHint.Always);
        }
        if (craftBottomBar != null) {
            craftBottomBar.setCullHint(showCraftElements ?
                    com.jme3.scene.Spatial.CullHint.Never :
                    com.jme3.scene.Spatial.CullHint.Always);
        }

        // 生命界面元素只在生命界面显示
        boolean showLifeElements = isVisible && currentInterface == InterfaceType.LIFE;

        if (lifeChip != null) {
            lifeChip.setCullHint(showLifeElements ?
                    com.jme3.scene.Spatial.CullHint.Never :
                    com.jme3.scene.Spatial.CullHint.Always);
        }

        // 搜索框只在制造界面显示
        if (searchTextField != null) {
            searchTextField.getRootNode().setCullHint(showCraftElements ?
                    com.jme3.scene.Spatial.CullHint.Never :
                    com.jme3.scene.Spatial.CullHint.Always);
        }

        // Pressed状态的按钮始终隐藏（只在点击时短暂显示）
        if (forgeButtonPressed != null) {
            forgeButtonPressed.setCullHint(com.jme3.scene.Spatial.CullHint.Always);
        }
        if (searchButtonPressed != null) {
            searchButtonPressed.setCullHint(com.jme3.scene.Spatial.CullHint.Always);
        }
        if (buttonPressed != null) {
            buttonPressed.setCullHint(com.jme3.scene.Spatial.CullHint.Always);
        }
    }

    /**
     * 更新鼠标光标状态
     */
    private void updateMouseCursor() {
        if (isVisible) {
            // 显示背包时：显示鼠标并解除锁定
            app.getInputManager().setCursorVisible(true);
        } else {
            // 隐藏背包时：隐藏鼠标并锁定
            app.getInputManager().setCursorVisible(false);
        }
    }

    public boolean isVisible() {
        return isVisible;
    }

    /**
     * 接入背包数据（通用格子容器UI），创建对应的Lemur格子面板并挂载到背包界面上。
     * 只在背包界面（INTERFACE_TYPE.INVENTORY）显示，制造/生命界面时隐藏。
     */
    public void setBackpack(com.Hecate.item.Inventory backpack, com.Hecate.item.ItemRegistry itemRegistry) {
        if (backpackGridPanel != null) {
            backpackGridPanel.getContainer().removeFromParent();
        }
        backpackGridPanel = new com.Hecate.ui.inventory.InventoryGridPanel(
                backpack, itemRegistry, app.getAssetManager(), app.getGuiNode(), BACKPACK_COLUMNS, BACKPACK_SLOT_SIZE,
                BACKPACK_BACKGROUND_TEXTURE, BACKPACK_HIGHLIGHT_TEXTURE);
        app.getGuiNode().attachChild(backpackGridPanel.getContainer());
        wireItemHoverListener();
        positionBackpackGridPanel();
        updateVisibility();
    }

    /**
     * 设置面板管理器（鼠标悬停背包物品图标时显示/隐藏说明面板）。可能在setBackpack()
     * 调用之前或之后设置，两种顺序都要能正确接上。
     */
    public void setPanelManager(com.Hecate.ui.PanelManager panelManager) {
        this.panelManager = panelManager;
        wireItemHoverListener();
    }

    private void wireItemHoverListener() {
        if (backpackGridPanel == null) {
            return;
        }
        if (panelManager == null) {
            backpackGridPanel.setItemHoverListener(null);
            return;
        }
        backpackGridPanel.setItemHoverListener(new com.Hecate.ui.inventory.InventoryGridPanel.ItemHoverListener() {
            @Override
            public void onItemHovered(com.Hecate.item.ItemDef def, float screenX, float screenY) {
                panelManager.showIntroduceWordPanel(def.getName(), screenX, screenY);
            }

            @Override
            public void onItemUnhovered() {
                panelManager.hideIntroduceWordPanel();
            }
        });
    }

    /**
     * 将背包格子面板定位在背包背景贴图的中心（背包界面本身的贴图目前只是一张静态背景，
     * 没有预留格子区域坐标，先居中显示，后续贴图有专门的格子区域后再调整偏移）。
     */
    private void positionBackpackGridPanel() {
        if (backpackGridPanel == null) {
            return;
        }
        int screenWidth = app.getCamera().getWidth();
        int screenHeight = app.getCamera().getHeight();
        com.jme3.math.Vector3f panelSize = backpackGridPanel.getContainer().getPreferredSize();
        float panelX = (screenWidth - panelSize.x) / 2f;
        float panelY = (screenHeight + panelSize.y) / 2f;
        backpackGridPanel.getContainer().setLocalTranslation(panelX, panelY, 1010);
    }

    /**
     * 检查搜索框是否有焦点（正在输入）
     */
    public boolean isTextFieldFocused() {
        return searchTextField != null && searchTextField.isFocused();
    }

    /**
     * 更新方法（每帧调用）
     * 用于更新TextField的光标闪烁等动画效果
     */
    public void update(float tpf) {
        if (searchTextField != null) {
            searchTextField.update(tpf);
        }
        // 背包拖拽中时，幽灵图标需要每帧跟随鼠标——非拖拽状态下InventoryGridPanel.update是no-op
        if (backpackGridPanel != null) {
            backpackGridPanel.update(app.getInputManager().getCursorPosition());
        }
    }

    /**
     * 清理资源
     */
    public void cleanup() {
        if (inventoryBackground != null && inventoryBackground.getParent() != null) {
            inventoryBackground.removeFromParent();
        }
        if (forgeButton != null && forgeButton.getParent() != null) {
            forgeButton.removeFromParent();
        }
        if (forgeButtonPressed != null && forgeButtonPressed.getParent() != null) {
            forgeButtonPressed.removeFromParent();
        }
        if (craftSearchBox != null && craftSearchBox.getParent() != null) {
            craftSearchBox.removeFromParent();
        }
        if (searchButton != null && searchButton.getParent() != null) {
            searchButton.removeFromParent();
        }
        if (searchButtonPressed != null && searchButtonPressed.getParent() != null) {
            searchButtonPressed.removeFromParent();
        }
        if (buttonNormal != null && buttonNormal.getParent() != null) {
            buttonNormal.removeFromParent();
        }
        if (buttonPressed != null && buttonPressed.getParent() != null) {
            buttonPressed.removeFromParent();
        }
        if (craftPreviewPanel != null && craftPreviewPanel.getParent() != null) {
            craftPreviewPanel.removeFromParent();
        }
        if (craftSliderBg != null && craftSliderBg.getParent() != null) {
            craftSliderBg.removeFromParent();
        }
        if (craftSliderPreview != null && craftSliderPreview.getParent() != null) {
            craftSliderPreview.removeFromParent();
        }
        if (craftBottomBar != null && craftBottomBar.getParent() != null) {
            craftBottomBar.removeFromParent();
        }
        if (lifeChip != null && lifeChip.getParent() != null) {
            lifeChip.removeFromParent();
        }
        if (searchTextField != null) {
            searchTextField.cleanup();
        }
        if (backpackGridPanel != null && backpackGridPanel.getContainer().getParent() != null) {
            backpackGridPanel.getContainer().removeFromParent();
        }
        app.getInputManager().deleteMapping("InventoryClick");
        app.getInputManager().removeListener(this);
    }

    /**
     * 处理屏幕大小改变（如果需要的话）
     */
    public void onScreenResize(int width, int height) {
        // 移除旧的背包
        if (inventoryBackground != null) {
            inventoryBackground.removeFromParent();
        }

        // 重新创建适应新分辨率的背包
        createInventoryUI();

        // 恢复可见性状态
        updateVisibility();
    }
}
