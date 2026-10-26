package cn.xanderye.android.jdck.config;

import android.webkit.WebView;

import java.lang.ref.WeakReference;

/**
 * 全局持有当前界面 WebView，供短信接收器注入验证码。
 * 使用弱引用，避免单例长期持有 Activity 造成泄漏。
 *
 * @author XanderYe
 * @description:
 * @date 2021/8/19 20:36
 */
public class Config {

    private static final Config CONFIG = new Config();

    private WeakReference<WebView> webViewRef;

    private Config() {
    }

    public static Config getInstance() {
        return CONFIG;
    }

    public WebView getWebView() {
        return webViewRef == null ? null : webViewRef.get();
    }

    public void setWebView(WebView webView) {
        webViewRef = webView == null ? null : new WeakReference<>(webView);
    }
}
