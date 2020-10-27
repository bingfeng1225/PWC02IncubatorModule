package cn.qd.peiwen.demo;

import androidx.appcompat.app.AppCompatActivity;
import cn.haier.bio.medical.incubator.c02.C02IncubatorManager;
import cn.haier.bio.medical.incubator.c02.IC02IncubatorListener;
import cn.qd.peiwen.serialport.PWSerialPort;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;

public class MainActivity extends AppCompatActivity implements IC02IncubatorListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        String path = "/dev/ttyS4";
        if ("magton".equals(Build.MODEL)) {
            path = "/dev/ttyS2";
        }
        C02IncubatorManager.getInstance().init(path);
        C02IncubatorManager.getInstance().changeListener(this);
        C02IncubatorManager.getInstance().enable();
    }

    @Override
    public void onC02IncubatorReady() {

    }

    @Override
    public void onC02IncubatorConnected() {

    }

    @Override
    public void onC02IncubatorSwitchWriteModel() {
        if (!"magton".equals(Build.MODEL)) {
            PWSerialPort.writeFile("/sys/class/gpio/gpio24/value", "0");
        } else {
            PWSerialPort.writeFile("/sys/class/misc/sunxi-acc/acc/sochip_acc", "1");
        }
    }

    @Override
    public void onC02IncubatorSwitchReadModel() {
        if (!"magton".equals(Build.MODEL)) {
            PWSerialPort.writeFile("/sys/class/gpio/gpio24/value", "1");
        } else {
            PWSerialPort.writeFile("/sys/class/misc/sunxi-acc/acc/sochip_acc", "0");
        }
    }

    @Override
    public void onC02IncubatorPrint(String message) {
        Log.e("TAG","" + message);
    }

    @Override
    public void onC02IncubatorException(Throwable throwable) {

    }

    @Override
    public void onC02IncubatorPackageReceived(byte[] data) {

    }
}
