package org.visallo.core.util;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class StringUtilTest {
    @Test
    public void testContainsOnWordBoundaryCaseInsensitive() throws Exception {
        assertTrue(StringUtil.containsOnWordBoundaryCaseInsensitive("test string", "test"));
        assertTrue(StringUtil.containsOnWordBoundaryCaseInsensitive("string test", "test"));
        assertTrue(StringUtil.containsOnWordBoundaryCaseInsensitive("string Test", "Test"));
        assertTrue(StringUtil.containsOnWordBoundaryCaseInsensitive("string test", "Test"));
        assertTrue(StringUtil.containsOnWordBoundaryCaseInsensitive("string Test", "test"));
        assertTrue(StringUtil.containsOnWordBoundaryCaseInsensitive("string test.", "test"));
        assertTrue(StringUtil.containsOnWordBoundaryCaseInsensitive("string test string", "test"));
        assertTrue(StringUtil.containsOnWordBoundaryCaseInsensitive("string ,test. string", "test"));
        assertTrue(StringUtil.containsOnWordBoundaryCaseInsensitive("string \"test\" string", "test"));
        assertTrue(StringUtil.containsOnWordBoundaryCaseInsensitive("string \'test\' string", "test"));
        assertFalse(StringUtil.containsOnWordBoundaryCaseInsensitive("C is cool", "c++"));
        assertFalse(StringUtil.containsOnWordBoundaryCaseInsensitive("string testing", "test"));
    }
}