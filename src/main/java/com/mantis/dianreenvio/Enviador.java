/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package com.mantis.dianreenvio;

import BaseDatos.ConexionManager;
import BaseDatos.ServerMantisSQL;
import java.awt.Color;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author Charly Cimino
 */
public class Enviador extends javax.swing.JFrame {

    /**
     * Creates new form Enviador
     */
    boolean xdetener = false;
    TimerTask tarea = null;
    Timer timer = new Timer(true); // hilo daemon
    private volatile boolean detenido = false;
    ServerMantisSQL Con = new ServerMantisSQL();
    DefaultTableModel modelFactura = null;
    ConexionWebservice conws = new ConexionWebservice();

    public Enviador() {
        initComponents();
        this.setIconImage(new ImageIcon(getClass().getResource("/logo35azul.png")).getImage());

        // Fecha actual
        Date hoy = new Date();

// Asignar a los datepickers
        iFacFec.setDate(hoy);
        fFacFec.setDate(hoy);

        modelFactura = (DefaultTableModel) TablaFactura.getModel();
        seltodos.setSelected(false);
        CargarFacturas();
        if (seltodos.isSelected()) {
            auto();
        }

    }

    public void limpiarGrid() {

        int filas = modelFactura.getRowCount();
        for (int a = 0; filas > a; a++) {
            modelFactura.removeRow(0);
        }
    }

    public void CargarFacturas() {
        limpiarGrid();

        List<Object> paramsFactura = new ArrayList<>();
        List<Object> paramsComprobante = new ArrayList<>();

        StringBuilder whereComun = new StringBuilder();

// Número documento
        if (!eFacNro.getText().trim().isEmpty()) {
            whereComun.append(" AND %1$s LIKE ?");
            String valor = "%" + eFacNro.getText().trim() + "%";
            paramsFactura.add(valor);
            paramsComprobante.add(valor);
        }

// Fechas
        if (iFacFec.getDate() != null && fFacFec.getDate() != null) {
            whereComun.append(" AND %2$s >= ? AND %3$s <= ?");

            LocalDate ini = iFacFec.getDate().toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();

            LocalDate fin = fFacFec.getDate().toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();

            Timestamp tsIni = Timestamp.valueOf(ini.atStartOfDay());
            Timestamp tsFin = Timestamp.valueOf(fin.atTime(23, 59, 59));

            paramsFactura.add(tsIni);
            paramsFactura.add(tsFin);

            paramsComprobante.add(tsIni);
            paramsComprobante.add(tsFin);
        }

        StringBuilder whereFactura = new StringBuilder();
        String comboDian = eTipEle.getSelectedItem().toString();

        if (!comboDian.equalsIgnoreCase("TODOS")) {
            whereFactura.append(" AND t.TipEle = ?");
            paramsFactura.add(0, comboDian);
        } else {
            whereFactura.append(" AND t.TipEle IN ('FAC','NOT','DSO','DDS','NTB')");
        }

        whereFactura.append(whereComun);

        String whereFacturaFinal = String.format(
                whereFactura.toString(),
                "f.FacNro",
                "f.FacFec",
                "f.FacFec"
        );

        StringBuilder whereComprobante = new StringBuilder();

        if (!comboDian.equalsIgnoreCase("TODOS")) {
            whereComprobante.append(" AND t.TipEle = ?");
            paramsComprobante.add(0, comboDian);
        } else {
            whereComprobante.append(" AND t.TipEle IN ('DSO','DDS')");
        }

        whereComprobante.append(whereComun);

        String whereComprobanteFinal = String.format(
                whereComprobante.toString(),
                "c.ComNum",
                "c.ComFecCre",
                "c.ComFecCre"
        );

        //and ( comenvcode IS NULL OR comenvcode = '' OR comenvcode = '500')  
        //and ( facenvcode IS NULL OR facenvcode = '' OR facenvcode = '500') 
        /* String sql = """
SELECT 
    f.FacSec        AS Sec,
    f.FacFec        AS Fecha,
    f.FacNro        AS Numero,
    n.NitIde        AS Nit,
    cli.CliNom      AS Cliente,
    f.FacEleStatus  AS Estado
FROM factura f
LEFT JOIN tipos t  ON f.factipcod = t.tipcod
left join facturaenvio e on e.facsec = f.facsec                     
LEFT JOIN clientes cli ON f.FacNitSec = cli.NitSec AND f.FacCliSec = cli.CliSec
LEFT JOIN Nit n ON f.FacNitSec = n.NitSec
WHERE FacEst = 'A' and (f.FacEleStatus IS NULL OR f.FacEleStatus = '' OR f.FacEleStatus = 'K') 
""" + whereFacturaFinal + """

UNION ALL

SELECT 
    c.ComSec        AS Sec,
    c.ComFecEnc     AS Fecha,
    c.ComNum        AS Numero,
    n.NitIde        AS Nit,
    cli.CliNom      AS Cliente,
    c.ComEleStatus  AS Estado
FROM Comprobante c
LEFT JOIN tipos t  ON c.ComTipCod = t.tipcod
left join ComprobanteEnvio e on e.comsec = c.comsec
LEFT JOIN clientes cli ON c.ComNitSec = cli.NitSec AND c.ComCliSec = cli.CliSec
LEFT JOIN Nit n ON c.ComNitSec = n.NitSec
WHERE LTRIM(RTRIM(comnum)) <> '' and c.comsec NOT LIKE '[0-9]%' and  ComEst = 'A' and (c.ComEleStatus IS NULL OR c.ComEleStatus = '' OR c.ComEleStatus = 'K') 
""" + whereComprobanteFinal + """

ORDER BY Fecha DESC
""";*/
        String sql
                = "SELECT DISTINCT "
                + "    f.FacSec AS Sec, "
                + "    f.FacFec AS Fecha, "
                + "    f.FacNro AS Numero, "
                + "    n.NitIde AS Nit, "
                + "    cli.CliNom AS Cliente, "
                + "    f.FacEleStatus AS Estado "
                + "FROM factura f "
                + "LEFT JOIN tipos t ON f.factipcod = t.tipcod "
                + "LEFT JOIN facturaenvio e ON e.facsec = f.facsec "
                + "LEFT JOIN clientes cli "
                + "       ON f.FacNitSec = cli.NitSec "
                + "      AND f.FacCliSec = cli.CliSec "
                + "LEFT JOIN Nit n ON f.FacNitSec = n.NitSec "
                + "WHERE ( "
                + "        (e.facenvresp LIKE '%" + eError.getText() + "%') "
                + "     OR (e.facenvresp IS NULL) "
                + ") "
                + "AND f.FacEst = 'A' "
                + "AND (f.FacEleStatus IS NULL OR f.FacEleStatus = '' OR f.FacEleStatus = 'K') "
                + whereFacturaFinal + " "
                + "ORDER BY Fecha DESC";
                /*+ "UNION ALL "
                + "SELECT DISTINCT "
                + "    c.ComSec AS Sec, "
                + "    c.ComFecCre AS Fecha, "
                + "    c.ComNum AS Numero, "
                + "    n.NitIde AS Nit, "
                + "    cli.CliNom AS Cliente, "
                + "    c.ComEleStatus AS Estado "
                + "FROM Comprobante c "
                + "LEFT JOIN tipos t ON c.ComTipCod = t.tipcod "
                + "LEFT JOIN ComprobanteEnvio e ON e.comsec = c.comsec "
                + "LEFT JOIN clientes cli "
                + "       ON c.ComNitSec = cli.NitSec "
                + "      AND c.ComCliSec = cli.CliSec "
                + "LEFT JOIN Nit n ON c.ComNitSec = n.NitSec "
                + "WHERE ((e.comenvresp LIKE '%" + eError.getText() + "%') OR (e.comenvresp IS NULL) ) "
                + " AND LTRIM(RTRIM(c.ComNum)) <> '' "
                + "AND c.ComSec NOT LIKE '[0-9]%' "
                + "AND c.ComEst = 'A' "
                + "AND (c.ComEleStatus IS NULL OR c.ComEleStatus = '' OR c.ComEleStatus = 'K') "
                + whereComprobanteFinal + " "*/
                

        System.out.println(sql);

        List<Object> parametrosFinales = new ArrayList<>();
        parametrosFinales.addAll(paramsFactura); // Factura
        //parametrosFinales.addAll(paramsComprobante); // Comprobante

        try (Connection con = ConexionManager.getInstancia().getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {

            for (int i = 0; i < parametrosFinales.size(); i++) {
                ps.setObject(i + 1, parametrosFinales.get(i));
            }

            ResultSet rs = ps.executeQuery();

            boolean sel = false;

            while (rs.next()) {

                if (selsinerrores.isSelected()) {
                    if (rs.getString("Estado").contains("K")) {
                        sel = false;
                    } else {
                        sel = true;
                    }
                } else {
                    sel = seltodos.isSelected();
                }

                modelFactura.addRow(new Object[]{
                    rs.getString("Sec"),
                    sel, // checkbox
                    rs.getDate("Fecha"),
                    rs.getString("Numero"),
                    rs.getString("Nit"),
                    rs.getString("Cliente"),
                    rs.getString("Estado")
                });
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        }

    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        iFacFec = new com.toedter.calendar.JDateChooser();
        jLabel3 = new javax.swing.JLabel();
        fFacFec = new com.toedter.calendar.JDateChooser();
        jLabel4 = new javax.swing.JLabel();
        eTipEle = new javax.swing.JComboBox<>();
        jLabel5 = new javax.swing.JLabel();
        eFacNro = new javax.swing.JTextField();
        enviar = new javax.swing.JButton();
        seltodos = new javax.swing.JCheckBox();
        Auto = new javax.swing.JButton();
        detener = new javax.swing.JButton();
        selsinerrores = new javax.swing.JCheckBox();
        eError = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        Filtrar = new javax.swing.JButton();
        EvioAsincrono = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        TablaFactura = new javax.swing.JTable();
        configuracion = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        TXTresponse = new javax.swing.JTextArea();
        nroFac = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel1.setBackground(new java.awt.Color(255, 255, 255));

        jLabel1.setFont(new java.awt.Font("Dialog", 0, 24)); // NOI18N
        jLabel1.setText("Envio facturas");

        jPanel2.setBackground(new java.awt.Color(246, 250, 255));

        jLabel2.setText("Desde");

        iFacFec.setDateFormatString("yyyy-MM-dd");

        jLabel3.setText("Hasta");

        fFacFec.setDateFormatString("yyyy-MM-dd");

        jLabel4.setText("Numero");

        eTipEle.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "TODOS", "FAC", "NOT", "DSO", "DDS", "NTB" }));

        jLabel5.setText("Tipo");

        enviar.setText("Enviar");
        enviar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                enviarActionPerformed(evt);
            }
        });

        seltodos.setText("Sel. Todos");
        seltodos.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                seltodosActionPerformed(evt);
            }
        });

        Auto.setBackground(new java.awt.Color(255, 0, 0));
        Auto.setForeground(new java.awt.Color(255, 255, 255));
        Auto.setText("Auto");
        Auto.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                AutoActionPerformed(evt);
            }
        });

        detener.setText("Detener");
        detener.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                detenerActionPerformed(evt);
            }
        });

        selsinerrores.setText("Sel.SinErrores");
        selsinerrores.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selsinerroresActionPerformed(evt);
            }
        });

        jLabel6.setText("Error");

        Filtrar.setText("Filtrar");
        Filtrar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                FiltrarActionPerformed(evt);
            }
        });

        EvioAsincrono.setText("Envio.AS");
        EvioAsincrono.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                EvioAsincronoActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(iFacFec, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel3)
                    .addComponent(fFacFec, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel5)
                    .addComponent(eTipEle, javax.swing.GroupLayout.PREFERRED_SIZE, 89, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(eFacNro, javax.swing.GroupLayout.PREFERRED_SIZE, 129, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(eError, javax.swing.GroupLayout.PREFERRED_SIZE, 129, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel6))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(selsinerrores, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(seltodos, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(Filtrar, javax.swing.GroupLayout.PREFERRED_SIZE, 117, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(EvioAsincrono, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(enviar, javax.swing.GroupLayout.PREFERRED_SIZE, 117, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(detener, javax.swing.GroupLayout.PREFERRED_SIZE, 74, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(Auto, javax.swing.GroupLayout.DEFAULT_SIZE, 73, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap(14, Short.MAX_VALUE)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel5, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel3, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel2, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(seltodos)
                        .addComponent(Filtrar)
                        .addComponent(EvioAsincrono))
                    .addComponent(jLabel4, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel6, javax.swing.GroupLayout.Alignment.TRAILING))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(iFacFec, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(fFacFec, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(eTipEle, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(enviar)
                        .addComponent(Auto)
                        .addComponent(selsinerrores)
                        .addComponent(eError, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(eFacNro, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(detener)))
                .addContainerGap())
        );

        TablaFactura.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "FacSec", "Sel", "Fecha", "FacNro", "Nit", "Cliente", "Estado"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Object.class, java.lang.Boolean.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class
            };
            boolean[] canEdit = new boolean [] {
                true, true, false, true, false, true, true
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        TablaFactura.setShowGrid(true);
        jScrollPane1.setViewportView(TablaFactura);
        if (TablaFactura.getColumnModel().getColumnCount() > 0) {
            TablaFactura.getColumnModel().getColumn(0).setMinWidth(0);
            TablaFactura.getColumnModel().getColumn(0).setPreferredWidth(0);
            TablaFactura.getColumnModel().getColumn(0).setMaxWidth(0);
        }

        configuracion.setIcon(new javax.swing.ImageIcon(getClass().getResource("/config15.png"))); // NOI18N
        configuracion.setText("Configuracion");
        configuracion.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                configuracionMouseClicked(evt);
            }
        });

        TXTresponse.setBackground(new java.awt.Color(51, 51, 51));
        TXTresponse.setColumns(20);
        TXTresponse.setFont(new java.awt.Font("Monospaced", 0, 12)); // NOI18N
        TXTresponse.setForeground(new java.awt.Color(204, 255, 204));
        TXTresponse.setRows(5);
        jScrollPane2.setViewportView(TXTresponse);

        nroFac.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        nroFac.setText("Factura");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(20, 20, 20)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jScrollPane1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(8, 8, 8)
                        .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(6, 6, 6)))
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(configuracion)
                        .addGap(17, 17, 17))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 467, Short.MAX_VALUE)
                            .addComponent(nroFac))
                        .addGap(15, 15, 15))))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(21, 21, 21)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1)
                    .addComponent(configuracion))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 577, Short.MAX_VALUE))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(24, 24, 24)
                                .addComponent(jScrollPane2)))
                        .addGap(24, 24, 24))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(nroFac)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void configuracionMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_configuracionMouseClicked
        Config login = new Config();
        login.setLocationRelativeTo(null); // Centra la ventana
        login.setVisible(true);
    }//GEN-LAST:event_configuracionMouseClicked

    private void EvioAsincronoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_EvioAsincronoActionPerformed
enviarFacturasSeleccionadasAsincrono();

    }//GEN-LAST:event_EvioAsincronoActionPerformed

    private void enviarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_enviarActionPerformed

        enviarFacturasSeleccionadas();


    }//GEN-LAST:event_enviarActionPerformed
    public void auto() {

        detener.setEnabled(true);
        Auto.setForeground(Color.decode("#FFFFFF"));
        Auto.setBackground(Color.decode("#288F22"));
        if (detener.isEnabled()) {
            timer = new Timer(); // ✅ SIEMPRE nuevo
            tarea = new TimerTask() {
                @Override
                public void run() {
                    envioAuto();
                }
            };
            timer.scheduleAtFixedRate(tarea, 1000, 1000);
        }
    }

    private void seltodosActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_seltodosActionPerformed

    }//GEN-LAST:event_seltodosActionPerformed

    private void detenerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_detenerActionPerformed

        Auto.setBackground(Color.decode("#DE120D"));
        Auto.setEnabled(true);
        Auto.setForeground(Color.decode("#FFFFFF"));
        detener.setEnabled(false);

        if (timer != null) {
            timer.cancel(); // cancela el timer actual
            timer = null;
        }
        xdetener = true;
        bloquearUI(false);
        CargarFacturas();
    }//GEN-LAST:event_detenerActionPerformed

    private void AutoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_AutoActionPerformed

        xdetener = false;
        seltodos.setSelected(true);
        bloquearUI(true);
        auto();
    }//GEN-LAST:event_AutoActionPerformed

    private void selsinerroresActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selsinerroresActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_selsinerroresActionPerformed

    private void FiltrarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_FiltrarActionPerformed
        CargarFacturas();
    }//GEN-LAST:event_FiltrarActionPerformed

    public void envioAuto() {

        /*new Thread(new Runnable() {
            @Override
            public void run() {*/
        DefaultTableModel model = (DefaultTableModel) TablaFactura.getModel();

        for (int i = 0; i < model.getRowCount(); i++) {
            if (xdetener) {
                return;
            }

            Boolean seleccionado = (Boolean) model.getValueAt(i, 1); // columna checkbox

            if (Boolean.TRUE.equals(seleccionado)) {

                String numeroFactura = model.getValueAt(i, 0).toString();
                String eFacnro = model.getValueAt(i, 3).toString();

                try {
                    bloquearUI(true);
                    nroFac.setText("Enviando: " + eFacnro);

                    enviarFacturaDian(numeroFactura, eFacnro);
                } catch (Exception ex) {
                    TXTresponse.setText(
                            "Error enviando factura " + numeroFactura + "\n" + ex.getMessage()
                    );
                }
            }
        }

        bloquearUI(false);
        CargarFacturas();
        /*  }

        }).start();*/

    }

    private void enviarFacturasSeleccionadas() {

        new Thread(new Runnable() {
            @Override
            public void run() {
                DefaultTableModel model = (DefaultTableModel) TablaFactura.getModel();

                for (int i = 0; i < model.getRowCount(); i++) {

                    Boolean seleccionado = (Boolean) model.getValueAt(i, 1); // columna checkbox

                    if (Boolean.TRUE.equals(seleccionado)) {

                        String numeroFactura = model.getValueAt(i, 0).toString();
                        String eFacnro = model.getValueAt(i, 3).toString();

                        try {
                            bloquearUI(true);
                            nroFac.setText("Enviando: " + eFacnro);

                            enviarFacturaDian(numeroFactura, eFacnro);
                        } catch (Exception ex) {
                            TXTresponse.setText(
                                    "Error enviando factura " + numeroFactura + "\n" + ex.getMessage()
                            );
                        }
                    }
                }

                bloquearUI(false);
                CargarFacturas();
            }

        }).start();
    }

    private void enviarFacturasSeleccionadasAsincrono() {
          DefaultTableModel model = (DefaultTableModel) TablaFactura.getModel();
        int HILOS = 1000; // ajusta entre 5 y 10
ExecutorService executor = Executors.newFixedThreadPool(HILOS);

AtomicInteger enviadas = new AtomicInteger(0);
int total = model.getRowCount();

bloquearUI(true);

for (int i = 0; i < model.getRowCount(); i++) {
    Boolean seleccionado = (Boolean) model.getValueAt(i, 1);

    if (Boolean.TRUE.equals(seleccionado)) {
        String numeroFactura = model.getValueAt(i, 0).toString();
        String eFacnro = model.getValueAt(i, 3).toString();

        executor.submit(() -> {
            try {
                enviarFacturaDian(numeroFactura, eFacnro);

                int count = enviadas.incrementAndGet();
                SwingUtilities.invokeLater(() ->
                    nroFac.setText("Enviadas "+ eFacnro+" "+ count + " / " + total)
                );
          
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() ->
                    TXTresponse.append(
                        "❌ Error factura " + numeroFactura + ": " + ex.getMessage() + "\n"
                    )
                );
            }
        });
    }
}

executor.shutdown();

// cuando termine todo
new Thread(() -> {
    try {
        executor.awaitTermination(1, TimeUnit.HOURS);
        SwingUtilities.invokeLater(() -> {
            bloquearUI(false);
            nroFac.setText("Proceso terminado");
             CargarFacturas(); 
        });
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
}).start();
        
        
      /*  DefaultTableModel model = (DefaultTableModel) TablaFactura.getModel();

        for (int i = 0; i < model.getRowCount(); i++) {
            Boolean seleccionado = (Boolean) model.getValueAt(i, 1);

            if (Boolean.TRUE.equals(seleccionado)) {
                String numeroFactura = model.getValueAt(i, 0).toString();
                String eFacnro = model.getValueAt(i, 3).toString();

                SwingWorker<Void, Void> worker = new SwingWorker<>() {

                    @Override
                    protected Void doInBackground() throws Exception {
                        enviarFacturaDian(numeroFactura, eFacnro);
                        return null;
                    }

                    @Override
                    protected void done() {
                        nroFac.setText("Enviado: " + eFacnro);
                    }
                };

                worker.execute();
            }
        }
*/
    }

    private void bloquearUI(boolean estado) {
        TablaFactura.setEnabled(!estado);
        EvioAsincrono.setEnabled(!estado);
        enviar.setEnabled(!estado);
    }

    private void enviarFacturaDian(String numeroFactura, String eFacnro) throws Exception {

        String endpoint = conws.getURLServicio();//"http://169.46.48.131:8087/MediMantisDiscol/rest/wsEnvioFacturaDianV2";
        URL url = new URL(endpoint);

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        // JSON Body
        String jsonBody = "{ \"NumeroFactura\": \"" + numeroFactura + "\" }";

        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            os.flush();
        }

        int responseCode = conn.getResponseCode();

        InputStream is = (responseCode >= 200 && responseCode < 300)
                ? conn.getInputStream()
                : conn.getErrorStream();

        String response;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            response = br.lines().collect(Collectors.joining());
        }

        conn.disconnect();

        String bonito = response
                .replace("{", "{\n")
                .replace("}", "\n}")
                .replace(",", ",\n");

        TXTresponse.setText(bonito);
        nroFac.setText(eFacnro);
        //procesarRespuesta(response, numeroFactura);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(Enviador.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Enviador.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Enviador.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Enviador.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Enviador().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton Auto;
    private javax.swing.JButton EvioAsincrono;
    private javax.swing.JButton Filtrar;
    private javax.swing.JTextArea TXTresponse;
    private javax.swing.JTable TablaFactura;
    private javax.swing.JLabel configuracion;
    private javax.swing.JButton detener;
    private javax.swing.JTextField eError;
    private javax.swing.JTextField eFacNro;
    private javax.swing.JComboBox<String> eTipEle;
    private javax.swing.JButton enviar;
    private com.toedter.calendar.JDateChooser fFacFec;
    private com.toedter.calendar.JDateChooser iFacFec;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JLabel nroFac;
    private javax.swing.JCheckBox selsinerrores;
    private javax.swing.JCheckBox seltodos;
    // End of variables declaration//GEN-END:variables
}
