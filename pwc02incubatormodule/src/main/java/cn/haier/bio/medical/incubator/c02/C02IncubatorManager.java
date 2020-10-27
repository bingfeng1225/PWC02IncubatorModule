package cn.haier.bio.medical.incubator.c02;

/***
 * 超低温变频、T系列、双系统主控板通讯
 *
 */
public class C02IncubatorManager {
    private C02IncubatorSerialPort serialPort;
    private static C02IncubatorManager manager;

    public static C02IncubatorManager getInstance() {
        if (manager == null) {
            synchronized (C02IncubatorManager.class) {
                if (manager == null)
                    manager = new C02IncubatorManager();
            }
        }
        return manager;
    }

    private C02IncubatorManager() {

    }

    public void init(String path) {
        if(this.serialPort == null){
            this.serialPort = new C02IncubatorSerialPort();
            this.serialPort.init(path);
        }
    }

    public void enable() {
        if(null != this.serialPort){
            this.serialPort.enable();
        }
    }

    public void disable() {
        if(null != this.serialPort){
            this.serialPort.disable();
        }
    }

    public void release() {
        if(null != this.serialPort){
            this.serialPort.release();
            this.serialPort = null;
        }
    }

    public void sendData(byte[] data) {
        if (null != this.serialPort) {
            this.serialPort.sendData(data);
        }
    }

    public void changeListener(IC02IncubatorListener listener) {
        if(null != this.serialPort){
            this.serialPort.changeListener(listener);
        }
    }
}

