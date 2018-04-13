import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class Configuration {

    public static int RadioSendPeriodSeconds = 2;
    public static int KeepAlivePeriodSeconds = 5;
    public static int SecondsUntilDropConnection = 10;
    public static int MaxConnectionsPerFreeUser = 3;
    public static int MaxConnectionsPerUnlimitedUser = 999;

    public static String RabbitMQHost = "localhost";

    public static String UsersDBExchange = "USERS_DB";
    public static String UsersDBConnectTag = "connect";
    public static String UsersDBDisconnectTag = "disconnect";
    public static String UsersDBKeepAliveTag = "keepalive";
    public static String UsersStatisticsExchange = "USERS_STATS";
    public static int UsersStatisticsPeriodSeconds = 10;
    public static int UserStatisticsN = 100;

    public static String RadiosDBExchange = "RADIOS_DB";
    public static String RadiosDBConnectTag = "connect";
    public static String RadiosDBDisconnectTag = "disconnect";
    public static String RadiosStatisticsExchange = "RADIOS_STATS";
    public static int RadioStatisticsPeriodSeconds = 10;

    public static String ConnMgrUsersDBResponseQueue =
            "usersDBResponseQueueName";

    public static String ConnectionsQueue = "CONNECTIONS";
    public static String DisconnectionsQueue = "DISCONNECTIONS";
    public static String KeepAliveQueue = "KEEP_ALIVE";

    public static String RadioExchangePrefix = "BROADCAST-";

    public static String LogsExchange = "LOGS";
    public static String LogsConnectionTag = "connect";
    public static String LogsDisconnectionTag = "disconnect";

    public static boolean loadConfiguration(String configFilename) {

        try {
            // read json config
            BufferedReader br = new BufferedReader(
                    new FileReader( configFilename));
            String jsonString = "";
            String s;
            while ((s = br.readLine()) != null) {
                jsonString += s;
            }

            // esto es para que gson serialize variables estaticas
            GsonBuilder gsonBuilder  = new GsonBuilder();
            gsonBuilder.excludeFieldsWithModifiers(
                    java.lang.reflect.Modifier.TRANSIENT);

            Gson gson = gsonBuilder.create();
            // load to object
            Configuration config = gson.fromJson(jsonString,
                    Configuration.class);

            return true;
        }
        catch (FileNotFoundException e) {
            /*Logger.log("Monitor", "config file: " + configFilename +
                    " not found", Logger.logLevel.ERROR);*/
        } catch (IOException e) {
            /*Logger.log("Monitor", "config file: " + configFilename +
                    " could not be read", Logger.logLevel.ERROR);*/
        }

        return false;
    }
}
