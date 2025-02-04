/*
 * Copyright (C) 2016 The CyanogenMod Project
 * Copyright (C) 2017-2020, 2022 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.libremobileos.setupwizard;

import static android.os.Binder.getCallingUserHandle;
import static android.os.UserHandle.USER_CURRENT;
import static android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_3BUTTON_OVERLAY;
import static android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_GESTURAL_OVERLAY;

import static com.libremobileos.setupwizard.Manifest.permission.FINISH_SETUP;
import static com.libremobileos.setupwizard.SetupWizardApp.ACTION_SETUP_COMPLETE;
import static com.libremobileos.setupwizard.SetupWizardApp.DISABLE_NAV_KEYS;
import static com.libremobileos.setupwizard.SetupWizardApp.ENABLE_RECOVERY_UPDATE;
import static com.libremobileos.setupwizard.SetupWizardApp.GAPPS_CONFIG;
import static com.libremobileos.setupwizard.SetupWizardApp.GAPPS_CONFIG_PROP;
import static com.libremobileos.setupwizard.SetupWizardApp.GAPPS_CONFIG_PROP2;
import static com.libremobileos.setupwizard.SetupWizardApp.LOGV;
import static com.libremobileos.setupwizard.SetupWizardApp.NAVIGATION_OPTION_KEY;
import static com.libremobileos.setupwizard.SetupWizardApp.UPDATE_RECOVERY_PROP;

import android.animation.Animator;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.om.IOverlayManager;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.widget.ImageView;

import com.google.android.setupcompat.util.SystemBarHelper;
import com.google.android.setupcompat.util.WizardManagerHelper;

import com.libremobileos.setupwizard.R;
import com.libremobileos.setupwizard.util.SetupWizardUtils;

public class FinishActivity extends BaseSetupWizardActivity {

    public static final String TAG = FinishActivity.class.getSimpleName();

    private ImageView mReveal;

    private SetupWizardApp mSetupWizardApp;

    private final Handler mHandler = new Handler();

    private volatile boolean mIsFinishing = false;
    private volatile boolean mWillReboot = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (LOGV) {
            logActivityState("onCreate savedInstanceState=" + savedInstanceState);
        }
        mSetupWizardApp = (SetupWizardApp) getApplication();
        mReveal = (ImageView) findViewById(R.id.reveal);
        setNextText(R.string.start);
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.finish_activity;
    }

    @Override
    public void finish() {
        super.finish();
        if (!isResumed() || mResultCode != RESULT_CANCELED) {
            overridePendingTransition(R.anim.translucent_enter, R.anim.translucent_exit);
        }
    }

    @Override
    public void onNavigateNext() {
        applyForwardTransition(TRANSITION_ID_NONE);
        startFinishSequence();
    }

    private void finishSetup() {
        if (!mIsFinishing) {
            mIsFinishing = true;
            setupRevealImage();
        }
    }

    private void startFinishSequence() {
        Intent i = new Intent(ACTION_SETUP_COMPLETE);
        i.setPackage(getPackageName());
        sendBroadcastAsUser(i, getCallingUserHandle(), FINISH_SETUP);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        mWillReboot = SystemProperties.getInt(GAPPS_CONFIG_PROP2, 0) == 1
                && SystemProperties.getInt(GAPPS_CONFIG_PROP, -1) == -1;
        if (mWillReboot) {
            new AlertDialog.Builder(this)
                .setTitle(R.string.setup_complete)
                .setMessage(R.string.device_reboot)
                .setPositiveButton(R.string.ok, (d, n) -> {
                    completeSetup();
                })
                .setCancelable(false)
                .show();
        } else {
            SystemBarHelper.hideSystemBars(getWindow());
            finishSetup();
        }
    }

    private void setupRevealImage() {
        final Point p = new Point();
        getWindowManager().getDefaultDisplay().getRealSize(p);
        final WallpaperManager wallpaperManager =
                WallpaperManager.getInstance(this);
        wallpaperManager.forgetLoadedWallpaper();
        final Bitmap wallpaper = wallpaperManager.getBitmap();
        Bitmap cropped = null;
        if (wallpaper != null) {
            cropped = Bitmap.createBitmap(wallpaper, 0,
                    0, Math.min(p.x, wallpaper.getWidth()),
                    Math.min(p.y, wallpaper.getHeight()));
        }
        if (cropped != null) {
            mReveal.setScaleType(ImageView.ScaleType.CENTER_CROP);
            mReveal.setImageBitmap(cropped);
        } else {
            mReveal.setBackground(wallpaperManager
                    .getBuiltInDrawable(p.x, p.y, false, 0, 0));
        }
        animateOut();
    }

    private void animateOut() {
        int cx = (mReveal.getLeft() + mReveal.getRight()) / 2;
        int cy = (mReveal.getTop() + mReveal.getBottom()) / 2;
        int finalRadius = Math.max(mReveal.getWidth(), mReveal.getHeight());
        Animator anim =
                ViewAnimationUtils.createCircularReveal(mReveal, cx, cy, 0, finalRadius);
        anim.setDuration(900);
        anim.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                mReveal.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        completeSetup();
                    }
                });
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });
        anim.start();
    }

    private void completeSetup() {
        handleNavKeys(mSetupWizardApp);
        handleRecoveryUpdate(mSetupWizardApp);
        handleGappsConfig(mSetupWizardApp);
        handleNavigationOption(mSetupWizardApp);
        final WallpaperManager wallpaperManager =
                WallpaperManager.getInstance(mSetupWizardApp);
        wallpaperManager.forgetLoadedWallpaper();
        if (mWillReboot) {
            mHandler.postDelayed(() -> {
                PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                try {
                    pm.reboot("userspace");
                } catch (UnsupportedOperationException ignored) {
                    pm.reboot(null);
                }
            }, 10000);
        } else {
            finishAllAppTasks();
        }
        SetupWizardUtils.enableStatusBar(this);
        Intent intent = WizardManagerHelper.getNextIntent(getIntent(),
                Activity.RESULT_OK);
        startActivityForResult(intent, NEXT_REQUEST);
    }

    private static void handleNavKeys(SetupWizardApp setupWizardApp) {
        if (setupWizardApp.getSettingsBundle().containsKey(DISABLE_NAV_KEYS)) {
            writeDisableNavkeysOption(setupWizardApp,
                    setupWizardApp.getSettingsBundle().getBoolean(DISABLE_NAV_KEYS));
        }
    }

    private static void handleGappsConfig(SetupWizardApp setupWizardApp) {
        if (setupWizardApp.getSettingsBundle().containsKey(GAPPS_CONFIG)) {
            int val = setupWizardApp.getSettingsBundle()
                    .getInt(GAPPS_CONFIG);

            SystemProperties.set(GAPPS_CONFIG_PROP, String.valueOf(val));
        }
    }

    private static void handleRecoveryUpdate(SetupWizardApp setupWizardApp) {
        if (setupWizardApp.getSettingsBundle().containsKey(ENABLE_RECOVERY_UPDATE)) {
            boolean update = setupWizardApp.getSettingsBundle()
                    .getBoolean(ENABLE_RECOVERY_UPDATE);

            SystemProperties.set(UPDATE_RECOVERY_PROP, String.valueOf(update));
        }
    }

    private void handleNavigationOption(Context context) {
        Bundle settingsBundle = mSetupWizardApp.getSettingsBundle();
        if (settingsBundle.containsKey(NAVIGATION_OPTION_KEY)) {
            IOverlayManager overlayManager = IOverlayManager.Stub.asInterface(
                    ServiceManager.getService(Context.OVERLAY_SERVICE));
            String selectedNavMode = settingsBundle.getString(NAVIGATION_OPTION_KEY);

            try {
                overlayManager.setEnabledExclusiveInCategory(selectedNavMode, USER_CURRENT);
            } catch (Exception e) {}
        }
    }

    private static void writeDisableNavkeysOption(Context context, boolean enabled) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        final boolean virtualKeysEnabled = Settings.System.getIntForUser(
                context.getContentResolver(), Settings.System.FORCE_SHOW_NAVBAR, 0,
                UserHandle.USER_CURRENT) != 0;
        if (enabled != virtualKeysEnabled) {
            Settings.System.putIntForUser(context.getContentResolver(),
                    Settings.System.FORCE_SHOW_NAVBAR, enabled ? 1 : 0,
                    UserHandle.USER_CURRENT);
        }
    }
}
