package com.vectras.boxvidra.adapters;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.system.ErrnoException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.vectras.boxvidra.R;
import com.vectras.boxvidra.activities.MainActivity;
import com.vectras.boxvidra.core.TermuxX11;
import com.vectras.boxvidra.fragments.HomeFragment;
import com.vectras.boxvidra.services.MainService;

import java.io.File;
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

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        File winePrefix = winePrefixes.get(position);
        holder.textView.setText(winePrefix.getName());
    }

    @Override
    public int getItemCount() {
        return winePrefixes.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        TextView textView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.TVTitle);
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int position = getAdapterPosition();
            File winePrefix = winePrefixes.get(position);
            Intent serviceIntent = new Intent(MainActivity.activity, MainService.class);
            serviceIntent.putExtra("WINEPREFIX", winePrefix.getName());
            MainActivity.activity.startForegroundService(serviceIntent);
            try {
                TermuxX11.main(new String[]{":0"});
            } catch (ErrnoException e) {
                throw new RuntimeException(e);
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