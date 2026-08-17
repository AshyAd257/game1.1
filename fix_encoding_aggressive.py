import re

input_file = r"C:\Users\29232\OneDrive\Desktop\game1(1)\src\main\java\com\Hecate\puppet\editor\core\EditorPuppetPartRenderer.java"
output_file = input_file

# Read file in binary mode
with open(input_file, 'rb') as f:
    content_bytes = bytearray(f.read())

# Process byte by byte, tracking if we're in a comment
result = bytearray()
i = 0
in_line_comment = False
in_block_comment = False

while i < len(content_bytes):
    byte = content_bytes[i]

    # Check for line comment start
    if not in_block_comment and i < len(content_bytes) - 1:
        if content_bytes[i] == ord('/') and content_bytes[i+1] == ord('/'):
            in_line_comment = True
            result.append(byte)
            i += 1
            continue

    # Check for block comment start
    if not in_line_comment and i < len(content_bytes) - 1:
        if content_bytes[i] == ord('/') and content_bytes[i+1] == ord('*'):
            in_block_comment = True
            result.append(byte)
            i += 1
            continue

    # Check for block comment end
    if in_block_comment and i < len(content_bytes) - 1:
        if content_bytes[i] == ord('*') and content_bytes[i+1] == ord('/'):
            in_block_comment = False
            result.append(byte)
            i += 1
            result.append(content_bytes[i])
            i += 1
            continue

    # Check for line comment end (newline)
    if in_line_comment and byte == ord('\n'):
        in_line_comment = False
        result.append(byte)
        i += 1
        continue

    # If we're in a comment and byte is non-ASCII, replace with space
    if (in_line_comment or in_block_comment) and byte > 127:
        result.append(ord(' '))
        i += 1
        continue

    # Otherwise, keep the byte as is
    result.append(byte)
    i += 1

# Write the cleaned bytes
with open(output_file, 'wb') as f:
    f.write(result)

print(f"Aggressive encoding fix completed.")
print(f"Processed {len(content_bytes)} bytes")
print(f"Output {len(result)} bytes")
print(f"Replaced {len(content_bytes) - len(result)} non-ASCII bytes in comments with spaces")
