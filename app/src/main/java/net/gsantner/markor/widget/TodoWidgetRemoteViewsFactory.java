package net.gsantner.markor.widget;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import net.gsantner.markor.ApplicationObject;
import net.gsantner.markor.R;
import net.gsantner.markor.format.todotxt.TodoTxtTask;
import net.gsantner.markor.model.AppSettings;
import net.gsantner.markor.model.Document;
import net.gsantner.opoc.util.GsContextUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class TodoWidgetRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {

    private final Context _context;
    private final AppSettings _appSettings;
    private final Document _document;
    private final List<TodoTxtTask> _tasks;

    public TodoWidgetRemoteViewsFactory(Context context, Intent intent) {
        _context = context;
        _appSettings = ApplicationObject.settings();
        _document = new Document(_appSettings.getTodoFile());
        _tasks = new ArrayList<>();
    }

    @Override
    public void onCreate() {
        onDataSetChanged();
    }

    @Override
    public void onDataSetChanged() {
        _tasks.clear();
        final String content = _document.loadContent(_context);
        if (content == null) {
            return;
        }
        List<TodoTxtTask> tasks = TodoTxtTask.getAllTasks(content);
        _tasks.addAll(tasks);
    }

    @Override
    public void onDestroy() {
        _tasks.clear();
    }

    @Override
    public int getCount() {
        return _tasks.size();
    }

    @Override
    public RemoteViews getViewAt(int position) {
        RemoteViews views = new RemoteViews(_context.getPackageName(), R.layout.todo_widget_list_item);

        views.setTextViewText(R.id.todo_widget_item_text, getTaskSpannable(_tasks.get(position)));

        final Intent fillInIntent = new Intent()
                .putExtra(Document.EXTRA_FILE_LINE_NUMBER, position);
        views.setOnClickFillInIntent(R.id.todo_widget_item_text, fillInIntent);

        return views;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    private Spannable getTaskSpannable(TodoTxtTask task) {
        SpannableStringBuilder spannable = new SpannableStringBuilder();
        int currentPos = 0;
        final char priority = task.getPriority();
        final String dueDate = task.getDueDate();
        final String description = task.getDescription();
        final boolean isDone = task.isDone();

        if (priority != TodoTxtTask.PRIORITY_NONE) {
            spannable.append(String.valueOf(priority));
            spannable.setSpan(new ForegroundColorSpan(getColorFromPriority(priority)), currentPos, ++currentPos, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new StyleSpan(Typeface.BOLD), 0, currentPos, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        if (!dueDate.isEmpty()) {
            currentPos = addSpaceIfNecessary(currentPos, spannable);
            String displayDueDate = Calendar.getInstance().get(Calendar.YEAR) == Integer.parseInt(dueDate.substring(0, 4))
                    ? dueDate.substring(5) : dueDate;
            spannable.append(displayDueDate);
            spannable.setSpan(new ForegroundColorSpan(0xffEF2929), currentPos, currentPos += displayDueDate.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new StyleSpan(Typeface.ITALIC), currentPos - displayDueDate.length(), currentPos, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        if (!description.isEmpty()) {
            currentPos = addSpaceIfNecessary(currentPos, spannable);
            spannable.append(description);
            spannable.setSpan(new ForegroundColorSpan(_appSettings.getEditorForegroundColor()), currentPos, currentPos += description.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        currentPos = appendTags(spannable, task.getProjects(), '+', 0xffef6C00, currentPos);
        currentPos = appendTags(spannable, task.getContexts(), '@', 0xff88b04b, currentPos);
        if(isDone){
            spannable.setSpan(new ForegroundColorSpan(GsContextUtils.instance.isDarkModeEnabled(_appSettings.getContext()) ? 0x999d9d9d : 0x993d3d3d), 0, currentPos, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new StrikethroughSpan(), 0, currentPos, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        return spannable;
    }

    private static int addSpaceIfNecessary(int currentPos, SpannableStringBuilder spannable) {
        if (currentPos > 0) {
            spannable.append(" ");
            currentPos++;
        }
        return currentPos;
    }

    private int appendTags(SpannableStringBuilder spannable, List<String> tags, char prefix, int color, int currentPos) {
        for (String tag : tags) {
            currentPos = addSpaceIfNecessary(currentPos, spannable);
            spannable.append(prefix).append(tag);
            spannable.setSpan(new ForegroundColorSpan(color), currentPos, currentPos += tag.length() + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return currentPos;
    }

    private int getColorFromPriority(char priority) {
        final int COLOR_PRIORITY_A = 0xffEF2929;
        final int COLOR_PRIORITY_B = 0xffd16900;
        final int COLOR_PRIORITY_C = 0xff59a112;
        final int COLOR_PRIORITY_D = 0xff0091c2;
        final int COLOR_PRIORITY_E = 0xffa952cb;
        final int COLOR_PRIORITY_F = 0xff878986;

        switch (priority)
        {
            case 'A':
                return COLOR_PRIORITY_A;
            case 'B':
                return COLOR_PRIORITY_B;
            case 'C':
                return COLOR_PRIORITY_C;
            case 'D':
                return COLOR_PRIORITY_D;
            case 'E':
                return COLOR_PRIORITY_E;
            case 'F':
                return COLOR_PRIORITY_F;
            default:
                return _appSettings.getEditorForegroundColor();
        }
    }
}
