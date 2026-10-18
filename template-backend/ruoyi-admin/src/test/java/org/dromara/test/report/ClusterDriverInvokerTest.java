package org.dromara.test.report;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.report.config.ClusterDriverProperties;
import org.dromara.report.domain.dto.DriverInvokeResult;
import org.dromara.report.service.impl.ClusterDriverInvoker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;

@Tag("local")
class ClusterDriverInvokerTest {
    @TempDir
    Path directory;

    private ClusterDriverProperties properties(String script) throws Exception {
        Path target = directory.resolve("scanner_fixture.py");
        Files.writeString(target, script);
        ClusterDriverProperties properties = new ClusterDriverProperties();
        properties.setEnabled(true);
        properties.setBatchEnabled(true);
        properties.setPython(System.getenv().getOrDefault("REPORT_TEST_PYTHON", "python"));
        properties.setScannerPath(target.toString());
        properties.setRootDir(directory.toString());
        properties.setTimeoutSeconds(10);
        return properties;
    }

    @Test
    void batchArgumentsAndSingleLineJsonAreReadFromARealProcess() throws Exception {
        String script = "import json, sys\nprint(json.dumps({'status':'success','message':sys.argv[1],"
            + "'data':{'file_count':2}}))\n";
        ClusterDriverInvoker invoker = new ClusterDriverInvoker(properties(script));
        DriverInvokeResult result = invoker.invokeBatch("20261018", "product", "sample");
        Assertions.assertTrue(result.isSuccess());
        Assertions.assertEquals(2, result.getFileCount());
        Assertions.assertEquals("20261018/product/sample", result.getMessage());
    }

    @Test
    void largeOutputDoesNotBlockTheProcessPipe() throws Exception {
        ClusterDriverInvoker invoker = new ClusterDriverInvoker(properties(
            "print('x' * 200000)\nprint('{\"status\":\"success\",\"data\":{\"file_count\":1}}')\n"));
        Assertions.assertTrue(invoker.invokeBatch("20261018", "product", "sample").isSuccess());
    }

    @Test
    void nonZeroExitCannotPretendToBeSuccessful() throws Exception {
        ClusterDriverInvoker invoker = new ClusterDriverInvoker(properties(
            "import sys\nprint('{\"status\":\"success\"}')\nsys.exit(9)\n"));
        Assertions.assertThrows(ServiceException.class, () -> invoker.invokeBatch("20261018", "product", "sample"));
    }

    @Test
    void timeoutStopsTheChildProcess() throws Exception {
        ClusterDriverProperties properties = properties("import time\ntime.sleep(3)\n");
        properties.setTimeoutSeconds(1);
        ClusterDriverInvoker invoker = new ClusterDriverInvoker(properties);
        Assertions.assertThrows(ServiceException.class, () -> invoker.invokeBatch("20261018", "product", "sample"));
    }

    @Test
    void disabledBatchAndPathTraversalAreRejected() throws Exception {
        ClusterDriverProperties properties = properties("raise RuntimeError('must not execute')\n");
        properties.setBatchEnabled(false);
        ClusterDriverInvoker invoker = new ClusterDriverInvoker(properties);
        Assertions.assertThrows(ServiceException.class, () -> invoker.invokeBatch("20261018", "product", "sample"));
        properties.setBatchEnabled(true);
        Assertions.assertThrows(ServiceException.class, () -> invoker.invokeBatch("20261018", "../product", "sample"));
    }
}
