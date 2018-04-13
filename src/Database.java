import java.util.List;

public interface Database<T extends DatabaseRow> {

    T getRow(String key);

    List<T> getRows();

    boolean createRow(T row);

    boolean updateRow(T row);

    boolean removeRow(String key);
}
