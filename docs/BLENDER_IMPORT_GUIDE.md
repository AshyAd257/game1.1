# Blender 模型导入指南

## 概述

game1(1) 项目现在支持从 Blender 导入模型文件，包括模型和贴图的自动处理。

## 支持的格式

### 模型格式
- **OBJ** - Wavefront Object (.obj)
- **FBX** - Autodesk FBX (.fbx)
- **GLTF** - GL Transmission Format (.gltf)
- **GLB** - Binary GLTF (.glb)
- **J3O** - jMonkeyEngine Binary (.j3o)

### 纹理格式
- **PNG** (.png)
- **JPG** (.jpg, .jpeg)
- **TGA** (.tga)
- **BMP** (.bmp)
- **DDS** (.dds)

## 快速开始

### 1. 基本导入

```java
// 获取 AssetManager（从你的应用中）
AssetManager assetManager = app.getAssetManager();

// 创建导入工具
BlenderImportTool tool = new BlenderImportTool(assetManager);

// 导入单个模型
BlenderModel model = tool.importModel(
    "C:/path/to/your/model.obj",  // 源文件路径
    "my_model"                      // 模型ID
);

// 使用模型
if (model != null && model.isLoaded()) {
    Node instance = model.createInstance();
    rootNode.attachChild(instance);
}
```

### 2. 批量导入

```java
// 批量导入整个文件夹
int count = tool.importFolder(
    "C:/path/to/models/",  // 文件夹路径
    "imported"             // 模型ID前缀
);

System.out.println("成功导入 " + count + " 个模型");
```

### 3. 从注册表获取模型

```java
// 获取已导入的模型
BlenderModelRegistry registry = BlenderModelRegistry.getInstance();
BlenderModel model = registry.getModel("my_model");

if (model != null) {
    Node instance = model.createInstance();
    rootNode.attachChild(instance);
}
```

## 高级用法

### 1. 使用 BlenderAssetLoader

```java
BlenderAssetLoader loader = new BlenderAssetLoader(assetManager);

// 导入模型（自动检测纹理）
File modelFile = new File("C:/models/house.obj");
BlenderModel model = loader.importModelFile(modelFile, "house");

// 导入模型（手动控制纹理检测）
BlenderModel model2 = loader.importModelFile(
    modelFile,
    "house2",
    false  // 不自动检测纹理
);
```

### 2. 使用 BlenderImporter（底层API）

```java
BlenderImporter importer = new BlenderImporter(assetManager);

// 导入模型并获取详细结果
BlenderImporter.ImportResult result = importer.importModel(
    new File("C:/models/tree.obj"),
    "oak_tree",
    true  // 自动检测纹理
);

if (result.isSuccess()) {
    System.out.println("模型路径: " + result.getModelPath());
    System.out.println("纹理数量: " + result.getTexturePaths().size());

    // 纹理信息
    for (Map.Entry<String, String> entry : result.getTexturePaths().entrySet()) {
        System.out.println(entry.getKey() + ": " + entry.getValue());
    }
} else {
    System.err.println("导入失败: " + result.getErrorMessage());
}
```

### 3. 手动添加纹理

```java
BlenderModel model = new BlenderModel("my_model", "Models/Blender/my_model.obj");

// 添加纹理
model.addTexture("DiffuseMap", "Textures/Blender/my_model_diffuse.png");
model.addTexture("NormalMap", "Textures/Blender/my_model_normal.png");
model.addTexture("SpecularMap", "Textures/Blender/my_model_specular.png");

// 设置材质类型
model.setMaterialType("Common/MatDefs/Light/Lighting.j3md");

// 加载模型
if (model.load(assetManager)) {
    // 注册模型
    BlenderModelRegistry.getInstance().registerModel(model);
}
```

## 纹理自动检测

导入器会自动检测并复制模型文件同目录下的纹理文件，并根据文件名推测纹理类型：

| 文件名关键词 | 纹理类型 |
|------------|---------|
| diffuse, color, albedo | DiffuseMap |
| normal, norm | NormalMap |
| specular, spec | SpecularMap |
| metallic, metal | MetallicMap |
| roughness, rough | RoughnessMap |
| ao, ambient | LightMap |
| emission, emissive | GlowMap |
| alpha, opacity | AlphaMap |
| 其他 | DiffuseMap (默认) |

### 推荐的纹理命名规范

```
my_model_diffuse.png
my_model_normal.png
my_model_specular.png
my_model_roughness.png
```

## 文件组织

导入后的文件会自动复制到项目的资源目录：

```
src/main/resources/
├── Models/
│   └── Blender/
│       ├── my_model.obj
│       ├── house.fbx
│       └── tree.gltf
└── Textures/
    └── Blender/
        ├── my_model_diffuse.png
        ├── my_model_normal.png
        ├── house_color.jpg
        └── tree_albedo.png
```

## 实用工具方法

### 列出已导入的模型

```java
BlenderImportTool tool = new BlenderImportTool(assetManager);
List<String> models = tool.listImportedModels();

for (String modelInfo : models) {
    System.out.println(modelInfo);
}
```

### 查看模型详细信息

```java
String info = tool.getModelInfo("my_model");
System.out.println(info);
```

输出示例：
```
=== 模型信息 ===
ID: my_model
路径: Models/Blender/my_model.obj
已加载: 是
动画数量: 3
  - idle
  - walk
  - run
```

### 获取资源目录信息

```java
BlenderImportTool.ResourceInfo info = new BlenderImportTool.ResourceInfo();
System.out.println(info);
```

## 完整示例

查看示例代码：
- `BlenderImportExample.java` - 完整的游戏示例
- `BlenderImportExample.ProgrammaticExample` - 程序化使用示例

运行示例：
```bash
# 编译
mvn compile

# 运行示例
mvn exec:java -Dexec.mainClass="com.Hecate.blender.BlenderImportExample"
```

## 故障排除

### 问题：模型导入失败

**可能原因：**
1. 文件路径不正确
2. 文件格式不支持
3. 模型文件损坏

**解决方法：**
```java
// 检查文件是否存在
File file = new File("C:/models/model.obj");
if (!file.exists()) {
    System.err.println("文件不存在: " + file.getAbsolutePath());
}

// 查看详细错误信息
BlenderImporter.ImportResult result = importer.importModel(file, "model", true);
if (!result.isSuccess()) {
    System.err.println("导入失败: " + result.getErrorMessage());
}
```

### 问题：纹理没有正确应用

**可能原因：**
1. 纹理文件名不符合规范
2. 材质定义不正确
3. UV坐标问题

**解决方法：**
```java
// 手动检查纹理路径
Map<String, String> textures = result.getTexturePaths();
for (String key : textures.keySet()) {
    System.out.println(key + " -> " + textures.get(key));
}

// 手动应用纹理
model.addTexture("DiffuseMap", "Textures/Blender/my_texture.png");
```

### 问题：模型显示为黑色

**原因：** 场景中没有光照

**解决方法：**
```java
// 添加方向光
DirectionalLight sun = new DirectionalLight();
sun.setDirection(new Vector3f(-0.5f, -0.5f, -0.5f).normalizeLocal());
sun.setColor(ColorRGBA.White);
rootNode.addLight(sun);

// 添加环境光
AmbientLight ambient = new AmbientLight();
ambient.setColor(ColorRGBA.White.mult(0.3f));
rootNode.addLight(ambient);
```

## API 参考

### BlenderImporter
- `importModel(File, String, boolean)` - 导入单个模型
- `importModelFolder(File, String)` - 批量导入文件夹

### BlenderAssetLoader
- `importModelFile(File, String)` - 导入并注册模型
- `importModelFile(File, String, boolean)` - 导入并注册（控制纹理检测）
- `importModelFolder(File, String)` - 批量导入并注册

### BlenderModel
- `load(AssetManager)` - 加载模型
- `createInstance()` - 创建模型实例
- `addTexture(String, String)` - 添加纹理
- `setMaterialType(String)` - 设置材质类型

### BlenderImportTool
- `importModel(String, String)` - 从路径导入模型
- `importFolder(String, String)` - 批量导入文件夹
- `listImportedModels()` - 列出已导入的模型
- `getModelInfo(String)` - 获取模型详细信息

## 最佳实践

1. **命名规范**：使用清晰的命名，包含纹理类型关键词
2. **文件组织**：将模型和纹理放在同一目录
3. **格式选择**：推荐使用 GLTF/GLB 格式（支持完整的材质信息）
4. **纹理优化**：使用合适的纹理分辨率（推荐 1024x1024 或 2048x2048）
5. **测试加载**：导入后立即测试模型是否正确加载

## 参考资料

- [jMonkeyEngine 文档](https://wiki.jmonkeyengine.org/)
- [Blender 导出设置](https://docs.blender.org/manual/en/latest/addons/import_export/)
- [GLTF 格式规范](https://www.khronos.org/gltf/)
