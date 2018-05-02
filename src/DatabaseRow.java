// clase que detalla la estructura de las filas de una base de datos del sistema
public abstract class DatabaseRow {

    private String primary_key;

    public DatabaseRow(String primary_key) {
        this.setPrimary_key(primary_key);
    }

    public String getPrimary_key() {
        return primary_key;
    }

    public void setPrimary_key(String primary_key) {
        this.primary_key = primary_key;
    }
}
