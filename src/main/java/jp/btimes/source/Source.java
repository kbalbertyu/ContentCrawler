package jp.btimes.source;

import cn.btimes.model.common.Article;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2020/4/19 20:15
 */
public abstract class Source extends cn.btimes.source.Source {

    void resizeContentImage(Article article, String[] from, String[] to) {
        List<String> newImages = new ArrayList<>();

        String content = article.getContent();
        for (String image : article.getContentImages()) {
            String newSrc = image;
            for (int i = 0; i < from.length; i++) {
                newSrc = StringUtils.replacePattern(newSrc, from[i], to[i]);
            }
            newImages.add(newSrc);
            content = StringUtils.replace(content, image, newSrc);
        }
        article.setContent(content);
        article.setContentImages(newImages);
    }
}
