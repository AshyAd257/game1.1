package com.Hecate.arena;

import com.Hecate.flame.FlameParticle;
import com.Hecate.ink.FactionRegistry;
import com.Hecate.ink.RegionMeshRenderer;
import com.Hecate.ink.SparseGridManager;
import com.Hecate.module.player.PlayerControlModule;
import com.Hecate.module.world.WorldModule;
import com.Hecate.monster.MonsterManager;
import com.Hecate.physics.CollisionManager;
import com.Hecate.player.PlayerController;
import com.Hecate.world.ChunkManager;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;

/**
 * 世界切换器 - 在主世界和竞技场之间双向切换
 *
 * <p>维护两套独立的 {@link ChunkManager}/场景节点/{@link SparseGridManager}（墨水网格）
 * 上下文，切换时：
 * <ul>
 *   <li>将非活动世界的根节点从场景图上整体摘除（而非仅设置CullHint隐藏——
 *       摘除后该子树不会被任何渲染/阴影/拾取遍历访问到，不存在"隐藏但仍被处理"的可能）</li>
 *   <li>重新绑定 {@link WorldModule} 的渲染/更新循环、{@link CollisionManager} 的地形查询、
 *       {@link PlayerControlModule} 的方块交互系统、墨水渲染/落地系统到目标世界</li>
 *   <li>保存/恢复玩家在主世界的位置，并将玩家传送到目标世界</li>
 * </ul>
 *
 * <p>墨水（{@link SparseGridManager}）按世界隔离为两份独立实例，而不是共享一份、
 * 靠坐标偏移规避碰撞——这样主世界和竞技场的墨水永远不会互相"渗出"，且两边的墨水
 * 都能独立按各自的时间线正常衰减（见 {@link #update}）。
 */
public class WorldSwitcher {

    public enum WorldType {
        MAIN_WORLD,
        ARENA
    }

    private final WorldModule worldModule;
    private final CollisionManager collisionManager;
    private final PlayerControlModule playerControlModule;
    private final PlayerController playerController;
    private final RegionMeshRenderer regionMeshRenderer;
    private final MonsterManager monsterManager;
    private final Node rootNode;

    // 主世界上下文（切换器创建时的初始状态）
    private final ChunkManager mainChunkManager;
    private final Node mainWorldNode;
    private final SparseGridManager mainGridManager;

    // 竞技场上下文（独立的ChunkManager + 独立的墨水网格，存档路径为 saves/arena/）
    private final ChunkManager arenaChunkManager;
    private final Node arenaWorldNode;
    private final ArenaWorld arenaWorld;
    private final SparseGridManager arenaGridManager;
    private boolean arenaGenerated = false;

    // 竞技场参数（回到世界原点——墨水按世界隔离后不再需要坐标偏移来规避碰撞）
    private static final float ARENA_CENTER_X = 0f;
    private static final float ARENA_CENTER_Z = 0f;
    private static final int ARENA_DIAMETER = 80;
    private static final float SPAWN_CLEARANCE = 3f; // 出生点在地面上方的额外高度，避免卡地形

    // 竞技场边缘填充网格下探深度：只需刚好遮住台面底部（几格厚的"边缘"即可），
    // 而不是主世界那种可无限挖掘的-300深坑——平台外本来就该是虚空，不是峡谷。
    private static final float ARENA_EDGE_FILL_DEPTH = ArenaWorld.FLOOR_HEIGHT - 3f;

    private WorldType currentWorld = WorldType.MAIN_WORLD;
    private Vector3f savedMainWorldPosition;
    private final Vector3f mainWorldSpawnPosition;

    /**
     * 构造世界切换器
     * <p>必须在 WorldModule 完成 onInitialize()（chunkManager/worldNode 已创建）、
     * PlayerControlModule 完成 onInitialize()（playerController 已创建）、
     * 且墨水系统（gridManager/regionMeshRenderer）已初始化之后构造。
     *
     * @param worldModule 世界模块（渲染/更新循环驱动者）
     * @param collisionManager 碰撞检测管理器
     * @param playerControlModule 玩家控制模块
     * @param regionMeshRenderer 墨水渲染器（用于切换渲染的墨水数据源）
     * @param mainGridManager 主世界的墨水网格管理器
     * @param rootNode 场景根节点，用于挂载竞技场世界节点
     * @param monsterManager 怪物管理器（可为null，即怪物系统未启用）
     */
    public WorldSwitcher(WorldModule worldModule, CollisionManager collisionManager,
                          PlayerControlModule playerControlModule, RegionMeshRenderer regionMeshRenderer,
                          SparseGridManager mainGridManager, Node rootNode, MonsterManager monsterManager) {
        this.worldModule = worldModule;
        this.collisionManager = collisionManager;
        this.playerControlModule = playerControlModule;
        this.playerController = playerControlModule.getPlayerController();
        this.regionMeshRenderer = regionMeshRenderer;
        this.mainGridManager = mainGridManager;
        this.rootNode = rootNode;
        this.monsterManager = monsterManager;

        // 记录主世界上下文（此时已挂载在rootNode下，由WorldModule.onInitialize创建）
        this.mainChunkManager = worldModule.getChunkManager();
        this.mainWorldNode = worldModule.getWorldNode();

        // 创建竞技场世界节点和独立的ChunkManager（暂不挂载到场景图，进入竞技场时才attach）
        // generateTerrainOnLoad=false：竞技场是封闭世界，玩家移动时WorldModule会动态
        // 加载周围区块——若不禁用，地板范围外的区块会自动长出与主世界同款的噪声地形，
        // 而不是保持虚空。
        this.arenaWorldNode = new Node("ArenaWorld");
        this.arenaChunkManager = new ChunkManager(arenaWorldNode, "arena", false);
        this.arenaWorld = new ArenaWorld(arenaChunkManager);

        // 创建竞技场独立的墨水网格（与主世界完全隔离的数据，参数与主世界保持一致）
        this.arenaGridManager = new SparseGridManager(new FactionRegistry());
        this.arenaGridManager.setInkDecayTime(mainGridManager.getInkDecayTime());
        this.arenaGridManager.setIgniteDecayTime(mainGridManager.getIgniteDecayTime());

        // 记录主世界原本的出生点（用于虚空死亡后按当前所在世界复活）
        this.mainWorldSpawnPosition = playerController.getSpawnPosition();
    }

    /**
     * 每帧更新（在ApplicationContext.update中调用）
     * <p>让当前未激活的那份墨水网格也能继续正常衰减，不会因为玩家不在那个世界
     * 而"冻结"——离开竞技场一段时间后回去，墨水应该已经按真实流逝的时间淡去。
     */
    public void update(float tpf) {
        mainGridManager.update(tpf);
        arenaGridManager.update(tpf);
    }

    /**
     * 切换世界（主世界 <-> 竞技场）
     */
    public void toggle() {
        if (currentWorld == WorldType.MAIN_WORLD) {
            enterArena();
        } else {
            returnToMainWorld();
        }
    }

    public WorldType getCurrentWorld() {
        return currentWorld;
    }

    private void enterArena() {


        // 1. 保存主世界玩家位置
        savedMainWorldPosition = playerController.getPlayerPosition().clone();

        // 2. 首次进入时生成竞技场地形（后续复用，不重复生成）
        if (!arenaGenerated) {
            arenaWorld.generateArena(ARENA_CENTER_X, ARENA_CENTER_Z, ARENA_DIAMETER);
            arenaGenerated = true;
        }

        // 3. 将主世界节点从场景图摘除，挂载竞技场节点
        //    直接摘除而非设置CullHint：确保主世界的地形/方块彻底脱离渲染、
        //    阴影投射、拾取射线等一切场景图遍历，不会有任何残留可见的可能。
        rootNode.detachChild(mainWorldNode);
        rootNode.attachChild(arenaWorldNode);

        // 4. 重新绑定渲染/碰撞/方块交互系统到竞技场
        worldModule.bindActiveWorld(arenaChunkManager, arenaWorldNode, ARENA_EDGE_FILL_DEPTH);
        collisionManager.setChunkManager(arenaChunkManager);
        playerControlModule.setChunkManager(arenaChunkManager);
        playerControlModule.setWorldNode(arenaWorldNode);

        // 4b. 重新绑定墨水系统到竞技场独立的墨水网格（不与主世界共享数据）
        regionMeshRenderer.setGridManager(arenaGridManager);
        FlameParticle.setGridManager(arenaGridManager);
        playerControlModule.setGridManager(arenaGridManager);

        // 4c. 清空怪物：MonsterManager的怪物列表不区分世界，若不清空，主世界的怪物
        //     几何体挂在已摘除的mainWorldNode下（不可见），但仍会被子弹命中检测判定为
        //     "存在"，导致竞技场中出现打中空气却触发命中的错乱。死亡涂墨也会改用
        //     竞技场的墨水网格，同样需要先清空再切换gridManager。
        if (monsterManager != null) {
            monsterManager.clear();
            monsterManager.setGridManager(arenaGridManager);
        }

        // 5. 传送玩家到竞技场中心，地面上方留出安全高度
        float spawnY = arenaWorld.getSpawnHeight(ARENA_CENTER_X, ARENA_CENTER_Z) + SPAWN_CLEARANCE;
        Vector3f arenaSpawnPos = new Vector3f(ARENA_CENTER_X, spawnY, ARENA_CENTER_Z);
        playerController.teleportTo(arenaSpawnPos);

        // 6. 出生点切换为竞技场：若玩家在竞技场中摔入虚空死亡，应复活在竞技场内
        //    重新开始战斗，而不是被送回主世界
        playerController.setSpawnPosition(arenaSpawnPos);

        currentWorld = WorldType.ARENA;
        System.out.println("已进入竞技场");
    }

    private void returnToMainWorld() {
        System.out.println("正在返回主世界...");

        // 1. 将竞技场节点从场景图摘除，挂载回主世界节点
        rootNode.detachChild(arenaWorldNode);
        rootNode.attachChild(mainWorldNode);

        // 2. 重新绑定渲染/碰撞/方块交互系统到主世界
        worldModule.bindActiveWorld(mainChunkManager, mainWorldNode);
        collisionManager.setChunkManager(mainChunkManager);
        playerControlModule.setChunkManager(mainChunkManager);
        playerControlModule.setWorldNode(mainWorldNode);

        // 2b. 重新绑定墨水系统回主世界的墨水网格
        regionMeshRenderer.setGridManager(mainGridManager);
        FlameParticle.setGridManager(mainGridManager);
        playerControlModule.setGridManager(mainGridManager);

        // 2c. 清空竞技场内的怪物（同理，见enterArena中的说明），切回主世界的墨水网格
        if (monsterManager != null) {
            monsterManager.clear();
            monsterManager.setGridManager(mainGridManager);
        }

        // 3. 恢复玩家在主世界的位置
        Vector3f returnPos = savedMainWorldPosition != null
                ? savedMainWorldPosition.clone()
                : mainWorldSpawnPosition.clone();
        playerController.teleportTo(returnPos);

        // 4. 出生点恢复为主世界：回到主世界后若摔入虚空死亡，应复活在主世界出生点
        playerController.setSpawnPosition(mainWorldSpawnPosition);

        currentWorld = WorldType.MAIN_WORLD;
        System.out.println("已返回主世界: " + returnPos);
    }

    /**
     * 保存两个世界所有被修改过的区块（游戏退出时调用）
     */
    public void saveAll() {
        mainChunkManager.saveAllModifiedChunks();
        arenaChunkManager.saveAllModifiedChunks();
    }
}
