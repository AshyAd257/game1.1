import re

input_file = r"C:\Users\29232\OneDrive\Desktop\game1(1)\src\main\java\com\Hecate\puppet\editor\core\EditorPuppetPartRenderer.java"
output_file = input_file

# Read file in binary mode and decode with error handling
with open(input_file, 'rb') as f:
    content_bytes = f.read()

# Decode as UTF-8, replacing errors with replacement character
content = content_bytes.decode('utf-8', errors='replace')

# Function to check if text contains garbled characters
def has_garbled(text):
    """Check if text contains garbled characters (non-ASCII that aren't valid Chinese)"""
    for char in text:
        if ord(char) > 127:
            # Check if it's the Unicode replacement character
            if char == '\ufffd':
                return True
            # Check if it's outside valid Chinese character ranges
            if not (0x4E00 <= ord(char) <= 0x9FFF or  # CJK Unified Ideographs
                    0x3400 <= ord(char) <= 0x4DBF or  # CJK Extension A
                    0x20000 <= ord(char) <= 0x2A6DF): # CJK Extension B
                return True
    return False

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

        # Check if comment has garbled characters
        if has_garbled(comment_part):
            # Remove the comment, keep only the code part
            # Preserve the line if it has actual code
            if code_part.strip():
                cleaned_lines.append(code_part.rstrip())
            else:
                # This was a comment-only line, skip it
                continue
        else:
            # Comment is clean, keep the whole line
            cleaned_lines.append(line)
    elif '/*' in line or '*/' in line or '*' in line.lstrip()[:2]:
        # Check for block comments with garbled text
        if has_garbled(line):
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

print(f"Improved encoding fix completed. Cleaned file saved to: {output_file}")
print(f"Processed {len(lines)} lines")
print(f"Output {len(cleaned_lines)} lines")
print(f"Removed {len(lines) - len(cleaned_lines)} lines with garbled comments")
