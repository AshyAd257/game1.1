# Blender 模型导入 - 快速参考卡片

## 🚀 三种导入方法

### ⭐ 方法 1：最简单 - BlenderImportTool
```java
BlenderImportTool tool = new BlenderImportTool(assetManager);

// 单个文件
BlenderModel model = tool.importModel("C:/models/tree.obj", "oak_tree");

// 批量导入
int count = tool.importFolder("C:/models/", "prefix");
```

### 方法 2：标准 - BlenderAssetLoader
```java
BlenderAssetLoader loader = new BlenderAssetLoader(assetManager);

// 导入文件
File file = new File("C:/models/house.obj");
BlenderModel model = loader.importModelFile(file, "house");

// 批量导入
loader.importModelFolder(new File("C:/models/"), "building");
```

### 方法 3：底层 - BlenderImporter
```java
BlenderImporter importer = new BlenderImporter(assetManager);

// 获取详细结果
BlenderImporter.ImportResult result = 
    importer.importModel(new File("C:/model.obj"), "id", true);

if (result.isSuccess()) {
    // 使用 result.getLoadedSpatial()
}
```

## 📦 使用导入的模型

```java
// 从注册表获取
BlenderModelRegistry registry = BlenderModelRegistry.getInstance();
BlenderModel model = registry.getModel("oak_tree");

// 创建实例
Node instance = model.createInstance();
instance.setLocalTranslation(0, 0, 0);
rootNode.attachChild(instance);
```

## 🎨 支持的格式

**模型**: OBJ, FBX, GLTF, GLB, J3O  
**纹理**: PNG, JPG, TGA, BMP, DDS

## 🏷️ 纹理命名规范

| 类型 | 关键词 | 示例 |
|-----|--------|------|
| 漫反射 | diffuse, color, albedo | model_diffuse.png |
| 法线 | normal, norm | model_normal.png |
| 高光 | specular, spec | model_spec.png |

## 📁 文件位置

导入后自动复制到：
```
src/main/resources/
├── Models/Blender/     ← 模型
└── Textures/Blender/   ← 纹理
```

## 🛠️ 实用方法

```java
// 列出所有模型
List<String> models = tool.listImportedModels();

// 查看模型信息
String info = tool.getModelInfo("model_id");

// 打印使用帮助
BlenderImportTool.printUsage();
```

## ⚡ 快速测试

```bash
# 编译
mvn compile

# 运行示例
mvn exec:java -Dexec.mainClass="com.Hecate.blender.BlenderImportExample"
```

## 💡 常见问题

**Q: 模型是黑色的？**  
A: 添加光照
```java
DirectionalLight sun = new DirectionalLight();
sun.setDirection(new Vector3f(-0.5f, -0.5f, -0.5f));
rootNode.addLight(sun);
```

**Q: 纹理没显示？**  
A: 检查文件名包含关键词，或手动添加
```java
model.addTexture("DiffuseMap", "Textures/Blender/tex.png");
```

## 📚 完整文档

- **BLENDER_IMPORT_README.md** - 快速开始
- **docs/BLENDER_IMPORT_GUIDE.md** - 详细指南
- **BlenderImportExample.java** - 完整示例
