package cn.btimes.model;

import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019-01-21 11:21 PM
 */
public class Messengers {
    private List<Messenger> messengerList = new ArrayList<>();

    public void add(Messenger messenger) {
        this.messengerList.add(messenger);
    }

    public boolean isNotEmpty() {
        return CollectionUtils.isNotEmpty(this.messengerList);
    }

    public List<Messenger> getList() {
        return this.messengerList;
    }
}
