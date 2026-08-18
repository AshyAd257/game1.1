# 多语言系统使用说明

## 概述

游戏现在支持多语言本地化系统，所有游戏内文本统一管理在语言文件中。

## 文件结构

```
src/main/java/com/Hecate/localization/
  ├── Language.java           # 语言枚举
  └── Localization.java       # 本地化管理器

src/main/resources/localization/
  ├── en_US.properties        # 英文文本
  └── zh_CN.properties        # 简体中文文本
```

## 使用方法

### 1. 在代码中获取本地化文本

```java
import com.Hecate.localization.Localization;

// 简单文本
String title = Localization.get("buff.select.title");

// 带参数替换的文本（{0}, {1}, {2}...）
String waveText = Localization.get("wave.current", waveNumber);
String inkText = Localization.get("player.ink", inkPercentage);
```

### 2. 切换语言

```java
import com.Hecate.localization.Language;
import com.Hecate.localization.Localization;

// 切换到中文
Localization.setLanguage(Language.ZH_CN);

// 切换到英文
Localization.setLanguage(Language.EN_US);
```

### 3. 添加新的文本条目

在两个语言文件中同时添加相同的键名：

**en_US.properties:**
```properties
menu.start=Start Game
menu.settings=Settings
```

**zh_CN.properties:**
```properties
menu.start=开始游戏
menu.settings=设置
```

### 4. 添加新语言

1. 在 `Language.java` 中添加新的枚举值
2. 创建对应的 `.properties` 文件（如 `ja_JP.properties`）
3. 翻译所有文本条目

## 当前已本地化的内容

- ✅ Buff 选择界面
- ✅ 所有 Buff 类型名称和描述
- ✅ 波次系统文本（预留）
- ✅ 玩家状态文本（预留）

## 注意事项

1. `.properties` 文件使用 UTF-8 编码
2. 所有文本键名使用小写加点号分隔（如 `buff.fire_rate.name`）
3. 参数替换使用 `{0}`, `{1}`, `{2}` 等占位符
4. 默认语言是英文（`EN_US`）
5. 如果找不到对应的文本键，会返回键名本身
