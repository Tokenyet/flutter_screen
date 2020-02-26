package flutter.plugins.screen.screen;

import android.provider.Settings;
import android.view.WindowManager;
import android.os.Build;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.os.PowerManager;
import android.content.Context;

import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/**
 * ScreenPlugin
 */
public class ScreenPlugin implements MethodCallHandler {

  private ScreenPlugin(Registrar registrar){
    this._registrar = registrar;
  }
  private Registrar _registrar;
  private PowerManager.WakeLock _wakeLock;

  public static void registerWith(Registrar registrar) {
    final MethodChannel channel = new MethodChannel(registrar.messenger(), "github.com/clovisnicolas/flutter_screen");
    channel.setMethodCallHandler(new ScreenPlugin(registrar));
  }

  @Override
  public void onMethodCall(MethodCall call, Result result) {
    switch(call.method){
      case "brightness":
        result.success(getBrightness());
        break;
      case "setBrightness":
        double brightness = call.argument("brightness");
        WindowManager.LayoutParams layoutParams = _registrar.activity().getWindow().getAttributes();
        layoutParams.screenBrightness = (float)brightness;
        _registrar.activity().getWindow().setAttributes(layoutParams);
        result.success(null);
        break;
      case "isKeptOn":
        int flags = _registrar.activity().getWindow().getAttributes().flags;
        result.success((flags & WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0) ;
        break;
      case "keepOn":
        Boolean on = call.argument("on");
        if (on) {
          System.out.println("Keeping screen on ");
          _registrar.activity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        else{
          System.out.println("Not keeping screen on");
          _registrar.activity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        result.success(null);
        break;
	  case "onTop":
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
        _registrar.activity().setShowWhenLocked(true);
        _registrar.activity().setTurnScreenOn(true);
        KeyguardManager keyguardManager = (KeyguardManager)_registrar.activity().getSystemService(Context.KEYGUARD_SERVICE);
        keyguardManager.requestDismissKeyguard(_registrar.activity(), null);
        //_registrar.activity().getWindow().addFlags(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
      } else {
        _registrar.activity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
          | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
          | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
          | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
          | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);
          //| WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY); // 20190704 Smart Cover issue test
        /*_registrar.activity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);*/
      }
      if(_wakeLock != null)
        _wakeLock.release();
      PowerManager pm = (PowerManager) _registrar.activity().getSystemService(Context.POWER_SERVICE);
      _wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK
                  | PowerManager.ACQUIRE_CAUSES_WAKEUP
                  | PowerManager.ON_AFTER_RELEASE, "StarryPower");
      _wakeLock.acquire();
      break;
	  case "unlockTop":
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
        _registrar.activity().setShowWhenLocked(false);
        _registrar.activity().setTurnScreenOn(false);
        //_registrar.activity().getWindow().clearFlags(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
      } else {
        _registrar.activity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON | 
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
            WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);
        //    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
      }
      if(_wakeLock != null)
        _wakeLock.release();
      break;
        default:
          result.notImplemented();
          break;
      }
  }

  private float getBrightness(){
    float result = _registrar.activity().getWindow().getAttributes().screenBrightness;
    if (result < 0) { // the application is using the system brightness
      try {
        result = Settings.System.getInt(_registrar.context().getContentResolver(), Settings.System.SCREEN_BRIGHTNESS) / (float)255;
      } catch (Settings.SettingNotFoundException e) {
        result = 1.0f;
        e.printStackTrace();
      }
    }
    return result;
  }

}
