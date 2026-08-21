package com.Hecate.player;

import com.Hecate.weapon.Weapon;
import com.Hecate.weapon.BasicShooter;
import com.jme3.app.SimpleApplication;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;

/**
 * 玩家战斗控制器
 * 负责武器装备、攻击逻辑、弹药管理，从 PlayerController 中抽离
 */
public class PlayerCombatController {

    private final SimpleApplication app;
    private final Camera camera;
    private final PlayerAmmo playerAmmo;

    // 当前武器
    private Weapon currentWeapon;

    // Gun1武器系统
    private Node gun1WeaponNode = null;
    private boolean isGun1Equipped = false;

    // Gun2武器系统（狙击枪）
    private Node gun2WeaponNode = null;
    private boolean isGun2Equipped = false;
    private com.Hecate.weapon.ProjectileManager projectileManager;

    // 统一的持有物品系统
    private Node currentHeldItemNode = null;

    // 持枪状态系统
    private boolean isHoldingGun = false;
    private boolean isLeftButtonPressed = false;
    private float continuousFireTimer = 0f;

    // 玩家位置获取器（用于射击起点）
    private PositionProvider positionProvider;

    // 武器依赖项（用于创建 SteampunkGun 等）
    private com.Hecate.flame.SimpleFlameRenderer flameRenderer;
    private com.Hecate.ink.SparseGridManager gridManager;
    private Node worldNode;
    private int playerFactionId = com.Hecate.ink.FactionRegistry.DARK_DEFAULT;
    private com.Hecate.monster.MonsterManager monsterManager;

    /**
     * 位置提供者接口
     */
    public interface PositionProvider {
        Vector3f getPosition();
    }

    /**
     * 构造函数
     */
    public PlayerCombatController(SimpleApplication app, PlayerAmmo playerAmmo) {
        this.app = app;
        this.camera = app.getCamera();
        this.playerAmmo = playerAmmo;

        // 初始化默认武器
        this.currentWeapon = BasicShooter.createDefault();
    }

    /**
     * 设置玩家位置提供者
     */
    public void setPositionProvider(PositionProvider provider) {
        this.positionProvider = provider;
    }

    /**
     * 设置子弹管理器（用于 Gun2）
     */
    public void setProjectileManager(com.Hecate.weapon.ProjectileManager projectileManager) {
        this.projectileManager = projectileManager;
    }

    /**
     * 设置火焰渲染器（用于 SteampunkGun）
     */
    public void setFlameRenderer(com.Hecate.flame.SimpleFlameRenderer flameRenderer) {
        this.flameRenderer = flameRenderer;
    }

    /**
     * 设置墨水网格管理器（用于 SteampunkGun）
     */
    public void setGridManager(com.Hecate.ink.SparseGridManager gridManager) {
        this.gridManager = gridManager;
    }

    /**
     * 设置世界节点（用于射线检测）
     */
    public void setWorldNode(Node worldNode) {
        this.worldNode = worldNode;
    }

    /**
     * 设置玩家阵营ID
     */
    public void setPlayerFactionId(int factionId) {
        this.playerFactionId = factionId;
    }

    /**
     * 设置怪物管理器（用于武器与怪物交互）
     */
    public void setMonsterManager(com.Hecate.monster.MonsterManager monsterManager) {
        this.monsterManager = monsterManager;
    }

    /**
     * 更新战斗逻辑（每帧调用）
     */
    public void update(float tpf) {
        // 更新持有物品位置（跟随摄像机）
        updateHeldItemPosition();

        // 更新武器状态
        if (currentWeapon != null) {
            currentWeapon.update(tpf);
        }

        // 处理连发
        if (isHoldingGun && isLeftButtonPressed && currentWeapon != null) {
            if (!currentWeapon.getStats().hasCharge()) {
                continuousFireTimer += tpf;
                // getFireRate()返回的是两次开火之间的秒数间隔（而非每秒发数），
                // 与Weapon.tryFire()内部冷却检测的语义一致，不能取倒数
                float fireInterval = currentWeapon.getStats().getFireRate();

                if (continuousFireTimer >= fireInterval) {
                    performGunAttack();
                    continuousFireTimer = 0f;
                }
            }
        }

        // 更新子弹管理器
        if (projectileManager != null) {
            projectileManager.update(tpf);
        }
    }

    /**
     * 装备 Gun1
     */
    public boolean equipGun1() {
        if (isGun1Equipped) {
            return false;
        }

        try {
            // 卸下其他武器
            unequipAllWeapons();

            // 加载 Gun1 模型
            gun1WeaponNode = (Node) app.getAssetManager().loadModel("weapons/steampunkgun.glb");
            gun1WeaponNode.setLocalScale(0.15f);
            // 本地-Z才是摄像机朝向的前方（camNode旋转=camera.getRotation()时），
            // 正Z会把模型摆到摄像机背后（看不见）
            gun1WeaponNode.setLocalTranslation(0.3f, -0.2f, -0.5f);

            // 附加到摄像机
            Node camNode = new Node("CameraNode");
            camNode.attachChild(gun1WeaponNode);
            app.getRootNode().attachChild(camNode);
            camNode.setLocalTranslation(camera.getLocation());
            camNode.setLocalRotation(camera.getRotation());

            currentHeldItemNode = camNode;
            isGun1Equipped = true;
            isHoldingGun = true;

            // 切换到 Gun1 专用武器（SteampunkGun）
            com.Hecate.weapon.WeaponStats steampunkStats = new com.Hecate.weapon.WeaponStats.Builder("steampunk_gun", "蒸汽朋克枪")
                .ammoCost(3f)
                .baseDamage(10f)
                .fireRate(0.3f)  // 每秒约3发
                .projectileVelocity(20f)
                .hasCharge(false)
                .build();
            com.Hecate.weapon.SteampunkGun steampunkGun = new com.Hecate.weapon.SteampunkGun(steampunkStats);

            // 设置依赖项
            if (flameRenderer != null) {
                steampunkGun.setFlameRenderer(flameRenderer);
            }
            if (gridManager != null) {
                steampunkGun.setGridManager(gridManager);
            }
            if (worldNode != null) {
                steampunkGun.setWorldNode(worldNode);
            }
            steampunkGun.setPlayerFactionId(playerFactionId);

            setCurrentWeapon(steampunkGun);

            return true;
        } catch (Exception e) {
            System.err.println("装备Gun1失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 卸下 Gun1
     */
    public boolean unequipGun1() {
        if (!isGun1Equipped) {
            return false;
        }

        if (gun1WeaponNode != null && gun1WeaponNode.getParent() != null) {
            gun1WeaponNode.getParent().removeFromParent();
        }

        gun1WeaponNode = null;
        isGun1Equipped = false;
        isHoldingGun = false;
        isLeftButtonPressed = false;
        continuousFireTimer = 0f;
        currentHeldItemNode = null;

        // 恢复默认武器
        setCurrentWeapon(BasicShooter.createDefault());

        return true;
    }

    /**
     * 装备 Gun2（狙击枪，使用占位方块）
     */
    public boolean equipGun2() {
        if (isGun2Equipped) {
            return false;
        }

        try {
            // 卸下其他武器
            unequipAllWeapons();

            // 创建占位方块模型
            Box box = new Box(0.1f, 0.05f, 0.4f);
            Geometry geo = new Geometry("Gun2Placeholder", box);
            Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
            mat.setColor("Color", ColorRGBA.DarkGray);
            geo.setMaterial(mat);

            gun2WeaponNode = new Node("Gun2Node");
            gun2WeaponNode.attachChild(geo);
            // 本地-Z才是摄像机朝向的前方（camNode旋转=camera.getRotation()时），
            // 正Z会把模型摆到摄像机背后（看不见）
            gun2WeaponNode.setLocalTranslation(0.2f, -0.15f, -0.3f);

            // 附加到摄像机
            Node camNode = new Node("CameraNode");
            camNode.attachChild(gun2WeaponNode);
            app.getRootNode().attachChild(camNode);
            camNode.setLocalTranslation(camera.getLocation());
            camNode.setLocalRotation(camera.getRotation());

            currentHeldItemNode = camNode;
            isGun2Equipped = true;
            isHoldingGun = true;

            // 切换到 Gun2 专用武器（SniperRifle）
            com.Hecate.weapon.WeaponStats sniperStats = new com.Hecate.weapon.WeaponStats.Builder("sniper_rifle", "狙击枪")
                .ammoCost(5f)
                .baseDamage(80f)
                .fireRate(0.5f)
                .projectileVelocity(50f)
                .hasCharge(true)
                .maxChargeTime(2.0f)
                .chargeMultiplier(2.25f)  // 180/80
                .maxRange(100f)
                .build();

            com.Hecate.weapon.ProjectileProfile sniperProfile = new com.Hecate.weapon.ProjectileProfile.Builder("sniper_projectile", "狙击弹")
                .arcType(com.Hecate.weapon.ProjectileProfile.ArcType.BALLISTIC)
                .gravity(1.0f)
                .hitEffect(com.Hecate.weapon.ProjectileProfile.HitEffect.piercing(
                    80f,    // damage
                    5.0f,   // inkRadius
                    0,      // inkTeam
                    100       // pierceCount - 穿透无上限个目标，但是出于方便就填了100个
                ))
                .visualConfig(com.Hecate.weapon.ProjectileProfile.VisualConfig.bullet())
                .build();

            com.Hecate.weapon.SniperRifle sniperRifle = new com.Hecate.weapon.SniperRifle(sniperStats, sniperProfile);

            // 设置依赖项
            if (gridManager != null) {
                sniperRifle.setGridManager(gridManager);
            }
            if (worldNode != null) {
                sniperRifle.setWorldNode(worldNode);
            }
            if (monsterManager != null) {
                sniperRifle.setMonsterManager(monsterManager);
            }
            sniperRifle.setPlayerFactionId(playerFactionId);

            setCurrentWeapon(sniperRifle);

            return true;
        } catch (Exception e) {
            System.err.println("装备Gun2失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 卸下 Gun2
     */
    public boolean unequipGun2() {
        if (!isGun2Equipped) {
            return false;
        }

        if (gun2WeaponNode != null && gun2WeaponNode.getParent() != null) {
            gun2WeaponNode.getParent().removeFromParent();
        }

        gun2WeaponNode = null;
        isGun2Equipped = false;
        isHoldingGun = false;
        isLeftButtonPressed = false;
        continuousFireTimer = 0f;
        currentHeldItemNode = null;

        // 恢复默认武器
        setCurrentWeapon(BasicShooter.createDefault());

        return true;
    }

    /**
     * 卸下所有武器
     */
    private void unequipAllWeapons() {
        if (isGun1Equipped) {
            unequipGun1();
        }
        if (isGun2Equipped) {
            unequipGun2();
        }
    }

    /**
     * 设置当前武器
     */
    public void setCurrentWeapon(Weapon weapon) {
        this.currentWeapon = weapon;
    }

    /**
     * 获取当前武器
     */
    public Weapon getCurrentWeapon() {
        return currentWeapon;
    }

    /**
     * 是否持有武器
     */
    public boolean isHoldingGun() {
        return isHoldingGun;
    }

    /**
     * 设置左键按下状态
     */
    public void setLeftButtonPressed(boolean pressed) {
        this.isLeftButtonPressed = pressed;
    }

    /**
     * 左键是否按下
     */
    public boolean isLeftButtonPressed() {
        return isLeftButtonPressed;
    }

    /**
     * 执行枪械攻击
     */
    public void performGunAttack() {
        if (currentWeapon == null || playerAmmo == null) {
            return;
        }

        // 检查武器是否支持蓄力
        if (currentWeapon.getStats().hasCharge()) {
            // 蓄力武器：开始蓄力
            if (!currentWeapon.isCharging()) {
                currentWeapon.startCharge();
            }
        } else {
            // 非蓄力武器：立即发射
            if (positionProvider != null) {
                Vector3f fireOrigin = positionProvider.getPosition().clone();
                fireOrigin.y += 1.0f;
                Vector3f fireDirection = camera.getDirection().clone();
                currentWeapon.tryFire(playerAmmo, fireOrigin, fireDirection);
            }
        }
    }

    /**
     * 释放蓄力攻击
     */
    public void releaseChargedAttack() {
        if (currentWeapon == null || playerAmmo == null) {
            return;
        }

        if (positionProvider != null) {
            Vector3f fireOrigin = positionProvider.getPosition().clone();
            fireOrigin.y += 1.0f;
            Vector3f fireDirection = camera.getDirection().clone();
            currentWeapon.releaseCharge(playerAmmo, fireOrigin, fireDirection);
        }
    }

    /**
     * 更新持有物品位置（跟随摄像机）
     */
    public void updateHeldItemPosition() {
        if (currentHeldItemNode != null) {
            currentHeldItemNode.setLocalTranslation(camera.getLocation());
            currentHeldItemNode.setLocalRotation(camera.getRotation());
        }
    }

    /**
     * 执行枪械攻击（带起点和方向参数）
     */
    public void performGunAttack(Vector3f fireOrigin, Vector3f fireDirection) {
        if (currentWeapon == null || playerAmmo == null) {
            return;
        }

        // 检查武器是否支持蓄力
        if (currentWeapon.getStats().hasCharge()) {
            // 蓄力武器：开始蓄力
            if (!currentWeapon.isCharging()) {
                currentWeapon.startCharge();
            }
        } else {
            // 非蓄力武器：立即发射
            currentWeapon.tryFire(playerAmmo, fireOrigin, fireDirection);
        }
    }

    /**
     * 释放蓄力攻击（带起点和方向参数）
     */
    public void releaseChargedAttack(Vector3f fireOrigin, Vector3f fireDirection) {
        if (currentWeapon == null || playerAmmo == null) {
            return;
        }

        currentWeapon.releaseCharge(playerAmmo, fireOrigin, fireDirection);
    }

    /**
     * 是否持有武器（兼容旧API）
     */
    public boolean isHoldingWeapon() {
        return isHoldingGun;
    }

    /**
     * 更新武器世界节点（用于世界切换）
     */
    public void updateWeaponWorldNode(Node worldNode) {
        if (currentWeapon instanceof com.Hecate.weapon.FlameWeapon) {
            ((com.Hecate.weapon.FlameWeapon) currentWeapon).setWorldNode(worldNode);
        }
    }

    /**
     * 扔掉当前手持物品
     */
    public void dropCurrentItem() {
        // 清除武器引用
        if (currentWeapon != null) {
            currentWeapon.cancelCharge();
            currentWeapon = null;
        }

        // 清除手持物品模型（统一处理所有持有物）
        if (currentHeldItemNode != null) {
            currentHeldItemNode.removeFromParent();
            currentHeldItemNode = null;
        }

        // 重置所有装备状态标志
        isGun1Equipped = false;
        isGun2Equipped = false;
        gun1WeaponNode = null;
        gun2WeaponNode = null;

        // 退出持枪状态
        isHoldingGun = false;
        isLeftButtonPressed = false;
        continuousFireTimer = 0f;


    }

    /**
     * 清理资源
     */
    public void cleanup() {
        unequipAllWeapons();
        if (currentHeldItemNode != null && currentHeldItemNode.getParent() != null) {
            currentHeldItemNode.removeFromParent();
        }
    }
}
