package com.Hecate.registry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AbstractModelRegistry的单元测试
 */
@DisplayName("抽象模型注册表测试")
public class AbstractModelRegistryTest {

    private TestModelRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new TestModelRegistry();
    }

    @Test
    @DisplayName("应该能够注册模型")
    void shouldRegisterModel() {
        // Given
        TestModel model = new TestModel("test1", true);

        // When
        registry.registerModel("test1", model);

        // Then
        assertTrue(registry.hasModel("test1"));
        assertEquals(model, registry.getModel("test1"));
        assertEquals(1, registry.getTotalModelCount());
    }

    @Test
    @DisplayName("应该能够获取所有模型ID")
    void shouldGetAllModelIds() {
        // Given
        registry.registerModel("model1", new TestModel("model1", true));
        registry.registerModel("model2", new TestModel("model2", true));
        registry.registerModel("model3", new TestModel("model3", false));

        // When
        var ids = registry.getAllModelIds();

        // Then
        assertEquals(3, ids.size());
        assertTrue(ids.contains("model1"));
        assertTrue(ids.contains("model2"));
        assertTrue(ids.contains("model3"));
    }

    @Test
    @DisplayName("应该能够移除模型")
    void shouldRemoveModel() {
        // Given
        TestModel model = new TestModel("test1", true);
        registry.registerModel("test1", model);

        // When
        TestModel removed = registry.removeModel("test1");

        // Then
        assertEquals(model, removed);
        assertFalse(registry.hasModel("test1"));
        assertNull(registry.getModel("test1"));
    }

    @Test
    @DisplayName("移除不存在的模型应该返回null")
    void shouldReturnNullWhenRemovingNonExistentModel() {
        // When
        TestModel removed = registry.removeModel("nonexistent");

        // Then
        assertNull(removed);
    }

    @Test
    @DisplayName("应该能够统计已加载的模型数量")
    void shouldCountLoadedModels() {
        // Given
        registry.registerModel("loaded1", new TestModel("loaded1", true));
        registry.registerModel("loaded2", new TestModel("loaded2", true));
        registry.registerModel("notLoaded", new TestModel("notLoaded", false));

        // When
        int loadedCount = registry.getLoadedModelCount();

        // Then
        assertEquals(2, loadedCount);
        assertEquals(3, registry.getTotalModelCount());
    }

    @Test
    @DisplayName("应该能够清空所有模型")
    void shouldClearAllModels() {
        // Given
        registry.registerModel("model1", new TestModel("model1", true));
        registry.registerModel("model2", new TestModel("model2", true));

        // When
        registry.clear();

        // Then
        assertEquals(0, registry.getTotalModelCount());
        assertFalse(registry.hasModel("model1"));
        assertFalse(registry.hasModel("model2"));
    }

    @Test
    @DisplayName("hasModel方法应该正确检查模型是否存在")
    void shouldCheckModelExistence() {
        // Given
        registry.registerModel("exists", new TestModel("exists", true));

        // Then
        assertTrue(registry.hasModel("exists"));
        assertFalse(registry.hasModel("notExists"));
    }

    @Test
    @DisplayName("getModel方法应该返回正确的模型")
    void shouldGetCorrectModel() {
        // Given
        TestModel model1 = new TestModel("model1", true);
        TestModel model2 = new TestModel("model2", false);
        registry.registerModel("model1", model1);
        registry.registerModel("model2", model2);

        // Then
        assertSame(model1, registry.getModel("model1"));
        assertSame(model2, registry.getModel("model2"));
        assertNull(registry.getModel("nonexistent"));
    }

    // ===== 测试辅助类 =====

    /**
     * 测试用的简单模型类
     */
    static class TestModel {
        private final String id;
        private final boolean loaded;

        TestModel(String id, boolean loaded) {
            this.id = id;
            this.loaded = loaded;
        }

        boolean isLoaded() {
            return loaded;
        }

        String getId() {
            return id;
        }
    }

    /**
     * 测试用的注册表实现
     */
    static class TestModelRegistry extends AbstractModelRegistry<TestModel> {
        @Override
        protected boolean isModelLoaded(TestModel model) {
            return model.isLoaded();
        }

        @Override
        protected String getModelTypeName() {
            return "Test";
        }
    }
}
