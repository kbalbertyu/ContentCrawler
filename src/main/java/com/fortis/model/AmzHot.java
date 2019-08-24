package com.fortis.model;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019/8/8 9:11
 */
public enum AmzHot {
    BestSellers("/Best-Sellers/zgbs/ref=zg_mw_tab"),
    NewReleases("/gp/new-releases/ref=zg_mw_tab"),
    MoverShakers("/gp/movers-and-shakers/ref=zg_mw_tab");

    public final String path;

    AmzHot(String path) {
        this.path = path;
    }
}
