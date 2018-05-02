import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Base64;

// fuente generadora de contenido de radio a partir de un archivo
public class RadioSourceFile implements RadioSource {

    String filename;
    FileInputStream audio;
    int bytesPerRead;
    byte[] buffer;

    public RadioSourceFile(String filename) {
        this.filename = filename;
        this.bytesPerRead = 1000 * Configuration.RadioSendPeriodMilliseconds;
        buffer = new byte[this.bytesPerRead];
    }

    @Override
    public void init() throws FileNotFoundException {
        audio = new FileInputStream(filename);
    }

    @Override
    public byte[] getNextByteBlock() {
        try {
            int bytesSent = audio.read(buffer);
            if (bytesSent == -1) {
                audio.close();
                audio = new FileInputStream(filename);
            }
            Logger.output(" [x] Sent: " + bytesSent + " bytes");
            return Base64.getEncoder().encode(buffer);
        } catch (IOException e) {
            Logger.output("IOException while reading blocks from file");
        }
        return "STATIC".getBytes();
    }

    @Override
    public void close() {
        try {
            audio.close();
        } catch (IOException e) {
            Logger.output("IOException while closing");
        }
    }
}
