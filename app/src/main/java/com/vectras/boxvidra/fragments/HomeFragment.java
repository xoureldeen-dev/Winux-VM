package com.vectras.boxvidra.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.system.ErrnoException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import com.termux.app.TermuxActivity;
import com.vectras.boxvidra.R;
import com.vectras.boxvidra.core.ObbParser;
import com.vectras.boxvidra.core.TermuxX11;
import com.vectras.boxvidra.services.MainService;

import static android.os.Build.VERSION.SDK_INT;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class HomeFragment extends Fragment implements View.OnClickListener {

    private MaterialButton startX11Btn, startVidraBtn, openTerminalBtn, stopX11Btn;
    private TextView tvAppInfo, termuxX11TextView, xfce4TextView, tvLogger;

    private boolean isX11Started = false;
    private boolean isXFCE4Started = false;

    private BroadcastReceiver logBroadcastReceiver;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        tvAppInfo = view.findViewById(R.id.tvAppInfo);

        startX11Btn = view.findViewById(R.id.startX11);
        startX11Btn.setOnClickListener(this);

        stopX11Btn = view.findViewById(R.id.stopX11);
        stopX11Btn.setOnClickListener(this);

        startVidraBtn = view.findViewById(R.id.startVidra);
        startVidraBtn.setOnClickListener(this);

        openTerminalBtn = view.findViewById(R.id.openTerminal);
        openTerminalBtn.setOnClickListener(this);

        termuxX11TextView = view.findViewById(R.id.tvIsTermuxX11);
        xfce4TextView = view.findViewById(R.id.tvIsXfce4);
        tvLogger = view.findViewById(R.id.tvLogger);
        tvLogger.setTypeface(Typeface.MONOSPACE);

        updateUI(); // Update UI based on current states

        return view;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.startX11) {
            if (!isTermuxX11Installed()) {
                showInstallTermuxX11Dialog();
            } else {
                startTermuxX11();
            }
        } else if (id == R.id.stopX11) {
            stopTermuxX11();
        } else if (id == R.id.startVidra) {
            Intent serviceIntent = new Intent(requireActivity(), MainService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                requireActivity().startForegroundService(serviceIntent);
            } else {
                requireActivity().startService(serviceIntent);
            }
            isXFCE4Started = true;
            startVidraBtn.setEnabled(false);
            xfce4TextView.setText(R.string.boxvidra_service_yes);
        } else if (id == R.id.openTerminal) {
            startActivity(new Intent(requireActivity(), TermuxActivity.class));
        }
    }

    private void updateUI() {
        // Update App Info
        try {
            JSONObject jsonObject = ObbParser.obbParse(getActivity());
            String info = ObbParser.parseJsonAndFormat(jsonObject);
            tvAppInfo.setText(info);
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            tvAppInfo.setText(R.string.failed_to_load_data);
        }

        // Update Termux X11 status
        if (isX11Started) {
            termuxX11TextView.setText(R.string.termuxx11_service_yes);
            startX11Btn.setEnabled(false);
            stopX11Btn.setVisibility(View.VISIBLE);
        } else {
            termuxX11TextView.setText(R.string.termuxx11_service_no);
            startX11Btn.setEnabled(true);
            stopX11Btn.setVisibility(View.GONE);
        }

        // Update XFCE4 status
        if (isXFCE4Started) {
            xfce4TextView.setText(R.string.boxvidra_service_yes);
        } else {
            xfce4TextView.setText(R.string.boxvidra_service_no);
        }
    }

    private boolean isTermuxX11Installed() {
        PackageManager pm = requireContext().getPackageManager();
        try {
            PackageInfo info = pm.getPackageInfo("com.termux.x11", 0);
            return info != null;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private void showInstallTermuxX11Dialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Termux X11 Not Installed")
                .setMessage("Termux X11 is required to start X11. Would you like to install it now?")
                .setPositiveButton("Install", (dialog, which) -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse("https://github.com/termux/termux-x11/releases"));
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void startTermuxX11() {
        try {
            isX11Started = true;
            startX11Btn.setEnabled(false);
            stopX11Btn.setVisibility(View.VISIBLE);
            termuxX11TextView.setText(R.string.termuxx11_service_yes);
            // Assuming TermuxX11.main() starts the X11 service
            TermuxX11.main(new String[]{":0"});
        } catch (ErrnoException e) {
            isX11Started = false;
            startX11Btn.setEnabled(true);
            stopX11Btn.setVisibility(View.GONE);
            termuxX11TextView.setText(R.string.termuxx11_service_no);
        }
    }

    private void stopTermuxX11() {
        isX11Started = false;
        startX11Btn.setEnabled(true);
        stopX11Btn.setVisibility(View.GONE);
        termuxX11TextView.setText(R.string.termuxx11_service_no);

        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", "com.termux.x11", null));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Update UI based on current states
        updateUI();
        // Register broadcast receiver
        registerLogReceiver();
    }

    @Override
    public void onPause() {
        super.onPause();
        // Unregister broadcast receiver
        unregisterLogReceiver();
    }

    private void registerLogReceiver() {
        logBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction() != null && intent.getAction().equals("ACTION_LOG_MESSAGE")) {
                    String logMessage = intent.getStringExtra("logMessage");
                    updateLoggerUI(logMessage);
                }
            }
        };
        IntentFilter intentFilter = new IntentFilter("ACTION_LOG_MESSAGE");
        requireActivity().registerReceiver(logBroadcastReceiver, intentFilter);
    }

    private void unregisterLogReceiver() {
        if (logBroadcastReceiver != null) {
            requireActivity().unregisterReceiver(logBroadcastReceiver);
            logBroadcastReceiver = null;
        }
    }

    private void updateLoggerUI(String logMessage) {
        // Update UI on the main thread
        new Handler(Looper.getMainLooper()).post(() -> {
            tvLogger.append(logMessage + "\n");
            // Scroll to the bottom of the TextView (if it's a scrolling log)
            // tvLogger.scrollTo(0, tvLogger.getLayout().getLineTop(tvLogger.getLineCount()));
        });
    }
}
