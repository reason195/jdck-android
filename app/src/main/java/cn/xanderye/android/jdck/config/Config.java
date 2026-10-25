package cn.xanderye.android.jdck.config;

import android.webkit.WebView;
import cn.xanderye.android.jdck.entity.PanelType;
import cn.xanderye.android.jdck.entity.QlEnv;
import cn.xanderye.android.jdck.entity.QlInfo;
import lombok.Data;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * @author XanderYe
 * @description:
 * @date 2021/8/19 20:36
 */
@Data
public class Config {

    private static final Config CONFIG = new Config();

    private WebView webView;

    private final Map<PanelType, QlInfo> panelInfos = new EnumMap<>(PanelType.class);

    private final Map<PanelType, List<QlEnv>> envLists = new EnumMap<>(PanelType.class);

    private Config() {
    }

    public static Config getInstance() {
        return CONFIG;
    }
}
