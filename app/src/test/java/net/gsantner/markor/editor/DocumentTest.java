/*
 * ------------------------------------------------------------------------------
 * Gregor Santner <gsantner.net> wrote this. You can do whatever you want
 * with it. If we meet some day, and you think it is worth it, you can buy me a
 * coke in return. Provided as is without any kind of warranty. Do not blame or
 * sue me if something goes wrong. No attribution required.    - Gregor Santner
 *
 * License: Creative Commons Zero (CC0 1.0)
 *  http://creativecommons.org/publicdomain/zero/1.0/
 * ----------------------------------------------------------------------------
 */
package net.gsantner.markor.editor;

import net.gsantner.markor.model.Document;

import org.junit.Test;

import java.util.regex.Matcher;

import static org.assertj.core.api.Assertions.assertThat;

public class DocumentTest {

    @Test
    public void documentOlderVersion() {
        Document document = new Document();
        document.setTitle("Hello");
        document.forceAddNextChangeToHistory();
        document.setTitle("Hello World");
        document.forceAddNextChangeToHistory();
        document.goToEarlierVersion();
        assertThat(document.getTitle()).isEqualTo("Hello");
    }

    @Test
    public void documentNewerVersion() {
        Document document = new Document();
        document.setTitle("Hello");
        document.forceAddNextChangeToHistory();
        document.setTitle("Hello World");
        document.forceAddNextChangeToHistory();
        document.setTitle("Hello World Again");
        document.goToEarlierVersion();
        document.goToEarlierVersion();
        assertThat(document.getTitle()).isEqualTo("Hello");
        document.goToNewerVersion();
        assertThat(document.getTitle()).isEqualTo("Hello World");
        assertThat(document.canGoToNewerVersion()).isEqualTo(true);
        document.goToNewerVersion();
        assertThat(document.getTitle()).isEqualTo("Hello World Again");
        assertThat(document.canGoToNewerVersion()).isEqualTo(false);
    }
}
