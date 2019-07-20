package cn.btimes.model.baidu;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

@Data
public class BaiduAMPResult {
    private int remain;
    private int success;

    @JSONField(name = "remain_amp")
    private int reaminAmp;

    @JSONField(name = "success_amp")
    private int successAmp;

    @JSONField(name = "not_same_site")
    private String[] notSameSite;

    @JSONField(name = "not_valid")
    private String[] notValid;

    private int error;

    private String message;
}
