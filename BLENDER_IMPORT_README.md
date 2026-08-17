# Blender 模型导入功能 - 使用说明

## ✨ 功能概述

game1(1) 项目现已支持从 Blender 导入模型和贴图！本功能提供了完整的模型导入系统，可以自动处理模型文件和相关纹理。

## 📦 已实现的功能

### ✅ 核心功能
1. **BlenderImporter** - 底层导入器
   - 支持 OBJ、FBX、GLTF、GLB、J3O 格式
   - 自动检测并复制纹理文件
   - 智能识别纹理类型（漫反射、法线、高光等）
   - 完整的错误处理和日志记录

2. **BlenderModel** - 增强的模型类
   - 支持多纹理映射
   - 自动材质创建和应用
   - 动画验证
   - 模型实例化

3. **BlenderAssetLoader** - 资源加载器
   - 集成导入功能
   - 模型注册管理
   - 批量导入支持

4. **BlenderImportTool** - 工具类
   - 简化的API接口
   - 命令行友好
   - 实用查询方法

5. **BlenderImportExample** - 完整示例
   - 可运行的示例程序
   - 详细的使用说明
   - 程序化API示例

## 📋 支持的格式

### 模型格式
- ✅ OBJ - Wavefront Object (.obj)
- ✅ FBX - Autodesk FBX (.fbx)
- ✅ GLTF - GL Transmission Format (.gltf)
- ✅ GLB - Binary GLTF (.glb)
- ✅ J3O - jMonkeyEngine Binary (.j3o)

### 纹理格式
- ✅ PNG (.png)
- ✅ JPG (.jpg, .jpeg)
- ✅ TGA (.tga)
- ✅ BMP (.bmp)
- ✅ DDS (.dds)

## 🚀 快速开始

### 方法 1：使用 BlenderImportTool（推荐）

```java
// 获取 AssetManager
AssetManager assetManager = app.getAssetManager();

// 创建导入工具
BlenderImportTool tool = new BlenderImportTool(assetManager);

// 导入单个模型
BlenderModel model = tool.importModel(
    "C:/path/to/your/model.obj",  // 模型文件路径
    "my_model"                      // 模型ID
);

// 使用模型
if (model != null) {
    Node instance = model.createInstance();
    rootNode.attachChild(instance);
}
```

### 方法 2：使用 BlenderAssetLoader

```java
BlenderAssetLoader loader = new BlenderAssetLoader(assetManager);

// 导入模型文件
File modelFile = new File("C:/models/tree.obj");
BlenderModel model = loader.importModelFile(modelFile, "oak_tree");

// 批量导入文件夹
int count = loader.importModelFolder(new File("C:/models/"), "imported");
System.out.println("成功导入 " + count + " 个模型");
```

### 方法 3：使用 BlenderImporter（底层API）

```java
BlenderImporter importer = new BlenderImporter(assetManager);

// 导入并获取详细结果
BlenderImporter.ImportResult result = importer.importModel(
    new File("C:/models/house.obj"),
    "house",
    true  // 自动检测纹理
);

if (result.isSuccess()) {
    System.out.println("成功: " + result.getModelPath());
    System.out.println("纹理: " + result.getTexturePaths().size() + " 个");
} else {
    System.err.println("失败: " + result.getErrorMessage());
}
```

## 📁 文件组织

导入后，文件会自动复制到：

```
src/main/resources/
├── Models/
│   └── Blender/
│       ├── my_model.obj
│       └── house.fbx
└── Textures/
    └── Blender/
        ├── my_model_diffuse.png
        ├── my_model_normal.png
        └── house_color.jpg
```

## 🎨 纹理自动识别

系统会根据文件名关键词自动识别纹理类型：

| 关键词 | 纹理类型 | 示例 |
|-------|---------|------|
| diffuse, color, albedo | DiffuseMap | model_diffuse.png |
| normal, norm | NormalMap | model_normal.png |
| specular, spec | SpecularMap | model_spec.png |
| metallic, metal | MetallicMap | model_metal.png |
| roughness, rough | RoughnessMap | model_rough.png |
| ao, ambient | LightMap | model_ao.png |
| emission, emissive | GlowMap | model_emission.png |
| alpha, opacity | AlphaMap | model_alpha.png |

## 📚 详细文档

完整的使用指南请查看：
- **docs/BLENDER_IMPORT_GUIDE.md** - 详细使用指南
- **BlenderImportExample.java** - 可运行的完整示例
- **BlenderImportTool.java** - API参考

## 🔧 编译和运行

### 编译项目
```bash
cd "C:\Users\29232\OneDrive\Desktop\game1(1)"
mvn compile
```

### 运行示例
```bash
mvn exec:java -Dexec.mainClass="com.Hecate.blender.BlenderImportExample"
```

### 查看使用说明
```bash
mvn exec:java -Dexec.mainClass="com.Hecate.blender.BlenderImportExample$ProgrammaticExample"
```

## 💡 使用示例

### 示例 1：导入单个模型
```java
BlenderImportTool tool = new BlenderImportTool(assetManager);
BlenderModel tree = tool.importModel("C:/models/tree.obj", "oak_tree");

if (tree != null) {
    Node treeNode = tree.createInstance();
    treeNode.setLocalTranslation(0, 0, 0);
    rootNode.attachChild(treeNode);
}
```

### 示例 2：批量导入
```java
int count = tool.importFolder("C:/models/buildings/", "building");
System.out.println("导入了 " + count + " 个建筑模型");
```

### 示例 3：查看已导入的模型
```java
List<String> models = tool.listImportedModels();
for (String modelInfo : models) {
    System.out.println(modelInfo);
}
```

### 示例 4：获取模型详情
```java
String info = tool.getModelInfo("oak_tree");
System.out.println(info);
```

## 🎯 最佳实践

1. **文件命名**：使用描述性名称，包含纹理类型关键词
   ```
   house_diffuse.png
   house_normal.png
   house_specular.png
   ```

2. **文件组织**：将模型和纹理放在同一目录
   ```
   models/
   ├── house.obj
   ├── house_diffuse.png
   └── house_normal.png
   ```

3. **格式选择**：
   - 简单模型：OBJ
   - 复杂模型：GLTF/GLB（推荐）
   - 优化性能：J3O

4. **纹理分辨率**：
   - 小型物体：512x512
   - 中型物体：1024x1024
   - 大型物体：2048x2048

## 🐛 故障排除

### 问题：模型显示为黑色
**原因**：没有光照

**解决**：
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

### 问题：纹理没有显示
**可能原因**：
1. 纹理文件名不符合规范
2. UV坐标缺失

**解决**：
```java
// 查看导入的纹理
BlenderImporter.ImportResult result = importer.importModel(...);
for (String key : result.getTexturePaths().keySet()) {
    System.out.println(key + " -> " + result.getTexturePaths().get(key));
}

// 手动添加纹理
model.addTexture("DiffuseMap", "Textures/Blender/my_texture.png");
```

## 📊 API 概览

### BlenderImporter
- `importModel(File, String, boolean)` - 导入模型
- `importModelFolder(File, String)` - 批量导入

### BlenderAssetLoader
- `importModelFile(File, String)` - 导入并注册
- `importModelFolder(File, String)` - 批量导入并注册

### BlenderImportTool
- `importModel(String, String)` - 从路径导入
- `importFolder(String, String)` - 批量导入
- `listImportedModels()` - 列出模型
- `getModelInfo(String)` - 获取模型信息

### BlenderModel
- `load(AssetManager)` - 加载模型
- `createInstance()` - 创建实例
- `addTexture(String, String)` - 添加纹理
- `setMaterialType(String)` - 设置材质

## 📝 注意事项

1. ⚠️ 导入的文件会被**复制**到项目的 resources 目录
2. ⚠️ 同名文件会被**覆盖**
3. ⚠️ 建议先备份原始文件
4. ✅ 支持相对路径和绝对路径
5. ✅ 自动创建必要的目录结构

## 🔗 相关文件

### 核心类
- `BlenderImporter.java` - 导入器实现 (src/main/java/com/Hecate/blender/)
- `BlenderModel.java` - 模型类（已增强）
- `BlenderAssetLoader.java` - 加载器（已增强）
- `BlenderImportTool.java` - 工具类

### 示例和文档
- `BlenderImportExample.java` - 完整示例
- `docs/BLENDER_IMPORT_GUIDE.md` - 详细指南
- `BLENDER_IMPORT_README.md` - 本文件

## 🎉 开始使用

1. 准备你的 Blender 模型文件（OBJ、FBX、GLTF 等）
2. 确保纹理文件与模型在同一目录
3. 使用上面的任一方法导入
4. 在场景中使用导入的模型

祝你使用愉快！如有问题，请查看详细文档或示例代码。
