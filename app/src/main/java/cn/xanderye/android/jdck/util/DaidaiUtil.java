package cn.xanderye.android.jdck.util;

import cn.xanderye.android.jdck.entity.QlEnv;
import cn.xanderye.android.jdck.entity.QlInfo;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 呆呆面板（daidai-panel）Open API 适配。
 * 使用 AppKey/AppSecret 换取 Bearer 令牌，接口前缀为 /api/v1。
 */
public class DaidaiUtil {

    private DaidaiUtil() {
    }

    public static String login(QlInfo qlInfo) throws IOException {
        String url = baseUrl(qlInfo) + "/api/v1/open-api/token";
        JSONObject params = new JSONObject();
        params.put("app_key", qlInfo.getUsername());
        params.put("app_secret", qlInfo.getPassword());

        HttpUtil.ResEntity resEntity = HttpUtil.doPostJSON(url, null, null, params.toJSONString());
        JSONObject res = parseResponse(resEntity, "登录");
        JSONObject data = res.getJSONObject("data");
        String token = data == null ? null : data.getString("access_token");
        if (StringUtils.isBlank(token)) {
            throw new IOException("登录失败，token为空");
        }
        return token;
    }

    public static List<QlEnv> getEnvList(QlInfo qlInfo, String keyword) throws IOException {
        List<QlEnv> envList = new ArrayList<>();
        int page = 1;
        while (true) {
            String url = baseUrl(qlInfo) + "/api/v1/envs?all=1&page=" + page + "&page_size=100";
            if (StringUtils.isNotBlank(keyword)) {
                url += "&keyword=" + URLEncoder.encode(keyword, "UTF-8");
            }
            JSONObject res = parseResponse(HttpUtil.doGet(url, authHeaders(qlInfo), null, null), "获取环境变量");
            JSONArray data = res.getJSONArray("data");
            if (data == null || data.isEmpty()) {
                break;
            }
            envList.addAll(data.toJavaList(QlEnv.class));
            int total = res.getIntValue("total");
            if ((total > 0 && envList.size() >= total) || data.size() < 100) {
                break;
            }
            page++;
        }
        return envList;
    }

    public static boolean saveEnv(QlInfo qlInfo, QlEnv qlEnv) throws IOException {
        JSONObject params = new JSONObject();
        params.put("name", qlEnv.getName());
        params.put("remarks", qlEnv.getRemarks());
        params.put("value", qlEnv.getValue());

        if (qlEnv.getDaidaiId() != null) {
            String url = baseUrl(qlInfo) + "/api/v1/envs/" + qlEnv.getDaidaiId();
            parseResponse(HttpUtil.doPutJSON(url, authHeaders(qlInfo), null, params.toJSONString()), "更新环境变量");
        } else {
            String url = baseUrl(qlInfo) + "/api/v1/envs";
            JSONObject res = parseResponse(HttpUtil.doPostJSON(url, authHeaders(qlInfo), null, params.toJSONString()), "新增环境变量");
            JSONObject data = res.getJSONObject("data");
            if (data != null && data.getString("id") != null) {
                qlEnv.setDaidaiId(data.getString("id"));
            }
        }
        return true;
    }

    public static void enableEnv(QlInfo qlInfo, QlEnv qlEnv) throws IOException {
        // 没有 id 说明变量尚未创建，服务端新建时默认启用
        if (qlEnv.getDaidaiId() == null) {
            return;
        }
        String url = baseUrl(qlInfo) + "/api/v1/envs/" + qlEnv.getDaidaiId() + "/enable";
        parseResponse(HttpUtil.doPutJSON(url, authHeaders(qlInfo), null, null), "启用环境变量");
    }

    private static String baseUrl(QlInfo qlInfo) {
        String url = qlInfo.getAddress() == null ? "" : qlInfo.getAddress().trim();
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    private static Map<String, Object> authHeaders(QlInfo qlInfo) {
        Map<String, Object> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + qlInfo.getToken());
        return headers;
    }

    private static JSONObject parseResponse(HttpUtil.ResEntity resEntity, String action) throws IOException {
        int statusCode = resEntity.getStatusCode();
        JSONObject res = null;
        try {
            res = JSON.parseObject(resEntity.getResponse());
        } catch (Exception ignored) {
        }
        if (statusCode == 401 || statusCode == 403) {
            throw new PanelAuthException(errorMessage(res, action, statusCode), statusCode);
        }
        if (statusCode < 200 || statusCode >= 300) {
            throw new IOException(errorMessage(res, action, statusCode));
        }
        if (res == null) {
            throw new IOException(action + "失败，响应内容无效");
        }
        return res;
    }

    private static String errorMessage(JSONObject res, String action, int statusCode) {
        String message = res == null ? null : res.getString("error");
        if (StringUtils.isNotBlank(message)) {
            return action + "失败：" + message;
        }
        return action + "失败，服务器" + statusCode + "错误";
    }
}
