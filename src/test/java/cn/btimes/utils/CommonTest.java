package cn.btimes.utils;

import cn.btimes.model.common.ImageType;
import org.apache.commons.io.FileUtils;
import org.testng.annotations.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

import static org.testng.Assert.assertEquals;

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
        String path = "C:\\OrderManRnD\\Workspace\\ContentCrawler\\downloads\\1000.bmp";
        Common.convertImageFileType(path, ImageType.DEFAULT_TYPE);
        assertEquals(ImageType.DEFAULT_TYPE, Common.determineImageFileType(path));
    }

    @Test
    void downloadImageWithImageIO() throws IOException {
        String src = "https://oss.znfinnews.com/c758c8dc95c0db973beb7138993f3d1d";
        BufferedImage im = ImageIO.read(new URL(src));
        String ext = ImageType.DEFAULT_TYPE.toExt();
        ImageIO.write(im, ext, FileUtils.getFile("downloads", "test." + ext));
    }

    @Test
    public void extractFileNameFromUrl() {
        String url = "https://inews.gtimg.com/newsapp_bt/0/12226190320/641";
        String fileName = Common.extractFileNameFromUrl(url);
        assertEquals(fileName, "641");
    }

    @Test
    public void getAbsoluteUrl() {
        String pageUrl = "https://xueqiu.com/hq#exchange=US&industry=3_3&firstName=3";
        String url = "/S/HMI";
        String absUrl = Common.getAbsoluteUrl(url, pageUrl);
        assertEquals(absUrl, "https://xueqiu.com/S/HMI");
    }

    @Test
    public void getFileExtension() {
        String url = "https://pics2.baidu.com/feed/cc11728b4710b912f89a8ce78a18fc04934522bf.jpeg?token=4278c55f2f2726e4106b8fa42b8ac33b";
        String ext = Common.getFileExtension(url);
        assertEquals(ext, "jpeg");
    }
}