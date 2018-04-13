import java.util.concurrent.ThreadLocalRandom;

public class RadioSourceRandomNumbers implements RadioSource {


    @Override
    public void init() {

    }

    @Override
    public byte[] getNextByteBlock() {
        int randomNum = ThreadLocalRandom.current().nextInt(0, 100 + 1);
        System.out.println(" [x] Sent: " + randomNum);
        return Integer.toString(randomNum).getBytes();
    }

    @Override
    public void close() {

    }
}
