package com.Hecate.weapon;

import com.Hecate.ink.SparseGridManager;
import com.Hecate.monster.MonsterManager;
import com.Hecate.physics.CollisionManager;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.jme3.texture.Texture;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 子弹更新循环：驱动 {@link Projectile} 的飞行、命中判定、穿透、沿途涂墨、地形碰撞，
 * 并用一个方块（贴图非空时贴图，否则回退到纯色）作为子弹的视觉表现。
 * <p>Projectile/ProjectileProfile 系统此前只有测试代码在用（穿透、沿途涂墨等
 * 字段都是预留但从未被实际tick过），这个类是第一个把它接入实际玩法的地方。
 * <p>命中判定复用 MonsterManager.checkHit（与 FlameParticle 命中怪物走的是同一套
 * 去重逻辑），而不是走 Projectile.hit() 内部逐一分发——Projectile 本身不知道
 * "怪物"这个概念，只负责穿透计数和飞行状态。
 * <p>地形碰撞同理复用 CollisionManager.getTerrainHeightAt（与 FlameParticle.
 * findGroundSurface 同样的"沿路径步进采样"思路）——与怪物命中不同，地形不参与
 * 穿透判定，扫到地面/墙立即终止飞行。
 */
public class ProjectileManager {

    private final AssetManager assetManager;
    private final Node worldNode;
    private final MonsterManager monsterManager;
    private final SparseGridManager gridManager;
    private final CollisionManager collisionManager;

    private final List<ActiveProjectile> active = new ArrayList<>();

    // 每一发子弹独立的shotId计数器（用于MonsterManager.checkHit的伤害去重，
    // 与FlameParticleSystem.burst()的shotId用法一致：同一shotId在同一个怪物身上只计一次伤害，
    // 但穿透子弹需要能对不同怪物分别计伤，所以每发子弹一个新shotId，而不是每次命中一个新shotId）
    private static long nextShotId = 1_000_000_000L; // 与FlameParticleSystem的shotId空间区分，避免万一混用时冲突

    // 沿弹道线段采样检测地形的步长，与MonsterManager.checkHit的采样步长保持一致
    private static final float TERRAIN_STEP_SIZE = 0.1f;

    public ProjectileManager(AssetManager assetManager, Node worldNode,
                              MonsterManager monsterManager, SparseGridManager gridManager,
                              CollisionManager collisionManager) {
        this.assetManager = assetManager;
        this.worldNode = worldNode;
        this.monsterManager = monsterManager;
        this.gridManager = gridManager;
        this.collisionManager = collisionManager;
    }

    /**
     * 生成一发子弹并接管其生命周期。
     */
    public void spawn(Projectile projectile) {
        Geometry visual = createVisual(projectile.getProfile().getVisualConfig());
        visual.setLocalTranslation(projectile.getPosition());
        worldNode.attachChild(visual);

        long shotId = nextShotId++;
        active.add(new ActiveProjectile(projectile, visual, shotId));
    }

    /**
     * 每帧调用：更新所有活跃子弹的位置、命中判定、沿途涂墨，并清理已消失的子弹。
     */
    public void update(float tpf) {
        Iterator<ActiveProjectile> it = active.iterator();
        while (it.hasNext()) {
            ActiveProjectile ap = it.next();
            Projectile projectile = ap.projectile;

            if (!projectile.isAlive()) {
                detach(ap);
                it.remove();
                continue;
            }

            Vector3f oldPos = projectile.getPosition();
            projectile.update(tpf);
            Vector3f newPos = projectile.getPosition();

            ap.visual.setLocalTranslation(newPos);

            // 沿途涂墨：不走Projectile内部的PaintEvent/EventBus（那条链没有任何订阅者），
            // 直接复用FlameParticle落地涂墨时用的同一个SparseGridManager.inkCircle方法，
            // 按profile配置的间隔在这里自行计时。
            if (projectile.getProfile().isPaintAlongPath() && gridManager != null) {
                ap.pathPaintTimer += tpf;
                float interval = projectile.getProfile().getPathPaintInterval();
                if (interval > 0f && ap.pathPaintTimer >= interval) {
                    ap.pathPaintTimer = 0f;
                    float radius = projectile.getProfile().getHitEffect().inkRadius;
                    gridManager.inkCircle(newPos, radius, projectile.getTeamId());
                }
            }

            // 命中判定：复用MonsterManager现成的扫描线段命中检测
            if (monsterManager != null) {
                float damage = projectile.getProfile().getHitEffect().damage;
                Vector3f hitPos = monsterManager.checkHit(oldPos, newPos, damage, ap.shotId);
                if (hitPos != null) {
                    projectile.hit(hitPos);
                    if (!projectile.isAlive()) {
                        detach(ap);
                        it.remove();
                        continue;
                    }
                }
            }

            // 地形碰撞：与怪物命中不同，地形不参与穿透判定，只要沿途扫到地面/墙就立即终止。
            // 与FlameParticle.findGroundSurface()同样的"沿路径步进采样地形高度"思路。
            if (collisionManager != null) {
                Vector3f groundHit = findTerrainHit(oldPos, newPos);
                if (groundHit != null) {
                    projectile.terrainHit(groundHit);
                    ap.visual.setLocalTranslation(groundHit);
                    if (projectile.getProfile().getExpireEffect().dropToGround && gridManager != null) {
                        float radius = projectile.getProfile().getHitEffect().inkRadius;
                        gridManager.inkCircle(groundHit, radius, projectile.getTeamId());
                    }
                    detach(ap);
                    it.remove();
                    continue;
                }
            }

            if (!projectile.isAlive()) {
                detach(ap);
                it.remove();
            }
        }
    }

    /**
     * 沿oldPos到newPos的线段采样检测地形表面，找到子弹本帧是否穿过了地面/墙。
     */
    private Vector3f findTerrainHit(Vector3f oldPos, Vector3f newPos) {
        Vector3f direction = newPos.subtract(oldPos);
        float distance = direction.length();

        if (distance < 0.0001f) {
            return checkTerrainAtPoint(newPos);
        }

        direction.normalizeLocal();

        int steps = (int) Math.ceil(distance / TERRAIN_STEP_SIZE);
        for (int i = 0; i <= steps; i++) {
            float t = Math.min(i * TERRAIN_STEP_SIZE, distance);
            Vector3f checkPos = oldPos.add(direction.mult(t));
            Vector3f hit = checkTerrainAtPoint(checkPos);
            if (hit != null) {
                return hit;
            }
        }

        return null;
    }

    private Vector3f checkTerrainAtPoint(Vector3f point) {
        float terrainHeight = collisionManager.getTerrainHeightAt(point.x, point.z);
        if (!Float.isNaN(terrainHeight) && point.y <= terrainHeight) {
            return new Vector3f(point.x, terrainHeight, point.z);
        }
        return null;
    }

    /**
     * 清空所有子弹（例如武器卸下、世界切换时调用）。
     */
    public void clear() {
        for (ActiveProjectile ap : active) {
            detach(ap);
        }
        active.clear();
    }

    private void detach(ActiveProjectile ap) {
        if (ap.visual.getParent() != null) {
            ap.visual.removeFromParent();
        }
    }

    /**
     * 用方块作为子弹的视觉表现：texturePath非null时贴图，否则回退到纯色
     * （VisualConfig注释里说明的默认行为）。
     */
    private Geometry createVisual(ProjectileProfile.VisualConfig config) {
        float scale = config.scale;
        Box box = new Box(scale / 2f, scale / 2f, scale / 2f);
        Geometry geom = new Geometry("ProjectileBullet", box);

        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        if (config.texturePath != null) {
            Texture texture = assetManager.loadTexture(config.texturePath);
            mat.setTexture("ColorMap", texture);
        } else {
            Vector3f color = config.color;
            mat.setColor("Color", new ColorRGBA(color.x, color.y, color.z, 1.0f));
        }
        geom.setMaterial(mat);

        return geom;
    }

    /**
     * 一发正在飞行的子弹及其视觉表现、伤害去重shotId、沿途涂墨计时器。
     */
    private static class ActiveProjectile {
        final Projectile projectile;
        final Geometry visual;
        final long shotId;
        float pathPaintTimer = 0f;

        ActiveProjectile(Projectile projectile, Geometry visual, long shotId) {
            this.projectile = projectile;
            this.visual = visual;
            this.shotId = shotId;
        }
    }
}
