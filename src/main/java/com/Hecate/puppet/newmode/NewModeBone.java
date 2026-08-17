package com.Hecate.puppet.newmode;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

/**
 * 新模式骨骼 - 八棱柱卡片系统
 *
 * 与旧模式不同：
 * - 不使用6方向贴图切换
 * - 使用8张卡片围成八棱柱
 * - 每张卡片对应一个角度段
 * - 每张卡片有独立的位置、旋转、UV参数
 */
public class NewModeBone {

    /**
     * 卡片数据 - 每张卡片的独立属性
     */
    public static class CardData {
        public String texturePath = null;
        public Vector3f localPosition = new Vector3f(0, 0, 0);  // 相对骨骼的位置偏移
        public float rotationX = 0f;  // 欧拉角 X (度)
        public float rotationY = 0f;  // 欧拉角 Y (度)
        public float rotationZ = 0f;  // 欧拉角 Z (度)

        // 卡片几何参数（每张卡片独立）
        public float width = 1.0f;       // 卡片宽度（原 Ring Radius 的概念）
        public float height = 2.0f;      // 卡片高度（原 Card Height）
        public float zOffset = 0f;       // Z轴离心值（相对八棱柱中心的距离）

        public Vector2f uvOffset = new Vector2f(0, 0);  // UV偏移
        public Vector2f uvScale = new Vector2f(1, 1);   // UV缩放

        public CardData() {}

        public CardData clone() {
            CardData copy = new CardData();
            copy.texturePath = this.texturePath;
            copy.localPosition = this.localPosition.clone();
            copy.rotationX = this.rotationX;
            copy.rotationY = this.rotationY;
            copy.rotationZ = this.rotationZ;
            copy.width = this.width;
            copy.height = this.height;
            copy.zOffset = this.zOffset;
            copy.uvOffset = this.uvOffset.clone();
            copy.uvScale = this.uvScale.clone();
            return copy;
        }
    }

    /**
     * 简单的2D向量（UV坐标用）
     */
    public static class Vector2f {
        public float x, y;

        public Vector2f(float x, float y) {
            this.x = x;
            this.y = y;
        }

        public Vector2f clone() {
            return new Vector2f(x, y);
        }
    }

    private final String name;
    private NewModeBone parent;

    // 骨骼变换（编辑器使用欧拉角存储，避免万向节锁问题）
    private final Vector3f localPosition = new Vector3f(0, 0, 0);
    private float rotationX = 0f;  // 欧拉角 X (度)
    private float rotationY = 0f;  // 欧拉角 Y (度)
    private float rotationZ = 0f;  // 欧拉角 Z (度)
    private final Vector3f localScale = new Vector3f(1, 1, 1);

    // 8张卡片的数据（每张卡片独立）
    private final CardData[] cards = new CardData[8];

    // 八棱柱环半径（用于自动布局，所有卡片共享）
    private float ringRadius = 0.5f;

    // 是否启用透视
    private boolean perspective = false;

    // 透视参数
    private float fov = 45f;
    private float cameraZ = 5f;

    public NewModeBone(String name) {
        this.name = name;
        // 初始化8张卡片
        for (int i = 0; i < 8; i++) {
            cards[i] = new CardData();
        }
    }

    // ========== 基础属性 ==========

    public String getName() {
        return name;
    }

    public NewModeBone getParent() {
        return parent;
    }

    public void setParent(NewModeBone parent) {
        this.parent = parent;
    }

    // ========== 变换 - 编辑器使用欧拉角 ==========

    public Vector3f getLocalPosition() {
        return localPosition.clone();
    }

    public void setLocalPosition(float x, float y, float z) {
        this.localPosition.set(x, y, z);
    }

    public void setLocalPosition(Vector3f pos) {
        this.localPosition.set(pos);
    }

    // 获取欧拉角旋转（度）
    public float getRotationX() {
        return rotationX;
    }

    public float getRotationY() {
        return rotationY;
    }

    public float getRotationZ() {
        return rotationZ;
    }

    // 设置欧拉角旋转（度）
    public void setRotationX(float degrees) {
        this.rotationX = degrees;
    }

    public void setRotationY(float degrees) {
        this.rotationY = degrees;
    }

    public void setRotationZ(float degrees) {
        this.rotationZ = degrees;
    }

    public void setRotation(float x, float y, float z) {
        this.rotationX = x;
        this.rotationY = y;
        this.rotationZ = z;
    }

    // 转换为 Quaternion（用于应用到场景图）
    public Quaternion getLocalRotation() {
        return new Quaternion().fromAngles(
            (float)Math.toRadians(rotationX),
            (float)Math.toRadians(rotationY),
            (float)Math.toRadians(rotationZ)
        );
    }

    // 从 Quaternion 设置（用于加载旧数据）
    public void setLocalRotation(Quaternion rot) {
        float[] angles = rot.toAngles(new float[3]);
        this.rotationX = (float)Math.toDegrees(angles[0]);
        this.rotationY = (float)Math.toDegrees(angles[1]);
        this.rotationZ = (float)Math.toDegrees(angles[2]);
    }

    public Vector3f getLocalScale() {
        return localScale.clone();
    }

    public void setLocalScale(float x, float y, float z) {
        this.localScale.set(x, y, z);
    }

    /**
     * 计算世界变换
     */
    public void getWorldTransform(Vector3f outPos, Quaternion outRot, Vector3f outScale) {
        if (parent == null) {
            outPos.set(localPosition);
            outRot.set(getLocalRotation());  // 使用方法而不是直接访问字段
            outScale.set(localScale);
        } else {
            Vector3f parentPos = new Vector3f();
            Quaternion parentRot = new Quaternion();
            Vector3f parentScale = new Vector3f();
            parent.getWorldTransform(parentPos, parentRot, parentScale);

            // 位置 = 父位置 + 父旋转 * (父缩放 * 本地位置)
            Vector3f scaledLocal = localPosition.mult(parentScale);
            Vector3f rotatedLocal = parentRot.mult(scaledLocal);
            outPos.set(parentPos).addLocal(rotatedLocal);

            // 旋转 = 父旋转 * 本地旋转
            outRot.set(parentRot).multLocal(getLocalRotation());  // 使用方法而不是直接访问字段

            // 缩放 = 父缩放 * 本地缩放
            outScale.set(parentScale).multLocal(localScale);
        }
    }

    // ========== 卡片数据访问 ==========

    /**
     * 获取卡片数据
     * @param index 卡片索引 (0-7)
     */
    public CardData getCard(int index) {
        if (index >= 0 && index < 8) {
            return cards[index];
        }
        return null;
    }

    /**
     * 设置指定卡片的贴图
     * @param index 卡片索引 (0-7)
     * @param texturePath 贴图路径
     */
    public void setCardTexture(int index, String texturePath) {
        if (index >= 0 && index < 8) {
            cards[index].texturePath = texturePath;
        }
    }

    /**
     * 获取指定卡片的贴图
     * @param index 卡片索引 (0-7)
     * @return 贴图路径，可能为null
     */
    public String getCardTexture(int index) {
        if (index >= 0 && index < 8) {
            return cards[index].texturePath;
        }
        return null;
    }

    /**
     * 获取所有卡片贴图
     */
    public String[] getAllCardTextures() {
        String[] textures = new String[8];
        for (int i = 0; i < 8; i++) {
            textures[i] = cards[i].texturePath;
        }
        return textures;
    }

    // ========== 八棱柱布局参数（骨骼级别，所有卡片共享）==========

    public float getRingRadius() {
        return ringRadius;
    }

    public void setRingRadius(float ringRadius) {
        this.ringRadius = ringRadius;
    }

    // ========== 透视参数 ==========

    public boolean isPerspective() {
        return perspective;
    }

    public void setPerspective(boolean perspective) {
        this.perspective = perspective;
    }

    public float getFov() {
        return fov;
    }

    public void setFov(float fov) {
        this.fov = fov;
    }

    public float getCameraZ() {
        return cameraZ;
    }

    public void setCameraZ(float cameraZ) {
        this.cameraZ = cameraZ;
    }

    @Override
    public String toString() {
        return "NewModeBone{" +
                "name='" + name + '\'' +
                ", parent=" + (parent != null ? parent.getName() : "null") +
                ", position=" + localPosition +
                ", ringRadius=" + ringRadius +
                '}';
    }
}
