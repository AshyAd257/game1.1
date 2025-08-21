package com.Hecate.module.test;

import com.Hecate.module.AbstractGameModule;
import com.Hecate.module.Version;

/**
 * 测试模块 - 展示如何创建具体模块
 */
public class TestModule extends AbstractGameModule {
    private static final String MODULE_ID = "test-module";
    private static final Version MODULE_VERSION = new Version(1, 0, 0);

    private float timer = 0;

    // 添加无参构造函数
    public TestModule() {
        // 构造函数不需要做任何事情
    }

    @Override
    public String getId() {
        return MODULE_ID;
    }

    @Override
    public Version getVersion() {
        return MODULE_VERSION;
    }

    @Override
    public void onLoad() {
        System.out.println("测试模块: onLoad()被调用");
    }

    @Override
    public void onInitialize() {
        System.out.println("测试模块: onInitialize()被调用");
    }

    @Override
    public void onPostInitialize() {
        System.out.println("测试模块: onPostInitialize()被调用");
    }

    @Override
    public void onUpdate(float tpf) {
        // 每秒打印一次消息
        timer += tpf;
        if (timer >= 1.0f) {
            System.out.println("测试模块: onUpdate()被调用 - 游戏已运行" + (int)timer + "秒");
            timer -= 1.0f;
        }
    }

    @Override
    public void onDisable() {
        System.out.println("测试模块: onDisable()被调用");
    }
}