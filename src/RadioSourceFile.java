import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Base64;

public class RadioSourceFile implements RadioSource {

    String filename;
    FileInputStream audio;
    int bytesPerRead;
    byte[] buffer;

    public RadioSourceFile(String filename, int bitrate) {
        this.filename = filename;
        this.bytesPerRead = 1000 * Configuration.RadioSendPeriodSeconds;
        buffer = new byte[this.bytesPerRead];
    }

    @Override
    public void init() throws FileNotFoundException {
        audio = new FileInputStream(filename);
    }

    @Override
    public byte[] getNextByteBlock() {
        try {
            audio.read(buffer);
            return Base64.getEncoder().encode(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "STATIC".getBytes();
    }

    @Override
    public void close() {

    }
}
