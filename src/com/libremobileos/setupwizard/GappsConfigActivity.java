/*
 * Copyright (C) 2022 The Android Open Source Project
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

import static com.libremobileos.setupwizard.SetupWizardApp.GAPPS_CONFIG;
import static com.libremobileos.setupwizard.SetupWizardApp.GAPPS_CONFIG_PROP;
import static com.libremobileos.setupwizard.SetupWizardApp.GAPPS_CONFIG_PROP2;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemProperties;
import android.util.Log;
import android.view.View;
import android.widget.RadioButton;

import com.google.android.setupcompat.util.WizardManagerHelper;

import com.libremobileos.setupwizard.R;

public class GappsConfigActivity extends BaseSetupWizardActivity {

    private RadioButton gapps;
    private RadioButton vanilla;
    private RadioButton microg;
    private RadioButton microgPs;
    private RadioButton microgS;

    private int selected;
    private SetupWizardApp mSetupWizardApp;
    private static boolean sFirstTime = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSetupWizardApp = (SetupWizardApp) getApplication();
        getGlifLayout().setDescriptionText(getString(R.string.gapps_config_desc));

        if (SystemProperties.getInt(GAPPS_CONFIG_PROP2, 0) != 1) {
            Log.v(TAG, "No gms feature, skipping GappsConfigActivity");

            Intent intent = WizardManagerHelper.getNextIntent(getIntent(), Activity.RESULT_OK);
            nextAction(NEXT_REQUEST, intent);
            finish();
            return;
        }

        setNextText(R.string.next);

        gapps = findViewById(R.id.gapps_config_radio1);
        View gappsView = findViewById(R.id.gapps_config_radiobutton1_view);
        gappsView.setOnClickListener(v -> {
            setSelected(0);
        });

        vanilla = findViewById(R.id.gapps_config_radio2);
        View vanillaView = findViewById(R.id.gapps_config_radiobutton2_view);
        vanillaView.setOnClickListener(v -> {
            setSelected(2);
        });

        microg = findViewById(R.id.gapps_config_radio3);
        View microgView = findViewById(R.id.gapps_config_radiobutton3_view);
        microgView.setOnClickListener(v -> {
            setSelected(1);
        });

        microgPs = findViewById(R.id.gapps_config_radio4);
        View microgPsView = findViewById(R.id.gapps_config_radiobutton4_view);
        microgPsView.setOnClickListener(v -> {
            setSelected(3);
        });

        microgS = findViewById(R.id.gapps_config_radio5);
        View microgSView = findViewById(R.id.gapps_config_radiobutton5_view);
        microgSView.setOnClickListener(v -> {
            setSelected(4);
        });

        // Allow overriding the default state
        if (sFirstTime) {
            mSetupWizardApp.getSettingsBundle().putInt(GAPPS_CONFIG,
                    SystemProperties.getInt(GAPPS_CONFIG_PROP, 0));

            final int select = mSetupWizardApp.getSettingsBundle().getInt(GAPPS_CONFIG, 0);
            setSelected(select);
        }

        sFirstTime = false;
    }

    private void setSelected(int id) {
        gapps.setChecked(id == 0);
        vanilla.setChecked(id == 2); /* 2 and 1 are swapped to match prop value */
        microg.setChecked(id == 1);
        microgPs.setChecked(id == 3);
        microgS.setChecked(id == 4);
        selected = id;
    }

    @Override
    public void onResume() {
        super.onResume();

        final Bundle myPageBundle = mSetupWizardApp.getSettingsBundle();
        final int select = myPageBundle.getInt(GAPPS_CONFIG, 0);
        setSelected(select);
    }

    @Override
    protected void onNextPressed() {
        mSetupWizardApp.getSettingsBundle().putInt(GAPPS_CONFIG,
               selected);

        Intent intent = WizardManagerHelper.getNextIntent(getIntent(), Activity.RESULT_OK);
        nextAction(NEXT_REQUEST, intent);
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.gapps_config;
    }

    @Override
    protected int getTitleResId() {
        return R.string.gapps_config;
    }

    @Override
    protected int getIconResId() {
        return R.drawable.ic_features;
    }
}
