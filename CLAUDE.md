# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

这是一个基于 jMonkeyEngine 3.5.2 的模块化体素游戏引擎，名为 Hecate。项目采用 Java 17 和 Maven 构建系统。

## 关键构建命令

```bash
# 编译项目
mvn compile

# 运行游戏
mvn exec:java -Dexec.mainClass="com.Hecate.Main"

# 打包成可执行 JAR
mvn package

# 清理构建文件
mvn clean
```

## 核心架构

### 模块系统 (Module System)
项目采用插件式模块架构，核心组件包括：

- **ModuleManager**: 管理模块的加载、依赖解析和生命周期
- **AbstractGameModule**: 所有模块的基类，提供标准生命周期方法
- **模块依赖系统**: 支持版本控制、冲突检测和能力系统
- **事件总线**: 模块间通信机制

### 世界系统 (World System)
体素世界渲染和管理：

- **ChunkManager**: 区块加载/卸载管理
- **Chunk**: 16x16x16 体素数据结构
- **TerrainGenerator**: 程序化地形生成
- **Block系统**: 方块类型、纹理和交互

### 资源管理
- **BlenderAssetLoader**: 加载 Blender 导出的 .obj 模型
- **BlockbenchAssetLoader**: 加载 Blockbench 模型格式
- **TextureManager**: 方块纹理管理和UV映射

### 物理系统
- **CollisionManager**: 碰撞检测管理
- **AABB**: 轴对齐包围盒碰撞检测
- **PointerSystem**: 射线投射和目标选择

### 玩家系统
- **PlayerController**: 第一人称相机控制
- **PlayerControlModule**: WASD移动、鼠标控制、方块交互

## 资源文件位置

- 3D模型: `src/main/resources/Models/blocks/`
- 纹理贴图: `src/main/resources/Textures/blocks/`
- 支持格式: .obj 模型文件, .png 纹理

## 代码风格约定

- 包名使用小写: com.Hecate.模块名
- 类名使用大驼峰命名
- 注释使用中文
- 模块遵循生命周期: onLoad() -> onInitialize() -> onPostInitialize() -> onUpdate() -> onDisable()

## 依赖关系

项目主要依赖：
- jMonkeyEngine 3.5.2 (核心引擎)
- jme3-bullet (物理引擎)
- Google Guava (工具库)
- SLF4J + Logback (日志)

模块间依赖通过 ModuleDependency 类明确声明，支持版本范围和可选依赖。

## 游戏控制

- WASD: 玩家移动
- 鼠标: 相机控制
- 左键: 破坏方块
- 右键: 放置方块
- 1234: 选择方块类型
- R键: 重置相机