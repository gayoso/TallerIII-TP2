import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

// clase para encapsular, cargar y guardar la configuracion de parametros
public class Configuration {

    public static int RadioSendPeriodMilliseconds = 1000;
    public static int RadioAudiofileBytesPerSecond = 102400;
    public static int KeepAlivePeriodSeconds = 5;
    public static int SecondsUntilDropConnection = 10;
    public static int MaxConnectionsPerFreeUser = 3;
    public static int MaxConnectionsPerUnlimitedUser = 999;

    public static String RabbitMQHost = "localhost";

    public static int PoolSizeForDBstatistics = 1;

    public static String UsersDBExchange = "USERS_DB";
    public static int UsersTypeConnect = 1;
    public static int UsersTypeDisconnect = 2;
    public static int UsersTypeKeepAlive = 3;
    public static String UsersStatisticsExchange = "USERS_STATS";
    public static int UsersStatisticsPeriodSeconds = 10;
    public static int UserStatisticsN = 100;

    public static String RadiosDBExchange = "RADIOS_DB";
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

    public static String maskListToStr(List<String> masks) {
        return String.join("", masks)
                .replace(".", "")
                .replace("#", "");
    }

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
        }
        catch (FileNotFoundException e) {
            Logger.output("FileNotFoundException loading " +
                    "configuration file, using defaults");
        } catch (IOException e) {
            Logger.output("IOException loading configuration file, using " +
                    "defaults");
        } finally {
            return true;
        }
    }
}
