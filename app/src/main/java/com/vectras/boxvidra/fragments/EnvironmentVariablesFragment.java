package com.vectras.boxvidra.fragments;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.vectras.boxvidra.R;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class EnvironmentVariablesFragment extends Fragment {

    public static final String PREFS_NAME = "MyPrefs";
    public static final String ENVIRONMENT_VARS_KEY = "environmentVars";

    private ArrayList<String> environmentVariables;
    private ArrayAdapter<String> adapter;

    private TextInputEditText editTextEnvName;
    private TextInputEditText editTextEnvValue;
    private ListView listViewEnv;
    private Button buttonAddEnv;

    private SharedPreferences sharedPreferences;

    public EnvironmentVariablesFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initialize SharedPreferences
        sharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Load saved environment variables from SharedPreferences
        Set<String> savedVars = sharedPreferences.getStringSet(ENVIRONMENT_VARS_KEY, new HashSet<>());
        environmentVariables = new ArrayList<>(savedVars);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_environment_variables, container, false);

        editTextEnvName = view.findViewById(R.id.editTextEnvName);
        editTextEnvValue = view.findViewById(R.id.editTextEnvValue);
        listViewEnv = view.findViewById(R.id.listViewEnv);
        buttonAddEnv = view.findViewById(R.id.buttonAddEnv);

        adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, environmentVariables);
        listViewEnv.setAdapter(adapter);


        // Handle long click on list item to delete environment variable
        listViewEnv.setOnItemLongClickListener((parent, view1, position, id) -> {
            // Display confirmation dialog for deletion
            new AlertDialog.Builder(requireContext(), R.style.MainDialogTheme)
                    .setTitle("Delete Environment Variable")
                    .setMessage("Are you sure you want to delete this environment variable?")
                    .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Remove item from list and update adapter
                            environmentVariables.remove(position);
                            adapter.notifyDataSetChanged();

                            // Save updated environment variables to SharedPreferences
                            saveEnvironmentVariables();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();

            return true; // Consume long click
        });

        buttonAddEnv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String envName = editTextEnvName.getText().toString().trim();
                String envValue = editTextEnvValue.getText().toString().trim();

                if (!envName.isEmpty() && !envValue.isEmpty()) {
                    String envVar = envName + "=" + envValue;
                    environmentVariables.add(envVar);
                    adapter.notifyDataSetChanged();

                    // Save updated environment variables to SharedPreferences
                    saveEnvironmentVariables();

                    // Clear input fields after adding
                    editTextEnvName.getText().clear();
                    editTextEnvValue.getText().clear();
                }
            }
        });

        return view;
    }

    private void saveEnvironmentVariables() {
        // Save environment variables to SharedPreferences
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Set<String> envSet = new HashSet<>(environmentVariables);
        editor.putStringSet(ENVIRONMENT_VARS_KEY, envSet);
        editor.apply();
    }
}
