package cn.btimes.model.common;

import cn.btimes.utils.Common;
import org.apache.commons.lang3.StringUtils;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019-02-12 10:20 PM
 */
public enum ArticleSource {
    CCDY("ccdy.cn", "中国文化传媒网"),
    CNBeta("cnbeta.com", "cnBeta.COM"),
    CNR("cnr.cn", "央广网"),
    COM163("163.com", "网易"),
    CSCOMCN("cs.com.cn", "中证网"),
    CTO51("51cto.com", "51CTO"),
    EEO("eeo.com.cn", "经济观察网"),
    EntGroup("entgroup.cn", "艺恩网"),
    GasGoo("gasgoo.com", "盖世汽车"),
    HeXun("hexun.com", "和讯网"),
    IFeng("ifeng.com", "凤凰网"),
    IYiOu("iyiou.com", "亿欧"),
    JieMian("jiemian.com", "界面"),
    LadyMax("ladymax.cn", "时尚头条网"),
    LUXE("luxe.co", "华丽志"),
    LvJie("lvjie.com.cn", "新旅界"),
    NBD("nbd.com.cn", "每经网"),
    People("people.com.cn", "人民网"),
    PinChain("pinchain.com", "品橙旅游"),
    QQ("qq.com", "腾讯新闻"),
    Sina("sina.com.cn", "新浪新闻"),
    SinaFinance("finance.sina.com.cn", "新浪财经"),
    ThePaper("thepaper.cn", "澎湃"),
    WallStreetCN("wallstreetcn.com", "华尔街见闻"),
    YiCai("yicai.com", "第一财经"),
    ZJOLbiz("biz.zjol.com.cn", "浙商网"),
    ZJOLec("ec.zjol.com.cn", "浙江在线-电商生活"),
    KR36("36kr.com", "36氪"),
    HuanQiuTech("tech.huanqiu.com", "环球科技"),
    HuanQiuTour("go.huanqiu.comm", "环球旅游"),
    STDaily("stdaily.com", "国际科技频道"),
    HanNvTuan("hannvtuan.com", "韩国女子组合"),
    Idol001("idol001.com", "爱豆"),
    ReBo("y3600.co", "热播网"),
    Sohu("sohu.com", "搜狐"),
    TechNode("technode.com", "动点科技"),
    STCN("stcn.com", "证券时报网"),
    ChangJiangTimes("changjiangtimes.com", "长江商报"),
    BJNews("bjnews.com.cn", "新京报"),
    TechWeb("techweb.com.cn", "TechWeb"),
    CaiFuHao("caifuhao.eastmoney.com", "财富号"),
    EastMoney("finance.eastmoney.com", "东方财富网"),
    Undefined("", "未知来源");

    ArticleSource(String domain, String title) {
        this.domain = domain;
        this.title = title;
    }

    public String getSourceKey() {
        return (StringUtils.isBlank(title) ? "" : (title + "<br />")) + domain;
    }

    public static ArticleSource getByLink(String link) {
        String domain = Common.getDomain(link);
        for (ArticleSource source : ArticleSource.values()) {
            if (StringUtils.containsIgnoreCase(domain, source.domain)) {
                return source;
            }
        }
        return ArticleSource.Undefined;
    }

    public final String domain;
    public final String title;
}
