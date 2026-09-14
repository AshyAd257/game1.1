import java.io.File;

/**
 * 测试木偶编辑器的贴图路径转换逻辑
 * 验证 convertToResourcePath() 方法是否正确处理 game1(1) 目录下的贴图文件
 */
public class TestTexturePathIssue {

    public static void main(String[] args) {
        System.out.println("=== 测试贴图路径转换 ===\n");

        // 测试场景1: resources目录下的贴图
        String resourcePath = "C:\\Users\\29232\\OneDrive\\Desktop\\game1(1)\\src\\main\\resources\\textures\\arm.png";
        System.out.println("场景1 - resources目录下的贴图:");
        System.out.println("  输入: " + resourcePath);
        System.out.println("  输出: " + convertToResourcePath(resourcePath));
        System.out.println("  期望: textures/arm.png");
        System.out.println();

        // 测试场景2: 项目根目录下的贴图（括号路径）
        String rootPath = "C:\\Users\\29232\\OneDrive\\Desktop\\game1(1)\\armlegs_backup_original.png";
        System.out.println("场景2 - 项目根目录下的贴图:");
        System.out.println("  输入: " + rootPath);
        System.out.println("  输出: " + convertToResourcePath(rootPath));
        System.out.println("  期望: Users/29232/OneDrive/Desktop/game1(1)/armlegs_backup_original.png");
        System.out.println();

        // 测试场景3: 任意外部路径
        String externalPath = "C:\\Users\\29232\\Downloads\\test.png";
        System.out.println("场景3 - 外部路径:");
        System.out.println("  输入: " + externalPath);
        System.out.println("  输出: " + convertToResourcePath(externalPath));
        System.out.println("  期望: Users/29232/Downloads/test.png");
        System.out.println();

        // 检查文件是否存在
        System.out.println("=== 检查实际文件 ===\n");
        checkFileExists(resourcePath);
        checkFileExists(rootPath);

        // 检查括号是否是问题
        System.out.println("\n=== 路径中的括号分析 ===");
        System.out.println("路径包含括号: " + rootPath.contains("(") + ", " + rootPath.contains(")"));
        System.out.println("括号在URL编码中可能导致问题");
        System.out.println("建议: 使用绝对路径加载，避免通过AssetManager");
    }

    /**
     * 模拟 PuppetEditorApp.convertToResourcePath() 的逻辑
     */
    private static String convertToResourcePath(String absolutePath) {
        // 将路径标准化（统一使用正斜杠）
        String normalized = absolutePath.replace('\\', '/');

        // 尝试找到 resources 目录
        int resourcesIndex = normalized.indexOf("/resources/");
        if (resourcesIndex != -1) {
            // 提取 resources 之后的路径
            String resourcePath = normalized.substring(resourcesIndex + "/resources/".length());
            return resourcePath;
        }

        // 找不到 resources 目录：去掉盘符
        if (normalized.matches("^[A-Za-z]:/.*")) {
            return normalized.substring(3);
        } else if (normalized.startsWith("/")) {
            return normalized.substring(1);
        }

        return normalized;
    }

    private static void checkFileExists(String path) {
        File file = new File(path);
        System.out.println("文件: " + path);
        System.out.println("  存在: " + file.exists());
        System.out.println("  可读: " + file.canRead());
        System.out.println("  大小: " + (file.exists() ? file.length() + " bytes" : "N/A"));
    }
}
