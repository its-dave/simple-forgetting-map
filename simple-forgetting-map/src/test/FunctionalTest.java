package test;

import org.junit.Assert;
import org.junit.Test;

import sfm.ForgettingMap;

public class FunctionalTest {
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
}
