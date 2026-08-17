import re

input_file = r"C:\Users\29232\OneDrive\Desktop\game1(1)\src\main\java\com\Hecate\puppet\editor\core\EditorPuppetPartRenderer.java.backup"
output_file = r"C:\Users\29232\OneDrive\Desktop\game1(1)\src\main\java\com\Hecate\puppet\editor\core\EditorPuppetPartRenderer.java"

# Read file as Latin-1 to preserve all bytes
with open(input_file, 'r', encoding='latin-1') as f:
    content = f.read()

# Remove all block comments /* ... */
content = re.sub(r'/\*.*?\*/', '', content, flags=re.DOTALL)

# Remove all line comments //...
lines = content.split('\n')
cleaned_lines = []

for line in lines:
    # Find // and remove everything after it
    if '//' in line:
        # Split at the first // to separate code from comment
        code_part = line.split('//')[0]
        cleaned_lines.append(code_part.rstrip())
    else:
        cleaned_lines.append(line)

# Write cleaned content as UTF-8
with open(output_file, 'w', encoding='utf-8') as f:
    f.write('\n'.join(cleaned_lines))

print(f"Stripped all comments from file.")
print(f"Processed {len(lines)} lines")
print(f"Output {len(cleaned_lines)} lines")
