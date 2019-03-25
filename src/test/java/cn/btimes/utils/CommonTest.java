package cn.btimes.utils;

import cn.btimes.model.common.ImageType;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.testng.Assert.*;

public class CommonTest {
    @Test
    public void getDomain() {
        String url = "https://www.cnblogs.com/conkis/p/3156749.html";
        assertEquals(Common.getDomain(url), "www.cnblogs.com");
    }

    @Test
    public void determineImageFileType() throws IOException {
        String file = "C:\\OrderManRnD\\Workspace\\ContentCrawler\\downloads\\1000";
        ImageType type = Common.determineImageFileType(file);
        assertEquals(ImageType.WEBP, type);
    }

    @Test
    public void convertImageFileType() throws IOException {
        String url = "http://inews.gtimg.com/newsapp_bt/0/8273474642/1000";
        String path = "C:\\OrderManRnD\\Workspace\\ContentCrawler\\downloads\\1000.bmp";
        Common.convertImageFileType(url, path, ImageType.DEFAULT_TYPE);
        assertEquals(ImageType.DEFAULT_TYPE, Common.determineImageFileType(path));
    }
}