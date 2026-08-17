# -*- coding: utf-8 -*-
"""
BMFont Chinese Font Generator
使用 PIL/Pillow 生成 jMonkeyEngine 兼容的中文 BMFont 字体

依赖: pip install pillow
"""

from PIL import Image, ImageDraw, ImageFont
import os
import sys

class BMFontGenerator:
    def __init__(self, font_path, font_size=16, texture_size=1024):
        """
        初始化 BMFont 生成器

        Args:
            font_path: TTF字体文件路径
            font_size: 字体大小（像素）
            texture_size: 纹理图片大小（宽高相同）
        """
        self.font_path = font_path
        self.font_size = font_size
        self.texture_size = texture_size

        try:
            self.font = ImageFont.truetype(font_path, font_size)
        except Exception as e:
            print(f"Error loading font: {e}")
            sys.exit(1)

    def generate(self, chars_file, output_name):
        """
        生成 BMFont 字体文件

        Args:
            chars_file: 字符集文件路径（chars.txt）
            output_name: 输出文件名（不含扩展名）
        """
        # 读取字符集
        with open(chars_file, 'r', encoding='utf-8') as f:
            chars = f.read()

        print(f"Generating BMFont for {len(chars)} characters...")

        # 创建纹理图片
        texture = Image.new('RGBA', (self.texture_size, self.texture_size), (0, 0, 0, 0))
        draw = ImageDraw.Draw(texture)

        # 字符信息列表
        char_info = []

        # 当前位置
        x, y = 2, 2
        line_height = self.font_size + 4
        max_char_width = 0

        for char in chars:
            # 获取字符边界框
            bbox = draw.textbbox((0, 0), char, font=self.font)
            char_width = bbox[2] - bbox[0] + 4
            char_height = bbox[3] - bbox[1] + 4

            # 换行检查
            if x + char_width > self.texture_size - 2:
                x = 2
                y += line_height

                if y + line_height > self.texture_size:
                    print(f"Warning: Texture size too small! Stopped at character: {char}")
                    break

            # 绘制字符
            draw.text((x + 2, y + 2), char, font=self.font, fill=(255, 255, 255, 255))

            # 记录字符信息
            char_info.append({
                'char': char,
                'x': x,
                'y': y,
                'width': char_width,
                'height': char_height,
                'xoffset': 0,
                'yoffset': 0,
                'xadvance': char_width
            })

            x += char_width
            max_char_width = max(max_char_width, char_width)

        # 保存纹理图片
        texture_file = f"{output_name}_0.png"
        texture.save(texture_file)
        print(f"Saved texture: {texture_file}")

        # 生成 .fnt 文件
        self._generate_fnt_file(output_name, char_info, texture_file, line_height)

        print(f"BMFont generation complete!")
        print(f"Files created: {output_name}.fnt, {texture_file}")

    def _generate_fnt_file(self, output_name, char_info, texture_file, line_height):
        """生成 .fnt 描述文件"""
        fnt_file = f"{output_name}.fnt"

        with open(fnt_file, 'w', encoding='utf-8') as f:
            # 头部信息
            f.write(f"info face=\"{os.path.basename(self.font_path)}\" size={self.font_size} bold=0 italic=0 charset=\"\" unicode=1 stretchH=100 smooth=1 aa=1 padding=2,2,2,2 spacing=0,0\n")
            f.write(f"common lineHeight={line_height} base={self.font_size} scaleW={self.texture_size} scaleH={self.texture_size} pages=1 packed=0\n")
            f.write(f"page id=0 file=\"{texture_file}\"\n")
            f.write(f"chars count={len(char_info)}\n")

            # 字符信息
            for info in char_info:
                char_id = ord(info['char'])
                f.write(f"char id={char_id} x={info['x']} y={info['y']} width={info['width']} height={info['height']} xoffset={info['xoffset']} yoffset={info['yoffset']} xadvance={info['xadvance']} page=0 chnl=15\n")

            # Kerning（暂时不添加）
            f.write("kernings count=0\n")


def main():
    """主函数"""
    print("=" * 60)
    print("BMFont Chinese Font Generator for jMonkeyEngine")
    print("=" * 60)

    # 配置
    FONT_PATHS = [
        "C:/Windows/Fonts/msyh.ttc",      # 微软雅黑
        "C:/Windows/Fonts/simhei.ttf",    # 黑体
        "C:/Windows/Fonts/simsun.ttc",    # 宋体
    ]

    CHARS_FILE = "chars.txt"
    OUTPUT_NAME = "ChineseFont"
    FONT_SIZE = 16
    TEXTURE_SIZE = 1024

    # 查找可用字体
    font_path = None
    for path in FONT_PATHS:
        if os.path.exists(path):
            font_path = path
            print(f"Using font: {path}")
            break

    if not font_path:
        print("Error: No suitable font found!")
        print("Please install one of: Microsoft YaHei, SimHei, SimSun")
        sys.exit(1)

    # 检查字符集文件
    if not os.path.exists(CHARS_FILE):
        print(f"Error: {CHARS_FILE} not found!")
        print("Please run create_charset.py first")
        sys.exit(1)

    # 生成字体
    generator = BMFontGenerator(font_path, FONT_SIZE, TEXTURE_SIZE)
    generator.generate(CHARS_FILE, OUTPUT_NAME)

    print("\nNext steps:")
    print(f"1. Copy {OUTPUT_NAME}.fnt and {OUTPUT_NAME}_0.png to:")
    print("   src/main/resources/Interface/Fonts/")
    print("2. Restart the application to use the new font")


if __name__ == "__main__":
    main()
