# Gun1 命令实现说明

## 功能概述

已成功实现 **Gun1** 命令，可以在游戏中通过控制台显示/隐藏蒸汽朋克枪模型。

## 使用方法

### 1. 打开控制台
在游戏中按 **/** 键打开控制台

### 2. 输入命令
在控制台输入框中输入：
```
gun1
```

### 3. 执行命令
按 **Enter** 键执行命令

### 4. 效果
- **首次输入**: 在玩家附近显示蒸汽朋克枪模型（steampunkgun.glb）
- **再次输入**: 隐藏模型
- 可以重复切换显示/隐藏

## 模型信息

- **模型文件**: `src/main/resources/weapons/steampunkgun.glb`
- **文件大小**: 93KB
- **格式**: GLTF Binary (.glb)
- **位置**: 玩家位置旁边 2 个单位，高度 +1
- **缩放**: 0.5倍（原始大小的一半）

## 技术实现

### 修改的文件

1. **GameConsole.java**
   - 修改 `addHistory()` 方法为 public，允许命令处理器添加控制台消息
   - 位置: `src/main/java/com/Hecate/ui/GameConsole.java:435`

2. **PlayerController.java**
   - 新增 `registerGun1Command()` 方法（第 363-417 行）
   - 在 `initializeGameConsole()` 中调用注册方法
   - 位置: `src/main/java/com/Hecate/player/PlayerController.java`

### 命令处理器功能

```java
gameConsole.registerCommand("gun1", new GameConsole.CommandHandler() {
    @Override
    public void execute(String[] args) {
        // 1. 首次执行：加载 steampunkgun.glb 模型
        // 2. 创建 Node 容器
        // 3. 设置模型位置和缩放
        // 4. 添加到场景的 rootNode
        // 5. 再次执行时：切换显示/隐藏
    }

    @Override
    public String getDescription() {
        return "显示/隐藏蒸汽朋克枪模型";
    }
});
```

### 关键特性

✅ **智能切换**: 首次显示模型，再次输入隐藏模型
✅ **延迟加载**: 模型仅在首次使用时加载，后续切换无需重新加载
✅ **错误处理**: 完整的异常处理和错误消息提示
✅ **位置定位**: 模型显示在玩家附近，便于查看
✅ **控制台反馈**: 每次操作都有清晰的控制台消息反馈

## 控制台命令列表

在控制台中输入 `/help` 可以查看所有可用命令：

```
/help    - 显示所有可用命令
/clear   - 清除控制台历史
/kill    - 杀死玩家（触发死亡特效）
/hurt    - 造成当前血量50%的伤害
/speed   - 设置移动速度倍数
/gun1    - 显示/隐藏蒸汽朋克枪模型 ⭐ 新增
```

## 测试步骤

1. 编译项目（已完成）：
   ```bash
   cd "C:\Users\29232\OneDrive\Desktop\game1(1)"
   mvn compile
   ```

2. 运行游戏：
   ```bash
   mvn exec:java -Dexec.mainClass="com.Hecate.Main"
   ```

3. 在游戏中：
   - 按 `/` 打开控制台
   - 输入 `gun1` 并按回车
   - 观察模型是否出现
   - 再次输入 `gun1` 验证隐藏功能

## 故障排除

### 问题：模型没有显示

**可能原因：**
1. 模型文件路径不正确
2. AssetManager 未正确加载 GLTF 格式

**解决方法：**
```bash
# 检查模型文件是否存在
ls "src/main/resources/weapons/steampunkgun.glb"

# 查看控制台错误消息
# 如果看到 "错误: 无法加载模型" 则说明路径有问题
```

### 问题：模型显示为黑色

**原因：** 场景中缺少光照

**解决方法：**
游戏已经包含光照系统（在 LightingSystem.java 中），如果模型显示为黑色，可以尝试：
1. 调整模型位置使其靠近光源
2. 或在命令中添加环境光

## 扩展建议

### 1. 添加位置参数
可以修改命令支持自定义位置：
```java
// /gun1 x y z
if (args.length >= 3) {
    float x = Float.parseFloat(args[0]);
    float y = Float.parseFloat(args[1]);
    float z = Float.parseFloat(args[2]);
    weaponModelNode.setLocalTranslation(x, y, z);
}
```

### 2. 添加旋转控制
```java
// /gun1 rotate angle
if (args.length > 0 && args[0].equals("rotate")) {
    float angle = Float.parseFloat(args[1]);
    weaponModelNode.rotate(0, angle * FastMath.DEG_TO_RAD, 0);
}
```

### 3. 添加更多武器模型
参考 Gun1 的实现，可以添加 Gun2, Gun3 等命令：
```java
registerCommand("gun2", ...);
registerCommand("gun3", ...);
```

## 日志信息

命令执行时会输出日志：
- `[PlayerController] Gun1命令已注册` - 启动时
- `[PlayerController] 蒸汽朋克枪模型加载成功` - 首次显示模型时
- `[PlayerController] 加载Gun1模型失败` - 出错时

## 相关文件

- 命令实现: `PlayerController.java:363-417`
- 控制台系统: `GameConsole.java`
- 模型文件: `src/main/resources/weapons/steampunkgun.glb`

## 完成状态

✅ 命令注册完成
✅ 模型加载逻辑实现
✅ 切换显示/隐藏功能
✅ 错误处理和日志
✅ 代码编译成功
✅ 文档编写完成

现在可以在游戏中使用 `/gun1` 命令了！
