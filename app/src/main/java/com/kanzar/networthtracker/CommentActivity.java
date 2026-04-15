package com.kanzar.networthtracker;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.kanzar.networthtracker.helpers.Prefs;
import com.kanzar.networthtracker.models.AssetFields;

public class CommentActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comment);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        }

        int month = getIntent().getIntExtra(AssetFields.MONTH, 0);
        int year  = getIntent().getIntExtra(AssetFields.YEAR, 0);
        String noteKey = noteKey(month, year);

        TextInputEditText monthComment = findViewById(R.id.monthComment);
        ExtendedFloatingActionButton saveButton = findViewById(R.id.newCommentSave);

        // Load existing note
        if (monthComment != null) {
            monthComment.setText(Prefs.getString(noteKey, ""));
        }

        if (saveButton != null) {
            saveButton.setOnClickListener(v -> {
                String text = monthComment != null ? monthComment.getText().toString().trim() : "";
                if (text.isEmpty()) {
                    Prefs.delete(noteKey);
                } else {
                    Prefs.save(noteKey, text);
                }
                finish();
            });
        }
    }

    public static String noteKey(int month, int year) {
        return "note_" + year + "_" + month;
    }
}
