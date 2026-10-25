package cn.xanderye.android.jdck.util;

import com.alibaba.fastjson.JSON;

import java.util.HashMap;
import java.util.Map;

/**
 * @author XanderYe
 * @description:
 * @date 2022/5/10 11:20
 */
public class JDUtil {

    public static Map<String, Object> formatCookies(String cookieString) {
        Map<String, Object> cookieMap = new HashMap<>(16);
        if (cookieString != null && !"".equals(cookieString)) {
            String[] cookies = cookieString.split(";");
            if (cookies.length > 0) {
                for (String parameter : cookies) {
                    int eqIndex = parameter.indexOf("=");
                    if (eqIndex > -1) {
                        String k = parameter.substring(0, eqIndex).trim();
                        String v = parameter.substring(eqIndex + 1).trim();
                        if (!"".equals(v)) {
                            cookieMap.put(k, v);
                        }
                    }
                }
            }
        }
        return cookieMap;
    }

    public static String buildLoginScript(String account, String password, boolean phoneAccount) {
        StringBuilder js = new StringBuilder(512);
        js.append("var account=").append(JSON.toJSONString(account)).append(";");
        js.append("var policy=document.getElementsByClassName('policy_tip-checkbox')[0];if(policy){policy.click();}");
        js.append("var evt=new InputEvent('input',{inputType:'insertText',data:account,dataTransfer:null,isComposing:false});");
        js.append("var username=document.getElementById('username');if(username){username.value=account;username.dispatchEvent(evt);}");
        if (phoneAccount) {
            js.append("var phoneInput=document.getElementsByClassName('acc-input mobile J_ping')[0];");
            js.append("if(phoneInput){phoneInput.value=account;phoneInput.dispatchEvent(evt);}");
        }
        if (password != null && !password.isEmpty()) {
            js.append("var password=").append(JSON.toJSONString(password)).append(";");
            js.append("var pwdEvt=new InputEvent('input',{inputType:'insertText',data:password,dataTransfer:null,isComposing:false});");
            js.append("var pwd=document.getElementById('pwd');if(pwd){pwd.value=password;pwd.dispatchEvent(pwdEvt);}");
            js.append("var loginLink=document.querySelector('#app>div>a');if(loginLink){loginLink.click();}");
        }
        return js.toString();
    }
}
