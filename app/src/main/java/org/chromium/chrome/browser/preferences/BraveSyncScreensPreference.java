/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.chromium.chrome.browser.preferences;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.hardware.SensorManager;
import org.chromium.base.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.text.format.DateUtils;
import android.text.Html;
import android.util.DisplayMetrics;
import android.view.ContextThemeWrapper;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TableLayout;
import android.widget.LinearLayout;

import org.chromium.base.Log;
import org.chromium.chrome.R;
import org.chromium.ui.UiUtils;

import org.chromium.base.ContextUtils;
import org.chromium.chrome.browser.ChromeApplication;
import org.chromium.chrome.browser.qrreader.BarcodeTracker;
import org.chromium.chrome.browser.qrreader.BarcodeTrackerFactory;
import org.chromium.chrome.browser.qrreader.CameraSource;
import org.chromium.chrome.browser.qrreader.CameraSourcePreview;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.io.IOException;
import java.lang.Runnable;

/**
 * Settings fragment that allows to control Sync functionality.
 */
public class BraveSyncScreensPreference extends PreferenceFragment
      implements View.OnClickListener, CompoundButton.OnCheckedChangeListener, BarcodeTracker.BarcodeGraphicTrackerCallback{

  private static final String PREF_NAME = "SyncPreferences";
  private static final String PREF_SYNC_SWITCH = "sync_switch";
  private static final String PREF_SEED = "Seed";
  private static final String PREF_SYNC_DEVICE_NAME = "SyncDeviceName";
  private static final String TAG = "SYNC_PREFERENCES";
  // Permission request codes need to be < 256
  private static final int RC_HANDLE_CAMERA_PERM = 2;
  // Intent request code to handle updating play services if needed.
    private static final int RC_HANDLE_GMS = 9001;
  private TextView mEmptyView;
  // The new to sync button displayed in the Sync view.
  private Button mNewToSyncButton;
  // The have a sync code button displayed in the Sync view.
  private Button mHaveASyncCodeButton;
  private Button mEnterCodeWordsButton;
  private Button mResetSync;
  // Brave Sync messaeg text view
  private TextView mBraveSyncTextView;
  private CameraSource mCameraSource;
  private CameraSourcePreview mCameraSourcePreview;
  private ImageView mImageView;
  private TableLayout mEnterWordsLayout;
  private Switch mSyncSwitch;
  private BraveSyncScreensObserver mSyncScreensObserver;
  private LinearLayout mBookmarksLayout;
  private String mDeviceName = "";

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
      super.onConfigurationChanged(newConfig);

      // Checks the orientation of the screen
      if (newConfig.orientation != Configuration.ORIENTATION_UNDEFINED
            && null != mCameraSourcePreview) {
          mCameraSourcePreview.stop();
          try {
              startCameraSource();
          } catch (SecurityException exc) {
          }
      }
  }

  @Override
  public View onCreateView(
          LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
      if (ensureCameraPermission()) {
          createCameraSource(true, false);
      }

      // Read which category we should be showing.
      return inflater.inflate(R.layout.brave_sync_layout, container, false);
  }

  private boolean ensureCameraPermission() {
      if (ActivityCompat.checkSelfPermission(getActivity().getApplicationContext(), Manifest.permission.CAMERA)
              == PackageManager.PERMISSION_GRANTED){
          return true;
      }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
          requestPermissions(
                  new String[]{Manifest.permission.CAMERA}, RC_HANDLE_CAMERA_PERM);
      }

      return false;
  }

  @Override
   public void onRequestPermissionsResult(int requestCode,
                                          String[] permissions,
                                          int[] grantResults) {
       if (requestCode != RC_HANDLE_CAMERA_PERM) {
           super.onRequestPermissionsResult(requestCode, permissions, grantResults);

           return;
       }

       if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
           // we have permission, so create the camerasource
           createCameraSource(true, false);

           return;
       }

       Log.e(TAG, "Permission not granted: results len = " + grantResults.length +
               " Result code = " + (grantResults.length > 0 ? grantResults[0] : "(empty)"));
       // We still allow to enter words
       //getActivity().onBackPressed();
   }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
      addPreferencesFromResource(R.xml.brave_sync_preferences);
      getActivity().setTitle(R.string.sign_in_sync);
      ListView listView = (ListView) getView().findViewById(android.R.id.list);
      mEmptyView = (TextView) getView().findViewById(android.R.id.empty);
      listView.setEmptyView(mEmptyView);
      listView.setDivider(null);

      mNewToSyncButton = (Button) getView().findViewById(R.id.new_to_sync);
      if (mNewToSyncButton != null) {
          mNewToSyncButton.setOnClickListener(this);
      }

      mHaveASyncCodeButton = (Button) getView().findViewById(R.id.have_existing_sync_code);
      if (mHaveASyncCodeButton != null) {
          mHaveASyncCodeButton.setOnClickListener(this);
      }

      mEnterCodeWordsButton = (Button) getView().findViewById(R.id.enter_code_words);
      if (mEnterCodeWordsButton != null) {
          mEnterCodeWordsButton.setOnClickListener(this);
      }
      mBraveSyncTextView = (TextView)getView().findViewById(R.id.brave_sync_text);
      setMainSyncText();
      mImageView = (ImageView)getView().findViewById(R.id.brave_sync_image);
      mCameraSourcePreview = (CameraSourcePreview)getView().findViewById(R.id.preview);
      mEnterWordsLayout = (TableLayout)getView().findViewById(R.id.tableEnterWords);
      mSyncSwitch = (Switch)getView().findViewById(R.id.sync_switch_control);
      if (null != mSyncSwitch) {
          mSyncSwitch.setOnCheckedChangeListener(this);
      }
      mBookmarksLayout = (LinearLayout)getView().findViewById(R.id.bookmarksLayout);
      mResetSync = (Button)getView().findViewById(R.id.reset_sync);
      if (null != mResetSync) {
          mResetSync.setOnClickListener(this);
      }
      setAppropriateView();
      getActivity().getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

      super.onActivityCreated(savedInstanceState);
  }

  private void setAppropriateView() {
      SharedPreferences sharedPref = getActivity().getApplicationContext().getSharedPreferences(PREF_NAME, 0);
      String seed = sharedPref.getString(PREF_SEED, null);
      if (null == seed || seed.isEmpty()) {
          if (null != mCameraSourcePreview) {
              mCameraSourcePreview.stop();
              mCameraSourcePreview.setVisibility(View.GONE);
          }
          if (null != mImageView) {
              mImageView.setVisibility(View.VISIBLE);
          }
          /*if (null != mNewToSyncButton) {
              mNewToSyncButton.setVisibility(View.VISIBLE);
          }*/
          if (null != mHaveASyncCodeButton) {
              mHaveASyncCodeButton.setVisibility(View.VISIBLE);
          }
          if (null != mEnterWordsLayout) {
              mEnterWordsLayout.setVisibility(View.GONE);
          }
          if (null != mEnterCodeWordsButton) {
              mEnterCodeWordsButton.setVisibility(View.GONE);
          }
          if (null != mBraveSyncTextView) {
              mBraveSyncTextView.setVisibility(View.VISIBLE);
          }
          if (null != mBookmarksLayout) {
              mBookmarksLayout.setVisibility(View.GONE);
          }
          if (null != mResetSync) {
              mResetSync.setVisibility(View.GONE);
          }
          return;
      }
      if (null != mCameraSourcePreview) {
          mCameraSourcePreview.stop();
          mCameraSourcePreview.setVisibility(View.GONE);
      }
      if (null != mImageView) {
          mImageView.setVisibility(View.GONE);
      }
      if (null != mNewToSyncButton) {
          mNewToSyncButton.setVisibility(View.GONE);
      }
      if (null != mHaveASyncCodeButton) {
          mHaveASyncCodeButton.setVisibility(View.GONE);
      }
      if (null != mEnterWordsLayout) {
          mEnterWordsLayout.setVisibility(View.GONE);
      }
      if (null != mEnterCodeWordsButton) {
          mEnterCodeWordsButton.setVisibility(View.GONE);
      }
      if (null != mBraveSyncTextView) {
          mBraveSyncTextView.setVisibility(View.GONE);
      }
      if (null != mSyncSwitch) {
          ChromeSwitchPreference syncSwitch = (ChromeSwitchPreference) findPreference(PREF_SYNC_SWITCH);
          if (null != syncSwitch) {
              mSyncSwitch.setChecked(syncSwitch.isChecked());
          }
      }
      /*if (null != mBookmarksLayout) {
          mBookmarksLayout.setVisibility(View.VISIBLE);
      }*/
      if (null != mResetSync) {
          mResetSync.setVisibility(View.VISIBLE);
      }
  }

  private void setMainSyncText() {
      setSyncText(getResources().getString(R.string.brave_sync), getResources().getString(R.string.brave_sync_description));
  }

  private void setQRCodeText() {
      setSyncText(getResources().getString(R.string.brave_sync_qrcode), getResources().getString(R.string.brave_sync_qrcode_message));
  }

  private void setSyncText(String title, String message) {
      String htmlMessage = "<b>" + title + "</b><br/>" + message.replace("\n", "<br/>");
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
          setTextViewStyle(htmlMessage);
      } else {
          setTextViewStyleOld(htmlMessage);
      }
  }

  @TargetApi(Build.VERSION_CODES.N)
  private void setTextViewStyle(String text) {
      if (null != mBraveSyncTextView) {
          mBraveSyncTextView.setText(Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY));
      }
  }

  private void setTextViewStyleOld(String text) {
      if (null != mBraveSyncTextView) {
          mBraveSyncTextView.setText(Html.fromHtml(text));
      }
  }

  /** OnClickListener for the clear button. We show an alert dialog to confirm the action */
  @Override
  public void onClick(View v) {
      if (getActivity() == null || v != mNewToSyncButton && v != mHaveASyncCodeButton
          && v != mEnterCodeWordsButton && v != mResetSync) return;

      if (mHaveASyncCodeButton == v) {
          showAddDeviceNameDialog();
      } else if (mNewToSyncButton == v) {
          // TODO
      } else if (mEnterCodeWordsButton == v) {
          if (null != mImageView) {
              mImageView.setVisibility(View.GONE);
          }
          if (null != mNewToSyncButton) {
              mNewToSyncButton.setVisibility(View.GONE);
          }
          if (null != mHaveASyncCodeButton) {
              mHaveASyncCodeButton.setVisibility(View.GONE);
          }
          if (null != mEnterCodeWordsButton) {
              mEnterCodeWordsButton.setVisibility(View.GONE);
          }
          if (null != mBraveSyncTextView) {
              mBraveSyncTextView.setVisibility(View.GONE);
          }
          if (null != mCameraSourcePreview) {
              mCameraSourcePreview.stop();
              mCameraSourcePreview.setVisibility(View.GONE);
          }
          if (null != mEnterWordsLayout) {
              mEnterWordsLayout.setVisibility(View.VISIBLE);
          }
          EditText wordControl = getWordControl(1);
          if (null != wordControl) {
              wordControl.requestFocus();
              InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
              imm.showSoftInput(wordControl, InputMethodManager.SHOW_FORCED);
          }
          EditText wordLastControl = getWordControl(16);
          if (null != wordLastControl) {
              wordLastControl.setOnEditorActionListener(new EditText.OnEditorActionListener() {
                  @Override
                  public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                      if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE)) {
                          ChromeApplication application = (ChromeApplication)ContextUtils.getApplicationContext();
                          String[] words = new String[16];
                          if (null != application && null != application.mBraveSyncWorker && null != words) {
                              for (int i = 1; i < 17; i++) {
                                  EditText wordControl = getWordControl(i);
                                  if (null == wordControl) {
                                      break;
                                  }
                                  words[i - 1] = wordControl.getText().toString();
                              }
                              application.mBraveSyncWorker.GetNumber(words);
                          }
                      }
                      return false;
                  }
              });
          }
          ChromeApplication application = (ChromeApplication)ContextUtils.getApplicationContext();
          if (null != application && null != application.mBraveSyncWorker) {
              if (null == mSyncScreensObserver) {
                  mSyncScreensObserver = new BraveSyncScreensObserver() {
                      @Override
                      public void onWordsCodeWrong() {
                          showEndDialog(getResources().getString(R.string.sync_device_failure));
                      }
                      @Override
                      public void onSeedReceived(String seed) {
                          if (!isBarCodeValid(seed, false)) {
                              showEndDialog(getResources().getString(R.string.sync_device_failure));
                          }
                          //Log.i("TAG", "!!!received seed == " + seed);
                          // Save seed and deviceId in preferences
                          SharedPreferences sharedPref = getActivity().getApplicationContext().getSharedPreferences(PREF_NAME, 0);
                          SharedPreferences.Editor editor = sharedPref.edit();
                          editor.putString(PREF_SEED, seed);
                          editor.apply();
                          getActivity().runOnUiThread(new Runnable() {
                              @Override
                              public void run() {
                                  ChromeSwitchPreference syncSwitch = (ChromeSwitchPreference) findPreference(PREF_SYNC_SWITCH);
                                  if (null != syncSwitch) {
                                      syncSwitch.setChecked(true);
                                      if (null != mSyncSwitch) {
                                          mSyncSwitch.setChecked(true);
                                      }
                                  }
                                  ChromeApplication application = (ChromeApplication)ContextUtils.getApplicationContext();
                                  if (null != application && null != application.mBraveSyncWorker) {
                                      showEndDialog(getResources().getString(R.string.sync_device_success));
                                      application.mBraveSyncWorker.InitSync(true);
                                  }
                                  setAppropriateView();
                              }
                          });
                      }
                  };
              }
              application.mBraveSyncWorker.InitJSWebView(mSyncScreensObserver);
          }
      } else if (mResetSync == v) {
          ResetSyncDialog();
      }
  }

  @Override
  public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
      if (getActivity() == null || buttonView != mSyncSwitch) return;
      ChromeSwitchPreference syncSwitch = (ChromeSwitchPreference) findPreference(PREF_SYNC_SWITCH);
      if (null != syncSwitch) {
          syncSwitch.setChecked(isChecked);
          /*if (null != mSyncSwitch) {
              mSyncSwitch.setChecked(isChecked);
          }*/
      }
  }

  private void showMainSyncScrypt() {
      if (null != mImageView) {
          mImageView.setVisibility(View.VISIBLE);
      }
      /*if (null != mNewToSyncButton) {
          mNewToSyncButton.setVisibility(View.VISIBLE);
      }*/
      if (null != mHaveASyncCodeButton) {
          mHaveASyncCodeButton.setVisibility(View.VISIBLE);
      }
      if (null != mBraveSyncTextView) {
          mBraveSyncTextView.setVisibility(View.VISIBLE);
      }
      if (null != mEnterCodeWordsButton) {
          mEnterCodeWordsButton.setVisibility(View.GONE);
      }
      if (null != mCameraSourcePreview) {
          mCameraSourcePreview.setVisibility(View.GONE);
      }
      if (null != mEnterWordsLayout) {
          mEnterWordsLayout.setVisibility(View.GONE);
      }
      setMainSyncText();
  }

  // Handles the requesting of the camera permission.
  private void requestCameraPermission() {
      Log.w(TAG, "Camera permission is not granted. Requesting permission");

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
          final String[] permissions = new String[]{Manifest.permission.CAMERA};

          requestPermissions(permissions, RC_HANDLE_CAMERA_PERM);
      }
  }

  @SuppressLint("InlinedApi")
  private void createCameraSource(boolean autoFocus, boolean useFlash) {
      Context context = getActivity().getApplicationContext();

      // A barcode detector is created to track barcodes.  An associated multi-processor instance
      // is set to receive the barcode detection results, track the barcodes, and maintain
      // graphics for each barcode on screen.  The factory is used by the multi-processor to
      // create a separate tracker instance for each barcode.
      BarcodeDetector barcodeDetector = new BarcodeDetector.Builder(context)
              .setBarcodeFormats(Barcode.ALL_FORMATS)
              .build();
      BarcodeTrackerFactory barcodeFactory = new BarcodeTrackerFactory(this);
      barcodeDetector.setProcessor(new MultiProcessor.Builder<>(barcodeFactory).build());

      if (!barcodeDetector.isOperational()) {
          // Note: The first time that an app using the barcode or face API is installed on a
          // device, GMS will download a native libraries to the device in order to do detection.
          // Usually this completes before the app is run for the first time.  But if that
          // download has not yet completed, then the above call will not detect any barcodes.
          //
          // isOperational() can be used to check if the required native libraries are currently
          // available.  The detectors will automatically become operational once the library
          // downloads complete on device.
          Log.i(TAG, "Detector dependencies are not yet available.");
      }

      // Creates and starts the camera.  Note that this uses a higher resolution in comparison
      // to other detection examples to enable the barcode detector to detect small barcodes
      // at long distances.
      DisplayMetrics metrics = new DisplayMetrics();
      getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);

      CameraSource.Builder builder = new CameraSource.Builder(getActivity().getApplicationContext(), barcodeDetector)
              .setFacing(CameraSource.CAMERA_FACING_BACK)
              .setRequestedPreviewSize(metrics.widthPixels, metrics.heightPixels)
              .setRequestedFps(24.0f);

      // make sure that auto focus is an available option
      builder = builder.setFocusMode(
              autoFocus ? Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE : null);

      mCameraSource = builder
              .setFlashMode(useFlash ? Camera.Parameters.FLASH_MODE_TORCH : null)
              .build();
  }

  private void startCameraSource() throws SecurityException {
      if (mCameraSource != null && mCameraSourcePreview.mCameraExist) {
          // check that the device has play services available.
          try {
              int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                      getActivity().getApplicationContext());
              if (code != ConnectionResult.SUCCESS) {
                  Dialog dlg =
                          GoogleApiAvailability.getInstance().getErrorDialog(getActivity(), code, RC_HANDLE_GMS);
                  if (null != dlg) {
                      dlg.show();
                  }
              }
          } catch (ActivityNotFoundException e) {
              Log.e(TAG, "Unable to start camera source.", e);
              mCameraSource.release();
              mCameraSource = null;

              return;
          }
          try {
              mCameraSourcePreview.start(mCameraSource);
          } catch (IOException e) {
              Log.e(TAG, "Unable to start camera source.", e);
              mCameraSource.release();
              mCameraSource = null;
          }
      }
  }

  @Override
  public void onResume() {
      super.onResume();
      try {
          if (null != mCameraSourcePreview && View.GONE != mCameraSourcePreview.getVisibility()) {
              startCameraSource();
          }
      } catch (SecurityException se) {
          Log.e(TAG,"Do not have permission to start the camera", se);
      } catch (RuntimeException e) {
          Log.e(TAG, "Could not start camera source.", e);
      }
  }

  @Override
  public void onPause() {
      super.onPause();
      if (mCameraSourcePreview != null) {
          mCameraSourcePreview.stop();
      }
      InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
      imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
  }

  @Override
  public void onDestroy() {
      super.onDestroy();
      if (mCameraSourcePreview != null) {
          mCameraSourcePreview.release();
      }
  }

  private boolean isBarCodeValid(String barcode, boolean hexValue) {
      if (hexValue && barcode.length() != 64) {
          return false;
      } else if (!hexValue) {
          String[] split = barcode.split(", ");
          if (split.length != 32) {
              return false;
          }
      }

      return true;
  }

  @Override
  public void onDetectedQrCode(Barcode barcode) {
      if (barcode != null) {
          //Log.i(TAG, "!!!code == " + barcode.displayValue);
          final String barcodeValue = barcode.displayValue;
          if (!isBarCodeValid(barcodeValue, true)) {
              showEndDialog(getResources().getString(R.string.sync_device_failure));
              showMainSyncScrypt();

              return;
          }
          String[] barcodeString = barcodeValue.replaceAll("..(?!$)", "$0 ").split(" ");
          String seed = "";
          for (int i = 0; i < barcodeString.length; i++) {
              if (0 != seed.length()) {
                  seed += ", ";
              }
              seed += String.valueOf(Integer.parseInt(barcodeString[i], 16));
          }
          //Log.i(TAG, "!!!seed == " + seed);
          // Save seed and deviceId in preferences
          SharedPreferences sharedPref = getActivity().getApplicationContext().getSharedPreferences(PREF_NAME, 0);
          SharedPreferences.Editor editor = sharedPref.edit();
          editor.putString(PREF_SEED, seed);
          editor.apply();
          getActivity().runOnUiThread(new Runnable() {
              @Override
              public void run() {
                  showEndDialog(getResources().getString(R.string.sync_device_success));
                  ChromeSwitchPreference syncSwitch = (ChromeSwitchPreference) findPreference(PREF_SYNC_SWITCH);
                  if (null != syncSwitch) {
                      syncSwitch.setChecked(true);
                      mSyncSwitch.setChecked(true);
                  }
                  ChromeApplication application = (ChromeApplication)ContextUtils.getApplicationContext();
                  if (null != application && null != application.mBraveSyncWorker) {
                      application.mBraveSyncWorker.InitSync(true);
                  }
                  setAppropriateView();
              }
          });
      }
  }

  private EditText getWordControl(int number) {
      EditText control = null;
      switch (number) {
        case 1:
          control = (EditText)getView().findViewById(R.id.editTextWord1);
          break;
        case 2:
          control = (EditText)getView().findViewById(R.id.editTextWord2);
          break;
        case 3:
          control = (EditText)getView().findViewById(R.id.editTextWord3);
          break;
        case 4:
          control = (EditText)getView().findViewById(R.id.editTextWord4);
          break;
        case 5:
          control = (EditText)getView().findViewById(R.id.editTextWord5);
          break;
        case 6:
          control = (EditText)getView().findViewById(R.id.editTextWord6);
          break;
        case 7:
          control = (EditText)getView().findViewById(R.id.editTextWord7);
          break;
        case 8:
          control = (EditText)getView().findViewById(R.id.editTextWord8);
          break;
        case 9:
          control = (EditText)getView().findViewById(R.id.editTextWord9);
          break;
        case 10:
          control = (EditText)getView().findViewById(R.id.editTextWord10);
          break;
        case 11:
          control = (EditText)getView().findViewById(R.id.editTextWord11);
          break;
        case 12:
          control = (EditText)getView().findViewById(R.id.editTextWord12);
          break;
        case 13:
          control = (EditText)getView().findViewById(R.id.editTextWord13);
          break;
        case 14:
          control = (EditText)getView().findViewById(R.id.editTextWord14);
          break;
        case 15:
          control = (EditText)getView().findViewById(R.id.editTextWord15);
          break;
        case 16:
          control = (EditText)getView().findViewById(R.id.editTextWord16);
          break;
        default:
          control = null;
      }

      return control;
  }

  private void showEndDialog(String message) {
      AlertDialog.Builder alert = new AlertDialog.Builder(getActivity(), R.style.AlertDialogTheme);
      if (null == alert) {
          return;
      }
      DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int button) {
          }
      };
      AlertDialog alertDialog = alert
              .setTitle(getResources().getString(R.string.sync_device))
              .setMessage(message)
              .setPositiveButton(R.string.ok, onClickListener)
              .create();
      alertDialog.getDelegate().setHandleNativeActionModesEnabled(false);
      alertDialog.show();
  }

  private void showAddDeviceNameDialog() {
      LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(
              Context.LAYOUT_INFLATER_SERVICE);
      View view = inflater.inflate(R.layout.add_sync_device_name_dialog, null);
      final EditText input = (EditText) view.findViewById(R.id.device_name);

      DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int button) {
              if (button == AlertDialog.BUTTON_POSITIVE) {
                  mDeviceName = input.getText().toString();
                  if (mDeviceName.isEmpty()) {
                      mDeviceName = input.getHint().toString();
                  }
                  SharedPreferences sharedPref = getActivity().getApplicationContext().getSharedPreferences(PREF_NAME, 0);
                  SharedPreferences.Editor editor = sharedPref.edit();
                  editor.putString(PREF_SYNC_DEVICE_NAME, mDeviceName);
                  editor.apply();
                  if (null != mImageView) {
                      mImageView.setVisibility(View.GONE);
                  }
                  if (null != mNewToSyncButton) {
                      mNewToSyncButton.setVisibility(View.GONE);
                  }
                  if (null != mHaveASyncCodeButton) {
                      mHaveASyncCodeButton.setVisibility(View.GONE);
                  }
                  if (null != mEnterWordsLayout) {
                      mEnterWordsLayout.setVisibility(View.GONE);
                  }
                  if (null != mEnterCodeWordsButton) {
                      mEnterCodeWordsButton.setVisibility(View.VISIBLE);
                  }
                  setQRCodeText();
                  if (null != mCameraSourcePreview) {
                      mCameraSourcePreview.setVisibility(View.VISIBLE);
                      int rc = ActivityCompat.checkSelfPermission(getActivity().getApplicationContext(), Manifest.permission.CAMERA);
                      if (rc == PackageManager.PERMISSION_GRANTED) {
                          try {
                            startCameraSource();
                          } catch (SecurityException exc) {
                          }
                      }
                  }
              }
          }
      };

      AlertDialog.Builder alert = new AlertDialog.Builder(getActivity(), R.style.AlertDialogTheme);
      if (null == alert) {
          return;
      }
      AlertDialog alertDialog = alert
              .setTitle(R.string.sync_settings_add_device_name_title)
              .setMessage(getResources().getString(R.string.sync_settings_add_device_name_label))
              .setView(view)
              .setPositiveButton(R.string.ok, onClickListener)
              .setNegativeButton(R.string.cancel, onClickListener)
              .create();
      alertDialog.getDelegate().setHandleNativeActionModesEnabled(false);
      alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
          @Override
          public void onShow(DialogInterface dialog) {
              UiUtils.showKeyboard(input);
          }
      });
      alertDialog.show();
      Button cancelButton = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
      cancelButton.setVisibility(View.GONE);
  }

  private void ResetSyncDialog() {
      AlertDialog.Builder alert = new AlertDialog.Builder(getActivity(), R.style.AlertDialogTheme);
      if (null == alert) {
          return;
      }
      DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int button) {
              if (button == AlertDialog.BUTTON_POSITIVE) {
                  ChromeSwitchPreference syncSwitch = (ChromeSwitchPreference) findPreference(PREF_SYNC_SWITCH);
                  if (null != syncSwitch) {
                      syncSwitch.setChecked(false);
                      if (null != mSyncSwitch) {
                          mSyncSwitch.setChecked(false);
                      }
                  }
                  ChromeApplication application = (ChromeApplication)ContextUtils.getApplicationContext();
                  if (null != application && null != application.mBraveSyncWorker) {
                      application.mBraveSyncWorker.ResetSync();
                  }
                  setAppropriateView();
              }
          }
      };
      AlertDialog alertDialog = alert
              .setTitle(getResources().getString(R.string.reset_sync))
              .setMessage(getResources().getString(R.string.resetting_sync))
              .setPositiveButton(R.string.ok, onClickListener)
              .setNegativeButton(R.string.cancel, onClickListener)
              .create();
      alertDialog.getDelegate().setHandleNativeActionModesEnabled(false);
      alertDialog.show();
  }
}
