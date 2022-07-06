package net.gsantner.markor;

import static org.assertj.core.api.Assertions.assertThat;

import net.gsantner.opoc.util.StringUtils;

import org.junit.Test;

public class StringUtilsTest {

    @Test
    public void findDiffTest() {
        assertThat(StringUtils.findDiff("", "", 0, 0)).isEqualTo(new int[] {0, 0, 0});
        assertThat(StringUtils.findDiff("abcd", "abcd", 0, 0)).isEqualTo(new int[] {4, 4, 4});
        assertThat(StringUtils.findDiff("ab", "abcd", 0, 0)).isEqualTo(new int[] {2, 2, 4});
        assertThat(StringUtils.findDiff("abcd", "ab", 0, 0)).isEqualTo(new int[] {2, 4, 2});
        assertThat(StringUtils.findDiff("ab1d", "ab2d", 0, 0)).isEqualTo(new int[] {2, 3, 3});
        assertThat(StringUtils.findDiff("ab12d", "ab34d", 0, 0)).isEqualTo(new int[] {2, 4, 4});
        assertThat(StringUtils.findDiff("ab12d", "ab3d", 0, 0)).isEqualTo(new int[] {2, 4, 3});
        assertThat(StringUtils.findDiff("ab12d", "abd", 0, 0)).isEqualTo(new int[] {2, 4, 2});
        assertThat(StringUtils.findDiff("abd", "ab12d", 0, 0)).isEqualTo(new int[] {2, 2, 4});
        assertThat(StringUtils.findDiff("abcd", "", 0, 0)).isEqualTo(new int[] {0, 4, 0});
        assertThat(StringUtils.findDiff("", "abcd", 0, 0)).isEqualTo(new int[] {0, 0, 4});
        assertThat(StringUtils.findDiff("ab11d", "ab1d", 0, 0)).isEqualTo(new int[] {3, 4, 3});
        assertThat(StringUtils.findDiff("aaaaa", "aaa", 0, 0)).isEqualTo(new int[] {3, 5, 3});
        assertThat(StringUtils.findDiff("aaa", "aaaaa", 0, 0)).isEqualTo(new int[] {3, 3, 5});
    }
}

