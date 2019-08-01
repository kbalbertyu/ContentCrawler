package cn.btimes.model.baidu;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

@Data
public class BaiduResult {
    private int remain;
    private int success;

    @JSONField(name = "remain_amp")
    private int reaminAmp;

    @JSONField(name = "success_amp")
    private int successAmp;

    @JSONField(name = "remain_mip")
    private int reaminMip;

    @JSONField(name = "success_mip")
    private int successMip;

    @JSONField(name = "not_same_site")
    private String[] notSameSite;

    @JSONField(name = "not_valid")
    private String[] invalid;

    private int error;

    private String message;

    public boolean canUpload(BaiduLink baiduLink) {
        if (baiduLink == BaiduLink.AMP) {
            return reaminAmp > 0;
        }
        if (baiduLink == BaiduLink.MIP) {
            return reaminMip > 0;
        }
        return remain > 0;
    }
}
