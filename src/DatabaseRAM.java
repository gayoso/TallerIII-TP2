import com.google.gson.internal.LinkedTreeMap;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

// clase que mantiene en memoria un mapa simulando una base de datos clave valor
public class DatabaseRAM<T extends DatabaseRow> implements Database<T> {

    private Map<String, T> database = new HashMap<>();

    public T getRow(String key) {
        T row = database.getOrDefault(key, null);
        return row;
    }

    @Override
    public List<T> getRows() {
        return new LinkedList<>(database.values());
    }

    @Override
    public boolean createRow(T row) {
        if (database.put(row.getPrimary_key(), row) == null) {
            return true;
        }
        return false;
    }

    @Override
    public boolean updateRow(T row) {
        if (!database.containsKey(row.getPrimary_key())) {
            return createRow(row);
        }
        database.put(row.getPrimary_key(), row);
        return true;
    }

    @Override
    public boolean removeRow(String primary_key) {
        if (database.remove(primary_key) != null) {
            return true;
        }
        return false;
    }

    protected Map<String, T> getDatabase() {
        return database;
    }

    protected void setDatabase(Map<String, T> db) {
        this.database = db;
    }
}
