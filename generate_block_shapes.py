# -*- coding: utf-8 -*-
"""
方块形状原型生成器 - 生成 cube/wedge/halfbrick 三种基础方块模型（glTF二进制/.glb）
以及对应的十字形UV展开参考模板图（PNG），供美术直接在模板上画贴图。

背景：项目里现有的 wood1.glb 用的是"打包式"UV（六个面的矩形硬塞进0-1方格，
紧凑但难以对照着图分辨哪块贴图对应哪个面）。这次改用美术更熟悉的"十字网展开"
布局：正面在中间，上/下/左/右面绕着正面展开成十字形，背面单独放在旁边，
每个面在图上的位置和名字一一对应，方便手绘。

三个模型是"形状原型"（shape prototype），不是某个具体材质的方块（不像wood1
是"木头方块"这种具体品种）——将来会有很多不同材质/用途的方块复用这三种基础
几何形状（不同的贴图、不同的方块ID），所以文件名不带数字后缀，直接用形状本身
命名：cube / wedge / halfbrick。

依赖: pip install pillow（生成UV模板图需要）
用法: python generate_block_shapes.py
输出: src/main/resources/blocks/{cube,wedge,halfbrick}.glb
      src/main/resources/blocks/{cube,wedge,halfbrick}_uv_template.png
"""

import json
import math
import struct
import os
import sys
from PIL import Image, ImageDraw, ImageFont

OUTPUT_DIR = os.path.join("src", "main", "resources", "blocks")

# UV模板图参数
# 十字网格每一列/每一行的像素尺寸必须按"面在3D里的真实世界尺寸"分配，不能用统一网格：
# cube六个面确实都是1x1正方形，用统一网格没问题；但halfbrick的侧面是1(宽)x0.5(高)的
# 长方形，wedge的斜面是1(宽)x√2(斜长)的长方形——如果照样塞进统一的正方形格子，
# 画上去的图案贴到模型上会被拉伸/压扁变形。所以改用"每列/每行独立尺寸"的布局系统
# （见下方 make_layout / layout_rect），按世界单位换算成像素，保证每个面的贴图区域
# 长宽比和它在3D里的真实长宽比完全一致。
UNIT_PX = 128  # 每1个世界单位（=1个方块边长）对应的像素数
UV_MARGIN_COLOR = (30, 30, 30, 255)       # 图片背景（UV空间之外/未使用区域）
UV_CHECKER_A = (210, 210, 210, 255)       # 棋盘格浅色
UV_CHECKER_B = (170, 170, 170, 255)       # 棋盘格深色
UV_GRID_LINE_COLOR = (255, 60, 60, 255)   # 面之间的分割网格线
UV_LABEL_COLOR = (20, 20, 20, 255)
CHECKER_CELLS = 8  # 每个面内部棋盘格细分数（仅用于视觉参考，帮助判断贴图方向/比例）


# ============================================================
# glTF 2.0 (.glb) 二进制容器手工构造
#
# 项目环境里没有 Blender 也没有 pygltflib，所以直接按 glTF 2.0
# 规范手工拼二进制容器：一个12字节文件头 + JSON chunk + BIN chunk。
# 数据本身很简单（几十个顶点的静态网格），不需要依赖第三方库。
# ============================================================

GLB_MAGIC = 0x46546C67          # "glTF"
GLB_VERSION = 2
CHUNK_TYPE_JSON = 0x4E4F534A    # "JSON"
CHUNK_TYPE_BIN = 0x004E4942     # "BIN\0"


def _pad4(data: bytes, pad_byte: bytes = b"\x20") -> bytes:
    """glTF chunk必须4字节对齐；JSON chunk用空格补齐，BIN chunk用\\0补齐"""
    remainder = len(data) % 4
    if remainder == 0:
        return data
    return data + pad_byte * (4 - remainder)


def build_glb(positions, normals, uvs, indices) -> bytes:
    """
    从顶点数据构造一个最小可用的.glb文件：单个mesh/单个primitive，
    使用一个不带贴图的默认PBR材质（贴图由美术后续在jME材质或外部工具里另行指定，
    这批模型本身只提供几何形状+UV，不预设任何颜色/贴图）。

    positions: List[Tuple[float,float,float]]
    normals:   List[Tuple[float,float,float]]（与positions等长，逐顶点法线）
    uvs:       List[Tuple[float,float]]（与positions等长，逐顶点UV，V轴已转换为glTF约定：0=顶部）
    indices:   List[int]（三角形列表，长度是3的倍数）
    """
    assert len(positions) == len(normals) == len(uvs)

    pos_bytes = b"".join(struct.pack("<3f", *v) for v in positions)
    norm_bytes = b"".join(struct.pack("<3f", *v) for v in normals)
    uv_bytes = b"".join(struct.pack("<2f", *v) for v in uvs)

    # 索引：顶点数不多（最多几十个），全部用无符号short(5123)即可
    idx_bytes = b"".join(struct.pack("<H", i) for i in indices)
    idx_bytes = _pad4(idx_bytes, b"\x00")

    # BIN chunk内部各段的偏移（4字节对齐，索引段已在上面补齐，其余三段长度本身就是12/12/8的倍数）
    pos_offset = 0
    norm_offset = pos_offset + len(pos_bytes)
    uv_offset = norm_offset + len(norm_bytes)
    idx_offset = uv_offset + len(uv_bytes)
    bin_chunk = pos_bytes + norm_bytes + uv_bytes + idx_bytes

    xs = [p[0] for p in positions]
    ys = [p[1] for p in positions]
    zs = [p[2] for p in positions]

    gltf = {
        "asset": {"version": "2.0", "generator": "generate_block_shapes.py"},
        "scene": 0,
        "scenes": [{"nodes": [0]}],
        "nodes": [{"mesh": 0, "name": "BlockShape"}],
        "meshes": [{
            "name": "BlockShape",
            "primitives": [{
                "attributes": {"POSITION": 0, "NORMAL": 1, "TEXCOORD_0": 2},
                "indices": 3,
                "material": 0,
                "mode": 4,  # TRIANGLES
            }]
        }],
        "materials": [{
            "name": "BlockShapeDefault",
            "pbrMetallicRoughness": {
                "baseColorFactor": [1.0, 1.0, 1.0, 1.0],
                "metallicFactor": 0.0,
                "roughnessFactor": 1.0,
            },
            "doubleSided": False,
        }],
        "buffers": [{"byteLength": len(bin_chunk)}],
        "bufferViews": [
            {"buffer": 0, "byteOffset": pos_offset, "byteLength": len(pos_bytes), "target": 34962},
            {"buffer": 0, "byteOffset": norm_offset, "byteLength": len(norm_bytes), "target": 34962},
            {"buffer": 0, "byteOffset": uv_offset, "byteLength": len(uv_bytes), "target": 34962},
            {"buffer": 0, "byteOffset": idx_offset, "byteLength": len(idx_bytes), "target": 34963},
        ],
        "accessors": [
            {"bufferView": 0, "componentType": 5126, "count": len(positions), "type": "VEC3",
             "min": [min(xs), min(ys), min(zs)], "max": [max(xs), max(ys), max(zs)]},
            {"bufferView": 1, "componentType": 5126, "count": len(normals), "type": "VEC3"},
            {"bufferView": 2, "componentType": 5126, "count": len(uvs), "type": "VEC2"},
            {"bufferView": 3, "componentType": 5123, "count": len(indices), "type": "SCALAR"},
        ],
    }

    json_bytes = _pad4(json.dumps(gltf, separators=(",", ":")).encode("utf-8"), b"\x20")

    header = struct.pack("<III", GLB_MAGIC, GLB_VERSION, 0)  # total length填0，最后回填
    json_chunk_header = struct.pack("<II", len(json_bytes), CHUNK_TYPE_JSON)
    bin_chunk_header = struct.pack("<II", len(bin_chunk), CHUNK_TYPE_BIN)

    body = json_chunk_header + json_bytes + bin_chunk_header + bin_chunk
    total_length = 12 + len(body)
    header = struct.pack("<III", GLB_MAGIC, GLB_VERSION, total_length)

    return header + body


# ============================================================
# 几何体定义
#
# 坐标系约定（与WorldModule.java保持一致）：
#   - Y轴 = 竖直向上
#   - 1个方块 = 1个单位立方体，中心在原点，占据 [-0.5, 0.5]^3
#   - 模型加载后WorldModule会按高度自动归一化缩放到1格高，所以这里
#     直接按标准1x1x1尺寸建模即可，不需要额外考虑缩放
# ============================================================

def face(v0, v1, v2, v3, uv0, uv1, uv2, uv3, normal):
    """
    构造一个四边形面（两个三角形，顶点顺序保证法线朝外/逆时针）。
    返回 (positions, normals, uvs, local_indices=[0,1,2,0,2,3])
    """
    positions = [v0, v1, v2, v3]
    normals = [normal] * 4
    uvs = [uv0, uv1, uv2, uv3]
    local_indices = [0, 1, 2, 0, 2, 3]
    return positions, normals, uvs, local_indices


def build_mesh(faces):
    """把多个face()的结果合并成一整个mesh的顶点/索引数组"""
    positions, normals, uvs, indices = [], [], [], []
    for f_positions, f_normals, f_uvs, f_local_indices in faces:
        base = len(positions)
        positions.extend(f_positions)
        normals.extend(f_normals)
        uvs.extend(f_uvs)
        indices.extend(base + i for i in f_local_indices)
    return positions, normals, uvs, indices


# ---- 十字形UV展开：按每个面在3D中的真实世界尺寸分配像素区域 ----
# 布局（对应贴图上从左到右/从上到下的十字形网，形状不同、行高列宽也不同）：
#         [ TOP/SLOPE ]
# [LEFT ] [ FRONT/空  ] [RIGHT] [BACK]
#         [ BOTTOM    ]
#
# 每个形状单独定义各面的像素矩形（px_x, px_y, px_w, px_h），宽高直接由"世界单位 x UNIT_PX"
# 算出，保证贴图区域的长宽比和面在3D里的真实长宽比一致，不会拉伸变形。

def px_rect_to_uv(px_rect, canvas_w, canvas_h):
    """
    把像素矩形 (px_x, px_y, px_w, px_h) 转换成四角UV坐标（glTF约定：V=0在图片顶部）。
    顶点顺序：左下、右下、右上、左上（对应face()里v0..v3的常规四边形顺序）。
    """
    x, y, w, h = px_rect
    u0, u1 = x / canvas_w, (x + w) / canvas_w
    v_top, v_bottom = y / canvas_h, (y + h) / canvas_h
    return (u0, v_bottom), (u1, v_bottom), (u1, v_top), (u0, v_top)


class CrossLayout:
    """
    十字网格布局：列宽/行高按"世界单位"指定（不是固定格子数），每个面按自己在3D里的
    真实宽/高领取对应比例的像素区域，保证长宽比一致、不拉伸变形。
    col_widths/row_heights: 世界单位列表，元素个数=列数/行数。
    """
    def __init__(self, col_widths, row_heights):
        self.col_widths = col_widths
        self.row_heights = row_heights
        self.col_offsets = [sum(col_widths[:i]) * UNIT_PX for i in range(len(col_widths))]
        self.row_offsets = [sum(row_heights[:i]) * UNIT_PX for i in range(len(row_heights))]
        self.canvas_w = sum(col_widths) * UNIT_PX
        self.canvas_h = sum(row_heights) * UNIT_PX

    def cell_px(self, col, row, colspan=1, rowspan=1):
        """返回某个格子（可跨列/跨行）的像素矩形 (x, y, w, h)"""
        x = self.col_offsets[col]
        y = self.row_offsets[row]
        w = sum(self.col_widths[col:col + colspan]) * UNIT_PX
        h = sum(self.row_heights[row:row + rowspan]) * UNIT_PX
        return (x, y, w, h)

    def cell_uv(self, col, row, colspan=1, rowspan=1):
        """返回某个格子的四角UV坐标（左下/右下/右上/左上），供face()/tri_face()直接使用"""
        return px_rect_to_uv(self.cell_px(col, row, colspan, rowspan), self.canvas_w, self.canvas_h)

    def cell_uv_crop(self, col, row, colspan=1, rowspan=1, u_frac=(0.0, 1.0), v_frac=(0.0, 1.0)):
        """
        返回某个格子内部按比例裁切出的一小块区域的四角UV坐标。
        v_frac=(0.0,1.0)代表格子内部从"世界空间的上边"到"下边"的纵向裁切范围
        （0.0=格子顶部=世界坐标更高的一侧，1.0=格子底部=世界坐标更低的一侧，
        与cell_px()内部y轴增长方向一致）。
        u_frac=(0.0,1.0)是同样含义的横向裁切范围（0.0=格子左边，1.0=格子右边）。
        两者可以同时使用（比如竖放半砖裁横向的同时，横放半砖裁纵向的，是同一机制
        在不同轴上的应用），默认都是(0.0,1.0)即不裁切，等价于cell_uv()。

        用途：让矮个子/瘦个子的面（比如半砖的侧面，只有完整方块对应边长的一半）
        复用与标准方块side完全同尺寸的方格、同样的贴图密度，只截取其中一部分来
        采样——而不是单独给它分配一块被压缩变形的小方格。这样美术画好标准方块的
        贴图后，半砖可以直接复用同一张图（类似Minecraft台阶复用完整方块贴图的做法）。
        """
        x, y, w, h = self.cell_px(col, row, colspan, rowspan)
        x0 = x + u_frac[0] * w
        x1 = x + u_frac[1] * w
        y0 = y + v_frac[0] * h
        y1 = y + v_frac[1] * h
        u0, u1 = x0 / self.canvas_w, x1 / self.canvas_w
        v_top, v_bottom = y0 / self.canvas_h, y1 / self.canvas_h
        return (u0, v_bottom), (u1, v_bottom), (u1, v_top), (u0, v_top)


def cube_layout():
    """cube六个面全是1x1正方形，四列四行都是1.0世界单位宽/高"""
    return CrossLayout(col_widths=[1.0, 1.0, 1.0, 1.0], row_heights=[1.0, 1.0, 1.0])


def build_cube():
    """
    标准1x1x1方块。六个面各占十字网一格：
    行0=TOP，行1=LEFT/FRONT/RIGHT/BACK，行2=BOTTOM
    """
    h = 0.5
    layout = cube_layout()
    faces = []

    # FRONT (Z+)  col1,row1
    uv = layout.cell_uv(1, 1)
    faces.append(face((-h, -h, h), (h, -h, h), (h, h, h), (-h, h, h), *uv, (0, 0, 1)))
    # RIGHT (X+)  col2,row1
    uv = layout.cell_uv(2, 1)
    faces.append(face((h, -h, h), (h, -h, -h), (h, h, -h), (h, h, h), *uv, (1, 0, 0)))
    # BACK (Z-)  col3,row1
    uv = layout.cell_uv(3, 1)
    faces.append(face((h, -h, -h), (-h, -h, -h), (-h, h, -h), (h, h, -h), *uv, (0, 0, -1)))
    # LEFT (X-)  col0,row1
    uv = layout.cell_uv(0, 1)
    faces.append(face((-h, -h, -h), (-h, -h, h), (-h, h, h), (-h, h, -h), *uv, (-1, 0, 0)))
    # TOP (Y+)  col1,row0
    uv = layout.cell_uv(1, 0)
    faces.append(face((-h, h, h), (h, h, h), (h, h, -h), (-h, h, -h), *uv, (0, 1, 0)))
    # BOTTOM (Y-)  col1,row2
    uv = layout.cell_uv(1, 2)
    faces.append(face((-h, -h, -h), (h, -h, -h), (h, -h, h), (-h, -h, h), *uv, (0, -1, 0)))

    return build_mesh(faces), layout


def halfbrick_layout():
    """
    与cube_layout()完全相同的网格尺寸（同样的列宽/行高/画布分辨率）——半砖不单独设计
    一套被压缩的贴图区域，而是让侧面复用跟标准方块side完全同尺寸的方格，只截取其中
    下半部分来采样（半砖几何是y∈[-0.5,0]，正好是完整方块[-0.5,0.5]范围的下半段）。
    这样美术画好标准方块的贴图后，半砖可以直接复用同一张图，不需要单独画一张按比例
    缩小/拉伸的侧面贴图。
    """
    return cube_layout()


def build_halfbrick():
    """
    1 x 0.5 x 1 半砖：横截面和标准方块一样（1x1），高度减半，底面贴地（顶面从0.5降到0），
    与WorldModule按"整体高度归一化缩放到1格"的假设保持一致——半砖本身几何比例就是矮的，
    加载后会被等比缩放，但相对比例（宽:高:深 = 1:0.5:1）保持不变，视觉上仍是"半高"效果。
    Y方向范围 [-0.5, 0.0]（贴着方块下沿放置，模拟"半砖垫在地上"的常见摆放方式）。

    侧面贴图：不单独压缩成矮长方形区域，而是复用与标准方块side完全同尺寸的方格，
    只截取该方格的下半部分（v_frac=(0.5,1.0)）——半砖的Y范围[-0.5,0]正好是完整方块
    Y范围[-0.5,0.5]的下半段，所以截取下半区域在空间对应关系上是准确的，画面细节
    密度也和标准方块完全一致，可以直接复用同一张贴图。
    """
    half_xz = 0.5
    y_bottom, y_top = -0.5, 0.0
    layout = halfbrick_layout()
    side_v_frac = (0.5, 1.0)  # 截取方格下半部分（对应世界Y从0降到-0.5）
    faces = []

    uv = layout.cell_uv_crop(1, 1, v_frac=side_v_frac)
    faces.append(face((-half_xz, y_bottom, half_xz), (half_xz, y_bottom, half_xz),
                       (half_xz, y_top, half_xz), (-half_xz, y_top, half_xz), *uv, (0, 0, 1)))
    uv = layout.cell_uv_crop(2, 1, v_frac=side_v_frac)
    faces.append(face((half_xz, y_bottom, half_xz), (half_xz, y_bottom, -half_xz),
                       (half_xz, y_top, -half_xz), (half_xz, y_top, half_xz), *uv, (1, 0, 0)))
    uv = layout.cell_uv_crop(3, 1, v_frac=side_v_frac)
    faces.append(face((half_xz, y_bottom, -half_xz), (-half_xz, y_bottom, -half_xz),
                       (-half_xz, y_top, -half_xz), (half_xz, y_top, -half_xz), *uv, (0, 0, -1)))
    uv = layout.cell_uv_crop(0, 1, v_frac=side_v_frac)
    faces.append(face((-half_xz, y_bottom, -half_xz), (-half_xz, y_bottom, half_xz),
                       (-half_xz, y_top, half_xz), (-half_xz, y_top, -half_xz), *uv, (-1, 0, 0)))
    uv = layout.cell_uv(1, 0)
    faces.append(face((-half_xz, y_top, half_xz), (half_xz, y_top, half_xz),
                       (half_xz, y_top, -half_xz), (-half_xz, y_top, -half_xz), *uv, (0, 1, 0)))
    uv = layout.cell_uv(1, 2)
    faces.append(face((-half_xz, y_bottom, -half_xz), (half_xz, y_bottom, -half_xz),
                       (half_xz, y_bottom, half_xz), (-half_xz, y_bottom, half_xz), *uv, (0, -1, 0)))

    return build_mesh(faces), layout


def tri_face(v0, v1, v2, uv0, uv1, uv2, normal):
    """三角面（wedge的两个侧面用），顶点顺序需保证法线朝外（右手定则，v0->v1->v2逆时针）"""
    positions = [v0, v1, v2]
    normals = [normal] * 3
    uvs = [uv0, uv1, uv2]
    local_indices = [0, 1, 2]
    return positions, normals, uvs, local_indices


# ============================================================
# 半砖（slab）多朝向系统：沿X/Y/Z轴任一方向砍半，六种朝向复用同一套推导
# ============================================================

def _frac_up(lo, hi):
    """
    局部坐标区间[lo,hi]（取值范围在-0.5..0.5内）线性映射到UV的[0,1]裁剪比例，
    映射方向：世界坐标增大 -> 比例增大。lo/hi对应cell_uv_crop的frac[0]/frac[1]（较小值/较大值）。
    """
    return (lo + 0.5, hi + 0.5)


def _frac_down(lo, hi):
    """
    同_frac_up，但映射方向相反：世界坐标增大 -> 比例减小。
    用于该面自身在build_cube()里的UV顶点顺序恰好是反向参数化的情形（比如BACK面的x->u、
    RIGHT面的z->u——这是每个面自己的UV顶点顺序决定的固有结果，不是bug，必须如实保留，
    否则会导致贴图裁剪区间跟BOTTOM/TOP等直接映射的面对不齐）。
    """
    return (0.5 - hi, 0.5 - lo)


def build_slab(axis, sign):
    """
    生成沿axis轴砍半的半砖几何体+UV，复用cube_layout()画布（保证美术画的"一张完整方块
    贴图"可以不做任何修改直接套用在所有朝向的半砖上，叠满整格时两个互补朝向各显示半张，
    拼起来与cube.glb+同一张贴图完全等价）。

    axis: 'x'|'y'|'z' 决定沿哪个轴切割
    sign: -1 -> 局部坐标负半区[-0.5,0.0]；+1 -> 正半区[0.0,0.5]

    UV裁剪规则：法线平行于axis的两个"端盖"面（如axis='y'时的TOP/BOTTOM）代表半砖真正
    暴露在外的完整表面，不裁剪，直接用cube对应面的完整UV格子；另外4个"侧面"（法线垂直
    于axis）沿axis方向裁一半。裁剪落在U轴还是V轴、映射方向是UP还是DOWN，是每个面在
    build_cube()里的UV顶点顺序本身决定的固定属性（与axis/sign无关），已逐面手工核对：
      axis='y': FRONT/RIGHT/BACK/LEFT 四个侧面统一用DOWN映射裁v_frac
      axis='x': FRONT/TOP/BOTTOM用UP映射裁u_frac，BACK用DOWN映射裁u_frac
      axis='z': LEFT/TOP用UP映射（LEFT裁u_frac，TOP裁v_frac），RIGHT/BOTTOM用DOWN映射
                （RIGHT裁u_frac，BOTTOM裁v_frac）
    （build_slab('y',-1)生成的几何体/UV已用来跟既有的build_halfbrick()交叉验证完全一致，
    确认了上述推导的正确性。）
    """
    h = 0.5
    layout = cube_layout()
    lo, hi = (-h, 0.0) if sign < 0 else (0.0, h)
    faces = []

    if axis == 'y':
        # 端盖：TOP@hi，BOTTOM@lo，不裁剪
        uv = layout.cell_uv(1, 0)
        faces.append(face((-h, hi, h), (h, hi, h), (h, hi, -h), (-h, hi, -h), *uv, (0, 1, 0)))
        uv = layout.cell_uv(1, 2)
        faces.append(face((-h, lo, -h), (h, lo, -h), (h, lo, h), (-h, lo, h), *uv, (0, -1, 0)))

        # 侧面 FRONT/RIGHT/BACK/LEFT：均为DOWN映射，统一v_frac
        v_frac = _frac_down(lo, hi)
        uv = layout.cell_uv_crop(1, 1, v_frac=v_frac)
        faces.append(face((-h, lo, h), (h, lo, h), (h, hi, h), (-h, hi, h), *uv, (0, 0, 1)))
        uv = layout.cell_uv_crop(2, 1, v_frac=v_frac)
        faces.append(face((h, lo, h), (h, lo, -h), (h, hi, -h), (h, hi, h), *uv, (1, 0, 0)))
        uv = layout.cell_uv_crop(3, 1, v_frac=v_frac)
        faces.append(face((h, lo, -h), (-h, lo, -h), (-h, hi, -h), (h, hi, -h), *uv, (0, 0, -1)))
        uv = layout.cell_uv_crop(0, 1, v_frac=v_frac)
        faces.append(face((-h, lo, -h), (-h, lo, h), (-h, hi, h), (-h, hi, -h), *uv, (-1, 0, 0)))

    elif axis == 'x':
        # 端盖：LEFT@lo，RIGHT@hi，不裁剪
        uv = layout.cell_uv(0, 1)
        faces.append(face((lo, -h, -h), (lo, -h, h), (lo, h, h), (lo, h, -h), *uv, (-1, 0, 0)))
        uv = layout.cell_uv(2, 1)
        faces.append(face((hi, -h, h), (hi, -h, -h), (hi, h, -h), (hi, h, h), *uv, (1, 0, 0)))

        # 侧面：FRONT/TOP/BOTTOM为UP映射，BACK为DOWN映射，都裁u_frac
        u_up = _frac_up(lo, hi)
        u_down = _frac_down(lo, hi)
        uv = layout.cell_uv_crop(1, 1, u_frac=u_up)
        faces.append(face((lo, -h, h), (hi, -h, h), (hi, h, h), (lo, h, h), *uv, (0, 0, 1)))
        uv = layout.cell_uv_crop(3, 1, u_frac=u_down)
        faces.append(face((hi, -h, -h), (lo, -h, -h), (lo, h, -h), (hi, h, -h), *uv, (0, 0, -1)))
        uv = layout.cell_uv_crop(1, 0, u_frac=u_up)
        faces.append(face((lo, h, h), (hi, h, h), (hi, h, -h), (lo, h, -h), *uv, (0, 1, 0)))
        uv = layout.cell_uv_crop(1, 2, u_frac=u_up)
        faces.append(face((lo, -h, -h), (hi, -h, -h), (hi, -h, h), (lo, -h, h), *uv, (0, -1, 0)))

    else:  # axis == 'z'
        # 端盖：BACK@lo，FRONT@hi，不裁剪
        uv = layout.cell_uv(3, 1)
        faces.append(face((h, -h, lo), (-h, -h, lo), (-h, h, lo), (h, h, lo), *uv, (0, 0, -1)))
        uv = layout.cell_uv(1, 1)
        faces.append(face((-h, -h, hi), (h, -h, hi), (h, h, hi), (-h, h, hi), *uv, (0, 0, 1)))

        # 侧面：LEFT/TOP为UP映射，RIGHT/BOTTOM为DOWN映射；LEFT/RIGHT裁u_frac，TOP/BOTTOM裁v_frac
        frac_up = _frac_up(lo, hi)
        frac_down = _frac_down(lo, hi)
        uv = layout.cell_uv_crop(0, 1, u_frac=frac_up)
        faces.append(face((-h, -h, lo), (-h, -h, hi), (-h, h, hi), (-h, h, lo), *uv, (-1, 0, 0)))
        uv = layout.cell_uv_crop(2, 1, u_frac=frac_down)
        faces.append(face((h, -h, hi), (h, -h, lo), (h, h, lo), (h, h, hi), *uv, (1, 0, 0)))
        uv = layout.cell_uv_crop(1, 0, v_frac=frac_up)
        faces.append(face((-h, h, hi), (h, h, hi), (h, h, lo), (-h, h, lo), *uv, (0, 1, 0)))
        uv = layout.cell_uv_crop(1, 2, v_frac=frac_down)
        faces.append(face((-h, -h, lo), (h, -h, lo), (h, -h, hi), (-h, -h, hi), *uv, (0, -1, 0)))

    return build_mesh(faces), layout


# 六个朝向到(axis, sign)的映射，及对应输出文件名
SLAB_ORIENTATIONS = {
    "bottom": ("y", -1),
    "top": ("y", +1),
    "left": ("x", -1),
    "right": ("x", +1),
    "back": ("z", -1),
    "front": ("z", +1),
}


SLOPE_LENGTH = math.sqrt(2.0)  # 斜面真实斜长：从背面顶边斜切到底面前边缘，直角边均为1


def wedge_layout():
    """
    行0=SLOPE，行1=LEFT/BACK/RIGHT，行2=BOTTOM。
    关键修复：行0（SLOPE所在行）的行高必须是真实斜长sqrt(2)≈1.414世界单位，
    不能用"跨2个1.0行"（=2.0）去近似——2.0比真实值多拉长了41%，画上去的贴图
    会在模型上出现明显的纵向拉伸。行1/行2都是1x1正方形，维持1.0。
    """
    return CrossLayout(col_widths=[1.0, 1.0, 1.0, 1.0], row_heights=[SLOPE_LENGTH, 1.0, 1.0])


def build_wedge():
    """
    三角楔形方块：横截面（Y-Z平面）是直角等腰三角形，沿X轴挤出成柱体。
      - 底面（BOTTOM）：1x1正方形，y=-0.5
      - 背面（BACK）：1x1正方形，z=-0.5，从y=-0.5到y=0.5（"背面...是正常的方形"）
      - 斜面（SLOPE）：从背面顶边斜切到底面前边缘，是矩形，宽1x斜长sqrt(2)≈1.414，
        比其余1x1的面长，对应"正面是拉长的方形"
      - 左右两个侧面（LEFT/RIGHT）：直角边均为1的等腰直角三角形（"两个边为1x1的等腰三角形"）
    """
    h = 0.5
    # 六个唯一角点（用变量名标注几何位置，方便按面复用）
    bl_back, br_back = (-h, -h, -h), (h, -h, -h)   # 底面-后排左/右
    bl_front, br_front = (-h, -h, h), (h, -h, h)   # 底面-前排左/右
    tl_back, tr_back = (-h, h, -h), (h, h, -h)     # 背面顶边左/右

    layout = wedge_layout()
    faces = []

    # BOTTOM
    uv = layout.cell_uv(1, 2)
    faces.append(face(bl_back, br_back, br_front, bl_front, *uv, (0, -1, 0)))

    # BACK
    uv = layout.cell_uv(3, 1)
    faces.append(face(br_back, bl_back, tl_back, tr_back, *uv, (0, 0, -1)))

    # SLOPE（斜面，占单独一行，行高=真实斜长sqrt(2)，不再用跨行近似）
    uv = layout.cell_uv(1, 0)
    faces.append(face(bl_front, br_front, tr_back, tl_back, *uv, (0, 0.70710678, 0.70710678)))

    # LEFT三角面：直角顶点在后-下角，两条直角边分别指向前-下角和后-上角
    uv_bl, uv_br, uv_tr, uv_tl = layout.cell_uv(0, 1)
    faces.append(tri_face(bl_back, bl_front, tl_back, uv_bl, uv_br, uv_tl, (-1, 0, 0)))

    # RIGHT三角面：与LEFT镜像，直角顶点同样在后-下角
    uv_bl, uv_br, uv_tr, uv_tl = layout.cell_uv(2, 1)
    faces.append(tri_face(br_front, br_back, tr_back, uv_bl, uv_br, uv_tr, (1, 0, 0)))

    return build_mesh(faces), layout


# ============================================================
# UV参考模板图生成（PNG，供美术直接在上面画贴图）
# ============================================================

def _checkerboard(draw, x0, y0, x1, y1, cells=CHECKER_CELLS):
    """在矩形区域内画棋盘格，帮助判断贴图缩放比例和朝向"""
    cell_w = (x1 - x0) / cells
    cell_h = (y1 - y0) / cells
    for row in range(cells):
        for col in range(cells):
            color = UV_CHECKER_A if (row + col) % 2 == 0 else UV_CHECKER_B
            cx0 = x0 + col * cell_w
            cy0 = y0 + row * cell_h
            draw.rectangle([cx0, cy0, cx0 + cell_w, cy0 + cell_h], fill=color)


def _draw_label(draw, cx, cy, text, font):
    bbox = draw.textbbox((0, 0), text, font=font)
    tw, th = bbox[2] - bbox[0], bbox[3] - bbox[1]
    draw.text((cx - tw / 2, cy - th / 2), text, fill=UV_LABEL_COLOR, font=font)


def render_uv_template(layout, quad_faces, tri_faces, output_path):
    """
    layout: CrossLayout实例，提供该形状实际的画布像素尺寸（不同形状画布比例不同，
            不能再用固定的UV_IMAGE_WIDTH/HEIGHT）
    quad_faces: List[(label, (uv0,uv1,uv2,uv3))]  四边形面，uv为(u,v)四角坐标(0-1，V=0在顶部)
    tri_faces:  List[(label, (uv0,uv1,uv2))]      三角形面，uv为(u,v)三角坐标
    """
    width, height = round(layout.canvas_w), round(layout.canvas_h)
    img = Image.new("RGBA", (width, height), UV_MARGIN_COLOR)
    draw = ImageDraw.Draw(img)
    font = ImageFont.load_default(size=18)

    def to_px(uv):
        # U映射到宽度，V映射到高度——两者分辨率不同（4列x3行的画布本身就不是正方形），
        # 用同一个size同时缩放U/V会把正方形格子拉扁，必须分开算
        return (uv[0] * width, uv[1] * height)

    for label, corners in quad_faces:
        pts = [to_px(c) for c in corners]
        xs = [p[0] for p in pts]
        ys = [p[1] for p in pts]
        x0, x1 = min(xs), max(xs)
        y0, y1 = min(ys), max(ys)
        _checkerboard(draw, x0, y0, x1, y1)
        draw.rectangle([x0, y0, x1, y1], outline=UV_GRID_LINE_COLOR, width=2)
        _draw_label(draw, (x0 + x1) / 2, (y0 + y1) / 2, label, font)

    for label, corners in tri_faces:
        pts = [to_px(c) for c in corners]
        # 棋盘格填充：先在三角形的外接矩形区域画棋盘格，再用多边形蒙版裁剪出三角形区域
        xs = [p[0] for p in pts]
        ys = [p[1] for p in pts]
        x0, x1 = min(xs), max(xs)
        y0, y1 = min(ys), max(ys)
        mask = Image.new("L", (width, height), 0)
        mask_draw = ImageDraw.Draw(mask)
        mask_draw.polygon(pts, fill=255)
        checker_layer = Image.new("RGBA", (width, height), (0, 0, 0, 0))
        checker_draw = ImageDraw.Draw(checker_layer)
        _checkerboard(checker_draw, x0, y0, x1, y1)
        img.paste(checker_layer, (0, 0), mask)
        draw.polygon(pts, outline=UV_GRID_LINE_COLOR, width=2)
        centroid = (sum(xs) / 3, sum(ys) / 3)
        _draw_label(draw, centroid[0], centroid[1], label, font)

    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    img.save(output_path)


# ============================================================
# 主流程
# ============================================================

def write_glb(mesh_data, output_path):
    positions, normals, uvs, indices = mesh_data
    glb_bytes = build_glb(positions, normals, uvs, indices)
    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    with open(output_path, "wb") as f:
        f.write(glb_bytes)
    print(f"  写出模型: {output_path} ({len(positions)}顶点, {len(indices)//3}三角面, {len(glb_bytes)}字节)")


def main():
    print("生成方块形状原型: cube / wedge / halfbrick")

    # ---- cube ----
    cube_mesh, layout = build_cube()
    write_glb(cube_mesh, os.path.join(OUTPUT_DIR, "cube.glb"))
    render_uv_template(
        layout,
        quad_faces=[
            ("TOP", layout.cell_uv(1, 0)),
            ("BOTTOM", layout.cell_uv(1, 2)),
            ("FRONT", layout.cell_uv(1, 1)),
            ("BACK", layout.cell_uv(3, 1)),
            ("LEFT", layout.cell_uv(0, 1)),
            ("RIGHT", layout.cell_uv(2, 1)),
        ],
        tri_faces=[],
        output_path=os.path.join(OUTPUT_DIR, "cube_uv_template.png"),
    )

    # ---- halfbrick ----
    halfbrick_mesh, layout = build_halfbrick()
    write_glb(halfbrick_mesh, os.path.join(OUTPUT_DIR, "halfbrick.glb"))
    render_uv_template(
        layout,
        quad_faces=[
            ("TOP", layout.cell_uv(1, 0)),
            ("BOTTOM", layout.cell_uv(1, 2)),
            ("FRONT", layout.cell_uv(1, 1)),
            ("BACK", layout.cell_uv(3, 1)),
            ("LEFT", layout.cell_uv(0, 1)),
            ("RIGHT", layout.cell_uv(2, 1)),
        ],
        tri_faces=[],
        output_path=os.path.join(OUTPUT_DIR, "halfbrick_uv_template.png"),
    )

    # ---- wedge ----
    wedge_mesh, layout = build_wedge()
    write_glb(wedge_mesh, os.path.join(OUTPUT_DIR, "wedge.glb"))
    left_uv = layout.cell_uv(0, 1)
    right_uv = layout.cell_uv(2, 1)
    render_uv_template(
        layout,
        quad_faces=[
            ("SLOPE", layout.cell_uv(1, 0)),
            ("BOTTOM", layout.cell_uv(1, 2)),
            ("BACK", layout.cell_uv(3, 1)),
        ],
        tri_faces=[
            ("LEFT", (left_uv[0], left_uv[1], left_uv[3])),
            ("RIGHT", (right_uv[0], right_uv[1], right_uv[2])),
        ],
        output_path=os.path.join(OUTPUT_DIR, "wedge_uv_template.png"),
    )

    # ---- slab（半砖六个朝向） ----
    print("生成半砖六个朝向: bottom/top/left/right/front/back")
    for orientation, (axis, sign) in SLAB_ORIENTATIONS.items():
        slab_mesh, layout = build_slab(axis, sign)
        write_glb(slab_mesh, os.path.join(OUTPUT_DIR, f"slab_{orientation}.glb"))

    print("完成。输出目录:", OUTPUT_DIR)


if __name__ == "__main__":
    main()
