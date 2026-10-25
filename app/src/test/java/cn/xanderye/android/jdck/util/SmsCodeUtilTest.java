package cn.xanderye.android.jdck.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class SmsCodeUtilTest {

    @Test
    public void extractsJdVerificationCode() {
        assertEquals("123456", SmsCodeUtil.extractJdCode("【京东】您的验证码为123456，5分钟内有效"));
        assertEquals("654321", SmsCodeUtil.extractJdCode("京东验证码：654321"));
    }

    @Test
    public void ignoresNonJdSms() {
        assertNull(SmsCodeUtil.extractJdCode("【某银行】验证码：123456"));
        assertNull(SmsCodeUtil.extractJdCode("【京东】您的订单号123456789已发货"));
        assertNull(SmsCodeUtil.extractJdCode(null));
    }
}
