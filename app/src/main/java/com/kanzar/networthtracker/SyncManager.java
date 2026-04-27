package com.kanzar.networthtracker;

import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.annotation.NonNull;
import androidx.credentials.ClearCredentialStateRequest;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.ClearCredentialException;
import androidx.credentials.exceptions.GetCredentialException;

import com.google.android.gms.auth.api.identity.AuthorizationRequest;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Tasks;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.kanzar.networthtracker.api.repositories.DriveServiceHelper;
import com.kanzar.networthtracker.helpers.Prefs;
import com.kanzar.networthtracker.models.Asset;
import com.kanzar.networthtracker.statistics.Events;
import com.kanzar.networthtracker.statistics.events.SigninCompleted;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.realm.Realm;

public class SyncManager {

    public interface SyncListener {
        void onSyncStarted();
        void onSyncFinished();
        void onLoginStateChanged(boolean signedIn);
    }

    private static final Gson GSON = new com.google.gson.GsonBuilder().serializeSpecialFloatingPointValues().create();
    private final MainActivity activity;
    private final SyncListener listener;
    private final CredentialManager credentialManager;
    private DriveServiceHelper driveServiceHelper;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final ActivityResultLauncher<IntentSenderRequest> authLauncher;

    public SyncManager(MainActivity activity, SyncListener listener, ActivityResultLauncher<IntentSenderRequest> authLauncher) {
        this.activity = activity;
        this.listener = listener;
        this.authLauncher = authLauncher;
        this.credentialManager = CredentialManager.create(activity);
    }

    public void startSignIn() {
        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(activity.getString(R.string.server_client_id))
                .setAutoSelectEnabled(false)
                .build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        credentialManager.getCredentialAsync(
                activity, request, new android.os.CancellationSignal(), executorService,
                new CredentialManagerCallback<>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        handleSignInCredential(result.getCredential());
                    }
                    @Override
                    public void onError(@NonNull GetCredentialException e) {
                        Log.e("SyncManager", "Sign in failed", e);
                        activity.runOnUiThread(() ->
                                Toast.makeText(activity, "Sign in failed. Please try again.", Toast.LENGTH_SHORT).show());
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
                    Log.e("SyncManager", "Failed to parse Google credential", e);
                }
            }
        }

        if (googleCred == null) return;

        String email = googleCred.getId();
        Prefs.save(Prefs.PREFS_USER_EMAIL, email);
        Prefs.save(Prefs.PREFS_TOKEN, "signed_in");
        new Events().send(new SigninCompleted());
        activity.runOnUiThread(() -> {
            listener.onLoginStateChanged(true);
            Toast.makeText(activity, "Signed in as " + email, Toast.LENGTH_SHORT).show();
        });
        requestDriveAuthorizationAndSync();
    }

    public void requestDriveAuthorizationAndSync() {
        AuthorizationRequest authRequest = AuthorizationRequest.builder()
                .setRequestedScopes(Collections.singletonList(new Scope(DriveScopes.DRIVE_APPDATA)))
                .build();

        Identity.getAuthorizationClient(activity)
                .authorize(authRequest)
                .addOnSuccessListener(authResult -> {
                    if (authResult.hasResolution()) {
                        try {
                            android.app.PendingIntent pi = authResult.getPendingIntent();
                            if (pi != null) {
                                authLauncher.launch(new IntentSenderRequest.Builder(pi.getIntentSender()).build());
                            }
                        } catch (Exception e) {
                            Log.e("SyncManager", "Auth resolution failed", e);
                        }
                    } else {
                        String accessToken = authResult.getAccessToken();
                        if (accessToken != null) {
                            initDriveServiceAndSync(accessToken);
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("SyncManager", "Drive authorization failed", e));
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
            Log.e("SyncManager", "Drive init failed", e);
        }
    }

    public void signOut() {
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
                        activity.runOnUiThread(() -> listener.onLoginStateChanged(false));
                    }
                    @Override public void onError(@NonNull ClearCredentialException e) {
                        activity.runOnUiThread(() -> listener.onLoginStateChanged(false));
                    }
                });
    }

    public void triggerSync() {
        if (!Prefs.contains(Prefs.PREFS_TOKEN)) return;

        activity.runOnUiThread(listener::onSyncStarted);

        AuthorizationRequest authRequest = AuthorizationRequest.builder()
                .setRequestedScopes(Collections.singletonList(new Scope(DriveScopes.DRIVE_APPDATA)))
                .build();

        Identity.getAuthorizationClient(activity)
                .authorize(authRequest)
                .addOnSuccessListener(authResult -> {
                    if (authResult.hasResolution()) {
                        activity.runOnUiThread(listener::onSyncFinished);
                        return;
                    }
                    String accessToken = authResult.getAccessToken();
                    if (accessToken == null) {
                        activity.runOnUiThread(listener::onSyncFinished);
                        return;
                    }
                    initDriveServiceAndSync(accessToken);
                })
                .addOnFailureListener(e -> activity.runOnUiThread(() -> {
                    listener.onSyncFinished();
                    Toast.makeText(activity, R.string.sync_failed, Toast.LENGTH_SHORT).show();
                }));
    }

    private void performSync() {
        executorService.execute(() -> {
            try (Realm realm = Realm.getDefaultInstance()) {
                String fileId = Tasks.await(driveServiceHelper.searchFile("assets.json"));

                boolean localEmpty = realm.where(Asset.class).count() == 0;
                if (fileId != null && localEmpty) {
                    String json = Tasks.await(driveServiceHelper.readFile(fileId));
                    List<Asset> remoteAssets = GSON.fromJson(json, new TypeToken<List<Asset>>(){}.getType());
                    if (remoteAssets != null && !remoteAssets.isEmpty()) {
                        realm.executeTransaction(r -> r.copyToRealmOrUpdate(remoteAssets));
                    }
                }

                List<Asset> localAssets = realm.where(Asset.class).findAll();
                String localJson = GSON.toJson(realm.copyFromRealm(localAssets));

                if (fileId == null) {
                    Tasks.await(driveServiceHelper.createFile("assets.json", localJson));
                } else {
                    Tasks.await(driveServiceHelper.updateFile(fileId, localJson));
                }

                activity.runOnUiThread(() -> {
                    listener.onSyncFinished();
                });
            } catch (Exception e) {
                Log.e("SyncManager", "Drive sync error", e);
                activity.runOnUiThread(() -> {
                    listener.onSyncFinished();
                    Toast.makeText(activity, R.string.sync_failed, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    public void shutdown() {
        executorService.shutdown();
    }
}
