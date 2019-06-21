package cn.btimes.service;

import cn.btimes.utils.Common;
import com.kber.test.BaseTest;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class YiQiZengCrawlerTest extends BaseTest {

    @Test
    public void formatPrice() {
        assertTrue(Common.numericEquals(YiQiZengCrawler.formatPrice("标准售价：￥21.00"), 21.00f));
        assertTrue(Common.numericEquals(YiQiZengCrawler.formatPrice("￥18.00"), 18.00f));
        assertTrue(Common.numericEquals(YiQiZengCrawler.formatPrice("市价：152.00"), 152.00f));
    }
}