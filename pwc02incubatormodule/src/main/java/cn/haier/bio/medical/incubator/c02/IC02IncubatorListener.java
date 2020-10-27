package cn.haier.bio.medical.incubator.c02;

public interface IC02IncubatorListener {
    void onC02IncubatorReady();

    void onC02IncubatorConnected();

    void onC02IncubatorSwitchWriteModel();

    void onC02IncubatorSwitchReadModel();

    void onC02IncubatorPrint(String message);

    void onC02IncubatorException(Throwable throwable);

    void onC02IncubatorPackageReceived(byte[] data);
}
