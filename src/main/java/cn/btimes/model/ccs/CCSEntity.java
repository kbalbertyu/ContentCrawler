package cn.btimes.model.ccs;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;

/**
 * Chinese Concept Stock
 *
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2020/7/11 10:42
 */
@Data
public class CCSEntity {
    private String label;
    private String title;
    private String stockClass;
    private String stockCode;
    private String link;
    private String section;
    private float unitPrice;
    private float unitPriceRising;
    private float unitPriceRisingPercentage;
    private float yesterdayClosingPrice;
    private float week52TopUnitPrice;
    private float week52BottomUnitPrice;
    private float totalMarketValue;
    private String totalMarketValueText;
    private long totalStocks;
    private float priceEarningRatio;
    private String intro;
    private String nameCN;
    private String nameEN;
    private String industry;
    private String address;
    private String phone;
    private String website;
    private String stockMarket;

    public boolean valid() {
        return StringUtils.isNotBlank(stockClass) &&
            StringUtils.isNotBlank(stockCode);
    }
}
