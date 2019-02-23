package com.tiromansev.scanbarcode.zxing;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.Result;
import com.tiromansev.scanbarcode.R;
import com.tiromansev.scanbarcode.zxing.camera.CameraManager;

import java.util.Collection;
import java.util.Map;

/**
 * This class handles all the messaging which comprises the state machine for capture.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class CaptureActivityHandler extends Handler {

  private static final String TAG = CaptureActivityHandler.class.getSimpleName();

  private final ZxingCaptureActivity activity;
  private final DecodeThread decodeThread;
  private State state;
  private final CameraManager cameraManager;

  private enum State {
    PREVIEW,
    SUCCESS,
    DONE
  }

  CaptureActivityHandler(ZxingCaptureActivity activity,
                         Collection<BarcodeFormat> decodeFormats,
                         Map<DecodeHintType,?> baseHints,
                         String characterSet,
                         CameraManager cameraManager) {
    this.activity = activity;
    decodeThread = new DecodeThread(activity, decodeFormats, baseHints, characterSet,
        new ViewfinderResultPointCallback(activity.getViewfinderView()));
    decodeThread.start();
    state = State.SUCCESS;

    // Start ourselves capturing previews and decoding.
    this.cameraManager = cameraManager;
    cameraManager.startPreview();
    restartPreviewAndDecode();
  }

  @Override
  public void handleMessage(Message message) {
    if (message.what == R.id.restart_preview) {
      restartPreviewAndDecode();

    } else if (message.what == R.id.decode_succeeded) {
      state = State.SUCCESS;
      Bundle bundle = message.getData();
      Bitmap barcode = null;
      if (bundle != null) {
        byte[] compressedBitmap = bundle.getByteArray(DecodeThread.BARCODE_BITMAP);
        if (compressedBitmap != null) {
          barcode = BitmapFactory.decodeByteArray(compressedBitmap, 0, compressedBitmap.length, null);
          // Mutable copy:
          barcode = barcode.copy(Bitmap.Config.ARGB_8888, true);
        }
      }
      activity.handleDecodeInternally(((Result) message.obj).getText(), barcode);

    } else if (message.what == R.id.decode_failed) {// We're decoding as fast as possible, so when one decode fails, start another.
      state = State.PREVIEW;
      cameraManager.requestPreviewFrame(decodeThread.getHandler(), R.id.decode);

    } else if (message.what == R.id.return_scan_result) {
      activity.setResult(Activity.RESULT_OK, (Intent) message.obj);
      activity.finish();


    }
  }

  public void quitSynchronously() {
    state = State.DONE;
    cameraManager.stopPreview();
    Message quit = Message.obtain(decodeThread.getHandler(), R.id.quit);
    quit.sendToTarget();
    try {
      // Wait at most half a second; should be enough time, and onPause() will timeout quickly
      decodeThread.join(500L);
    } catch (InterruptedException e) {
      // continue
    }

    // Be absolutely sure we don't send any queued up messages
    removeMessages(R.id.decode_succeeded);
    removeMessages(R.id.decode_failed);
  }

  private void restartPreviewAndDecode() {
    if (state == State.SUCCESS) {
      state = State.PREVIEW;
      cameraManager.requestPreviewFrame(decodeThread.getHandler(), R.id.decode);
      activity.drawViewfinder();
    }
  }

}
