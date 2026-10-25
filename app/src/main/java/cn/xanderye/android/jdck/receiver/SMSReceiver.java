package cn.xanderye.android.jdck.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.webkit.WebView;
import android.widget.Toast;
import cn.xanderye.android.jdck.config.Config;
import cn.xanderye.android.jdck.util.SmsCodeUtil;

/**
 * 短信监听器
 */
public class SMSReceiver extends BroadcastReceiver {

    public static final String SMS_RECEIVED = Telephony.Sms.Intents.SMS_RECEIVED_ACTION;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !SMS_RECEIVED.equals(intent.getAction())) {
            return;
        }
        SmsMessage[] messages = Telephony.Sms.Intents.getMessagesFromIntent(intent);
        if (messages == null) {
            return;
        }
        StringBuilder body = new StringBuilder();
        for (SmsMessage sms : messages) {
            String part = sms.getMessageBody();
            if (part != null) {
                body.append(part);
            }
        }
        String code = SmsCodeUtil.extractJdCode(body.toString());
        if (code != null) {
            fillCode(context, code);
        }
    }

    private void fillCode(Context context, String code) {
        WebView webView = Config.getInstance().getWebView();
        if (webView == null || webView.getWindowToken() == null) {
            return;
        }
        webView.post(() -> {
            String execJs = "(function(){"
                    + "var host=location.host;"
                    + "if(host!=='jd.com'&&!host.endsWith('.jd.com'))return false;"
                    + "var input=document.getElementById('authcode');"
                    + "if(!input)return false;"
                    + "input.value='" + code + "';"
                    + "var evt=new InputEvent('input',{inputType:'insertText',data:'" + code + "',dataTransfer:null,isComposing:false});"
                    + "input.dispatchEvent(evt);"
                    + "var btn=document.getElementsByClassName('btn J_ping')[0];"
                    + "if(btn){setTimeout(function(){btn.click();},200);}"
                    + "return true;})();";
            webView.evaluateJavascript(execJs, result -> {
                if ("true".equals(result)) {
                    Toast.makeText(context, "已填入验证码", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}
