package net.gsantner.markor.activity;

import net.gsantner.markor.R;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

public class ActionOrderActivity extends AppCompatActivity {

    private Toolbar _toolbar;
    private RecyclerView _recycler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.action_reorder_activity);

        _toolbar = findViewById(R.id.action_reorder_toolbar);
        setSupportActionBar(_toolbar);

        ActionBar bar = getSupportActionBar();
        bar.setDisplayHomeAsUpEnabled(true);

        _recycler = findViewById(R.id.action_reorder_recycler);

        // Pull settings
        // Create recycler options
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.reorder_actions_actionbar__menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {

            case android.R.id.home:
                finish();
                return true;

            case R.id.action_reorder_accept:
                // Save order state
                // Return to settings
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
