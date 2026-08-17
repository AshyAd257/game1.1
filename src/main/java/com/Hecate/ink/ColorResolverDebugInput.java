package com.Hecate.ink;

import com.jme3.app.SimpleApplication;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;

/**
 * ColorResolver 游戏内测试工具
 *
 * 按键说明：
 * - F5: 切换观察者阵营（光/暗）
 * - F6: 切换战斗状态（开/关）
 *
 * 使用方式：
 * 在 ApplicationContext 或 Main 中注册这个监听器
 */
public class ColorResolverDebugInput implements ActionListener {

    // 支持旧渲染器或新渲染器或Decal渲染器
    private final Object renderer; // GridDebugRenderer 或 RegionMeshRenderer 或 DecalInkRenderer
    private int currentFactionId = FactionRegistry.DARK_DEFAULT;
    private boolean inCombat = true;

    public ColorResolverDebugInput(Object renderer) {
        this.renderer = renderer;
    }

    /**
     * 注册按键监听
     */
    public void registerInputs(SimpleApplication app) {
        app.getInputManager().addMapping("ToggleFaction", new KeyTrigger(KeyInput.KEY_F5));
        app.getInputManager().addMapping("ToggleCombat", new KeyTrigger(KeyInput.KEY_F6));
        app.getInputManager().addListener(this, "ToggleFaction", "ToggleCombat");
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (!isPressed) return;

        switch (name) {
            case "ToggleFaction":
                toggleFaction();
                break;
            case "ToggleCombat":
                toggleCombat();
                break;
        }
    }

    /**
     * 切换观察者阵营
     */
    private void toggleFaction() {
        if (currentFactionId == FactionRegistry.LIGHT_DEFAULT) {
            currentFactionId = FactionRegistry.DARK_DEFAULT;
            System.out.println("[ColorResolver测试] 切换到暗属性视角");
        } else {
            currentFactionId = FactionRegistry.LIGHT_DEFAULT;
            System.out.println("[ColorResolver测试] 切换到光属性视角");
        }

        // 兼容三种渲染器
        if (renderer instanceof GridDebugRenderer) {
            ((GridDebugRenderer) renderer).setObserverFactionId(currentFactionId);
        } else if (renderer instanceof RegionMeshRenderer) {
            ((RegionMeshRenderer) renderer).setObserverFactionId(currentFactionId);
        } else if (renderer instanceof DecalInkRenderer) {
            ((DecalInkRenderer) renderer).setObserverFactionId(currentFactionId);
        }
    }

    /**
     * 切换战斗状态
     */
    private void toggleCombat() {
        inCombat = !inCombat;
        System.out.println("[ColorResolver测试] 战斗状态: " + (inCombat ? "开启" : "关闭"));

        // 兼容三种渲染器
        if (renderer instanceof GridDebugRenderer) {
            ((GridDebugRenderer) renderer).setObserverInCombat(inCombat);
        } else if (renderer instanceof RegionMeshRenderer) {
            ((RegionMeshRenderer) renderer).setObserverInCombat(inCombat);
        } else if (renderer instanceof DecalInkRenderer) {
            ((DecalInkRenderer) renderer).setObserverInCombat(inCombat);
        }
    }
}
