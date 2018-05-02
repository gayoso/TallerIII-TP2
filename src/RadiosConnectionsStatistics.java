import java.util.HashMap;
import java.util.Map;

// clase que representa las estadistias de cantidad de conexiones por radio
public class RadiosConnectionsStatistics {

    private Map<String, Integer> radioConnections = new HashMap<>();

    public Map<String, Integer> getRadioConnections() {
        return radioConnections;
    }

    public void setRadioConnections(Map<String, Integer> radioConnections) {
        this.radioConnections = radioConnections;
    }
}
