import com.google.gson.Gson;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

// clase que mantiene en memoria un mapa con el contenido de un archivo json
// y permite cargarlo o guardarlo a disco
public class DatabaseJson<T extends DatabaseRow> extends DatabaseRAM<T> {

    private String filename;
    private Class<T> classOfT;

    public DatabaseJson(String filename, Class<T> classOfT) {
        this.filename = filename;
        this.classOfT = classOfT;

        loadFromFile();
    }

    @Override
    public boolean createRow(T row) {
        boolean result = super.createRow(row);
        if (result) {
            saveToFile();
        }
        return result;
    }

    @Override
    public boolean updateRow(T row) {
        boolean result = super.updateRow(row);
        if (result) {
            saveToFile();
        }
        return result;
    }

    @Override
    public boolean removeRow(String primary_key) {
        boolean result = super.removeRow(primary_key);
        if (result) {
            saveToFile();
        }
        return result;
    }

    private void loadFromFile() {
        try {

            BufferedReader br = new BufferedReader(new FileReader(filename));
            Map<String, T> db = new HashMap<>();

            Gson gson = new Gson();
            String line;
            while ((line = br.readLine()) != null) {
                Logger.output(line);
                T row = gson.fromJson(line, classOfT);
                db.put(row.getPrimary_key(), row);
            }
            br.close();
            setDatabase(db);

        } catch (IOException e) {
            Logger.output("Unable to load database from file: " + filename);
        }
    }

    private void saveToFile() {
        try {
            PrintWriter writer = new PrintWriter(new FileWriter(filename));
            Map<String, T> db = getDatabase();
            Gson gson = new Gson();

            for (String key : db.keySet()) {
                String jsonRow = gson.toJson(db.get(key), classOfT);
                writer.println(jsonRow);
            }
            writer.close();
        } catch (IOException e) {
            Logger.output("Unable to save database to file: " + filename);
        }
    }
}
