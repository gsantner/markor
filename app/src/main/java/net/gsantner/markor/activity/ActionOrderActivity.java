/*#######################################################
 *
 *   Maintained by Gregor Santner, 2018-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.activity;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import net.gsantner.markor.R;
import net.gsantner.markor.format.markdown.MarkdownTextActions;
import net.gsantner.markor.format.plaintext.PlaintextTextActions;
import net.gsantner.markor.format.todotxt.TodoTxtTextActions;
import net.gsantner.markor.ui.hleditor.TextActions;
import net.gsantner.markor.util.ActivityUtils;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.markor.util.ContextUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static android.support.v7.widget.helper.ItemTouchHelper.ACTION_STATE_DRAG;

public class ActionOrderActivity extends AppCompatActivity {

    public static final String EXTRA_FORMAT_KEY = "FORMAT_KEY";

    private OrderAdapter _adapter;
    private List<String> _keys;
    private List<String> _disabled;
    private List<TextActions.ActionItem> _actions;
    private TextActions _textActions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setExitTransition(null);
        }
        final AppSettings appSettings = new AppSettings(this);
        final ActivityUtils contextUtils = new ActivityUtils(this);
        contextUtils.setAppLanguage(appSettings.getLanguage());
        setTheme(R.style.AppTheme_Light);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.action_order_activity);

        //  Set back button
        final Toolbar _toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(_toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Set up recyclerview
        final RecyclerView recycler = findViewById(R.id.action_order_activity_recycler);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.addItemDecoration(new DividerItemDecoration(recycler.getContext(), DividerItemDecoration.VERTICAL));

        extractActionData();
        _adapter = new OrderAdapter(_actions, _keys, _disabled);

        final ItemTouchHelper.Callback callback = new ReorderCallback(_adapter);
        final ItemTouchHelper helper = new ItemTouchHelper(callback);
        helper.attachToRecyclerView(recycler);

        recycler.setHasFixedSize(true);
        recycler.setAdapter(_adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.action_order__menu, menu);

        final ContextUtils cu = ContextUtils.get();
        cu.tintMenuItems(menu, true, Color.WHITE);
        cu.freeContextRef();

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                finish();
                return true;
            }

            case R.id.action_reorder_reset: {
                final List<String> activeKeys = _textActions.getActiveActionKeys();
                for (int i = 0; i < activeKeys.size(); i++) {
                    String key = activeKeys.get(i);
                    _adapter.order.set(i, _keys.indexOf(key));
                }
                _adapter.notifyDataSetChanged();
                return true;
            }
        }
        return false;
    }

    private void saveNewOrder() {
        final ArrayList<String> reorderedKeys = new ArrayList<>();

        for (final int i : _adapter.order) {
            reorderedKeys.add(_keys.get(i));
        }

        _textActions.saveActionOrder(reorderedKeys);
        _textActions.saveDisabledActions(new ArrayList<>(_adapter._disabled));
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveNewOrder();
    }

    @SuppressWarnings("ConstantConditions")
    private void extractActionData() {
        final int documentType = getIntent().getExtras().getInt(EXTRA_FORMAT_KEY);

        switch (documentType) {
            default:
            case R.id.action_format_markdown:
                _textActions = new MarkdownTextActions(this, null);
                break;
            case R.id.action_format_todotxt:
                _textActions = new TodoTxtTextActions(this, null);
                break;
            case R.id.action_format_plaintext:
                _textActions = new PlaintextTextActions(this, null);
                break;
        }

        final Map<String, TextActions.ActionItem> actionMap = _textActions.getActiveActionMap();
        _keys = _textActions.getActionOrder();
        _disabled = _textActions.getDisabledActions();

        _actions = new ArrayList<>();
        for (final String key : _keys) {
            _actions.add(actionMap.get(key));
        }
    }

    private static class Holder extends RecyclerView.ViewHolder {
        private final RelativeLayout _row;

        private Holder(View row) {
            super(row);
            _row = (RelativeLayout) row;
        }

        private void bindModel(final TextActions.ActionItem action, final String key, final Set<String> disabled) {
            final Switch enabled = _row.findViewById(R.id.enabled_switch);
            enabled.setOnCheckedChangeListener(null);

            enabled.setChecked(!disabled.contains(key));
            enabled.setOnCheckedChangeListener((button, isChecked) -> {
                if (isChecked) {
                    disabled.remove(key);
                } else {
                    disabled.add(key);
                }
            });

            ((ImageView) _row.findViewById(R.id.start_icon)).setImageResource(action.iconId);
            ((TextView) _row.findViewById(R.id.action_text)).setText(action.stringId);
        }

        public void setHighlight() {
            _row.setAlpha(0.5f);
        }

        public void unsetHighlight() {
            _row.setAlpha(1.0f);
        }
    }

    private class OrderAdapter extends RecyclerView.Adapter<Holder> {
        private final List<TextActions.ActionItem> _actions;
        private final List<String> _keys;
        private final Set<String> _disabled;
        private final List<Integer> order;

        private OrderAdapter(List<TextActions.ActionItem> actions, List<String> keys, List<String> disabled) {
            super();
            _actions = actions;
            _keys = keys;
            _disabled = new HashSet<>(disabled);

            order = new ArrayList<>();
            for (int i = 0; i < _actions.size(); i++) {
                order.add(i);
            }
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(getLayoutInflater().inflate(R.layout.action_order_item, parent, false));
        }

        @Override
        public void onBindViewHolder(Holder holder, int position) {
            final int index = order.get(position);
            holder.bindModel(_actions.get(index), _keys.get(index), _disabled);
        }

        @Override
        public int getItemCount() {
            return _actions.size();
        }
    }

    private class ReorderCallback extends ItemTouchHelper.SimpleCallback {
        private final OrderAdapter _adapter;

        private ReorderCallback(OrderAdapter adapter) {
            super(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0);
            _adapter = adapter;
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
            final int from = viewHolder.getAdapterPosition();
            final int to = target.getAdapterPosition();

            final int value = _adapter.order.get(from);
            _adapter.order.remove(from);
            _adapter.order.add(to, value);
            _adapter.notifyItemMoved(from, to);

            return true;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            // Not implemented, no swiping
        }

        @Override
        public void onSelectedChanged(RecyclerView.ViewHolder holder, int actionState) {
            if (actionState == ACTION_STATE_DRAG) {
                ((Holder) holder).setHighlight();
            }
        }

        @Override
        public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder holder) {
            super.clearView(recyclerView, holder);
            ((Holder) holder).unsetHighlight();
        }
    }
}
