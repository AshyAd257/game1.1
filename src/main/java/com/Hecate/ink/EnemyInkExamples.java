package com.Hecate.ink;

import com.jme3.math.Vector3f;

/**
 * 敌人涂墨系统使用示例
 *
 * 【目录】
 * 1. 基础使用 - 敌人武器
 * 2. 静态涂墨 - InkHelper
 * 3. 自定义敌人武器
 * 4. 特殊效果涂墨
 * 5. AI集成示例
 *
 * @author Hecate Team
 * @since 2026-08-20
 */
public class EnemyInkExamples {

    // ===== 示例 1：创建敌人火焰武器 =====

    public void example1_CreateEnemyFlameWeapon(SparseGridManager gridManager) {
        // 创建敌人火焰武器（默认配置）
        EnemyFlameWeapon weapon = new EnemyFlameWeapon(FactionRegistry.DARK_DEFAULT);

        // 自定义参数
        weapon.setInkRadius(0.6f);
        weapon.setCooldown(1.5f);
        weapon.setRange(4.0f);

        // 敌人攻击时调用
        Vector3f enemyPos = new Vector3f(10, 0, 10);
        Vector3f targetDir = new Vector3f(1, 0, 0); // 向东发射

        weapon.fire(enemyPos, targetDir, gridManager);
    }

    // ===== 示例 2：创建敌人弹道武器 =====

    public void example2_CreateEnemyProjectileWeapon(SparseGridManager gridManager) {
        // 创建敌人弹道武器
        EnemyProjectileWeapon weapon = new EnemyProjectileWeapon(FactionRegistry.DARK_DEFAULT);

        // 自定义弹道参数
        InkHelper.ProjectilePreset preset = new InkHelper.ProjectilePreset();
        preset.radius = 0.5f;          // 涂墨半径
        preset.interval = 0.2f;        // 涂墨间隔
        preset.maxDistance = 8.0f;     // 最大射程

        weapon.setProjectilePreset(preset);

        // 敌人攻击时调用
        Vector3f enemyPos = new Vector3f(5, 0, 5);
        Vector3f targetDir = new Vector3f(0, 0, 1); // 向北发射

        weapon.fire(enemyPos, targetDir, gridManager);
    }

    // ===== 示例 3：创建自爆敌人 =====

    public void example3_SelfDestructEnemy(SparseGridManager gridManager) {
        // 创建区域涂墨武器（自爆效果）
        EnemyAreaWeapon weapon = new EnemyAreaWeapon(FactionRegistry.DARK_DEFAULT);

        // 配置爆炸参数
        weapon.setInkRadius(2.0f);       // 爆炸半径
        weapon.setShouldIgnite(true);    // 点燃涂墨

        // 敌人死亡时触发
        Vector3f deathPos = new Vector3f(0, 0, 0);
        weapon.fireAt(deathPos, gridManager);
    }

    // ===== 示例 4：使用 InkHelper 静态方法 =====

    public void example4_UseInkHelper(SparseGridManager gridManager) {
        Vector3f pos = new Vector3f(0, 0, 0);
        int enemyFaction = FactionRegistry.DARK_DEFAULT;

        // 单点涂墨
        InkHelper.inkPoint(pos, 1.0f, enemyFaction, gridManager);

        // 区域涂墨
        InkHelper.inkArea(pos, 2.0f, enemyFaction, gridManager);

        // 区域涂墨 + 点燃
        InkHelper.inkAndIgniteArea(pos, 1.5f, enemyFaction, gridManager);

        // 火焰散射涂墨
        Vector3f direction = new Vector3f(1, 0, 0);
        InkHelper.inkFlamePattern(pos, direction, enemyFaction, gridManager);

        // 弹道轨迹涂墨
        Vector3f endPos = new Vector3f(5, 0, 0);
        InkHelper.inkProjectileTrail(pos, endPos, enemyFaction, gridManager);

        // 环形涂墨（冲击波效果）
        InkHelper.inkRing(pos, 1.0f, 3.0f, enemyFaction, gridManager);

        // 线性涂墨（墙壁）
        InkHelper.inkLine(pos, endPos, 0.5f, enemyFaction, gridManager);
    }

    // ===== 示例 5：检查墨水状态 =====

    public void example5_CheckInkStatus(SparseGridManager gridManager) {
        Vector3f pos = new Vector3f(0, 0, 0);
        int myFaction = FactionRegistry.DARK_DEFAULT;

        // 检查是否有墨水
        boolean hasInk = InkHelper.hasInkAt(pos, gridManager);

        // 获取墨水阵营
        int inkFaction = InkHelper.getInkFactionAt(pos, gridManager);

        // 检查是否为敌方墨水
        boolean isEnemyInk = InkHelper.isEnemyInkAt(pos, myFaction, gridManager);

        System.out.println("位置 " + pos + ":");
        System.out.println("  有墨水: " + hasInk);
        System.out.println("  阵营: " + inkFaction);
        System.out.println("  是敌方: " + isEnemyInk);
    }

    // ===== 示例 6：自定义敌人武器类 =====

    /**
     * 自定义敌人武器示例：散弹枪
     */
    public static class EnemyShotgunWeapon extends BaseEnemyInkWeapon {

        private int pelletCount = 8;      // 弹丸数量
        private float spreadAngle = 30f;  // 散布角度

        public EnemyShotgunWeapon(int factionId) {
            super(factionId, "shotgun");
            this.inkRadius = 0.3f;
        }

        @Override
        public void fire(Vector3f origin, Vector3f direction, SparseGridManager gridManager) {
            if (!inkEnabled || gridManager == null) {
                return;
            }

            // 计算散弹落点
            Vector3f normalizedDir = direction.normalize();

            for (int i = 0; i < pelletCount; i++) {
                // 随机偏移角度
                float angleOffset = (float)(Math.random() * spreadAngle - spreadAngle / 2);
                float distance = 3.0f + (float)(Math.random() * 2.0f);

                // 计算落点（简化版，实际需要旋转矩阵）
                float radians = (float)Math.toRadians(angleOffset);
                Vector3f offset = new Vector3f(
                    (float)Math.sin(radians) * distance,
                    0,
                    (float)Math.cos(radians) * distance
                );

                Vector3f landPos = origin.add(normalizedDir.mult(distance)).add(offset);

                // 涂墨
                gridManager.inkCircle(landPos, inkRadius, factionId);
            }
        }

        @Override
        public float getCooldown() {
            return 2.0f;
        }

        @Override
        public float getRange() {
            return 5.0f;
        }
    }

    // ===== 示例 7：AI集成 - 简单敌人控制器 =====

    /**
     * 简单敌人AI示例
     */
    public static class SimpleEnemyAI {
        private BaseEnemyInkWeapon weapon;
        private SparseGridManager gridManager;
        private float cooldownTimer = 0f;

        public SimpleEnemyAI(BaseEnemyInkWeapon weapon, SparseGridManager gridManager) {
            this.weapon = weapon;
            this.gridManager = gridManager;
        }

        /**
         * 每帧更新
         * @param tpf 时间增量
         * @param enemyPos 敌人位置
         * @param targetPos 目标位置
         */
        public void update(float tpf, Vector3f enemyPos, Vector3f targetPos) {
            // 更新冷却
            cooldownTimer -= tpf;

            // 检查是否可以攻击
            if (cooldownTimer <= 0) {
                float distance = enemyPos.distance(targetPos);

                // 在射程内，发射武器
                if (distance <= weapon.getRange()) {
                    Vector3f direction = targetPos.subtract(enemyPos).normalize();
                    weapon.fire(enemyPos, direction, gridManager);

                    // 重置冷却
                    cooldownTimer = weapon.getCooldown();
                }
            }
        }
    }

    // ===== 示例 8：多阶段敌人攻击 =====

    /**
     * Boss敌人示例：三阶段攻击
     */
    public static class BossEnemy {
        private EnemyFlameWeapon flameWeapon;
        private EnemyProjectileWeapon projectileWeapon;
        private EnemyAreaWeapon areaWeapon;
        private SparseGridManager gridManager;

        private int currentPhase = 1;

        public BossEnemy(int factionId, SparseGridManager gridManager) {
            this.gridManager = gridManager;

            // 初始化三种武器
            this.flameWeapon = new EnemyFlameWeapon(factionId);
            this.projectileWeapon = new EnemyProjectileWeapon(factionId);
            this.areaWeapon = new EnemyAreaWeapon(factionId);
            this.areaWeapon.setShouldIgnite(true);
        }

        /**
         * 执行攻击
         * @param bossPos Boss位置
         * @param targetPos 目标位置
         */
        public void attack(Vector3f bossPos, Vector3f targetPos) {
            Vector3f direction = targetPos.subtract(bossPos).normalize();

            switch (currentPhase) {
                case 1:
                    // 阶段1：火焰攻击
                    flameWeapon.fire(bossPos, direction, gridManager);
                    break;

                case 2:
                    // 阶段2：远程弹道
                    projectileWeapon.fire(bossPos, direction, gridManager);
                    break;

                case 3:
                    // 阶段3：区域涂墨 + 环形冲击波
                    areaWeapon.fireAt(bossPos, gridManager);
                    InkHelper.inkRing(bossPos, 1.5f, 3.0f,
                        flameWeapon.getFactionId(), gridManager);
                    break;
            }
        }

        public void setPhase(int phase) {
            this.currentPhase = phase;
            System.out.println("Boss进入阶段 " + phase);
        }
    }

    // ===== 示例 9：环境涂墨陷阱 =====

    /**
     * 环境陷阱示例
     */
    public static class InkTrap {
        private Vector3f position;
        private float radius;
        private int factionId;
        private boolean triggered = false;

        public InkTrap(Vector3f position, float radius, int factionId) {
            this.position = position;
            this.radius = radius;
            this.factionId = factionId;
        }

        /**
         * 检测并触发陷阱
         * @param playerPos 玩家位置
         * @param gridManager 网格管理器
         */
        public void checkTrigger(Vector3f playerPos, SparseGridManager gridManager) {
            if (triggered) {
                return;
            }

            float distance = position.distance(playerPos);

            if (distance < 2.0f) {
                // 触发陷阱
                InkHelper.inkAndIgniteArea(position, radius, factionId, gridManager);
                triggered = true;
                System.out.println("陷阱触发！");
            }
        }

        public void reset() {
            triggered = false;
        }
    }
}
