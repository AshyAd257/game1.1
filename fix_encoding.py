import re

input_file = r"C:\Users\29232\OneDrive\Desktop\game1(1)\src\main\java\com\Hecate\puppet\editor\core\EditorPuppetPartRenderer.java"
output_file = input_file

with open(input_file, 'r', encoding='utf-8', errors='ignore') as f:
    lines = f.readlines()

cleaned_lines = []
for line in lines:
    if '//' in line:
        parts = line.split('//', 1)
        code_part = parts[0]
        comment_part = parts[1] if len(parts) > 1 else ''
        has_garbled = any(ord(c) > 127 for c in comment_part)
        if has_garbled:
            cleaned_lines.append(code_part.rstrip() + '\n')
        else:
            cleaned_lines.append(line)
    else:
        cleaned_lines.append(line)

with open(output_file, 'w', encoding='utf-8') as f:
    f.writelines(cleaned_lines)

print(f"Encoding fix completed. Cleaned file saved to: {output_file}")
