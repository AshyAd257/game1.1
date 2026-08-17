import re

input_file = r"C:\Users\29232\OneDrive\Desktop\game1(1)\src\main\java\com\Hecate\puppet\editor\core\EditorPuppetPartRenderer.java"
output_file = input_file

with open(input_file, 'r', encoding='utf-8', errors='ignore') as f:
    content = f.read()

# Remove block comments with garbled text
def has_garbled(text):
    """Check if text contains garbled characters (non-ASCII that aren't valid Chinese)"""
    for char in text:
        if ord(char) > 127:
            # Check if it's a valid Chinese character range
            if not (0x4E00 <= ord(char) <= 0x9FFF or  # CJK Unified Ideographs
                    0x3400 <= ord(char) <= 0x4DBF or  # CJK Extension A
                    0x20000 <= ord(char) <= 0x2A6DF): # CJK Extension B
                return True
    return False

# Remove block comments /* */ that contain garbled text
def remove_garbled_block_comments(text):
    result = []
    i = 0
    while i < len(text):
        if i < len(text) - 1 and text[i:i+2] == '/*':
            # Find the end of block comment
            end = text.find('*/', i + 2)
            if end != -1:
                comment_content = text[i:end+2]
                if has_garbled(comment_content):
                    # Skip this block comment
                    i = end + 2
                    continue
                else:
                    result.append(comment_content)
                    i = end + 2
                    continue
        result.append(text[i])
        i += 1
    return ''.join(result)

# First remove garbled block comments
content = remove_garbled_block_comments(content)

# Process line by line for inline comments
lines = content.split('\n')
cleaned_lines = []

for line in lines:
    # Check if line is a standalone comment line (starts with // after whitespace)
    stripped = line.lstrip()
    if stripped.startswith('//'):
        # This is a standalone comment line
        if has_garbled(line):
            # Skip this line entirely
            continue
        else:
            # Keep clean comment line
            cleaned_lines.append(line)
    elif '//' in line:
        # This line has code followed by inline comment
        parts = line.split('//', 1)
        code_part = parts[0]
        comment_part = parts[1] if len(parts) > 1 else ''

        # Always keep the code part
        if has_garbled(comment_part):
            # Remove garbled inline comment, keep only code
            cleaned_lines.append(code_part.rstrip())
        else:
            # Keep both code and clean comment
            cleaned_lines.append(line)
    else:
        # No comment in this line, keep as is
        cleaned_lines.append(line)

# Write cleaned content
with open(output_file, 'w', encoding='utf-8') as f:
    f.write('\n'.join(cleaned_lines))

print(f"Encoding fix completed. Cleaned file saved to: {output_file}")
print(f"Processed {len(lines)} lines")
print(f"Output {len(cleaned_lines)} lines")
