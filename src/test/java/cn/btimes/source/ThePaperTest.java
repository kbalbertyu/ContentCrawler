package cn.btimes.source;

import cn.btimes.model.common.Article;
import cn.btimes.model.common.ImageUploadResult;
import cn.btimes.service.WebDriverLauncher;
import com.google.inject.Inject;
import com.kber.test.BaseTest;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class ThePaperTest extends BaseTest{

    @Inject private WebDriverLauncher webDriverLauncher;
    @Inject private ThePaper source;

    @Test
    public void testUploadImages() {
        WebDriver driver = webDriverLauncher.start();
        Article article = new Article();
        List<String> images = new ArrayList<>();
        images.add("http://image.thepaper.cn/www/image/13/665/231.jpg");
        article.setContentImages(images);
        ImageUploadResult result = source.uploadImages(article, driver);
        assertNotNull(result);
        assertTrue(result.hasFiles());
    }
}