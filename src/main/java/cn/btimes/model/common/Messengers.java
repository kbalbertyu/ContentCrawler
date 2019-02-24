package cn.btimes.model.common;

import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019-01-21 11:21 PM
 */
public class Messengers {
    private static List<Messenger> messengerList = new ArrayList<>();

    public void add(Messenger messenger) {
        messengerList.add(messenger);
    }

    public boolean isNotEmpty() {
        return CollectionUtils.isNotEmpty(messengerList);
    }

    public List<Messenger> getList() {
        return messengerList;
    }

    public void clear() {
        messengerList.clear();
    }
}
