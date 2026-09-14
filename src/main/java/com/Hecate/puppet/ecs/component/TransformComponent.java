package com.Hecate.puppet.ecs.component;

import com.Hecate.puppet.ecs.Component;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

/**
 * 变换组件
 * 存储实体的位置、旋转、缩放信息
 *
 * 【性能优化】包含世界变换缓存和脏标记系统
 * 解决原Bone类每次调用getWorldTransform()都递归计算的性能问题
 */
public class TransformComponent implements Component {

    // ========== Local Transform ==========
    private Vector3f localPosition;
    private Quaternion localRotation;
    private Vector3f localScale;

    // ========== Rest Pose（初始姿势） ==========
    private Vector3f restPosition;
    private Quaternion restRotation;
    private Vector3f restScale;

    // ========== Cached World Transform ==========
    private Vector3f cachedWorldPosition;
    private Quaternion cachedWorldRotation;
    private Vector3f cachedWorldScale;

    // ========== Dirty Flag（脏标记） ==========
    private boolean worldTransformDirty;

    // 启用状态
    private boolean enabled;

    /**
     * 构造函数 - 创建默认变换
     */
    public TransformComponent() {
        this(new Vector3f(0, 0, 0), new Quaternion(), new Vector3f(1, 1, 1));
    }

    /**
     * 构造函数 - 指定初始变换
     *
     * @param position 初始位置
     * @param rotation 初始旋转
     * @param scale 初始缩放
     */
    public TransformComponent(Vector3f position, Quaternion rotation, Vector3f scale) {
        // Local transform
        this.localPosition = position.clone();
        this.localRotation = rotation.clone();
        this.localScale = scale.clone();

        // Rest pose（初始化为与local相同）
        this.restPosition = position.clone();
        this.restRotation = rotation.clone();
        this.restScale = scale.clone();

        // Cached world transform
        this.cachedWorldPosition = new Vector3f();
        this.cachedWorldRotation = new Quaternion();
        this.cachedWorldScale = new Vector3f();

        this.worldTransformDirty = true;
        this.enabled = true;
    }

    /**
     * 标记世界变换为脏
     * 当local transform改变时调用
     */
    public void markDirty() {
        this.worldTransformDirty = true;
    }

    /**
     * 检查世界变换是否为脏
     *
     * @return true如果需要重新计算世界变换
     */
    public boolean isDirty() {
        return worldTransformDirty;
    }

    /**
     * 重置到Rest姿势
     */
    public void resetToRestPose() {
        this.localPosition.set(restPosition);
        this.localRotation.set(restRotation);
        this.localScale.set(restScale);
        markDirty();
    }

    // ========== Local Transform Getters/Setters ==========

    public Vector3f getLocalPosition() {
        return localPosition;
    }

    public void setLocalPosition(Vector3f position) {
        this.localPosition.set(position);
        markDirty();
    }

    public void setLocalPosition(float x, float y, float z) {
        this.localPosition.set(x, y, z);
        markDirty();
    }

    public Quaternion getLocalRotation() {
        return localRotation;
    }

    public void setLocalRotation(Quaternion rotation) {
        this.localRotation.set(rotation);
        markDirty();
    }

    public Vector3f getLocalScale() {
        return localScale;
    }

    public void setLocalScale(Vector3f scale) {
        this.localScale.set(scale);
        markDirty();
    }

    public void setLocalScale(float x, float y, float z) {
        this.localScale.set(x, y, z);
        markDirty();
    }

    // ========== Rest Pose Getters/Setters ==========

    public Vector3f getRestPosition() {
        return restPosition;
    }

    public void setRestPosition(Vector3f position) {
        this.restPosition.set(position);
    }

    public Quaternion getRestRotation() {
        return restRotation;
    }

    public void setRestRotation(Quaternion rotation) {
        this.restRotation.set(rotation);
    }

    public Vector3f getRestScale() {
        return restScale;
    }

    public void setRestScale(Vector3f scale) {
        this.restScale.set(scale);
    }

    // ========== Cached World Transform Getters/Setters ==========

    /**
     * 获取缓存的世界位置
     * 注意：调用者需要确保缓存是最新的（通过TransformSystem更新）
     *
     * @return 世界位置（只读）
     */
    public Vector3f getCachedWorldPosition() {
        return cachedWorldPosition;
    }

    /**
     * 获取缓存的世界旋转
     *
     * @return 世界旋转（只读）
     */
    public Quaternion getCachedWorldRotation() {
        return cachedWorldRotation;
    }

    /**
     * 获取缓存的世界缩放
     *
     * @return 世界缩放（只读）
     */
    public Vector3f getCachedWorldScale() {
        return cachedWorldScale;
    }

    /**
     * 设置缓存的世界变换
     * 由TransformSystem调用
     *
     * @param position 世界位置
     * @param rotation 世界旋转
     * @param scale 世界缩放
     */
    public void setCachedWorldTransform(Vector3f position, Quaternion rotation, Vector3f scale) {
        this.cachedWorldPosition.set(position);
        this.cachedWorldRotation.set(rotation);
        this.cachedWorldScale.set(scale);
        this.worldTransformDirty = false;
    }

    // ========== Component Interface ==========

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public Component clone() {
        TransformComponent cloned = new TransformComponent(
            localPosition.clone(),
            localRotation.clone(),
            localScale.clone()
        );
        cloned.setRestPosition(restPosition.clone());
        cloned.setRestRotation(restRotation.clone());
        cloned.setRestScale(restScale.clone());
        return cloned;
    }

    @Override
    public String toString() {
        return String.format("TransformComponent[pos=(%.2f,%.2f,%.2f), dirty=%s]",
                localPosition.x, localPosition.y, localPosition.z, worldTransformDirty);
    }
}
