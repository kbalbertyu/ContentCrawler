package cn.btimes.model.ccs;

import cn.btimes.model.ccs.CCSFinanceData.FinanceData;
import lombok.Data;

import java.util.HashMap;

/**
 * 中概股：公司简介及外链
 *
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2020/7/24 10:01
 */
@Data
public class CCSInfo {
    private String stockCode;
    private String introShort;
    private String website;
    private String address;
    private String phone;
    /**
     * 公司简介
     **/
    private String intro;
    /**
     * 公司高管
     **/
    private String executives;
    /**
     * 内部持股
     **/
    private String internalShareholdingUrl;
    /**
     * 所属指数
     **/
    private String affiliationIndexUrl;
    /**
     * 盘前交易
     **/
    private String preMarketTradeUrl;
    /**
     * 盘后交易
     **/
    private String afterHoursTradingUrl;
    /**
     * 历史价格
     **/
    private String historicalPriceUrl;
    /**
     * 期权交易
     **/
    private String optionTradingUrl;
    /**
     * 空仓数据
     **/
    private String shortPositionDataUrl;
    /**
     * 内部交易
     **/
    private String internalTransactionUrl;
    /**
     * 市场预期
     **/
    private String marketExpectationsUrl;
    /**
     * SEC文件
     **/
    private String secFileUrl;
    /**
     * 财报公告(中)
     **/
    private String financeReportCNUrl;
    /**
     * 财报公告(英)
     **/
    private String financeReportENUrl;
    /**
     * 电话会议实录
     **/
    private String phoneConferenceRecordUrl;
    /**
     * 收益预估
     **/
    private String revenueForecastUrl;
    /**
     * 评级变化
     **/
    private String ratingChangesUrl;
    /**
     * 研究报告
     **/
    private String researchReportUrl;

    private HashMap<FinanceData, CCSFinanceData> financeData;
}
