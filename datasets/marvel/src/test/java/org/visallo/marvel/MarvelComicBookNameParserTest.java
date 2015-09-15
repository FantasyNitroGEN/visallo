package org.visallo.marvel;

import com.google.common.collect.Maps;
import org.junit.Test;

import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class MarvelComicBookNameParserTest {
    private final Map<String, String> map = Maps.newHashMap();

    private final String[] metadata = new String[] { "4EX\t4 (EARTH X)\t2000\n" ,
            "1602\tMARVEL 1602\t2003\n" ,
            "1602:FF\tMARVEL 1602: FANTASTICK FOUR\t2006\n" ,
            "1602:NW\tMARVEL 1602: NEW WORLD\t2006\n" ,
            "1602:SM\tSPIDER-MAN 1602\t2010\n" ,
            "1985\t1985\t2008\n" ,
            "2001\t2001: A SPACE ODYSSEY\t1976-1977\n" ,
            "2099:MD\t2099: MANIFEST DESTINY\t1998\n" ,
            "2099:WOT\t2099: WORLD OF TOMORROW\t1996-1997\n" ,
            "2099AD\t2099 A.D.\t1995\n" ,
            "2099AD:A\t2099 A.D. APOCALYPSE\t1995\n" ,
            "2099AD:G\t2099 A.D. GENESIS\t1996\n" ,
            "2099U\t2099 UNLIMITED\t1993-1995\n" ,
            "A\tAVENGERS\t1963-1996, 2004\n" ,
            "A2\tAVENGERS VOL. 2\t1996-1997\n" ,
            "A3\tAVENGERS VOL. 3\t1998-2004\n" ,
            "A4\tAVENGERS VOL. 4\t2010-2012\n" ,
            "A5\tAVENGERS VOL. 5\t2013-"};
    
    @Test
    public void test(){
        MarvelComicBookNameParser testSubject = new MarvelComicBookNameParser();
        for(String metadataLine : this.metadata) {
            testSubject.addMetadata(metadataLine);
        }

        String encodedName = "A 77";
        String name = testSubject.getName(encodedName);
        assertThat(name, is("Avengers 77"));

        String encodedName1 = "A@ 77";
        String name1 = testSubject.getName(encodedName1);
        assertThat(name1, is("Avengers 77"));
    }



}
