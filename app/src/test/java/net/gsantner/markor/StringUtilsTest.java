package net.gsantner.markor;

import static org.assertj.core.api.Assertions.assertThat;

import net.gsantner.opoc.util.StringUtils;

import org.junit.Test;

public class StringUtilsTest {

    @Test
    public void findDiffTest() {
        assertThat(StringUtils.findDiff("", "")).isEqualTo(new int[] {0, 0, 0});
        assertThat(StringUtils.findDiff("abcd", "abcd")).isEqualTo(new int[] {4, 4, 4});
        assertThat(StringUtils.findDiff("ab", "abcd")).isEqualTo(new int[] {2, 2, 4});
        assertThat(StringUtils.findDiff("abcd", "ab")).isEqualTo(new int[] {2, 4, 2});
        assertThat(StringUtils.findDiff("ab1d", "ab2d")).isEqualTo(new int[] {2, 3, 3});
        assertThat(StringUtils.findDiff("ab12d", "ab34d")).isEqualTo(new int[] {2, 4, 4});
        assertThat(StringUtils.findDiff("ab12d", "ab3d")).isEqualTo(new int[] {2, 4, 3});
        assertThat(StringUtils.findDiff("ab12d", "abd")).isEqualTo(new int[] {2, 4, 2});
        assertThat(StringUtils.findDiff("abd", "ab12d")).isEqualTo(new int[] {2, 2, 4});
        assertThat(StringUtils.findDiff("abcd", "")).isEqualTo(new int[] {0, 4, 0});
        assertThat(StringUtils.findDiff("", "abcd")).isEqualTo(new int[] {0, 0, 4});
    }
}

