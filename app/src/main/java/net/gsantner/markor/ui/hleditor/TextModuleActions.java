/*#######################################################
 *
 *   Maintained by Gregor Santner, 2018-
 *   https://gsantner.net/
 *
 *   License: Apache 2.0 / Commercial
 *  https://github.com/gsantner/opoc/#licensing
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.ui.hleditor;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.DrawableRes;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.TableRow;
import android.widget.Toast;

import net.gsantner.markor.R;
import net.gsantner.markor.model.Document;
import net.gsantner.markor.util.ActivityUtils;
import net.gsantner.markor.util.AppSettings;

import static net.gsantner.opoc.util.ShareUtil.REQUEST_TAKE_CAMERA_PICTURE;


@SuppressWarnings("WeakerAccess")
public abstract class TextModuleActions {
    protected HighlightingEditor _hlEditor;
    protected Document _document;
    protected Activity _activity;
    protected Context _context;
    protected AppSettings _appSettings;
    protected ActivityUtils _au;
    private int _textModuleSidePadding;
    protected String _cameraPictureFilepath;

    public TextModuleActions(Activity activity, Document document) {
        _document = document;
        _activity = activity;
        _au = new ActivityUtils(activity);
        _context = activity != null ? activity : _hlEditor.getContext();
        _appSettings = new AppSettings(_context);
        _textModuleSidePadding = (int) (_appSettings.getEditorTextmoduleBarItemPadding() * _context.getResources().getDisplayMetrics().density);
    }

    public abstract void appendTextModuleActionsToBar(ViewGroup viewGroup);

    public View.OnLongClickListener getLongListenerShowingToastWithText(final String text) {
        return v -> {
            try {
                if (!TextUtils.isEmpty(text)) {
                    Toast.makeText(_activity, text, Toast.LENGTH_SHORT).show();
                }
            } catch (Exception ignored) {
            }
            return true;
        };
    }

    protected void appendTextModuleActionToBar(ViewGroup barLayout, @DrawableRes int iconRes, final View.OnClickListener listener, final View.OnLongClickListener longClickListener) {
        ImageView btn = (ImageView) _activity.getLayoutInflater().inflate(R.layout.ui__quick_keyboard_button, null);
        btn.setImageResource(iconRes);
        btn.setOnClickListener(v -> {
            try {
                listener.onClick(v);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        if (longClickListener != null) {
            btn.setOnLongClickListener(v -> {
                try {
                    listener.onClick(v);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                return true;
            });
        }
        btn.setPadding(_textModuleSidePadding, btn.getPaddingTop(), _textModuleSidePadding, btn.getPaddingBottom());

        boolean isDarkTheme = AppSettings.get().isDarkThemeEnabled();
        btn.setColorFilter(ContextCompat.getColor(_context,
                isDarkTheme ? android.R.color.white : R.color.grey));
        barLayout.addView(btn);
    }

    protected void setBarVisible(ViewGroup barLayout, boolean visible) {
        if (barLayout.getId() == R.id.document__fragment__edit__textmodule_actions_bar && barLayout.getParent() instanceof HorizontalScrollView) {
            ((HorizontalScrollView) barLayout.getParent())
                    .setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_TAKE_CAMERA_PICTURE) {
            if (resultCode == Activity.RESULT_OK) {
                if (_cameraPictureFilepath != null) {
                    onPictureTaken(_cameraPictureFilepath);
                } else {
                    _au.showSnackBar(R.string.main__error_unable_to_create_file, false);
                }
            } else {
                _au.showSnackBar(R.string.main__error_no_picture_selected, false);
            }
        }
    }

    //
    //
    //
    //
    public HighlightingEditor getHighlightingEditor() {
        return _hlEditor;
    }

    public TextModuleActions setHighlightingEditor(HighlightingEditor hlEditor) {
        _hlEditor = hlEditor;
        return this;
    }

    protected void onPictureTaken(String url) {

    }

    public Document getDocument() {
        return _document;
    }

    public TextModuleActions setDocument(Document document) {
        _document = document;
        return this;
    }

    public Activity getActivity() {
        return _activity;
    }

    public TextModuleActions setActivity(Activity activity) {
        _activity = activity;
        return this;
    }

    public Context getContext() {
        return _context;
    }

    public TextModuleActions setContext(Context context) {
        _context = context;
        return this;
    }
}
