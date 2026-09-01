package com.Hecate.player;

import com.Hecate.weapon.Weapon;
import com.Hecate.weapon.BasicShooter;
import com.jme3.app.SimpleApplication;
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
 * <p>武器装备/卸下不再由 /gun1、/gun2 等专属命令手写触发——本类实现
 * {@link com.Hecate.player.inventory.PlayerEquipment.WeaponEquipListener}，
 * 当玩家在背包里选中/切走某个带weaponId的物品槛位时，
 * {@link com.Hecate.player.inventory.PlayerEquipment} 会自动调用
 * {@link #onWeaponEquipped}/{@link #onWeaponUnequipped}。武器实例本身由
 * {@link com.Hecate.weapon.WeaponFactory} 按id构造，这里只负责把构造好的
 * {@link Weapon} 接入开火/连发/持有模型这些运行时状态。
 */
public class PlayerCombatController implements com.Hecate.player.inventory.PlayerEquipment.WeaponEquipListener {

    private final SimpleApplication app;
    private final Camera camera;
    private final PlayerAmmo playerAmmo;

    // 当前武器（背包选中槛位对应的Weapon实例，或空手默认武器BasicShooter）
    private Weapon currentWeapon;
    private String currentWeaponId; // 对应的物品id，用于避免重复装备同一把武器

    private com.Hecate.weapon.ProjectileManager projectileManager;

    // 当前手持物品的显示模型（跟随玩家身体的Node，见updateHeldItemPosition）
    private Node currentHeldItemNode = null;

    // 持枪状态系统
    private boolean isHoldingGun = false;
    private boolean isLeftButtonPressed = false;
    private float continuousFireTimer = 0f;

    // 玩家位置获取器（用于射击起点）
    private PositionProvider positionProvider;

    // 武器依赖项（构造Weapon实例后逐一注入）
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
     * 设置子弹管理器。世界切换等场景会重建ProjectileManager实例，此时如果当前武器
     * 是SteampunkGun，需要把它的spawnListener重新指向新实例，否则它会一直往旧的
     * （已clear且不再被update的）ProjectileManager里生成子弹，表现为子弹消失。
     */
    public void setProjectileManager(com.Hecate.weapon.ProjectileManager projectileManager) {
        this.projectileManager = projectileManager;

        if (currentWeapon instanceof com.Hecate.weapon.SteampunkGun) {
            ((com.Hecate.weapon.SteampunkGun) currentWeapon).setSpawnListener(
                    projectileManager != null ? projectileManager::spawn : null);
        } else if (currentWeapon instanceof com.Hecate.weapon.SniperRifle) {
            ((com.Hecate.weapon.SniperRifle) currentWeapon).setSpawnListener(
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
     * 设置事件总线（武器装备/卸下时发布事件，供PanelManager等UI系统订阅）
     */
    public void setEventBus(com.Hecate.event.EventBus eventBus) {
        this.eventBus = eventBus;
    }

    /**
     * 设置墨水网格管理器（用于 SteampunkGun/SniperRifle）
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
     * {@link com.Hecate.player.inventory.PlayerEquipment.WeaponEquipListener} 回调：
     * 背包选中槛位切到一个带weaponId的物品时触发。统一处理所有武器的装备逻辑——
     * 依赖注入（gridManager/worldNode/monsterManager/spawnListener等）、持有模型
     * 挂载、事件发布，此前这些代码在equipGun1/equipGun2里各写一份，现在按weaponId分支
     * 只在"构造依赖不同"这一点上有差异。
     */
    @Override
    public void onWeaponEquipped(String weaponId, Weapon weapon) {
        if (weaponId.equals(currentWeaponId)) {
            return; // 已经装备着同一把，避免重复走一遍挂载/事件发布
        }
        unequipCurrentWeapon();

        injectWeaponDependencies(weapon);
        setCurrentWeapon(weapon);
        currentWeaponId = weaponId;
        isHoldingGun = true;

        attachHeldModel(weaponId);

        if (eventBus != null) {
            eventBus.publish(new com.Hecate.event.WeaponEquippedEvent(
                    weapon.getKind(), playerAmmo.getCurrentAmmo(), playerAmmo.getMaxAmmo()));
        }
    }

    /**
     * {@link com.Hecate.player.inventory.PlayerEquipment.WeaponEquipListener} 回调：
     * 背包选中槛位切到一个没有weaponId的物品（或空格）时触发。
     */
    @Override
    public void onWeaponUnequipped() {
        if (currentWeaponId == null) {
            return; // 本来就没装备任何武器，no-op
        }
        unequipCurrentWeapon();

        if (eventBus != null) {
            eventBus.publish(new com.Hecate.event.WeaponUnequippedEvent());
        }
    }

    /**
     * 按weaponId把已构造好的Weapon实例注入运行时依赖（墨水/世界节点/怪物管理器/
     * 子弹生成监听器）——不同Weapon子类需要的依赖不同，用instanceof分支，
     * 与equipGun1/equipGun2原先各自手写注入的效果等价。
     */
    private void injectWeaponDependencies(Weapon weapon) {
        if (weapon instanceof com.Hecate.weapon.SteampunkGun) {
            com.Hecate.weapon.SteampunkGun gun = (com.Hecate.weapon.SteampunkGun) weapon;
            if (gridManager != null) gun.setGridManager(gridManager);
            if (worldNode != null) gun.setWorldNode(worldNode);
            gun.setPlayerFactionId(playerFactionId);
            if (projectileManager != null) gun.setSpawnListener(projectileManager::spawn);
        } else if (weapon instanceof com.Hecate.weapon.SniperRifle) {
            com.Hecate.weapon.SniperRifle rifle = (com.Hecate.weapon.SniperRifle) weapon;
            if (gridManager != null) rifle.setGridManager(gridManager);
            if (worldNode != null) rifle.setWorldNode(worldNode);
            if (monsterManager != null) rifle.setMonsterManager(monsterManager);
            rifle.setPlayerFactionId(playerFactionId);
            // 此前equipGun2()从未调用这一行——Gun2装备后开火实际上从未真正生成过
            // 子弹（SniperRifle.fireBullet内部判空直接跳过），是独立路径时代遗留的
            // 功能缺失，这次统一走注入流程后一并修正。
            if (projectileManager != null) rifle.setSpawnListener(projectileManager::spawn);
        }
    }

    /**
     * 挂载武器的手持显示模型。当前只有steampunk_gun有真实模型，其余武器用一个
     * 深灰色占位方块（与此前equipGun2的占位方块效果一致）——武器种类会持续增加，
     * 不要求每种都先画好模型才能测试装备/开火逻辑。
     */
    private void attachHeldModel(String weaponId) {
        Node modelNode;
        if ("steampunk_gun".equals(weaponId)) {
            modelNode = (Node) app.getAssetManager().loadModel("weapons/steampunkgun.glb");
            modelNode.setLocalScale(0.3f);
        } else {
            Box box = new Box(0.1f, 0.05f, 0.4f);
            Geometry geo = new Geometry(weaponId + "Placeholder", box);
            Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
            mat.setColor("Color", ColorRGBA.DarkGray);
            geo.setMaterial(mat);
            modelNode = new Node(weaponId + "Node");
            modelNode.attachChild(geo);
        }

        // 附加到根节点，每帧按玩家位置+朝向计算世界坐标（第三人称视角下手持物需要
        // 跟随玩家本体而不是摄像机——挂在摄像机子节点上会因为离摄像机太近被近裁剪面
        // 裁掉，且第三人称视角下摄像机本就离玩家很远，不应该用第一人称的挂法）
        app.getRootNode().attachChild(modelNode);
        currentHeldItemNode = modelNode;
    }

    /**
     * 卸下当前武器：移除持有模型、重置状态标志、恢复空手默认武器。
     * 不发布事件——事件发布由调用方（onWeaponEquipped换枪前/onWeaponUnequipped）决定，
     * 因为"换枪"场景不需要中间发一次WeaponUnequippedEvent再发WeaponEquippedEvent。
     */
    private void unequipCurrentWeapon() {
        if (currentHeldItemNode != null && currentHeldItemNode.getParent() != null) {
            currentHeldItemNode.removeFromParent();
        }
        currentHeldItemNode = null;
        currentWeaponId = null;

        isHoldingGun = false;
        isLeftButtonPressed = false;
        continuousFireTimer = 0f;

        setCurrentWeapon(BasicShooter.createDefault());
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
     * 被近裁剪面裁掉（见attachHeldModel的注释）。
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
     * 清理资源
     */
    public void cleanup() {
        unequipCurrentWeapon();
    }
}
