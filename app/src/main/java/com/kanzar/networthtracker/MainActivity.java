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

public class MainActivity extends AppCompatActivity implements AssetAdapter.OnItemClickListener {

    private static final int PERMISSION_EXPORT = 1;
    private static final int PERMISSION_EXPORT_AUTO = 2;
    private static final int PERMISSION_IMPORT = 3;
    private static final int PERMISSION_IMPORT_CSS = 4;

    private AssetAdapter adapter;
    private final Month month = new Month();
    private CredentialManager credentialManager;
    private DriveServiceHelper driveServiceHelper;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    // View bindings
    private RecyclerView assetList;
    private DrawerLayout drawerLayout;
    private SwipeRefreshLayout refreshLayout;
    private CardView newAssetLayout;
    private AutoCompleteTextView newAssetName;
    private EditText newAssetValue;
    private EditText newAssetChange;
    private FloatingActionButton newAssetSave;
    private TextView monthName;
    private TextView monthValue;
    private TextView monthValueChange;
    private TextView headerPrevValue;
    private TextView headerAssetCount;
    private com.kanzar.networthtracker.views.MiniBarView miniBarChart;
    private PercentView percentView;
    private ObjectAnimator syncAnim;
    private CardView tutorial;
    private boolean updatingForm = false;

    // Speed-dial FAB
    private View fabScrim;
    private android.view.ViewGroup fabNoteContainer;
    private android.view.ViewGroup fabAssetContainer;
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
                month.setMonth(data.getIntExtra("month", month.getMonth()));
                month.setYear(data.getIntExtra("year", month.getYear()));
                updateViews();
            }
        });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        setupRecyclerView();
        credentialManager = CredentialManager.create(this);
        drawerLayout.addDrawerListener(new androidx.drawerlayout.widget.DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerClosed(View drawerView) {
                // Collapse import submenu so it starts closed next time
                View sub = findViewById(R.id.navImportSubmenu);
                if (sub.getVisibility() == View.VISIBLE) {
                    sub.setVisibility(View.GONE);
                    ((TextView) findViewById(R.id.navImport)).setCompoundDrawablesRelativeWithIntrinsicBounds(
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

    private void initViews() {
        assetList = findViewById(R.id.assetList);
        drawerLayout = findViewById(R.id.drawerLayout);
        refreshLayout = findViewById(R.id.refreshLayout);
        newAssetLayout = findViewById(R.id.newAssetLayout);
        newAssetName = findViewById(R.id.newAssetName);
        newAssetValue = findViewById(R.id.newAssetValue);
        newAssetChange = findViewById(R.id.newAssetChange);
        newAssetSave = findViewById(R.id.newAssetSave);
        monthName = findViewById(R.id.monthName);
        monthValue = findViewById(R.id.monthValue);
        monthValueChange = findViewById(R.id.monthValueChange);
        headerPrevValue = findViewById(R.id.headerPrevValue);
        headerAssetCount = findViewById(R.id.headerAssetCount);
        miniBarChart = findViewById(R.id.miniBarChart);
        percentView = findViewById(R.id.percentView);
        tutorial = findViewById(R.id.tutorial);
        fabScrim = findViewById(R.id.fabScrim);
        fabNoteContainer = findViewById(R.id.fabNoteContainer);
        fabAssetContainer = findViewById(R.id.fabAssetContainer);
        findViewById(R.id.btnSaveAsset).setOnClickListener(v -> saveAsset());
    }

    private void handleIntent(Intent intent) {
        Uri data = intent.getData();
        if (data != null && hasStoragePermission(PERMISSION_IMPORT_CSS)) {
            importFromFile(data);
        }
    }

    private void importFromFile(Uri uri) {
        try {
            LocalImport.importFromUri(uri);
        } catch (Exception e) {
            Toast.makeText(this, R.string.import_empty, Toast.LENGTH_SHORT).show();
        }
    }

    private void setupRecyclerView() {
        adapter = new AssetAdapter(this);
        assetList.setLayoutManager(new LinearLayoutManager(this));
        assetList.setAdapter(adapter);
    }

    private void viewListeners() {
        findViewById(R.id.menu).setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        monthName.setOnClickListener(v -> showMonthYearPicker());

        findViewById(R.id.googleSignIn).setOnClickListener(v -> {
            new Events().send(new SigninClicked());
            startSignIn();
        });

        findViewById(R.id.signOut).setOnClickListener(v -> {
            new Events().send(new SignoutClicked());
            signOut();
        });

        refreshLayout.setOnRefreshListener(() -> {
            refreshLayout.setRefreshing(false);
            triggerSync();
        });

        findViewById(R.id.navPreferences).setOnClickListener(v -> {
            drawerLayout.closeDrawers();
            startActivity(new Intent(this, PreferencesActivity.class));
        });

        findViewById(R.id.navMonthly).setOnClickListener(v -> {
            new Events().send(new ButtonClicked("monthView"));
            monthPickerLauncher.launch(new Intent(this, MonthActivity.class));
            drawerLayout.closeDrawers();
        });

        findViewById(R.id.navGraph).setOnClickListener(v -> {
            new Events().send(new ButtonClicked("chartView"));
            startActivity(new Intent(this, ChartActivity.class));
            drawerLayout.closeDrawers();
        });

        findViewById(R.id.navAllocation).setOnClickListener(v -> {
            Intent intent = new Intent(this, AllocationActivity.class);
            intent.putExtra("month", month.getMonth());
            intent.putExtra("year", month.getYear());
            startActivity(intent);
            drawerLayout.closeDrawers();
        });

        findViewById(R.id.navAssetTrend).setOnClickListener(v -> {
            startActivity(new Intent(this, AssetTrendActivity.class));
            drawerLayout.closeDrawers();
        });

        findViewById(R.id.navExport).setOnClickListener(v -> {
            if (hasStoragePermission(PERMISSION_EXPORT)) {
                LocalBackup.startExport(false);
            }
            drawerLayout.closeDrawers();
        });

        View navImportSubmenu = findViewById(R.id.navImportSubmenu);
        TextView navImport = findViewById(R.id.navImport);
        navImport.setOnClickListener(v -> {
            boolean open = navImportSubmenu.getVisibility() == View.VISIBLE;
            navImportSubmenu.setVisibility(open ? View.GONE : View.VISIBLE);
            navImport.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    androidx.core.content.ContextCompat.getDrawable(this, R.drawable.ic_import),
                    null,
                    androidx.core.content.ContextCompat.getDrawable(this,
                            open ? R.drawable.ic_chevron_down : R.drawable.ic_chevron_right),
                    null);
        });

        findViewById(R.id.navImportBackup).setOnClickListener(v -> {
            if (hasStoragePermission(PERMISSION_IMPORT)) {
                LocalImport.startImport(this);
            }
            drawerLayout.closeDrawers();
        });

        findViewById(R.id.navImportFile).setOnClickListener(v -> {
            drawerLayout.closeDrawers();
            importFileLauncher.launch(new String[]{"text/*", "application/octet-stream"});
        });

        findViewById(R.id.previousMonth).setOnClickListener(v -> {
            new Events().send(new ButtonClicked("previousMonth"));
            month.previous();
            closeAssetView();
            updateViews();
        });

        findViewById(R.id.nextMonth).setOnClickListener(v -> {
            new Events().send(new ButtonClicked("nextMonth"));
            month.next();
            closeAssetView();
            updateViews();
        });

        newAssetName.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() >= 2) {
                    String[] assetNames = getAssetNames(s.toString());
                    newAssetName.setAdapter(new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_dropdown_item_1line, assetNames));
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        newAssetChange.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (updatingForm) return;
                Asset asset = getNewAsset();
                Asset previous = asset.getPrevious();
                double prevValue = (previous != null) ? previous.getValue() : 0.0;
                double change = toDouble(newAssetChange);
                BigDecimal newValue = BigDecimal.valueOf(prevValue).add(BigDecimal.valueOf(change));
                if (toDouble(newAssetValue) != newValue.doubleValue()) {
                    updatingForm = true;
                    newAssetValue.setText(formatStringValue(newValue.doubleValue()));
                    updatingForm = false;
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        newAssetValue.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (updatingForm) return;
                Asset asset = getNewAsset();
                Asset previous = asset.getPrevious();
                double prevValue = (previous != null) ? previous.getValue() : 0.0;
                BigDecimal change = BigDecimal.valueOf(asset.getValue()).subtract(BigDecimal.valueOf(prevValue));
                if (toDouble(newAssetChange) != change.doubleValue()) {
                    updatingForm = true;
                    newAssetChange.setText(formatStringValue(change.doubleValue()));
                    updatingForm = false;
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Main FAB: toggle speed-dial
        newAssetSave.setOnClickListener(v -> {
            new Events().send(new ButtonClicked("newAssetSave"));
            if (newAssetLayout.getVisibility() == View.VISIBLE) {
                closeAssetView();
                return;
            }
            toggleFab();
        });

        // Speed-dial: Add Asset
        findViewById(R.id.fabAsset).setOnClickListener(v -> {
            collapseFab();
            openAssetView(null, null);
        });

        // Speed-dial: Monthly Note
        findViewById(R.id.fabNote).setOnClickListener(v -> {
            collapseFab();
            toComment(null);
        });

        // Scrim: dismiss on tap outside
        fabScrim.setOnClickListener(v -> collapseFab());

        findViewById(R.id.editGoals).setOnClickListener(v -> {
            Intent goalsIntent = new Intent(this, GoalActivity.class);
            goalsIntent.putExtra("current_net_worth", month.getValue());
            startActivity(goalsIntent);
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
                    updateViews();
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
                updateViews();
                Toast.makeText(this, R.string.clear_all_data_success, Toast.LENGTH_SHORT).show();
            });
        });
    }


    private void showSyncing(boolean syncing) {
        if (syncing) {
            syncAnim = ObjectAnimator.ofFloat(monthValue, "alpha", 1f, 0.25f);
            syncAnim.setDuration(600);
            syncAnim.setRepeatMode(ObjectAnimator.REVERSE);
            syncAnim.setRepeatCount(ObjectAnimator.INFINITE);
            syncAnim.setInterpolator(new AccelerateDecelerateInterpolator());
            syncAnim.start();
        } else {
            if (syncAnim != null) {
                syncAnim.cancel();
                syncAnim = null;
            }
            monthValue.setAlpha(1f);
        }
    }

    // ── Login state ──────────────────────────────────────────────────────────

    private boolean isSignedIn() {
        return Prefs.contains(Prefs.PREFS_TOKEN);
    }

    private void checkLogin() {
        boolean signedIn = isSignedIn();
        findViewById(R.id.googleSignIn).setVisibility(signedIn ? View.GONE : View.VISIBLE);
        findViewById(R.id.signedInLayout).setVisibility(signedIn ? View.VISIBLE : View.GONE);
        if (signedIn) {
            ((TextView) findViewById(R.id.userEmail)).setText(Prefs.getString(Prefs.PREFS_USER_EMAIL, ""));
            triggerSync();
        }
    }

    private void onSignedOut() {
        findViewById(R.id.googleSignIn).setVisibility(View.VISIBLE);
        findViewById(R.id.signedInLayout).setVisibility(View.GONE);
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

    // ── Asset interactions ───────────────────────────────────────────────────

    @Override
    public void onItemClick(Asset item) {
        openAssetView(item.getName(), item.getValue());
    }

    @Override
    public void onItemLongClick(Asset item) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.menu)
                .setItems(new String[]{getString(R.string.menu_delete)}, (dialog, which) -> {
                    if (which == 0) {
                        new Events().send(new AssetDeleted(item));
                        try (Realm realm = Realm.getDefaultInstance()) {
                            realm.executeTransaction(r -> {
                                Asset toDelete = r.where(Asset.class).equalTo(AssetFields.ID, item.getId()).findFirst();
                                if (toDelete != null) toDelete.deleteFromRealm();
                            });
                            new Events().setProperty("numberOfAssets", String.valueOf(realm.where(Asset.class).count()));
                        }
                        updateViews();
                    }
                })
                .show();
    }

    private Asset getNewAsset() {
        String name = newAssetName.getText().toString();
        double value = toDouble(newAssetValue);
        return new Asset(name, value, month.getMonth(), month.getYear());
    }

    private void saveAsset() {
        String name = newAssetName.getText().toString();
        if (name.isEmpty()) {
            newAssetName.setError(getString(R.string.new_asset_name_empty));
            return;
        }
        String valueStr = newAssetValue.getText().toString();
        if (valueStr.isEmpty()) {
            newAssetValue.setError(getString(R.string.new_asset_value_empty));
            return;
        }
        Asset asset = getNewAsset();
        new Events().send(new AssetAdded(asset));
        try (Realm realm = Realm.getDefaultInstance()) {
            realm.executeTransaction(r -> r.copyToRealmOrUpdate(asset));
            new Events().setProperty("numberOfAssets", String.valueOf(realm.where(Asset.class).count()));
        }
        updateViews();
        closeAssetView();
        if (!Prefs.getBoolean(Prefs.PREFS_AUTO_IMPORT_PERMISSION, false)) {
            if (hasStoragePermission(PERMISSION_EXPORT_AUTO)) {
                LocalBackup.startExport(true);
            }
        }
    }

    private void openAssetView(@Nullable String name, @Nullable Double value) {
        updatingForm = true;
        newAssetLayout.setVisibility(View.VISIBLE);
        newAssetName.setText(name != null ? name : "");
        newAssetValue.setText(value != null ? formatStringValue(value) : "");
        // Calculate correct change from previous month
        if (name != null && value != null) {
            Asset temp = new Asset(name, value, month.getMonth(), month.getYear());
            Asset prev = temp.getPrevious();
            double prevVal = (prev != null) ? prev.getValue() : 0.0;
            newAssetChange.setText(formatStringValue(value - prevVal));
        } else {
            newAssetChange.setText("");
        }
        updatingForm = false;
        if (name != null) newAssetName.setSelection(name.length());
    }

    private void closeAssetView() {
        newAssetLayout.setVisibility(View.GONE);
        collapseFab();
    }

    private void toggleFab() {
        if (fabExpanded) collapseFab();
        else expandFab();
    }

    private void expandFab() {
        fabExpanded = true;
        newAssetSave.animate().rotation(45f).setDuration(200).start();
        fabScrim.setVisibility(View.VISIBLE);
        fabScrim.animate().alpha(1f).setDuration(200).start();
        showFabItem(fabAssetContainer, 0);
        showFabItem(fabNoteContainer, 60);
    }

    private void collapseFab() {
        if (!fabExpanded) return;
        fabExpanded = false;
        newAssetSave.animate().rotation(0f).setDuration(200).start();
        fabScrim.animate().alpha(0f).setDuration(200)
                .withEndAction(() -> fabScrim.setVisibility(View.GONE)).start();
        hideFabItem(fabAssetContainer);
        hideFabItem(fabNoteContainer);
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
                } else if (newAssetLayout.getVisibility() == View.VISIBLE) {
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
        updateViews();
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

    private void updateViews() {
        List<Asset> assets = month.getAssets();
        try (Realm realm = Realm.getDefaultInstance()) {
            tutorial.setVisibility(realm.where(Asset.class).count() == 0 ? View.VISIBLE : View.GONE);
        }
        adapter.setItems(assets);
        month.calculateValues();
        monthName.setText(month.toString());
        monthValue.setText(Tools.formatAmount(month.getValue()));
        percentView.init(month.getPreviousMonth().getValue(), month.getValue());
        percentView.fillValueChange(monthValueChange);

        // Previous month value
        double prevValue = month.getPreviousMonth().getValue();
        if (prevValue != 0) {
            headerPrevValue.setVisibility(View.VISIBLE);
            headerPrevValue.setText(getString(R.string.header_prev_value, Tools.formatAmount(prevValue)));
        } else {
            headerPrevValue.setVisibility(View.GONE);
        }

        // Asset / liability count (real entries only, skip helpers)
        int assetCount = 0, liabilityCount = 0;
        for (Asset a : assets) {
            if (!a.isHelper()) {
                if (a.getValue() >= 0) assetCount++;
                else liabilityCount++;
            }
        }
        if (assetCount > 0 || liabilityCount > 0) {
            StringBuilder countStr = new StringBuilder();
            if (assetCount > 0) {
                countStr.append(getString(assetCount == 1 ? R.string.asset_count_one : R.string.asset_count_many, assetCount));
            }
            if (liabilityCount > 0) {
                if (countStr.length() > 0) countStr.append(getString(R.string.count_separator));
                countStr.append(getString(liabilityCount == 1 ? R.string.liability_count_one : R.string.liability_count_many, liabilityCount));
            }
            headerAssetCount.setVisibility(View.VISIBLE);
            headerAssetCount.setText(countStr.toString());
        } else {
            headerAssetCount.setVisibility(View.GONE);
        }

        updateWidget();
        getCommentPreview();
        updateGoalProgress();
        updateMiniBar();
    }

    private void updateMiniBar() {
        executorService.execute(() -> {
            java.util.ArrayList<Float> history = new java.util.ArrayList<>();
            // Get 6 months including current
            for (int i = 5; i >= 0; i--) {
                Month m = new Month(month.getMonth(), month.getYear());
                for (int j = 0; j < i; j++) m.previous();
                m.calculateValues();
                history.add((float) m.getValue());
            }
            runOnUiThread(() -> miniBarChart.setData(history, 5));
        });
    }

    private void updateWidget() {
        Intent intent = new Intent(this, OverviewWidget.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        int[] ids = AppWidgetManager.getInstance(getApplication()).getAppWidgetIds(new ComponentName(getApplication(), OverviewWidget.class));
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        sendBroadcast(intent);
    }

    private void getCommentPreview() {
        String note = Prefs.getString(
                com.kanzar.networthtracker.CommentActivity.noteKey(month.getMonth(), month.getYear()), "");
        TextView preview = findViewById(R.id.monthCommentPreview);
        if (!note.isEmpty()) {
            preview.setVisibility(View.VISIBLE);
            preview.setText(note);
        } else {
            preview.setVisibility(View.GONE);
        }
    }

    private void updateGoalProgress() {
        float g1 = Prefs.getFloat(Prefs.PREFS_GOAL_1Y, 0f);
        float g3 = Prefs.getFloat(Prefs.PREFS_GOAL_3Y, 0f);
        float g5 = Prefs.getFloat(Prefs.PREFS_GOAL_5Y, 0f);

        boolean anyGoal = g1 > 0 || g3 > 0 || g5 > 0;
        int goalVis = anyGoal ? View.VISIBLE : View.GONE;
        findViewById(R.id.goalsCard).setVisibility(goalVis);
        findViewById(R.id.goalsDivider).setVisibility(goalVis);
        // Show "Set Goals" nav item only when no goals exist; once set, user edits via the card
        findViewById(R.id.navGoals).setVisibility(anyGoal ? View.GONE : View.VISIBLE);
        if (!anyGoal) return;

        double current = month.getValue();
        int setYear = Prefs.getInt(Prefs.PREFS_GOAL_SET_YEAR, Calendar.getInstance().get(Calendar.YEAR));

        updateGoalRow(R.id.goal1yRow, R.id.goal1yLabel, R.id.goal1yBar, R.id.goal1yPercent,
                g1, current, setYear + 1);
        updateGoalRow(R.id.goal3yRow, R.id.goal3yLabel, R.id.goal3yBar, R.id.goal3yPercent,
                g3, current, setYear + 3);
        updateGoalRow(R.id.goal5yRow, R.id.goal5yLabel, R.id.goal5yBar, R.id.goal5yPercent,
                g5, current, setYear + 5);
    }

    private void updateGoalRow(int rowId, int labelId, int barId, int percentId,
                                float goal, double current, int targetYear) {
        View row = findViewById(rowId);
        if (goal <= 0) {
            row.setVisibility(View.GONE);
            return;
        }
        row.setVisibility(View.VISIBLE);
        ((TextView) findViewById(labelId)).setText(String.valueOf(targetYear));

        int progress = (int) Math.min(Math.max(current / goal * 100, 0), 100);
        ((android.widget.ProgressBar) findViewById(barId)).setProgress(progress);
        ((TextView) findViewById(percentId)).setText(getString(R.string.helper_text, String.valueOf(progress)).replace("Click to add ", "").replace(" to this month", "%"));
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
                    month.setYear(selectedYear);
                    month.setMonth(selectedMonth);
                    closeAssetView();
                    updateViews();
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
                    updateViews();
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
