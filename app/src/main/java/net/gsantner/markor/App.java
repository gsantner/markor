/*#######################################################
 *
 *   Maintained by Gregor Santner, 2017-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor;

import android.app.Application;

public class App extends Application {
    // Make resources not marked as unused
    @SuppressWarnings("unused")
    private static final Object[] unused_ignore = new Object[]
            {R.color.colorPrimary, R.color.icons, R.color.divider, R.plurals.item_selected, R.string.project_page, R.style.AppTheme, R.raw.readme};

    private static final int[] unused_ignores = new int[]{
            R.string.appearance, R.string.info, R.string.append_to_witharg, R.string.about, R.string.error_cannot_create_notebook_dir__appspecific, R.string.show_license_of_the_app, R.string.show_third_party_licenses, R.string.open_with, R.string.todo_list, R.string.task, R.string.category, R.string.list, R.string.history, R.string.sync, R.string.update, R.string.clear, R.string.due_date, R.string.current_date, R.string.add_task, R.string.add_x_witharg, R.string.create_note, R.string.back_to_previous_folder, R.string.app_settings, R.string.editor_settings, R.string.remember_last_folder_location_on_startup, R.string.number_of_files_witharg, R.string.main_view, R.string.contexts, R.string.projects, R.string.categories, R.string.resources, R.string.vertical_alignment, R.string.horizontal_alignment, R.string.default_, R.string.left, R.string.right, R.string.top, R.string.bottom, R.string.center, R.string.directory, R.string.error_picture_selection, R.string.enable_undo_and_redo_be_patient, R.string.textaction,
            R.string.tags,
    };

    private volatile static App _app;

    public static App get() {
        return _app;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        _app = this;
    }
}
