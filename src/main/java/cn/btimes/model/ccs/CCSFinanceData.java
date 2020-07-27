package cn.btimes.model.ccs;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * 中概股：财务数据
 * 包括：主要指标、分红派息、利润表、资产负债表、现金流量表
 *
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2020/7/24 10:27
 */
@Data
public class CCSFinanceData {
    private String intro;
    private String currency;
    private List<String> all;
    private List<String> yearly;
    private List<String> middle;
    private List<String> oneQuarter;
    private List<String> threeQuarter;

    public void assignPageFields(String scope, List<String> pageContents) {
        if (StringUtils.equals(scope, "全部")) {
            this.all = pageContents;
        }
        if (StringUtils.equals(scope, "年报")) {
            this.yearly = pageContents;
        }
        if (StringUtils.equals(scope, "中报")) {
            this.middle = pageContents;
        }
        if (StringUtils.equals(scope, "一季报")) {
            this.oneQuarter = pageContents;
        }
        if (StringUtils.equals(scope, "三季报")) {
            this.threeQuarter = pageContents;
        }
    }

    public enum FinanceData {
        MajorIndex("主要指标"),
        Dividends("分红派息"),
        ProfitStatement("利润表"),
        BalanceSheet("资产负债表"),
        CashFlowStatement("现金流量表");

        public static FinanceData parse(String label) {
            for (FinanceData data : FinanceData.values()) {
                if (StringUtils.equals(data.label, label)) {
                    return data;
                }
            }
            return null;
        }

        private final String label;

        FinanceData(String label) {
            this.label = label;
        }
    }
}
