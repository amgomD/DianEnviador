package com.mantis.dianreenvio;


import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Charly Cimino Aprendé más Java en mi canal:
 * https://www.youtube.com/c/CharlyCimino Encontrá más código en mi repo de
 * GitHub: https://github.com/CharlyCimino
 */
public class ConexionWebservice {

    private String URLServicio = "";

    public ConexionWebservice() {
    }

    public String getURLServicio() {
        String rutaURL = "C:\\DianReenvio\\UrlServidor.txt";
            
   
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(rutaURL));
            String texto = br.readLine();
            while (texto != null) {
                URLServicio = texto;
                texto = br.readLine();
            }

        } catch (FileNotFoundException ex) {
            //System.out.println("Error: Fichero no encontrado");
            ex.printStackTrace();
        } catch (IOException ex) {
          
        } 
 
        return URLServicio;
    }

    public void setURLServicio(String URLServicio) {
        this.URLServicio = URLServicio;
    }

}
