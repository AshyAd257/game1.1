# 木偶编辑器与游戏本体隔离方案

## 当前问题

1. **直接依赖**: 21个文件直接导入了 `com.Hecate.puppet.editor` 包
2. **代码混合**: 编辑器专用类和游戏运行时类在同一包中
3. **构建混乱**: 编辑器代码会被打包到游戏JAR中

## 架构设计

### 1. 包结构重组

```
com.Hecate.puppet/
├── core/                    # 游戏运行时核心（游戏和编辑器共享）
│   ├── Bone.java           # 骨骼数据结构
│   ├── Skeleton.java       # 骨架系统
│   ├── PuppetPartRenderer.java  # 部件渲染器
│   └── PuppetRenderer.java      # 木偶渲染器
│
├── runtime/                 # 游戏运行时专用
│   ├── PuppetPlayerController.java  # 玩家木偶控制
│   └── PlanarShadow.java           # 平面阴影
│
├── animation/               # 动画系统（共享）
│   ├── AnimationClip.java
│   ├── AnimationPlayer.java
│   └── Keyframe.java
│
├── config/                  # 配置和IO（共享）
│   ├── PuppetIO.java
│   ├── PartConfig.java
│   └── AnimationIO.java
│
└── editor/                  # 编辑器专用（游戏不依赖）
    ├── PuppetEditorApp.java
    ├── PuppetEditorUI.java
    ├── InspectorPanel.java
    ├── core/                # 编辑器专用的核心扩展
    │   ├── EditorBone.java
    │   ├── EditorSkeleton.java
    │   └── EditorPuppetRenderer.java
    └── animation/
        └── EditorAnimationPlayer.java
```

### 2. 依赖关系

```
游戏本体 (Main.java)
    ↓ 直接依赖
puppet.core/*          # 核心渲染和数据结构
puppet.runtime/*       # 游戏运行时
puppet.animation/*     # 动画播放
puppet.config/*        # 加载木偶配置
    ↓ 反射加载（可选）
puppet.editor/*        # 编辑器（通过反射，不直接依赖）
```

### 3. Maven构建配置

#### 方案A: 单模块 + Maven Profile

```xml
<profiles>
    <!-- 游戏构建配置 -->
    <profile>
        <id>game</id>
        <activation>
            <activeByDefault>true</activeByDefault>
        </activation>
        <build>
            <plugins>
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-jar-plugin</artifactId>
                    <configuration>
                        <excludes>
                            <exclude>**/puppet/editor/**</exclude>
                        </excludes>
                    </configuration>
                </plugin>
            </plugins>
        </build>
    </profile>

    <!-- 编辑器构建配置 -->
    <profile>
        <id>editor</id>
        <build>
            <plugins>
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-jar-plugin</artifactId>
                    <configuration>
                        <archive>
                            <manifest>
                                <mainClass>com.Hecate.puppet.editor.PuppetEditorApp</mainClass>
                            </manifest>
                        </archive>
                    </configuration>
                </plugin>
            </plugins>
        </build>
    </profile>
</profiles>
```

#### 方案B: 多模块项目（推荐）

```
game1/
├── pom.xml                    # 父POM
├── puppet-core/               # 核心模块（共享）
│   ├── pom.xml
│   └── src/main/java/com/Hecate/puppet/
│       ├── core/
│       ├── animation/
│       └── config/
│
├── puppet-editor/             # 编辑器模块
│   ├── pom.xml
│   └── src/main/java/com/Hecate/puppet/editor/
│
└── game-main/                 # 游戏主模块
    ├── pom.xml
    └── src/main/java/com/Hecate/
```

### 4. 当前状态分析

#### 需要移动的文件

**核心类（移到 puppet.core）:**
- Bone.java
- Skeleton.java
- PuppetPartRenderer.java
- PuppetRenderer.java
- FreeBonePhysics.java

**运行时类（移到 puppet.runtime）:**
- PuppetPlayerController.java (在player包中)
- PlanarShadow.java

**编辑器专用（保持在 puppet.editor）:**
- 所有 editor/ 下的文件

#### 需要修复的导入

21个文件导入了编辑器包，需要检查：
- 是否真的需要编辑器功能
- 能否通过接口或反射解耦
- 是否应该移到编辑器包内

## 实施步骤

### 阶段1: 核心类分离（最小改动）

1. 创建 `puppet.core` 包
2. 移动核心类到 core 包
3. 更新所有导入语句
4. 测试游戏和编辑器是否正常运行

### 阶段2: Maven配置

1. 添加 Maven Profile 排除编辑器
2. 创建两个构建命令：
   - `mvn clean package -Pgame` - 构建游戏（不含编辑器）
   - `mvn clean package -Peditor` - 构建编辑器

### 阶段3: 验证隔离

1. 构建游戏JAR，检查是否包含编辑器类
2. 运行游戏，确保不加载编辑器类
3. 按I键测试编辑器反射加载

## 优势

1. **减小游戏体积**: 游戏JAR不包含编辑器代码
2. **加快启动速度**: 不加载不必要的类
3. **降低内存占用**: 编辑器类不会被加载到内存
4. **清晰的架构**: 明确区分运行时和开发工具
5. **独立发布**: 可以单独发布游戏和编辑器

## 兼容性

- 游戏通过反射加载编辑器，如果编辑器不存在也能正常运行
- 编辑器可以独立运行，也可以从游戏内启动
- 核心类被两者共享，保持一致性

## 建议

**推荐方案**: 先实施阶段1（核心类分离），然后阶段2（Maven配置）

这样可以：
- 最小化代码改动
- 保持现有功能
- 逐步实现隔离
- 易于回滚
