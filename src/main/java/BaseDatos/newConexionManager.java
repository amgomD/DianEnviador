package BaseDatos;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class newConexionManager {

    private static newConexionManager instancia;
    private String connectionUrl;

    private newConexionManager() {
        try {
            Class.forName("net.sourceforge.jtds.jdbc.Driver");
            connectionUrl = cargarConnectionUrl();
        } catch (Exception e) {
            throw new RuntimeException("Error inicializando conexión", e);
        }
    }

    public static synchronized newConexionManager getInstancia() {
        if (instancia == null) {
            instancia = new newConexionManager();
        }
        return instancia;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(connectionUrl);
    }

    public static String cargarConnectionUrl() {
        StringBuilder connectionUrl = new StringBuilder();

        try (BufferedReader br = new BufferedReader(
                new FileReader("C:\\DianReenvio\\ConexionServer.txt"))) {

            String linea;
            while ((linea = br.readLine()) != null) {
                connectionUrl.append(linea);
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return connectionUrl.toString();
    }
}