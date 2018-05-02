import java.util.List;

// interfas para conectar una base de datos al sistema
public interface Database<T extends DatabaseRow> {

    T getRow(String key);

    List<T> getRows();

    boolean createRow(T row);

    boolean updateRow(T row);

    boolean removeRow(String key);
}
