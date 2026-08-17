package com.Hecate.loader;

import com.Hecate.registry.AbstractModelRegistry;
import com.jme3.asset.AssetManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * AbstractAssetLoader的单元测试
 */
@DisplayName("抽象资源加载器测试")
public class AbstractAssetLoaderTest {

    @Mock
    private AssetManager mockAssetManager;

    private TestModelRegistry registry;
    private TestAssetLoader loader;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        registry = new TestModelRegistry();
        loader = new TestAssetLoader(mockAssetManager, registry);
    }

    @Test
    @DisplayName("应该能够获取AssetManager")
    void shouldGetAssetManager() {
        // Then
        assertSame(mockAssetManager, loader.getAssetManager());
    }

    @Test
    @DisplayName("应该能够获取注册表")
    void shouldGetRegistry() {
        // Then
        assertSame(registry, loader.getRegistry());
    }

    @Test
    @DisplayName("checkExistingModel应该返回存在的模型")
    void shouldReturnExistingModel() {
        // Given
        TestModel model = new TestModel("existing", true);
        registry.registerModel("existing", model);

        // When
        TestModel result = loader.checkExistingModelPublic("existing");

        // Then
        assertSame(model, result);
    }

    @Test
    @DisplayName("checkExistingModel应该在模型不存在时返回null")
    void shouldReturnNullWhenModelDoesNotExist() {
        // When
        TestModel result = loader.checkExistingModelPublic("nonexistent");

        // Then
        assertNull(result);
    }

    @Test
    @DisplayName("reloadModel应该在模型存在时调用loadModelFile")
    void shouldCallLoadModelFileWhenReloadingExistingModel() {
        // Given
        TestModel model = new TestModel("test", true);
        registry.registerModel("test", model);

        // When
        boolean result = loader.reloadModel("test");

        // Then
        assertTrue(result);
        assertTrue(loader.loadModelFileCalled);
    }

    @Test
    @DisplayName("reloadModel应该在模型不存在时返回false")
    void shouldReturnFalseWhenReloadingNonexistentModel() {
        // When
        boolean result = loader.reloadModel("nonexistent");

        // Then
        assertFalse(result);
        assertFalse(loader.loadModelFileCalled);
    }

    @Test
    @DisplayName("loadDefaultModels应该调用loadModelsImpl")
    void shouldCallLoadModelsImplWhenLoadingDefaultModels() {
        // When
        loader.loadDefaultModels();

        // Then
        assertTrue(loader.loadModelsImplCalled);
    }

    @Test
    @DisplayName("loadDefaultModels应该捕获异常并继续执行")
    void shouldCatchExceptionInLoadDefaultModels() {
        // Given
        TestAssetLoader loaderWithException = new TestAssetLoader(mockAssetManager, registry) {
            @Override
            protected void loadModelsImpl() {
                throw new RuntimeException("Test exception");
            }
        };

        // When/Then - 不应该抛出异常
        assertDoesNotThrow(() -> loaderWithException.loadDefaultModels());
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
     * 测试用的加载器实现
     */
    static class TestAssetLoader extends AbstractAssetLoader<TestModel, TestModelRegistry> {
        boolean loadModelsImplCalled = false;
        boolean loadModelFileCalled = false;

        TestAssetLoader(AssetManager assetManager, TestModelRegistry registry) {
            super(assetManager, registry);
        }

        @Override
        protected String getModelTypeName() {
            return "Test";
        }

        @Override
        protected void loadModelsImpl() {
            loadModelsImplCalled = true;
        }

        @Override
        protected boolean loadModelFile(TestModel model) {
            loadModelFileCalled = true;
            return true;
        }

        // 公开受保护的方法用于测试
        TestModel checkExistingModelPublic(String modelId) {
            return super.checkExistingModel(modelId);
        }
    }
}
