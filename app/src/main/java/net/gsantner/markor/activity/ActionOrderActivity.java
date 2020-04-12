package net.gsantner.markor.activity;

import net.gsantner.markor.R;
import net.gsantner.markor.format.markdown.MarkdownTextActions;
import net.gsantner.markor.format.plaintext.PlaintextTextActions;
import net.gsantner.markor.format.todotxt.TodoTxtTextActions;
import net.gsantner.markor.ui.hleditor.TextActions;
import net.gsantner.markor.util.ContextUtils;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
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
import android.widget.LinearLayout;
import android.widget.TextView;

import com.vladsch.flexmark.util.collection.OrderedMap;

import java.util.ArrayList;
import java.util.List;

import static android.support.v7.widget.helper.ItemTouchHelper.ACTION_STATE_DRAG;

public class ActionOrderActivity extends AppCompatActivity {

    private Adapter _adapter;
    private ArrayList<String> _keys;
    private ArrayList<ActionItem> _actions;
    private TextActions _textActions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.action_reorder_activity);

        //  Set back button
        Toolbar _toolbar = findViewById(R.id.action_reorder_toolbar);
        setSupportActionBar(_toolbar);
        ActionBar bar = getSupportActionBar();
        assert bar != null;
        bar.setDisplayHomeAsUpEnabled(true);

        // Set up recyclerview
        RecyclerView recycler = findViewById(R.id.action_reorder_recycler);
        recycler.setLayoutManager(new LinearLayoutManager(this));

        extractActionData();
        _adapter = new Adapter(_actions);

        ItemTouchHelper.Callback callback = new ReorderCallback(_adapter);
        ItemTouchHelper helper = new ItemTouchHelper(callback);
        helper.attachToRecyclerView(recycler);

        recycler.setHasFixedSize(true);
        recycler.setAdapter(_adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.reorder_actions_actionbar__menu, menu);

        ContextUtils cu = ContextUtils.get();
        cu.tintMenuItems(menu, true, Color.WHITE);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {

            case android.R.id.home:
                // Return to settings
                finish();
                return true;

            case R.id.action_reorder_accept:
                ArrayList<String> reorderedKeys = new ArrayList<>();
                for (int i : _adapter.order) reorderedKeys.add(_keys.get(i));
                _textActions.saveActionOrder(reorderedKeys);
                finish();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void extractActionData() {

        // Extract actions
        int documentType = getIntent().getExtras().getInt("key");

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

        OrderedMap<String, ActionItem> actionMap = _textActions.getActiveActionMap();
        _keys = new ArrayList<>(_textActions.getActionOrder());
        _actions = new ArrayList<ActionItem>();

        for (String key: _keys) {
            _actions.add(actionMap.get(key));
        }
    }

    class Adapter extends RecyclerView.Adapter<Holder> {
        private List<ActionItem> _actions;
        ArrayList<Integer> order;

        Adapter(List<ActionItem> actions) {
            super();
            _actions = actions;

            order = new ArrayList<>();
            for (int i = 0; i <_actions.size(); i++) order.add(i);
        }

        @Override
        public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new Holder(getLayoutInflater().inflate(R.layout.action_reorder_item, parent, false));
        }

        @Override
        public void onBindViewHolder(Holder holder, int position) {
            holder.bindModel(_actions.get(order.get(position)));
        }

        @Override
        public int getItemCount() {
            return _actions.size();
        }
    }

    static class Holder extends RecyclerView.ViewHolder {

        private LinearLayout _row;

        Holder(View row) {
            super(row);
            _row = (LinearLayout) row;
        }

        void bindModel(ActionItem action) {
            ((ImageView) _row.getChildAt(0)).setImageResource(action.iconId);
            ((TextView) _row.getChildAt(1)).setText(action.stringId);
        }

        void setHighlight() {
            _row.setAlpha(0.5f);
        }

        void unsetHighlight() {
            _row.setAlpha(1.0f);
        }

    }

    class ReorderCallback extends ItemTouchHelper.SimpleCallback {

        Adapter _adapter;

        ReorderCallback(Adapter adapter) {
            super(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0);
            _adapter = adapter;
        }

        @Override
        public boolean onMove(
                RecyclerView recyclerView,
                RecyclerView.ViewHolder viewHolder,
                RecyclerView.ViewHolder target) {

            final int from = viewHolder.getAdapterPosition();
            final int to = target.getAdapterPosition();

            int value = _adapter.order.get(from);
            _adapter.order.remove(from);
            _adapter.order.add(to, value);
            _adapter.notifyItemMoved(from, to);

            return true;
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
            // Not implemented, no swiping
        }

        @Override
        public void onSelectedChanged(RecyclerView.ViewHolder holder, int actionState) {
            if (actionState == ACTION_STATE_DRAG) {
                ((Holder) holder).setHighlight();
            }
        }

        @Override
        public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder holder) {
            super.clearView(recyclerView, holder);
            ((Holder) holder).unsetHighlight();
        }
    }
}


