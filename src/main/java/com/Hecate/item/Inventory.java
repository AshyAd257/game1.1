package com.Hecate.item;

import java.util.ArrayList;
import java.util.List;

/**
 * 通用格子容器：固定N格（按columns分行），每格存一个 {@link ItemStack}（空格为
 * {@link ItemStack#EMPTY}）。背包、箱子、战利品、镶嵌界面都基于同一个类——不为某个
 * 具体用途（如"背包"）派生子类，差异（格数、列数、UI贴图）由调用方在构造时/展示层决定。
 *
 * <p><b>跨格物品（如Gun1横向占2格）</b>：只有"锚点格"（放置时点击/传入的那个index）在
 * {@code slots}里真正持有 {@link ItemStack}；物品在 {@link ItemDef} 里声明的
 * cellWidth/cellHeight会向右/向下额外占用的格子，通过 {@code occupiedBy} 标记为
 * "被该锚点占用"，本身仍是空ItemStack。这样避免同一份数据在多个格子里重复存储，
 * UI只需要在锚点格渲染跨格图标，其余被占用格保持不渲染。
 *
 * <p>{@link #addItem} 是唯一的"拿物品"入口：1x1物品优先堆叠到已有同类未满槛位、
 * 再填入空槛位（堆叠上限来自 {@link ItemRegistry}）；跨格物品不支持堆叠，逐个寻找
 * 能完整容纳其footprint的空位。
 */
public class Inventory {
    private final List<ItemStack> slots;
    // occupiedBy[i] = 占用第i格的锚点格index；-1表示该格空闲（既不是锚点也不是任何物品的跨格延伸）
    private final int[] occupiedBy;
    private final int columns;
    private final ItemRegistry itemRegistry;

    /**
     * 单行容器（向后兼容旧调用）：columns=size，等价于所有格子排成一行。
     * 跨格物品在单行容器里仍然可以正常放置（只要不超出这一行的右边界）。
     */
    public Inventory(int size, ItemRegistry itemRegistry) {
        this(size, size, itemRegistry);
    }

    public Inventory(int size, int columns, ItemRegistry itemRegistry) {
        if (size < 1) {
            throw new IllegalArgumentException("size 必须至少为1");
        }
        if (columns < 1) {
            throw new IllegalArgumentException("columns 必须至少为1");
        }
        if (itemRegistry == null) {
            throw new IllegalArgumentException("itemRegistry 不能为空");
        }
        this.columns = columns;
        this.itemRegistry = itemRegistry;
        this.slots = new ArrayList<>(size);
        this.occupiedBy = new int[size];
        for (int i = 0; i < size; i++) {
            slots.add(ItemStack.EMPTY);
            occupiedBy[i] = -1;
        }
    }

    public int getSize() {
        return slots.size();
    }

    public int getColumns() {
        return columns;
    }

    /**
     * 指定格子的原始物品堆。跨格物品的"被占用格"（非锚点）返回EMPTY——
     * 该格并不真正持有数据，渲染时应改用 {@link #getAnchorIndex} 找到锚点格再取数据。
     */
    public ItemStack getSlot(int index) {
        checkIndex(index);
        return slots.get(index);
    }

    /**
     * 占用该格的锚点格index；该格空闲则返回-1；该格自身就是锚点则返回自身index。
     */
    public int getAnchorIndex(int index) {
        checkIndex(index);
        return occupiedBy[index];
    }

    public boolean isEmpty(int index) {
        checkIndex(index);
        return occupiedBy[index] < 0;
    }

    /**
     * 直接在指定格子设置物品堆并作为锚点（用于拖拽换位、清空等"精确控制"场景）。
     * 会先清空该格此前占用的旧footprint，再按新物品的cellWidth/cellHeight校验并
     * 标记新footprint；新footprint与其他物品的footprint重叠时拒绝写入并抛异常
     * （调用方应先确认目标位置空闲，例如通过 {@link #canPlaceAt}）。
     */
    public void setSlot(int index, ItemStack stack) {
        checkIndex(index);
        clearFootprintOwnedBy(index);

        if (stack == null || stack.isEmpty()) {
            slots.set(index, ItemStack.EMPTY);
            return;
        }

        ItemDef def = itemRegistry.getItemDef(stack.getItemId());
        if (def == null) {
            throw new IllegalArgumentException("未知物品ID: " + stack.getItemId());
        }

        int[] footprint = computeFootprint(index, def.getCellWidth(), def.getCellHeight());
        if (footprint == null) {
            throw new IllegalStateException("物品 " + stack.getItemId() + " 无法放置在格子 " + index + "（超出边界或与其他物品重叠）");
        }

        slots.set(index, stack);
        for (int cell : footprint) {
            occupiedBy[cell] = index;
        }
    }

    /**
     * 清空指定格子所属的整个物品footprint（点击跨格物品的任意被占用格，效果等同于清空整个物品）。
     */
    public void clearSlot(int index) {
        checkIndex(index);
        int anchor = occupiedBy[index];
        if (anchor < 0) {
            return;
        }
        clearFootprintOwnedBy(anchor);
    }

    /**
     * 若在index处放置指定尺寸的物品，footprint是否完全落在边界内且不与现有物品重叠。
     */
    public boolean canPlaceAt(int index, int cellWidth, int cellHeight) {
        checkIndex(index);
        return computeFootprint(index, cellWidth, cellHeight) != null;
    }

    /**
     * 指定格子背后实际物品占用的所有格子index——用于UI高亮"整个物品"而不是单个格子
     * （如Gun1横向占2格，鼠标悬停在任一格上，两格都应该高亮，不能只亮锚点或只亮鼠标
     * 所在的那一格）。
     * @param index 格子（可以是锚点格，也可以是被占用的任意格）
     * @return 该格所属物品的完整footprint；该格是空格则返回只含自身的单元素数组
     */
    public int[] getItemFootprint(int index) {
        checkIndex(index);
        int anchor = occupiedBy[index];
        if (anchor < 0) {
            return new int[]{index};
        }
        ItemStack stack = slots.get(anchor);
        ItemDef def = itemRegistry.getItemDef(stack.getItemId());
        // def不可能为null（能存进slots的物品必然在setSlot时已校验过itemRegistry里存在）；
        // 兜底返回锚点自身，避免因防御性判断中断UI高亮逻辑
        if (def == null) {
            return new int[]{anchor};
        }
        int[] footprint = computeFootprintBounded(anchor, def.getCellWidth(), def.getCellHeight());
        return footprint != null ? footprint : new int[]{anchor};
    }

    /**
     * 预览：若以anchorIndex为锚点摆放cellWidth x cellHeight的物品，会覆盖哪些格子——
     * 只做边界检查（不越出行边界/容器总格数），不检查是否与其他物品重叠。用于拖拽悬停时
     * 的落点预览高亮（此刻物品尚未真正放下，允许"预览到一个当前被占用的位置"，实际能否
     * 放下由松手时调用 {@link #moveItem} 真正校验，校验结果与本方法的边界判断相互独立）。
     * @return 覆盖的格子index；若锚点本身选取的位置连边界都不满足（换行/超出总格数），返回null
     */
    public int[] previewFootprint(int anchorIndex, int cellWidth, int cellHeight) {
        checkIndex(anchorIndex);
        return computeFootprintBounded(anchorIndex, cellWidth, cellHeight);
    }

    /**
     * 交换两个槛位的内容（拖拽换位）。仅支持两侧都是1x1物品（或空格）——跨格物品的
     * 拖拽换位需要额外的"目标区域是否容得下新footprint"校验，当前UI交互（点两次换位）
     * 尚未覆盖这个场景，先明确拒绝而不是产生错误的占用状态。
     */
    public void swapSlots(int indexA, int indexB) {
        checkIndex(indexA);
        checkIndex(indexB);

        if (isMultiCellFootprint(indexA) || isMultiCellFootprint(indexB)) {
            throw new IllegalStateException("跨格物品暂不支持拖拽换位（格子 " + indexA + " / " + indexB + "）");
        }

        ItemStack a = slots.get(indexA);
        ItemStack b = slots.get(indexB);
        int anchorA = occupiedBy[indexA];
        int anchorB = occupiedBy[indexB];

        slots.set(indexA, b);
        slots.set(indexB, a);
        occupiedBy[indexA] = anchorB < 0 ? -1 : indexA;
        occupiedBy[indexB] = anchorA < 0 ? -1 : indexB;
    }

    /**
     * 拖拽落位：把fromIndex处的物品移动到toIndex（不做同类堆叠合并，只做位置交换/搬移——
     * 拖拽落在同类未满堆叠上不会自动合并数量，因为这不是本次要支持的行为）。
     * <p>支持跨格物品（与 {@link #swapSlots} 不同，那个方法遇到跨格物品会直接拒绝）：
     * <ul>
     *   <li>目标格为空：直接把整个footprint搬到以toIndex为锚点的新位置，新位置必须完整
     *       容纳该物品尺寸，否则搬移失败并保持原状不变</li>
     *   <li>目标格被另一物品占用：尝试双向交换——A搬到B的锚点、B搬到A的锚点，双方新位置
     *       都必须放得下（包括两者新footprint互相不重叠），否则整体回滚、两者都保持原状</li>
     *   <li>fromIndex/toIndex是同一个物品footprint内的格子（包括fromIndex==toIndex，
     *       或跨格物品拖到自己身上其他被占用格）：视为no-op，直接返回true</li>
     * </ul>
     * @param fromIndex 拖拽起点（可以是物品的锚点格，也可以是它跨格占用的任意一格）
     * @param toIndex 拖拽落点
     * @return 是否成功移动；false表示目标位置放不下，fromIndex处的物品保持原样未变
     */
    public boolean moveItem(int fromIndex, int toIndex) {
        checkIndex(fromIndex);
        checkIndex(toIndex);

        int fromAnchor = occupiedBy[fromIndex];
        if (fromAnchor < 0) {
            return false; // 起点是空格，没有物品可移动
        }

        int toAnchor = occupiedBy[toIndex];
        if (toAnchor == fromAnchor) {
            return true; // 落在自己身上（含跨格物品的其他被占用格），no-op
        }

        ItemStack fromStack = slots.get(fromAnchor);
        ItemDef fromDef = itemRegistry.getItemDef(fromStack.getItemId());

        if (toAnchor < 0) {
            return moveToEmpty(fromAnchor, fromStack, fromDef, toIndex);
        }
        return swapWithOccupied(fromAnchor, fromStack, fromDef, toAnchor);
    }

    private boolean moveToEmpty(int fromAnchor, ItemStack fromStack, ItemDef fromDef, int toIndex) {
        clearFootprintOwnedBy(fromAnchor);
        if (!canPlaceAt(toIndex, fromDef.getCellWidth(), fromDef.getCellHeight())) {
            setSlot(fromAnchor, fromStack); // 回滚：原footprint刚被清空，此刻必然还空着，可以放回
            return false;
        }
        setSlot(toIndex, fromStack);
        return true;
    }

    private boolean swapWithOccupied(int fromAnchor, ItemStack fromStack, ItemDef fromDef, int toAnchor) {
        ItemStack toStack = slots.get(toAnchor);
        ItemDef toDef = itemRegistry.getItemDef(toStack.getItemId());

        clearFootprintOwnedBy(fromAnchor);
        clearFootprintOwnedBy(toAnchor);

        if (!canPlaceAt(fromAnchor, toDef.getCellWidth(), toDef.getCellHeight())) {
            setSlot(fromAnchor, fromStack);
            setSlot(toAnchor, toStack);
            return false;
        }
        setSlot(fromAnchor, toStack);

        // 此时fromAnchor的格子已经被toStack的新footprint占用；如果fromStack的footprint
        // 摆在toAnchor时会与之重叠（两个跨格物品互换、新位置正好挨在一起的情况），这里会
        // 如实检测到冲突并拒绝——这是"双方新位置也不能互相重叠"这条规则的实际生效点。
        if (!canPlaceAt(toAnchor, fromDef.getCellWidth(), fromDef.getCellHeight())) {
            clearFootprintOwnedBy(fromAnchor);
            setSlot(fromAnchor, fromStack);
            setSlot(toAnchor, toStack);
            return false;
        }
        setSlot(toAnchor, fromStack);
        return true;
    }

    /**
     * 添加物品：1x1物品优先堆叠到已有同种未满槛位，剩余部分依次填入空槛位；
     * 跨格物品（cellWidth/cellHeight>1）不支持堆叠，逐个寻找能完整容纳其footprint的空位。
     * @param itemId 物品ID（必须已在itemRegistry注册）
     * @param count 要添加的数量，必须大于0
     * @return 实际未能放入（容器已满/放不下footprint）的剩余数量；0表示全部放入成功
     */
    public int addItem(String itemId, int count) {
        if (count < 1) {
            throw new IllegalArgumentException("count 必须至少为1");
        }
        ItemDef def = itemRegistry.getItemDef(itemId);
        if (def == null) {
            throw new IllegalArgumentException("未知物品ID: " + itemId);
        }

        if (def.isMultiCell()) {
            return addMultiCellItem(itemId, def, count);
        }
        return addSingleCellItem(itemId, def, count);
    }

    private int addSingleCellItem(String itemId, ItemDef def, int count) {
        int remaining = count;
        int maxStack = def.getMaxStackSize();

        // 第一遍：堆叠到已有同种未满槛位
        for (int i = 0; i < slots.size() && remaining > 0; i++) {
            ItemStack stack = slots.get(i);
            if (occupiedBy[i] == i && !stack.isEmpty() && stack.getItemId().equals(itemId) && stack.getCount() < maxStack) {
                int space = maxStack - stack.getCount();
                int added = Math.min(space, remaining);
                slots.set(i, stack.withCount(stack.getCount() + added));
                remaining -= added;
            }
        }

        // 第二遍：填入空槛位
        for (int i = 0; i < slots.size() && remaining > 0; i++) {
            if (occupiedBy[i] < 0) {
                int added = Math.min(maxStack, remaining);
                slots.set(i, new ItemStack(itemId, added));
                occupiedBy[i] = i;
                remaining -= added;
            }
        }

        return remaining;
    }

    private int addMultiCellItem(String itemId, ItemDef def, int count) {
        int remaining = count;
        int maxStack = def.getMaxStackSize();

        while (remaining > 0) {
            int anchor = findFreeAnchor(def.getCellWidth(), def.getCellHeight());
            if (anchor < 0) {
                break; // 放不下任何一个新footprint了
            }
            int placed = Math.min(maxStack, remaining);
            setSlot(anchor, new ItemStack(itemId, placed));
            remaining -= placed;
        }

        return remaining;
    }

    private int findFreeAnchor(int cellWidth, int cellHeight) {
        for (int i = 0; i < slots.size(); i++) {
            if (occupiedBy[i] < 0 && computeFootprint(i, cellWidth, cellHeight) != null) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 从指定格子所属物品的堆里移除数量（不足则移除全部并清空整个footprint）。
     * @return 实际移除的数量
     */
    public int removeFromSlot(int index, int count) {
        checkIndex(index);
        if (count < 1) {
            throw new IllegalArgumentException("count 必须至少为1");
        }
        int anchor = occupiedBy[index];
        if (anchor < 0) {
            return 0;
        }

        ItemStack stack = slots.get(anchor);
        int removed = Math.min(stack.getCount(), count);
        int remainingCount = stack.getCount() - removed;

        if (remainingCount > 0) {
            slots.set(anchor, stack.withCount(remainingCount));
        } else {
            clearFootprintOwnedBy(anchor);
        }
        return removed;
    }

    /**
     * 按物品ID移除数量（不指定具体格子）：依次从持有该物品的锚点格里扣除，直到扣满
     * count或该物品耗尽。用于"卸下装备时把对应物品从背包收走"这类场景——调用方只知道
     * 物品ID，不关心它具体存在哪个格子里。
     * @return 实际移除的数量
     */
    public int removeItem(String itemId, int count) {
        if (count < 1) {
            throw new IllegalArgumentException("count 必须至少为1");
        }
        int remaining = count;
        for (int i = 0; i < slots.size() && remaining > 0; i++) {
            ItemStack stack = slots.get(i);
            if (occupiedBy[i] == i && !stack.isEmpty() && stack.getItemId().equals(itemId)) {
                remaining -= removeFromSlot(i, remaining);
            }
        }
        return count - remaining;
    }

    /**
     * 统计某种物品在整个容器内的总数量（跨所有锚点格求和）。
     */
    public int countItem(String itemId) {
        int total = 0;
        for (int i = 0; i < slots.size(); i++) {
            ItemStack stack = slots.get(i);
            if (occupiedBy[i] == i && !stack.isEmpty() && stack.getItemId().equals(itemId)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    public boolean isFull() {
        for (int anchor : occupiedBy) {
            if (anchor < 0) {
                return false;
            }
        }
        return true;
    }

    private boolean isMultiCellFootprint(int index) {
        int anchor = occupiedBy[index];
        if (anchor < 0) {
            return false;
        }
        ItemStack stack = slots.get(anchor);
        if (stack.isEmpty()) {
            return false;
        }
        ItemDef def = itemRegistry.getItemDef(stack.getItemId());
        return def != null && def.isMultiCell();
    }

    /**
     * 清空以anchorIndex为锚点的整个footprint（物品数据+全部occupiedBy标记）。
     */
    private void clearFootprintOwnedBy(int anchorIndex) {
        for (int i = 0; i < occupiedBy.length; i++) {
            if (occupiedBy[i] == anchorIndex) {
                occupiedBy[i] = -1;
            }
        }
        slots.set(anchorIndex, ItemStack.EMPTY);
    }

    /**
     * 计算以anchorIndex为左上角、cellWidth x cellHeight的footprint覆盖的所有格子index。
     * 任一格超出行边界（会换行）、超出容器总大小、或已被其他物品占用，则返回null。
     */
    private int[] computeFootprint(int anchorIndex, int cellWidth, int cellHeight) {
        int[] footprint = computeFootprintBounded(anchorIndex, cellWidth, cellHeight);
        if (footprint == null) {
            return null;
        }
        for (int cellIndex : footprint) {
            // 允许的重叠只有：这个格子本来就是anchorIndex自身且尚未清空（setSlot调用前已清空，
            // 所以这里不会出现这种情况；保留严格校验，重叠即拒绝）
            if (occupiedBy[cellIndex] >= 0) {
                return null;
            }
        }
        return footprint;
    }

    /**
     * 计算footprint覆盖的格子index，只做边界检查（换行/总格数），不检查是否与其他物品重叠。
     * {@link #computeFootprint} 在此基础上追加重叠校验；{@link #previewFootprint} 直接使用
     * 这个不含重叠校验的版本，用于拖拽悬停预览。
     */
    private int[] computeFootprintBounded(int anchorIndex, int cellWidth, int cellHeight) {
        int anchorRow = anchorIndex / columns;
        int anchorCol = anchorIndex % columns;

        if (anchorCol + cellWidth > columns) {
            return null; // 会跨行越界（换到下一行左边），不允许
        }

        int[] footprint = new int[cellWidth * cellHeight];
        int cursor = 0;
        for (int dr = 0; dr < cellHeight; dr++) {
            for (int dc = 0; dc < cellWidth; dc++) {
                int cellIndex = (anchorRow + dr) * columns + (anchorCol + dc);
                if (cellIndex >= slots.size()) {
                    return null; // 超出容器总格数
                }
                footprint[cursor++] = cellIndex;
            }
        }
        return footprint;
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= slots.size()) {
            throw new IndexOutOfBoundsException("槛位索引必须在 0-" + (slots.size() - 1) + " 之间，实际: " + index);
        }
    }
}
