import com.rabbitmq.client.*;

import java.io.IOException;
import java.util.concurrent.*;

// clase que representa una estacion de radio que transmite
public class RadioStation extends RabbitMQProcess {

    private String exchangeName;
    private ScheduledExecutorService transmissionScheduler;
    private ScheduledFuture<?> transmissionHandle;

    private RadioSource source;

    public RadioStation(String host, String radioName, RadioSource source) throws
            IOException, TimeoutException {
        super(host);

        this.source = source;
        source.init();

        // declare BROADCAST exchange
        exchangeName = Configuration.RadioExchangePrefix + radioName;
        getChannel().exchangeDeclare(exchangeName, BuiltinExchangeType.FANOUT);
    }

    @Override
    public void run() {
        scheduleTransmission();
    }

    private void scheduleTransmission() {
        transmissionScheduler = Executors
                .newScheduledThreadPool(1);

        transmissionHandle =
                transmissionScheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                byte[] nextBlock = source.getNextByteBlock();

                try {
                    getChannel().basicPublish(exchangeName, "",
                            null, nextBlock);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, Configuration.RadioSendPeriodMilliseconds,
                        Configuration.RadioSendPeriodMilliseconds,
                        TimeUnit.MILLISECONDS);
    }

    @Override
    protected void close() throws IOException, TimeoutException {
        super.close();

        transmissionHandle.cancel(true);
        transmissionScheduler.shutdown();
        source.close();
    }

    public static void main(String[] argv) throws Exception {
        RadioSource source = argv.length == 2 ?
                new RadioSourceFile(argv[1]) :
                new RadioSourceRandomNumbers();
        RadioStation radio = new RadioStation(Configuration.RabbitMQHost, argv[0], source);
        radio.run();
    }

}
