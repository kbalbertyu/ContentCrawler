package cn.btimes.source;

import com.kber.test.BaseTest;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class SourceTest extends BaseTest {

    @Test
    public void extractFromHoursBefore() {
        assertEquals(4, Source.extractFromHoursBefore("4小时前"));
        assertEquals(14, Source.extractFromHoursBefore("14小时前"));
    }

}