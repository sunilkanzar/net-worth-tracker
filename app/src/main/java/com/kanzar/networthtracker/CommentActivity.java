package com.kanzar.networthtracker;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.kanzar.networthtracker.databinding.ActivityCommentBinding;
import com.kanzar.networthtracker.helpers.Month;
import com.kanzar.networthtracker.helpers.Prefs;
import com.kanzar.networthtracker.models.AssetFields;

public class CommentActivity extends AppCompatActivity {

    private ActivityCommentBinding binding;
    private String noteKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCommentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        int month = getIntent().getIntExtra(AssetFields.MONTH, 0);
        int year  = getIntent().getIntExtra(AssetFields.YEAR, 0);
        noteKey = noteKey(month, year);

        binding.monthComment.setText(Prefs.getString(noteKey, ""));

        Month currentMonth = new Month(month, year);
        binding.toolbar.setTitle("Note For " + currentMonth.toString());

        binding.toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_save_note) {
                saveNote();
                return true;
            }
            return false;
        });
    }

    private void saveNote() {
        android.text.Editable editable = binding.monthComment.getText();
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
