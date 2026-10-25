package cn.xanderye.android.jdck.entity;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

/**
 * @author XanderYe
 * @description:
 * @date 2022/5/11 14:15
 */
@Data
public class QlEnv {

    @JSONField(name = "_id")
    private String qlId;

    @JSONField(name = "id")
    private String daidaiId;

    private String name;

    private String remarks;

    private Integer status;

    private String value;
}
