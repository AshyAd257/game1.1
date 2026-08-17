#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""删除所有 System.out.println 语句"""

import os
import re

def remove_sysout_from_file(filepath):
    """从单个文件中删除 System.out.println"""
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            lines = f.readlines()

        # 过滤掉包含 System.out.println 的行（包括注释掉的）
        new_lines = []
        for line in lines:
            # 跳过包含 System.out.println 的行
            if 'System.out.println' not in line:
                new_lines.append(line)

        # 只有内容变化时才写回文件
        if len(new_lines) != len(lines):
            with open(filepath, 'w', encoding='utf-8') as f:
                f.writelines(new_lines)
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
                # 跳过备份文件
                if file.endswith('.backup') or '.bak' in file or file.endswith('.new'):
                    continue

                filepath = os.path.join(root, file)
                total_files += 1

                if remove_sysout_from_file(filepath):
                    modified_count += 1
                    print(f"已清理: {filepath}")

    print(f"\n完成！共检查 {total_files} 个文件，修改了 {modified_count} 个文件")

if __name__ == '__main__':
    main()
