import java.util.Base64;
import java.util.concurrent.ThreadLocalRandom;

// fuente generadora de contenido de una radio a partir de numeros al azar
public class RadioSourceRandomNumbers implements RadioSource {


    @Override
    public void init() {

    }

    @Override
    public byte[] getNextByteBlock() {
        int randomNum = ThreadLocalRandom.current().nextInt(0, 100 + 1);
        Logger.output(" [x] Sent: " + randomNum);
        return Base64.getEncoder().encode(Integer.toString(randomNum)
                .getBytes());
    }

    @Override
    public void close() {

    }
}
