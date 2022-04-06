package net.gsantner.markor.activity.LaunchActivity;
import java.io.File;

public class OpenSpecialNotebook extends OpenSpecialBase {
    @Override
    public File getSpecialFile() {
        return _appSettings.getNotebookDirectory();
    }
}
