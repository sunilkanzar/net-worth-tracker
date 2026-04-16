package com.kanzar.networthtracker;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.kanzar.networthtracker.helpers.Prefs;
import com.kanzar.networthtracker.models.AssetFields;

public class CommentActivity extends AppCompatActivity {

    private TextInputEditText monthComment;
    private String noteKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comment);

        int month = getIntent().getIntExtra(AssetFields.MONTH, 0);
        int year  = getIntent().getIntExtra(AssetFields.YEAR, 0);
        noteKey = noteKey(month, year);

        monthComment = findViewById(R.id.monthComment);
        monthComment.setText(Prefs.getString(noteKey, ""));

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_save_note) {
                saveNote();
                return true;
            }
            return false;
        });
    }

    private void saveNote() {
        android.text.Editable editable = monthComment.getText();
        String text = editable != null ? editable.toString().trim() : "";
        if (text.isEmpty()) {
            Prefs.delete(noteKey);
        } else {
            Prefs.save(noteKey, text);
        }
        finish();
    }

    public static String noteKey(int month, int year) {
        return "note_" + year + "_" + month;
    }
}
