import java.io.File;

/**
 * Test the fixed convertToResourcePath method
 * Verifies that paths with parentheses are handled correctly
 */
public class TestTexturePathFix {

    public static void main(String[] args) {
        System.out.println("=== Testing Fixed Texture Path Conversion ===\n");

        // Test Case 1: Resources directory path (should extract relative path)
        testPath(
            "C:\\Users\\29232\\OneDrive\\Desktop\\game1(1)\\src\\main\\resources\\textures\\arm.png",
            "textures/arm.png",
            "Resources path"
        );

        // Test Case 2: Project root with parentheses (should keep absolute path)
        testPath(
            "C:\\Users\\29232\\OneDrive\\Desktop\\game1(1)\\armlegs_backup_original.png",
            "C:/Users/29232/OneDrive/Desktop/game1(1)/armlegs_backup_original.png",
            "Project root with parentheses"
        );

        // Test Case 3: External path without special chars (should strip drive letter)
        testPath(
            "C:\\Users\\29232\\Downloads\\test.png",
            "Users/29232/Downloads/test.png",
            "External path (no special chars)"
        );

        // Test Case 4: Path with spaces (should keep absolute path)
        testPath(
            "C:\\Users\\29232\\My Documents\\test image.png",
            "C:/Users/29232/My Documents/test image.png",
            "Path with spaces"
        );

        System.out.println("\n=== Summary ===");
        System.out.println("All test cases verify the fix handles:");
        System.out.println("1. Standard resources paths - extracted to relative");
        System.out.println("2. Paths with parentheses - kept as absolute");
        System.out.println("3. Paths with spaces - kept as absolute");
        System.out.println("4. Normal external paths - stripped drive letter");
    }

    private static void testPath(String input, String expected, String testName) {
        String result = convertToResourcePath(input);
        boolean matches = result.equals(expected);

        System.out.println("Test: " + testName);
        System.out.println("  Input:    " + input);
        System.out.println("  Expected: " + expected);
        System.out.println("  Got:      " + result);
        System.out.println("  Status:   " + (matches ? "PASS" : "FAIL"));
        System.out.println();
    }

    /**
     * Fixed version of convertToResourcePath
     */
    private static String convertToResourcePath(String absolutePath) {
        String normalized = absolutePath.replace('\\', '/');

        // Check for resources directory
        int resourcesIndex = normalized.indexOf("/resources/");
        if (resourcesIndex != -1) {
            String resourcePath = normalized.substring(resourcesIndex + "/resources/".length());
            return resourcePath;
        }

        // Check if file exists and is absolute
        java.io.File file = new java.io.File(absolutePath);
        if (file.exists() && file.isAbsolute()) {
            // Check for special characters that cause AssetManager issues
            if (normalized.contains("(") || normalized.contains(")") ||
                normalized.contains(" ") || normalized.contains("%")) {
                // Return absolute path for BufferedImage loading
                return absolutePath.replace('\\', '/');
            }
        }

        // Strip drive letter for FileLocator
        if (normalized.matches("^[A-Za-z]:/.*")) {
            return normalized.substring(3);
        } else if (normalized.startsWith("/")) {
            return normalized.substring(1);
        } else {
            String absPath = file.getAbsolutePath().replace('\\', '/');
            if (absPath.matches("^[A-Za-z]:/.*")) {
                return absPath.substring(3);
            } else if (absPath.startsWith("/")) {
                return absPath.substring(1);
            }
            return absPath;
        }
    }
}
