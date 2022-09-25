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
    public void testAddMethodThreadSafety() {
        final Integer TEST_KEY_1 = -1;
        final Integer TEST_KEY_2 = -2;
        final int MAX_SIZE = 2;

        ForgettingMap<Integer, String> map = new ForgettingMap<>(MAX_SIZE);
        Value<String> testValue1 = new Value<>("a");
        testValue1.incrementUseCount();
        Value<String> testValue2 = new Value<>("b");
        map.map.put(TEST_KEY_1, testValue1);
        map.map.put(TEST_KEY_2, testValue2);

        // Set up running simultaneous threads
        int testThreadCount = 500;
        ExecutorService service = Executors.newFixedThreadPool(testThreadCount);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean running = new AtomicBoolean();
        AtomicInteger overlaps = new AtomicInteger();
        List<Future<Integer>> futures = new ArrayList<>(testThreadCount);
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

                    // Add a new entry
                    running.set(true);
                    map.add(key, Integer.toString(key));
                    running.set(false);

                    return map.map.size();
                })
            );
        }

        // Run all threads simultaneously
        latch.countDown();

        // Check multiple threads don't try to remove the same least-used entry
        for (Future<Integer> future : futures) {
            try {
                int mapSize = future.get().intValue();
                Assert.assertTrue("map size was " + mapSize + ", should only be " + MAX_SIZE + " or " + String.valueOf(MAX_SIZE + 1), mapSize == MAX_SIZE || mapSize == MAX_SIZE + 1);
            } catch (InterruptedException e) {
                e.printStackTrace();
                Assert.fail("unexpected InterruptedException: " + e);
            } catch (ExecutionException e) {
                e.printStackTrace();
                Assert.fail("unexpected ExecutionException: " + e);
            }
        }

        // Check map has been updated as expected
        Assert.assertEquals("original entry has changed unexpectedly: ", testValue1.getValue(), map.map.get(TEST_KEY_1).getValue());
        Assert.assertEquals("original entry was not replaced: ", null, map.map.get(TEST_KEY_2));

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
        Value<Character> value = map.map.get(TEST_KEY_1);
        Assert.assertEquals(TEST_VALUE_1, value.getValue());
        Assert.assertEquals(0, value.getUseCount());

        map.add(TEST_KEY_2, TEST_VALUE_2);
        value = map.map.get(TEST_KEY_2);
        Assert.assertEquals(TEST_VALUE_2, value.getValue());
        Assert.assertEquals(0, value.getUseCount());
    }

    @Test
    public void testAddMethodDoesNotExceedMaxSize() {
        int maxSize = 1;
        ForgettingMap<Short, Exception> map = new ForgettingMap<>(maxSize);
        Assert.assertTrue("maximum size exceeded", map.map.size() <= maxSize);

        map.add(Short.valueOf("1"), new InterruptedException());
        Assert.assertTrue("maximum size exceeded", map.map.size() <= maxSize);

        map.add(Short.valueOf("2"), new NullPointerException());
        Assert.assertTrue("maximum size exceeded", map.map.size() <= maxSize);
    }

    @Test
    public void testAddMethodRemovesLeastUsedKey() {
        final Object TEST_KEY_1 = "key1";
        final Object TEST_KEY_2 = '2';
        final Object TEST_KEY_3 = 3;

        ForgettingMap<Object, Boolean> map = new ForgettingMap<>(2);
        Value<Boolean> testValue1 = new Value<>(true);
        testValue1.incrementUseCount();
        Value<Boolean> testValue2 = new Value<>(false);
        testValue2.incrementUseCount();
        testValue2.incrementUseCount();
        map.map.put(TEST_KEY_1, testValue1);
        map.map.put(TEST_KEY_2, testValue2);

        map.add(TEST_KEY_3, false);
        Assert.assertEquals(null, map.map.get(TEST_KEY_1));
        Assert.assertNotNull(map.map.get(TEST_KEY_2).getValue());
        Assert.assertNotNull(map.map.get(TEST_KEY_3).getValue());
    }

    @Test
    public void testFindMethodThreadSafety() {
        final Integer TEST_KEY_1 = 1;

        ForgettingMap<Integer, String> map = new ForgettingMap<>(1);
        map.map.put(TEST_KEY_1, new Value<>("value"));

        // Set up running simultaneous threads
        int testThreadCount = 5000;
        ExecutorService service = Executors.newFixedThreadPool(testThreadCount);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean running = new AtomicBoolean();
        AtomicInteger overlaps = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>(testThreadCount);
        for (int i = 0; i < testThreadCount; i++) {
            futures.add(
                service.submit(() -> {
                    // Wait until all threads have been set up before running any of them
                    try {
                        latch.await();
                    } catch(InterruptedException e) {
                        e.printStackTrace();
                        Assert.fail("unexpected InterruptedException: " + e);
                    }

                    // Confirm threads are actually running in parallel
                    if (running.get()) {
                        overlaps.incrementAndGet();
                    }

                    // Increment the use count
                    running.set(true);
                    map.find(TEST_KEY_1);
                    running.set(false);
                })
            );
        }

        // Run all threads simultaneously
        latch.countDown();
        for (int i = 0; i < testThreadCount; i++) {
            try {
                futures.get(i).get();
            } catch (InterruptedException e) {
                e.printStackTrace();
                Assert.fail("unexpected InterruptedException: " + e);
            } catch (ExecutionException e) {
                e.printStackTrace();
                Assert.fail("unexpected ExecutionException: " + e);
            }
        }

        // Check use count increased by 1 each time
        Assert.assertEquals("usage count did not match number of method calls: ", testThreadCount, map.map.get(TEST_KEY_1).getUseCount());

        Assert.assertTrue("multi-theading has not actually been tested, use more threads", overlaps.get() > 1);
    }

    @Test
    public void testFindMethodReturnsValue() {
        final String TEST_KEY_1 = "key1";
        final String TEST_KEY_2 = "key2";
        final Object TEST_VALUE_1 = true;
        final Object TEST_VALUE_2 = "value";

        ForgettingMap<String, Object> map = new ForgettingMap<>(2);
        map.map.put(TEST_KEY_1, new Value<>(TEST_VALUE_1));
        map.map.put(TEST_KEY_2, new Value<>(TEST_VALUE_2));

        Assert.assertEquals(TEST_VALUE_1, map.find(TEST_KEY_1));
        Assert.assertEquals(TEST_VALUE_2, map.find(TEST_KEY_2));
        Assert.assertEquals(null, map.find("fake value"));
    }

    @Test
    public void testFindMethodIncrementsUsageCount() {
        final String TEST_KEY_1 = "key1";

        ForgettingMap<String, String> map = new ForgettingMap<>(1);
        map.map.put(TEST_KEY_1, new Value<>("value"));

        map.find(TEST_KEY_1);
        Assert.assertEquals(1, map.map.get(TEST_KEY_1).getUseCount());

        map.find(TEST_KEY_1);
        Assert.assertEquals(2, map.map.get(TEST_KEY_1).getUseCount());
    }

    @Test
    public void testGetLeastUsedKeyMethod() {
        final Object TEST_KEY_1 = 1;
        final Object TEST_KEY_2 = '2';

        ForgettingMap<Object, Object> map = new ForgettingMap<>(2);
        Value<Object> testValue1 = new Value<>(true);
        testValue1.incrementUseCount();
        Value<Object> testValue2 = new Value<>(false);
        testValue2.incrementUseCount();
        testValue2.incrementUseCount();
        map.map.put(TEST_KEY_1, testValue1);
        map.map.put(TEST_KEY_2, testValue2);

        Assert.assertEquals(TEST_KEY_1, map.getLeastUsedKey());
    }
}
