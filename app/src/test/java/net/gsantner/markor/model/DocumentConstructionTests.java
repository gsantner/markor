/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Harshad Vedartham
 * SPDX-License-Identifier: Apache-2.0
 *
#########################################################*/
package net.gsantner.markor.model;

import static org.assertj.core.api.Assertions.assertThat;

import net.gsantner.markor.format.FormatRegistry;

import org.junit.Test;

import java.io.File;

public class DocumentConstructionTests {

    @Test
    public void knownFormatConstructorSetsFormatForAmbiguousFilename() {
        final Document document = new Document(new File("archive.txt"), FormatRegistry.FORMAT_TODOTXT);

        assertThat(document.getFormat()).isEqualTo(FormatRegistry.FORMAT_TODOTXT);
    }
}
