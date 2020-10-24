package net.gsantner.markor.format.zimwiki;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ZimWikiFileContentsCreator {
    public static String createZimWikiHeaderAndTitleContents(String fileNameWithoutExtension, Date creationDate, String creationDateLinePrefix) {
        String headerContentTypeLine = "Content-Type: text/x-zim-wiki";
        String headerWikiFormatLine = "Wiki-Format: zim 0.6";
        SimpleDateFormat headerDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.ROOT);
        String creationDateFormatted = headerDateFormat.format(creationDate);
        String headerCreationDateLine = "Creation-Date: "+creationDateFormatted;
        String title = fileNameWithoutExtension.trim().replaceAll("_", " ");
        String titleLine = "====== " + title + " ======";
        SimpleDateFormat creationDateLineFormat = new SimpleDateFormat("'"+creationDateLinePrefix+"'"+" EEEE dd MMMM yyyy", Locale.getDefault());
        String creationDateLine = creationDateLineFormat.format(creationDate);

        String contents = headerContentTypeLine + "\n"
                + headerWikiFormatLine + "\n"
                + headerCreationDateLine + "\n\n"
                + titleLine + "\n"
                + creationDateLine + "\n";
        return contents;
    }
}
