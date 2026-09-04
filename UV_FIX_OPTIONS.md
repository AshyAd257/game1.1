# UV显示问题修复选项

## 当前状态
- ✅ 贴图路径已修复（textures/armlegs/armlegs.png）
- ✅ UV坐标存在于glb文件中
- ✅ 纹理过滤设置为Nearest（像素艺术）
- ✅ 添加了调试日志

## 如果贴图显示仍然不对，尝试以下方案：

### 方案1: 在Blender中检查UV映射
1. 打开armlegmesh.glb
2. 进入UV Editing模式
3. 检查UV岛是否正确对齐到贴图
4. 重新导出，确保勾选 "Include" > "UVs" 和 "Materials"

### 方案2: 导出时包含材质信息
在Blender导出glTF时：
- 勾选 "Materials" > "Export"
- 勾选 "Images" > "Embedded" 或 "External"
- 这样jME可以自动使用正确的材质和UV映射

### 方案3: Y轴翻转修复（如果贴图上下颠倒）
在 SkeletalPlayerController.java 的 applyTextureRecursive 方法中添加：

```java
// 在设置材质之前，翻转UV的Y坐标
Mesh mesh = geom.getMesh();
VertexBuffer uvBuffer = mesh.getBuffer(VertexBuffer.Type.TexCoord);
if (uvBuffer != null) {
    FloatBuffer uvData = (FloatBuffer) uvBuffer.getData();
    uvData.rewind();
    for (int i = 0; i < uvData.limit(); i += 2) {
        float u = uvData.get(i);
        float v = uvData.get(i + 1);
        uvData.put(i, u);
        uvData.put(i + 1, 1.0f - v);  // 翻转Y轴
    }
    uvBuffer.updateData(uvData);
}
```

### 方案4: 检查Blender导出设置
确保导出选项：
- Format: glTF Binary (.glb)
- Transform: +Y Up (jME使用Y-up坐标系)
- Geometry: Apply Modifiers
- Geometry: UVs ✓
- Geometry: Normals ✓
- Material: Export materials ✓

### 方案5: 使用Unlit材质（排除光照影响）
如果光照导致颜色不对：

```java
Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
mat.setTexture("ColorMap", texture);
```

## 调试检查清单
运行游戏后检查日志：
- [ ] 贴图加载成功？
- [ ] 贴图尺寸正确（200x200）？
- [ ] 每个几何体都有UV坐标？
- [ ] UV坐标数量合理（24+ 顶点）？
- [ ] 没有 "No UV coordinates" 警告？

如果以上都正常但显示仍不对，截图发给我，我可以进一步诊断。
