package cn.xanderye.android.jdck.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SmsCodeUtil {

    private static final String JD_KEYWORD = "京东";

    private static final Pattern JD_CODE_PATTERN =
            Pattern.compile("(?:验证码|校验码|动态码)[^0-9]{0,12}(\\d{6})");

    private SmsCodeUtil() {
    }

    public static String extractJdCode(String body) {
        if (body == null || !body.contains(JD_KEYWORD)) {
            return null;
        }
        Matcher matcher = JD_CODE_PATTERN.matcher(body);
        return matcher.find() ? matcher.group(1) : null;
    }
}
