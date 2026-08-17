package com.Hecate.puppet.editor;

import com.jme3.app.SimpleApplication;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Node;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Quad;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.util.BufferUtils;
import com.Hecate.puppet.animation.AnimationClip;
import com.Hecate.puppet.animation.Keyframe;
import java.util.List;
import java.util.ArrayList;

/**
 * Timeline时间轴面板
 * 显示当前时间、帧数和关键帧
 */
public class TimelinePanel {

    private final SimpleApplication app;
    private final BitmapFont font;
    private final Node rootNode;

    private BitmapText timeText;
    private BitmapText frameText;
    private Geometry playhead;
    private Node keyframeMarkersNode;  // 关键帧标记节点
    private List<Geometry> keyframeMarkers;  // 关键帧标记列表
    private Node timelineScaleNode;  // 时间刻度节点
    private float currentTime = 0f;
    private AnimationClip animationClip;  // 当前动画片段
    private int x, y;  // 改为可变，支持拖动
    private final int width, height;
    private final float timelineWidth;  // 时间轴宽度
    private final float timelineStartX;  // 时间轴起始X位置
    private float maxDisplayTime = 10.0f;  // 最大显示时间（秒），动态调整

    // 拖动相关
    private boolean isPanelDragging = false;  // 面板拖动状态
    private int dragStartX = 0;
    private int dragStartY = 0;
    private int panelStartX = 0;
    private int panelStartY = 0;
    private final int titleBarHeight = 30;  // 标题栏高度
    private Geometry backgroundGeometry;  // 背景几何体（用于拖动更新）
    private BitmapText titleText;  // 标题文本（用于拖动更新）

    // 回调接口（仅保留时间轴相关的回调）
    public interface TimelineCallbacks {
        void onTimeChanged(float newTime);
        void onKeyframeSelected(float keyframeTime);
        void onScrubbingStarted();  // 开始拖拽时调用
        void onScrubbingEnded();    // 结束拖拽时调用
    }
    private TimelineCallbacks callbacks;

    // 拖动状态
    private boolean isDraggingPlayhead = false;
    private boolean allowPlayToggle = false;  // 防止启动时意外播放
    private float playToggleLockTimer = 1.0f;  // 启动后1秒内锁定
    private boolean wasPlayingBeforeScrub = false;  // 拖拽前的播放状态

    // 选中的关键帧
    private Float selectedKeyframeTime = null;  // 选中的关键帧时间

    public TimelinePanel(SimpleApplication app, BitmapFont font, int x, int y, int width, int height) {
        this.app = app;
        this.font = font;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        this.timelineStartX = 10f;
        this.timelineWidth = width - 20f;
        this.keyframeMarkers = new ArrayList<>();

        this.rootNode = new Node("TimelinePanel");
        initializePanel();
    }

    /**
     * 初始化面板
     */
    private void initializePanel() {
        // 创建半透明背景
        Quad bgQuad = new Quad(width, height);
        backgroundGeometry = new Geometry("TimelineBackground", bgQuad);
        Material bgMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        bgMat.setColor("Color", new ColorRGBA(0.2f, 0.2f, 0.2f, 0.9f));
        bgMat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        backgroundGeometry.setMaterial(bgMat);
        backgroundGeometry.setLocalTranslation(x, y, -1);
        rootNode.attachChild(backgroundGeometry);

        // 标题
        titleText = new BitmapText(font);
        titleText.setSize(font.getCharSet().getRenderedSize() * 2.0f);
        titleText.setColor(ColorRGBA.Yellow);
        titleText.setLocalTranslation(x + 10, y + height - 10, 0);
        rootNode.attachChild(titleText);

        // 时间显示
        timeText = new BitmapText(font);
        timeText.setText("Time: 0.00s");
        timeText.setSize(font.getCharSet().getRenderedSize() * 2.0f);
        timeText.setColor(ColorRGBA.White);
        timeText.setLocalTranslation(x + 10, y + height - 50, 0);
        rootNode.attachChild(timeText);

        // 帧数显示
        frameText = new BitmapText(font);
        frameText.setText("Frame: 0");
        frameText.setSize(font.getCharSet().getRenderedSize() * 2.0f);
        frameText.setColor(ColorRGBA.White);
        frameText.setLocalTranslation(x + 300, y + height - 50, 0);
        rootNode.attachChild(frameText);

        // Playhead（播放头）
        Quad playheadQuad = new Quad(2, height - 60);
        playhead = new Geometry("Playhead", playheadQuad);
        Material playheadMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        playheadMat.setColor("Color", ColorRGBA.Red);
        playheadMat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        playhead.setMaterial(playheadMat);
        playhead.setLocalTranslation(x + 10, y + 10, 0);
        rootNode.attachChild(playhead);

        // 关键帧标记节点
        keyframeMarkersNode = new Node("KeyframeMarkers");
        rootNode.attachChild(keyframeMarkersNode);

        // 时间刻度节点
        timelineScaleNode = new Node("TimelineScale");
        rootNode.attachChild(timelineScaleNode);
        createTimelineScale();
    }

    /**
     * 创建时间轴刻度
     */
    private void createTimelineScale() {
        // 时间轴底线Y位置
        float timelineY = y + 50;

        // 根据时间范围动态调整刻度间隔
        float majorInterval, minorInterval;
        if (maxDisplayTime <= 10) {
            majorInterval = 1.0f;   // 每秒
            minorInterval = 0.5f;   // 每0.5秒
        } else if (maxDisplayTime <= 30) {
            majorInterval = 5.0f;   // 每5秒
            minorInterval = 1.0f;   // 每1秒
        } else if (maxDisplayTime <= 60) {
            majorInterval = 10.0f;  // 每10秒
            minorInterval = 5.0f;   // 每5秒
        } else {
            majorInterval = 20.0f;  // 每20秒
            minorInterval = 10.0f;  // 每10秒
        }

        // 创建刻度
        for (float time = 0; time <= maxDisplayTime; time += minorInterval) {
            boolean isMajor = (Math.abs(time % majorInterval) < 0.001f);

            // 计算刻度位置
            float normalizedTime = time / maxDisplayTime;
            float scaleX = x + timelineStartX + (normalizedTime * timelineWidth);

            // 刻度线高度
            float scaleHeight = isMajor ? 15f : 8f;

            // 创建刻度线（垂直线）
            Quad scaleQuad = new Quad(1, scaleHeight);
            Geometry scaleLine = new Geometry("Scale_" + time, scaleQuad);

            Material scaleMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
            scaleMat.setColor("Color", isMajor ? ColorRGBA.White : new ColorRGBA(0.6f, 0.6f, 0.6f, 1.0f));
            scaleMat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
            scaleLine.setMaterial(scaleMat);

            scaleLine.setLocalTranslation(scaleX, timelineY, 0);
            timelineScaleNode.attachChild(scaleLine);

            // 只在主刻度上添加时间标签
            if (isMajor) {
                BitmapText timeLabel = new BitmapText(font);
                timeLabel.setText(String.format("%.0fs", time));
                timeLabel.setSize(font.getCharSet().getRenderedSize() * 1.2f);
                timeLabel.setColor(ColorRGBA.White);

                // 计算标签位置（居中对齐刻度线）
                float labelWidth = timeLabel.getLineWidth();
                timeLabel.setLocalTranslation(scaleX - labelWidth / 2, timelineY - 5, 0);
                timelineScaleNode.attachChild(timeLabel);
            }
        }
    }

    /**
     * 设置当前时间
     */
    public void setTime(float time) {
        this.currentTime = time;
        timeText.setText(String.format("Time: %.2fs", time));

        // 30 FPS
        int frame = (int)(time * 30);
        frameText.setText("Frame: " + frame);

        // 检查是否需要扩展时间轴
        checkAndAdjustTimelineScale(time);

        // 移动playhead
        float normalizedTime = Math.min(time / maxDisplayTime, 1.0f);
        playhead.setLocalTranslation(x + timelineStartX + (normalizedTime * timelineWidth), y + 10, 0);
    }

    /**
     * 检查并调整时间轴范围
     */
    private void checkAndAdjustTimelineScale(float time) {
        // 如果时间超过当前显示范围的90%，扩展时间轴
        if (time > maxDisplayTime * 0.9f) {
            // 扩展到下一个10秒的倍数
            float newMaxTime = (float) Math.ceil(time / 10.0f) * 10.0f;
            if (newMaxTime < time + 10) {
                newMaxTime += 10;
            }

            if (newMaxTime != maxDisplayTime) {
                maxDisplayTime = newMaxTime;
                refreshTimelineScale();
                updateKeyframeMarkers();
            }
        }
    }

    /**
     * 刷新时间轴刻度
     */
    private void refreshTimelineScale() {
        // 清除旧的刻度（但保留节点在场景图中）
        timelineScaleNode.detachAllChildren();
        // 重新创建刻度
        createTimelineScale();
    }

    /**
     * 获取根节点
     */
    public Node getRootNode() {
        return rootNode;
    }

    /**
     * 获取当前时间
     */
    public float getCurrentTime() {
        return currentTime;
    }

    /**
     * 设置动画片段
     */
    public void setAnimationClip(AnimationClip clip) {
        this.animationClip = clip;
        updateKeyframeMarkers();
    }

    /**
     * 获取动画片段
     */
    public AnimationClip getAnimationClip() {
        return animationClip;
    }

    /**
     * 创建钻石形状的Mesh（尖端向下）
     * @param size 钻石的大小（宽度和高度）
     */
    private Mesh createDiamondMesh(float size) {
        Mesh mesh = new Mesh();

        // 钻石形顶点（尖端向下）:
        //     top
        //    /   \
        //  left  right
        //    \   /
        //    bottom
        float halfSize = size / 2f;
        float[] vertices = {
            0f, halfSize, 0f,           // top (顶部)
            -halfSize, 0f, 0f,          // left (左侧)
            halfSize, 0f, 0f,           // right (右侧)
            0f, -halfSize, 0f           // bottom (底部尖端)
        };

        // 索引（两个三角形组成钻石）
        short[] indices = {
            0, 1, 2,  // 上半部分三角形
            1, 3, 2   // 下半部分三角形
        };

        mesh.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(vertices));
        mesh.setBuffer(VertexBuffer.Type.Index, 1, BufferUtils.createShortBuffer(indices));
        mesh.updateBound();

        return mesh;
    }

    /**
     * 更新关键帧标记显示
     */
    public void updateKeyframeMarkers() {
        // 清除旧的标记
        for (Geometry marker : keyframeMarkers) {
            marker.removeFromParent();
        }
        keyframeMarkers.clear();

        if (animationClip == null) {
            return;
        }

        // 获取所有关键帧
        List<Keyframe> allKeyframes = animationClip.getAllKeyframes();

        // 检查是否需要扩展时间轴以显示所有关键帧
        for (Keyframe kf : allKeyframes) {
            float keyframeTime = kf.getTime();
            if (keyframeTime > maxDisplayTime * 0.9f) {
                float newMaxTime = (float) Math.ceil(keyframeTime / 10.0f) * 10.0f;
                if (newMaxTime < keyframeTime + 10) {
                    newMaxTime += 10;
                }
                if (newMaxTime != maxDisplayTime) {
                    maxDisplayTime = newMaxTime;
                    refreshTimelineScale();
                    // 继续处理关键帧标记（会使用新的maxDisplayTime）
                }
            }
        }

        // 创建关键帧标记
        // 时间轴底线Y位置（与createTimelineScale()保持一致）
        float timelineY = y + 50;

        for (Keyframe kf : allKeyframes) {
            float keyframeTime = kf.getTime();

            // 计算标记在时间轴上的X位置（相对于面板，不包含x偏移）
            float normalizedTime = keyframeTime / maxDisplayTime;
            float scaleX = timelineStartX + (normalizedTime * timelineWidth);

            // 创建钻石形关键帧标记（尖端向下指向时间轴）
            float diamondSize = 32f;  // 增大到32像素，更容易点击
            Mesh diamondMesh = createDiamondMesh(diamondSize);
            Geometry marker = new Geometry("Keyframe_" + keyframeTime, diamondMesh);

            Material markerMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");

            // 根据关键帧类型和选中状态设置颜色
            ColorRGBA markerColor;
            boolean isSelected = (selectedKeyframeTime != null && Math.abs(keyframeTime - selectedKeyframeTime) < 0.001f);

            if (isSelected) {
                // 选中的关键帧：亮绿色高亮
                markerColor = new ColorRGBA(0.0f, 1.0f, 0.0f, 1.0f);  // 亮绿色
            } else if (kf.getType() == Keyframe.KeyframeType.SNAPSHOT) {
                // 快照关键帧：蓝色
                markerColor = new ColorRGBA(0.2f, 0.5f, 1.0f, 1.0f);  // 蓝色
            } else {
                // 插值关键帧：黄色
                markerColor = ColorRGBA.Yellow;  // 黄色
            }
            markerMat.setColor("Color", markerColor);
            markerMat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
            marker.setMaterial(markerMat);

            // 标记位置：钻石顶部在时间轴刻度线上方，尖端指向时间轴
            float markerPosX = scaleX;  // 钻石中心对齐刻度线
            float markerPosY = 50 + 15 + diamondSize / 2;  // 在时间轴刻度线上方（相对于面板底部）
            marker.setLocalTranslation(markerPosX, markerPosY, 10);  // z=10确保在最上层

            // 存储关键帧时间和类型到用户数据，用于点击检测
            marker.setUserData("keyframeTime", keyframeTime);
            marker.setUserData("keyframeType", kf.getType().name());  // 转换为String，因为jME3 UserData不支持自定义枚举
            marker.setUserData("diamondSize", diamondSize);  // 也存储大小以便点击检测使用

            keyframeMarkersNode.attachChild(marker);
            keyframeMarkers.add(marker);
        }
    }

    /**
     * 设置回调接口
     */
    public void setCallbacks(TimelineCallbacks callbacks) {
        this.callbacks = callbacks;
    }

    /**
     * 处理鼠标点击
     */
    public boolean handleMouseClick(int mouseX, int mouseY) {
        int screenHeight = app.getCamera().getHeight();

        // 转换为GUI坐标
        float mouseGuiY = screenHeight - mouseY;

        // 检查是否点击在Timeline区域内
        float timelineTop = screenHeight - y - height;
        float timelineBottom = screenHeight - y;

        if (mouseGuiY < timelineTop || mouseGuiY > timelineBottom) {
            return false;
        }

        if (mouseX < x || mouseX > x + width) {
            return false;
        }

        // 首先检查是否点击了关键帧标记（钻石形）
        for (int i = 0; i < keyframeMarkers.size(); i++) {
            Geometry marker = keyframeMarkers.get(i);
            float markerCenterX = marker.getLocalTranslation().x + x;  // 转换为屏幕坐标
            float markerCenterY = marker.getLocalTranslation().y + y;  // 转换为屏幕坐标

            // 从UserData获取钻石大小
            Float diamondSizeObj = marker.getUserData("diamondSize");
            float diamondSize = (diamondSizeObj != null) ? diamondSizeObj : 32f;
            float halfSize = diamondSize / 2f;

            // 钻石形点击检测（使用矩形包围盒简化检测）
            float minX = markerCenterX - halfSize;
            float maxX = markerCenterX + halfSize;
            float minY = markerCenterY - halfSize;
            float maxY = markerCenterY + halfSize;

            if (mouseX >= minX && mouseX <= maxX &&
                mouseGuiY >= minY && mouseGuiY <= maxY) {

                // 点击了关键帧，从用户数据获取时间
                Float keyframeTime = marker.getUserData("keyframeTime");
                if (keyframeTime != null) {
                    // 更新选中状态
                    selectedKeyframeTime = keyframeTime;
                    // 刷新关键帧标记显示（高亮选中的关键帧）
                    updateKeyframeMarkers();

                    if (callbacks != null) {
                        callbacks.onKeyframeSelected(keyframeTime);
                        callbacks.onTimeChanged(keyframeTime);
                    }
                    setTime(keyframeTime);
                    return true;
                }
            }
        }

        // 没有点击关键帧，检查是否点击了播放头或时间轴
        float playheadX = playhead.getLocalTranslation().x;
        if (mouseX >= playheadX && mouseX <= playheadX + 2) {
            // 点击了播放头，开始拖动
            isDraggingPlayhead = true;

            // 通知开始scrubbing
            if (callbacks != null) {
                callbacks.onScrubbingStarted();
            }
            return true;
        }

        // 点击了时间轴其他位置，跳转到该时间
        float normalizedTime = (mouseX - x - timelineStartX) / timelineWidth;
        normalizedTime = Math.max(0, Math.min(1, normalizedTime));
        float newTime = normalizedTime * maxDisplayTime;

        setTime(newTime);
        if (callbacks != null) {
            callbacks.onTimeChanged(newTime);
        }

        return true;
    }

    /**
     * 处理鼠标拖动
     */
    public void handleMouseDrag(int mouseX, int mouseY) {
        if (isDraggingPlayhead) {
            // 计算新的时间
            float normalizedTime = (mouseX - x - timelineStartX) / timelineWidth;
            normalizedTime = Math.max(0, Math.min(1, normalizedTime));
            float newTime = normalizedTime * maxDisplayTime;

            setTime(newTime);
            if (callbacks != null) {
                callbacks.onTimeChanged(newTime);
            }
        }
    }

    /**
     * 处理鼠标释放
     */
    public void handleMouseRelease() {
        if (isDraggingPlayhead) {
            // 通知结束scrubbing
            if (callbacks != null) {
                callbacks.onScrubbingEnded();
            }
        }
        isDraggingPlayhead = false;
    }

    /**
     * 获取Timeline的Y位置（用于检测点击）
     */
    public int getY() {
        return y;
    }

    /**
     * 获取Timeline的高度
     */
    public int getHeight() {
        return height;
    }

    /**
     * 获取Timeline的X位置
     */
    public int getX() {
        return x;
    }

    /**
     * 获取Timeline的宽度
     */
    public int getWidth() {
        return width;
    }

    /**
     * 检查鼠标是否在Timeline区域内
     */
    public boolean isMouseOver(int mouseX, int mouseY) {
        int screenHeight = app.getCamera().getHeight();
        float mouseGuiY = screenHeight - mouseY;

        return mouseX >= x && mouseX <= x + width &&
               mouseGuiY >= y && mouseGuiY <= y + height;
    }

    /**
     * 处理鼠标滚轮缩放时间轴
     * 以鼠标位置为中心进行缩放
     * @param scrollAmount 滚动量，正数为缩小（显示更多时间），负数为放大（显示更少时间）
     */
    public boolean handleMouseScroll(int mouseX, int mouseY, int scrollAmount) {
        if (!isMouseOver(mouseX, mouseY)) {
            return false;
        }

        // 计算鼠标在时间轴上的相对位置（0-1）
        float relativeMouseX = (mouseX - x - timelineStartX) / timelineWidth;
        relativeMouseX = Math.max(0, Math.min(1, relativeMouseX));

        // 计算鼠标位置对应的时间
        float mouseTime = relativeMouseX * maxDisplayTime;

        // 缩放因子（向上滚=放大，向下滚=缩小）
        float zoomFactor = 1.0f - (scrollAmount * 0.15f);  // 调整方向和速度

        // 计算新的时间范围
        float newMaxDisplayTime = maxDisplayTime * zoomFactor;

        // 限制时间范围：最小0.5秒（用于精细动画），最大300秒
        newMaxDisplayTime = Math.max(0.5f, Math.min(300.0f, newMaxDisplayTime));

        if (Math.abs(newMaxDisplayTime - maxDisplayTime) < 0.01f) {
            return true; // 已达到缩放极限，但仍然消费事件
        }

        // 应用新的时间范围
        maxDisplayTime = newMaxDisplayTime;

        // 刷新时间轴
        refreshTimelineScale();
        updateKeyframeMarkers();

        // 更新播放头位置（保持当前时间不变）
        setTime(currentTime);

        return true;
    }

    /**
     * 以指定时间为中心缩放时间轴
     * @param centerTime 缩放中心时间
     * @param zoomIn true为放大（显示更少时间），false为缩小（显示更多时间）
     */
    public void zoomTimelineAtTime(float centerTime, boolean zoomIn) {
        // 缩放因子
        float zoomFactor = zoomIn ? 0.85f : 1.18f;  // 放大15%或缩小15%

        // 计算新的时间范围
        float newMaxDisplayTime = maxDisplayTime * zoomFactor;

        // 限制时间范围：最小0.5秒（用于精细动画），最大300秒
        newMaxDisplayTime = Math.max(0.5f, Math.min(300.0f, newMaxDisplayTime));

        if (Math.abs(newMaxDisplayTime - maxDisplayTime) < 0.01f) {
            return; // 已达到缩放极限
        }

        // 应用新的时间范围
        maxDisplayTime = newMaxDisplayTime;

        // 刷新时间轴
        refreshTimelineScale();
        updateKeyframeMarkers();

        // 更新播放头位置（保持当前时间不变）
        setTime(currentTime);
    }

    /**
     * 更新锁定计时器和按钮状态
     */
    public void update(float tpf) {
        if (!allowPlayToggle && playToggleLockTimer > 0) {
            playToggleLockTimer -= tpf;
            if (playToggleLockTimer <= 0) {
                allowPlayToggle = true;
            }
        }
    }

    /**
     * 检查是否点击在标题栏（用于拖动面板）
     */
    public boolean handleTitleBarClick(int mouseX, int mouseY) {
        // 检查X范围
        if (mouseX < x || mouseX > x + width) {
            return false;
        }

        // 检查Y范围（标题栏区域，从面板顶部开始的titleBarHeight高度）
        if (mouseY >= y + height - titleBarHeight && mouseY <= y + height) {
            isPanelDragging = true;
            dragStartX = mouseX;
            dragStartY = mouseY;
            panelStartX = x;
            panelStartY = y;
            return true;
        }

        return false;
    }

    /**
     * 处理面板拖动
     */
    public void handlePanelDrag(int mouseX, int mouseY) {
        if (isPanelDragging) {
            int deltaX = mouseX - dragStartX;
            int deltaY = mouseY - dragStartY;

            x = panelStartX + deltaX;
            y = panelStartY + deltaY;

            // 限制在屏幕范围内
            x = Math.max(0, Math.min(x, app.getCamera().getWidth() - width));
            y = Math.max(0, Math.min(y, app.getCamera().getHeight() - height));

            // 更新所有UI元素的位置
            updatePanelPosition();
        }
    }

    /**
     * 处理面板拖动释放
     */
    public void handlePanelDragRelease() {
        if (isPanelDragging) {
            isPanelDragging = false;
        }
    }

    /**
     * 更新面板位置（用于拖动时）
     */
    private void updatePanelPosition() {
        // 更新背景位置
        if (backgroundGeometry != null) {
            backgroundGeometry.setLocalTranslation(x, y, -1);
        }

        // 更新标题
        if (titleText != null) {
            titleText.setLocalTranslation(x + 10, y + height - 10, 0);
        }

        // 更新时间显示
        if (timeText != null) {
            timeText.setLocalTranslation(x + 10, y + height - 50, 0);
        }

        // 更新帧数显示
        if (frameText != null) {
            frameText.setLocalTranslation(x + 300, y + height - 50, 0);
        }

        // 更新时间轴相关元素
        updateTimelineElements();
    }

    /**
     * 更新时间轴相关元素位置
     */
    private void updateTimelineElements() {
        // 更新播放头位置
        if (playhead != null) {
            float playheadX = x + timelineStartX + (currentTime / maxDisplayTime) * timelineWidth;
            playhead.setLocalTranslation(playheadX, y + 10, 0);  // 使用与setTime()一致的Y偏移
        }

        // 更新时间刻度（清除子节点但不移除容器节点）
        if (timelineScaleNode != null) {
            timelineScaleNode.detachAllChildren();  // 清除旧刻度，但保留节点在场景图中
            createTimelineScale();  // createTimelineScale会将新刻度附加到timelineScaleNode
            // 确保timelineScaleNode仍然在rootNode中
            if (timelineScaleNode.getParent() == null) {
                rootNode.attachChild(timelineScaleNode);
            }
        }

        // 更新关键帧标记
        updateKeyframeMarkers();

        // 确保keyframeMarkersNode仍然在rootNode中
        if (keyframeMarkersNode != null && keyframeMarkersNode.getParent() == null) {
            rootNode.attachChild(keyframeMarkersNode);
        }
    }

    /**
     * 获取选中的关键帧时间
     */
    public Float getSelectedKeyframeTime() {
        return selectedKeyframeTime;
    }

    /**
     * 清除关键帧选择
     */
    public void clearSelection() {
        selectedKeyframeTime = null;
        updateKeyframeMarkers();
    }
}
