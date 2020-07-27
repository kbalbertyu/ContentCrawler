package cn.btimes.model.ccs;

import com.google.common.base.Objects;
import lombok.Data;

import java.util.List;

/**
 * 中概股：雪球网站的中概股数据
 * 来源网站：<a href="https://xueqiu.com/hq#exchange=US&industry=3_3&firstName=3">雪球</a>
 *
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2020/7/24 0:39
 */
@Data
public class CCSData {
    private String stockCode;
    private String stockName;
    /**
     * 报价系统：NASDAQ
     **/
    private String quotation;
    private float currentPrice;
    /**
     * 涨跌额
     **/
    private float upDown;
    /**
     * 涨跌幅
     **/
    private float upDownPercent;
    /**
     * 年初至今
     **/
    private float year2NowPercent;
    /**
     * 市盈率(TTM)
     **/
    private float peRatio;
    /**
     * 股息率
     **/
    private float dividendYieldPercent;
    private float unitPrice;
    private float unitPriceRising;
    private float unitPriceRisingPercent;

    /**
     * Stock Issues
     **/
    private List<String> stockIssues;

    /**
     * 收盘/开盘时间
     **/
    private String stockStatus;
    private String stockTime;

    /**
     * 盘前/盘后交易
     **/
    private String beforeAfter;
    private float beforeAfterPrice;
    private float beforeAfterDiff;
    private float beforeAfterPercent;

    /**
     * 最高
     **/
    private float topUnitPrice;
    /**
     * 最低
     **/
    private float bottomUnitPrice;
    /**
     * 今开
     **/
    private float todayOpenUnitPrice;
    /**
     * 昨收
     **/
    private float yesterdayCloseUnitPrice;
    /**
     * 成交量：股、万股
     **/
    private float dealVolume;
    /**
     * 成交量单位：万元、亿元
     **/
    private String dealVolumeUnit;
    /**
     * 成交额
     **/
    private float turnover;
    /**
     * 成交额单位：万元、亿元
     **/
    private String turnoverUnit;
    /**
     * 换手率
     **/
    private float turnoverRatioPercent;
    /**
     * 振幅
     **/
    private float amplitudePercent;
    /**
     * 52周最高
     **/
    private float week52TopUnitPrice;
    /**
     * 52周最低
     **/
    private float week52BottomUnitPrice;
    /**
     * 量比
     **/
    private float quantityRatio;
    /**
     * 委比
     **/
    private float weibiPercent;
    /**
     * 市盈率(TTM): 亏损
     **/
    private String peRatioText;
    /**
     * 市盈率(静)
     **/
    private String peRatioStaticText;
    /**
     * 市净率
     **/
    private float pbRatio;
    /**
     * 市销率
     **/
    private float marketSalesRate;
    /**
     * 每股收益
     **/
    private float earningsPerShare;
    /**
     * 每股净资产
     **/
    private float netAssetsPerShare;
    /**
     * 股息(TTM)
     **/
    private String dividends;
    /**
     * 股息率(TTM)
     **/
    private String dividendYield;
    /**
     * 每手股数
     **/
    private int sharesPerLot;
    /**
     * 最小价差
     **/
    private float minimumSpread;
    /**
     * 市值
     **/
    private float marketValue;
    /**
     * 市值单位：万元、亿元
     **/
    private String marketValueUnit;
    /**
     * 总股本
     **/
    private float totalEquity;
    /**
     * 总股本单位：万、亿
     **/
    private String totalEquityUnit;
    /**
     * 机构持股
     **/
    private String institutionalHoldings;
    /**
     * Beta
     **/
    private float beta;
    /**
     * 空头回补天数
     **/
    private float shortCoveringDays;
    /**
     * 货币单位
     **/
    private String currency;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CCSData ccsData = (CCSData) o;
        return Objects.equal(stockCode, ccsData.stockCode);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(stockCode);
    }
}
