package net.gsantner.markor.frontend.search;

import android.os.Bundle;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import net.gsantner.markor.R;

// Test
public class TestActivity extends AppCompatActivity {

    private SearchDialogFragment searchDialogFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        EditText editText = findViewById(R.id.editText);
        String content = getIntent().getStringExtra("content");
        if (content != null) {
            editText.setText(content);
        }
        searchDialogFragment = SearchDialogFragment.newInstance(R.id.topReplacementLayout, this, editText);
        searchDialogFragment.show();

        findViewById(R.id.searchButton).setOnClickListener(view -> searchDialogFragment.show());
    }
}
