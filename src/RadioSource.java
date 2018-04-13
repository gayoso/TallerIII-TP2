import java.io.FileNotFoundException;

public interface RadioSource {

    void init() throws FileNotFoundException;

    byte[] getNextByteBlock();

    void close();
}
