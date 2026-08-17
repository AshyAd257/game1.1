# -*- coding: utf-8 -*-
"""
创建中文字符集文件
用于 BMFont 生成中文字体
"""

# 木偶编辑器需要的所有中文字符
chinese_chars = """
木偶编辑器返回退出语言
快捷键显示隐藏全屏切换选择骨骼调整宽度高度录制关键帧
空格播放暂停鼠标左键拖拽旋转视角滚轮缩放
滑条尺寸位置部件名称属性值面板时间帧数
添加删除复制粘贴撤销重做保存加载导出
纹理图片文件路径坐标角度透明度颜色
动画序列循环速度插值线性贝塞尔
层级父子关系绑定解绑锁定解锁
可见不可见启用禁用激活取消
确定取消应用重置默认自定义
新建打开关闭最近项目设置帮助关于
工具选项视图窗口布局主题
上下左右前后顶底侧面正交透视
网格吸附对齐分布间距边距填充
镜像翻转水平垂直
标题内容描述注释标签分类
搜索过滤排序分组展开折叠
警告错误信息提示成功失败
是否确认继续跳过忽略
"""

# 英文字符和数字
ascii_chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"

# 特殊符号
symbols = "+-*/=()[]{},.;:!?<>|&@#$%^~_\\'\"`"

# 合并所有字符并去重
all_chars = chinese_chars + ascii_chars + symbols
unique_chars = ''.join(sorted(set(all_chars.replace('\n', '').replace(' ', ''))))

# 写入文件
with open('chars.txt', 'w', encoding='utf-8') as f:
    f.write(unique_chars)

print(f"Created chars.txt with {len(unique_chars)} characters")
print(f"Preview: {unique_chars[:50]}...")
