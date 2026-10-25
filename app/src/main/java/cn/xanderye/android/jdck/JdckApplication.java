package cn.xanderye.android.jdck;

import android.app.Application;
import com.alibaba.fastjson.parser.ParserConfig;

public class JdckApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // 本项目只解析固定类型，不需要 fastjson 的 autoType
        ParserConfig.getGlobalInstance().setSafeMode(true);
    }
}
