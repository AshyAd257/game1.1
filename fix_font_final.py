#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
修复 BMFont 字体文件的 yoffset 问题 - 版本3
参考 jMonkeyEngine 默认字体的格式
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

                # 根据 jMonkeyEngine 默认字体的规律:
                # base=26, 大多数字符 height=16-17, yoffset=3
                # 所以 yoffset = base - height (大约)
                # 但我们的 base=16, 所以需要调整
                # 对于 height=16 的字符, yoffset 应该接近 0
                # 对于 height=19 的字符, yoffset 应该是负数

                # 简单公式: yoffset = base - height
                # 这样 height=16 时 yoffset=0
                # height=19 时 yoffset=-3
                correct_yoffset = base_value - height

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
    output_file = 'ChineseFont.fnt'  # 直接覆盖

    print("开始修复字体文件（版本3 - 最终版）...")
    fix_font_file(input_file, output_file)
    print("完成！")
