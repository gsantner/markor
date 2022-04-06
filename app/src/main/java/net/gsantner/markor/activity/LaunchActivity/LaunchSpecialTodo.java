package net.gsantner.markor.activity.LaunchActivity;
import java.io.File;

public class LaunchSpecialTodo extends LaunchSpecialBase {
    @Override
    public File getSpecialFile() {
        return _appSettings.getTodoFile();
    }
}

