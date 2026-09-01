package com.Hecate.weapon;

/**
 * 武器工厂：按 {@link WeaponDefinition} 的id构造真正能开火的 {@link Weapon} 实例。
 * <p>这是 {@code WeaponRegistry} 里注册的静态数据（数值表）和实际"能打的枪"（Weapon子类）
 * 之间的桥——此前两者完全脱节：{@link WeaponRegistry} 注册了 smg_01/flame_thrower/
 * steampunk_gun 三条 {@link WeaponDefinition}，但从未有代码用它们构造出可开火的
 * {@link Weapon} 实例；{@link SteampunkGun}/{@link SniperRifle} 的真正开火数值完全是
 * 手写在各自的 {@code create()} 静态方法里，与 {@link WeaponRegistry} 里的同名条目
 * 数值不一致、互不知情。
 * <p>本类只做"id到构造方法"的分发，不改变 {@link SteampunkGun}/{@link SniperRifle}
 * 内部任何逻辑或数值。只有已经接上真正开火实现的武器id才在这里注册分支；
 * 未接入的（smg_01/flame_thrower）查询返回null——对应的 {@link WeaponDefinition}
 * 也应该保持 {@code obtainable=false}，不出现在物品栏自动注册里（见ItemRegistry），
 * 否则玩家会拿到一把"看起来是枪但打不出子弹"的哑物品。
 */
public final class WeaponFactory {
    private WeaponFactory() {
    }

    /**
     * 按武器id构造一把新的可开火武器实例；未接入开火逻辑的id返回null。
     */
    public static Weapon create(String weaponId) {
        if (weaponId == null) {
            return null;
        }
        switch (weaponId) {
            case "steampunk_gun":
                return SteampunkGun.create();
            case "sniper_rifle":
                return SniperRifle.create();
            default:
                return null;
        }
    }

    private static final java.util.Set<String> SUPPORTED_IDS =
            java.util.Set.of("steampunk_gun", "sniper_rifle");

    /**
     * 该武器id是否有对应的真实开火实现（即 {@link #create} 会返回非null）。
     * 用固定集合判断而不是实际调用create()再看是否为null——避免为了一次判断
     * 白白构造一个立刻丢弃的Weapon实例。
     */
    public static boolean isSupported(String weaponId) {
        return SUPPORTED_IDS.contains(weaponId);
    }
}
