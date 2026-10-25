package cn.xanderye.android.jdck.util;

import cn.xanderye.android.jdck.entity.QlEnv;
import cn.xanderye.android.jdck.entity.QlInfo;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author XanderYe
 * @description:
 * @date 2022/5/11 14:04
 */
public class QinglongUtil {

    /**
     * 登录
     * @param qlInfo
     * @return java.lang.String
     * @description:
     * @date 2024/3/22 11:00
     */

    public static String login(QlInfo qlInfo) throws IOException {
        String url = baseUrl(qlInfo) + "/open/auth/token?client_id=" + qlInfo.getUsername()
                + "&client_secret=" + qlInfo.getPassword();

        HttpUtil.ResEntity resEntity = HttpUtil.doGet(url, null);
        if (resEntity.getStatusCode() == 401) {
            throw new IOException("登录失败：ClientID 或 ClientSecret 错误");
        }
        if (resEntity.getStatusCode() != 200) {
            throw new IOException("服务器" + resEntity.getStatusCode() + "错误");
        }
        JSONObject res = parseJson(resEntity, "登录");
        if (code(res) != 200) {
            throw new IOException(res.getString("message"));
        }
        JSONObject data = res.getJSONObject("data");
        if (data == null) {
            throw new IOException("登录失败，响应数据为空");
        }
        String token = data.getString("token");
        if (StringUtils.isBlank(token)) {
            throw new IOException("登录失败，token为空");
        }
        return token;
    }

    /**
     * 获取环境变量
     *
     * @date 2024/3/22 11:00
     */
    public static List<QlEnv> getEnvList(QlInfo qlInfo, String key) throws IOException {
        String url = baseUrl(qlInfo) + "/open/envs?searchValue=" + key;
        HttpUtil.ResEntity resEntity = HttpUtil.doGet(url, authHeaders(qlInfo), null, null);
        JSONObject res = parseResponse(resEntity, "获取环境变量");
        JSONArray data = res.getJSONArray("data");
        if (data == null) {
            throw new IOException("获取环境变量失败，响应数据为空");
        }
        return data.toJavaList(QlEnv.class);
    }

    /**
     * 更新环境变量
     * @param qlInfo
     * @param qlEnv
     * @return boolean
     * @author yclown
     * @description:
     * @date 2024/3/22 11:00
     */
    public static boolean saveEnv(QlInfo qlInfo, QlEnv qlEnv) throws IOException {
        String url = baseUrl(qlInfo) + "/open/envs";
        JSONObject params = new JSONObject();
        params.put("name", qlEnv.getName());
        params.put("remarks", qlEnv.getRemarks());
        params.put("value", qlEnv.getValue());
        HttpUtil.ResEntity resEntity;
        String envId = resolveEnvId(qlEnv);
        if (envId != null) {
            // 更新
            params.put("id", envId);
            resEntity = HttpUtil.doPutJSON(url, authHeaders(qlInfo), null, params.toJSONString());
        } else {
            // 新增
            JSONArray adds = new JSONArray();
            adds.add(params);
            resEntity = HttpUtil.doPostJSON(url, authHeaders(qlInfo), null, adds.toJSONString());
        }
        JSONObject res = parseResponse(resEntity, "保存环境变量");
        if (envId == null) {
            applyCreatedEnvId(qlEnv, res);
        }
        return true;
    }

    /**
     * 启用环境变量
     * @param qlInfo
     * @param qlEnv
     * @author yclown
     * @description:
     * @date 2024/3/22 11:00
     */
    public static void enableEnv(QlInfo qlInfo, QlEnv qlEnv) throws IOException {
        String envId = resolveEnvId(qlEnv);
        if (envId == null) {
            return;
        }
        String url = baseUrl(qlInfo) + "/open/envs/enable";
        JSONArray ids = new JSONArray();
        ids.add(envId);
        HttpUtil.ResEntity resEntity = HttpUtil.doPutJSON(url, authHeaders(qlInfo), null, ids.toJSONString());
        parseResponse(resEntity, "启用环境变量");
    }

    private static JSONObject parseResponse(HttpUtil.ResEntity resEntity, String action) throws IOException {
        if (resEntity.getStatusCode() == 401) {
            throw new PanelAuthException("青龙token已失效，请重新登录", 401);
        }
        if (resEntity.getStatusCode() != 200) {
            throw new IOException(action + "失败：服务器" + resEntity.getStatusCode() + "错误");
        }
        JSONObject res = parseJson(resEntity, action);
        int code = code(res);
        if (code == 401) {
            throw new PanelAuthException("青龙token已失效，请重新登录", 401);
        }
        if (code != 200) {
            String message = res.getString("message");
            throw new IOException(StringUtils.isBlank(message) ? action + "失败，服务器返回异常" : message);
        }
        return res;
    }

    private static JSONObject parseJson(HttpUtil.ResEntity resEntity, String action) throws IOException {
        try {
            JSONObject res = JSON.parseObject(resEntity.getResponse());
            if (res == null) {
                throw new IOException(action + "失败，响应内容无效");
            }
            return res;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(action + "失败，响应内容无效");
        }
    }

    private static int code(JSONObject res) {
        Integer code = res.getInteger("code");
        return code == null ? -1 : code;
    }

    private static Map<String, Object> authHeaders(QlInfo qlInfo) {
        Map<String, Object> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + qlInfo.getToken());
        return headers;
    }

    private static String baseUrl(QlInfo qlInfo) {
        String url = qlInfo.getAddress() == null ? "" : qlInfo.getAddress().trim();
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    /**
     * 青龙新增环境变量后回填服务端生成的 id，后续启用时才不会跳过。
     */
    private static void applyCreatedEnvId(QlEnv qlEnv, JSONObject res) {
        Object data = res.get("data");
        Object created = null;
        if (data instanceof JSONObject) {
            created = data;
        } else if (data instanceof JSONArray && !((JSONArray) data).isEmpty()) {
            created = ((JSONArray) data).get(0);
        }
        if (created instanceof JSONObject) {
            JSONObject env = (JSONObject) created;
            String id = env.getString("id");
            if (StringUtils.isNotBlank(id)) {
                qlEnv.setDaidaiId(id);
                return;
            }
            String legacyId = env.getString("_id");
            if (StringUtils.isNotBlank(legacyId)) {
                qlEnv.setQlId(legacyId);
            }
        } else if (created instanceof String && StringUtils.isNotBlank((String) created)) {
            qlEnv.setDaidaiId((String) created);
        }
    }

    /**
     * 青龙旧版环境变量主键为 _id，新版 Open API 返回 id。
     */
    private static String resolveEnvId(QlEnv qlEnv) {
        return qlEnv.getQlId() != null ? qlEnv.getQlId() : qlEnv.getDaidaiId();
    }
}
