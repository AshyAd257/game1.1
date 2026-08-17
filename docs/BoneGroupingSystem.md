# 骨骼分组和旋转系统 (Bone Grouping and Rotation System)

## 概述 (Overview)

骨骼分组系统允许将多个骨骼组合成一个逻辑单元，支持整体旋转和方向管理。该系统主要用于实现角色的转向逻辑，当角色转向时，其多方向贴图会根据旋转角度自动重映射到新的方向。

## 核心组件 (Core Components)

### 1. EditorBone - 编辑器骨骼类
**位置**: `src/main/java/com/Hecate/puppet/editor/core/EditorBone.java`

添加的分组相关字段和方法：
- `private String groupId` - 骨骼所属组的唯一ID
- `public String getGroupId()` - 获取组ID
- `public void setGroupId(String groupId)` - 设置组ID
- `public boolean hasGroup()` - 检查骨骼是否属于某个组

### 2. EditorBoneGroup - 骨骼组类
**位置**: `src/main/java/com/Hecate/puppet/editor/core/EditorBoneGroup.java`

**功能**:
- 管理一组骨骼的成员关系
- 记录当前旋转角度（0°, 90°, 180°, 270°）
- 提供旋转操作方法

**核心方法**:
```java
// 成员管理
public boolean addMember(EditorBone bone)      // 添加骨骼到组
public boolean removeMember(EditorBone bone)   // 从组移除骨骼
public boolean containsMember(EditorBone bone) // 检查骨骼是否在组中
public List<EditorBone> getMembers()           // 获取所有成员

// 旋转操作
public void rotateLeft90()   // 向左旋转90度（逆时针）
public void rotateRight90()  // 向右旋转90度（顺时针）
public void rotate180()      // 转身180度
public void resetRotation()  // 重置旋转到0度
```

### 3. EditorGroupManager - 分组管理器
**位置**: `src/main/java/com/Hecate/puppet/editor/core/EditorGroupManager.java`

**功能**:
- 统一管理编辑器中的所有骨骼组
- 维护骨骼与组的关联关系
- 提供组的CRUD操作

**核心方法**:
```java
// 组管理
public EditorBoneGroup createGroup(String name)              // 创建新组
public boolean deleteGroup(String groupId)                   // 删除组
public EditorBoneGroup getGroup(String groupId)              // 获取组
public EditorBoneGroup getGroupByName(String name)           // 根据名称查找组
public EditorBoneGroup getGroupOf(EditorBone bone)           // 获取骨骼所属的组
public List<EditorBoneGroup> getAllGroups()                  // 获取所有组

// 成员管理
public boolean addBoneToGroup(String groupId, EditorBone bone)  // 添加骨骼到组
public boolean removeBoneFromGroup(EditorBone bone)             // 从组移除骨骼
public int addBonesToGroup(String groupId, List<EditorBone> bones)  // 批量添加

// 维护操作
public boolean validateGroups()  // 验证组的完整性
public void repairGroups()       // 修复组的完整性
public void clearAllGroups()     // 清空所有组
```

### 4. DirectionRotation - 方向旋转工具类
**位置**: `src/main/java/com/Hecate/puppet/core/DirectionRotation.java`

**功能**:
处理骨骼旋转时的方向重映射逻辑。当骨骼组旋转时，各个方向的贴图和属性会重新映射到新的方向。

**方向映射规则**:

向左旋转90°（逆时针）:
- front → left（正面贴图移到左侧）
- left → back（左侧贴图移到背面）
- back → right（背面贴图移到右侧）
- right → front（右侧贴图移到正面）
- up/down 不变

向右旋转90°（顺时针）:
- front → right
- right → back
- back → left
- left → front
- up/down 不变

转身180°:
- front ↔ back
- left ↔ right
- up/down 不变

**核心方法**:
```java
// Bone版本（游戏运行时）
public static void rotateDirectionsLeft90(Bone bone)
public static void rotateDirectionsRight90(Bone bone)
public static void rotateDirections180(Bone bone)

// EditorBone版本（编辑器）
public static void rotateDirectionsLeft90(EditorBone bone)
public static void rotateDirectionsRight90(EditorBone bone)
public static void rotateDirections180(EditorBone bone)

// 辅助方法
public static String getRotatedDirection(String direction, int rotationType)
```

**重映射的属性**:
- 贴图路径 (directionTextures)
- UV坐标 (directionUVs)
- 渲染优先级 (directionPriorities)
- 尺寸（宽度/高度）(directionWidths/Heights)
- 位置偏移 (directionOffsets)
- 旋转角度 (directionRotations)
- 内容中心偏移 (directionContentCenters)
- 贴图旋转角度 (directionTextureRotations)
- 当前方向 (currentDirection)

### 5. GroupControlPanel - 分组控制UI面板
**位置**: `src/main/java/com/Hecate/puppet/editor/GroupControlPanel.java`

**功能**:
提供图形化界面用于管理骨骼分组

**UI组件**:
- 组名输入框（TextField）
- 创建组/删除组按钮
- 添加骨骼/移除骨骼按钮
- 三个旋转按钮：← 90°（左转）、↑ 180°（转身）、→ 90°（右转）
- 当前选中组显示
- 组列表显示

**事件回调接口**:
```java
public interface GroupActionListener {
    void onGroupCreated(EditorBoneGroup group);
    void onGroupDeleted(String groupId);
    void onBoneAddedToGroup(EditorBone bone, EditorBoneGroup group);
    void onBoneRemovedFromGroup(EditorBone bone, EditorBoneGroup group);
    void onGroupRotated(EditorBoneGroup group, int degrees);
}
```

### 6. EditorSkeleton - 骨骼树管理
**位置**: `src/main/java/com/Hecate/puppet/editor/core/EditorSkeleton.java`

添加的分组相关字段和方法：
- `private final EditorGroupManager groupManager` - 分组管理器
- `public EditorGroupManager getGroupManager()` - 获取分组管理器

在构造函数中自动创建分组管理器：
```java
public EditorSkeleton(String name) {
    this.name = name;
    this.boneMap = new HashMap<>();
    this.allBones = new ArrayList<>();
    this.groupManager = new EditorGroupManager(this);  // 创建分组管理器
}
```

## 使用流程 (Usage Workflow)

### 1. 创建骨骼组
```java
EditorSkeleton skeleton = ...;
EditorGroupManager manager = skeleton.getGroupManager();

// 创建新组
EditorBoneGroup headGroup = manager.createGroup("头部组");
```

### 2. 添加骨骼到组
```java
EditorBone headBone = skeleton.findBone("head");
EditorBone eyesBone = skeleton.findBone("eyes");

// 添加骨骼到组（自动设置骨骼的groupId）
manager.addBoneToGroup(headGroupId, headBone);
manager.addBoneToGroup(headGroupId, eyesBone);
```

### 3. 旋转骨骼组
```java
// 获取组
EditorBoneGroup group = manager.getGroup(headGroupId);

// 左转90度 - 所有成员的方向贴图会自动重映射
group.rotateLeft90();

// 右转90度
group.rotateRight90();

// 转身180度
group.rotate180();

// 重置到初始方向
group.resetRotation();
```

### 4. 在UI中使用
```java
// 在PuppetEditorUI中已集成
groupControlPanel.setSkeleton(skeleton);  // 设置骨骼系统
groupControlPanel.setSelectedBone(bone);  // 设置选中的骨骼
groupControlPanel.setVisible(true);       // 显示面板
```

## 数据持久化 (Data Persistence)

骨骼组信息会保存在木偶配置文件中（.puppet格式），包括：
- 组ID和组名
- 组成员列表
- 当前旋转角度

加载木偶时，分组信息会自动恢复，骨骼的groupId会正确设置。

## 设计模式和架构决策

### 编辑器/运行时分离
为了保持编辑器和游戏运行时的独立性，创建了两套平行的类：

**编辑器版本** (com.Hecate.puppet.editor.core):
- EditorBone
- EditorBoneGroup
- EditorGroupManager
- EditorSkeleton

**运行时版本** (com.Hecate.puppet.core):
- Bone
- BoneGroup (计划中)
- GroupManager (计划中)
- Skeleton

两套类具有相似的接口，但职责不同：
- 编辑器版本：支持编辑、修改、序列化
- 运行时版本：优化性能，只读或受限修改

### 方法重载 (Method Overloading)
DirectionRotation类使用方法重载来支持两种骨骼类型：
```java
// 游戏运行时版本
public static void rotateDirectionsLeft90(Bone bone) {
    applyDirectionMapping(bone, LEFT_90_MAP);
}

// 编辑器版本
public static void rotateDirectionsLeft90(EditorBone bone) {
    applyDirectionMappingEditor(bone, LEFT_90_MAP);
}
```

### UUID-based组管理
使用UUID作为组ID而不是组名，好处：
- 组名可以重命名而不影响引用
- 避免名称冲突
- 支持序列化和持久化

### 双向关系维护
骨骼和组之间的关系是双向的：
- 骨骼存储 groupId（快速查询所属组）
- 组存储成员列表（快速遍历所有成员）

EditorGroupManager负责维护这两个引用的一致性。

## 测试建议

创建测试木偶文件来验证功能：
1. 创建一个简单的木偶（如头+身体+手臂）
2. 为每个部件设置多方向贴图（front, back, left, right）
3. 创建组（如"头部组"包含头和眼睛）
4. 测试旋转操作，验证贴图是否正确重映射
5. 保存并重新加载，验证分组信息是否正确恢复

## 文件清单

### 核心类
- `src/main/java/com/Hecate/puppet/core/DirectionRotation.java` - 方向旋转工具（支持Bone和EditorBone）
- `src/main/java/com/Hecate/puppet/editor/core/EditorBone.java` - 编辑器骨骼（添加groupId字段和方法）
- `src/main/java/com/Hecate/puppet/editor/core/EditorBoneGroup.java` - 骨骼组类
- `src/main/java/com/Hecate/puppet/editor/core/EditorGroupManager.java` - 分组管理器
- `src/main/java/com/Hecate/puppet/editor/core/EditorSkeleton.java` - 骨骼树（添加groupManager字段）

### UI组件
- `src/main/java/com/Hecate/puppet/editor/GroupControlPanel.java` - 分组控制面板
- `src/main/java/com/Hecate/puppet/editor/PuppetEditorUI.java` - 主UI（集成GroupControlPanel）

### 编译顺序
```bash
# 1. 编译DirectionRotation
javac DirectionRotation.java

# 2. 编译EditorBone
javac EditorBone.java

# 3. 编译骨骼组相关类
javac EditorBoneGroup.java EditorGroupManager.java EditorSkeleton.java

# 4. 编译UI组件
javac GroupControlPanel.java PuppetEditorUI.java PuppetEditorApp.java
```

## 未来扩展

1. **动画支持**: 为组的旋转添加动画过渡效果
2. **批量操作**: 支持同时旋转多个组
3. **组嵌套**: 支持组内嵌套子组
4. **预设配置**: 保存和加载常用的分组配置
5. **可视化指示器**: 在3D视图中显示组的边界和旋转状态
