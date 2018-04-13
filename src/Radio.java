import com.rabbitmq.client.*;

import java.io.IOException;
import java.util.concurrent.*;

public class Radio extends RabbitMQProcess {

    private String exchangeName;
    private ScheduledExecutorService transmissionScheduler;
    private ScheduledFuture<?> transmissionHandle;

    private RadioSource source;

    public Radio(String host, String radioName, RadioSource source) throws
            IOException, TimeoutException {
        super(host);

        this.source = source;
        source.init();

        // declare BROADCAST exchange
        exchangeName = Configuration.RadioExchangePrefix + radioName;
        channel.exchangeDeclare(exchangeName, BuiltinExchangeType.FANOUT);

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
                    channel.basicPublish(exchangeName, "", null, nextBlock);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, Configuration.RadioSendPeriodSeconds,
                        Configuration.RadioSendPeriodSeconds,
                        TimeUnit.SECONDS);
    }

    @Override
    protected void close() throws IOException, TimeoutException {
        super.close();

        transmissionHandle.cancel(true);
        transmissionScheduler.shutdown();
        source.close();
    }

    public static void main(String[] argv) throws Exception {
        RadioSource source = argv.length == 3 ?
                new RadioSourceFile(argv[1],
                        Integer.parseInt(argv[2]) * 1024) :
                new RadioSourceRandomNumbers();
        Radio radio = new Radio(Configuration.RabbitMQHost, argv[0], source);
    }

}
