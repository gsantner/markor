package net.gsantner.markor.activity.LaunchActivity;
import java.io.File;

public class OpenSpecialTodo extends OpenSpecialBase {
    @Override
    public File getSpecialFile() {
        return _appSettings.getTodoFile();
    }
}

