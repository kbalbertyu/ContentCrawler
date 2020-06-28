package cn.btimes.service;

import com.amzass.service.common.ApplicationContext;
import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2020/5/31 0:02
 */
public class PDFExtractor {
    private static final Logger LOGGER = LoggerFactory.getLogger(PDFExtractor.class);

    public static void main(String[] args) {
        File pdfFile = FileUtils.getFile(args[0]);
        ApplicationContext.getBean(PDFExtractor.class).execute(pdfFile);
    }

    private void execute(File pdfFile) {
        PDDocument doc;
        try {
            doc = PDDocument.load(pdfFile);

            String imageFileDir = pdfFile.getParent();
            int pageCount = doc.getNumberOfPages();
            PDFRenderer pdfRenderer = new PDFRenderer(doc);
            for (int pageIndex=0; pageIndex<pageCount; pageIndex++) {
                BufferedImage image = pdfRenderer.renderImageWithDPI(pageIndex, 600, ImageType.RGB);
                File imageFilePath = FileUtils.getFile(imageFileDir, (pageIndex + 1) + ".jpg");
                ImageIO.write(image, "jpg", imageFilePath);
            }

            try {
                doc.close();
            } catch (IOException e) {
                LOGGER.error("Unable to close the PDF file", e);
            }
        } catch (IOException e) {
            LOGGER.error("Unable to open the PDF file", e);
        }
        System.exit(0);
    }
}
