package cn.xanderye.android.jdck.util;

import android.content.SharedPreferences;
import cn.xanderye.android.jdck.entity.PanelType;
import cn.xanderye.android.jdck.entity.QlInfo;
import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 按面板类型分别持久化配置，兼容旧版本的单面板 qlJSON。
 */
public final class PanelStore {

    private static final String LEGACY_KEY = "qlJSON";

    private static final String KEY_PREFIX = "qlJSON_";

    private PanelStore() {
    }

    public static List<QlInfo> loadAll(SharedPreferences config) {
        migrateLegacy(config);
        List<QlInfo> panels = new ArrayList<>();
        for (PanelType type : PanelType.values()) {
            QlInfo info = load(config, type);
            if (info != null) {
                panels.add(info);
            }
        }
        return panels;
    }

    public static QlInfo load(SharedPreferences config, PanelType type) {
        String json = config.getString(key(type), null);
        if (StringUtils.isBlank(json)) {
            return null;
        }
        QlInfo info = JSON.parseObject(json, QlInfo.class);
        if (info != null) {
            info.setPanelType(type);
        }
        return info;
    }

    public static void save(SharedPreferences config, QlInfo info) {
        config.edit().putString(key(info.getPanelType()), JSON.toJSONString(info)).apply();
    }

    private static void migrateLegacy(SharedPreferences config) {
        String legacy = config.getString(LEGACY_KEY, null);
        if (legacy == null) {
            return;
        }
        QlInfo info = JSON.parseObject(legacy, QlInfo.class);
        SharedPreferences.Editor edit = config.edit();
        if (info != null && !config.contains(key(info.getPanelType()))) {
            edit.putString(key(info.getPanelType()), JSON.toJSONString(info));
        }
        edit.remove(LEGACY_KEY).apply();
    }

    private static String key(PanelType type) {
        return KEY_PREFIX + type.name();
    }
}
