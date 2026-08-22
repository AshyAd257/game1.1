package com.Hecate.monster;

import com.jme3.asset.AssetManager;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.Hecate.ink.SparseGridManager;
import com.Hecate.physics.AABB;
import com.Hecate.physics.CollisionManager;
import com.Hecate.player.PlayerController;

import java.util.ArrayList;
import java.util.List;

/**
 * 怪物管理器：持有当前世界内所有存活的怪物，负责：
 * <ul>
 *   <li>生成/更新/清理怪物实体</li>
 *   <li>子弹沿弹道路径的命中检测（{@link #checkHit}，由 FlameParticle 每帧调用）</li>
 *   <li>怪物死亡时在原地画一滩玩家友方涂色</li>
 * </ul>
 *
 * <p>怪物列表本身不按"世界"分离（竞技场/主世界共用同一个 MonsterManager 实例），
 * 但死亡涂墨要写入哪个 SparseGridManager 取决于玩家当前所在的世界——
 * {@link com.Hecate.arena.WorldSwitcher} 切换世界时会调用 {@link #setGridManager}
 * 重新指向当前活动的墨水网格。
 */
public class MonsterManager {

    private final AssetManager assetManager;
    private final List<Monster> monsters = new ArrayList<>();

    // 死亡涂墨参数
    private static final float DEATH_INK_RADIUS = 1.0f;

    // 波次系统参数：以玩家为圆心的出生圈半径；若该半径处是虚空（竞技场边缘外），
    // 依次退化到更小的半径，最保守时紧贴玩家生成。
    private static final float WAVE_SPAWN_RADIUS = 8.0f;
    private static final float WAVE_SPAWN_RADIUS_FALLBACK = 4.0f;
    private static final float WAVE_SPAWN_RADIUS_MIN = 1.5f;

    // 三波递进配置：数量 + 变体
    private static final int[] WAVE_COUNTS = {3, 6, 1};
    private static final MonsterVariant[] WAVE_VARIANTS = {
            MonsterVariant.SLOW, MonsterVariant.NORMAL, MonsterVariant.MINI_BOSS
    };

    private boolean waveActive = false;
    private int currentWaveNumber = 0; // 0表示尚未开始；1~WAVE_COUNTS.length为进行中的波次
    private int aliveInCurrentWave = 0;
    private Node activeWaveWorldNode;
    // 当前波已经死光、buff选择界面正在等待玩家操作（异步，跨越多帧）。
    // 防止update()每帧都重复判定"死光了"而反复弹出选择界面。
    private boolean waitingForBuffSelection = false;

    private SparseGridManager gridManager;

    // 碰撞管理器（用于AI移动时查询地形高度/避免虚空）与玩家控制器（索敌目标+接触伤害）。
    // 二者是全局单例，不随主世界/竞技场切换而更换实例——CollisionManager.setChunkManager()
    // 已经在WorldSwitcher里被重新指向到当前活动世界，这里持有的引用本身无需跟着变。
    private CollisionManager collisionManager;
    private PlayerController playerController;

    public MonsterManager(AssetManager assetManager) {
        this.assetManager = assetManager;
    }

    /**
     * 切换当前生效的墨水网格（世界切换时调用）
     */
    public void setGridManager(SparseGridManager gridManager) {
        this.gridManager = gridManager;
    }

    /**
     * 设置碰撞管理器（用于怪物AI移动时查询地形高度）
     */
    public void setCollisionManager(CollisionManager collisionManager) {
        this.collisionManager = collisionManager;
    }

    /**
     * 设置玩家控制器（怪物索敌目标，以及接触伤害的施加对象）
     */
    public void setPlayerController(PlayerController playerController) {
        this.playerController = playerController;
    }

    /**
     * 在指定位置生成一只普通怪物（用于 /mob1 命令）
     * @param parentNode 怪物几何体挂载的场景节点（应为当前活动世界的节点）
     * @param spawnFootPosition 出生点（脚底位置）
     */
    public Monster spawnMonster(Node parentNode, Vector3f spawnFootPosition) {
        return spawnMonster(parentNode, spawnFootPosition, MonsterVariant.NORMAL);
    }

    /**
     * 在指定位置生成一只指定变体的怪物
     * @param parentNode 怪物几何体挂载的场景节点（应为当前活动世界的节点）
     * @param spawnFootPosition 出生点（脚底位置，Monster构造函数会按变体体型转换为几何中心坐标）
     * @param variant 怪物变体
     */
    public Monster spawnMonster(Node parentNode, Vector3f spawnFootPosition, MonsterVariant variant) {
        Monster monster = new Monster(assetManager, parentNode, spawnFootPosition, variant);
        monsters.add(monster);
        return monster;
    }

    /**
     * 开始一次三波递进的遭遇战（/wave1 命令）。
     * <p>若已有波次在进行中，不重复触发，返回false。
     * @param parentNode 怪物挂载的场景节点（当前活动世界的节点）
     * @return 是否成功开始（false表示已有波次在进行中）
     */
    public boolean startWaveEncounter(Node parentNode) {
        if (waveActive) {
            return false;
        }
        activeWaveWorldNode = parentNode;
        currentWaveNumber = 0;
        waveActive = true;
        advanceWave();
        return true;
    }

    public boolean isWaveActive() {
        return waveActive;
    }

    /**
     * 推进到下一波：生成对应数量/变体的怪物；若已超过总波数，标记遭遇战结束。
     */
    private void advanceWave() {
        currentWaveNumber++;

        if (currentWaveNumber > WAVE_COUNTS.length) {
            waveActive = false;

            return;
        }

        if (playerController == null || activeWaveWorldNode == null) {
            // 依赖未就位，无法生成怪物，直接结束遭遇战避免卡死在"进行中"状态
            waveActive = false;

            return;
        }

        int count = WAVE_COUNTS[currentWaveNumber - 1];
        MonsterVariant variant = WAVE_VARIANTS[currentWaveNumber - 1];
        Vector3f playerPosition = playerController.getPlayerPosition();

        aliveInCurrentWave = 0;
        for (int i = 0; i < count; i++) {
            float angle = i * (FastMath.TWO_PI / count);
            Vector3f spawnPos = computeSpawnPoint(playerPosition, angle);

            Monster monster = spawnMonster(activeWaveWorldNode, spawnPos, variant);
            monster.setWaveNumber(currentWaveNumber);
            aliveInCurrentWave++;
        }


    }

    /**
     * 在以center为圆心、角度angle方向的出生圈上计算一个安全的出生点（脚底位置）。
     * <p>依次尝试 8米 → 4米 → 1.5米 半径，只要该点查得到有效地形高度就采用；
     * 全部半径都是虚空（例如玩家紧贴竞技场边缘）则退化为紧贴玩家、使用玩家当前高度。
     */
    private Vector3f computeSpawnPoint(Vector3f center, float angle) {
        float dirX = FastMath.cos(angle);
        float dirZ = FastMath.sin(angle);

        float[] radii = {WAVE_SPAWN_RADIUS, WAVE_SPAWN_RADIUS_FALLBACK, WAVE_SPAWN_RADIUS_MIN};
        for (float radius : radii) {
            float x = center.x + dirX * radius;
            float z = center.z + dirZ * radius;

            if (collisionManager != null) {
                float terrainHeight = collisionManager.getTerrainHeightAt(x, z);
                if (!Float.isNaN(terrainHeight)) {
                    return new Vector3f(x, terrainHeight, z);
                }
            } else {
                // 没有碰撞系统可查询，直接采用（与Monster.resolveSafeMovement的兜底行为一致）
                return new Vector3f(x, center.y, z);
            }
        }

        // 所有半径都是虚空：退化为玩家当前位置（使用玩家当前高度）
        return center.clone();
    }

    /**
     * 每帧更新所有怪物：移动AI、闪白/hit-stop计时、死亡清理。
     * 攻击冷却判定/攻击结算挪到了固定逻辑刻，见 {@link #fixedUpdate}。
     */
    public void update(float tpf) {
        Vector3f playerPosition = playerController != null
                ? playerController.getPlayerPosition() : null;

        for (int i = monsters.size() - 1; i >= 0; i--) {
            Monster monster = monsters.get(i);

            if (playerPosition != null) {
                monster.update(tpf, playerPosition, collisionManager);
            }

            if (!monster.isAlive()) {
                if (waveActive && monster.getWaveNumber() == currentWaveNumber) {
                    aliveInCurrentWave--;
                }
                handleDeath(monster);
                monster.removeFromScene();
                monsters.remove(i);
                continue;
            }
        }

        if (waveActive && !waitingForBuffSelection && aliveInCurrentWave <= 0) {
            if (currentWaveNumber < WAVE_COUNTS.length) {
                // 还有下一小波：直接推进，不弹选择
                advanceWave();
            } else {
                // 所有小波（1、2、3）都打完了：弹出buff选择，选完后开始新一轮
                triggerBuffSelection();
            }
        }
    }

    /**
     * 固定步长更新（由 {@link com.Hecate.core.FixedTickScheduler} 驱动，默认20Hz）。
     * <p>攻击冷却判定、攻击行为结算（近战接触伤害/远程开火等，具体由每只怪物的
     * {@link MonsterAttackBehavior} 决定）属于"战斗判定"，挪到固定刻以保证在任意
     * 渲染帧率下判定时序一致。怪物移动/死亡清理/涂墨等视觉相关的东西仍在 {@link #update} 里。
     */
    public void fixedUpdate(float dt) {
        for (int i = monsters.size() - 1; i >= 0; i--) {
            Monster monster = monsters.get(i);
            if (!monster.isAlive()) continue;

            monster.fixedUpdate(dt, playerController, gridManager);
        }
    }

    /**
     * 随机不重复抽取3个候选buff，弹出选择界面；玩家选完后重新开始第一波（新一轮）。
     */
    private void triggerBuffSelection() {
        if (playerController == null) {
            // 没有玩家控制器可弹窗，直接结束遭遇战，避免卡死
            waveActive = false;
            return;
        }

        waitingForBuffSelection = true;

        List<com.Hecate.player.BuffType> pool =
                new ArrayList<>(java.util.Arrays.asList(com.Hecate.player.BuffType.values()));
        java.util.Collections.shuffle(pool);
        List<com.Hecate.player.BuffType> options = pool.subList(0, 3);

        playerController.showBuffSelection(options, () -> {
            waitingForBuffSelection = false;
            // 选完buff后重新开始第一波（新一轮，怪物会更强）
            currentWaveNumber = 0;
            advanceWave();
        });
    }

    /**
     * 检测子弹路径上（从oldPos到newPos的线段）是否命中任意存活怪物，
     * 命中则立即造成伤害。
     * <p>沿弹道线段采样检测，与地形碰撞检测（{@code CollisionManager.getTerrainHeightAt}）
     * 使用相同的"沿路径步进"思路，确保子弹不会因为单帧位移过大而穿过怪物。
     *
     * @param oldPos 子弹上一帧位置
     * @param newPos 子弹当前帧位置
     * @param damage 命中造成的伤害
     * @param shotId 发射本次弹道的这一发子弹的唯一标识（同一次开火的所有粒子共享同一
     *               shotId），用于避免同一发子弹的多个粒子几乎同时命中时重复计伤
     * @return 命中点（怪物表面附近），如果没有命中返回null
     */
    public Vector3f checkHit(Vector3f oldPos, Vector3f newPos, float damage, long shotId) {
        if (monsters.isEmpty()) {
            return null;
        }

        Vector3f direction = newPos.subtract(oldPos);
        float distance = direction.length();

        if (distance < 0.0001f) {
            // 几乎没有移动，只检测当前点
            return checkPointHit(newPos, damage, shotId);
        }

        direction.normalizeLocal();

        float stepSize = 0.1f;
        int steps = (int) Math.ceil(distance / stepSize);

        for (int i = 0; i <= steps; i++) {
            float t = Math.min(i * stepSize, distance);
            Vector3f checkPos = oldPos.add(direction.mult(t));

            Vector3f hit = checkPointHit(checkPos, damage, shotId);
            if (hit != null) {
                return hit;
            }
        }

        return null;
    }

    private Vector3f checkPointHit(Vector3f point, float damage, long shotId) {
        for (Monster monster : monsters) {
            if (!monster.isAlive()) continue;

            AABB box = monster.getBoundingBox();
            if (box.containsPoint(point)) {
                monster.takeDamage(damage, shotId);
                return monster.getPosition();
            }
        }
        return null;
    }

    private void handleDeath(Monster monster) {
        if (gridManager != null) {
            // 玩家友方阵营：暗属性默认阵营（与PlayerController默认队伍1对应）
            gridManager.inkCircle(monster.getPosition(), DEATH_INK_RADIUS,
                    com.Hecate.ink.FactionRegistry.DARK_DEFAULT);
        }
    }

    /**
     * 清空所有怪物（世界切换/退出清理时调用）
     * <p>同时重置波次状态：切换世界后怪物列表已清空，不应留有"波次仍在进行中"的
     * 悬空状态（否则下一次 startWaveEncounter 会因 waveActive=true 被误判为重复触发）。
     */
    public void clear() {
        for (Monster monster : monsters) {
            monster.removeFromScene();
        }
        monsters.clear();

        waveActive = false;
        currentWaveNumber = 0;
        aliveInCurrentWave = 0;
        activeWaveWorldNode = null;
        waitingForBuffSelection = false;
    }

    public int getMonsterCount() {
        return monsters.size();
    }
}
