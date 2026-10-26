package cn.xanderye.android.jdck.activity;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import cn.xanderye.android.jdck.R;
import cn.xanderye.android.jdck.entity.PanelType;
import cn.xanderye.android.jdck.entity.QlInfo;
import cn.xanderye.android.jdck.util.PanelStore;
import cn.xanderye.android.jdck.util.PanelUtil;
import cn.xanderye.android.jdck.util.TaskExecutor;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

/**
 * @origin XanderYe
 * @author yclown
 * @description:
 * @date 2024/3/22 11:00
 */
public class LoginActivity extends AppCompatActivity {

    private SharedPreferences config;

    private EditText addressText, usernameText, passwordText;

    private TextView usernameLabel, passwordLabel;

    private RadioGroup panelTypeGroup;

    private RadioButton qinglongRadio, daidaiRadio;

    private CheckBox enabledCheck;

    private Button loginBtn, cancelBtn;

    private final Map<PanelType, QlInfo> panels = new EnumMap<>(PanelType.class);

    private PanelType editingType = PanelType.QINGLONG;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // 配置存储
        config = getSharedPreferences("CONFIG", Context.MODE_PRIVATE);
        for (QlInfo info : PanelStore.loadAll(config)) {
            panels.put(info.getPanelType(), info);
        }

        addressText = findViewById(R.id.addressText);
        usernameText = findViewById(R.id.usernameText);
        passwordText = findViewById(R.id.passwordText);
        usernameLabel = findViewById(R.id.usernameLabel);
        passwordLabel = findViewById(R.id.passwordLabel);
        panelTypeGroup = findViewById(R.id.panelTypeGroup);
        qinglongRadio = findViewById(R.id.qinglongRadio);
        daidaiRadio = findViewById(R.id.daidaiRadio);
        enabledCheck = findViewById(R.id.panelEnabledCheck);

        // 已有配置时优先展示，默认编辑青龙面板
        editingType = panels.isEmpty() || panels.containsKey(PanelType.QINGLONG)
                ? PanelType.QINGLONG : PanelType.DAIDAI;
        if (editingType == PanelType.DAIDAI) {
            daidaiRadio.setChecked(true);
        } else {
            qinglongRadio.setChecked(true);
        }
        applyPanelType(editingType);
        fillForm(panels.get(editingType));

        panelTypeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            PanelType type = checkedId == R.id.daidaiRadio ? PanelType.DAIDAI : PanelType.QINGLONG;
            if (type == editingType) {
                return;
            }
            // 切换前暂存当前输入，避免未保存的内容丢失
            stashForm();
            editingType = type;
            applyPanelType(type);
            fillForm(panels.get(type));
        });

        loginBtn = findViewById(R.id.loginBtn);
        loginBtn.setOnClickListener(v -> login());

        cancelBtn = findViewById(R.id.cancelBtn);
        cancelBtn.setOnClickListener(v -> this.finish());
    }

    private void login() {
        String addr = addressText.getEditableText().toString();
        String user = usernameText.getEditableText().toString();
        String pwd = passwordText.getEditableText().toString();

        if (StringUtils.isBlank(addr)) {
            Toast.makeText(this, "地址不能为空", Toast.LENGTH_SHORT).show();
            return;
        }
        if (StringUtils.isAnyBlank(user, pwd)) {
            Toast.makeText(this, "请输入必要参数", Toast.LENGTH_SHORT).show();
            return;
        }
        PanelType type = editingType;
        QlInfo qlInfo = new QlInfo();
        qlInfo.setAddress(addr);
        qlInfo.setUsername(user);
        qlInfo.setPassword(pwd);
        qlInfo.setPanelType(type);
        qlInfo.setEnabled(enabledCheck.isChecked());

        TaskExecutor.execute(() -> {
            try {
                String token = PanelUtil.login(qlInfo);
                if (StringUtils.isBlank(token)) {
                    toastOnUiThread("登录失败，token为空");
                    return;
                }
                qlInfo.setToken(token);
                // 登录后更新环境变量，同时验证应用是否具备环境变量权限
                PanelUtil.getEnvList(qlInfo, "");
                PanelStore.save(config, qlInfo);
                runOnUiThread(() -> {
                    Toast.makeText(this, panelName(type) + "配置成功", Toast.LENGTH_SHORT).show();
                    this.finish();
                });
            } catch (IOException e) {
                toastOnUiThread(e.getMessage());
            }
        });
    }

    private void applyPanelType(PanelType panelType) {
        boolean daidai = panelType == PanelType.DAIDAI;
        usernameLabel.setText(daidai ? R.string.dd_username_text : R.string.ql_username_text);
        passwordLabel.setText(daidai ? R.string.dd_password_text : R.string.ql_password_text);
        usernameText.setHint(daidai ? "App Key" : "ClientID");
        passwordText.setHint(daidai ? "App Secret" : "ClientSecret");
    }

    private void fillForm(QlInfo qlInfo) {
        if (qlInfo == null) {
            addressText.setText("");
            usernameText.setText("");
            passwordText.setText("");
            enabledCheck.setChecked(true);
            return;
        }
        addressText.setText(qlInfo.getAddress());
        usernameText.setText(qlInfo.getUsername());
        passwordText.setText(qlInfo.getPassword());
        enabledCheck.setChecked(qlInfo.isEnabled());
    }

    private void stashForm() {
        QlInfo qlInfo = panels.get(editingType);
        if (qlInfo == null) {
            qlInfo = new QlInfo();
        }
        qlInfo.setPanelType(editingType);
        qlInfo.setAddress(addressText.getEditableText().toString());
        qlInfo.setUsername(usernameText.getEditableText().toString());
        qlInfo.setPassword(passwordText.getEditableText().toString());
        qlInfo.setEnabled(enabledCheck.isChecked());
        panels.put(editingType, qlInfo);
    }

    private void toastOnUiThread(String message) {
        runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
    }

    private String panelName(PanelType type) {
        return type == PanelType.DAIDAI ? "呆呆面板" : "青龙面板";
    }
}
