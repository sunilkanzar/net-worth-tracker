package com.kanzar.networthtracker;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import androidx.viewpager2.widget.ViewPager2;
import android.animation.ObjectAnimator;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import com.kanzar.networthtracker.databinding.ActivityMainBinding;
import java.text.DateFormatSymbols;
import java.util.Arrays;
import java.util.Calendar;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.IntentSenderRequest;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.view.GravityCompat;
import androidx.credentials.ClearCredentialStateRequest;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.ClearCredentialException;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.gms.auth.api.identity.AuthorizationRequest;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.common.api.Scope;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.kanzar.networthtracker.api.repositories.DriveServiceHelper;

import java.util.Collections;
import java.util.Locale;

import com.google.android.gms.tasks.Tasks;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.kanzar.networthtracker.adapters.AssetAdapter;
import com.kanzar.networthtracker.backup.LocalBackup;
import com.kanzar.networthtracker.backup.LocalImport;
import com.kanzar.networthtracker.eventbus.BackupSavedEvent;
import com.kanzar.networthtracker.eventbus.ImportedEvent;
import com.kanzar.networthtracker.eventbus.MessageEvent;
import com.kanzar.networthtracker.helpers.Month;
import com.kanzar.networthtracker.helpers.Prefs;
import com.kanzar.networthtracker.helpers.Tools;
import com.kanzar.networthtracker.models.Asset;
import com.kanzar.networthtracker.models.AssetFields;
import com.kanzar.networthtracker.models.Goal;
import com.kanzar.networthtracker.models.Note;
import com.kanzar.networthtracker.statistics.Events;
import com.kanzar.networthtracker.statistics.events.AssetAdded;
import com.kanzar.networthtracker.statistics.events.AssetDeleted;
import com.kanzar.networthtracker.statistics.events.ButtonClicked;
import com.kanzar.networthtracker.statistics.events.PermissionNotGranted;
import com.kanzar.networthtracker.statistics.events.SigninClicked;
import com.kanzar.networthtracker.statistics.events.SigninCompleted;
import com.kanzar.networthtracker.statistics.events.SignoutClicked;
import com.kanzar.networthtracker.views.PercentView;
import com.kanzar.networthtracker.widget.OverviewWidget;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.realm.Case;
import io.realm.Realm;
import io.realm.Sort;

public class MainActivity extends AppCompatActivity implements MonthPageFragment.Listener {

    private static final int PERMISSION_EXPORT = 1;
    private static final int PERMISSION_EXPORT_AUTO = 2;
    private static final int PERMISSION_IMPORT = 3;
    private static final int PERMISSION_IMPORT_CSS = 4;

    private Month month = new Month();
    private CredentialManager credentialManager;
    private DriveServiceHelper driveServiceHelper;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private ActivityMainBinding binding;
    private ObjectAnimator syncAnim;
    private BottomSheetBehavior<View> assetSheetBehavior;
    private boolean updatingForm = false;
    private boolean firstResume = true;

    // Speed-dial FAB
    private boolean fabExpanded = false;

    // Launcher for Drive scope consent screen
    private final androidx.activity.result.ActivityResultLauncher<IntentSenderRequest> authorizationLauncher =
        registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult(), result -> {
            if (result.getResultCode() == RESULT_OK) {
                // User granted scope — re-run authorize() to get access token
                requestDriveAuthorizationAndSync(false);
            }
        });

    // Launcher for picking any CSV file from device storage
    private final androidx.activity.result.ActivityResultLauncher<String[]> importFileLauncher =
        registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.OpenDocument(), uri -> {
            if (uri != null) {
                try {
                    LocalImport.importFromUri(uri);
                } catch (Exception e) {
                    Toast.makeText(this, R.string.import_empty, Toast.LENGTH_SHORT).show();
                }
            }
        });

    // Launcher for MonthActivity month/year picker
    private final androidx.activity.result.ActivityResultLauncher<Intent> monthPickerLauncher =
        registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Intent data = result.getData();
                int m = data.getIntExtra("month", month.getMonth());
                int y = data.getIntExtra("year", month.getYear());
                binding.viewPager.setCurrentItem(MonthPagerAdapter.positionOf(m, y), false);
            }
        });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setupBottomSheet();
        setupViewPager();
        credentialManager = CredentialManager.create(this);
        binding.drawerLayout.addDrawerListener(new androidx.drawerlayout.widget.DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerClosed(View drawerView) {
                // Collapse submenus so they start closed next time
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
        viewListeners();
        handleIntent(getIntent());
        checkLogin();
        setupBackHandler();
    }

    private    void setupViewPager() {
        MonthPagerAdapter pagerAdapter = new MonthPagerAdapter(this);
        binding.viewPager.setAdapter(pagerAdapter);
        binding.viewPager.setOffscreenPageLimit(1); // Reduce initial load
        int startPos = MonthPagerAdapter.positionOf(month.getMonth(), month.getYear());
        binding.viewPager.setCurrentItem(startPos, false);
        binding.viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                int[] my = MonthPagerAdapter.monthYearAt(position);
                month = new Month(my[0], my[1], false); // Don't calculate on UI thread
                binding.monthName.setText(month.toString());
                updateMonthNameColor();
                closeAssetView();
            }
        });
    }

    private void setupBottomSheet() {
        assetSheetBehavior = BottomSheetBehavior.from(binding.newAssetSheet.newAssetLayout);
        assetSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        assetSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    onAssetViewClosed();
                } else if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    binding.fabScrim.setVisibility(View.VISIBLE);
                    binding.fabScrim.setAlpha(1f);
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                // slideOffset is -1 for hidden, 1 for expanded (when peekHeight is 0)
                float alpha = (slideOffset + 1f); 
                if (alpha > 0) {
                    binding.fabScrim.setVisibility(View.VISIBLE);
                    binding.fabScrim.setAlpha(Math.min(1f, alpha));
                } else {
                    binding.fabScrim.setVisibility(View.GONE);
                }
            }
        });
    }

    private void onAssetViewClosed() {
        binding.mainFab.show();
        binding.fabScrim.setVisibility(View.GONE);
        if (fabExpanded) {
            collapseFab();
        }
    }

    private void refreshAllLoadedPages() {
        int current = binding.viewPager.getCurrentItem();
        int range = 2; // Refresh 5 months (current + 2 before + 2 after)
        for (int i = current - range; i <= current + range; i++) {
            if (i < 0 || i >= MonthPagerAdapter.PAGE_COUNT) continue;
            MonthPageFragment f = (MonthPageFragment) getSupportFragmentManager()
                    .findFragmentByTag("f" + i);
            if (f != null) f.refresh(false);
        }
        updateGoalProgress();
        updateWidget();
    }

    private void viewListeners() {
        binding.monthName.setOnClickListener(v -> showMonthYearPicker());
        binding.monthName.setOnLongClickListener(v -> goToCurrentMonth());

        binding.navigationView.googleSignIn.setOnClickListener(v -> {
            new Events().send(new SigninClicked());
            startSignIn();
        });

        binding.navigationView.signOut.setOnClickListener(v -> {
            new Events().send(new SignoutClicked());
            signOut();
        });

        binding.navigationView.navPreferences.setOnClickListener(v -> {
            binding.drawerLayout.closeDrawers();
            startActivity(new Intent(this, PreferencesActivity.class));
        });

        binding.navigationView.navMonthly.setOnClickListener(v -> {
            new Events().send(new ButtonClicked("monthView"));
            monthPickerLauncher.launch(new Intent(this, MonthActivity.class));
            binding.drawerLayout.closeDrawers();
        });

        binding.navigationView.navGraph.setOnClickListener(v -> {
            new Events().send(new ButtonClicked("chartView"));
            startActivity(new Intent(this, ChartActivity.class));
            binding.drawerLayout.closeDrawers();
        });

        binding.navigationView.navAllocation.setOnClickListener(v -> {
            Intent intent = new Intent(this, AllocationActivity.class);
            intent.putExtra("month", month.getMonth());
            intent.putExtra("year", month.getYear());
            startActivity(intent);
            binding.drawerLayout.closeDrawers();
        });

        binding.navigationView.navAssetTrend.setOnClickListener(v -> {
            startActivity(new Intent(this, AssetTrendActivity.class));
            binding.drawerLayout.closeDrawers();
        });

        binding.navigationView.navBackup.setOnClickListener(v -> {
            boolean open = binding.navigationView.navBackupSubmenu.getVisibility() == View.VISIBLE;
            binding.navigationView.navBackupSubmenu.setVisibility(open ? View.GONE : View.VISIBLE);
            binding.navigationView.navBackupChevron.setRotation(open ? 0f : 90f);
        });

        binding.navigationView.navCreateBackup.setOnClickListener(v -> {
            if (hasStoragePermission(PERMISSION_EXPORT)) {
                LocalBackup.startExport(LocalBackup.ExportType.MANUAL);
            }
            binding.drawerLayout.closeDrawers();
        });

        binding.navigationView.navRestoreBackup.setOnClickListener(v -> {
            if (hasStoragePermission(PERMISSION_IMPORT)) {
                LocalImport.startImport(this);
            }
            binding.drawerLayout.closeDrawers();
        });

        binding.navigationView.navImportExport.setOnClickListener(v -> {
            boolean open = binding.navigationView.navImportExportSubmenu.getVisibility() == View.VISIBLE;
            binding.navigationView.navImportExportSubmenu.setVisibility(open ? View.GONE : View.VISIBLE);
            binding.navigationView.navImportExportChevron.setRotation(open ? 0f : 90f);
        });

        binding.navigationView.navShareExport.setOnClickListener(v -> {
            LocalBackup.startExport(LocalBackup.ExportType.SHARE);
            binding.drawerLayout.closeDrawers();
        });

        binding.navigationView.navImportFile.setOnClickListener(v -> {
            binding.drawerLayout.closeDrawers();
            importFileLauncher.launch(new String[]{"text/*", "application/octet-stream"});
        });

        binding.previousMonth.setOnClickListener(v -> {
            new Events().send(new ButtonClicked("previousMonth"));
            binding.viewPager.setCurrentItem(binding.viewPager.getCurrentItem() - 1, true);
        });
        binding.previousMonth.setOnLongClickListener(v -> goToCurrentMonth());

        binding.nextMonth.setOnClickListener(v -> {
            new Events().send(new ButtonClicked("nextMonth"));
            binding.viewPager.setCurrentItem(binding.viewPager.getCurrentItem() + 1, true);
        });
        binding.nextMonth.setOnLongClickListener(v -> goToCurrentMonth());

        binding.newAssetSheet.newAssetName.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() >= 2) {
                    String[] assetNames = getAssetNames(s.toString());
                    binding.newAssetSheet.newAssetName.setAdapter(new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_dropdown_item_1line, assetNames));
                }
                if (s.length() > 0) {
                    binding.newAssetSheet.assetInitial.setText(s.toString().substring(0, 1).toUpperCase());
                } else {
                    binding.newAssetSheet.assetInitial.setText("+");
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        binding.newAssetSheet.newAssetChange.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (updatingForm) return;
                
                String input = s.toString();
                if (input.equals("-")) {
                    updateSign(false);
                    return;
                }
                
                double change = toDouble(binding.newAssetSheet.newAssetChange);
                if (input.startsWith("-") || change < 0) {
                    updateSign(false);
                } else if (input.length() > 0 && change > 0) {
                    updateSign(true);
                }
                
                Asset asset = getNewAsset();
                Asset previous = asset.getPrevious();
                double prevValue = (previous != null) ? previous.getValue() : 0.0;
                
                // Effective change is now exactly what's in the box
                double effectiveChange = change;
                
                BigDecimal newValue = BigDecimal.valueOf(prevValue).add(BigDecimal.valueOf(effectiveChange));
                if (toDouble(binding.newAssetSheet.newAssetValue) != newValue.doubleValue()) {
                    updatingForm = true;
                    binding.newAssetSheet.newAssetValue.setText(formatStringValue(newValue.doubleValue()));
                    binding.newAssetSheet.newAssetValueFormatted.setText(formatString(newValue.doubleValue()));
                    updatingForm = false;
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        binding.newAssetSheet.newAssetValue.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (updatingForm) return;
                String input = s.toString();
                if (input.equals("-")) return;
                double val = toDouble(binding.newAssetSheet.newAssetValue);
                binding.newAssetSheet.newAssetValueFormatted.setText(formatString(val));
                Asset asset = getNewAsset();
                Asset previous = asset.getPrevious();
                double prevValue = (previous != null) ? previous.getValue() : 0.0;
                BigDecimal change = BigDecimal.valueOf(val).subtract(BigDecimal.valueOf(prevValue));
                
                if (toDouble(binding.newAssetSheet.newAssetChange) != change.doubleValue()) {
                    updatingForm = true;
                    binding.newAssetSheet.newAssetChange.setText(formatStringValue(change.doubleValue()));
                    updateSign(change.doubleValue() >= 0);
                    updatingForm = false;
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        binding.newAssetSheet.btnPlus.setOnClickListener(v -> {
            updateSign(true);
            String s = binding.newAssetSheet.newAssetChange.getText().toString();
            if (s.startsWith("-")) {
                binding.newAssetSheet.newAssetChange.setText(s.substring(1));
            }
        });

        binding.newAssetSheet.btnMinus.setOnClickListener(v -> {
            updateSign(false);
            String s = binding.newAssetSheet.newAssetChange.getText().toString();
            if (!s.startsWith("-")) {
                binding.newAssetSheet.newAssetChange.setText("-" + s);
            }
        });

        binding.newAssetSheet.pillSame.setOnClickListener(v -> {
            updatingForm = true;
            binding.newAssetSheet.newAssetChange.setText("0");
            updateSign(true);
            Asset asset = getNewAsset();
            Asset previous = asset.getPrevious();
            double prevValue = (previous != null) ? previous.getValue() : 0.0;
            binding.newAssetSheet.newAssetValue.setText(formatStringValue(prevValue));
            binding.newAssetSheet.newAssetValueFormatted.setText(formatString(prevValue));
            updatingForm = false;
        });

        binding.newAssetSheet.pillPlus5.setOnClickListener(v -> {
            Asset asset = getNewAsset();
            Asset previous = asset.getPrevious();
            double prevValue = (previous != null) ? previous.getValue() : 0.0;
            double change = Math.round(prevValue * 0.05 * 100.0) / 100.0;
            updatingForm = true;
            binding.newAssetSheet.newAssetChange.setText(formatStringValue(change));
            updateSign(true);
            double newValue = prevValue + change;
            binding.newAssetSheet.newAssetValue.setText(formatStringValue(newValue));
            binding.newAssetSheet.newAssetValueFormatted.setText(formatString(newValue));
            updatingForm = false;
        });

        binding.newAssetSheet.pillMinus5.setOnClickListener(v -> {
            Asset asset = getNewAsset();
            Asset previous = asset.getPrevious();
            double prevValue = (previous != null) ? previous.getValue() : 0.0;
            double change = -Math.round(prevValue * 0.05 * 100.0) / 100.0;
            updatingForm = true;
            binding.newAssetSheet.newAssetChange.setText(formatStringValue(change));
            updateSign(false);
            double newValue = prevValue + change;
            binding.newAssetSheet.newAssetValue.setText(formatStringValue(newValue));
            binding.newAssetSheet.newAssetValueFormatted.setText(formatString(newValue));
            updatingForm = false;
        });

        binding.newAssetSheet.pillPurchase.setOnClickListener(v -> showActionDialog("Purchase", true));
        binding.newAssetSheet.pillSell.setOnClickListener(v -> showActionDialog("Sell", false));
        binding.newAssetSheet.pillProfit.setOnClickListener(v -> showActionDialog("Profit Booking", true));
        binding.newAssetSheet.pillLoss.setOnClickListener(v -> showActionDialog("Loss Harvest", false));
        binding.newAssetSheet.pillReinvest.setOnClickListener(v -> showActionDialog("Reinvest", true));
        binding.newAssetSheet.pillChurning.setOnClickListener(v -> showActionDialog("Portfolio Churning", true));

        // Main FAB: toggle speed-dial
        binding.mainFab.setOnClickListener(v -> {
            new Events().send(new ButtonClicked("newAssetSave"));
            if (assetSheetBehavior.getState() != BottomSheetBehavior.STATE_HIDDEN) {
                closeAssetView();
                return;
            }
            toggleFab();
        });

        // Speed-dial: Add Asset
        binding.fabAsset.setOnClickListener(v -> {
            collapseFab();
            openAssetView(null, null);
        });

        // Speed-dial: Monthly Note
        binding.fabNote.setOnClickListener(v -> {
            collapseFab();
            toComment(null);
        });

        // Scrim: dismiss on tap outside
        binding.fabScrim.setOnClickListener(v -> {
            if (assetSheetBehavior.getState() != BottomSheetBehavior.STATE_HIDDEN) {
                closeAssetView();
            } else {
                collapseFab();
            }
        });

        binding.newAssetSheet.btnSaveAsset.setOnClickListener(v -> saveAsset());

        binding.newAssetSheet.btnCloseSheet.setOnClickListener(v -> closeAssetView());

        binding.navigationView.navSetGoal.setOnClickListener(v -> {
            Intent goalsIntent = new Intent(this, GoalActivity.class);
            goalsIntent.putExtra("current_net_worth", month.getValue());
            startActivity(goalsIntent);
            binding.drawerLayout.closeDrawers();
        });
    }

    // ── Authentication ──────────────────────────────────────────────────────

    private void startSignIn() {
        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(getString(R.string.server_client_id))
                .setAutoSelectEnabled(false)
                .build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        credentialManager.getCredentialAsync(
                this, request, new android.os.CancellationSignal(), executorService,
                new CredentialManagerCallback<>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        handleSignInCredential(result.getCredential());
                    }
                    @Override
                    public void onError(@NonNull GetCredentialException e) {
                        Log.e("MainActivity", "Sign in failed", e);
                        runOnUiThread(() ->
                            Toast.makeText(MainActivity.this, "Sign in failed. Please try again.", Toast.LENGTH_SHORT).show());
                    }
                });
    }

    private void handleSignInCredential(androidx.credentials.Credential credential) {
        GoogleIdTokenCredential googleCred = null;
        if (credential instanceof GoogleIdTokenCredential) {
            googleCred = (GoogleIdTokenCredential) credential;
        } else if (credential instanceof CustomCredential) {
            CustomCredential custom = (CustomCredential) credential;
            if (GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(custom.getType())) {
                try {
                    googleCred = GoogleIdTokenCredential.createFrom(custom.getData());
                } catch (Exception e) {
                    Log.e("MainActivity", "Failed to parse Google credential", e);
                }
            }
        }

        if (googleCred == null) {
            Log.w("MainActivity", "Unrecognized credential type: " + credential.getClass().getName());
            return;
        }

        String email = googleCred.getId();
        Prefs.save(Prefs.PREFS_USER_EMAIL, email);
        Prefs.save(Prefs.PREFS_TOKEN, "signed_in");
        new Events().send(new SigninCompleted());
        runOnUiThread(() -> {
            checkLogin();
            Toast.makeText(this, "Signed in as " + email, Toast.LENGTH_SHORT).show();
        });
        requestDriveAuthorizationAndSync(false);
    }

    private void requestDriveAuthorizationAndSync(boolean showToast) {
        AuthorizationRequest authRequest = AuthorizationRequest.builder()
                .setRequestedScopes(Collections.singletonList(new Scope(DriveScopes.DRIVE_APPDATA)))
                .build();

        Identity.getAuthorizationClient(this)
                .authorize(authRequest)
                .addOnSuccessListener(authResult -> {
                    if (authResult.hasResolution()) {
                        // Show Drive consent screen
                        try {
                            android.app.PendingIntent pi = authResult.getPendingIntent();
                            if (pi != null) {
                                authorizationLauncher.launch(
                                    new IntentSenderRequest.Builder(pi.getIntentSender()).build());
                            }
                        } catch (Exception e) {
                            Log.e("MainActivity", "Auth resolution failed", e);
                        }
                    } else {
                        String accessToken = authResult.getAccessToken();
                        if (accessToken != null) {
                            initDriveServiceAndSync(accessToken);
                        }
                    }
                })
                .addOnFailureListener(e ->
                    Log.e("MainActivity", "Drive authorization failed", e));
    }

    private void initDriveServiceAndSync(String accessToken) {
        try {
            Drive drive = new Drive.Builder(
                    new NetHttpTransport(),
                    new GsonFactory(),
                    request -> request.getHeaders().setAuthorization("Bearer " + accessToken))
                    .setApplicationName("Net Worth Tracker")
                    .build();

            driveServiceHelper = new DriveServiceHelper(drive);
            performSync();
        } catch (Exception e) {
            Log.e("MainActivity", "Drive init failed", e);
        }
    }

    private void signOut() {
        Prefs.delete(Prefs.PREFS_TOKEN);
        Prefs.delete(Prefs.PREFS_USER_EMAIL);
        Prefs.delete(Prefs.PREFS_LAST_SYNC);
        driveServiceHelper = null;

        credentialManager.clearCredentialStateAsync(
                new ClearCredentialStateRequest(),
                new android.os.CancellationSignal(),
                executorService,
                new CredentialManagerCallback<>() {
                    @Override public void onResult(Void result) {
                        runOnUiThread(() -> onSignedOut());
                    }
                    @Override public void onError(@NonNull ClearCredentialException e) {
                        runOnUiThread(() -> onSignedOut());
                    }
                });
    }

    // ── Sync ─────────────────────────────────────────────────────────────────

    private void triggerSync() {
        if (!isSignedIn()) {
            return;
        }

        showSyncing(true);

        // Get a fresh access token (returns cached token if still valid)
        AuthorizationRequest authRequest = AuthorizationRequest.builder()
                .setRequestedScopes(Collections.singletonList(new Scope(DriveScopes.DRIVE_APPDATA)))
                .build();

        Identity.getAuthorizationClient(this)
                .authorize(authRequest)
                .addOnSuccessListener(authResult -> {
                    if (authResult.hasResolution()) {
                        runOnUiThread(() -> showSyncing(false));
                        return;
                    }
                    String accessToken = authResult.getAccessToken();
                    if (accessToken == null) {
                        runOnUiThread(() -> showSyncing(false));
                        return;
                    }
                    try {
                        Drive drive = new Drive.Builder(
                                new NetHttpTransport(),
                                new GsonFactory(),
                                request -> request.getHeaders().setAuthorization("Bearer " + accessToken))
                                .setApplicationName("Net Worth Tracker")
                                .build();
                        driveServiceHelper = new DriveServiceHelper(drive);
                        performSync();
                    } catch (Exception e) {
                        runOnUiThread(() -> showSyncing(false));
                    }
                })
                .addOnFailureListener(e -> runOnUiThread(() -> {
                    showSyncing(false);
                    Toast.makeText(this, R.string.sync_failed, Toast.LENGTH_SHORT).show();
                }));
    }

    private void performSync() {
        if (executorService.isShutdown()) return;
        executorService.execute(() -> {
            try (Realm realm = Realm.getDefaultInstance()) {
                String fileId = Tasks.await(driveServiceHelper.searchFile("assets.json"));

                // Pull from Drive only when local is empty (new install / new device).
                // If local has data, local is the source of truth — never overwrite with Drive.
                boolean localEmpty = realm.where(Asset.class).count() == 0;
                if (fileId != null && localEmpty) {
                    String json = Tasks.await(driveServiceHelper.readFile(fileId));
                    Gson gson = new com.google.gson.GsonBuilder()
                            .serializeSpecialFloatingPointValues()
                            .create();
                    List<Asset> remoteAssets = gson.fromJson(json, new TypeToken<List<Asset>>(){}.getType());
                    if (remoteAssets != null && !remoteAssets.isEmpty()) {
                        realm.executeTransaction(r -> r.copyToRealmOrUpdate(remoteAssets));
                    }
                }

                // Always push local state to Drive
                List<Asset> localAssets = realm.where(Asset.class).findAll();
                Gson gson = new com.google.gson.GsonBuilder()
                        .serializeSpecialFloatingPointValues()
                        .create();
                String localJson = gson.toJson(realm.copyFromRealm(localAssets));

                if (fileId == null) {
                    Tasks.await(driveServiceHelper.createFile("assets.json", localJson));
                } else {
                    Tasks.await(driveServiceHelper.updateFile(fileId, localJson));
                }

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    refreshAllLoadedPages();
                    showSyncing(false);
                });
            } catch (Exception e) {
                Log.e("MainActivity", "Drive sync error", e);
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    showSyncing(false);
                    Toast.makeText(this, R.string.sync_failed, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void showSyncing(boolean syncing) {
        if (syncing) {
            int from = ContextCompat.getColor(this, R.color.divider);
            int to   = ContextCompat.getColor(this, Tools.getAccentColor());
            syncAnim = ObjectAnimator.ofArgb(binding.bottomAppBarCard, "strokeColor", from, to);
            syncAnim.setDuration(700);
            syncAnim.setRepeatMode(ObjectAnimator.REVERSE);
            syncAnim.setRepeatCount(ObjectAnimator.INFINITE);
            syncAnim.setInterpolator(new AccelerateDecelerateInterpolator());
            syncAnim.start();
        } else {
            if (syncAnim != null) {
                syncAnim.cancel();
                syncAnim = null;
            }
            binding.bottomAppBarCard.setStrokeColor(ContextCompat.getColor(this, R.color.divider));
        }
    }

    // ── Login state ──────────────────────────────────────────────────────────

    private boolean isSignedIn() {
        return Prefs.contains(Prefs.PREFS_TOKEN);
    }

    private void checkLogin() {
        boolean signedIn = isSignedIn();
        binding.navigationView.googleSignIn.setVisibility(signedIn ? View.GONE : View.VISIBLE);
        binding.navigationView.signedInLayout.setVisibility(signedIn ? View.VISIBLE : View.GONE);
        if (signedIn) {
            String email = Prefs.getString(Prefs.PREFS_USER_EMAIL, "");
            binding.navigationView.userEmail.setText(email);
            if (!email.isEmpty()) {
                binding.navigationView.drawerAvatarText.setText(email.substring(0, 1).toUpperCase());
            } else {
                binding.navigationView.drawerAvatarText.setText("W");
            }
            triggerSync();
        }
    }

    private void onSignedOut() {
        binding.navigationView.googleSignIn.setVisibility(View.VISIBLE);
        binding.navigationView.signedInLayout.setVisibility(View.GONE);
    }

    // ── Permissions ──────────────────────────────────────────────────────────

    private boolean hasStoragePermission(int requestCode) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            return true;
        }
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == 0) {
            return true;
        }
        ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, requestCode);
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != 0) {
            Toast.makeText(this, getString(R.string.backup_permission), Toast.LENGTH_SHORT).show();
            new Events().send(new PermissionNotGranted(requestCode));
            Prefs.save(Prefs.PREFS_AUTO_IMPORT_PERMISSION, true);
            return;
        }
        switch (requestCode) {
            case PERMISSION_EXPORT:      LocalBackup.startExport(LocalBackup.ExportType.MANUAL); break;
            case PERMISSION_EXPORT_AUTO: LocalBackup.startExport(LocalBackup.ExportType.AUTO);   break;
            case PERMISSION_IMPORT:      LocalImport.startImport(this);  break;
            case PERMISSION_IMPORT_CSS:
                if (getIntent().getData() != null) importFromFile(getIntent().getData());
                break;
        }
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            Uri uri = intent.getData();
            if (uri != null) {
                importFromFile(uri);
            }
        }
    }

    private void importFromFile(Uri uri) {
        try {
            LocalImport.importFromUri(uri);
        } catch (Exception e) {
            Log.e("MainActivity", "Import failed", e);
            Toast.makeText(this, R.string.import_no_backups, Toast.LENGTH_SHORT).show();
        }
    }

    // ── MonthPageFragment.Listener ───────────────────────────────────────────

    @Override
    public void onOpenDrawer() {
        binding.drawerLayout.openDrawer(GravityCompat.START);
    }

    @Override
    public void onRequestSync() {
        triggerSync();
    }

    @Override
    public void onEditAsset(Asset item) {
        openAssetView(item.getName(), item.getValue());
    }

    @Override
    public void onLongPressAsset(Asset item) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.menu)
                .setItems(new String[]{
                        getString(R.string.menu_view_trend),
                        getString(R.string.menu_delete)
                }, (dialog, which) -> {
                    if (which == 0) {
                        Intent intent = new Intent(this, SingleAssetTrendActivity.class);
                        intent.putExtra(SingleAssetTrendActivity.EXTRA_ASSET_NAME, item.getName());
                        startActivity(intent);
                    } else if (which == 1) {
                        new Events().send(new AssetDeleted(item));
                        try (Realm realm = Realm.getDefaultInstance()) {
                            realm.executeTransaction(r -> {
                                Asset toDelete = r.where(Asset.class).equalTo(AssetFields.ID, item.getId()).findFirst();
                                if (toDelete != null) toDelete.deleteFromRealm();
                            });
                            new Events().setProperty("numberOfAssets", String.valueOf(realm.where(Asset.class).count()));
                        }
                        refreshAllLoadedPages();
                    }
                })
                .show();
    }

    @Override
    public void onDataChanged() {
        refreshAllLoadedPages();
    }

    public void onMonthReady(Month m) {
        int[] current = MonthPagerAdapter.monthYearAt(binding.viewPager.getCurrentItem());
        if (m.getMonth() != current[0] || m.getYear() != current[1]) return;
        month = m;
        binding.monthName.setText(m.toString());
        updateMonthNameColor();
        updateGoalProgress();
        updateWidget();
        if (binding.viewPager.getOffscreenPageLimit() == ViewPager2.OFFSCREEN_PAGE_LIMIT_DEFAULT) {
            binding.viewPager.setOffscreenPageLimit(2);
        }
    }

    private Asset getNewAsset() {
        String name = binding.newAssetSheet.newAssetName.getText().toString();
        double value = toDouble(binding.newAssetSheet.newAssetValue);
        // The sign logic for change is handled in listeners, but the Asset itself 
        // just takes the final value from the main input.
        return new Asset(name, value, month.getMonth(), month.getYear());
    }

    private void saveAsset() {
        String name = binding.newAssetSheet.newAssetName.getText().toString();
        if (name.isEmpty()) {
            binding.newAssetSheet.newAssetName.setError(getString(R.string.new_asset_name_empty));
            binding.newAssetSheet.newAssetName.requestFocus();
            return;
        }
        String valueStr = binding.newAssetSheet.newAssetValue.getText().toString();
        if (valueStr.isEmpty()) {
            binding.newAssetSheet.newAssetValue.setError(getString(R.string.new_asset_value_empty));
            binding.newAssetSheet.newAssetValue.requestFocus();
            return;
        }
        Asset asset = getNewAsset();
        new Events().send(new AssetAdded(asset));
        try (Realm realm = Realm.getDefaultInstance()) {
            realm.executeTransaction(r -> r.copyToRealmOrUpdate(asset));
            new Events().setProperty("numberOfAssets", String.valueOf(realm.where(Asset.class).count()));
        }
        refreshAllLoadedPages();
        closeAssetView();
        if (!Prefs.getBoolean(Prefs.PREFS_AUTO_IMPORT_PERMISSION, false)) {
            if (hasStoragePermission(PERMISSION_EXPORT_AUTO)) {
                LocalBackup.startExport(LocalBackup.ExportType.AUTO);
            }
        }
    }

    private void updateSign(boolean isPlus) {
        int accentColor = ContextCompat.getColor(this, Tools.getAccentColor());
        int bgElev = ContextCompat.getColor(this, R.color.sheet_input_bg);
        int textDim = ContextCompat.getColor(this, R.color.sheet_text_dim);

        if (isPlus) {
            binding.newAssetSheet.btnPlus.setCardBackgroundColor(ColorStateList.valueOf(bgElev));
            binding.newAssetSheet.btnPlus.setCardElevation(2f * getResources().getDisplayMetrics().density);
            binding.newAssetSheet.txtPlus.setTextColor(accentColor);

            binding.newAssetSheet.btnMinus.setCardBackgroundColor(ColorStateList.valueOf(Color.TRANSPARENT));
            binding.newAssetSheet.btnMinus.setCardElevation(0f);
            binding.newAssetSheet.txtMinus.setTextColor(textDim);
        } else {
            binding.newAssetSheet.btnPlus.setCardBackgroundColor(ColorStateList.valueOf(Color.TRANSPARENT));
            binding.newAssetSheet.btnPlus.setCardElevation(0f);
            binding.newAssetSheet.txtPlus.setTextColor(textDim);

            binding.newAssetSheet.btnMinus.setCardBackgroundColor(ColorStateList.valueOf(bgElev));
            binding.newAssetSheet.btnMinus.setCardElevation(2f * getResources().getDisplayMetrics().density);
            binding.newAssetSheet.txtMinus.setTextColor(accentColor);
        }
    }

    private void openAssetView(@Nullable String name, @Nullable Double value) {
        updatingForm = true;
        
        String monthName = month.toString();
        boolean isNew = name == null;

        if (isNew) {
            binding.newAssetSheet.sheetStepLabel.setText(R.string.sheet_new_entry);
            binding.newAssetSheet.sheetTitle.setText(R.string.sheet_add_title);
            binding.newAssetSheet.labelAssetName.setVisibility(View.VISIBLE);
            binding.newAssetSheet.newAssetName.setVisibility(View.VISIBLE);
            binding.newAssetSheet.assetInitialCard.setVisibility(View.GONE);
            binding.newAssetSheet.pillSame.setVisibility(View.GONE);
            binding.newAssetSheet.pillPlus5.setVisibility(View.GONE);
            binding.newAssetSheet.pillMinus5.setVisibility(View.GONE);
            binding.newAssetSheet.pillPurchase.setVisibility(View.GONE);
            binding.newAssetSheet.pillSell.setVisibility(View.GONE);
            binding.newAssetSheet.pillProfit.setVisibility(View.GONE);
            binding.newAssetSheet.pillLoss.setVisibility(View.GONE);
            binding.newAssetSheet.pillReinvest.setVisibility(View.GONE);
            binding.newAssetSheet.pillChurning.setVisibility(View.GONE);
        } else {
            binding.newAssetSheet.sheetStepLabel.setText("Edit · " + monthName);
            binding.newAssetSheet.sheetTitle.setText(name);
            binding.newAssetSheet.labelAssetName.setVisibility(View.GONE);
            binding.newAssetSheet.newAssetName.setVisibility(View.GONE);
            binding.newAssetSheet.assetInitialCard.setVisibility(View.VISIBLE);
            binding.newAssetSheet.pillSame.setVisibility(View.VISIBLE);
            binding.newAssetSheet.pillPlus5.setVisibility(View.VISIBLE);
            binding.newAssetSheet.pillMinus5.setVisibility(View.VISIBLE);
            binding.newAssetSheet.pillPurchase.setVisibility(View.VISIBLE);
            binding.newAssetSheet.pillSell.setVisibility(View.VISIBLE);
            binding.newAssetSheet.pillProfit.setVisibility(View.VISIBLE);
            binding.newAssetSheet.pillLoss.setVisibility(View.VISIBLE);
            binding.newAssetSheet.pillReinvest.setVisibility(View.VISIBLE);
            binding.newAssetSheet.pillChurning.setVisibility(View.VISIBLE);
        }
        
        binding.newAssetSheet.newAssetName.setText(name != null ? name : "");
        binding.newAssetSheet.newAssetValue.setText(value != null ? formatStringValue(value) : "");
        
        if (name != null && !name.isEmpty()) {
            binding.newAssetSheet.assetInitial.setText(name.substring(0, 1).toUpperCase());
            int color = Tools.getAssetColor(name);
            binding.newAssetSheet.assetInitialCard.setCardBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.sheet_input_bg)));
            binding.newAssetSheet.assetInitial.setTextColor(color);
        } else {
            binding.newAssetSheet.assetInitial.setText("+");
            binding.newAssetSheet.assetInitialCard.setCardBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.sheet_chip_bg)));
            binding.newAssetSheet.assetInitial.setTextColor(ContextCompat.getColor(this, R.color.sheet_text_dim));
        }

        // Calculate correct change from previous month
        if (name != null && value != null) {
            Asset temp = new Asset(name, value, month.getMonth(), month.getYear());
            Asset prev = temp.getPrevious();
            double prevVal = (prev != null) ? prev.getValue() : 0.0;
            double change = value - prevVal;
            binding.newAssetSheet.newAssetChange.setText(formatStringValue(change));
            binding.newAssetSheet.newAssetValueFormatted.setText(formatString(value));
            updateSign(change >= 0);
        } else {
            binding.newAssetSheet.newAssetChange.setText("");
            binding.newAssetSheet.newAssetValueFormatted.setText("0");
            updateSign(true);
        }
        updatingForm = false;
        
        assetSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        binding.mainFab.hide();
        if (name != null) binding.newAssetSheet.newAssetName.setSelection(name.length());
    }

    private void showActionDialog(String title, boolean isPositive) {
        android.widget.EditText input = new android.widget.EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setHint("Amount");
        
        int dp = (int) (16 * getResources().getDisplayMetrics().density);
        android.widget.FrameLayout container = new android.widget.FrameLayout(this);
        android.widget.FrameLayout.LayoutParams params = new android.widget.FrameLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = dp;
        params.rightMargin = dp;
        params.topMargin = dp / 2;
        input.setLayoutParams(params);
        container.addView(input);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(container)
                .setPositiveButton("Apply", (d, which) -> {
                    double amount = 0;
                    try {
                        amount = Double.parseDouble(input.getText().toString());
                    } catch (Exception ignored) {}
                    
                    if (amount == 0) return;
                    
                    if (!isPositive) amount = -amount;
                    
                    Asset asset = getNewAsset();
                    Asset previous = asset.getPrevious();
                    double prevValue = (previous != null) ? previous.getValue() : 0.0;
                    
                    double newValue = prevValue + amount;
                    
                    updatingForm = true;
                    binding.newAssetSheet.newAssetChange.setText(formatStringValue(amount));
                    updateSign(amount >= 0);
                    binding.newAssetSheet.newAssetValue.setText(formatStringValue(newValue));
                    binding.newAssetSheet.newAssetValueFormatted.setText(formatString(newValue));
                    updatingForm = false;
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        Tools.styleDialog(dialog);
        dialog.show();
    }

    private String formatString(double value) {
        return java.text.NumberFormat.getCurrencyInstance(new java.util.Locale("en", "IN"))
                .format(value).replace("₹", "").trim();
    }

    private void closeAssetView() {
        assetSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    }

    private void toggleFab() {
        if (fabExpanded) collapseFab();
        else expandFab();
    }

    private void expandFab() {
        fabExpanded = true;
        binding.mainFab.animate().rotation(45f).setDuration(200).start();
        binding.mainFab.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#1F2937")));
        binding.fabScrim.setVisibility(View.VISIBLE);
        binding.fabScrim.animate().alpha(1f).setDuration(200).start();
        showFabItem(binding.fabAssetContainer, 0);
        showFabItem(binding.fabNoteContainer, 54);
    }

    private void collapseFab() {
        if (!fabExpanded) return;
        fabExpanded = false;
        binding.mainFab.animate().rotation(0f).setDuration(200).start();
        binding.mainFab.setBackgroundTintList(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, Tools.getAccentColor())));
        binding.fabScrim.animate().alpha(0f).setDuration(200)
                .withEndAction(() -> binding.fabScrim.setVisibility(View.GONE)).start();
        hideFabItem(binding.fabAssetContainer);
        hideFabItem(binding.fabNoteContainer);
    }

    private void showFabItem(android.view.ViewGroup container, long delay) {
        container.setVisibility(View.VISIBLE);
        container.setAlpha(0f);
        container.setTranslationY(60f);
        container.animate().alpha(1f).translationY(0f)
                .setStartDelay(delay).setDuration(200).start();
    }

    private void hideFabItem(android.view.ViewGroup container) {
        container.animate().alpha(0f).translationY(60f)
                .setDuration(150)
                .withEndAction(() -> container.setVisibility(View.GONE)).start();
    }

    private void setupBackHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    binding.drawerLayout.closeDrawer(GravityCompat.START);
                } else if (fabExpanded) {
                    collapseFab();
                } else if (assetSheetBehavior != null && assetSheetBehavior.getState() != BottomSheetBehavior.STATE_HIDDEN) {
                    closeAssetView();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    // ── Views ────────────────────────────────────────────────────────────────

    private String[] getAssetNames(String name) {
        try (Realm realm = Realm.getDefaultInstance()) {
            List<Asset> results = realm.where(Asset.class)
                    .distinct(AssetFields.NAME)
                    .contains(AssetFields.NAME, name, Case.INSENSITIVE)
                    .findAll();
            String[] names = new String[results.size()];
            for (int i = 0; i < results.size(); i++) {
                Asset a = results.get(i);
                names[i] = (a != null && a.getName() != null) ? a.getName() : "";
            }
            return names;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyAccentColor();
        if (firstResume) {
            firstResume = false;
            return;
        }
        refreshAllLoadedPages();
    }

    private void applyAccentColor() {
        int accentColor = ContextCompat.getColor(this, Tools.getAccentColor());
        ColorStateList accentList = ColorStateList.valueOf(accentColor);
        
        binding.previousMonth.setImageTintList(accentList);
        binding.nextMonth.setImageTintList(accentList);
        
        if (!fabExpanded) {
            binding.mainFab.setBackgroundTintList(accentList);
        }

        binding.newAssetSheet.sheetStepLabel.setTextColor(accentColor);
        binding.newAssetSheet.btnSaveAsset.setBackgroundTintList(accentList);
        
        // Navigation Drawer accents
        binding.navigationView.drawerGoal1yRow.goalPercent.setTextColor(accentColor);
        binding.navigationView.drawerGoal1yRow.goalProgress.setProgressTintList(accentList);
        binding.navigationView.drawerGoal3yRow.goalPercent.setTextColor(accentColor);
        binding.navigationView.drawerGoal3yRow.goalProgress.setProgressTintList(accentList);
        binding.navigationView.drawerGoal5yRow.goalPercent.setTextColor(accentColor);
        binding.navigationView.drawerGoal5yRow.goalProgress.setProgressTintList(accentList);
        
        binding.navigationView.drawerAvatarContainer.setBackgroundTintList(accentList);
        binding.navigationView.signOut.setImageTintList(accentList);
        
        // Loop through the drawer to find section headers
        applyAccentToDrawerHeaders(binding.navigationView.navigation, accentColor);

        updateMonthNameColor();
    }

    private void updateMonthNameColor() {
        if (month == null) return;
        Calendar today = Calendar.getInstance();
        boolean isCurrent = (month.getMonth() == (today.get(Calendar.MONTH) + 1)) && (month.getYear() == today.get(Calendar.YEAR));
        int accentColor = ContextCompat.getColor(this, Tools.getAccentColor());
        int defaultColor = ContextCompat.getColor(this, R.color.text);
        binding.monthName.setTextColor(isCurrent ? accentColor : defaultColor);
    }

    private void applyAccentToDrawerHeaders(ViewGroup group, int color) {
        for (int i = 0; i < group.getChildCount(); i++) {
            View v = group.getChildAt(i);
            if (v instanceof TextView) {
                TextView tv = (TextView) v;
                if (tv.getLetterSpacing() >= 0.09f) {
                    tv.setTextColor(color);
                }
            } else if (v instanceof ViewGroup) {
                applyAccentToDrawerHeaders((ViewGroup) v, color);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }

    private void updateWidget() {
        Intent intent = new Intent(this, OverviewWidget.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        int[] ids = AppWidgetManager.getInstance(getApplication()).getAppWidgetIds(new ComponentName(getApplication(), OverviewWidget.class));
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        sendBroadcast(intent);
    }

    private void updateGoalProgress() {
        if (executorService.isShutdown()) return;
        executorService.execute(() -> {
            List<Goal> goals;
            double current;
            try (Realm realm = Realm.getDefaultInstance()) {
                goals = realm.copyFromRealm(realm.where(Goal.class).sort("targetYear", Sort.ASCENDING).findAll());
                current = month.getValue(realm);
            }

            final double nw = current;
            final List<Goal> finalGoals = goals;
            final boolean hasAny = !goals.isEmpty();

            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                binding.navigationView.drawerGoalSection.setVisibility(hasAny ? View.VISIBLE : View.GONE);
                binding.navigationView.navSetGoal.setVisibility(hasAny ? View.GONE : View.VISIBLE);

                if (!hasAny) {
                    binding.navigationView.navSetGoal.setOnClickListener(v -> {
                        Intent goalsIntent = new Intent(this, GoalActivity.class);
                        goalsIntent.putExtra("current_net_worth", nw);
                        startActivity(goalsIntent);
                        binding.drawerLayout.closeDrawers();
                    });
                    return;
                }

                // Bind up to 3 goals (drawer layout has 3 rows)
                bindGoalRow(binding.navigationView.drawerGoal1yRow.getRoot(), binding.navigationView.drawerGoal1yRow.goalLabel,
                        binding.navigationView.drawerGoal1yRow.goalPercent, binding.navigationView.drawerGoal1yRow.goalProgress,
                        finalGoals.size() > 0 ? finalGoals.get(0) : null, nw);
                bindGoalRow(binding.navigationView.drawerGoal3yRow.getRoot(), binding.navigationView.drawerGoal3yRow.goalLabel,
                        binding.navigationView.drawerGoal3yRow.goalPercent, binding.navigationView.drawerGoal3yRow.goalProgress,
                        finalGoals.size() > 1 ? finalGoals.get(1) : null, nw);
                bindGoalRow(binding.navigationView.drawerGoal5yRow.getRoot(), binding.navigationView.drawerGoal5yRow.goalLabel,
                        binding.navigationView.drawerGoal5yRow.goalPercent, binding.navigationView.drawerGoal5yRow.goalProgress,
                        finalGoals.size() > 2 ? finalGoals.get(2) : null, nw);

                binding.navigationView.drawerGoalSection.setOnClickListener(v -> {
                    Intent goalsIntent = new Intent(this, GoalActivity.class);
                    goalsIntent.putExtra("current_net_worth", nw);
                    startActivity(goalsIntent);
                    binding.drawerLayout.closeDrawers();
                });
            });
        });
    }

    private void bindGoalRow(View row, TextView label, TextView percent,
                             android.widget.ProgressBar bar,
                             Goal goal, double current) {
        if (goal == null || goal.getTargetValue() <= 0) {
            row.setVisibility(View.GONE);
            return;
        }
        row.setVisibility(View.VISIBLE);
        label.setText(String.format(Locale.getDefault(), "%d GOAL", goal.getTargetYear()));
        int progress = (int) Math.min(Math.max(current / goal.getTargetValue() * 100, 0), 100);
        percent.setText(progress + "%");
        bar.setProgress(progress);
    }


    private void showMonthYearPicker() {
        // Month names (Jan–Dec, trim trailing empty entry from DateFormatSymbols)
        String[] allMonths = new DateFormatSymbols().getMonths();
        String[] monthNames = Arrays.copyOf(allMonths, 12);

        int currentYear = Calendar.getInstance().get(Calendar.YEAR);

        // Build pickers
        NumberPicker monthPicker = new NumberPicker(this);
        monthPicker.setMinValue(0);
        monthPicker.setMaxValue(11);
        monthPicker.setDisplayedValues(monthNames);
        monthPicker.setValue(month.getMonth() - 1);
        monthPicker.setWrapSelectorWheel(true);

        NumberPicker yearPicker = new NumberPicker(this);
        yearPicker.setMinValue(1970);
        yearPicker.setMaxValue(currentYear + 5);
        yearPicker.setValue(month.getYear());
        yearPicker.setWrapSelectorWheel(false);

        // Layout
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        int pad = (int) (24 * getResources().getDisplayMetrics().density);
        row.setPadding(pad, pad / 2, pad, 0);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        row.addView(monthPicker, params);
        row.addView(yearPicker, params);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Go to month")
                .setView(row)
                .setPositiveButton("Go", (d, which) -> {
                    int selectedMonth = monthPicker.getValue() + 1;
                    int selectedYear  = yearPicker.getValue();
                    binding.viewPager.setCurrentItem(MonthPagerAdapter.positionOf(selectedMonth, selectedYear), false);
                    closeAssetView();
                })
                .setNegativeButton("Cancel", null)
                .create();
        Tools.styleDialog(dialog);
        dialog.show();
    }

    public void toComment(View view) {
        Intent intent = new Intent(this, CommentActivity.class);
        intent.putExtra(AssetFields.MONTH, month.getMonth());
        intent.putExtra(AssetFields.YEAR, month.getYear());
        startActivity(intent);
    }

    // ── EventBus ─────────────────────────────────────────────────────────────

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvent event) {
        Toast.makeText(this, event.getMessage(), Toast.LENGTH_SHORT).show();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onImportedEvent(ImportedEvent event) {
        if (event.getAssets().isEmpty() && event.getNotes().isEmpty() && event.getGoals().isEmpty()) {
            Toast.makeText(this, R.string.import_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        int total = event.getAssets().size() + event.getNotes().size() + event.getGoals().size();
        String title = String.format(getString(R.string.import_confirmation), total);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(title)
                .setPositiveButton(R.string.backup_share_yes, (d, which) -> {
                    try (Realm realm = Realm.getDefaultInstance()) {
                        realm.executeTransaction(r -> {
                            r.delete(Asset.class);
                            r.delete(Note.class);
                            r.delete(Goal.class);
                            r.copyToRealmOrUpdate(event.getAssets());
                            r.copyToRealmOrUpdate(event.getNotes());
                            r.copyToRealmOrUpdate(event.getGoals());
                        });
                    }
                    refreshAllLoadedPages();
                    updateGoalProgress();
                    Toast.makeText(this, R.string.import_success, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.backup_share_no, null)
                .create();
        Tools.styleDialog(dialog);
        dialog.show();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onBackupSavedEvent(BackupSavedEvent event) {
        if (event.isShareImmediately()) {
            shareFile(event.getFile());
        } else {
            Toast.makeText(this, R.string.backup_saved, Toast.LENGTH_SHORT).show();
        }
    }

    private void shareFile(java.io.File file) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", file);
        intent.setType("application/octet-stream");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.putExtra(Intent.EXTRA_SUBJECT, file.getName());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, getString(R.string.backup_share_title)));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean goToCurrentMonth() {
        Calendar now = Calendar.getInstance();
        int m = now.get(Calendar.MONTH) + 1;
        int y = now.get(Calendar.YEAR);
        int pos = MonthPagerAdapter.positionOf(m, y);
        if (binding.viewPager.getCurrentItem() != pos) {
            binding.viewPager.setCurrentItem(pos, true);
        }
        return true;
    }

    private double toDouble(EditText editText) {
        try {
            return Double.parseDouble(editText.getText().toString());
        } catch (Exception e) {
            return 0.0;
        }
    }

    private String formatStringValue(double d) {
        if (Double.isInfinite(d) || Double.isNaN(d)) return "0";
        java.math.BigDecimal bd = java.math.BigDecimal.valueOf(d);
        return bd.stripTrailingZeros().toPlainString();
    }
}
