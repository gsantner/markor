package net.gsantner.markor.activity.LaunchActivity;
import java.io.File;

public class LaunchSpecialQuicknote extends LaunchSpecialBase {
    @Override
    public File getSpecialFile() {
        return _appSettings.getQuickNoteFile();
    }
}
