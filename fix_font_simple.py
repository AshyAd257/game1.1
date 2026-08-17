#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
完全重新生成 BMFont 配置文件
使用正确的参数
"""

import re

def fix_font_file(input_file, output_file):
    """修复字体文件"""

    with open(input_file, 'r', encoding='utf-8') as f:
        lines = f.readlines()

    fixed_lines = []

    for line in lines:
        if line.startswith('common'):
            # 修改 common 行，设置正确的 base 值
            # base 应该等于 lineHeight，这样字符会从顶部开始绘制
            line = re.sub(r'base=\d+', 'base=20', line)
            fixed_lines.append(line)
            print(f"修改后的 common 行: {line.strip()}")
        elif line.startswith('char '):
            # 对于所有字符，设置 yoffset = 0
            # 这样字符会从顶部开始绘制，不会有偏移
            line = re.sub(r'yoffset=-?\d+', 'yoffset=0', line)
            fixed_lines.append(line)
        else:
            fixed_lines.append(line)

    with open(output_file, 'w', encoding='utf-8') as f:
        f.writelines(fixed_lines)

    print(f"修复完成！输出文件: {output_file}")

if __name__ == '__main__':
    input_file = 'ChineseFont_backup.fnt'
    output_file = 'ChineseFont.fnt'

    print("开始修复字体文件（设置 base=lineHeight, yoffset=0）...")
    fix_font_file(input_file, output_file)
    print("完成！")
