package com.vectras.boxvidra.fragments;
import android.os.Bundle;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.vectras.boxvidra.R;

public class OptionsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);

        // Handle clicks on preferences
        findPreference("key_environment").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                // Navigate to EnvironmentManagementFragment
                navigateToEnvironmentManagementFragment();
                return true;
            }
        });
    }

    private void navigateToEnvironmentManagementFragment() {
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, new EnvironmentVariablesFragment())
                .addToBackStack(null)
                .commit();
    }
}
