package com.googlecode.pngtastic.ant;

import com.googlecode.pngtastic.core.PngFilterType;
import com.googlecode.pngtastic.core.PngImage;
import com.googlecode.pngtastic.core.PngOptimizer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class PngOptimizerBenchmark {
    private static final int iterations = 1;
    public static final boolean removeGamma = true;
    private static final int compressionLevel = 9;
    private static final String compressor = "zopfli";

    public static void main(String[] args) throws Exception {
        int microBenchmarkIterations = 50;
        int concurrency = 10;


        runTestsWithImage(microBenchmarkIterations, concurrency, "some image from your hard drive", ">>>PATH TO YOUR IMAGE ON DISK<<<");
    }

    private static void runTestsWithImage(int microBenchmarkIterations, int concurrency, String description, String testFile) throws Exception {
        System.out.println("==== RUNNING TESTS FOR IMAGE TYPE: "+description+" ====");

        byte[] rawPngData = readPngDataFromFile(testFile);

        runTestsWithImage(microBenchmarkIterations, concurrency, rawPngData);
    }

    private static void runTestsWithImage(int microBenchmarkIterations, int concurrency, byte[] rawPngData) throws Exception {
        runBenchmark("zopfli with max compression", rawPngData, microBenchmarkIterations, concurrency, () -> {
                    try {
                        ByteArrayInputStream bytes = new ByteArrayInputStream(rawPngData);

                        PngOptimizer optimizer = new PngOptimizer("info");
                        optimizer.setCompressor(compressor, iterations);
                        return optimizer.optimize(new PngImage(bytes), removeGamma, compressionLevel);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
        );

        runBenchmark("default compressor with max compression", rawPngData, microBenchmarkIterations, concurrency, () -> {
                    try {
                        ByteArrayInputStream bytes = new ByteArrayInputStream(rawPngData);

                        PngOptimizer optimizer = new PngOptimizer("info");
                        return optimizer.optimize(new PngImage(bytes), removeGamma, compressionLevel);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
        );

        runBenchmark("default compressor with adaptive filtering disabled (new)", rawPngData, microBenchmarkIterations, concurrency, () -> {
                    try {
                        ByteArrayInputStream bytes = new ByteArrayInputStream(rawPngData);

                        PngOptimizer optimizer = new PngOptimizer("info");
                        return optimizer.optimize(new PngImage(bytes), removeGamma, compressionLevel, PngFilterType.NONE);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
        );
    }

    static DecimalFormat decimalFormat = new DecimalFormat("##.#");

    /**
     * @param testName
     * @param rawPngData
     * @param microBenchmarkIterations number of runnables to execute
     * @param concurrency              number of concurrent threads
     * @param runnable                 test to run    @throws InterruptedException
     */
    private static Collection<Future<PngImage>> runBenchmark(String testName, byte[] rawPngData, int microBenchmarkIterations, int concurrency, Callable<PngImage> runnable) throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(concurrency);

        List<Callable<PngImage>> futures = new ArrayList<>();
        for (int i = 0; i < microBenchmarkIterations; i++) {
            futures.add(runnable);
        }

        long start = System.currentTimeMillis();
        List<Future<PngImage>> futures1 = executorService.invokeAll(futures);
        executorService.shutdown();

        // grab one of the compressed images and report its file size
        PngImage exampleCompressed = futures1.iterator().next().get();
        int before = rawPngData.length;
        int after = exampleCompressed.getImageData().length;
        float percentCompressed = (float) after/before * 100;
        System.out.println(testName+": runtime="+(System.currentTimeMillis()-start)+"ms), compression="+ decimalFormat.format(percentCompressed));
        return futures1;
    }

    private static byte[] readPngDataFromFile(String testFile) throws IOException {
        FileInputStream fin = new FileInputStream(testFile);
        ByteArrayOutputStream baos = new ByteArrayOutputStream(512);
        copyStream(fin, baos);
        return baos.toByteArray();
    }

    private static void copyStream(FileInputStream fin, ByteArrayOutputStream baos) throws IOException {
        byte[] buffer = new byte[1024];
        int len;
        while ((len = fin.read(buffer)) != -1) {
            baos.write(buffer, 0, len);
        }
    }
}
