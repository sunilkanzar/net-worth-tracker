package com.kanzar.networthtracker;

import android.content.ComponentName;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.Toast;
import android.animation.ObjectAnimator;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.view.GravityCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.kanzar.networthtracker.databinding.ActivityMainBinding;
import com.kanzar.networthtracker.helpers.Month;
import com.kanzar.networthtracker.helpers.Prefs;
import com.kanzar.networthtracker.helpers.Tools;
import com.kanzar.networthtracker.models.Asset;
import com.kanzar.networthtracker.models.AssetFields;
import com.kanzar.networthtracker.models.Goal;
import com.kanzar.networthtracker.models.Note;
import com.kanzar.networthtracker.statistics.Events;
import com.kanzar.networthtracker.statistics.events.AssetDeleted;
import com.kanzar.networthtracker.statistics.events.ButtonClicked;
import com.kanzar.networthtracker.statistics.events.PermissionNotGranted;
import com.kanzar.networthtracker.statistics.events.SigninClicked;
import com.kanzar.networthtracker.statistics.events.SignoutClicked;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.text.DateFormatSymbols;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.realm.Realm;
import io.realm.Sort;

import com.kanzar.networthtracker.backup.LocalBackup;
import com.kanzar.networthtracker.backup.LocalImport;
import com.kanzar.networthtracker.eventbus.BackupSavedEvent;
import com.kanzar.networthtracker.eventbus.DataChangedEvent;
import com.kanzar.networthtracker.eventbus.ImportedEvent;
import com.kanzar.networthtracker.eventbus.MessageEvent;

public class MainActivity extends AppCompatActivity implements MonthPageFragment.Listener, SyncManager.SyncListener, AssetSheetManager.Listener {

    private static final int PERMISSION_EXPORT = 1;
    private static final int PERMISSION_EXPORT_AUTO = 2;
    private static final int PERMISSION_IMPORT = 3;

    private Month month = new Month();
    private ActivityMainBinding binding;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    
    private SyncManager syncManager;
    private AssetSheetManager assetSheetManager;
    
    private ObjectAnimator syncAnim;
    private boolean firstResume = true;
    private boolean fabExpanded = false;

    private final ActivityResultLauncher<IntentSenderRequest> authLauncher = registerForActivityResult(
        new ActivityResultContracts.StartIntentSenderForResult(), result -> {
            if (result.getResultCode() == RESULT_OK) syncManager.requestDriveAuthorizationAndSync();
        });

    private final ActivityResultLauncher<String[]> importFileLauncher = registerForActivityResult(
        new ActivityResultContracts.OpenDocument(), uri -> {
            if (uri != null) { try { LocalImport.importFromUri(uri); } catch (Exception e) { Toast.makeText(this, R.string.import_empty, Toast.LENGTH_SHORT).show(); } }
        });

    private final ActivityResultLauncher<Intent> monthPickerLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Intent d = result.getData();
                binding.viewPager.setCurrentItem(MonthPagerAdapter.positionOf(d.getIntExtra("month", month.getMonth()), d.getIntExtra("year", month.getYear())), false);
            }
        });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        syncManager = new SyncManager(this, this, authLauncher);
        assetSheetManager = new AssetSheetManager(this, binding, this);
        
        setupViewPager();
        setupNavigation();
        viewListeners();
        handleIntent(getIntent());
        updateLoginUI();
        setupBackHandler();
    }

    private void setupViewPager() {
        binding.viewPager.setAdapter(new MonthPagerAdapter(this));
        binding.viewPager.setOffscreenPageLimit(2);
        binding.viewPager.setCurrentItem(MonthPagerAdapter.positionOf(month.getMonth(), month.getYear()), false);
        binding.viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                int[] my = MonthPagerAdapter.monthYearAt(position);
                month = new Month(my[0], my[1], false);
                binding.monthName.setText(month.toString());
                updateMonthNameColor();
                assetSheetManager.close();
            }
        });
    }

    private void setupNavigation() {
        binding.drawerLayout.addDrawerListener(new androidx.drawerlayout.widget.DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerClosed(View drawerView) {
                if (binding.navigationView.navBackupSubmenu.getVisibility() == View.VISIBLE) {
                    binding.navigationView.navBackupSubmenu.setVisibility(View.GONE);
                    binding.navigationView.navBackupChevron.setRotation(0f);
                }
                if (binding.navigationView.navImportExportSubmenu.getVisibility() == View.VISIBLE) {
                    binding.navigationView.navImportExportSubmenu.setVisibility(View.GONE);
                    binding.navigationView.navImportExportChevron.setRotation(0f);
                }
            }
        });
    }

    private void viewListeners() {
        binding.monthName.setOnClickListener(v -> showMonthYearPicker());
        binding.monthName.setOnLongClickListener(v -> goToCurrentMonth());
        
        binding.navigationView.googleSignIn.setOnClickListener(v -> { new Events().send(new SigninClicked()); syncManager.startSignIn(); });
        binding.navigationView.signOut.setOnClickListener(v -> { new Events().send(new SignoutClicked()); syncManager.signOut(); });
        binding.navigationView.navPreferences.setOnClickListener(v -> { binding.drawerLayout.closeDrawers(); startActivity(new Intent(this, PreferencesActivity.class)); });
        binding.navigationView.navMonthly.setOnClickListener(v -> { new Events().send(new ButtonClicked("monthView")); monthPickerLauncher.launch(new Intent(this, MonthActivity.class)); binding.drawerLayout.closeDrawers(); });
        binding.navigationView.navGraph.setOnClickListener(v -> { new Events().send(new ButtonClicked("chartView")); startActivity(new Intent(this, ChartActivity.class)); binding.drawerLayout.closeDrawers(); });
        binding.navigationView.navAllocation.setOnClickListener(v -> { Intent i = new Intent(this, AllocationActivity.class); i.putExtra("month", month.getMonth()); i.putExtra("year", month.getYear()); startActivity(i); binding.drawerLayout.closeDrawers(); });
        binding.navigationView.navAssetTrend.setOnClickListener(v -> { startActivity(new Intent(this, AssetTrendActivity.class)); binding.drawerLayout.closeDrawers(); });
        binding.navigationView.navSetGoal.setOnClickListener(v -> { Intent i = new Intent(this, GoalActivity.class); i.putExtra("current_net_worth", month.getValue()); startActivity(i); binding.drawerLayout.closeDrawers(); });
        binding.navigationView.drawerGoalSection.setOnClickListener(v -> { Intent i = new Intent(this, GoalActivity.class); i.putExtra("current_net_worth", month.getValue()); startActivity(i); binding.drawerLayout.closeDrawers(); });
        
        binding.navigationView.navBackup.setOnClickListener(v -> toggleSubmenu(binding.navigationView.navBackupSubmenu, binding.navigationView.navBackupChevron));
        binding.navigationView.navImportExport.setOnClickListener(v -> toggleSubmenu(binding.navigationView.navImportExportSubmenu, binding.navigationView.navImportExportChevron));
        
        binding.navigationView.navCreateBackup.setOnClickListener(v -> { if (hasStoragePermission(PERMISSION_EXPORT)) LocalBackup.startExport(LocalBackup.ExportType.MANUAL); binding.drawerLayout.closeDrawers(); });
        binding.navigationView.navRestoreBackup.setOnClickListener(v -> { if (hasStoragePermission(PERMISSION_IMPORT)) LocalImport.startImport(this); binding.drawerLayout.closeDrawers(); });
        binding.navigationView.navShareExport.setOnClickListener(v -> { LocalBackup.startExport(LocalBackup.ExportType.SHARE); binding.drawerLayout.closeDrawers(); });
        binding.navigationView.navImportFile.setOnClickListener(v -> { binding.drawerLayout.closeDrawers(); importFileLauncher.launch(new String[]{"text/*", "application/octet-stream"}); });

        binding.previousMonth.setOnClickListener(v -> { new Events().send(new ButtonClicked("previousMonth")); binding.viewPager.setCurrentItem(binding.viewPager.getCurrentItem() - 1, true); });
        binding.nextMonth.setOnClickListener(v -> { new Events().send(new ButtonClicked("nextMonth")); binding.viewPager.setCurrentItem(binding.viewPager.getCurrentItem() + 1, true); });
        
        binding.mainFab.setOnClickListener(v -> { if (assetSheetManager.isVisible()) assetSheetManager.close(); else toggleFab(); });
        binding.fabAsset.setOnClickListener(v -> { collapseFab(); assetSheetManager.open(null, null); });
        binding.fabNote.setOnClickListener(v -> { collapseFab(); startActivity(new Intent(this, CommentActivity.class).putExtra(AssetFields.MONTH, month.getMonth()).putExtra(AssetFields.YEAR, month.getYear())); });
        binding.fabScrim.setOnClickListener(v -> { if (assetSheetManager.isVisible()) assetSheetManager.close(); else collapseFab(); });
    }

    private void toggleSubmenu(View submenu, View chevron) {
        boolean open = submenu.getVisibility() == View.VISIBLE;
        submenu.setVisibility(open ? View.GONE : View.VISIBLE);
        chevron.setRotation(open ? 0f : 90f);
    }

    @Override public void onSyncStarted() { showSyncing(true); }
    @Override public void onSyncFinished() { showSyncing(false); refreshAllLoadedPages(); }
    @Override public void onLoginStateChanged(boolean signedIn) { updateLoginUI(); if (signedIn) syncManager.triggerSync(); }
    @Override public void onDataChanged() { refreshAllLoadedPages(); }
    @Override public Month getCurrentMonth() { return month; }
    @Override public void onOpenDrawer() { binding.drawerLayout.openDrawer(GravityCompat.START); }
    @Override public void onRequestSync() { syncManager.triggerSync(); }

    public void onAssetViewClosed() {
        binding.mainFab.show();
        binding.fabScrim.setVisibility(View.GONE);
        if (fabExpanded) collapseFab();
    }

    public void showScrim(float alpha) {
        binding.fabScrim.setVisibility(alpha > 0 ? View.VISIBLE : View.GONE);
        binding.fabScrim.setAlpha(alpha);
    }

    public void checkAutoBackup() {
        if (!Prefs.getBoolean(Prefs.PREFS_AUTO_IMPORT_PERMISSION, false) && hasStoragePermission(PERMISSION_EXPORT_AUTO))
            LocalBackup.startExport(LocalBackup.ExportType.AUTO);
    }

    private void updateLoginUI() {
        boolean signedIn = Prefs.contains(Prefs.PREFS_TOKEN);
        binding.navigationView.googleSignIn.setVisibility(signedIn ? View.GONE : View.VISIBLE);
        binding.navigationView.signedInLayout.setVisibility(signedIn ? View.VISIBLE : View.GONE);
        if (signedIn) {
            String email = Prefs.getString(Prefs.PREFS_USER_EMAIL, "");
            binding.navigationView.userEmail.setText(email);
            binding.navigationView.drawerAvatarText.setText(!email.isEmpty() ? email.substring(0, 1).toUpperCase() : "W");
            syncManager.triggerSync();
        }
    }

    private void refreshAllLoadedPages() {
        EventBus.getDefault().post(new DataChangedEvent());
        updateGoalProgress();
    }

    @Override public void onEditAsset(Asset item) { assetSheetManager.open(item.getName(), item.getValue()); }
    @Override public void onLongPressAsset(Asset item) {
        new AlertDialog.Builder(this).setTitle(R.string.menu).setItems(new String[]{getString(R.string.menu_view_trend), getString(R.string.menu_delete)}, (d, w) -> {
            if (w == 0) startActivity(new Intent(this, SingleAssetTrendActivity.class).putExtra(SingleAssetTrendActivity.EXTRA_ASSET_NAME, item.getName()));
            else if (w == 1) {
                new Events().send(new AssetDeleted(item));
                try (Realm realm = Realm.getDefaultInstance()) { realm.executeTransaction(r -> { Asset t = r.where(Asset.class).equalTo(AssetFields.ID, item.getId()).findFirst(); if (t != null) t.deleteFromRealm(); }); }
                refreshAllLoadedPages();
            }
        }).show();
    }

    @Override public void onMonthReady(Month m) {
        int[] cur = MonthPagerAdapter.monthYearAt(binding.viewPager.getCurrentItem());
        if (m.getMonth() != cur[0] || m.getYear() != cur[1]) return;
        month = m;
        binding.monthName.setText(m.toString());
        updateMonthNameColor();
        updateGoalProgress();
        if (binding.viewPager.getOffscreenPageLimit() == ViewPager2.OFFSCREEN_PAGE_LIMIT_DEFAULT) binding.viewPager.setOffscreenPageLimit(2);
    }

    private void toggleFab() { if (fabExpanded) collapseFab(); else expandFab(); }
    private void expandFab() {
        fabExpanded = true;
        binding.mainFab.animate().rotation(45f).setDuration(200).start();
        binding.mainFab.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#1F2937")));
        showScrim(1f);
        showFabItem(binding.fabAssetContainer, 0);
        showFabItem(binding.fabNoteContainer, 54);
    }
    private void collapseFab() {
        if (!fabExpanded) return;
        fabExpanded = false;
        binding.mainFab.animate().rotation(0f).setDuration(200).start();
        binding.mainFab.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, Tools.getAccentColor())));
        binding.fabScrim.animate().alpha(0f).setDuration(200).withEndAction(() -> binding.fabScrim.setVisibility(View.GONE)).start();
        hideFabItem(binding.fabAssetContainer);
        hideFabItem(binding.fabNoteContainer);
    }
    private void showFabItem(ViewGroup c, long d) { c.setVisibility(View.VISIBLE); c.setAlpha(0f); c.setTranslationY(60f); c.animate().alpha(1f).translationY(0f).setStartDelay(d).setDuration(200).start(); }
    private void hideFabItem(ViewGroup c) { c.animate().alpha(0f).translationY(60f).setDuration(150).withEndAction(() -> c.setVisibility(View.GONE)).start(); }

    private void setupBackHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) binding.drawerLayout.closeDrawer(GravityCompat.START);
                else if (fabExpanded) collapseFab();
                else if (assetSheetManager.isVisible()) assetSheetManager.close();
                else { setEnabled(false); getOnBackPressedDispatcher().onBackPressed(); }
            }
        });
    }

    private boolean goToCurrentMonth() {
        Calendar now = Calendar.getInstance();
        binding.viewPager.setCurrentItem(MonthPagerAdapter.positionOf(now.get(Calendar.MONTH) + 1, now.get(Calendar.YEAR)), true);
        return true;
    }

    private boolean hasStoragePermission(int requestCode) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) return true;
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == 0) return true;
        ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, requestCode);
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != 0) {
            Toast.makeText(this, getString(R.string.backup_permission), Toast.LENGTH_SHORT).show();
            new Events().send(new PermissionNotGranted(requestCode));
            return;
        }
        if (requestCode == PERMISSION_EXPORT) LocalBackup.startExport(LocalBackup.ExportType.MANUAL);
        else if (requestCode == PERMISSION_IMPORT) LocalImport.startImport(this);
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null) {
            try { LocalImport.importFromUri(intent.getData()); } catch (Exception e) { Toast.makeText(this, R.string.import_no_backups, Toast.LENGTH_SHORT).show(); }
        }
    }

    private void showSyncing(boolean syncing) {
        if (syncing) {
            int from = ContextCompat.getColor(this, R.color.divider), to = ContextCompat.getColor(this, Tools.getAccentColor());
            syncAnim = ObjectAnimator.ofArgb(binding.bottomAppBarCard, "strokeColor", from, to);
            syncAnim.setDuration(700); syncAnim.setRepeatMode(ObjectAnimator.REVERSE); syncAnim.setRepeatCount(ObjectAnimator.INFINITE);
            syncAnim.setInterpolator(new AccelerateDecelerateInterpolator()); syncAnim.start();
        } else { if (syncAnim != null) { syncAnim.cancel(); syncAnim = null; } binding.bottomAppBarCard.setStrokeColor(ContextCompat.getColor(this, R.color.divider)); }
    }

    private void applyAccentColor() {
        int accent = ContextCompat.getColor(this, Tools.getAccentColor());
        ColorStateList list = ColorStateList.valueOf(accent);
        binding.previousMonth.setImageTintList(list); binding.nextMonth.setImageTintList(list);
        if (!fabExpanded) binding.mainFab.setBackgroundTintList(list);
        binding.navigationView.drawerGoal1yRow.goalPercent.setTextColor(accent); binding.navigationView.drawerGoal1yRow.goalProgress.setProgressTintList(list);
        binding.navigationView.drawerGoal3yRow.goalPercent.setTextColor(accent); binding.navigationView.drawerGoal3yRow.goalProgress.setProgressTintList(list);
        binding.navigationView.drawerGoal5yRow.goalPercent.setTextColor(accent); binding.navigationView.drawerGoal5yRow.goalProgress.setProgressTintList(list);
        binding.navigationView.drawerAvatarContainer.setBackgroundTintList(list); binding.navigationView.signOut.setImageTintList(list);
        applyAccentToDrawerHeaders(binding.navigationView.navigation, accent);
        updateMonthNameColor();
    }

    private void updateMonthNameColor() {
        Calendar today = Calendar.getInstance();
        boolean isCurrent = (month.getMonth() == (today.get(Calendar.MONTH) + 1)) && (month.getYear() == today.get(Calendar.YEAR));
        binding.monthName.setTextColor(ContextCompat.getColor(this, isCurrent ? Tools.getAccentColor() : R.color.text));
    }

    private void applyAccentToDrawerHeaders(ViewGroup group, int color) {
        for (int i = 0; i < group.getChildCount(); i++) {
            View v = group.getChildAt(i);
            if (v instanceof TextView && ((TextView) v).getLetterSpacing() >= 0.09f) ((TextView) v).setTextColor(color);
            else if (v instanceof ViewGroup) applyAccentToDrawerHeaders((ViewGroup) v, color);
        }
    }

    private void updateGoalProgress() {
        executorService.execute(() -> {
            try (Realm realm = Realm.getDefaultInstance()) {
                List<Goal> goals = realm.copyFromRealm(realm.where(Goal.class)
                        .sort("targetYear", Sort.ASCENDING, "targetMonth", Sort.ASCENDING)
                        .findAll());
                double nw = month.getValue(realm);
                runOnUiThread(() -> {
                    if (isFinishing()) return;
                    boolean noGoals = goals.isEmpty();
                    binding.navigationView.drawerHeader.setVisibility(noGoals ? View.GONE : View.VISIBLE);
                    binding.navigationView.drawerHeaderBottomDivider.setVisibility(noGoals ? View.GONE : View.VISIBLE);
                    binding.navigationView.drawerGoalSection.setVisibility(noGoals ? View.GONE : View.VISIBLE);
                    binding.navigationView.navSetGoal.setVisibility(noGoals ? View.VISIBLE : View.GONE);
                    bindGoalRow(binding.navigationView.drawerGoal1yRow.getRoot(), binding.navigationView.drawerGoal1yRow.goalLabel, binding.navigationView.drawerGoal1yRow.goalPercent, binding.navigationView.drawerGoal1yRow.goalProgress, goals.size() > 0 ? goals.get(0) : null, nw);
                    bindGoalRow(binding.navigationView.drawerGoal3yRow.getRoot(), binding.navigationView.drawerGoal3yRow.goalLabel, binding.navigationView.drawerGoal3yRow.goalPercent, binding.navigationView.drawerGoal3yRow.goalProgress, goals.size() > 1 ? goals.get(1) : null, nw);
                    bindGoalRow(binding.navigationView.drawerGoal5yRow.getRoot(), binding.navigationView.drawerGoal5yRow.goalLabel, binding.navigationView.drawerGoal5yRow.goalPercent, binding.navigationView.drawerGoal5yRow.goalProgress, goals.size() > 2 ? goals.get(2) : null, nw);
                });
            }
        });
    }

    private void bindGoalRow(View row, TextView label, TextView percent, android.widget.ProgressBar bar, Goal goal, double current) {
        if (goal == null || goal.getTargetValue() <= 0) { row.setVisibility(View.GONE); return; }
        row.setVisibility(View.VISIBLE);
        label.setText(String.format(Locale.getDefault(), "%s %d GOAL", Tools.getMonthName(goal.getTargetMonth()), goal.getTargetYear()));
        int progress = (int) Math.min(Math.max(current / goal.getTargetValue() * 100, 0), 100);
        percent.setText(progress + "%"); bar.setProgress(progress);
    }

    private void showMonthYearPicker() {
        String[] monthNames = Arrays.copyOf(new DateFormatSymbols().getMonths(), 12);
        NumberPicker mp = new NumberPicker(this); mp.setMinValue(0); mp.setMaxValue(11); mp.setDisplayedValues(monthNames); mp.setValue(month.getMonth() - 1); mp.setWrapSelectorWheel(true);
        NumberPicker yp = new NumberPicker(this); yp.setMinValue(1970); yp.setMaxValue(Calendar.getInstance().get(Calendar.YEAR) + 5); yp.setValue(month.getYear()); yp.setWrapSelectorWheel(false);
        LinearLayout r = new LinearLayout(this); r.setOrientation(LinearLayout.HORIZONTAL); r.setGravity(Gravity.CENTER); int p = (int) (24 * getResources().getDisplayMetrics().density); r.setPadding(p, p / 2, p, 0);
        r.addView(mp, new LinearLayout.LayoutParams(0, -2, 1f)); r.addView(yp, new LinearLayout.LayoutParams(0, -2, 1f));
        AlertDialog d = new AlertDialog.Builder(this).setTitle("Go to month").setView(r).setPositiveButton("Go", (dialog, w) -> { binding.viewPager.setCurrentItem(MonthPagerAdapter.positionOf(mp.getValue() + 1, yp.getValue()), false); assetSheetManager.close(); }).setNegativeButton("Cancel", null).create();
        Tools.styleDialog(d); d.show();
    }

    @Override protected void onStart() { super.onStart(); if (!EventBus.getDefault().isRegistered(this)) EventBus.getDefault().register(this); }
    @Override protected void onPause() { 
        super.onPause(); 
        if (assetSheetManager != null) {
            assetSheetManager.clearFocus();
        }
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && binding.getRoot().getWindowToken() != null) {
            imm.hideSoftInputFromWindow(binding.getRoot().getWindowToken(), 0);
        }
        EventBus.getDefault().unregister(this); 
    }
    @Override protected void onDestroy() { super.onDestroy(); executorService.shutdown(); syncManager.shutdown(); }
    @Override protected void onResume() { 
        super.onResume(); 
        applyAccentColor(); 
        
        // Ensure keyboard is hidden if sheet is not visible
        if (assetSheetManager != null && !assetSheetManager.isVisible()) {
            assetSheetManager.clearFocus();
            binding.mainFab.postDelayed(() -> {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null && binding.getRoot().getWindowToken() != null) {
                    imm.hideSoftInputFromWindow(binding.getRoot().getWindowToken(), 0);
                }
            }, 100);
        }

        if (firstResume) { firstResume = false; return; } 
        refreshAllLoadedPages(); 
    }

    @Subscribe(threadMode = ThreadMode.MAIN) public void onMessageEvent(MessageEvent e) { Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show(); }
    @Subscribe(threadMode = ThreadMode.MAIN) public void onImportedEvent(ImportedEvent e) {
        if (e.getAssets().isEmpty() && e.getNotes().isEmpty() && e.getGoals().isEmpty()) { Toast.makeText(this, R.string.import_empty, Toast.LENGTH_SHORT).show(); return; }
        String title = String.format(getString(R.string.import_confirmation), e.getAssets().size() + e.getNotes().size() + e.getGoals().size());
        AlertDialog d = new AlertDialog.Builder(this).setTitle(title).setPositiveButton(R.string.backup_share_yes, (dialog, w) -> {
            try (Realm realm = Realm.getDefaultInstance()) { realm.executeTransaction(r -> { r.delete(Asset.class); r.delete(Note.class); r.delete(Goal.class); r.copyToRealmOrUpdate(e.getAssets()); r.copyToRealmOrUpdate(e.getNotes()); r.copyToRealmOrUpdate(e.getGoals()); }); }
            refreshAllLoadedPages(); updateGoalProgress(); Toast.makeText(this, R.string.import_success, Toast.LENGTH_SHORT).show();
        }).setNegativeButton(R.string.backup_share_no, null).create();
        Tools.styleDialog(d); d.show();
    }
    @Subscribe(threadMode = ThreadMode.MAIN) public void onBackupSavedEvent(BackupSavedEvent e) { if (e.isShareImmediately()) shareFile(e.getFile()); else Toast.makeText(this, R.string.backup_saved, Toast.LENGTH_SHORT).show(); }
    private void shareFile(java.io.File f) { Intent i = new Intent(Intent.ACTION_SEND); Uri u = FileProvider.getUriForFile(this, getPackageName() + ".provider", f); i.setType("application/octet-stream"); i.putExtra(Intent.EXTRA_STREAM, u); i.putExtra(Intent.EXTRA_SUBJECT, f.getName()); i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION); startActivity(Intent.createChooser(i, getString(R.string.backup_share_title))); }
}
