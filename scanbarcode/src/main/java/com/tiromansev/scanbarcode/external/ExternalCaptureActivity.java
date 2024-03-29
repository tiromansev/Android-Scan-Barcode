package com.tiromansev.scanbarcode.external;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.preference.PreferenceManager;

import com.tiromansev.scanbarcode.PreferenceActivity;
import com.tiromansev.scanbarcode.PreferencesFragment;
import com.tiromansev.scanbarcode.R;
import com.tiromansev.scanbarcode.TouchableRadioButton;
import com.tiromansev.scanbarcode.zxing.BeepManager;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class ExternalCaptureActivity extends AppCompatActivity {

    public static final String BARCODE = "BARCODE";

    public ImageButton btnKeyboard;
    public ImageButton btnSendLog;
    public ImageButton btnSettings;
    public EditText edtBarcode;
    public TextView tvBarcode;
    public Button btnClose;
    public TouchableRadioButton rbMode1;
    public TouchableRadioButton rbMode2;
    public TextView tvMode1;
    public TextView tvMode2;
    public SwitchCompat swDiagnosticMode;
    public static final String ENTER_SYMBOL = "0";
    public static final String TAB_SYMBOL = "1";
    public static final String SPACE_SYMBOL = "2";
    private int divider = KeyEvent.KEYCODE_ENTER;
    public BeepManager beepManager;
    private static final int PREFS_REQUEST = 99;
    private String barcode = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_external_capture);
        edtBarcode = findViewById(R.id.edtBarcode);
        tvBarcode = findViewById(R.id.tvBarcode);
        btnClose = findViewById(R.id.btnClose);
        btnKeyboard = findViewById(R.id.btnKeyboard);
        btnSettings = findViewById(R.id.btnScanSettings);
        btnSendLog = findViewById(R.id.btnSendLog);
        rbMode1 = findViewById(R.id.rbMode1);
        rbMode2 = findViewById(R.id.rbMode2);
        tvMode1 = findViewById(R.id.tvMode1);
        tvMode2 = findViewById(R.id.tvMode2);
        swDiagnosticMode = findViewById(R.id.swDiagnosticMode);
        edtBarcode.getBackground().mutate().setColorFilter(getResources().getColor(R.color.color_external_caption), PorterDuff.Mode.SRC_ATOP);
        tvBarcode.getBackground().mutate().setColorFilter(getResources().getColor(R.color.color_external_caption), PorterDuff.Mode.SRC_ATOP);

        btnKeyboard.setOnClickListener(v -> keyboardClicked());
        btnSendLog.setOnClickListener(view -> sendLog());
        setProperties();
        beepManager = new BeepManager(this);
        btnSettings.setOnClickListener(v -> {
            Log.d("external_scan", "settings clicked");
            Intent intent = getPrefsIntent();
            startActivityForResult(intent, PREFS_REQUEST);
        });

        btnClose.setOnClickListener(v -> closeView(edtBarcode.getText().toString()));

        edtBarcode.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                finish();
                return true;
            }
            if (!useInputField()) {
                return false;
            }

            int keyAction = event.getAction();
            int ch = event.getUnicodeChar();
            boolean log = useLog();
            String lastSymbol = getLastSymbol();

            if (log) {
                log(getTime() + ": [field handle]" +
                        " keyAction = " + keyAction +
                        " keyCode = " + keyCode +
                        " ch = " + ch +
                        " last symbol = " + lastSymbol);
            }

            String barcode = edtBarcode.getText().toString();

            if (log) {
                log(getTime() + ": [field handle] " + "barcode = " + barcode);
            }

            if (keyCode == divider) {
                if (log) {
                    log(getTime() + ": [field handle] " + "barcode handled = " + barcode + "\n");
                }
                handleBarcode(barcode);
                edtBarcode.setText(null);
                return true;
            }

            return false;
        });

        TouchableRadioButton.OnChangeListener mode1Listener = checked -> {
            rbMode2.setChecked(false);
            setUseInputField(true);
            setLayout();
            restartScan();
        };
        TouchableRadioButton.OnChangeListener mode2Listener = checked -> {
            rbMode1.setChecked(false);
            setUseInputField(false);
            setLayout();
            restartScan();
        };

        boolean useInput = useInputField();
        rbMode2.setChecked(!useInput);
        rbMode1.setChecked(useInput);
        setLayout();
        rbMode2.setChangeListener(mode2Listener);
        rbMode1.setChangeListener(mode1Listener);
        swDiagnosticMode.setOnCheckedChangeListener((compoundButton, checked) -> {
            setUseLog(checked);
            btnSendLog.setVisibility(useLog() ? View.VISIBLE : View.GONE);
        });
    }

    protected void sendLog() {

    }

    protected void keyboardClicked() {

    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();

        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
            return true;
        }
        if (useInputField()) {
            return super.dispatchKeyEvent(event);
        }

        int keyAction = event.getAction();
        int ch = event.getUnicodeChar();
        boolean log = useLog();
        String lastSymbol = getLastSymbol();

        Log.d("external_scan", "keyAction = " + keyAction +
                " keyCode = " + keyCode +
                " ch = " + ch +
                " last symbol = " + lastSymbol);
        if (log) {
            log(getTime() + ": [activity handle]" +
                    " keyAction = " + keyAction +
                    " keyCode = " + keyCode +
                    " ch = " + ch +
                    " last symbol = " + lastSymbol);
        }

        if (keyAction == KeyEvent.ACTION_DOWN) {
            if (keyCode == divider) {
                Log.d("external_scan", "barcode handled = " + barcode + "\n");
                if (log) {
                    log(getTime() + ": [activity handle] " + "barcode handled = " + barcode + "\n");
                }
                handleBarcode(barcode);
                resetBarcode();
                return true;
            }

            if (ch > 0) {
                barcode += (char) ch;
                tvBarcode.setText(barcode);
            }
            Log.d("external_scan", "barcode = " + barcode);
            if (log) {
                log(getTime() + ": [activity handle] " + "barcode = " + barcode);
            }

            return true;
        }

        return super.dispatchKeyEvent(event);
    }

    private String getTime() {
        Date date = Calendar.getInstance().getTime();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-mm-dd hh:mm:ss");
        return dateFormat.format(date);
    }

    private void setLayout() {
        boolean useInput = useInputField();

        edtBarcode.setVisibility(useInput ? View.VISIBLE : View.GONE);
        btnClose.setVisibility(useInput ? View.VISIBLE : View.GONE);
        tvBarcode.setVisibility(useInput ? View.GONE : View.VISIBLE);
        btnSendLog.setVisibility(useLog() ? View.VISIBLE : View.GONE);
        swDiagnosticMode.setChecked(useLog());

        if (useInput) {
            setFocus();
        }
    }

    private void resetBarcode() {
        barcode = "";
        tvBarcode.setText("");
    }

    public void closeView(String barcode) {

    }

    protected void log(String log) {

    }

    public Intent getPrefsIntent() {
        return new Intent(ExternalCaptureActivity.this, PreferenceActivity.class);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PREFS_REQUEST) {
            setProperties();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private String getLastSymbol() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        return prefs.getString(PreferencesFragment.KEY_SCAN_LAST_SYMBOL, ENTER_SYMBOL);
    }

    private void setUseLog(boolean use) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putBoolean(PreferencesFragment.KEY_LOG_IN_EXTERNAL_MODE, use);
        edit.apply();
    }

    private void setUseInputField(boolean use) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putBoolean(PreferencesFragment.KEY_USE_INPUT_FIELD_IN_EXTERNAL_MODE, use);
        edit.apply();
    }

    protected Boolean useInputField() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        return prefs.getBoolean(PreferencesFragment.KEY_USE_INPUT_FIELD_IN_EXTERNAL_MODE, true);
    }

    protected Boolean useLog() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        return prefs.getBoolean(PreferencesFragment.KEY_LOG_IN_EXTERNAL_MODE, false);
    }

    public void setProperties() {
        String lastSymbol = getLastSymbol();
        switch (lastSymbol) {
            case ENTER_SYMBOL:
                divider = KeyEvent.KEYCODE_ENTER;
                break;
            case TAB_SYMBOL:
                divider = KeyEvent.KEYCODE_TAB;
                break;
            case SPACE_SYMBOL:
                divider = KeyEvent.KEYCODE_SPACE;
                break;
        }
    }

    public void handleBarcode(String rawResult) {
        Log.d("external_scan", "start handling " + rawResult);
    }

    public void restartScan() {
        Log.d("external_scan", "restart scan");
        resetBarcode();
    }

    public void playBeepSoundAndVibrate() {
        beepManager.playBeepSoundAndVibrate();
    }

    public void playFailedSoundAndVibrate() {
        beepManager.playFailedSoundAndVibrate();
    }

    public void setFocus() {
        edtBarcode.requestFocus();
        edtBarcode.selectAll();
    }

    @Override
    protected void onPause() {
        beepManager.close();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        beepManager.updatePrefs();
        setLayout();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
