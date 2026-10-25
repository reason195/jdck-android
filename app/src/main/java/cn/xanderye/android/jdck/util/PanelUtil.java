package cn.xanderye.android.jdck.util;

import cn.xanderye.android.jdck.entity.PanelType;
import cn.xanderye.android.jdck.entity.QlEnv;
import cn.xanderye.android.jdck.entity.QlInfo;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.List;

/**
 * 面板操作统一入口，根据面板类型分发到青龙或呆呆面板实现。
 */
public class PanelUtil {

    private PanelUtil() {
    }

    public static String login(QlInfo qlInfo) throws IOException {
        if (isDaidai(qlInfo)) {
            return DaidaiUtil.login(qlInfo);
        }
        return QinglongUtil.login(qlInfo);
    }

    public static List<QlEnv> getEnvList(QlInfo qlInfo, String keyword) throws IOException {
        return withReLogin(qlInfo, () -> {
            if (isDaidai(qlInfo)) {
                return DaidaiUtil.getEnvList(qlInfo, keyword);
            }
            return QinglongUtil.getEnvList(qlInfo, keyword);
        });
    }

    public static boolean saveEnv(QlInfo qlInfo, QlEnv qlEnv) throws IOException {
        return withReLogin(qlInfo, () -> {
            if (isDaidai(qlInfo)) {
                return DaidaiUtil.saveEnv(qlInfo, qlEnv);
            }
            return QinglongUtil.saveEnv(qlInfo, qlEnv);
        });
    }

    public static void enableEnv(QlInfo qlInfo, QlEnv qlEnv) throws IOException {
        withReLogin(qlInfo, () -> {
            if (isDaidai(qlInfo)) {
                DaidaiUtil.enableEnv(qlInfo, qlEnv);
            } else {
                QinglongUtil.EableEnv(qlInfo, qlEnv);
            }
            return null;
        });
    }

    private static boolean isDaidai(QlInfo qlInfo) {
        return qlInfo.getPanelType() == PanelType.DAIDAI;
    }

    /**
     * 呆呆面板令牌有效期 24 小时，过期返回 401 时用已保存的 AppKey/AppSecret 自动重登一次。
     */
    private static <T> T withReLogin(QlInfo qlInfo, Action<T> action) throws IOException {
        try {
            return action.run();
        } catch (PanelAuthException e) {
            if (e.getStatusCode() != 401 || StringUtils.isAnyBlank(qlInfo.getUsername(), qlInfo.getPassword())) {
                throw e;
            }
            qlInfo.setToken(login(qlInfo));
            return action.run();
        }
    }

    private interface Action<T> {
        T run() throws IOException;
    }
}
