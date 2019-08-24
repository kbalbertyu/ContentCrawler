package com.fortis.model;

import com.google.common.base.Objects;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019/8/24 12:42
 */
@Data
@AllArgsConstructor
public class Link {
    private String url;
    private String category;
    private String subCategory;

    public Link(String url) {
        this.url = url;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Link link = (Link) o;
        return Objects.equal(url, link.url) &&
            Objects.equal(category, link.category) &&
            Objects.equal(subCategory, link.subCategory);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(url, category, subCategory);
    }
}
