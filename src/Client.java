import com.google.gson.Gson;
import com.rabbitmq.client.*;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.concurrent.*;

public class Client extends  RabbitMQProcess {

    private String radioExchange = "";

    private String username = "";
    private String radio;
    private int connectionId;
    private String radioConsumeTag = "";
    FileOutputStream transmissionWriter = null;

    private ScheduledExecutorService keepAliveScheduler =
            Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> keepAliveHandle;

    public Client(String host) throws IOException, TimeoutException {
        super(host);
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setRadio(String radio) {
        this.radio = radio;
    }

    public boolean requestConnectionToRadio() throws IOException,
            InterruptedException {

        if (username.equals("")) {
            System.out.println("ERROR: Did you specify a username?");
            return false;
        }

        // define callback queue
        String callbackQueueName = channel.queueDeclare().getQueue();

        // create request
        UserConnectRequest request = new UserConnectRequest(username, radio,
                callbackQueueName);
        String requestJson = new Gson().toJson(request);

        // publish to CONNECTIONS queue
        channel.basicPublish("", Configuration.ConnectionsQueue, null,
                requestJson.getBytes());

        final BlockingQueue<String> responseQueue =
                new ArrayBlockingQueue<String>(1);

        String callbackTag = channel.basicConsume(callbackQueueName,true,
                new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                       AMQP.BasicProperties properties,
                                       byte[] body) throws IOException {
                responseQueue.offer(new String(body, "UTF-8"));
            }
        });

        String jsonResponse = responseQueue.take();
        channel.basicCancel(callbackTag);
        UserConnectResponse response = new Gson().fromJson(jsonResponse,
                UserConnectResponse.class);
        if (!response.couldConnect) {
            System.out.println("ERROR: Connection refused, are " +
                    "you already connected on 3 devices?");
            return false;
        }

        connectionId = response.connectionId;
        radioExchange = Configuration.RadioExchangePrefix + response.radio;
        return true;
    }

    public boolean listenToRadio() throws IOException {

        if (radioExchange.equals("")) {
            return false;
        }

        // declare radio broadcast exchange
        channel.exchangeDeclare(radioExchange, BuiltinExchangeType.FANOUT);

        // declare temporary queue and bind
        String queueName = channel.queueDeclare().getQueue();
        channel.queueBind(queueName, radioExchange, "");
        System.out.println("Creating queue: " + queueName);

        // open new file for transmission
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        String transmissionName = "client" + "-" + username + "-" + radio +
                "-" + connectionId + "-" + sdf.format(new Date()) + ".wav";
        transmissionWriter = new FileOutputStream(transmissionName);

        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                       AMQP.BasicProperties properties,
                                       byte[] body) throws IOException {
                String message = new String(body, "UTF-8");
                System.out.println(" [x] Received '" + message + "'");
                transmissionWriter.write(body);
            }
        };
        radioConsumeTag = channel.basicConsume(queueName, true, consumer);
        return true;
    }

    public void scheduleKeepAlive() {
        final Runnable sendKeepAlive = new Runnable() {
            @Override
            public void run() {
                KeepAliveRequest request = new KeepAliveRequest(username,
                        connectionId, radio);
                String requestJson = new Gson().toJson(request);
                try {
                    channel.basicPublish("", Configuration.KeepAliveQueue,
                            null, requestJson.getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        keepAliveHandle = keepAliveScheduler.scheduleAtFixedRate
                (sendKeepAlive, 5,5, TimeUnit.SECONDS);
    }

    public void stopKeepAlive() {
        keepAliveScheduler.schedule(new Runnable() {
            @Override
            public void run() {
                keepAliveHandle.cancel(true);
            }
        }, 0, TimeUnit.SECONDS);
    }

    public void stopListeningToRadio() throws IOException {
        if (radioConsumeTag.equals("")) {
            System.out.println("ERROR: not listening to radio");
        } else {
            // create request
            UserDisconnectRequest request = new UserDisconnectRequest(username,
                    radio, connectionId);
            String requestJson = new Gson().toJson(request);

            // publish to DISCONNECTIONS queue
            channel.basicPublish("", Configuration.DisconnectionsQueue, null,
                    requestJson.getBytes());

            // stop receiving transmission
            channel.basicCancel(radioConsumeTag);
            radioConsumeTag = "";

            // close transmission file
            transmissionWriter.close();
            transmissionWriter = null;
        }
    }

    public void printOptions() {
        System.out.println("\n");
        System.out.println("Choose an action: ");
        System.out.println("\t" + "1. Set user");
        System.out.println("\t" + "2. Connect to radio");
        System.out.println("\t" + "3. Disconnect from radio");
        System.out.println("\t" + "4. Exit");
    }

    public boolean mainMenu(Scanner in) throws IOException,
            InterruptedException {
        String choiceStr = in.nextLine();
        int choice = Integer.parseInt(choiceStr);
        switch (choice) {
            case 1:
                System.out.print("Please specify a username: ");
                String username = in.nextLine();
                setUsername(username);
                break;
            case 2:
                System.out.println("Please specify a radio: ");
                String radio = in.nextLine();
                setRadio(radio);
                if (!requestConnectionToRadio()) {
                    break;
                }
                listenToRadio();
                scheduleKeepAlive();
                break;
            case 3:
                stopListeningToRadio();
                stopKeepAlive();
                break;
            case 4:
                System.out.println("Press CTRL+C to exit");
                return true;
            default:
                System.out.println("ERROR: Invalid option");
                break;
        }

        printOptions();
        return false;
    }

    @Override
    protected void close() throws IOException, TimeoutException {
        super.close();
        if (transmissionWriter != null) {
            transmissionWriter.close();
        }
    }


    public static void main(String[] argv) throws Exception {

        Scanner in = new Scanner(System.in);

        Client client = new Client(Configuration.RabbitMQHost);
        client.printOptions();

        boolean end = false;
        while (!end) {
            try {
                end = client.mainMenu(in);
            } catch (NoSuchElementException e) {
                end = true;
            }
        }
    }

}
