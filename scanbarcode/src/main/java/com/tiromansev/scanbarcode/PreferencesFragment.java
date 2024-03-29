package com.tiromansev.scanbarcode;

import android.content.SharedPreferences;
import android.hardware.Camera;
import android.os.Bundle;
import android.widget.Toast;

import androidx.preference.CheckBoxPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.tiromansev.scanbarcode.vision.PreferenceUtils;
import com.tiromansev.scanbarcode.vision.Utils;
import com.tiromansev.scanbarcode.vision.camera.CameraSizePair;
import com.tiromansev.scanbarcode.vision.camera.CameraSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PreferencesFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    private CheckBoxPreference[] checkBoxPrefs;

    public static final String CAMERA_ZXING_SCANNER = "0";
    public static final String EXTERNAL_USB_SCANNER = "1";
    public static final String CAMERA_VISION_SCANNER = "2";
    public static final String CAMERA_ZXING_VERTICAL_SCANNER = "3";
    public static final String CAMERA_MLKIT_SCANNER = "4";

    public static final String KEY_DECODE_1D_PRODUCT = "preferences_decode_1D_product";
    public static final String KEY_DECODE_1D_INDUSTRIAL = "preferences_decode_1D_industrial";
    public static final String KEY_DECODE_QR = "preferences_decode_QR";
    public static final String KEY_DECODE_DATA_MATRIX = "preferences_decode_Data_Matrix";
    public static final String KEY_DECODE_AZTEC = "preferences_decode_Aztec";
    public static final String KEY_DECODE_PDF417 = "preferences_decode_PDF417";
    public static final String KEY_FRONT_LIGHT_VISION_MODE = "preferences_front_light_vision_mode";

    public static final String KEY_PLAY_BEEP = "preferences_play_beep";
    public static final String KEY_VIBRATE = "preferences_vibrate";
    public static final String KEY_FRONT_LIGHT_MODE = "preferences_front_light_mode";
    public static final String KEY_AUTO_FOCUS = "preferences_auto_focus";
    public static final String KEY_INVERT_SCAN = "preferences_invert_scan";
    public static final String KEY_SCAN_TYPE_INT = "preferences_scan_type_int";
    public static final String KEY_SCAN_LAST_SYMBOL = "preferences_scan_last_symbol";
    public static final String KEY_DISABLE_AUTO_ORIENTATION = "preferences_orientation";
    public static final String KEY_USE_INPUT_FIELD_IN_EXTERNAL_MODE = "preferences_use_input_field_in_external_mode";
    public static final String KEY_LOG_IN_EXTERNAL_MODE = "preferences_log_in_external_mode";

    public static final String KEY_DISABLE_CONTINUOUS_FOCUS = "preferences_disable_continuous_focus";
    public static final String KEY_DISABLE_EXPOSURE = "preferences_disable_exposure";
    public static final String KEY_DISABLE_METERING = "preferences_disable_metering";
    public static final String KEY_DISABLE_BARCODE_SCENE_MODE = "preferences_disable_barcode_scene_mode";
    public static final String KEY_SCAN_TYPE_CATEGORY = "preferences_scan_type_category";
    public static final String EXTRAS_HIDE_TYPE = "EXTRAS_HIDE_TYPE";

    public static final String KEY_MLKIT_DECODE_CODE_128 = "preferences_mlkit_decode_code_128";
    public static final String KEY_MLKIT_DECODE_CODABAR = "preferences_mlkit_decode_codabar";
    public static final String KEY_MLKIT_DECODE_CODE_39 = "preferences_mlkit_decode_code_39";
    public static final String KEY_MLKIT_DECODE_CODE_93 = "preferences_mlkit_decode_code_93";
    public static final String KEY_MLKIT_DECODE_DATA_MATRIX = "preferences_mlkit_decode_data_matrix";
    public static final String KEY_MLKIT_DECODE_EAN_13 = "preferences_mlkit_decode_ean_13";
    public static final String KEY_MLKIT_DECODE_EAN_8 = "preferences_mlkit_decode_ean_8";
    public static final String KEY_MLKIT_DECODE_ITF = "preferences_mlkit_decode_itf";
    public static final String KEY_MLKIT_DECODE_UPC_A = "preferences_mlkit_decode_upc_a";
    public static final String KEY_MLKIT_DECODE_UPC_E = "preferences_mlkit_decode_upc_e";
    public static final String KEY_MLKIT_DECODE_AZTEC = "preferences_mlkit_decode_aztec";
    public static final String KEY_MLKIT_DECODE_QR_CODES = "preferences_mlkit_decode_qr_codes";
    public static final String KEY_MLKIT_DECODE_PDF_417 = "preferences_mlkit_decode_pdf_417";

    private PreferenceScreen preferences;

    public static PreferencesFragment newInstance(boolean hideType) {
        Bundle args = new Bundle();
        args.putBoolean(EXTRAS_HIDE_TYPE, hideType);

        PreferencesFragment fragment = new PreferencesFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.barcode_zxing_preferences);
        preferences = getPreferenceScreen();
        setBarcodePreferences();

        checkBoxPrefs = findDecodePrefs(preferences,
                KEY_DECODE_1D_PRODUCT,
                KEY_DECODE_1D_INDUSTRIAL,
                KEY_DECODE_QR,
                KEY_DECODE_DATA_MATRIX,
                KEY_DECODE_AZTEC,
                KEY_DECODE_PDF417);
        disableLastCheckedPref();

        //заголовок
        if (getActivity() != null) {
            getActivity().setTitle(R.string.caption_setting_scan);
        }

        initSummaries();
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {

    }

    private static CheckBoxPreference[] findDecodePrefs(PreferenceScreen preferences, String... keys) {
        CheckBoxPreference[] prefs = new CheckBoxPreference[keys.length];
        for (int i = 0; i < keys.length; i++) {
            prefs[i] = (CheckBoxPreference) preferences.findPreference(keys[i]);
        }
        return prefs;
    }

    @Override
    public void onResume() {
        super.onResume();

        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();

        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        disableLastCheckedPref();
        updatePrefSummary(findPreference(key));
        if (key.equals(KEY_SCAN_TYPE_INT)) {
            setBarcodePreferences();
        }
    }

    private void setBarcodePreferences() {
        String scanType = CAMERA_VISION_SCANNER;
        ListPreference scanPreference = (ListPreference) getPreferenceScreen().findPreference(KEY_SCAN_TYPE_INT);
        if (scanPreference != null) {
           scanType = scanPreference.getValue();
        }

        getPreferenceScreen().removeAll();
        switch (scanType) {
            case CAMERA_ZXING_SCANNER:
                addPreferencesFromResource(R.xml.barcode_zxing_preferences);
                break;
            case EXTERNAL_USB_SCANNER:
                addPreferencesFromResource(R.xml.barcode_external_preferences);
                break;
            case CAMERA_VISION_SCANNER:
                addPreferencesFromResource(R.xml.barcode_vision_preferences);
                setUpRearCameraPreviewSizePreference();
                break;
            case CAMERA_ZXING_VERTICAL_SCANNER:
                addPreferencesFromResource(R.xml.barcode_zxing_vertical_preferences);
                break;
            case CAMERA_MLKIT_SCANNER:
                addPreferencesFromResource(R.xml.barcode_mlkit_preferences);
                break;
        }
        initSummaries();

        if (getArguments() != null && getArguments().containsKey(EXTRAS_HIDE_TYPE)) {
            boolean hideType = getArguments().getBoolean(EXTRAS_HIDE_TYPE, false);
            if (hideType) {
                PreferenceCategory category = (PreferenceCategory) preferences.findPreference(KEY_SCAN_TYPE_CATEGORY);
                if (category != null) {
                    preferences.removePreference(category);
                }
            }
        }
    }

    private void initSummaries() {
        for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); i++) {
            initSummary(getPreferenceScreen().getPreference(i));
        }
    }

    private void initSummary(Preference p) {
        if (p instanceof PreferenceCategory) {
            PreferenceCategory pCat = (PreferenceCategory) p;
            for (int i = 0; i < pCat.getPreferenceCount(); i++) {
                initSummary(pCat.getPreference(i));
            }
        } else {
            updatePrefSummary(p);
        }
    }

    private void updatePrefSummary(Preference p) {
        if (p == null) {
            return;
        }
        if (p instanceof ListPreference) {
            ListPreference listPref = (ListPreference) p;
            p.setSummary(listPref.getEntry());
        }
    }

    private void disableLastCheckedPref() {
        Collection<CheckBoxPreference> checked = new ArrayList<>(checkBoxPrefs.length);
        for (CheckBoxPreference pref : checkBoxPrefs) {
            if (pref != null) {
                if (pref.isChecked()) {
                    checked.add(pref);
                }
            }
        }
        boolean disable = checked.size() <= 1;
        for (CheckBoxPreference pref : checkBoxPrefs) {
            if (pref != null) {
                pref.setEnabled(!(disable && checked.contains(pref)));
            }
        }
    }

    private void setUpRearCameraPreviewSizePreference() {
        ListPreference previewSizePreference =
                (ListPreference) findPreference(getString(R.string.pref_key_rear_camera_preview_size));
        if (previewSizePreference == null) {
            return;
        }

        Camera camera = null;
        try {
            camera = Camera.open(CameraSource.CAMERA_FACING_BACK);
            List<CameraSizePair> previewSizeList = Utils.generateValidPreviewSizeList(camera);
            String[] previewSizeStringValues = new String[previewSizeList.size()];
            Map<String, String> previewToPictureSizeStringMap = new HashMap<>();
            for (int i = 0; i < previewSizeList.size(); i++) {
                CameraSizePair sizePair = previewSizeList.get(i);
                previewSizeStringValues[i] = sizePair.preview.toString();
                if (sizePair.picture != null) {
                    previewToPictureSizeStringMap.put(
                            sizePair.preview.toString(), sizePair.picture.toString());
                }
            }
            previewSizePreference.setEntries(previewSizeStringValues);
            previewSizePreference.setEntryValues(previewSizeStringValues);
            previewSizePreference.setSummary(previewSizePreference.getEntry());
            previewSizePreference.setOnPreferenceChangeListener(
                    (preference, newValue) -> {
                        String newPreviewSizeStringValue = (String) newValue;
                        previewSizePreference.setSummary(newPreviewSizeStringValue);
                        PreferenceUtils.saveStringPreference(
                                getActivity(),
                                R.string.pref_key_rear_camera_picture_size,
                                previewToPictureSizeStringMap.get(newPreviewSizeStringValue));
                        return true;
                    });

        } catch (Exception e) {
            // If there's no camera for the given camera id, hide the corresponding preference.
            if (previewSizePreference.getParent() != null) {
                previewSizePreference.getParent().removePreference(previewSizePreference);
            }
        } finally {
            if (camera != null) {
                camera.release();
            }
        }
    }

}
