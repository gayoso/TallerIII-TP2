import java.io.FileNotFoundException;

// interfaz para representar la fuente generadora de contenido de una radio
public interface RadioSource {

    void init() throws FileNotFoundException;

    byte[] getNextByteBlock();

    void close();
}
