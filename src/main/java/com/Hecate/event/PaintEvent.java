package com.Hecate.event;

import com.jme3.math.Vector3f;

/**
 * 涂墨事件 - 子弹命中时发出，涂墨系统订阅
 *
 * 设计理念：
 * - 子弹不知道涂墨系统的存在，只负责发射事件
 * - 涂墨系统在别处订阅此事件，获取涂墨参数
 * - 彻底解耦：爆炸弹、弹跳弹、火焰弹都发射同一个事件
 *
 * 事件携带信息：
 * - position: 涂墨中心点（3D世界坐标）
 * - radius: 涂墨半径（米）
 * - teamId: 队伍ID（0=己方，1=敌方，用于颜色区分）
 * - intensity: 涂墨强度（0-1，影响饱和度/覆盖力）
 */
public class PaintEvent extends GameEvent {

    private final Vector3f position;   // 涂墨中心点
    private final float radius;        // 涂墨半径（米）
    private final int teamId;          // 队伍ID
    private final float intensity;     // 涂墨强度（0-1）

    /**
     * 构造函数
     * @param position 涂墨中心点（世界坐标）
     * @param radius 涂墨半径（米）
     * @param teamId 队伍ID（0=己方，1=敌方）
     * @param intensity 涂墨强度（0-1，1.0=完全覆盖）
     */
    public PaintEvent(Vector3f position, float radius, int teamId, float intensity) {
        super();
        this.position = position.clone();  // 克隆避免外部修改
        this.radius = radius;
        this.teamId = teamId;
        this.intensity = intensity;
    }

    // Getters
    public Vector3f getPosition() { return position.clone(); }
    public float getRadius() { return radius; }
    public int getTeamId() { return teamId; }
    public float getIntensity() { return intensity; }

    @Override
    public String toString() {
        return String.format("PaintEvent[pos=(%.2f,%.2f,%.2f), radius=%.2f, team=%d, intensity=%.2f]",
                position.x, position.y, position.z, radius, teamId, intensity);
    }
}
