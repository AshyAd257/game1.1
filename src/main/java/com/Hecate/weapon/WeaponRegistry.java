package com.Hecate.weapon;

/**
 * 武器注册表 - 存储所有武器定义和子弹配置
 *
 * 类似"表格文件"，集中管理所有静态数据
 * 运行时通过ID查询定义
 */
public class WeaponRegistry {

    // 单例实例
    private static WeaponRegistry instance;

    // 数据存储
    private final java.util.Map<String, WeaponDefinition> weaponDefs;
    private final java.util.Map<String, ProjectileProfile> projectileProfiles;

    private WeaponRegistry() {
        weaponDefs = new java.util.HashMap<>();
        projectileProfiles = new java.util.HashMap<>();
        registerDefaults();
    }

    public static WeaponRegistry getInstance() {
        if (instance == null) {
            instance = new WeaponRegistry();
        }
        return instance;
    }

    /**
     * 注册默认武器和子弹配置
     */
    private void registerDefaults() {
        // ==================== 子弹配置 ====================

        // 1. 短距弹丸（速射手用）
        ProjectileProfile pelletShort = new ProjectileProfile.Builder(
                "pellet_short", "短距弹丸")
                .arcType(ProjectileProfile.ArcType.LINEAR)
                .velocity(25.0f)
                .maxRange(30.0f)
                .maxLifetime(1.2f)
                .hitEffect(ProjectileProfile.HitEffect.simple(8, 0.5f, 0))
                .visualConfig(ProjectileProfile.VisualConfig.bullet())
                .build();
        registerProjectile(pelletShort);

        // 2. 火焰粒子（火焰喷射器用）
        ProjectileProfile flameParticle = new ProjectileProfile.Builder(
                "flame_particle", "火焰粒子")
                .arcType(ProjectileProfile.ArcType.BALLISTIC)
                .velocity(15.0f)
                .gravity(-15.0f)  // 受重力影响
                .drag(0.98f)
                .maxRange(20.0f)
                .maxLifetime(2.0f)
                .hitEffect(new ProjectileProfile.HitEffect(
                        5, 0.5f, 0, true, false, 0))  // 点燃墨水
                .expireEffect(ProjectileProfile.ExpireEffect.dropInk())
                .visualConfig(ProjectileProfile.VisualConfig.flame())
                .build();
        registerProjectile(flameParticle);

        // 3. 散弹（蒸汽朋克枪用）
        ProjectileProfile pelletScatter = new ProjectileProfile.Builder(
                "pellet_scatter", "散弹")
                .arcType(ProjectileProfile.ArcType.LINEAR)
                .velocity(18.0f)
                .maxRange(5.0f)   // 短射程
                .maxLifetime(0.3f)
                .hitEffect(ProjectileProfile.HitEffect.simple(12, 0.8f, 0))
                .visualConfig(ProjectileProfile.VisualConfig.bullet())
                .build();
        registerProjectile(pelletScatter);

        // ==================== 武器定义 ====================

        // 1. 速射手（SMG）
        WeaponDefinition smg01 = new WeaponDefinition.Builder(
                "smg_01", "速射手")
                .fireMode(WeaponDefinition.FireMode.AUTO)
                .ammoMax(100)
                .ammoPerShot(1.0f)
                .projectileProfile("pellet_short")
                .behaviors("spread", "autoFire")
                .param("fireRate", 0.1f)         // 每0.1秒一发（600 RPM）
                .param("spreadAngle", 6.0f)      // 6度散射
                .viewConfig("dot", "rightHand")
                .build();
        registerWeapon(smg01);

        // 2. 火焰喷射器
        WeaponDefinition flameThrower = new WeaponDefinition.Builder(
                "flame_thrower", "火焰喷射器")
                .fireMode(WeaponDefinition.FireMode.AUTO)
                .ammoMax(1000)
                .ammoPerShot(10.0f)              // 每发消耗10点
                .projectileProfile("flame_particle")
                .behaviors("multiShot")          // 每次发射多个粒子
                .param("fireRate", 0.05f)        // 超快射速
                .param("shotsPerFire", 5)        // 每次5粒子
                .param("spreadAngle", 8.0f)
                .viewConfig("cross", "rightHand")
                .build();
        registerWeapon(flameThrower);

        // 3. 蒸汽朋克枪（霰弹枪）
        WeaponDefinition steampunkGun = new WeaponDefinition.Builder(
                "steampunk_gun", "蒸汽朋克枪")
                .fireMode(WeaponDefinition.FireMode.SINGLE)
                .ammoMax(24)
                .ammoPerShot(1.0f)
                .projectileProfile("pellet_scatter")
                .behaviors("spread", "multiShot")
                .param("fireRate", 0.8f)         // 慢射速
                .param("shotsPerFire", 8)        // 每次8发散弹
                .param("spreadAngle", 60.0f)     // 60度扇形
                .viewConfig("circle", "rightHand")
                .build();
        registerWeapon(steampunkGun);
    }

    /**
     * 注册武器定义
     */
    public void registerWeapon(WeaponDefinition def) {
        weaponDefs.put(def.getId(), def);
    }

    /**
     * 注册子弹配置
     */
    public void registerProjectile(ProjectileProfile profile) {
        projectileProfiles.put(profile.getId(), profile);
    }

    /**
     * 获取武器定义
     */
    public WeaponDefinition getWeaponDef(String id) {
        return weaponDefs.get(id);
    }

    /**
     * 获取子弹配置
     */
    public ProjectileProfile getProjectileProfile(String id) {
        return projectileProfiles.get(id);
    }

    /**
     * 创建武器实例（工厂方法）
     */
    public WeaponInstance createWeaponInstance(String weaponId) {
        WeaponDefinition def = getWeaponDef(weaponId);
        if (def == null) {
            throw new IllegalArgumentException("Unknown weapon ID: " + weaponId);
        }
        return new WeaponInstance(def);
    }

    /**
     * 获取所有武器ID
     */
    public java.util.Set<String> getAllWeaponIds() {
        return weaponDefs.keySet();
    }

    /**
     * 获取所有子弹配置ID
     */
    public java.util.Set<String> getAllProjectileIds() {
        return projectileProfiles.keySet();
    }
}
