package cn.xanderye.android.jdck.activity;

import android.app.AlertDialog;
import android.content.*;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import cn.xanderye.android.jdck.BuildConfig;
import cn.xanderye.android.jdck.R;
import cn.xanderye.android.jdck.config.Config;
import cn.xanderye.android.jdck.entity.PanelType;
import cn.xanderye.android.jdck.entity.QlEnv;
import cn.xanderye.android.jdck.entity.QlInfo;
import cn.xanderye.android.jdck.receiver.SMSReceiver;
import cn.xanderye.android.jdck.util.JDUtil;
import cn.xanderye.android.jdck.util.PanelStore;
import cn.xanderye.android.jdck.util.PanelUtil;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity {

    private Context context;

    private Button addBtn, delBtn, inputBtn, getCookieBtn, clearCookieBtn,sendCK;

    private Spinner phoneSpinner;

    private WebView webView;

    private static final String JD_URL = "https://home.m.jd.com/myJd/home.action";

    private static final Pattern PHONE_PATTERN = Pattern.compile("1\\d{10}");

    private SharedPreferences config;

    private String cookie = null;

    private Set<String> phoneSet = new HashSet<>();

    private Boolean smsEnabled;
    private SMSReceiver smsReceiver;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;

        // 配置存储
        config = getSharedPreferences("CONFIG", Context.MODE_PRIVATE);
        // 获取短信接收器配置
        smsEnabled = config.getBoolean("smsEnabled", false);
        if (smsEnabled) {
            // 注册接收器
            registerSMSReceiver();
        }

        webView = findViewById(R.id.webView);
        Config.getInstance().setWebView(webView);
        //支持javascript
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        //自适应屏幕
        webView.getSettings().setLoadWithOverviewMode(true);
        resetWebview();

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                //添加Cookie获取操作
                CookieManager cookieManager = CookieManager.getInstance();
                cookie = cookieManager.getCookie(url);
                super.onPageFinished(view, url);
            }
        });
        // 配置账号下拉框
        phoneSpinner = findViewById(R.id.phoneSpinner);
        String phoneStr = config.getString("phoneStr", null);
        if (StringUtils.isNotBlank(phoneStr)) {
            String[] phones = phoneStr.split("\r\n");
            phoneSet = new HashSet<>(Arrays.asList(phones));
            updatePhone();
        }
        // 添加按钮
        addBtn = findViewById(R.id.addBtn);
        addBtn.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(R.string.add_account_title);
            final AlertDialog dialog = builder.create();
            View dialogView = View.inflate(context, R.layout.activity_phone, null);
            //设置对话框布局
            dialog.setView(dialogView);
            dialog.show();

            EditText phoneText = dialogView.findViewById(R.id.phoneText);
            EditText passwordText = dialogView.findViewById(R.id.passwordText);
            Button confirmBtn = dialogView.findViewById(R.id.confirmBtn);
            confirmBtn.setOnClickListener(v2 -> {
                String phone = phoneText.getText().toString().trim();
                String password = passwordText.getText().toString().trim();
                if (StringUtils.isBlank(phone)) {
                    Toast.makeText(this, "账号输入错误", Toast.LENGTH_SHORT).show();
                    return;
                }
                dialog.cancel();
                phoneSet.add(StringUtils.isBlank(password) ? phone : phone + " " + password);
                updatePhone();
                Toast.makeText(this, "添加成功", Toast.LENGTH_SHORT).show();
            });
        });
        // 删除按钮
        delBtn = findViewById(R.id.delBtn);
        delBtn.setOnClickListener(v -> {
            String selectedPhone = (String) phoneSpinner.getSelectedItem();
            if (selectedPhone == null) {
                Toast.makeText(this, "请先选择账号", Toast.LENGTH_SHORT).show();
                return;
            }
            phoneSet = phoneSet.stream().filter(phone -> !phone.equals(selectedPhone)).collect(Collectors.toSet());
            // 更新账号
            updatePhone();
            Toast.makeText(this, "删除成功", Toast.LENGTH_SHORT).show();
        });

        // 一键输入按钮
        inputBtn = findViewById(R.id.inputBtn);
        inputBtn.setOnClickListener(v -> {
            String selectedPhone = (String) phoneSpinner.getSelectedItem();
            if (selectedPhone == null) {
                Toast.makeText(this, "请先选择账号", Toast.LENGTH_SHORT).show();
                return;
            }
            String[] info=  selectedPhone.split(" ");
            String Phone=info[0];
            String execJs = "var account='" + Phone + "';";
            execJs += "document.getElementsByClassName('policy_tip-checkbox')[0].click();";
            execJs += "var evt=new InputEvent('input',{inputType:'insertText',data:account,dataTransfer:null,isComposing:false});";
            execJs += "document.getElementById('username').value=account;";
            execJs += "document.getElementById('username').dispatchEvent(evt);";
            Matcher matcher = PHONE_PATTERN.matcher(Phone);
            if (matcher.matches()) {
                execJs += "document.getElementsByClassName('acc-input mobile J_ping')[0].value=account;";
                execJs += "document.getElementsByClassName('acc-input mobile J_ping')[0].dispatchEvent(evt);";
            }
            if(info.length>1){
                String Pwd=info[1];
                execJs += "var password='" + Pwd + "';";
                execJs += "var evt=new InputEvent('input',{inputType:'insertText',data:password,dataTransfer:null,isComposing:false});";
                execJs += "document.getElementById('pwd').value=password;";
                execJs += "document.getElementById('pwd').dispatchEvent(evt);";
                execJs += "document.querySelector('#app>div>a').click()";
            }

            webView.loadUrl("javascript:" + execJs);
        });
        // 获取cookie按钮
        getCookieBtn = findViewById(R.id.getCookieBtn);
        getCookieBtn.setOnClickListener(v -> {
            Map<String, Object> map = JDUtil.formatCookies(cookie);
            String ptKey = (String) map.get("pt_key");
            String ptPin = (String) map.get("pt_pin");
            if (StringUtils.isAnyBlank(ptKey, ptPin)) {
                Toast.makeText(this, "未获取到Cookie，请先登录", Toast.LENGTH_SHORT).show();
                return;
            }
            String selectedPhone = (String) phoneSpinner.getSelectedItem();
            String Phone="";
            if (selectedPhone != null) {
                String[] info=  selectedPhone.split(" ");
                Phone=info[0];
            }

            String cookie = MessageFormat.format("pt_key={0};pt_pin={1};", ptKey, ptPin);
            copyToClipboard(cookie);
            if (enabledPanels().isEmpty()) {
                Toast.makeText(this, "获取成功，已复制到剪切板", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "获取成功，已复制到剪切板，尝试同步更新面板环境变量", Toast.LENGTH_SHORT).show();
                updateCookie(cookie,Phone);
            }
        });
        // 重置cookie刷新页面按钮
        clearCookieBtn = findViewById(R.id.clearCookieBtn);
        clearCookieBtn.setOnClickListener(v -> {
//            updateCookie("pt_key=test;pt_pin=Yclown;");
            resetWebview();
        });

        // 检查已启用面板的token有效 同时获取环境变量
        checkPanels();
    }

    @Override
    protected void onDestroy() {
        unregisterSMSReceiver();
        super.onDestroy();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        String text = smsEnabled ? "关闭短信识别" : "开启短信识别";
        menu.add(Menu.NONE, 1, 1, "面板设置");
        menu.add(Menu.NONE, 2, 2, text);
        menu.add(Menu.NONE, 3, 3, "关于");
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case 1: {
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
            } break;
            case 2: {
                String toastText;
                if (smsEnabled) {
                    toastText = "关闭成功，请重启应用生效";
                } else {
                    String[] permissions = new String[]{"android.permission.RECEIVE_SMS", "android.permission.READ_SMS"};
                    boolean result = checkPermissionAllGranted(permissions);
                    if (!result) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        builder.setMessage("未获取到短信读取权限，是否跳转设置页面并开启权限（MIUI需要额外开启通知类短信权限）？");
                        builder.setPositiveButton("设置", (dialog, which) -> {
                            openSettings();
                        });
                        builder.create().show();
                        break;
                    }
                    toastText = "开启成功，请重启应用生效";
                }
                Toast.makeText(this, toastText, Toast.LENGTH_LONG).show();
                smsEnabled = !smsEnabled;
                String text = smsEnabled ? "关闭短信识别" : "开启短信识别";
                item.setTitle(text);
                SharedPreferences.Editor edit = config.edit();
                edit.putBoolean("smsEnabled", smsEnabled);
                edit.apply();
            } break;
            case 3: {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setMessage(getString(R.string.app_name) + " v" + BuildConfig.VERSION_NAME + "\n@XanderYe 版权所有 @yclown修改");
                builder.setPositiveButton("项目页面", (dialog, which) -> {
                    Uri uri = Uri.parse("https://github.com/yclown/jdck-android");
                    Intent intent = new Intent();
                    intent.setAction("android.intent.action.VIEW");
                    intent.setData(uri);
                    startActivity(intent);
                });
                builder.create().show();
            }
            break;
        }
        return true;
    }

    /**
     * 检查权限
     * @param permissions
     * @return boolean
     * @author XanderYe
     * @date 2022/12/30
     */
    private boolean checkPermissionAllGranted(String[] permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * 打开设置
     * @param
     * @return void
     * @author XanderYe
     * @date 2022/12/30
     */
    private void openSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.fromParts("package", getPackageName(), null));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    /**
     * 注册短信接收器
     * @param
     * @return void
     * @author XanderYe
     * @date 2022/12/30
     */
    private void registerSMSReceiver() {
        smsReceiver = new SMSReceiver();
        IntentFilter filter = new IntentFilter();
        filter.setPriority(1000);
        filter.addAction(SMSReceiver.SMS_RECEIVED);
        registerReceiver(smsReceiver, filter);
    }

    /**
     * 取消注册短信接收器
     * @param
     * @return void
     * @author XanderYe
     * @date 2022/12/30
     */
    private void unregisterSMSReceiver() {
        if (smsReceiver != null) {
            unregisterReceiver(smsReceiver);
            smsReceiver = null;
        }
    }

    /**
     * 清空cookie并加载页面
     * @param
     * @return void
     * @author XanderYe
     * @date 2022/5/10
     */
    private void resetWebview() {
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeAllCookies(null);
        cookieManager.flush();
        webView.clearCache(true);
        webView.loadUrl(JD_URL);
    }

    /**
     * 更新下拉和存储中的账号
     * @return void
     * @author XanderYe
     * @date 2022/5/10
     */
    private void updatePhone() {
        // 更新账号
        String newPhoneStr = phoneSet.stream().collect(Collectors.joining("\r\n"));
        SharedPreferences.Editor edit = config.edit();
        edit.putString("phoneStr", newPhoneStr);
        edit.apply();
        String[] phones = phoneSet.toArray(new String[0]);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.item_phone, phones);
        phoneSpinner.setAdapter(adapter);
    }

    /**
     * 复制文字到剪切板
     * @param copyStr
     * @return boolean
     * @author XanderYe
     * @date 2022/5/10
     */
    private boolean copyToClipboard(String copyStr) {
        try {
            //获取剪贴板管理器
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            // 创建普通字符型ClipData
            ClipData mClipData = ClipData.newPlainText("Label", copyStr);
            // 将ClipData内容放到系统剪贴板里。
            cm.setPrimaryClip(mClipData);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void checkPanels() {
        List<QlInfo> panels = enabledPanels();
        if (panels.isEmpty()) {
            return;
        }
        ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();
        singleThreadExecutor.execute(() -> {
            List<String> valid = new ArrayList<>();
            List<String> failed = new ArrayList<>();
            for (QlInfo qlInfo : panels) {
                String name = panelName(qlInfo.getPanelType());
                try {
                    String oldToken = qlInfo.getToken();
                    List<QlEnv> qlEnvList = PanelUtil.getEnvList(qlInfo,"");
                    Config.getInstance().getPanelInfos().put(qlInfo.getPanelType(), qlInfo);
                    Config.getInstance().getEnvLists().put(qlInfo.getPanelType(), qlEnvList);
                    // 呆呆面板令牌过期后已自动重登，持久化新令牌避免每次启动都重新登录
                    if (!StringUtils.equals(oldToken, qlInfo.getToken())) {
                        PanelStore.save(config, qlInfo);
                    }
                    valid.add(name);
                } catch (IOException e) {
                    failed.add(name + "连接失败：" + e.getMessage());
                }
            }
            runOnUiThread(() -> {
                if (!valid.isEmpty()) {
                    Toast.makeText(this, StringUtils.join(valid, "、") + "token有效", Toast.LENGTH_SHORT).show();
                }
                for (String message : failed) {
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                }
            });
        });
        singleThreadExecutor.shutdown();
    }

    private List<QlInfo> enabledPanels() {
        List<QlInfo> panels = new ArrayList<>();
        for (QlInfo qlInfo : PanelStore.loadAll(config)) {
            if (qlInfo.isEnabled() && StringUtils.isNotBlank(qlInfo.getToken())) {
                panels.add(qlInfo);
            }
        }
        return panels;
    }

    private String panelName(PanelType type) {
        return type == PanelType.DAIDAI ? "呆呆面板" : "青龙面板";
    }

    /**
     * 调用面板接口更新cookie
     * @param cookie
     * @return void
     * @author XanderYe
     * @date 2022/5/11
     */
    private void updateCookie(String cookie,String phone) {
        Map<String, Object> map = JDUtil.formatCookies(cookie);
        String ptPin = (String) map.get("pt_pin");
        List<QlInfo> panels = enabledPanels();
        ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();
        singleThreadExecutor.execute(() -> {
            List<String> success = new ArrayList<>();
            List<String> failed = new ArrayList<>();
            for (QlInfo qlInfo : panels) {
                String name = panelName(qlInfo.getPanelType());
                try {
                    updatePanelCookie(qlInfo, cookie, phone, ptPin);
                    success.add(name);
                } catch (IOException e) {
                    failed.add(name + "更新失败：" + e.getMessage());
                }
            }
            runOnUiThread(() -> {
                if (!success.isEmpty()) {
                    Toast.makeText(this, "更新cookie成功：" + StringUtils.join(success, "、"), Toast.LENGTH_SHORT).show();
                }
                for (String message : failed) {
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                }
            });
        });
        singleThreadExecutor.shutdown();
    }

    private void updatePanelCookie(QlInfo qlInfo, String cookie, String phone, String ptPin) throws IOException {
        // 每次更新前拉取最新环境变量，避免缓存过期导致重复新增
        List<QlEnv> qlEnvList = PanelUtil.getEnvList(qlInfo, "");
        Config.getInstance().getEnvLists().put(qlInfo.getPanelType(), qlEnvList);
        QlEnv targetEnv = null;
        for (QlEnv qlEnv : qlEnvList) {
            Map<String, Object> envMap = JDUtil.formatCookies(qlEnv.getValue());
            String tempPin = (String) envMap.get("pt_pin");
            if (ptPin.equals(tempPin)) {
                targetEnv = qlEnv;
                break;
            }
        }
        boolean isNew = targetEnv == null;
        if (isNew) {
            targetEnv = new QlEnv();
            targetEnv.setName("JD_COOKIE");
        }
        targetEnv.setValue(cookie);
        targetEnv.setRemarks(phone);
        boolean success = PanelUtil.saveEnv(qlInfo, targetEnv);
        if (!success) {
            throw new IOException("保存环境变量失败");
        }
        PanelUtil.enableEnv(qlInfo, targetEnv);
        if (isNew) {
            qlEnvList.add(targetEnv);
        }
        Config.getInstance().getPanelInfos().put(qlInfo.getPanelType(), qlInfo);
        // 面板可能在请求过程中自动重登，持久化刷新后的令牌
        PanelStore.save(config, qlInfo);
    }
}