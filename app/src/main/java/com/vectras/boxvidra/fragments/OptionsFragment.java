package com.vectras.boxvidra.fragments;
import android.content.Intent;
import android.os.Bundle;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.termux.app.TermuxActivity;
import com.vectras.boxvidra.R;

public class OptionsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);

        findPreference("key_environment").setOnPreferenceClickListener(preference -> {
            navigateToEnvironmentManagementFragment();
            return true;
        });

        findPreference("key_terminal").setOnPreferenceClickListener(preference -> {
            Intent intent = new Intent(getActivity(), TermuxActivity.class);
            startActivity(intent);
            return true;
        });

    }

    private void navigateToEnvironmentManagementFragment() {
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, new EnvironmentVariablesFragment())
                .addToBackStack(null)
                .commit();
    }
}
