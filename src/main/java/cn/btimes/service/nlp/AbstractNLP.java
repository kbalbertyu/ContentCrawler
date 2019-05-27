package cn.btimes.service.nlp;

import cn.btimes.model.nlp.Tag;

import java.util.List;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019-05-23 9:31 AM
 */
public abstract class AbstractNLP {

    public abstract List<Tag> generateTags(String title, String content);
}
