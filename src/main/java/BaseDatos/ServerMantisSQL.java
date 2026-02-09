/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package BaseDatos;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 *
 * @author ASUS
 */
public class ServerMantisSQL {
    
        static Statement statement = null;
    
        public static void main() {
            
          
        }
        
        public static void Cargar(){
          String connectionUrl ="";
               /*     "jdbc:sqlserver://yourserver.database.windows.net:1433;"
                    + "database=AdventureWorks;"
                    + "user=yourusername@yourserver;"
                    + "password=<password>;"
                    + "encrypt=true;"
                    + "trustServerCertificate=false;"
                    + "loginTimeout=30;";   */
            
            System.out.println("Ruta actual: " + System.getProperty("user.dir"));

            String nombreFichero = "C:\\DianReenvio\\ConexionServer.txt";
// Declarar una variable BufferedReader
        BufferedReader br = null;
        try {
            // Crear un objeto BufferedReader al que se le pasa 
            //   un objeto FileReader con el nombre del fichero
            br = new BufferedReader(new FileReader(nombreFichero));
            // Leer la primera línea, guardando en un String
            String texto = br.readLine();
            // Repetir mientras no se llegue al final del fichero
            while(texto != null) {
                // Hacer lo que sea con la línea leída
                // En este ejemplo sólo se muestra por consola
                connectionUrl+=texto;
                // Leer la siguiente línea
                texto = br.readLine();
            }
        }
        // Captura de excepción por fichero no encontrado
        catch (FileNotFoundException ex) {
            //System.out.println("Error: Fichero no encontrado");
            ex.printStackTrace();
        }
        // Captura de cualquier otra excepción
        catch(Exception ex) {
            //System.out.println("Error de lectura del fichero");
            ex.printStackTrace();
        }
        // Asegurar el cierre del fichero en cualquier caso
        finally {
            try {
                // Cerrar el fichero si se ha podido abrir
                if(br != null) {
                    br.close();
                }
            }
            catch (Exception ex) {
                //System.out.println("Error al cerrar el fichero");
                ex.printStackTrace();
            }
        }
System.out.println("connectionUrl " +connectionUrl);
            ResultSet resultSet = null;
           // Statement statement= null;
            try {
                Class.forName("net.sourceforge.jtds.jdbc.Driver");
                 Connection connection = DriverManager.getConnection(connectionUrl);
                 statement  = connection.createStatement(); 

                // Create and execute a SELECT SQL statement.
           /*     String selectSql = "SELECT TOP 10 Title, FirstName, LastName from SalesLT.Customer";
                resultSet = statement.executeQuery(selectSql);

                // Print results from select statement
                while (resultSet.next()) {
                    System.out.println(resultSet.getString(2) + " " + resultSet.getString(3));
                }
            */
            }
            catch (SQLException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(ServerMantisSQL.class.getName()).log(Level.SEVERE, null, ex);
            }
            }
       //https://learn.microsoft.com/es-es/sql/connect/jdbc/step-3-proof-of-concept-connecting-to-sql-using-java?view=sql-server-ver16

        public static ResultSet Select(String  selectSql) {
                  ResultSet resultSet = null;
       if (selectSql.toLowerCase().contains("update") || selectSql.toLowerCase().contains("delete")){

         }else{
                          Cargar();
   
            try {
                resultSet = statement.executeQuery("SET DATEFORMAT 'YMD'; "+selectSql);
            } catch (SQLException ex) {
                Logger.getLogger(ServerMantisSQL.class.getName()).log(Level.SEVERE, null, ex);
            }
    
    }
            

        
        return resultSet;
        }
        
        
        public int sqlExec(String consulta){
            int filas = 0;
            
            if (consulta.toLowerCase().contains("delete")){
                filas = 403;
            }else{
           
                 Cargar();
                ResultSet resultSet = null;
            try {
                filas = statement.executeUpdate("SET DATEFORMAT 'YMD'; "+consulta);
            } catch (SQLException ex) {
                System.out.println("ErroMantis"+ex.toString());
                Logger.getLogger(ServerMantisSQL.class.getName()).log(Level.SEVERE, null, ex);
            }
              
                
            }
       
            return filas;
        }
        
   
}
