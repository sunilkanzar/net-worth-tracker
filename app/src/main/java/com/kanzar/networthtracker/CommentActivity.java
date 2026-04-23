package com.kanzar.networthtracker;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import androidx.appcompat.app.AppCompatActivity;
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

        Month currentMonth = new Month(month, year);
        binding.headerTitle.setText("Note · " + currentMonth.toString());

        String saved = Prefs.getString(noteKey, "");
        binding.monthComment.setText(saved);
        binding.charCount.setText(saved.length() + " characters");

        binding.btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        binding.btnSave.setOnClickListener(v -> saveNote());

        binding.monthComment.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                binding.charCount.setText(s.length() + " characters");
            }
        });

        binding.chipGoal.setOnClickListener(v -> appendTag("🎯 Goal"));
        binding.chipIncome.setOnClickListener(v -> appendTag("💰 Income"));
        binding.chipDip.setOnClickListener(v -> appendTag("📉 Dip"));
        binding.chipMilestone.setOnClickListener(v -> appendTag("✨ Milestone"));
    }

    private void appendTag(String tag) {
        Editable text = binding.monthComment.getText();
        if (text == null) return;
        String current = text.toString();
        String insert = current.isEmpty() ? tag + " " : (current.endsWith(" ") ? tag + " " : " " + tag + " ");
        int sel = binding.monthComment.getSelectionEnd();
        text.insert(sel, insert);
    }

    private void saveNote() {
        Editable editable = binding.monthComment.getText();
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
