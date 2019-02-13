package cn.btimes.model;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class ArticleDataTest {

    @Test public void getCategoryId() {
        String options = "{\"ar_hidecomment\":0,\"ar_hidebanner\":0,\"ar_hidesocial\":0,\"artitle_bold\":0,\"artitle_italic\":0,\"artitle_underline\":0,\"ar_hidevideo\":0,\"ar_inlineslideshow\":0,\"ar_inlineslideshow_mobile\":0,\"ar_cat\":[\"24\"]}";
        ArticleData articleData = new ArticleData();
        articleData.setOptions(options);
        assertEquals(articleData.category().id, 24);

        options = "{\"ar_hidecomment\":0,\"ar_hidebanner\":0,\"ar_hidesocial\":0,\"artitle_bold\":0,\"artitle_italic\":0,\"artitle_underline\":0,\"ar_hidevideo\":0,\"ar_inlineslideshow\":0,\"ar_inlineslideshow_mobile\":0}";
        articleData.setOptions(options);
        assertEquals(articleData.category().id, 0);
    }
}