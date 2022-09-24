package sfm;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;

public class ForgettingMapTest {

    @Test
    public void testRequirements() {
        ForgettingMap<String, Integer> map = new ForgettingMap<>(3);
        final String TEST_KEY_1 = "key1";
        final String TEST_KEY_2 = "key2";
        final String TEST_KEY_3 = "key3";
        final String TEST_KEY_4 = "key4";
        final Integer TEST_VALUE_1 = 1;
        final Integer TEST_VALUE_2 = 2;
        final Integer TEST_VALUE_3 = 3;
        final Integer TEST_VALUE_4 = 4;

        // Add entries and check they exist
        map.add(TEST_KEY_1, TEST_VALUE_1);
        Assert.assertEquals(TEST_VALUE_1, map.find(TEST_KEY_1));
        map.add(TEST_KEY_2, TEST_VALUE_2);
        Assert.assertEquals(TEST_VALUE_1, map.find(TEST_KEY_1));
        Assert.assertEquals(TEST_VALUE_2, map.find(TEST_KEY_2));
        map.add(TEST_KEY_3, TEST_VALUE_3);
        Assert.assertEquals(TEST_VALUE_1, map.find(TEST_KEY_1));
        Assert.assertEquals(TEST_VALUE_2, map.find(TEST_KEY_2));
        Assert.assertEquals(TEST_VALUE_3, map.find(TEST_KEY_3));

        // Add another entry and check it replaces the least-used
        map.add(TEST_KEY_4, TEST_VALUE_4);
        Assert.assertEquals(TEST_VALUE_1, map.find(TEST_KEY_1));
        Assert.assertEquals(TEST_VALUE_2, map.find(TEST_KEY_2));
        Assert.assertEquals(null, map.find(TEST_KEY_3));
        Assert.assertEquals(TEST_VALUE_4, map.find(TEST_KEY_4));

        // Use a later entry so an earlier entry is now the least-used
        map.find(TEST_KEY_4);
        map.find(TEST_KEY_4);
        map.find(TEST_KEY_4);

        // Add another entry and check it replaces the least-used
        map.add(TEST_KEY_3, TEST_VALUE_3);
        Assert.assertEquals(TEST_VALUE_1, map.find(TEST_KEY_1));
        Assert.assertEquals(null, map.find(TEST_KEY_2));
        Assert.assertEquals(TEST_VALUE_3, map.find(TEST_KEY_3));
        Assert.assertEquals(TEST_VALUE_4, map.find(TEST_KEY_4));

        // Use a later entry so there is a tie for least-used
        map.find(TEST_KEY_3);
        map.find(TEST_KEY_3);
        map.find(TEST_KEY_3);
        map.find(TEST_KEY_3);
        map.find(TEST_KEY_3);

        // Add another entry and check it replaces the earliest least-used
        map.add(TEST_KEY_2, TEST_VALUE_2);
        Assert.assertEquals(null, map.find(TEST_KEY_1));
        Assert.assertEquals(TEST_VALUE_2, map.find(TEST_KEY_2));
        Assert.assertEquals(TEST_VALUE_3, map.find(TEST_KEY_3));
        Assert.assertEquals(TEST_VALUE_4, map.find(TEST_KEY_4));
    }

    @Test
    public void testThreadSafety() {
        ForgettingMap<Integer, String> map = new ForgettingMap<>(1);

        // Set up running simultaneous threads
        int testThreadCount = 42;
        ExecutorService service = Executors.newFixedThreadPool(testThreadCount);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean running = new AtomicBoolean();
        AtomicInteger overlaps = new AtomicInteger();
        List<Future<String>> futures = new ArrayList<>(testThreadCount);
        for (int i = 0; i < testThreadCount; i++) {
            final int key = i;
            futures.add(
                service.submit(() -> {
                    // Wait until all threads have been set up before running any of them
                    latch.await();

                    // Confirm threads are actually running in parallel
                    if (running.get()) {
                        overlaps.incrementAndGet();
                    }

                    // Add a new entry, wait, then get the value
                    running.set(true);
                    map.add(key, Integer.toString(key));
                    Thread.sleep(500);
                    String value = map.find(key);
                    running.set(false);

                    return value;
                })
            );
        }

        // Run all threads simultaneously
        latch.countDown();

        // Check each entry has the expected value and so was not overwritten by another thread
        for (int i = 0; i < testThreadCount; i++) {
            try {
                Assert.assertEquals("entry " + i + "was overridden by another thread", Integer.toString(i), futures.get(i).get());
            } catch(InterruptedException e) {
                Assert.fail("unexpected InterruptedException: " + e);
            } catch(ExecutionException e) {
                Assert.fail("unexpected ExecutionException: " + e);
            }
        }

        Assert.assertTrue("multi-theading has not actually been tested, use more threads", overlaps.get() > 1);
    }

    @Test
    public void testAddMethodAddsEntry() {
        final Long TEST_KEY_1 = Long.valueOf(1);
        final Long TEST_KEY_2 = Long.valueOf(2);
        final Character TEST_VALUE_1 = 'a';
        final Character TEST_VALUE_2 = 'b';

        ForgettingMap<Long, Character> map = new ForgettingMap<>(1);

        map.add(TEST_KEY_1, TEST_VALUE_1);
        Assert.assertEquals(TEST_VALUE_1, map.map.get(TEST_KEY_1));
        Assert.assertEquals(Integer.valueOf(0), map.usageCountMap.get(TEST_KEY_1));

        map.add(TEST_KEY_2, TEST_VALUE_2);
        Assert.assertEquals(TEST_VALUE_2, map.map.get(TEST_KEY_2));
        Assert.assertEquals(Integer.valueOf(0), map.usageCountMap.get(TEST_KEY_2));
    }

    @Test
    public void testAddMethodDoesNotExceedMaxSize() {
        int maxSize = 1;
        ForgettingMap<Short, Exception> map = new ForgettingMap<>(maxSize);
        Assert.assertTrue("maximum size exceeded", map.map.size() <= maxSize);
        Assert.assertTrue("maximum size exceeded", map.usageCountMap.size() <= maxSize);

        map.add(Short.valueOf("1"), new InterruptedException());
        Assert.assertTrue("maximum size exceeded", map.map.size() <= maxSize);
        Assert.assertTrue("maximum size exceeded", map.usageCountMap.size() <= maxSize);

        map.add(Short.valueOf("2"), new NullPointerException());
        Assert.assertTrue("maximum size exceeded", map.map.size() <= maxSize);
        Assert.assertTrue("maximum size exceeded", map.usageCountMap.size() <= maxSize);
    }

    @Test
    public void testAddMethodRemovesLeastUsedKey() {
        final Object TEST_KEY_1 = "key1";
        final Object TEST_KEY_2 = '2';
        final Object TEST_KEY_3 = 3;

        ForgettingMap<Object, Boolean> map = new ForgettingMap<>(2);
        map.map.put(TEST_KEY_1, true);
        map.map.put(TEST_KEY_2, false);
        map.usageCountMap.put(TEST_KEY_1, 1);
        map.usageCountMap.put(TEST_KEY_2, 2);

        map.add(TEST_KEY_3, false);
        Assert.assertEquals(null, map.map.get(TEST_KEY_1));
        Assert.assertEquals(null, map.usageCountMap.get(TEST_KEY_1));
        Assert.assertNotNull(map.map.get(TEST_KEY_2));
        Assert.assertNotNull(map.usageCountMap.get(TEST_KEY_2));
        Assert.assertNotNull(map.map.get(TEST_KEY_3));
        Assert.assertNotNull(map.usageCountMap.get(TEST_KEY_3));
    }

    @Test
    public void testFindMethodReturnsValue() {
        final String TEST_KEY_1 = "key1";
        final String TEST_KEY_2 = "key2";
        final Object TEST_VALUE_1 = true;
        final Object TEST_VALUE_2 = "value";

        ForgettingMap<String, Object> map = new ForgettingMap<>(2);
        map.map.put(TEST_KEY_1, TEST_VALUE_1);
        map.map.put(TEST_KEY_2, TEST_VALUE_2);
        map.usageCountMap.put(TEST_KEY_1, 0);
        map.usageCountMap.put(TEST_KEY_2, 0);

        Assert.assertEquals(TEST_VALUE_1, map.find(TEST_KEY_1));
        Assert.assertEquals(TEST_VALUE_2, map.find(TEST_KEY_2));
        Assert.assertEquals(null, map.find("fake value"));
    }

    @Test
    public void testFindMethodIncrementsUsageCount() {
        final String TEST_KEY_1 = "key1";
        
        ForgettingMap<String, String> map = new ForgettingMap<>(1);
        map.map.put(TEST_KEY_1, "value");
        map.usageCountMap.put(TEST_KEY_1, 0);

        map.find(TEST_KEY_1);
        Assert.assertEquals(Integer.valueOf(1), map.usageCountMap.get(TEST_KEY_1));

        map.find(TEST_KEY_1);
        Assert.assertEquals(Integer.valueOf(2), map.usageCountMap.get(TEST_KEY_1));
    }

    @Test
    public void testGetLeastUsedKeyMethod() {
        final Object TEST_KEY_1 = 1;
        final Object TEST_KEY_2 = '2';

        ForgettingMap<Object, Object> map = new ForgettingMap<>(2);
        map.map.put(TEST_KEY_1, true);
        map.map.put(TEST_KEY_2, false);
        map.usageCountMap.put(TEST_KEY_1, 1);
        map.usageCountMap.put(TEST_KEY_2, 2);
        
        Assert.assertEquals(TEST_KEY_1, map.getLeastUsedKey());
    }
}
