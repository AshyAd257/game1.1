package com.Hecate.item;

/**
 * 物品静态定义（表格数据）。
 * <p>与 {@link com.Hecate.block.Block}/{@code WeaponDefinition} 同类角色：由
 * {@link ItemRegistry} 集中持有，运行时通过id查询；{@link ItemStack} 只存id+数量，
 * 具体名称/图标/堆叠上限等都从这里查。
 */
public class ItemDef {
    private final String id;
    private final String name;
    private final String iconPath;
    private final int maxStackSize;
    // 物品图标在格子容器里占用的格数（如Gun1横向占2格），默认1x1。
    // 大于1x1的物品在Inventory里只有"锚点格"真正持有ItemStack，其余被占用的格子
    // 通过occupiedBy标记为不可用（见Inventory），不重复存储同一份数据。
    private final int cellWidth;
    private final int cellHeight;
    // 行为引用：为空表示该物品是纯素材（无左右键交互）。有blockId时左键挖/右键放置
    // （具体交互逻辑仍由现有的BlockInteraction系统负责，这里只是身份引用，不是行为实现）；
    // 有weaponId时左键开火（具体开火逻辑由WeaponFactory.create(weaponId)构造的Weapon负责）。
    // 两者理论上互斥（一个物品不该同时是方块又是武器），但这里不做强制校验——
    // 调用方（ItemRegistry.registerFromBlocks/registerFromWeapons）保证不会同时设置两者。
    private String blockId;
    private String weaponId;

    public ItemDef(String id, String name, String iconPath, int maxStackSize) {
        this(id, name, iconPath, maxStackSize, 1, 1);
    }

    public ItemDef(String id, String name, String iconPath, int maxStackSize, int cellWidth, int cellHeight) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("id 不能为空");
        }
        if (maxStackSize < 1) {
            throw new IllegalArgumentException("maxStackSize 必须至少为1");
        }
        if (cellWidth < 1 || cellHeight < 1) {
            throw new IllegalArgumentException("cellWidth/cellHeight 必须至少为1");
        }
        this.id = id;
        this.name = name;
        this.iconPath = iconPath;
        this.maxStackSize = maxStackSize;
        this.cellWidth = cellWidth;
        this.cellHeight = cellHeight;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getIconPath() {
        return iconPath;
    }

    public int getMaxStackSize() {
        return maxStackSize;
    }

    public int getCellWidth() {
        return cellWidth;
    }

    public int getCellHeight() {
        return cellHeight;
    }

    public boolean isMultiCell() {
        return cellWidth > 1 || cellHeight > 1;
    }

    public String getBlockId() {
        return blockId;
    }

    public boolean hasBlockId() {
        return blockId != null;
    }

    /**
     * 设置该物品对应的方块id（供 {@link ItemRegistry#registerFromBlocks} 自动注册调用）。
     */
    public ItemDef setBlockId(String blockId) {
        this.blockId = blockId;
        return this;
    }

    public String getWeaponId() {
        return weaponId;
    }

    public boolean hasWeaponId() {
        return weaponId != null;
    }

    /**
     * 设置该物品对应的武器id（供 {@link ItemRegistry#registerFromWeapons} 自动注册调用）。
     */
    public ItemDef setWeaponId(String weaponId) {
        this.weaponId = weaponId;
        return this;
    }

    @Override
    public String toString() {
        return "ItemDef{" + "id='" + id + '\'' + ", name='" + name + '\'' +
                ", maxStackSize=" + maxStackSize + ", cell=" + cellWidth + "x" + cellHeight + '}';
    }
}
