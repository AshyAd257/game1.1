#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
修复 BMFont 字体文件的 yoffset 问题 - 版本2
使用正确的 yoffset 计算方式
"""

import re
import sys

def fix_font_file(input_file, output_file):
    """修复字体文件的 yoffset"""

    with open(input_file, 'r', encoding='utf-8') as f:
        lines = f.readlines()

    # 查找 base 和 lineHeight 值
    base_value = 16  # 默认值
    line_height = 20  # 默认值

    for line in lines:
        if line.startswith('common'):
            match_base = re.search(r'base=(\d+)', line)
            match_lineheight = re.search(r'lineHeight=(\d+)', line)
            if match_base:
                base_value = int(match_base.group(1))
            if match_lineheight:
                line_height = int(match_lineheight.group(1))
            print(f"找到 base 值: {base_value}, lineHeight: {line_height}")
            break

    fixed_lines = []
    fixed_count = 0

    for line in lines:
        if line.startswith('char '):
            # 提取字符信息
            match_height = re.search(r'height=(\d+)', line)
            if match_height:
                height = int(match_height.group(1))

                # 对于 jMonkeyEngine 的 BitmapFont:
                # yoffset 应该让字符的底部对齐到基线
                # yoffset = lineHeight - base - height
                # 这样字符会从正确的位置开始绘制
                correct_yoffset = line_height - base_value - height

                # 替换 yoffset
                new_line = re.sub(r'yoffset=-?\d+', f'yoffset={correct_yoffset}', line)
                fixed_lines.append(new_line)
                fixed_count += 1
            else:
                fixed_lines.append(line)
        else:
            fixed_lines.append(line)

    # 写入修复后的文件
    with open(output_file, 'w', encoding='utf-8') as f:
        f.writelines(fixed_lines)

    print(f"修复完成！共修复 {fixed_count} 个字符")
    print(f"输出文件: {output_file}")

if __name__ == '__main__':
    input_file = 'ChineseFont_backup.fnt'  # 使用备份文件
    output_file = 'ChineseFont_fixed_v2.fnt'

    print("开始修复字体文件（版本2）...")
    fix_font_file(input_file, output_file)
    print("完成！")
