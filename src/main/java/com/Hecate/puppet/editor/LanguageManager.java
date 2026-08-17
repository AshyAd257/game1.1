package com.Hecate.puppet.editor;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

/**
 * 语言管理器 - 管理UI文本的多语言支持
 *
 * <p><b>依赖注入支持</b>：推荐使用无参构造函数创建实例，支持多语言环境隔离。
 *
 * <h3>推荐用法（依赖注入）</h3>
 * <pre>{@code
 * // 在 PuppetEditorApp 初始化时创建
 * LanguageManager langManager = new LanguageManager();
 * PuppetEditorUI ui = new PuppetEditorUI(app, langManager);
 * }</pre>
 *
 * <h3>向后兼容用法（已废弃）</h3>
 * <pre>{@code
 * // 旧代码仍可正常工作
 * LanguageManager.getInstance().getText("key");
 * }</pre>
 *
 * @see LanguageChangeListener
 */
public class LanguageManager {

    private static LanguageManager defaultInstance;
    private String currentLanguage = "zh"; // 默认中文
    private Map<String, Map<String, String>> translations;
    private List<LanguageChangeListener> listeners;

    public interface LanguageChangeListener {
        void onLanguageChanged(String newLanguage);
    }

    /**
     * 构造函数 - 创建新的语言管理器实例
     * <p>支持依赖注入，允许多个独立的语言环境（例如：编辑器、游戏、测试环境）
     */
    public LanguageManager() {
        translations = new HashMap<>();
        listeners = new ArrayList<>();
        initTranslations();
    }

    /**
     * 获取默认实例（向后兼容）
     *
     * @return 全局共享的语言管理器实例
     * @deprecated 推荐使用依赖注入：直接创建实例 {@code new LanguageManager()}
     */
    @Deprecated
    public static LanguageManager getInstance() {
        if (defaultInstance == null) {
            defaultInstance = new LanguageManager();
        }
        return defaultInstance;
    }

    /**
     * 获取默认实例
     * <p>用于需要全局共享语言设置的场景（如编辑器全局设置）
     *
     * @return 默认语言管理器实例
     */
    public static LanguageManager getDefaultInstance() {
        return getInstance();
    }

    /**
     * 创建新的独立实例
     * <p>用于需要隔离语言环境的场景（如测试、多窗口、多用户）
     *
     * @return 新的语言管理器实例
     */
    public static LanguageManager createInstance() {
        return new LanguageManager();
    }

    private void initTranslations() {
        // 初始化中文文本
        Map<String, String> zh = new HashMap<>();
        zh.put("back", "返回");
        zh.put("exit", "退出");
        zh.put("language", "语言");
        zh.put("save", "保存");
        zh.put("load", "加载");
        zh.put("new_part", "新建部件");
        zh.put("delete_part", "删除部件");
        zh.put("hide_part", "隐藏部件");
        zh.put("show_all", "显示全部");
        zh.put("bone_transform", "骨骼变换");
        zh.put("toggle_bones", "切换骨线");
        zh.put("snap_grid", "吸附网格");
        zh.put("mirror_mode", "镜像模式");
        // 滑条标签
        zh.put("width", "宽度");
        zh.put("height", "高度");
        zh.put("priority", "优先级");
        zh.put("pos_x", "位置X");
        zh.put("pos_y", "位置Y");
        zh.put("pos_z", "位置Z");
        zh.put("rot_x", "旋转X");
        zh.put("rot_y", "旋转Y");
        zh.put("rot_z", "旋转Z");
        zh.put("tex_rot", "贴图旋转");
        zh.put("grid_size", "网格大小");
        zh.put("freedom", "自由度");
        zh.put("open_uv_editor", "打开UV编辑器");
        // 按钮标签
        zh.put("add_part", "添加部件");
        zh.put("load_puppet", "加载木偶");
        zh.put("add_puppet", "添加木偶");
        zh.put("save_puppet", "保存木偶");
        zh.put("export_anim", "导出动画");
        zh.put("import_anim", "导入动画");
        zh.put("load_texture", "加载纹理");
        zh.put("set_parent", "添加刚性骨骼");
        zh.put("add_free_bone", "添加自由骨骼");
        zh.put("clear_parent", "清除骨骼");
        zh.put("transform_mode_part", "模式:部件");
        zh.put("transform_mode_bone", "模式:骨骼");
        zh.put("bone_lines_on", "骨线:开");
        zh.put("bone_lines_off", "骨线:关");
        zh.put("play", "播放动画");
        zh.put("pause", "暂停");
        zh.put("reset_timeline", "重置时间轴");
        zh.put("add_keyframe", "添加关键帧");
        zh.put("add_snapshot", "添加快照帧");
        zh.put("delete_keyframe", "删除帧");
        zh.put("undo", "撤销");
        zh.put("redo", "重做");
        zh.put("copy", "复制");
        zh.put("paste", "粘贴");
        zh.put("paste_mirror", "镜像粘贴");
        zh.put("grid_on", "网格:开");
        zh.put("grid_off", "网格:关");
        zh.put("preview_on", "预览:开");
        zh.put("preview_off", "预览:关");
        zh.put("billboard_2d", "2D模式");
        zh.put("billboard_3d", "3D模式");
        zh.put("texture_single", "纹理:单向");
        zh.put("texture_multi", "纹理:多向");
        zh.put("dir_front", "方向:前");
        zh.put("dir_back", "方向:后");
        zh.put("dir_left", "方向:左");
        zh.put("dir_right", "方向:右");
        zh.put("gravity_down", "重力:下");
        zh.put("hide_on", "隐藏部件:开");
        zh.put("hide_off", "隐藏部件");
        translations.put("zh", zh);

        // 初始化英文文本
        Map<String, String> en = new HashMap<>();
        en.put("back", "Back");
        en.put("exit", "Exit");
        en.put("language", "Lang");
        en.put("save", "Save");
        en.put("load", "Load");
        en.put("new_part", "New Part");
        en.put("delete_part", "Delete");
        en.put("hide_part", "Hide");
        en.put("show_all", "Show All");
        en.put("bone_transform", "Transform");
        en.put("toggle_bones", "Bones");
        en.put("snap_grid", "Grid");
        en.put("mirror_mode", "Mirror");
        // 滑条标签
        en.put("width", "Width");
        en.put("height", "Height");
        en.put("priority", "Priority");
        en.put("offset_multi", "Offset Multi");
        en.put("pos_x", "Pos X");
        en.put("pos_y", "Pos Y");
        en.put("pos_z", "Pos Z");
        en.put("rot_x", "Rot X");
        en.put("rot_y", "Rot Y");
        en.put("rot_z", "Rot Z");
        en.put("tex_rot", "Tex Rot");
        en.put("grid_size", "Grid Size");
        en.put("freedom", "Freedom");
        en.put("open_uv_editor", "Open UV Editor");
        // 按钮标签
        en.put("add_part", "Add Part");
        en.put("load_puppet", "Load Puppet");
        en.put("add_puppet", "Add Puppet");
        en.put("save_puppet", "Save Puppet");
        en.put("export_anim", "Export Anim");
        en.put("import_anim", "Import Anim");
        en.put("load_texture", "Load Texture");
        en.put("set_parent", "Set Parent");
        en.put("add_free_bone", "Add Free Bone");
        en.put("clear_parent", "Clear Parent");
        en.put("transform_mode_part", "Mode: Part");
        en.put("transform_mode_bone", "Mode: Bone");
        en.put("bone_lines_on", "Lines: ON");
        en.put("bone_lines_off", "Lines: OFF");
        en.put("play", "Play");
        en.put("pause", "Pause");
        en.put("reset_timeline", "Reset");
        en.put("add_keyframe", "Add Key");
        en.put("add_snapshot", "Add Snap");
        en.put("delete_keyframe", "Del Key");
        en.put("undo", "Undo");
        en.put("redo", "Redo");
        en.put("copy", "Copy");
        en.put("paste", "Paste");
        en.put("paste_mirror", "Paste Mirror");
        en.put("grid_on", "Grid: ON");
        en.put("grid_off", "Grid: OFF");
        en.put("preview_on", "Preview: ON");
        en.put("preview_off", "Preview: OFF");
        en.put("billboard_2d", "2D Mode");
        en.put("billboard_3d", "3D Mode");
        en.put("texture_single", "Tex: Single");
        en.put("texture_multi", "Tex: Multi");
        en.put("dir_front", "Dir: Front");
        en.put("dir_back", "Dir: Back");
        en.put("dir_left", "Dir: Left");
        en.put("dir_right", "Dir: Right");
        en.put("gravity_down", "Gravity: Down");
        en.put("hide_on", "Hide: ON");
        en.put("hide_off", "Hide: OFF");
        translations.put("en", en);
    }

    /**
     * 获取指定key的文本
     */
    public String getText(String key) {
        Map<String, String> langMap = translations.get(currentLanguage);
        if (langMap != null && langMap.containsKey(key)) {
            return langMap.get(key);
        }
        // 如果当前语言没有，尝试返回中文
        langMap = translations.get("zh");
        if (langMap != null && langMap.containsKey(key)) {
            return langMap.get(key);
        }
        return key; // 如果都没有，返回key本身
    }

    /**
     * 设置当前语言
     */
    public void setLanguage(String language) {
        if (!language.equals(currentLanguage)) {
            currentLanguage = language;
            notifyListeners();
        }
    }

    /**
     * 获取当前语言
     */
    public String getCurrentLanguage() {
        return currentLanguage;
    }

    /**
     * 添加语言变化监听器
     */
    public void addListener(LanguageChangeListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * 移除语言变化监听器
     */
    public void removeListener(LanguageChangeListener listener) {
        listeners.remove(listener);
    }

    /**
     * 通知所有监听器语言已变化
     */
    private void notifyListeners() {
        for (LanguageChangeListener listener : listeners) {
            listener.onLanguageChanged(currentLanguage);
        }
    }
}
