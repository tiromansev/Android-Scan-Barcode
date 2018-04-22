package com.tiromansev.scanbarcode.external;

import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;

import com.tiromansev.scanbarcode.zxing.BeepManager;
import com.tiromansev.scanbarcode.PreferencesFragment;
import com.tiromansev.scanbarcode.R;

public class ExternalCaptureActivity extends AppCompatActivity {

    public EditText edtBarcode;
    public static final String ENTER_SYMBOL = "0";
    public static final String TAB_SYMBOL = "1";
    public static final String SPACE_SYMBOL = "2";
    private String lastSymbol = ENTER_SYMBOL;
    public BeepManager beepManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_external_capture);
        edtBarcode = findViewById(R.id.edtBarcode);
        edtBarcode.getBackground().mutate().setColorFilter(getResources().getColor(R.color.color_external_caption), PorterDuff.Mode.SRC_ATOP);

        edtBarcode.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    finish();
                }
                boolean enterHandled = (lastSymbol.equals(ENTER_SYMBOL) && keyCode == KeyEvent.KEYCODE_ENTER);
                boolean tabHandled = (lastSymbol.equals(TAB_SYMBOL) && keyCode == KeyEvent.KEYCODE_TAB);
                boolean spaceHandled = (lastSymbol.equals(SPACE_SYMBOL) && keyCode == KeyEvent.KEYCODE_SPACE);
                if (enterHandled || tabHandled || spaceHandled) {
                    handleBarcode(edtBarcode.getText().toString());
                    edtBarcode.setText(null);
                    return true;
                }
                return false;
            }
        });

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        lastSymbol = prefs.getString(PreferencesFragment.KEY_SCAN_LAST_SYMBOL, ENTER_SYMBOL);
        beepManager = new BeepManager(this);
    }

    public void handleBarcode(String rawResult) {

    }

    public void playBeepSoundAndVibrate() {
        beepManager.playBeepSoundAndVibrate();
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
        setFocus();
    }

    public void setFocus() {
        edtBarcode.requestFocus();
        edtBarcode.selectAll();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}