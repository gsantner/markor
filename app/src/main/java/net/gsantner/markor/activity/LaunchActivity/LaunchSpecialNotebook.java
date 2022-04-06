package net.gsantner.markor.activity.LaunchActivity;
import java.io.File;

public class LaunchSpecialNotebook extends LaunchSpecialBase {
    @Override
    public File getSpecialFile() {
        return _appSettings.getNotebookDirectory();
    }
}
