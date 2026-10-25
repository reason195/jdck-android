package cn.xanderye.android.jdck.util;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JDUtilTest {

    @Test
    public void escapesAccountAndPasswordForJavaScript() {
        String js = JDUtil.buildLoginScript("13800138000", "pa'ss\\word \"x\" 123", true);

        assertTrue(js.contains("var account=\"13800138000\";"));
        assertTrue(js.contains("var password=\"pa'ss\\\\word \\\"x\\\" 123\";"));
        assertFalse(js.contains("var password='"));
        assertTrue(js.contains("phoneInput"));
    }

    @Test
    public void omitsPasswordBlockWhenPasswordIsEmpty() {
        String js = JDUtil.buildLoginScript("13800138000", null, true);

        assertFalse(js.contains("var password="));
        assertFalse(js.contains("loginLink"));
    }
}
