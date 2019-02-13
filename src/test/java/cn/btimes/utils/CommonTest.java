package cn.btimes.utils;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class CommonTest {
    @Test
    public void getDomain() {
        String url = "https://www.cnblogs.com/conkis/p/3156749.html";
        assertEquals(Common.getDomain(url), "www.cnblogs.com");
    }

}