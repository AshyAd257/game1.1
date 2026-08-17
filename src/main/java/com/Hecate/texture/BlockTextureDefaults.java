package com.Hecate.texture;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Central place to declare built-in block texture definitions.
 */
public final class BlockTextureDefaults {
    private static final Map<String, BlockTextureDefinition> DEFAULT_DEFINITIONS = createDefaults();

    private BlockTextureDefaults() {
        // utility class
    }

    private static Map<String, BlockTextureDefinition> createDefaults() {
        Map<String, BlockTextureDefinition> definitions = new LinkedHashMap<>();

        definitions.put("air", BlockTextureDefinition.singleTexture("textures/blocks/air.png"));
        definitions.put("dirt", BlockTextureDefinition.singleTexture("textures/blocks/dirt.png"));
        definitions.put("dirt2", BlockTextureDefinition.singleTexture("textures/blocks/dirt2.png"));
        definitions.put("dirt3", BlockTextureDefinition.singleTexture("textures/blocks/dirt3.png"));
        definitions.put("dirt4", BlockTextureDefinition.singleTexture("textures/blocks/dirt4.png"));
        definitions.put("stone", BlockTextureDefinition.singleTexture("textures/blocks/stone.png"));
        definitions.put("grass", BlockTextureDefinition.threeTexture(
                "textures/blocks/grass_top.png",
                "textures/blocks/grass_side.png",
                "textures/blocks/dirt.png"
        ));
        definitions.put("glass", BlockTextureDefinition.singleTexture("textures/blocks/glass.png"));
        definitions.put("wood", BlockTextureDefinition.singleTexture("textures/blocks/wood.png"));
        definitions.put("cobblestone", BlockTextureDefinition.singleTexture("textures/blocks/cobblestone.png"));

        return Collections.unmodifiableMap(definitions);
    }

    /**
     * Register all default definitions with the supplied manager.
     */
    public static void registerAll(BlockTextureManager manager) {
        DEFAULT_DEFINITIONS.forEach(manager::defineBlockTexture);
    }

    /**
     * Retrieve an immutable view of the defaults.
     */
    public static Map<String, BlockTextureDefinition> getAll() {
        return DEFAULT_DEFINITIONS;
    }

    /**
     * Convenience accessor for tests or ad-hoc lookups.
     */
    public static BlockTextureDefinition get(String blockId) {
        return DEFAULT_DEFINITIONS.get(blockId);
    }
}
