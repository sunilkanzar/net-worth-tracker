package com.kanzar.networthtracker;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import androidx.appcompat.app.AppCompatActivity;
import com.kanzar.networthtracker.databinding.ActivityCommentBinding;
import com.kanzar.networthtracker.helpers.Month;
import com.kanzar.networthtracker.models.AssetFields;
import com.kanzar.networthtracker.models.Note;
import io.realm.Realm;

public class CommentActivity extends AppCompatActivity {

    private ActivityCommentBinding binding;
    private int month;
    private int year;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCommentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        month = getIntent().getIntExtra(AssetFields.MONTH, 0);
        year  = getIntent().getIntExtra(AssetFields.YEAR, 0);

        Month currentMonth = new Month(month, year);
        binding.headerTitle.setText("Note · " + currentMonth.toString());

        String saved = "";
        try (Realm realm = Realm.getDefaultInstance()) {
            Note note = realm.where(Note.class)
                    .equalTo("id", Note.generateId(month, year))
                    .findFirst();
            if (note != null) {
                saved = note.getContent();
            }
        }

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
        binding.chipPurchase.setOnClickListener(v -> appendTag("🟢 Purchase"));
        binding.chipSell.setOnClickListener(v -> appendTag("🔴 Sell"));
        binding.chipProfit.setOnClickListener(v -> appendTag("📈 Profit Booking"));
        binding.chipLoss.setOnClickListener(v -> appendTag("📉 Loss Harvest"));
        binding.chipReinvest.setOnClickListener(v -> appendTag("🔄 Reinvest"));
        binding.chipChurning.setOnClickListener(v -> appendTag("🎢 Churning"));
        binding.chipIncome.setOnClickListener(v -> appendTag("💰 Income"));
        binding.chipDip.setOnClickListener(v -> appendTag("📉 Dip"));
        binding.chipMilestone.setOnClickListener(v -> appendTag("✨ Milestone"));
    }

    private void appendTag(String tag) {
        Editable text = binding.monthComment.getText();
        if (text == null) return;
        int sel = binding.monthComment.getSelectionEnd();
        
        StringBuilder sb = new StringBuilder();
        if (sel > 0 && text.charAt(sel - 1) != ' ' && text.charAt(sel - 1) != '\n') {
            sb.append(" ");
        }
        sb.append(tag).append(" ");
        
        text.insert(sel, sb.toString());
    }

    private void saveNote() {
        Editable editable = binding.monthComment.getText();
        String text = editable != null ? editable.toString().trim() : "";
        
        try (Realm realm = Realm.getDefaultInstance()) {
            realm.executeTransaction(r -> {
                if (text.isEmpty()) {
                    Note note = r.where(Note.class)
                            .equalTo("id", Note.generateId(month, year))
                            .findFirst();
                    if (note != null) note.deleteFromRealm();
                } else {
                    Note note = new Note(month, year, text);
                    r.copyToRealmOrUpdate(note);
                }
            });
        }

        finish();
    }
}
