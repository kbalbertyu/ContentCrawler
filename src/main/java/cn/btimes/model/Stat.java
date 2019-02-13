package cn.btimes.model;

import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.TreeMap;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019-02-12 3:15 PM
 */
@Data
public class Stat implements Comparable<Stat> {
    private Count count = new Count();
    private Map<Category, Count> categoryStats = new TreeMap<>();

    public void increase(boolean published, Category category) {
        count.increase(published);

        Count categoryCount = categoryStats.getOrDefault(category, new Count());
        categoryCount.increase(published);
        categoryStats.put(category, categoryCount);
    }

    @Override
    public int compareTo(@NotNull Stat stat) {
        int sub = count.getTotal() - stat.getCount().getTotal();
        if (sub > 0) {
            return 1;
        } else if (sub < 0) {
            return -1;
        }
        return 0;
    }
}
