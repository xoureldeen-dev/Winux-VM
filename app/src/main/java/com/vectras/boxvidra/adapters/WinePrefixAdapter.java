package com.vectras.boxvidra.adapters;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.system.ErrnoException;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.vectras.boxvidra.R;
import com.vectras.boxvidra.activities.MainActivity;
import com.vectras.boxvidra.core.TermuxX11;
import com.vectras.boxvidra.services.MainService;
import com.vectras.boxvidra.utils.BoxvidraUtils;
import com.vectras.boxvidra.utils.JsonUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class WinePrefixAdapter extends RecyclerView.Adapter<WinePrefixAdapter.ViewHolder> {

    private List<File> winePrefixes;
    private Context context;

    public WinePrefixAdapter(List<File> winePrefixes, Context context) {
        this.winePrefixes = winePrefixes;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_wine_prefix, parent, false);
        return new ViewHolder(view);
    }

    private boolean isMainServiceRunning() {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (MainService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void showOptionsDialog(ViewHolder holder, File winePrefix) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_prefix_options, null);

        SwitchMaterial switchWine64 = dialogView.findViewById(R.id.switchWine64);
        SwitchMaterial switchStartXFCE4 = dialogView.findViewById(R.id.switchStartXFCE4);

        // Load saved options if available
        File optionsFile = new File(winePrefix, "options.json");
        if (optionsFile.exists()) {
            try {
                JSONObject options = JsonUtils.loadOptionsFromJson(optionsFile);
                switchWine64.setChecked(options.getBoolean("wine64"));
                switchStartXFCE4.setChecked(options.getBoolean("startxfce4"));
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.MainDialogTheme)
                .setTitle("Choose Command")
                .setView(dialogView)
                .setPositiveButton("OK", (dialog, which) -> {
                    if (!switchWine64.isChecked() && !switchStartXFCE4.isChecked()) {
                        Toast.makeText(context, "At least one option must be enabled!", Toast.LENGTH_SHORT).show();
                        showOptionsDialog(holder, winePrefix);
                    } else {
                        JSONObject jsonObject = new JSONObject();
                        try {
                            jsonObject.put("wine64", switchWine64.isChecked());
                            jsonObject.put("startxfce4", switchStartXFCE4.isChecked());

                            JsonUtils.saveOptionsToJson(optionsFile, jsonObject);
                        } catch (JSONException | IOException e) {
                            e.printStackTrace();
                            Toast.makeText(context, "Failed to save options!", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancel", null);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        File winePrefix = winePrefixes.get(position);
        holder.textView.setText(winePrefix.getName());

        boolean isServiceRunning = isMainServiceRunning();
        holder.itemView.setEnabled(!isServiceRunning);
        holder.menuButton.setEnabled(!isServiceRunning);

        if (isServiceRunning) {
            holder.itemView.setOnClickListener(null);
            holder.itemView.setOnLongClickListener(null);
        } else {
            holder.itemView.setOnClickListener(holder);
            holder.itemView.setOnLongClickListener(holder);
        }
    }

    @Override
    public int getItemCount() {
        return winePrefixes.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        TextView textView;
        ImageButton menuButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.TVTitle);
            menuButton = itemView.findViewById(R.id.BTMenu);

            menuButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                File winePrefix = winePrefixes.get(position);
                showOptionsDialog(this, winePrefix);
            });

            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int position = getAdapterPosition();
            File winePrefix = winePrefixes.get(position);
            Intent serviceIntent = new Intent(MainActivity.activity, MainService.class);
            BoxvidraUtils.prefixName = winePrefix.getName();
            MainActivity.activity.startForegroundService(serviceIntent);

            if (!isTermuxX11Installed()) {
                showInstallTermuxX11Dialog();
                return;
            }

            try {
                TermuxX11.main(new String[]{":0"});
            } catch (ErrnoException e) {
                throw new RuntimeException(e);
            }
        }

        private boolean isTermuxX11Installed() {
            PackageManager pm = MainActivity.activity.getPackageManager();
            try {
                PackageInfo info = pm.getPackageInfo("com.termux.x11", 0);
                return (info != null);
            } catch (PackageManager.NameNotFoundException e) {
                return false;
            }
        }

        private void showInstallTermuxX11Dialog() {
            SpannableString spannableString = new SpannableString("Please install the Termux X11 plugin from: https://github.com/termux/termux-x11/releases");
            ClickableSpan clickableSpan = new ClickableSpan() {
                @Override
                public void onClick(View widget) {
                    String url = "https://github.com/termux/termux-x11/releases";
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    MainActivity.activity.startActivity(intent);
                }
            };

            int start = spannableString.toString().indexOf("https://github.com/termux/termux-x11/releases");
            int end = start + "https://github.com/termux/termux-x11/releases".length();
            spannableString.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            AlertDialog dialog = new AlertDialog.Builder(MainActivity.activity, R.style.MainDialogTheme)
                    .setTitle("Install Termux X11 Plugin")
                    .setMessage(spannableString)
                    .setPositiveButton("OK", null)
                    .create();

            dialog.show();
            TextView textView = dialog.findViewById(android.R.id.message);
            if (textView != null) {
                textView.setMovementMethod(LinkMovementMethod.getInstance());
            }
        }

        @Override
        public boolean onLongClick(View v) {
            int position = getAdapterPosition();
            File winePrefix = winePrefixes.get(position);
            // Confirm before deleting
            new AlertDialog.Builder(context, R.style.MainDialogTheme)
                    .setTitle("Delete Prefix")
                    .setMessage("Are you sure you want to delete this prefix?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        // Delete the prefix directory
                        deleteDirectory(winePrefix);
                        // Remove the item from the list and notify the adapter
                        winePrefixes.remove(position);
                        notifyItemRemoved(position);
                    })
                    .setNegativeButton("No", null)
                    .show();
            return true;
        }
    }

    private void deleteDirectory(File dir) {
        if (dir.isDirectory()) {
            File[] children = dir.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteDirectory(child);
                }
            }
        }
        dir.delete();
    }
}
