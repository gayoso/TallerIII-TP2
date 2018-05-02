import com.google.gson.Gson;
import com.rabbitmq.client.*;
import java.util.Base64;

import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.concurrent.*;

// esta clase es la aplicacion que los usuarios usan para conectarse
// al sistema y escuchar radios
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
            Logger.output("ERROR: Did you specify a username?");
            return false;
        }

        // define callback queue
        String callbackQueueName = getChannel().queueDeclare().getQueue();

        // create request
        UserConnectRequest request = new UserConnectRequest(username, radio,
                callbackQueueName);
        String requestJson = new Gson().toJson(request);

        // publish to usersDB exchange to start register connection operation
        DatabaseRequest usersdbRequest = new DatabaseRequest
                (Configuration.UsersTypeConnect, requestJson, username);
        getChannel().basicPublish(Configuration.UsersDBExchange,
                usersdbRequest.getRoutingKey(), null,
                new Gson().toJson(usersdbRequest).getBytes());

        final BlockingQueue<String> responseQueue =
                new ArrayBlockingQueue<String>(1);

        // es una cola temporaria, no sirve de nada el ack
        String callbackTag = getChannel().basicConsume(callbackQueueName,true,
                new DefaultConsumer(getChannel()) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                       AMQP.BasicProperties properties,
                                       byte[] body) throws IOException {
                responseQueue.offer(new String(body, "UTF-8"));
            }
        });

        String jsonResponse = responseQueue.take();
        getChannel().basicCancel(callbackTag);
        UserConnectResponse response = new Gson().fromJson(jsonResponse,
                UserConnectResponse.class);
        if (!response.isCouldConnect()) {
            Logger.output("ERROR: Connection refused, are " +
                    "you already connected on 3 devices?");
            return false;
        }

        connectionId = response.getConnectionId();
        radioExchange = Configuration.RadioExchangePrefix + response.getRadio();
        return true;
    }

    public boolean listenToRadio() throws IOException {

        if (radioExchange.equals("")) {
            return false;
        }

        // declare radio broadcast exchange
        getChannel().exchangeDeclare(radioExchange, BuiltinExchangeType.FANOUT);

        // declare temporary queue and bind
        String queueName = getChannel().queueDeclare().getQueue();
        getChannel().queueBind(queueName, radioExchange, "");
        Logger.output("Creating queue: " + queueName);

        // open new file for transmission
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        String transmissionName = "client" + "-" + username + "-" + radio +
                "-" + connectionId + "-" + sdf.format(new Date()) + ".wav";
        transmissionWriter = new FileOutputStream(transmissionName);

        Consumer consumer = new DefaultConsumer(getChannel()) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                       AMQP.BasicProperties properties,
                                       byte[] body) throws IOException {
                String message = new String(body, "UTF-8");
                Logger.output(" [x] Received '" + message + "'");
                byte[] decodedBody = Base64.getDecoder().decode(body);
                transmissionWriter.write(decodedBody);
            }
        };
        // es una cola temporaria, no sirve de nada el ack
        radioConsumeTag = getChannel().basicConsume(queueName, true,
                consumer);
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
                    getChannel().basicPublish("",
                            Configuration.KeepAliveQueue, null,
                            requestJson.getBytes());
                } catch (IOException e) {
                    Logger.output("IOException while listening to" +
                            "rado: " + radio + ", user: " + username +
                            ", connection id: " + connectionId);
                }
            }
        };
        keepAliveHandle = keepAliveScheduler.scheduleAtFixedRate
                (sendKeepAlive, Configuration.KeepAlivePeriodSeconds,
                        Configuration.KeepAlivePeriodSeconds, TimeUnit.SECONDS);
    }

    public void stopKeepAlive() {
        keepAliveHandle.cancel(true);
    }

    public void stopListeningToRadio() throws IOException {
        if (radioConsumeTag.equals("")) {
            Logger.output("ERROR: not listening to radio");
        } else {
            // create request
            UserDisconnectRequest request = new UserDisconnectRequest(username,
                    radio, connectionId);
            String requestJson = new Gson().toJson(request);

            // publish to DISCONNECTIONS queue
            getChannel().basicPublish("", Configuration.DisconnectionsQueue,
                    null, requestJson.getBytes());

            // stop receiving transmission
            getChannel().basicCancel(radioConsumeTag);
            radioConsumeTag = "";

            // close transmission file
            transmissionWriter.close();
            transmissionWriter = null;
        }
    }

    public void printOptions() {
        Logger.output("\n");
        Logger.output("Choose an action: ");
        Logger.output("\t" + "1. Set user");
        Logger.output("\t" + "2. Connect to radio");
        Logger.output("\t" + "3. Disconnect from radio");
        Logger.output("\t" + "4. Exit");
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
                Logger.output("Please specify a radio: ");
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
                Logger.output("Press CTRL+C to exit");
                return true;
            default:
                Logger.output("ERROR: Invalid option");
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

    @Override
    public void run() throws IOException {
        Scanner in = new Scanner(System.in);
        printOptions();

        boolean end = false;
        while (!end) {
            try {
                end = mainMenu(in);
            // esta excepcion aparece al apretar ctrl+c
            } catch (NoSuchElementException e) {
                end = true;
            } catch (InterruptedException e) {
                end = true;
            }
        }
    }


    public static void main(String[] argv) throws Exception {
        Client client = new Client(Configuration.RabbitMQHost);
        client.run();
    }

}
