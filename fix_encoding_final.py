import re

input_file = r"C:\Users\29232\OneDrive\Desktop\game1(1)\src\main\java\com\Hecate\puppet\editor\core\EditorPuppetPartRenderer.java"
output_file = input_file

# Read file as Latin-1 to preserve all bytes
with open(input_file, 'r', encoding='latin-1') as f:
    content = f.read()

# Remove all non-ASCII characters from comments
lines = content.split('\n')
cleaned_lines = []

for line in lines:
    # Check if line contains a comment
    if '//' in line:
        # Split at the first // to separate code from comment
        parts = line.split('//', 1)
        code_part = parts[0]
        comment_part = parts[1] if len(parts) > 1 else ''

        # Remove all non-ASCII characters from comment
        clean_comment = ''.join(c if ord(c) < 128 else ' ' for c in comment_part)

        # Reconstruct line
        if clean_comment.strip():
            cleaned_lines.append(code_part + '//' + clean_comment)
        else:
            cleaned_lines.append(code_part.rstrip())
    elif '/*' in line or '*/' in line or (line.strip().startswith('*') and not line.strip().startswith('*/')):
        # Block comment line - remove all non-ASCII characters
        clean_line = ''.join(c if ord(c) < 128 else ' ' for c in line)
        cleaned_lines.append(clean_line)
    else:
        # No comment, keep as is
        cleaned_lines.append(line)

# Write cleaned content as UTF-8
with open(output_file, 'w', encoding='utf-8') as f:
    f.write('\n'.join(cleaned_lines))

print(f"Final encoding fix completed.")
print(f"Processed {len(lines)} lines")
print(f"Output {len(cleaned_lines)} lines")
