#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""删除所有 System.out.println 语句（包括跨行的）"""

import os
import re

def clean_multiline_sysout(filepath):
    """彻底清理包括跨行的 System.out.println 语句"""
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()

        original_content = content

        # 模式1: 匹配完整的 System.out.println(...);
        # 包括跨行的情况
        pattern1 = r'System\.out\.println\s*\([^;]*?\)\s*;'
        content = re.sub(pattern1, '', content, flags=re.DOTALL)

        # 模式2: 匹配残留的 String.format 调用（通常是之前删除不完整的）
        # 查找孤立的字符串和参数列表
        lines = content.split('\n')
        cleaned_lines = []
        skip_until_semicolon = False

        for i, line in enumerate(lines):
            # 如果遇到了孤立的字符串字面量（带引号但前面没有赋值或调用）
            stripped = line.strip()

            # 检测残留的格式化字符串（通常以 " 开头，包含格式化占位符）
            if skip_until_semicolon:
                if ');' in line or '))' in line:
                    skip_until_semicolon = False
                continue

            # 检测孤立的格式化字符串开始
            if (stripped.startswith('"') and
                ('%s' in stripped or '%d' in stripped or '%f' in stripped or '%.2f' in stripped)):
                # 这可能是残留的 String.format 参数
                # 跳过直到找到结束的分号
                skip_until_semicolon = True
                continue

            # 检测孤立的参数行（通常是变量名后跟逗号或括号）
            if re.match(r'^\s+[a-zA-Z_][a-zA-Z0-9_]*(\.[a-zA-Z_][a-zA-Z0-9_]*)*\s*[,)]', stripped):
                # 检查前一行是否也是参数或格式字符串
                if i > 0:
                    prev_stripped = lines[i-1].strip()
                    if (prev_stripped.startswith('"') or
                        re.match(r'^\s+[a-zA-Z_][a-zA-Z0-9_]*', prev_stripped)):
                        skip_until_semicolon = True
                        continue

            cleaned_lines.append(line)

        content = '\n'.join(cleaned_lines)

        # 清理多余的空行（连续超过2个空行的情况）
        content = re.sub(r'\n\n\n+', '\n\n', content)

        # 只有内容变化时才写回
        if content != original_content:
            with open(filepath, 'w', encoding='utf-8') as f:
                f.write(content)
            return True
        return False

    except Exception as e:
        print(f"处理文件 {filepath} 时出错: {e}")
        return False

def main():
    src_dir = r'C:\Users\29232\OneDrive\Desktop\game1(1)\src\main\java'

    modified_count = 0
    total_files = 0

    for root, dirs, files in os.walk(src_dir):
        for file in files:
            if file.endswith('.java'):
                if file.endswith('.backup') or '.bak' in file or file.endswith('.new'):
                    continue

                filepath = os.path.join(root, file)
                total_files += 1

                if clean_multiline_sysout(filepath):
                    modified_count += 1
                    print(f"已清理: {filepath}")

    print(f"\n完成！共检查 {total_files} 个文件，修改了 {modified_count} 个文件")

if __name__ == '__main__':
    main()
