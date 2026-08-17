package com.Hecate;

import com.Hecate.registry.AbstractModelRegistryTest;
import com.Hecate.model.AbstractModelTest;
import com.Hecate.loader.AbstractAssetLoaderTest;
import com.Hecate.placer.AbstractModelPlacerTest;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * 重构后抽象基类的测试套件
 */
@Suite
@SuiteDisplayName("Hecate重构测试套件")
@SelectClasses({
    AbstractModelRegistryTest.class,
    AbstractModelTest.class,
    AbstractAssetLoaderTest.class,
    AbstractModelPlacerTest.class
})
public class RefactoringTestSuite {
    // 测试套件类，无需额外代码
}
