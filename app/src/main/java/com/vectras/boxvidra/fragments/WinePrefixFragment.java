package com.vectras.boxvidra.fragments;

import android.app.Dialog;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.color.MaterialColors;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.termux.app.TermuxService;
import com.vectras.boxvidra.R;
import com.vectras.boxvidra.adapters.WinePrefixAdapter;
import com.vectras.boxvidra.core.ObbParser;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.json.JSONException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class WinePrefixFragment extends Fragment {

    private RecyclerView recyclerView;
    private WinePrefixAdapter adapter;
    private List<File> winePrefixes;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_wine_prefix, container, false);
        recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2)); // 2 columns in the grid
        winePrefixes = new ArrayList<>();
        adapter = new WinePrefixAdapter(winePrefixes, getContext());
        recyclerView.setAdapter(adapter);
        loadWinePrefixes();

        FloatingActionButton fabAdd = view.findViewById(R.id.fabAdd);
        fabAdd.setOnClickListener(v -> showCreatePrefixDialog());
        return view;
    }

    private void loadWinePrefixes() {
        File root = new File(TermuxService.OPT_PATH + "/wine-prefixes");
        if (root.isDirectory()) {
            File[] files = root.listFiles();
            if (files != null) {
                winePrefixes.clear();
                for (File file : files) {
                    if (file.isDirectory() && new File(file, "drive_c").exists()) {
                        winePrefixes.add(file);
                    }
                }
                adapter.notifyDataSetChanged();
            }
        }
    }

    private void showCreatePrefixDialog() {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View dialogView = inflater.inflate(R.layout.dialog_create_prefix, null);
        EditText editTextPrefixName = dialogView.findViewById(R.id.edit_text_prefix_name);

        // Generate default name
        String defaultPrefixName = generateUniquePrefixName();
        editTextPrefixName.setText(defaultPrefixName);

        new AlertDialog.Builder(getContext(), R.style.MainDialogTheme)
                .setTitle("Create New Prefix")
                .setView(dialogView)
                .setPositiveButton("Create", (dialog, which) -> {
                    String prefixName = editTextPrefixName.getText().toString();
                    if (prefixName.isEmpty()) {
                        prefixName = defaultPrefixName;
                    }

                    // Check if the name already exists
                    if (!isPrefixNameExists(prefixName)) {
                        String tarGzPath;
                        try {
                            tarGzPath = TermuxService.OPT_PATH + "/wine/" + ObbParser.wineVersion(getActivity()) + "/container-amd64.tar.gz";
                        } catch (IOException | JSONException e) {
                            throw new RuntimeException(e);
                        }
                        new ExtractPrefixTask().execute(prefixName, tarGzPath);
                    } else {
                        Toast.makeText(getContext(), "Prefix name already exists!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private String generateUniquePrefixName() {
        int index = 1;
        String prefixName;
        File root = new File(TermuxService.OPT_PATH + "/wine-prefixes");
        do {
            prefixName = "Container - " + index++;
        } while (new File(root, prefixName).exists());
        return prefixName;
    }

    private boolean isPrefixNameExists(String prefixName) {
        File root = new File(TermuxService.OPT_PATH + "/wine-prefixes");
        File prefixDir = new File(root, prefixName);
        return prefixDir.exists();
    }

    private class ExtractPrefixTask extends AsyncTask<String, Integer, Boolean> {
        private Dialog progressDialog;
        private CircularProgressIndicator progressIndicator;
        private TextView progressMessage;

        @Override
        protected void onPreExecute() {
            progressDialog = new Dialog(getContext(), R.style.TransparentProgressDialog);
            progressDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            progressDialog.setContentView(R.layout.progress_dialog);
            progressDialog.setCancelable(false);

            progressIndicator = progressDialog.findViewById(R.id.progress_circular);
            progressMessage = progressDialog.findViewById(R.id.progress_message);

            int colorPrimary = ContextCompat.getColor(getContext(), R.color.purple_500);
            int colorAccent = ContextCompat.getColor(getContext(), R.color.purple_200);
            progressIndicator.setIndicatorColor(colorPrimary);
            progressIndicator.setTrackColor(colorAccent);

            Window window = progressDialog.getWindow();
            if (window != null) {
                WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
                layoutParams.copyFrom(window.getAttributes());
                layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
                layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
                layoutParams.gravity = Gravity.CENTER;
                window.setAttributes(layoutParams);
                window.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
            }

            progressDialog.show();
        }

        @Override
        protected Boolean doInBackground(String... params) {
            String prefixName = params[0];
            String tarGzPath = params[1];
            File prefixDir = new File(TermuxService.OPT_PATH + "/wine-prefixes", prefixName);
            if (!prefixDir.exists()) {
                prefixDir.mkdirs();
            }
            try {
                extractTarGzFile(new FileInputStream(tarGzPath), prefixDir, tarGzPath);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            progressIndicator.setIndeterminate(false);
            progressIndicator.setProgress(values[0]);
        }

        @Override
        protected void onPostExecute(Boolean success) {
            progressDialog.dismiss();
            if (success) {
                loadWinePrefixes();
            } else {
                // Handle failure
            }
        }

        private void extractTarGzFile(InputStream is, File targetDir, String tarGzPath) throws IOException {
            try (GzipCompressorInputStream gis = new GzipCompressorInputStream(is);
                 TarArchiveInputStream tis = new TarArchiveInputStream(gis)) {
                TarArchiveEntry entry;
                long totalSize = 0;
                long extractedSize = 0;
                while ((entry = tis.getNextTarEntry()) != null) {
                    totalSize += entry.getSize();
                }
                is.close();

                is = new FileInputStream(tarGzPath); // Reopen the input stream
                try (GzipCompressorInputStream gis2 = new GzipCompressorInputStream(is);
                     TarArchiveInputStream tis2 = new TarArchiveInputStream(gis2)) {
                    while ((entry = tis2.getNextTarEntry()) != null) {
                        File destPath = new File(targetDir, entry.getName());
                        if (entry.isDirectory()) {
                            destPath.mkdirs();
                        } else {
                            destPath.getParentFile().mkdirs();
                            try (FileOutputStream fos = new FileOutputStream(destPath)) {
                                byte[] buffer = new byte[1024];
                                int len;
                                while ((len = tis2.read(buffer)) != -1) {
                                    fos.write(buffer, 0, len);
                                    extractedSize += len;
                                    publishProgress((int) ((extractedSize * 100) / totalSize));
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
