package com.kanzar.networthtracker;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import androidx.appcompat.app.AppCompatActivity;
import com.kanzar.networthtracker.databinding.ActivityCommentBinding;
import com.kanzar.networthtracker.helpers.Month;
import com.kanzar.networthtracker.helpers.Tools;
import com.kanzar.networthtracker.models.AssetFields;
import com.kanzar.networthtracker.models.Note;
import com.kanzar.networthtracker.models.NoteTag;
import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import com.kanzar.networthtracker.databinding.DialogManageTagsBinding;
import com.kanzar.networthtracker.databinding.ItemTagManageBinding;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.core.content.ContextCompat;
import android.content.res.ColorStateList;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
                saved = note.getContent() != null ? note.getContent() : "";
            }
        }

        binding.monthComment.setText(saved);
        if (saved != null) binding.monthComment.setSelection(saved.length());
        binding.charCount.setText(saved.length() + " characters");

        binding.btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        binding.btnSave.setOnClickListener(v -> saveNote());
        binding.btnManageTags.setOnClickListener(v -> showManageTagsDialog());

        applyAccentColor();
        binding.monthComment.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                binding.charCount.setText(s.length() + " characters");
            }
        });

        setupTags();
    }

    private void applyAccentColor() {
        int accentColor = ContextCompat.getColor(this, Tools.getAccentColor());
        binding.btnSave.setBackgroundTintList(ColorStateList.valueOf(accentColor));
        binding.btnManageTags.setTextColor(accentColor);
    }

    private void setupTags() {
        try (Realm realm = Realm.getDefaultInstance()) {
            RealmResults<NoteTag> tags = realm.where(NoteTag.class).sort("index", Sort.ASCENDING).findAll();
            if (tags.isEmpty()) {
                realm.executeTransaction(r -> {
                    List<String> defaults = Arrays.asList(
                        "🟢Buy", "🔴Sell", "📉Dip", "🔄Reinvest", "💰Extra Income"
                    );
                    for (int i = 0; i < defaults.size(); i++) {
                        r.copyToRealmOrUpdate(new NoteTag(defaults.get(i), i));
                    }
                });
                tags = realm.where(NoteTag.class).sort("index", Sort.ASCENDING).findAll();
            }

            binding.chipRow.removeAllViews();
            for (NoteTag tag : tags) {
                String tagName = tag.getName();
                View chip = LayoutInflater.from(this).inflate(R.layout.item_note_chip, binding.chipRow, false);
                TextView tv = (TextView) chip;
                tv.setText(tagName);
                tv.setOnClickListener(v -> appendTag(tagName));
                binding.chipRow.addView(chip);
            }
            binding.chipRow.addView(binding.btnManageTags);
        }
    }

    private void showManageTagsDialog() {
        DialogManageTagsBinding dBinding = DialogManageTagsBinding.inflate(getLayoutInflater());
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dBinding.getRoot())
                .create();

        int accentColor = ContextCompat.getColor(this, Tools.getAccentColor());
        dBinding.btnAddTag.setBackgroundTintList(ColorStateList.valueOf(accentColor));
        dBinding.btnDone.setTextColor(accentColor);

        dBinding.rvTags.setLayoutManager(new LinearLayoutManager(this));
        TagAdapter adapter = new TagAdapter();
        dBinding.rvTags.setAdapter(adapter);

        ItemTouchHelper touchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                int from = viewHolder.getAdapterPosition();
                int to = target.getAdapterPosition();
                adapter.moveItem(from, to);
                return true;
            }
            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {}

            @Override
            public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                adapter.saveOrder();
                setupTags();
            }
        });
        touchHelper.attachToRecyclerView(dBinding.rvTags);

        dBinding.btnAddTag.setOnClickListener(v -> {
            String name = dBinding.editNewTag.getText().toString().trim();
            if (!name.isEmpty()) {
                try (Realm realm = Realm.getDefaultInstance()) {
                    realm.executeTransaction(r -> {
                        long count = r.where(NoteTag.class).count();
                        r.copyToRealmOrUpdate(new NoteTag(name, (int) count));
                    });
                }
                dBinding.editNewTag.setText("");
                adapter.refresh();
                setupTags();
            }
        });

        dBinding.btnDone.setOnClickListener(v -> dialog.dismiss());
        Tools.styleDialog(dialog);
        dialog.show();
    }

    private class TagAdapter extends RecyclerView.Adapter<TagAdapter.ViewHolder> {
        private List<NoteTag> tags = new ArrayList<>();

        TagAdapter() {
            refresh();
        }

        void refresh() {
            try (Realm realm = Realm.getDefaultInstance()) {
                tags = realm.copyFromRealm(realm.where(NoteTag.class).sort("index", Sort.ASCENDING).findAll());
            }
            notifyDataSetChanged();
        }

        void moveItem(int from, int to) {
            NoteTag movedItem = tags.remove(from);
            tags.add(to, movedItem);
            notifyItemMoved(from, to);
        }

        void saveOrder() {
            try (Realm realm = Realm.getDefaultInstance()) {
                realm.executeTransaction(r -> {
                    for (int i = 0; i < tags.size(); i++) {
                        NoteTag tag = tags.get(i);
                        NoteTag dbTag = r.where(NoteTag.class).equalTo("name", tag.getName()).findFirst();
                        if (dbTag != null) dbTag.setIndex(i);
                    }
                });
            }
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(ItemTagManageBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            NoteTag tag = tags.get(position);
            holder.binding.tagName.setText(tag.getName());
            
            View.OnClickListener editListener = v -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(CommentActivity.this);
                builder.setTitle("Edit Tag");
                final android.widget.EditText input = new android.widget.EditText(CommentActivity.this);
                input.setText(tag.getName());
                builder.setView(input);
                builder.setPositiveButton("Save", (d, which) -> {
                    String newName = input.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        try (Realm realm = Realm.getDefaultInstance()) {
                            realm.executeTransaction(r -> {
                                NoteTag toUpdate = r.where(NoteTag.class).equalTo("name", tag.getName()).findFirst();
                                if (toUpdate != null) {
                                    int oldIndex = toUpdate.getIndex();
                                    NoteTag updated = new NoteTag(newName, oldIndex);
                                    r.copyToRealmOrUpdate(updated);
                                    if (!newName.equals(tag.getName())) {
                                        toUpdate.deleteFromRealm();
                                    }
                                }
                            });
                        }
                        refresh();
                        setupTags();
                    }
                });
                builder.setNegativeButton("Cancel", (d, which) -> d.cancel());
                AlertDialog editDialog = builder.create();
                Tools.styleDialog(editDialog);
                editDialog.show();
            };

            holder.binding.btnEdit.setOnClickListener(editListener);
            holder.binding.tagName.setOnClickListener(editListener);

            holder.binding.btnDelete.setOnClickListener(v -> {
                try (Realm realm = Realm.getDefaultInstance()) {
                    realm.executeTransaction(r -> {
                        NoteTag toDelete = r.where(NoteTag.class).equalTo("name", tag.getName()).findFirst();
                        if (toDelete != null) toDelete.deleteFromRealm();
                    });
                }
                refresh();
                setupTags();
            });
        }

        @Override
        public int getItemCount() {
            return tags.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ItemTagManageBinding binding;
            ViewHolder(ItemTagManageBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }

    private void appendTag(String tag) {
        binding.monthComment.requestFocus();
        Editable text = binding.monthComment.getText();
        if (text == null) return;
        int sel = binding.monthComment.getSelectionEnd();
        if (sel < 0) sel = text.length();
        
        StringBuilder sb = new StringBuilder();
        if (sel > 0 && text.charAt(sel - 1) != '\n') {
            sb.append("\n");
        }
        sb.append(tag).append(" ");
        
        text.insert(sel, sb.toString());
        binding.monthComment.setSelection(sel + sb.length());

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(binding.monthComment, InputMethodManager.SHOW_IMPLICIT);
        }
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
