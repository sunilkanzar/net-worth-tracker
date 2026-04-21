package com.kanzar.networthtracker;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
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
    private MonthPagerAdapter pagerAdapter;
    private ObjectAnimator syncAnim;
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
        setupViewPager();
        credentialManager = CredentialManager.create(this);
        binding.drawerLayout.addDrawerListener(new androidx.drawerlayout.widget.DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerClosed(View drawerView) {
                // Collapse import submenu so it starts closed next time
                if (binding.navImportSubmenu.getVisibility() == View.VISIBLE) {
                    binding.navImportSubmenu.setVisibility(View.GONE);
                    binding.navImport.setCompoundDrawablesRelativeWithIntrinsicBounds(
                            androidx.core.content.ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_import),
                            null,
                            androidx.core.content.ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_chevron_down),
                            null);
                }
            }
        });
        viewListeners();
        handleIntent(getIntent());
        checkLogin();
        setupBackHandler();
    }

    private void setupViewPager() {
        pagerAdapter = new MonthPagerAdapter(this);
        binding.viewPager.setAdapter(pagerAdapter);
        binding.viewPager.setOffscreenPageLimit(ViewPager2.OFFSCREEN_PAGE_LIMIT_DEFAULT);
        int startPos = MonthPagerAdapter.positionOf(month.getMonth(), month.getYear());
        binding.viewPager.setCurrentItem(startPos, false);
        binding.viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                int[] my = MonthPagerAdapter.monthYearAt(position);
                month = new Month(my[0], my[1]);
                binding.monthName.setText(month.toString());
                closeAssetView();
            }
        });
    }

    private MonthPageFragment getCurrentFragment() {
        return (MonthPageFragment) getSupportFragmentManager()
                .findFragmentByTag("f" + binding.viewPager.getCurrentItem());
    }

    private void refreshCurrentPage() {
        MonthPageFragment f = getCurrentFragment();
        if (f != null) f.refresh(false);
        updateGoalProgress();
        updateWidget();
    }

    private void refreshAllLoadedPages() {
        int current = binding.viewPager.getCurrentItem();
        int limit = binding.viewPager.getOffscreenPageLimit();
        int range = limit == ViewPager2.OFFSCREEN_PAGE_LIMIT_DEFAULT ? 1 : limit;
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
        binding.monthName.setOnLongClickListener(v -> {
            Calendar now = Calendar.getInstance();
            int pos = MonthPagerAdapter.positionOf(now.get(Calendar.MONTH) + 1, now.get(Calendar.YEAR));
            if (binding.viewPager.getCurrentItem() != pos) {
                binding.viewPager.setCurrentItem(pos, true);
            }
            return true;
        });

        binding.googleSignIn.setOnClickListener(v -> {
            new Events().send(new SigninClicked());
            startSignIn();
        });

        binding.signOut.setOnClickListener(v -> {
            new Events().send(new SignoutClicked());
            signOut();
        });

        binding.navPreferences.setOnClickListener(v -> {
            binding.drawerLayout.closeDrawers();
            startActivity(new Intent(this, PreferencesActivity.class));
        });

        binding.navMonthly.setOnClickListener(v -> {
            new Events().send(new ButtonClicked("monthView"));
            monthPickerLauncher.launch(new Intent(this, MonthActivity.class));
            binding.drawerLayout.closeDrawers();
        });

        binding.navGraph.setOnClickListener(v -> {
            new Events().send(new ButtonClicked("chartView"));
            startActivity(new Intent(this, ChartActivity.class));
            binding.drawerLayout.closeDrawers();
        });

        binding.navAllocation.setOnClickListener(v -> {
            Intent intent = new Intent(this, AllocationActivity.class);
            intent.putExtra("month", month.getMonth());
            intent.putExtra("year", month.getYear());
            startActivity(intent);
            binding.drawerLayout.closeDrawers();
        });

        binding.navAssetTrend.setOnClickListener(v -> {
            startActivity(new Intent(this, AssetTrendActivity.class));
            binding.drawerLayout.closeDrawers();
        });

        binding.navExport.setOnClickListener(v -> {
            if (hasStoragePermission(PERMISSION_EXPORT)) {
                LocalBackup.startExport(false);
            }
            binding.drawerLayout.closeDrawers();
        });

        binding.navImport.setOnClickListener(v -> {
            boolean open = binding.navImportSubmenu.getVisibility() == View.VISIBLE;
            binding.navImportSubmenu.setVisibility(open ? View.GONE : View.VISIBLE);
            binding.navImport.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    androidx.core.content.ContextCompat.getDrawable(this, R.drawable.ic_import),
                    null,
                    androidx.core.content.ContextCompat.getDrawable(this,
                            open ? R.drawable.ic_chevron_down : R.drawable.ic_chevron_right),
                    null);
        });

        binding.navImportBackup.setOnClickListener(v -> {
            if (hasStoragePermission(PERMISSION_IMPORT)) {
                LocalImport.startImport(this);
            }
            binding.drawerLayout.closeDrawers();
        });

        binding.navImportFile.setOnClickListener(v -> {
            binding.drawerLayout.closeDrawers();
            importFileLauncher.launch(new String[]{"text/*", "application/octet-stream"});
        });

        binding.previousMonth.setOnClickListener(v -> {
            new Events().send(new ButtonClicked("previousMonth"));
            binding.viewPager.setCurrentItem(binding.viewPager.getCurrentItem() - 1, true);
        });

        binding.nextMonth.setOnClickListener(v -> {
            new Events().send(new ButtonClicked("nextMonth"));
            binding.viewPager.setCurrentItem(binding.viewPager.getCurrentItem() + 1, true);
        });

        binding.newAssetName.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() >= 2) {
                    String[] assetNames = getAssetNames(s.toString());
                    binding.newAssetName.setAdapter(new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_dropdown_item_1line, assetNames));
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        binding.newAssetChange.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (updatingForm) return;
                Asset asset = getNewAsset();
                Asset previous = asset.getPrevious();
                double prevValue = (previous != null) ? previous.getValue() : 0.0;
                double change = toDouble(binding.newAssetChange);
                BigDecimal newValue = BigDecimal.valueOf(prevValue).add(BigDecimal.valueOf(change));
                if (toDouble(binding.newAssetValue) != newValue.doubleValue()) {
                    updatingForm = true;
                    binding.newAssetValue.setText(formatStringValue(newValue.doubleValue()));
                    updatingForm = false;
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        binding.newAssetValue.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (updatingForm) return;
                Asset asset = getNewAsset();
                Asset previous = asset.getPrevious();
                double prevValue = (previous != null) ? previous.getValue() : 0.0;
                BigDecimal change = BigDecimal.valueOf(asset.getValue()).subtract(BigDecimal.valueOf(prevValue));
                if (toDouble(binding.newAssetChange) != change.doubleValue()) {
                    updatingForm = true;
                    binding.newAssetChange.setText(formatStringValue(change.doubleValue()));
                    updatingForm = false;
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Main FAB: toggle speed-dial
        binding.newAssetSave.setOnClickListener(v -> {
            new Events().send(new ButtonClicked("newAssetSave"));
            if (binding.newAssetLayout.getVisibility() == View.VISIBLE) {
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
        binding.fabScrim.setOnClickListener(v -> collapseFab());

        binding.btnSaveAsset.setOnClickListener(v -> saveAsset());

        View.OnClickListener openGoals = v -> {
            Intent goalsIntent = new Intent(this, GoalActivity.class);
            goalsIntent.putExtra("current_net_worth", month.getValue());
            startActivity(goalsIntent);
        };
        binding.editGoals.setOnClickListener(openGoals);
        binding.navGoals.setOnClickListener(openGoals);
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
        executorService.execute(() -> {
            try (Realm realm = Realm.getDefaultInstance()) {
                String fileId = Tasks.await(driveServiceHelper.searchFile("assets.json"));

                // Pull from Drive only when local is empty (new install / new device).
                // If local has data, local is the source of truth — never overwrite with Drive.
                boolean localEmpty = realm.where(Asset.class).count() == 0;
                if (fileId != null && localEmpty) {
                    String json = Tasks.await(driveServiceHelper.readFile(fileId));
                    List<Asset> remoteAssets = new Gson().fromJson(json, new TypeToken<List<Asset>>(){}.getType());
                    if (remoteAssets != null && !remoteAssets.isEmpty()) {
                        realm.executeTransaction(r -> r.copyToRealmOrUpdate(remoteAssets));
                    }
                }

                // Always push local state to Drive
                List<Asset> localAssets = realm.where(Asset.class).findAll();
                String localJson = new Gson().toJson(realm.copyFromRealm(localAssets));

                if (fileId == null) {
                    Tasks.await(driveServiceHelper.createFile("assets.json", localJson));
                } else {
                    Tasks.await(driveServiceHelper.updateFile(fileId, localJson));
                }

                runOnUiThread(() -> {
                    refreshAllLoadedPages();
                    showSyncing(false);
                });
            } catch (Exception e) {
                Log.e("MainActivity", "Drive sync error", e);
                runOnUiThread(() -> {
                    showSyncing(false);
                    Toast.makeText(this, R.string.sync_failed, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void confirmClearAllData() {
        int a = (int) (Math.random() * 9) + 1;
        int b = (int) (Math.random() * 9) + 1;
        int answer = a + b;

        int dp = (int) getResources().getDisplayMetrics().density;

        android.widget.LinearLayout container = new android.widget.LinearLayout(this);
        container.setOrientation(android.widget.LinearLayout.VERTICAL);
        container.setPadding(24 * dp, 8 * dp, 24 * dp, 8 * dp);

        android.widget.TextView label = new android.widget.TextView(this);
        label.setText("To confirm, solve: " + a + " + " + b + " = ?");
        label.setTextSize(13f);
        label.setTextColor(0xFF888888);
        label.setPadding(0, 0, 0, 6 * dp);
        container.addView(label);

        android.widget.EditText input = new android.widget.EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setHint("Your answer");
        input.setTextAlignment(android.view.View.TEXT_ALIGNMENT_CENTER);
        input.setTextSize(18f);
        container.addView(input);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.clear_all_data_confirm_title)
                .setMessage(R.string.clear_all_data_confirm_msg)
                .setView(container)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String entered = input.getText().toString().trim();
            if (entered.isEmpty()) {
                input.setError("Enter the answer");
                return;
            }
            if (Integer.parseInt(entered) != answer) {
                input.setError("Wrong answer, try again");
                return;
            }
            dialog.dismiss();
            performClearAllData();
        }));

        dialog.show();
    }

    private void performClearAllData() {
        showSyncing(true);
        executorService.execute(() -> {
            // 1. Clear local Realm data
            try (Realm realm = Realm.getDefaultInstance()) {
                realm.executeTransaction(r -> r.deleteAll());
            } catch (Exception e) {
                Log.e("MainActivity", "Clear local data error", e);
            }

            // 2. Delete Drive file if signed in
            if (driveServiceHelper != null) {
                try {
                    String fileId = Tasks.await(driveServiceHelper.searchFile("assets.json"));
                    if (fileId != null) {
                        Tasks.await(driveServiceHelper.deleteFile(fileId));
                    }
                } catch (Exception e) {
                    Log.e("MainActivity", "Clear Drive data error", e);
                }
            }

            runOnUiThread(() -> {
                showSyncing(false);
                refreshAllLoadedPages();
                Toast.makeText(this, R.string.clear_all_data_success, Toast.LENGTH_SHORT).show();
            });
        });
    }


    private void showSyncing(boolean syncing) {
        if (syncing) {
            int from = ContextCompat.getColor(this, R.color.divider);
            int to   = ContextCompat.getColor(this, R.color.colorAccent);
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
        binding.googleSignIn.setVisibility(signedIn ? View.GONE : View.VISIBLE);
        binding.signedInLayout.setVisibility(signedIn ? View.VISIBLE : View.GONE);
        if (signedIn) {
            binding.userEmail.setText(Prefs.getString(Prefs.PREFS_USER_EMAIL, ""));
            triggerSync();
        }
    }

    private void onSignedOut() {
        binding.googleSignIn.setVisibility(View.VISIBLE);
        binding.signedInLayout.setVisibility(View.GONE);
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
            case PERMISSION_EXPORT:      LocalBackup.startExport(false); break;
            case PERMISSION_EXPORT_AUTO: LocalBackup.startExport(true);  break;
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
                        Intent intent = new Intent(this, AssetTrendActivity.class);
                        intent.putExtra(AssetTrendActivity.EXTRA_PINNED_ASSET, item.getName());
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
        updateGoalProgress();
        updateWidget();
        if (binding.viewPager.getOffscreenPageLimit() == ViewPager2.OFFSCREEN_PAGE_LIMIT_DEFAULT) {
            binding.viewPager.setOffscreenPageLimit(2);
        }
    }

    private Asset getNewAsset() {
        String name = binding.newAssetName.getText().toString();
        double value = toDouble(binding.newAssetValue);
        return new Asset(name, value, month.getMonth(), month.getYear());
    }

    private void saveAsset() {
        String name = binding.newAssetName.getText().toString();
        if (name.isEmpty()) {
            binding.newAssetName.setError(getString(R.string.new_asset_name_empty));
            return;
        }
        String valueStr = binding.newAssetValue.getText().toString();
        if (valueStr.isEmpty()) {
            binding.newAssetValue.setError(getString(R.string.new_asset_value_empty));
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
                LocalBackup.startExport(true);
            }
        }
    }

    private void openAssetView(@Nullable String name, @Nullable Double value) {
        updatingForm = true;
        binding.newAssetLayout.setVisibility(View.VISIBLE);
        binding.newAssetName.setText(name != null ? name : "");
        binding.newAssetValue.setText(value != null ? formatStringValue(value) : "");
        // Calculate correct change from previous month
        if (name != null && value != null) {
            Asset temp = new Asset(name, value, month.getMonth(), month.getYear());
            Asset prev = temp.getPrevious();
            double prevVal = (prev != null) ? prev.getValue() : 0.0;
            binding.newAssetChange.setText(formatStringValue(value - prevVal));
        } else {
            binding.newAssetChange.setText("");
        }
        updatingForm = false;
        if (name != null) binding.newAssetName.setSelection(name.length());
    }

    private void closeAssetView() {
        binding.newAssetLayout.setVisibility(View.GONE);
        collapseFab();
    }

    private void toggleFab() {
        if (fabExpanded) collapseFab();
        else expandFab();
    }

    private void expandFab() {
        fabExpanded = true;
        binding.newAssetSave.animate().rotation(45f).setDuration(200).start();
        binding.fabScrim.setVisibility(View.VISIBLE);
        binding.fabScrim.animate().alpha(1f).setDuration(200).start();
        showFabItem(binding.fabAssetContainer, 0);
        showFabItem(binding.fabNoteContainer, 60);
    }

    private void collapseFab() {
        if (!fabExpanded) return;
        fabExpanded = false;
        binding.newAssetSave.animate().rotation(0f).setDuration(200).start();
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
                if (fabExpanded) {
                    collapseFab();
                } else if (binding.newAssetLayout.getVisibility() == View.VISIBLE) {
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
        if (firstResume) {
            firstResume = false;
            return;
        }
        refreshCurrentPage();
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
        float g1 = Prefs.getFloat(Prefs.PREFS_GOAL_1Y, 0f);
        float g3 = Prefs.getFloat(Prefs.PREFS_GOAL_3Y, 0f);
        float g5 = Prefs.getFloat(Prefs.PREFS_GOAL_5Y, 0f);

        boolean anyGoal = g1 > 0 || g3 > 0 || g5 > 0;
        int goalVis = anyGoal ? View.VISIBLE : View.GONE;
        binding.goalsCard.setVisibility(goalVis);
        binding.goalsDivider.setVisibility(goalVis);
        // Show "Set Goals" nav item only when no goals exist; once set, user edits via the card
        binding.navGoals.setVisibility(anyGoal ? View.GONE : View.VISIBLE);
        if (!anyGoal) return;

        double current = month.getValue();
        int setYear = Prefs.getInt(Prefs.PREFS_GOAL_SET_YEAR, Calendar.getInstance().get(Calendar.YEAR));

        updateGoalRow(binding.goal1yRow, binding.goal1yLabel, binding.goal1yBar, binding.goal1yPercent,
                g1, current, setYear + 1);
        updateGoalRow(binding.goal3yRow, binding.goal3yLabel, binding.goal3yBar, binding.goal3yPercent,
                g3, current, setYear + 3);
        updateGoalRow(binding.goal5yRow, binding.goal5yLabel, binding.goal5yBar, binding.goal5yPercent,
                g5, current, setYear + 5);
    }

    private void updateGoalRow(View row, TextView label, android.widget.ProgressBar bar, TextView percent,
                                float goal, double current, int targetYear) {
        if (goal <= 0) {
            row.setVisibility(View.GONE);
            return;
        }
        row.setVisibility(View.VISIBLE);
        label.setText(String.valueOf(targetYear));

        int progress = (int) Math.min(Math.max(current / goal * 100, 0), 100);
        bar.setProgress(progress);
        percent.setText(getString(R.string.helper_text, String.valueOf(progress)).replace("Click to add ", "").replace(" to this month", "%"));
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

        new AlertDialog.Builder(this)
                .setTitle("Go to month")
                .setView(row)
                .setPositiveButton("Go", (dialog, which) -> {
                    int selectedMonth = monthPicker.getValue() + 1;
                    int selectedYear  = yearPicker.getValue();
                    binding.viewPager.setCurrentItem(MonthPagerAdapter.positionOf(selectedMonth, selectedYear), false);
                    closeAssetView();
                })
                .setNegativeButton("Cancel", null)
                .show();
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
        if (event.getAssets().isEmpty()) {
            Toast.makeText(this, R.string.import_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        String title = String.format(getString(R.string.import_confirmation), event.getAssets().size());
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(R.string.backup_share)
                .setPositiveButton(R.string.backup_share_yes, (dialog, which) -> {
                    try (Realm realm = Realm.getDefaultInstance()) {
                        realm.executeTransaction(r -> r.copyToRealmOrUpdate(event.getAssets()));
                    }
                    refreshAllLoadedPages();
                })
                .setNegativeButton(R.string.backup_share_no, null)
                .show();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onBackupSavedEvent(BackupSavedEvent event) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.backup_saved)
                .setMessage(R.string.backup_share)
                .setPositiveButton(R.string.backup_share_yes, (dialog, which) -> {
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", event.getFile());
                    intent.setType("application/octet-stream");
                    intent.putExtra(Intent.EXTRA_STREAM, uri);
                    intent.putExtra(Intent.EXTRA_SUBJECT, event.getFile().getName());
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(Intent.createChooser(intent, getString(R.string.backup_share_title)));
                })
                .setNegativeButton(R.string.backup_share_no, null)
                .show();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

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
