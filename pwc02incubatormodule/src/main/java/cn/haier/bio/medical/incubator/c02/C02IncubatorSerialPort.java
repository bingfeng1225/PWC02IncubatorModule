package cn.haier.bio.medical.incubator.c02;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import java.io.IOException;
import java.lang.ref.WeakReference;

import cn.qd.peiwen.serialport.PWSerialPortHelper;
import cn.qd.peiwen.serialport.PWSerialPortListener;
import cn.qd.peiwen.serialport.PWSerialPortState;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

class C02IncubatorSerialPort implements PWSerialPortListener {
    private ByteBuf buffer;
    private HandlerThread thread;
    private PWSerialPortHelper helper;
    private C02IncubatorHandler handler;

    private boolean ready = false;
    private boolean enabled = false;
    private WeakReference<IC02IncubatorListener> listener;

    public C02IncubatorSerialPort() {
    }

    public void init(String path) {
        this.createHandler();
        this.createHelper(path);
        this.createBuffer();
    }

    public void enable() {
        if (this.isInitialized() && !this.enabled) {
            this.enabled = true;
            this.helper.open();
        }
    }

    public void disable() {
        if (this.isInitialized() && this.enabled) {
            this.enabled = false;
            this.helper.close();
        }
    }

    public void release() {
        this.listener = null;
        this.destoryHandler();
        this.destoryHelper();
        this.destoryBuffer();
    }

    public void sendData(byte[] data) {
        if (this.isInitialized() && this.enabled) {
            Message msg = Message.obtain();
            msg.what = 0x00;
            msg.obj = data;
            this.handler.sendMessage(msg);
        }
    }

    public void changeListener(IC02IncubatorListener listener) {
        this.listener = new WeakReference<>(listener);
    }

    private boolean isInitialized() {
        if (this.handler == null) {
            return false;
        }
        if (this.helper == null) {
            return false;
        }
        return this.buffer != null;
    }

    private void createHelper(String path) {
        if (this.helper == null) {
            this.helper = new PWSerialPortHelper("C02IncubatorSerialPort");
            this.helper.setTimeout(5);
            this.helper.setPath(path);
            this.helper.setBaudrate(9600);
            this.helper.init(this);
        }
    }

    private void destoryHelper() {
        if (null != this.helper) {
            this.helper.release();
            this.helper = null;
        }
    }

    private void createHandler() {
        if (this.thread == null && this.handler == null) {
            this.thread = new HandlerThread("C02IncubatorSerialPort");
            this.thread.start();
            this.handler = new C02IncubatorHandler(this.thread.getLooper());
        }
    }

    private void destoryHandler() {
        if (null != this.thread) {
            this.thread.quitSafely();
            this.thread = null;
            this.handler = null;
        }
    }

    private void createBuffer() {
        if (this.buffer == null) {
            this.buffer = Unpooled.buffer(4);
        }
    }

    private void destoryBuffer() {
        if (null != this.buffer) {
            this.buffer.release();
            this.buffer = null;
        }
    }

    private void write(byte[] data) {
        if (!this.isInitialized() || !this.enabled) {
            return;
        }
        this.helper.writeAndFlush(data);
        if (null != this.listener && null != this.listener.get()) {
            this.listener.get().onC02IncubatorPrint("C02IncubatorSerialPort Send:" + C02IncubatorTools.bytes2HexString(data, true, ", "));
        }
    }

    private void switchReadModel() {
        if (null != this.listener && null != this.listener.get()) {
            this.listener.get().onC02IncubatorSwitchReadModel();
        }
    }

    private void switchWriteModel() {
        if (null != this.listener && null != this.listener.get()) {
            this.listener.get().onC02IncubatorSwitchWriteModel();
        }
    }

    private boolean ignorePackage() {
        boolean result = false;
        int index = C02IncubatorTools.indexOf(this.buffer, C02IncubatorTools.HEADER);
        if (index != -1) {
            result = true;
            byte[] data = new byte[index];
            this.buffer.readBytes(data, 0, data.length);
            this.buffer.discardReadBytes();
            if (null != this.listener && null != this.listener.get()) {
                this.listener.get().onC02IncubatorPrint("C02IncubatorSerialPort 指令丢弃:" + C02IncubatorTools.bytes2HexString(data, true, ", "));
            }
        }
        return result;
    }


    private boolean processBytesBuffer() {
        while (this.buffer.readableBytes() < 4) {
            return true;
        }
        byte[] header = new byte[C02IncubatorTools.HEADER.length];
        this.buffer.getBytes(0, header);
        byte command = this.buffer.getByte(3);
        if (!C02IncubatorTools.checkHeader(header)) {
            return this.ignorePackage();
        }
        if (!C02IncubatorTools.checkCommand(command)) {
            //当前指令不合法 丢掉正常的包头以免重复判断
            this.buffer.resetReaderIndex();
            this.buffer.skipBytes(2);
            this.buffer.discardReadBytes();
            return this.ignorePackage();
        }

        int frameLength = 0xFF & this.buffer.getByte(2) + 3;
        if (this.buffer.readableBytes() < frameLength) {
            return true;
        }
        this.buffer.markReaderIndex();
        byte[] data = new byte[frameLength];
        this.buffer.readBytes(data, 0, data.length);

        if (!C02IncubatorTools.checkFrame(data)) {
            this.buffer.resetReaderIndex();
            //当前包不合法 丢掉正常的包头以免重复判断
            this.buffer.skipBytes(2);
            this.buffer.discardReadBytes();
            return this.ignorePackage();
        }
        this.buffer.discardReadBytes();
        if (!this.ready) {
            this.ready = true;
            if (null != this.listener && null != this.listener.get()) {
                this.listener.get().onC02IncubatorReady();
            }
        }
        this.switchWriteModel();
        if (null != this.listener && null != this.listener.get()) {
            this.listener.get().onC02IncubatorPrint("C02IncubatorSerialPort Recv:" + C02IncubatorTools.bytes2HexString(data, true, ", "));
        }
        Message msg = Message.obtain();
        msg.obj = data;
        msg.what = 0x01;
        this.handler.sendMessage(msg);
        return true;
    }

    @Override
    public void onConnected(PWSerialPortHelper helper) {
        if (!this.isInitialized() || !helper.equals(this.helper)) {
            return;
        }
        this.ready = false;
        this.buffer.clear();
        this.switchReadModel();
        if (null != this.listener && null != this.listener.get()) {
            this.listener.get().onC02IncubatorConnected();
        }
    }

    @Override
    public void onReadThreadReleased(PWSerialPortHelper helper) {
        if (!this.isInitialized() || !helper.equals(this.helper)) {
            return;
        }
        if (null != this.listener && null != this.listener.get()) {
            this.listener.get().onC02IncubatorPrint("C02IncubatorSerialPort read thread released");
        }
    }

    @Override
    public void onException(PWSerialPortHelper helper, Throwable throwable) {
        if (!this.isInitialized() || !helper.equals(this.helper)) {
            return;
        }
        this.ready = false;
        if (null != this.listener && null != this.listener.get()) {
            this.listener.get().onC02IncubatorException(throwable);
        }
    }

    @Override
    public void onStateChanged(PWSerialPortHelper helper, PWSerialPortState state) {
        if (!this.isInitialized() || !helper.equals(this.helper)) {
            return;
        }
        if (null != this.listener && null != this.listener.get()) {
            this.listener.get().onC02IncubatorPrint("C02IncubatorSerialPort state changed: " + state.name());
        }
    }

    @Override
    public boolean onByteReceived(PWSerialPortHelper helper, byte[] buffer, int length) throws IOException {
        if (!this.isInitialized() || !helper.equals(this.helper)) {
            return false;
        }
        this.buffer.writeBytes(buffer, 0, length);
        return this.processBytesBuffer();
    }


    private class C02IncubatorHandler extends Handler {
        public C02IncubatorHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 0: {
                    byte[] message = (byte[]) msg.obj;
                    if (null != message && message.length > 0) {
                        C02IncubatorSerialPort.this.write(message);
                    }
                    C02IncubatorSerialPort.this.switchReadModel();
                    break;
                }
                case 0x01:
                    if (null != C02IncubatorSerialPort.this.listener && null != C02IncubatorSerialPort.this.listener.get()) {
                        C02IncubatorSerialPort.this.listener.get().onC02IncubatorPackageReceived((byte[]) msg.obj);
                    }
                    break;
            }

        }
    }
}
