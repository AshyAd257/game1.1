package com.Hecate.placer;

import com.Hecate.registry.AbstractModelRegistry;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AbstractModelPlacer的单元测试
 */
@DisplayName("抽象模型放置器测试")
public class AbstractModelPlacerTest {

    private Node worldNode;
    private TestModelRegistry registry;
    private TestModelPlacer placer;

    @BeforeEach
    void setUp() {
        worldNode = new Node("worldNode");
        registry = new TestModelRegistry();
        placer = new TestModelPlacer(worldNode, registry);
    }

    @Test
    @DisplayName("应该能够获取世界节点")
    void shouldGetWorldNode() {
        // Then
        assertSame(worldNode, placer.getWorldNode());
    }

    @Test
    @DisplayName("应该能够获取注册表")
    void shouldGetRegistry() {
        // Then
        assertSame(registry, placer.getRegistry());
    }

    @Test
    @DisplayName("getAndValidateModel应该返回已加载的模型")
    void shouldReturnLoadedModel() {
        // Given
        TestModel model = new TestModel("test", true);
        registry.registerModel("test", model);

        // When
        TestModel result = placer.getAndValidateModelPublic("test");

        // Then
        assertSame(model, result);
    }

    @Test
    @DisplayName("getAndValidateModel应该在模型不存在时返回null")
    void shouldReturnNullWhenModelDoesNotExist() {
        // When
        TestModel result = placer.getAndValidateModelPublic("nonexistent");

        // Then
        assertNull(result);
    }

    @Test
    @DisplayName("getAndValidateModel应该在模型未加载时返回null")
    void shouldReturnNullWhenModelNotLoaded() {
        // Given
        TestModel model = new TestModel("test", false);
        registry.registerModel("test", model);

        // When
        TestModel result = placer.getAndValidateModelPublic("test");

        // Then
        assertNull(result);
    }

    @Test
    @DisplayName("placeModel(x,y,z)应该调用placeModel(Vector3f)")
    void shouldCallVectorPlaceModelFromCoordinates() {
        // Given
        TestModel model = new TestModel("test", true);
        registry.registerModel("test", model);

        // When
        Object result = placer.placeModel("test", 1.0f, 2.0f, 3.0f);

        // Then
        assertTrue((Boolean) result);
        assertNotNull(placer.lastPlacedPosition);
        assertEquals(1.0f, placer.lastPlacedPosition.x, 0.001f);
        assertEquals(2.0f, placer.lastPlacedPosition.y, 0.001f);
        assertEquals(3.0f, placer.lastPlacedPosition.z, 0.001f);
    }

    @Test
    @DisplayName("placeModel应该在模型未加载时返回false")
    void shouldReturnFalseWhenPlacingUnloadedModel() {
        // Given
        TestModel model = new TestModel("test", false);
        registry.registerModel("test", model);

        // When
        Object result = placer.placeModel("test", new Vector3f(1, 2, 3));

        // Then
        assertFalse((Boolean) result);
    }

    @Test
    @DisplayName("placeModel应该在模型不存在时返回false")
    void shouldReturnFalseWhenPlacingNonexistentModel() {
        // When
        Object result = placer.placeModel("nonexistent", new Vector3f(1, 2, 3));

        // Then
        assertFalse((Boolean) result);
    }

    // ===== 测试辅助类 =====

    /**
     * 测试用的模型类
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
    }

    /**
     * 测试用的注册表
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

    /**
     * 测试用的放置器实现
     */
    static class TestModelPlacer extends AbstractModelPlacer<TestModel, TestModelRegistry> {
        Vector3f lastPlacedPosition;

        TestModelPlacer(Node worldNode, TestModelRegistry registry) {
            super(worldNode, registry);
        }

        @Override
        public Object placeModel(String modelId, Vector3f position) {
            TestModel model = getAndValidateModel(modelId);
            if (model == null) {
                return false;
            }
            lastPlacedPosition = position;
            return true;
        }

        @Override
        protected boolean isModelLoaded(TestModel model) {
            return model.isLoaded();
        }

        // 公开受保护的方法用于测试
        TestModel getAndValidateModelPublic(String modelId) {
            return super.getAndValidateModel(modelId);
        }
    }
}
