package com.Hecate.character;

import com.jme3.app.SimpleApplication;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.light.AmbientLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;

/**
 * 混合角色系统示例
 * 演示如何使用 HybridCharacterRenderer
 */
public class HybridCharacterExample extends SimpleApplication {

    private HybridCharacterRenderer character;
    private int currentAnimIndex = 0;
    private String[] animations = {"idle", "walk", "run", "jump", "attack"};

    public static void main(String[] args) {
        HybridCharacterExample app = new HybridCharacterExample();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        // 设置相机位置
        cam.setLocation(new Vector3f(0, 2, 5));
        cam.lookAt(new Vector3f(0, 1, 0), Vector3f.UNIT_Y);

        // 添加光照
        setupLights();

        // 创建混合角色
        createCharacter();

        // 设置输入控制
        setupInput();

        // 显示使用说明
        printInstructions();
    }

    /**
     * 创建角色
     */
    private void createCharacter() {
        // 创建混合角色渲染器
        character = new HybridCharacterRenderer(this, "Characters/");

        // 加载默认皮肤
        // 注意：你需要先创建对应的配置文件和资源
        boolean success = character.loadSkin("skin_default");

        if (success) {
            // 设置位置
            character.setPosition(new Vector3f(0, 0, 0));

            // 附加到场景
            character.attachToScene(rootNode);

            // 播放待机动画
            character.playAnimation("idle", true);

            System.out.println("Character loaded successfully!");
            System.out.println("Available animations: " + character.getAvailableAnimations());
        } else {
            System.err.println("Failed to load character");
        }
    }

    /**
     * 设置光照
     */
    private void setupLights() {
        // 太阳光
        DirectionalLight sun = new DirectionalLight();
        sun.setDirection(new Vector3f(-0.5f, -0.5f, -0.5f).normalizeLocal());
        sun.setColor(ColorRGBA.White);
        rootNode.addLight(sun);

        // 环境光
        AmbientLight ambient = new AmbientLight();
        ambient.setColor(ColorRGBA.White.mult(0.3f));
        rootNode.addLight(ambient);
    }

    /**
     * 设置输入控制
     */
    private void setupInput() {
        // 数字键 1-5：切换动画
        for (int i = 0; i < 5; i++) {
            final int animIndex = i;
            inputManager.addMapping("Anim" + (i + 1), new KeyTrigger(KeyInput.KEY_1 + i));
            inputManager.addListener(new ActionListener() {
                @Override
                public void onAction(String name, boolean isPressed, float tpf) {
                    if (isPressed && character != null) {
                        playAnimation(animIndex);
                    }
                }
            }, "Anim" + (i + 1));
        }

        // R：更换武器
        inputManager.addMapping("ChangeWeapon", new KeyTrigger(KeyInput.KEY_R));
        inputManager.addListener(new ActionListener() {
            @Override
            public void onAction(String name, boolean isPressed, float tpf) {
                if (isPressed && character != null) {
                    changeWeapon();
                }
            }
        }, "ChangeWeapon");

        // T：更换头盔
        inputManager.addMapping("ChangeHelmet", new KeyTrigger(KeyInput.KEY_T));
        inputManager.addListener(new ActionListener() {
            @Override
            public void onAction(String name, boolean isPressed, float tpf) {
                if (isPressed && character != null) {
                    changeHelmet();
                }
            }
        }, "ChangeHelmet");

        // +/-：调整模型缩放
        inputManager.addMapping("ScaleUp", new KeyTrigger(KeyInput.KEY_EQUALS));
        inputManager.addMapping("ScaleDown", new KeyTrigger(KeyInput.KEY_MINUS));
        inputManager.addListener(new ActionListener() {
            @Override
            public void onAction(String name, boolean isPressed, float tpf) {
                if (!isPressed || character == null) return;

                float currentScale = character.getModelScale();
                if (name.equals("ScaleUp")) {
                    character.setModelScale(currentScale + 0.1f);
                    System.out.println("Model scale: " + character.getModelScale());
                } else {
                    character.setModelScale(Math.max(0.1f, currentScale - 0.1f));
                    System.out.println("Model scale: " + character.getModelScale());
                }
            }
        }, "ScaleUp", "ScaleDown");

        // H：切换3D模型可见性
        inputManager.addMapping("ToggleModel", new KeyTrigger(KeyInput.KEY_H));
        inputManager.addListener(new ActionListener() {
            @Override
            public void onAction(String name, boolean isPressed, float tpf) {
                if (isPressed && character != null) {
                    character.setHideModel(!character.isHideModel());
                    System.out.println("Hide model: " + character.isHideModel());
                }
            }
        }, "ToggleModel");
    }

    /**
     * 播放动画
     */
    private void playAnimation(int index) {
        if (index < 0 || index >= animations.length) return;

        String animName = animations[index];
        character.playAnimation(animName, true);
        System.out.println("Playing animation: " + animName);
    }

    /**
     * 更换武器
     */
    private void changeWeapon() {
        // 示例：在几种武器之间切换
        String[] weapons = {"weapon_sword", "weapon_axe", "weapon_bow", null};
        int weaponIndex = (int) (Math.random() * weapons.length);
        String weaponId = weapons[weaponIndex];

        if (weaponId == null) {
            // 移除武器
            character.removePart(PuppetPartDefinition.PartType.WEAPON_MAIN);
            System.out.println("Removed weapon");
        } else {
            // 更换武器
            character.replacePart(PuppetPartDefinition.PartType.WEAPON_MAIN, weaponId);
            System.out.println("Changed weapon to: " + weaponId);
        }
    }

    /**
     * 更换头盔
     */
    private void changeHelmet() {
        // 示例：在几种头盔之间切换
        String[] helmets = {"helmet_iron", "helmet_gold", "helmet_dragon", null};
        int helmetIndex = (int) (Math.random() * helmets.length);
        String helmetId = helmets[helmetIndex];

        if (helmetId == null) {
            // 移除头盔
            character.removePart(PuppetPartDefinition.PartType.ACCESSORY_HEAD);
            System.out.println("Removed helmet");
        } else {
            // 更换头盔
            character.replacePart(PuppetPartDefinition.PartType.ACCESSORY_HEAD, helmetId);
            System.out.println("Changed helmet to: " + helmetId);
        }
    }

    /**
     * 打印使用说明
     */
    private void printInstructions() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("Hybrid Character System Example");
        System.out.println("=".repeat(60));
        System.out.println("Controls:");
        System.out.println("  1-5    - Play animations (idle, walk, run, jump, attack)");
        System.out.println("  R      - Change weapon (random)");
        System.out.println("  T      - Change helmet (random)");
        System.out.println("  +/-    - Adjust model scale");
        System.out.println("  H      - Toggle 3D model visibility");
        System.out.println("  ESC    - Exit");
        System.out.println("=".repeat(60) + "\n");
    }

    @Override
    public void simpleUpdate(float tpf) {
        // 更新角色
        if (character != null) {
            character.update(tpf);
        }
    }

    @Override
    public void destroy() {
        // 清理资源
        if (character != null) {
            character.cleanup();
        }
        super.destroy();
    }
}
