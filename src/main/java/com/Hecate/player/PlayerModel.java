package com.Hecate.player;

import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;

/**
 * Minecraft Steve风格的玩家模型
 * 包含头部、身体、手臂和腿部的分离式结构
 */
public class PlayerModel {
    private final SimpleApplication app;
    
    // 主模型节点
    private Node modelNode;
    
    // 身体部位几何体
    private Geometry head;
    private Geometry body;
    private Geometry leftArm;
    private Geometry rightArm;
    private Geometry leftLeg;
    private Geometry rightLeg;
    
    // 身体部位节点（用于动画旋转）
    private Node headNode;
    private Node bodyNode;
    private Node leftArmNode;
    private Node rightArmNode;
    private Node leftLegNode;
    private Node rightLegNode;
    
    // Minecraft玩家模型比例常量（单位：方块）
    private static final float SCALE = 1f / 16f; // 1像素 = 1/16方块
    
    // 头部尺寸：8x8x8像素
    private static final float HEAD_WIDTH = 8 * SCALE;
    private static final float HEAD_HEIGHT = 8 * SCALE;
    private static final float HEAD_DEPTH = 8 * SCALE;
    
    // 身体尺寸：8x12x4像素
    private static final float BODY_WIDTH = 8 * SCALE;
    private static final float BODY_HEIGHT = 12 * SCALE;
    private static final float BODY_DEPTH = 4 * SCALE;
    
    // 手臂尺寸：4x12x4像素
    private static final float ARM_WIDTH = 4 * SCALE;
    private static final float ARM_HEIGHT = 12 * SCALE;
    private static final float ARM_DEPTH = 4 * SCALE;
    
    // 腿部尺寸：4x12x4像素
    private static final float LEG_WIDTH = 4 * SCALE;
    private static final float LEG_HEIGHT = 12 * SCALE;
    private static final float LEG_DEPTH = 4 * SCALE;
    
    public PlayerModel(SimpleApplication app) {
        this.app = app;
        createModel();
    }
    
    /**
     * 创建完整的玩家模型
     */
    private void createModel() {
        System.out.println("🎨 创建Minecraft Steve风格玩家模型...");
        
        // 创建主模型节点
        modelNode = new Node("PlayerModel");
        
        // 创建身体部位
        createHead();
        createBody();
        createArms();
        createLegs();
        
        // 设置各部位相对位置
        positionBodyParts();
        
        System.out.println("✅ 玩家模型创建完成");
    }
    
    /**
     * 创建头部
     */
    private void createHead() {
        // 创建头部几何体
        Box headBox = new Box(HEAD_WIDTH / 2, HEAD_HEIGHT / 2, HEAD_DEPTH / 2);
        head = new Geometry("Head", headBox);
        
        // 创建头部材质（棕色皮肤色）
        Material headMaterial = new Material(app.getAssetManager(), "Common/MatDefs/Light/Lighting.j3md");
        headMaterial.setColor("Diffuse", new ColorRGBA(0.96f, 0.8f, 0.69f, 1.0f)); // 皮肤色
        headMaterial.setColor("Ambient", new ColorRGBA(0.4f, 0.3f, 0.2f, 1.0f)); // 添加环境光
        headMaterial.setColor("Specular", ColorRGBA.White);
        headMaterial.setFloat("Shininess", 32f);
        headMaterial.setBoolean("UseMaterialColors", true);
        head.setMaterial(headMaterial);
        
        // 创建头部节点并添加几何体
        headNode = new Node("HeadNode");
        headNode.attachChild(head);
        
        // 添加眼睛和嘴巴细节
        addFacialFeatures();
        
        modelNode.attachChild(headNode);
        
        System.out.println("🗣️ 头部创建完成");
    }
    
    /**
     * 添加面部特征（眼睛、嘴巴）
     */
    private void addFacialFeatures() {
        // 左眼
        Box leftEyeBox = new Box(2 * SCALE / 2, 1 * SCALE / 2, 0.5f * SCALE / 2);
        Geometry leftEye = new Geometry("LeftEye", leftEyeBox);
        Material eyeMaterial = new Material(app.getAssetManager(), "Common/MatDefs/Light/Lighting.j3md");
        eyeMaterial.setColor("Diffuse", ColorRGBA.White);
        eyeMaterial.setColor("Ambient", ColorRGBA.Gray);
        eyeMaterial.setBoolean("UseMaterialColors", true);
        leftEye.setMaterial(eyeMaterial);
        leftEye.setLocalTranslation(-2 * SCALE, 1 * SCALE, HEAD_DEPTH / 2 + 0.01f);
        headNode.attachChild(leftEye);
        
        // 右眼
        Geometry rightEye = new Geometry("RightEye", leftEyeBox.clone());
        rightEye.setMaterial(eyeMaterial.clone());
        rightEye.setLocalTranslation(2 * SCALE, 1 * SCALE, HEAD_DEPTH / 2 + 0.01f);
        headNode.attachChild(rightEye);
        
        // 左眼瞳孔
        Box pupilBox = new Box(1 * SCALE / 2, 1 * SCALE / 2, 0.25f * SCALE / 2);
        Geometry leftPupil = new Geometry("LeftPupil", pupilBox);
        Material pupilMaterial = new Material(app.getAssetManager(), "Common/MatDefs/Light/Lighting.j3md");
        pupilMaterial.setColor("Diffuse", new ColorRGBA(0.2f, 0.4f, 0.8f, 1.0f)); // 蓝色瞳孔
        pupilMaterial.setColor("Ambient", ColorRGBA.Black);
        pupilMaterial.setBoolean("UseMaterialColors", true);
        leftPupil.setMaterial(pupilMaterial);
        leftPupil.setLocalTranslation(-2 * SCALE, 1 * SCALE, HEAD_DEPTH / 2 + 0.02f);
        headNode.attachChild(leftPupil);
        
        // 右眼瞳孔
        Geometry rightPupil = new Geometry("RightPupil", pupilBox.clone());
        rightPupil.setMaterial(pupilMaterial.clone());
        rightPupil.setLocalTranslation(2 * SCALE, 1 * SCALE, HEAD_DEPTH / 2 + 0.02f);
        headNode.attachChild(rightPupil);
        
        // 嘴巴
        Box mouthBox = new Box(3 * SCALE / 2, 0.5f * SCALE / 2, 0.25f * SCALE / 2);
        Geometry mouth = new Geometry("Mouth", mouthBox);
        Material mouthMaterial = new Material(app.getAssetManager(), "Common/MatDefs/Light/Lighting.j3md");
        mouthMaterial.setColor("Diffuse", new ColorRGBA(0.5f, 0.2f, 0.2f, 1.0f)); // 深红色嘴巴
        mouthMaterial.setColor("Ambient", ColorRGBA.Black);
        mouthMaterial.setBoolean("UseMaterialColors", true);
        mouth.setMaterial(mouthMaterial);
        mouth.setLocalTranslation(0, -2 * SCALE, HEAD_DEPTH / 2 + 0.01f);
        headNode.attachChild(mouth);
    }
    
    /**
     * 创建身体
     */
    private void createBody() {
        // 创建身体几何体
        Box bodyBox = new Box(BODY_WIDTH / 2, BODY_HEIGHT / 2, BODY_DEPTH / 2);
        body = new Geometry("Body", bodyBox);
        
        // 创建身体材质（青色衬衫）
        Material bodyMaterial = new Material(app.getAssetManager(), "Common/MatDefs/Light/Lighting.j3md");
        bodyMaterial.setColor("Diffuse", new ColorRGBA(0.0f, 0.8f, 0.8f, 1.0f)); // 青色衬衫（更接近Minecraft Steve）
        bodyMaterial.setColor("Ambient", new ColorRGBA(0.0f, 0.3f, 0.3f, 1.0f));
        bodyMaterial.setColor("Specular", ColorRGBA.White);
        bodyMaterial.setFloat("Shininess", 16f);
        bodyMaterial.setBoolean("UseMaterialColors", true);
        body.setMaterial(bodyMaterial);
        
        // 创建身体节点并添加几何体
        bodyNode = new Node("BodyNode");
        bodyNode.attachChild(body);
        modelNode.attachChild(bodyNode);
        
        System.out.println("👕 身体创建完成");
    }
    
    /**
     * 创建手臂
     */
    private void createArms() {
        // 左臂
        Box leftArmBox = new Box(ARM_WIDTH / 2, ARM_HEIGHT / 2, ARM_DEPTH / 2);
        leftArm = new Geometry("LeftArm", leftArmBox);
        
        // 右臂
        Box rightArmBox = new Box(ARM_WIDTH / 2, ARM_HEIGHT / 2, ARM_DEPTH / 2);
        rightArm = new Geometry("RightArm", rightArmBox);
        
        // 手臂材质（皮肤色）
        Material armMaterial = new Material(app.getAssetManager(), "Common/MatDefs/Light/Lighting.j3md");
        armMaterial.setColor("Diffuse", new ColorRGBA(0.96f, 0.8f, 0.69f, 1.0f)); // 皮肤色
        armMaterial.setColor("Ambient", new ColorRGBA(0.4f, 0.3f, 0.2f, 1.0f));
        armMaterial.setColor("Specular", ColorRGBA.White);
        armMaterial.setFloat("Shininess", 32f);
        armMaterial.setBoolean("UseMaterialColors", true);
        
        leftArm.setMaterial(armMaterial);
        rightArm.setMaterial(armMaterial.clone());
        
        // 创建手臂节点
        leftArmNode = new Node("LeftArmNode");
        rightArmNode = new Node("RightArmNode");
        
        leftArmNode.attachChild(leftArm);
        rightArmNode.attachChild(rightArm);
        
        modelNode.attachChild(leftArmNode);
        modelNode.attachChild(rightArmNode);
        
        System.out.println("💪 手臂创建完成");
    }
    
    /**
     * 创建腿部
     */
    private void createLegs() {
        // 左腿
        Box leftLegBox = new Box(LEG_WIDTH / 2, LEG_HEIGHT / 2, LEG_DEPTH / 2);
        leftLeg = new Geometry("LeftLeg", leftLegBox);
        
        // 右腿
        Box rightLegBox = new Box(LEG_WIDTH / 2, LEG_HEIGHT / 2, LEG_DEPTH / 2);
        rightLeg = new Geometry("RightLeg", rightLegBox);
        
        // 腿部材质（深蓝色牛仔裤）
        Material legMaterial = new Material(app.getAssetManager(), "Common/MatDefs/Light/Lighting.j3md");
        legMaterial.setColor("Diffuse", new ColorRGBA(0.15f, 0.2f, 0.5f, 1.0f)); // 稍微亮一点的蓝色牛仔裤
        legMaterial.setColor("Ambient", new ColorRGBA(0.05f, 0.05f, 0.15f, 1.0f));
        legMaterial.setColor("Specular", ColorRGBA.Gray);
        legMaterial.setFloat("Shininess", 8f);
        legMaterial.setBoolean("UseMaterialColors", true);
        
        leftLeg.setMaterial(legMaterial);
        rightLeg.setMaterial(legMaterial.clone());
        
        // 创建腿部节点
        leftLegNode = new Node("LeftLegNode");
        rightLegNode = new Node("RightLegNode");
        
        leftLegNode.attachChild(leftLeg);
        rightLegNode.attachChild(rightLeg);
        
        modelNode.attachChild(leftLegNode);
        modelNode.attachChild(rightLegNode);
        
        System.out.println("🦵 腿部创建完成");
    }
    
    /**
     * 设置各身体部位的相对位置
     */
    private void positionBodyParts() {
        System.out.println("📐 设置身体部位位置...");
        
        // 头部位置：在身体顶部
        float headY = (BODY_HEIGHT + HEAD_HEIGHT) / 2;
        headNode.setLocalTranslation(0, headY, 0);
        
        // 身体位置：中心位置
        bodyNode.setLocalTranslation(0, 0, 0);
        
        // 左臂位置：身体左侧，与身体同高度
        float armX = (BODY_WIDTH + ARM_WIDTH) / 2;
        leftArmNode.setLocalTranslation(-armX, 0, 0);
        
        // 右臂位置：身体右侧，与身体同高度
        rightArmNode.setLocalTranslation(armX, 0, 0);
        
        // 左腿位置：身体底部左侧
        float legY = -(BODY_HEIGHT + LEG_HEIGHT) / 2;
        float legX = LEG_WIDTH / 2;
        leftLegNode.setLocalTranslation(-legX, legY, 0);
        
        // 右腿位置：身体底部右侧
        rightLegNode.setLocalTranslation(legX, legY, 0);
        
        System.out.println("✅ 身体部位位置设置完成");
    }
    
    /**
     * 获取模型节点
     */
    public Node getModelNode() {
        return modelNode;
    }
    
    /**
     * 获取模型总高度
     */
    public float getTotalHeight() {
        return HEAD_HEIGHT + BODY_HEIGHT + LEG_HEIGHT;
    }
    
    /**
     * 获取模型总宽度
     */
    public float getTotalWidth() {
        return Math.max(HEAD_WIDTH, BODY_WIDTH + ARM_WIDTH * 2);
    }
    
    // Getter方法用于动画系统
    public Node getHeadNode() { return headNode; }
    public Node getBodyNode() { return bodyNode; }
    public Node getLeftArmNode() { return leftArmNode; }
    public Node getRightArmNode() { return rightArmNode; }
    public Node getLeftLegNode() { return leftLegNode; }
    public Node getRightLegNode() { return rightLegNode; }
    
    /**
     * 设置模型整体位置
     */
    public void setPosition(Vector3f position) {
        modelNode.setLocalTranslation(position);
    }
    
    /**
     * 设置模型整体旋转
     */
    public void setRotation(float yRotation) {
        modelNode.setLocalRotation(modelNode.getLocalRotation().fromAngleAxis(yRotation, Vector3f.UNIT_Y));
    }
}