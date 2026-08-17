import re

input_file = r"C:\Users\29232\OneDrive\Desktop\game1(1)\src\main\java\com\Hecate\puppet\editor\core\EditorPuppetPartRenderer.java"
output_file = input_file

# Read file in binary mode to see actual bytes
with open(input_file, 'rb') as f:
    content_bytes = f.read()

# Decode as UTF-8, replacing errors
content = content_bytes.decode('utf-8', errors='replace')

# Process line by line
lines = content.split('\n')
cleaned_lines = []

for line in lines:
    # Check if line contains a comment
    if '//' in line:
        # Split at the first // to separate code from comment
        parts = line.split('//', 1)
        code_part = parts[0]
        comment_part = parts[1] if len(parts) > 1 else ''

        # Check if comment has garbled characters (non-ASCII that aren't valid)
        has_garbled = False
        for char in comment_part:
            if ord(char) > 127:
                # Check if it's the Unicode replacement character or other garbled chars
                if char == '\ufffd' or ord(char) > 0x9FFF or (ord(char) < 0x4E00 and ord(char) > 127):
                    has_garbled = True
                    break

        if has_garbled:
            # Remove the comment, keep only the code part
            # But preserve the line if it has actual code
            if code_part.strip():
                cleaned_lines.append(code_part.rstrip())
            else:
                # This was a comment-only line, skip it
                continue
        else:
            # Comment is clean, keep the whole line
            cleaned_lines.append(line)
    elif '/*' in line or '*/' in line:
        # Check for block comments with garbled text
        has_garbled = any(ord(c) == 0xfffd or (ord(c) > 127 and (ord(c) > 0x9FFF or ord(c) < 0x4E00)) for c in line)
        if has_garbled:
            # Skip garbled block comment lines
            continue
        else:
            cleaned_lines.append(line)
    else:
        # No comment, keep as is
        cleaned_lines.append(line)

# Write cleaned content
with open(output_file, 'w', encoding='utf-8') as f:
    f.write('\n'.join(cleaned_lines))

print(f"Binary encoding fix completed. Cleaned file saved to: {output_file}")
print(f"Processed {len(lines)} lines")
print(f"Output {len(cleaned_lines)} lines")
print(f"Removed {len(lines) - len(cleaned_lines)} lines with garbled comments")
