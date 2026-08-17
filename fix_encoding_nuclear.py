import re

input_file = r"C:\Users\29232\OneDrive\Desktop\game1(1)\src\main\java\com\Hecate\puppet\editor\core\EditorPuppetPartRenderer.java"
output_file = input_file

# Read file in binary mode
with open(input_file, 'rb') as f:
    content_bytes = bytearray(f.read())

# Nuclear option: Remove ALL non-ASCII bytes from the entire file
# This will remove all Chinese comments but preserve Java code
result = bytearray()

for byte in content_bytes:
    if byte < 128:  # Only keep ASCII characters
        result.append(byte)
    else:
        # Replace non-ASCII with space to preserve line structure
        result.append(ord(' '))

# Write the cleaned bytes
with open(output_file, 'wb') as f:
    f.write(result)

print(f"Nuclear encoding fix completed.")
print(f"Processed {len(content_bytes)} bytes")
print(f"Replaced {sum(1 for b in content_bytes if b >= 128)} non-ASCII bytes with spaces")
