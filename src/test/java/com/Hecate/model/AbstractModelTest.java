package com.Hecate.model;

import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AbstractModel的单元测试
 */
@DisplayName("抽象模型测试")
public class AbstractModelTest {

    private TestModel model;

    @BeforeEach
    void setUp() {
        model = new TestModel("testModel", "models/test.obj");
    }

    @Test
    @DisplayName("应该正确初始化模型属性")
    void shouldInitializeModelProperties() {
        // Then
        assertEquals("testModel", model.getId());
        assertEquals("testModel", model.getModelId()); // 兼容方法
        assertEquals("models/test.obj", model.getModelPath());
        assertNull(model.getSpatial());
        assertFalse(model.isLoaded());
    }

    @Test
    @DisplayName("设置Spatial后应该标记为已加载")
    void shouldMarkAsLoadedWhenSpatialIsSet() {
        // Given
        Spatial spatial = new Geometry("test", new Box(1, 1, 1));

        // When
        model.setSpatial(spatial);

        // Then
        assertSame(spatial, model.getSpatial());
        assertTrue(model.isLoaded());
    }

    @Test
    @DisplayName("设置null的Spatial应该标记为未加载")
    void shouldMarkAsNotLoadedWhenSpatialIsNull() {
        // Given
        Spatial spatial = new Geometry("test", new Box(1, 1, 1));
        model.setSpatial(spatial);
        assertTrue(model.isLoaded());

        // When
        model.setSpatial(null);

        // Then
        assertNull(model.getSpatial());
        assertFalse(model.isLoaded());
    }

    @Test
    @DisplayName("应该返回正确的类型名称")
    void shouldReturnCorrectTypeName() {
        // Then
        assertEquals("TestModel", model.getTypeName());
    }

    @Test
    @DisplayName("toString方法应该包含关键信息")
    void shouldIncludeKeyInfoInToString() {
        // When
        String str = model.toString();

        // Then
        assertTrue(str.contains("testModel"));
        assertTrue(str.contains("models/test.obj"));
        assertTrue(str.contains("loaded=false"));
    }

    @Test
    @DisplayName("加载后toString应该显示loaded=true")
    void shouldShowLoadedTrueInToStringWhenLoaded() {
        // Given
        model.setSpatial(new Geometry("test", new Box(1, 1, 1)));

        // When
        String str = model.toString();

        // Then
        assertTrue(str.contains("loaded=true"));
    }

    @Test
    @DisplayName("getId和getModelId应该返回相同的值")
    void shouldReturnSameValueForIdAndModelId() {
        // Then
        assertEquals(model.getId(), model.getModelId());
    }

    // ===== 测试辅助类 =====

    /**
     * 测试用的模型实现
     */
    static class TestModel extends AbstractModel {
        TestModel(String id, String modelPath) {
            super(id, modelPath);
        }

        @Override
        public String getTypeName() {
            return "TestModel";
        }
    }
}
