/*#######################################################
 *
 *   Maintained 2023 by k3b
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.csv;

import android.content.Context;

import net.gsantner.markor.format.markdown.MarkdownTextConverter;

import java.io.File;

import other.de.stanetz.jpencconverter.JavaPasswordbasedCryption;

/**
 * Converts csv to md and let
 * {@link  MarkdownTextConverter#convertMarkup(String, Context, boolean, File)}
 * do the rest.
 *
 * This way csv columns may contain md expressions like bold text.
 */
@SuppressWarnings("WeakerAccess")
public class CsvTextConverter extends MarkdownTextConverter {
    @Override
    public String convertMarkup(String csvMarkup, Context context, boolean isExportInLightMode, File file) {
        String mdMarkup = Csv2MdTable.toMdTable(csvMarkup);
        return super.convertMarkup(mdMarkup, context, isExportInLightMode, file);
    }

    @Override
    protected boolean isFileOutOfThisFormat(String filepath, String extWithDot) {
        filepath = filepath.replace(JavaPasswordbasedCryption.DEFAULT_ENCRYPTION_EXTENSION, "");
        return filepath.toLowerCase().endsWith(".csv");
    }


}
