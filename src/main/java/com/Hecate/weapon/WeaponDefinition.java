package com.Hecate.weapon;

import java.util.HashMap;
import java.util.Map;

/**
 * 武器定义 - 武器的"表格数据"
 * 不可变的静态配置，所有武器实例共享
 *
 * 数据驱动设计：把所有变体参数都抽出来变成配置
 */
public class WeaponDefinition {

    // 基础信息
    private final String id;                    // 武器ID（唯一标识）
    private final String displayName;           // 显示名称

    // 武器种类（步枪/狙击枪/.../战锤）。同一个kind可以注册多个WeaponDefinition实例，
    // 作为"同一种武器的多个变体"（例如两把数值不同的步枪都是RIFLE）。
    private final WeaponKind kind;

    // 开火模式
    private final FireMode fireMode;            // auto/charge/single

    // 弹药配置
    private final int ammoMax;                  // 弹药上限
    private final float ammoPerShot;            // 每发消耗弹药

    // 子弹配置
    private final String projectileProfile;     // 子弹配置ID（引用ProjectileProfile）

    // 行为组件（可选模块）
    private final String[] behaviors;           // 行为列表：spread, charge, burst, etc.

    // 参数字典（behavior的配置参数）
    private final Map<String, Object> params;   // 灵活的参数存储

    // 视觉配置（UI/渲染相关）
    private final ViewConfig viewConfig;

    /**
     * 开火模式枚举
     */
    public enum FireMode {
        AUTO,       // 全自动：按住持续开火
        CHARGE,     // 蓄力：按住蓄力，松开释放
        SINGLE,     // 单发：每次点击发射一次
        BURST       // 三连发：每次点击连发N发
    }

    /**
     * 视觉配置（嵌套类）
     */
    public static class ViewConfig {
        public final String crosshair;      // 准星类型：dot, cross, circle
        public final String attachPoint;    // 挂载点：rightHand, leftHand, back

        public ViewConfig(String crosshair, String attachPoint) {
            this.crosshair = crosshair;
            this.attachPoint = attachPoint;
        }
    }

    /**
     * 构造函数（私有，使用Builder创建）
     */
    private WeaponDefinition(Builder builder) {
        this.id = builder.id;
        this.displayName = builder.displayName;
        this.kind = builder.kind;
        this.fireMode = builder.fireMode;
        this.ammoMax = builder.ammoMax;
        this.ammoPerShot = builder.ammoPerShot;
        this.projectileProfile = builder.projectileProfile;
        this.behaviors = builder.behaviors;
        this.params = builder.params;
        this.viewConfig = builder.viewConfig;
    }

    // Getters
    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public WeaponKind getKind() { return kind; }
    public FireMode getFireMode() { return fireMode; }
    public int getAmmoMax() { return ammoMax; }
    public float getAmmoPerShot() { return ammoPerShot; }
    public String getProjectileProfile() { return projectileProfile; }
    public String[] getBehaviors() { return behaviors; }
    public Map<String, Object> getParams() { return params; }
    public ViewConfig getViewConfig() { return viewConfig; }

    /**
     * 获取参数（带类型转换）
     */
    @SuppressWarnings("unchecked")
    public <T> T getParam(String key, T defaultValue) {
        Object value = params.get(key);
        if (value == null) return defaultValue;
        try {
            return (T) value;
        } catch (ClassCastException e) {
            return defaultValue;
        }
    }

    /**
     * Builder模式 - 流式API创建武器定义
     */
    public static class Builder {
        // 必需参数
        private final String id;
        private final String displayName;

        // 可选参数（设置默认值）
        // kind默认null——现有三个已注册武器（smg_01/flame_thrower/steampunk_gun）暂不分类，
        // 待正式接入新武器体系时再回填。
        private WeaponKind kind = null;
        private FireMode fireMode = FireMode.AUTO;
        private int ammoMax = 100;
        private float ammoPerShot = 1.0f;
        private String projectileProfile = "default_bullet";
        private String[] behaviors = new String[0];
        private Map<String, Object> params = new HashMap<>();
        private ViewConfig viewConfig = new ViewConfig("dot", "rightHand");

        public Builder(String id, String displayName) {
            this.id = id;
            this.displayName = displayName;
        }

        public Builder kind(WeaponKind val) { kind = val; return this; }
        public Builder fireMode(FireMode val) { fireMode = val; return this; }
        public Builder ammoMax(int val) { ammoMax = val; return this; }
        public Builder ammoPerShot(float val) { ammoPerShot = val; return this; }
        public Builder projectileProfile(String val) { projectileProfile = val; return this; }
        public Builder behaviors(String... val) { behaviors = val; return this; }
        public Builder param(String key, Object value) { params.put(key, value); return this; }
        public Builder viewConfig(String crosshair, String attachPoint) {
            viewConfig = new ViewConfig(crosshair, attachPoint);
            return this;
        }

        public WeaponDefinition build() {
            return new WeaponDefinition(this);
        }
    }

    @Override
    public String toString() {
        return String.format("WeaponDef[%s: %s, mode=%s, ammo=%d, projectile=%s]",
                id, displayName, fireMode, ammoMax, projectileProfile);
    }
}
