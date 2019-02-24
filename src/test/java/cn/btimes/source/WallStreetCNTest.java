package cn.btimes.source;

import cn.btimes.model.common.Article;
import com.amzass.utils.common.Exceptions.BusinessException;
import com.google.inject.Inject;
import org.testng.annotations.Test;

public class WallStreetCNTest extends BasePageTest {

    @Inject private WallStreetCN source;

    @Test(expectedExceptions = BusinessException.class)
    public void readArticle() {
        Article article = new Article();
        article.setUrl("https://weexcn.com/articles/3473184");
        article.setTitle("一期一会：开年爆雷，炒汇？藏金？持股？深度解读2019上半年如何配置大类资产");
        source.readArticle(driver, article);
    }

}