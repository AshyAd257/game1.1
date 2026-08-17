public class TestPathConversion {
    public static void main(String[] args) {
        String[] testPaths = {
            "textures/player/front.png",
            "Textures/blocks/grass.png",
            "C:/Users/29232/OneDrive/Desktop/game1(1)/src/main/resources/textures/player/front.png",
            "file:///C:/some/absolute/path.png"
        };

        for (String path : testPaths) {
            System.out.println("Input: " + path);
            System.out.println("Output: " + convertTexturePath(path));
            System.out.println();
        }
    }

    private static String convertTexturePath(String texturePath) {
        if (texturePath == null || texturePath.isEmpty()) {
            return texturePath;
        }

        System.out.println("  [Convert] Input: " + texturePath);

        if (texturePath.startsWith("file://")) {
            System.out.println("  [Convert] Already file:// protocol");
            return texturePath;
        }

        String normalized = texturePath.replace('\\', '/');

        int resourcesIndex = normalized.indexOf("/resources/");
        if (resourcesIndex != -1) {
            String resourcePath = normalized.substring(resourcesIndex + "/resources/".length());
            System.out.println("  [Convert] Found resources dir, extract: " + resourcePath);
            return resourcePath;
        }

        boolean isAbsolutePath = normalized.matches("^[A-Za-z]:.*") || normalized.startsWith("/");
        if (isAbsolutePath) {
            String fileProtocolPath;
            if (normalized.matches("^[A-Za-z]:.*")) {
                fileProtocolPath = "file:///" + normalized;
            } else {
                fileProtocolPath = "file://" + normalized;
            }
            System.out.println("  [Convert] Absolute path, convert to: " + fileProtocolPath);
            return fileProtocolPath;
        }

        System.out.println("  [Convert] Relative path, return: " + normalized);
        return normalized;
    }
}
