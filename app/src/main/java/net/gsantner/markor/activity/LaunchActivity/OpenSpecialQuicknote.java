package net.gsantner.markor.activity.LaunchActivity;
import java.io.File;

public class OpenSpecialQuicknote extends OpenSpecialBase {
    @Override
    public File getSpecialFile() {
        return _appSettings.getQuickNoteFile();
    }
}
