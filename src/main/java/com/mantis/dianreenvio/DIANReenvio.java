/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.mantis.dianreenvio;

import com.formdev.flatlaf.FlatLightLaf;
import java.awt.Component;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.security.Security;
import java.util.Locale;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 *
 * @author Charly Cimino
 */
public class DIANReenvio {

    public static void main(String[] args) {
      
        // ✅ Activar Look and Feel FlatLaf antes de crear UI
        try {
            Locale.setDefault(Locale.US); // fuerza punto como separador decimal
            System.setProperty("flatlaf.uiScale", "1.25");
//            UIManager.put("@scaleFactor", 1.25);
            UIManager.setLookAndFeel(new FlatLightLaf());

            //UIManager.put("TitlePane.background", Color.BLACK);
            //UIManager.put("TitlePane.foreground", Color.WHITE);
            //UIManager.put("TitlePane.inactiveBackground", Color.BLACK);
            //UIManager.put("TitlePane.inactiveForeground", Color.BLACK);
        } catch (Exception ex) {
            System.err.println("Error al aplicar FlatLaf");
            ex.printStackTrace();
        }



        // Establece el comportamiento global al inicio de la aplicación
        KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        manager.addKeyEventDispatcher(new KeyEventDispatcher() {
            @Override
            public boolean dispatchKeyEvent(KeyEvent e) {
                // Solo actúa cuando la tecla Enter es presionada
                if (e.getID() == KeyEvent.KEY_PRESSED && e.getKeyCode() == KeyEvent.VK_ENTER) {
                    Component comp = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
                    if (comp instanceof JTextArea) {
                        // Permitir Enter normal en JTextArea (para salto de línea)
                        return false;
                    }

                    if (comp instanceof JTable) {
                        // Permitir Enter normal en JTextArea (para salto de línea)
                        return false;
                    }

                    // Transferir el foco como lo haría la tecla Tab
                    KeyboardFocusManager.getCurrentKeyboardFocusManager().focusNextComponent();
                    return true; // Consume el evento
                }
                return false; // No hace nada con otras teclas
            }
        });

        // ✅ Crear ventana después del Look and Feel
        SwingUtilities.invokeLater(() -> {

            /* DashBoard lo = new DashBoard(0, "", "", "", 1);
            lo.setLocationRelativeTo(null); // Centra la ventana
            lo.setExtendedState(JFrame.MAXIMIZED_BOTH);
            lo.setVisible(true);
            
           /*Factura pos = new Factura();
            pos.setLocationRelativeTo(null); // Centra la ventana
            pos.setVisible(true);
            
             */
            Enviador login = new Enviador();
            login.setLocationRelativeTo(null); // Centra la ventana
            login.setVisible(true);

            /*
            Facturacion pos = new Facturacion(0, "", "", "", 1);
            pos.setLocationRelativeTo(null); // Centra la ventana
            pos.setExtendedState(JFrame.MAXIMIZED_BOTH);
            pos.setVisible(true);*/
        });
    }

}