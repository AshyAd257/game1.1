#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
修复 BMFont 字体文件的 yoffset 问题
"""

import re
import sys

def fix_font_file(input_file, output_file):
    """修复字体文件的 yoffset"""

    with open(input_file, 'r', encoding='utf-8') as f:
        lines = f.readlines()

    # 查找 base 值
    base_value = 16  # 默认值
    for line in lines:
        if line.startswith('common'):
            match = re.search(r'base=(\d+)', line)
            if match:
                base_value = int(match.group(1))
                print(f"找到 base 值: {base_value}")
                break

    fixed_lines = []
    fixed_count = 0

    for line in lines:
        if line.startswith('char '):
            # 提取字符信息
            match = re.search(r'height=(\d+)', line)
            if match:
                height = int(match.group(1))
                # 计算正确的 yoffset
                # yoffset 应该是 base - height，这样字符会正确对齐到基线
                correct_yoffset = base_value - height

                # 替换 yoffset
                new_line = re.sub(r'yoffset=\d+', f'yoffset={correct_yoffset}', line)
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
    input_file = 'ChineseFont.fnt'
    output_file = 'ChineseFont_fixed.fnt'

    print("开始修复字体文件...")
    fix_font_file(input_file, output_file)
    print("完成！")
