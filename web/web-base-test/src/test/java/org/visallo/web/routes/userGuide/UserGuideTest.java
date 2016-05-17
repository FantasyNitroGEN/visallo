package org.visallo.web.routes.userGuide;

import com.google.common.collect.Lists;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertArrayEquals;

public class UserGuideTest {
    @Test
    public void testMergeSummaryFile() {
        ArrayList<String> lines = Lists.newArrayList(
                "* [Navigation](navigation.md)",
                "    * [nav1](nav1.md)",
                "* Plugins",
                "* [Glossary](glossary.md)"
        );
        UserGuide.mergeSummaryFile(lines,
                Lists.newArrayList(
                        "* Navigation",
                        "    * [nav2](nav2.md)",
                        "* Plugins",
                        "    * [plugin1](plugin1.md)"
                )
        );
        assertArrayEquals(new String[]{
                "* [Navigation](navigation.md)",
                "    * [nav2](nav2.md)",
                "    * [nav1](nav1.md)",
                "* Plugins",
                "    * [plugin1](plugin1.md)",
                "* [Glossary](glossary.md)"
        }, lines.toArray(new String[lines.size()]));
    }
}