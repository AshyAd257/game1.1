# 编译说明

## ⚠️ 重要提示

`InspectorPanel.java` 源文件已被重命名为 `InspectorPanel.java.readonly`，因为：
1. 源文件是**简化版**（只显示信息，没有控件）
2. 完整版在编译后的 `.class` 文件中（包含所有滑条和按钮）
3. JAR 文件使用的是完整版

## 🔧 如何编译其他文件

如果需要修改并编译 `PuppetEditorUI.java` 或 `PuppetEditorApp.java` 或其他文件：

```bash
# 编译单个文件（使用已有的 InspectorPanel.class）
"C:\Users\29232\.jdks\ms-17.0.16\bin\javac.exe" -encoding UTF-8 \
  -cp "target/classes;C:\Users\29232\.m2\repository\org\jmonkeyengine\jme3-core\3.5.2-stable\jme3-core-3.5.2-stable.jar;C:\Users\29232\.m2\repository\com\google\code\gson\gson\2.8.1\gson-2.8.1.jar" \
  -d target/classes \
  src/main/java/com/Hecate/puppet/editor/你的文件.java

# 更新 JAR
jar uf dist/PuppetEditor/PuppetEditor.jar -C target/classes com/Hecate/puppet/editor/你的类.class
```

## ❌ 不要做的事

**不要**重命名 `InspectorPanel.java.readonly` 回 `InspectorPanel.java` 并编译它！
这会用简化版覆盖完整版，导致所有控件消失。

## ✅ 当前布局

界面布局已更新为从左到右：
```
┌──────────┬────────────────┬──────────────┬────────────────┐
│ 部件列表 │  Inspector面板 │ 方向纹理面板 │   3D 视图区    │
│  300px   │    450px       │    300px     │   (剩余空间)   │
│          │  (所有控件)    │              │                │
└──────────┴────────────────┴──────────────┴────────────────┘
```

所有滑条和按钮现在都在左侧第二栏，不会被推到屏幕下方。

## 🚀 运行

直接运行 JAR 即可：
```bash
cd dist\PuppetEditor
PuppetEditor.bat
```
