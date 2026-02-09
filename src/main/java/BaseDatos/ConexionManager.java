package BaseDatos;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Charly Cimino Aprendé más Java en mi canal:
 * https://www.youtube.com/c/CharlyCimino Encontrá más código en mi repo de
 * GitHub: https://github.com/CharlyCimino
 */
public class ConexionManager {

    private static ConexionManager instancia;
    private Connection connection;

    private ConexionManager() {
        try {
            Class.forName("net.sourceforge.jtds.jdbc.Driver");
            connection = DriverManager.getConnection(cargarConnectionUrl());
        } catch (SQLException e) {
            System.out.println("Error al conectar a la base de datos" + e.toString());
            throw new RuntimeException("Error al conectar a la base de datos", e);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(ConexionManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }


    
    public static synchronized ConexionManager getInstancia() {
    try {
        if (instancia == null) {
            instancia = new ConexionManager();
        } else if (instancia.connection == null || instancia.connection.isClosed()) {
            System.out.println("Reconectando a la base de datos...");
            instancia = new ConexionManager();
        }
    } catch (SQLException e) {
        System.out.println("Error verificando conexión: " + e);
        throw new RuntimeException("Error verificando conexión", e);
    }
    return instancia;
}


    public Connection getConnection() {
        return connection;
    }

    public void cerrar() {
        try {
            if (connection != null && !connection.isClosed()) {
                System.out.println("Cerrando conexión...");
                connection.close();
                instancia = null;
            }
        } catch (SQLException e) {
            System.out.println("Error al cerrar conexión: " + e);
        }
    }

    public static String cargarConnectionUrl() {
        StringBuilder connectionUrl = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader("C:\\DianReenvio\\ConexionServer.txt"))) {
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
