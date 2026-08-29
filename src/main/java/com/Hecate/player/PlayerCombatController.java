package com.Hecate.player;

import com.Hecate.weapon.Weapon;
import com.Hecate.weapon.BasicShooter;
import com.jme3.app.SimpleApplication;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
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

    // 事件总线（武器装备/卸下时通知PanelManager等UI系统）
    private com.Hecate.event.EventBus eventBus;

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
     * 设置子弹管理器（用于 Gun1/Gun2）。世界切换等场景会重建ProjectileManager实例，
     * 此时如果Gun1正装备着，需要把它的spawnListener重新指向新实例，否则它会一直
     * 往旧的（已clear且不再被update的）ProjectileManager里生成子弹，表现为子弹消失。
     */
    public void setProjectileManager(com.Hecate.weapon.ProjectileManager projectileManager) {
        this.projectileManager = projectileManager;

        if (currentWeapon instanceof com.Hecate.weapon.SteampunkGun) {
            ((com.Hecate.weapon.SteampunkGun) currentWeapon).setSpawnListener(
                    projectileManager != null ? projectileManager::spawn : null);
        }
    }

    /**
     * 设置火焰渲染器（用于 SteampunkGun）
     */
    public void setFlameRenderer(com.Hecate.flame.SimpleFlameRenderer flameRenderer) {
        this.flameRenderer = flameRenderer;
    }

    /**
     * 设置事件总线（Gun1/Gun2装备/卸下时发布事件，供PanelManager等UI系统订阅）
     */
    public void setEventBus(com.Hecate.event.EventBus eventBus) {
        this.eventBus = eventBus;
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
            gun1WeaponNode.setLocalScale(0.3f);

            // 附加到世界节点，每帧按玩家位置+朝向计算世界坐标（第三人称视角下手持物需要
            // 跟随玩家本体而不是摄像机——挂在摄像机子节点上会因为离摄像机太近被近裁剪面
            // 裁掉，且第三人称视角下摄像机本就离玩家很远，不应该用第一人称的挂法）
            app.getRootNode().attachChild(gun1WeaponNode);

            currentHeldItemNode = gun1WeaponNode;
            isGun1Equipped = true;
            isHoldingGun = true;

            // 切换到 Gun1 专用武器（SteampunkGun，方块抛体）
            com.Hecate.weapon.SteampunkGun steampunkGun = com.Hecate.weapon.SteampunkGun.create();

            // 设置依赖项
            if (gridManager != null) {
                steampunkGun.setGridManager(gridManager);
            }
            if (worldNode != null) {
                steampunkGun.setWorldNode(worldNode);
            }
            steampunkGun.setPlayerFactionId(playerFactionId);
            // 子弹的实际飞行/命中/涂墨交给外部的子弹更新循环接管（与Gun2共用同一个ProjectileManager）
            if (projectileManager != null) {
                steampunkGun.setSpawnListener(projectileManager::spawn);
            }

            setCurrentWeapon(steampunkGun);

            if (eventBus != null) {
                eventBus.publish(new com.Hecate.event.WeaponEquippedEvent(
                        steampunkGun.getKind(), playerAmmo.getCurrentAmmo(), playerAmmo.getMaxAmmo()));
            }

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
            gun1WeaponNode.removeFromParent();
        }

        gun1WeaponNode = null;
        isGun1Equipped = false;
        isHoldingGun = false;
        isLeftButtonPressed = false;
        continuousFireTimer = 0f;
        currentHeldItemNode = null;

        // 恢复默认武器
        setCurrentWeapon(BasicShooter.createDefault());

        if (eventBus != null) {
            eventBus.publish(new com.Hecate.event.WeaponUnequippedEvent());
        }

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

            // 附加到世界节点，每帧按玩家位置+朝向计算世界坐标（与Gun1同理，见equipGun1注释）
            app.getRootNode().attachChild(gun2WeaponNode);

            currentHeldItemNode = gun2WeaponNode;
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

            if (eventBus != null) {
                eventBus.publish(new com.Hecate.event.WeaponEquippedEvent(
                        sniperRifle.getKind(), playerAmmo.getCurrentAmmo(), playerAmmo.getMaxAmmo()));
            }

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
            gun2WeaponNode.removeFromParent();
        }

        gun2WeaponNode = null;
        isGun2Equipped = false;
        isHoldingGun = false;
        isLeftButtonPressed = false;
        continuousFireTimer = 0f;
        currentHeldItemNode = null;

        // 恢复默认武器
        setCurrentWeapon(BasicShooter.createDefault());

        if (eventBus != null) {
            eventBus.publish(new com.Hecate.event.WeaponUnequippedEvent());
        }

        return true;
    }

    /**
     * 卸下所有武器（公开：切换快捷栏槛位时，PlayerController需要强制卸下Gun1/Gun2，
     * 确保"手上拿方块/普通武器"与"手上拿Gun1/Gun2"互斥）
     */
    public void unequipAllWeapons() {
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
     * 更新持有物品位置（跟随玩家本体，世界坐标）。
     * <p>第三人称视角下手持物挂在玩家身上而不是摄像机上——摄像机离玩家本来就有
     * DEFAULT_CAMERA_DISTANCE那么远，挂在摄像机子节点上会让物体离摄像机过近，
     * 被近裁剪面裁掉（见equipGun1/equipGun2的注释）。
     */
    public void updateHeldItemPosition() {
        if (currentHeldItemNode == null || positionProvider == null) {
            return;
        }

        // 摄像机水平朝向（忽略俯仰角，与移动方向计算保持一致）
        Vector3f forward = camera.getDirection().clone();
        forward.y = 0;
        if (forward.lengthSquared() < 0.0001f) {
            return;
        }
        forward.normalizeLocal();
        Vector3f right = forward.cross(Vector3f.UNIT_Y).normalizeLocal();

        Vector3f weaponPos = positionProvider.getPosition().clone();
        weaponPos.addLocal(forward.mult(0.8f));  // 玩家前方0.8个单位
        weaponPos.addLocal(right.mult(0.3f));    // 玩家右侧0.3个单位（模拟右手持枪）
        weaponPos.y += 0.6f;                     // 视线高度附近

        currentHeldItemNode.setLocalTranslation(weaponPos);

        Quaternion rotation = new Quaternion();
        rotation.lookAt(forward, Vector3f.UNIT_Y);
        currentHeldItemNode.setLocalRotation(rotation);
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
        boolean wasHoldingGun = isGun1Equipped || isGun2Equipped;

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

        if (wasHoldingGun && eventBus != null) {
            eventBus.publish(new com.Hecate.event.WeaponUnequippedEvent());
        }
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
