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
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
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
    ScheduledExecutorService scheduler;
    ScheduledExecutorService schedulerDialog;
    ScheduledExecutorService schedulerDialogenvcor;
    private AtomicBoolean ejecutando = new AtomicBoolean(false);
    JDialog dialogProceso;
    JDialog dialogProcesocor;
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
        // CargarFacturas();
        if (seltodos.isSelected()) {
            auto();
        }

        iniciarDialogTimer();
        iniciarDialogcorTimer();

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

        String sql
                = "SELECT DISTINCT "
                + "    f.FacSec AS Sec, "
                + "    f.FacFec AS Fecha, "
                + "    f.FacNro AS Numero, "
                + "    n.NitIde AS Nit, "
                + "    cli.CliNom AS Cliente, "
                + "    f.FacEleStatus AS Estado,"
                + " (select sum(Facval) from facturapago p where p.facsec = f.facsec) as pago, "
                + " (select sum(Karvaltotmendes) from facturakardex k where k.facsec = f.facsec) as Total "
                + " FROM factura f  WITH (NOLOCK) "
                + "LEFT JOIN tipos t ON f.factipcod = t.tipcod "
                + "LEFT JOIN facturaenvio e ON e.facsec = f.facsec  "
                + "LEFT JOIN ModalidadContratoMed mod on mod.ModConMedSec = FacModConMedSec "
                + "LEFT JOIN clientes cli   "
                + "       ON f.FacNitSec = cli.NitSec "
                + "      AND f.FacCliSec = cli.CliSec "
                + "LEFT JOIN Nit n ON f.FacNitSec = n.NitSec   "
                //+ "WHERE f.FacEst = 'A' and f.FacEleEnvCor = 'W'  "
                + "WHERE f.FacEst = 'A'   "
                + " ";

        
        if(copago.isSelected()){
            sql
                    += " AND ModConMedTipEst = 'CO' ";  
        }
        
        if (sinArticulo.isSelected()) {
            sql
                    += "   AND NOT EXISTS (\n"
                    + "        SELECT 1\n"
                    + "        FROM facturakardex fk\n"
                    + "        WHERE fk.FacSec = f.FacSec\n"
                    + "  ) ";
        }

        if (ErrorCorreo.isSelected()) {
            sql
                    += " AND Nitide like '%" + NitIde.getText() + "%'  AND "
                    + "(CliCorEle  LIKE '%_@_%._%' and  CliCorEle NOT LIKE '% %' ) and  f.FacEleStatus = 'S' and  "
                    + " (f.FacEleEnvCor = 'K' or f.FacEleEnvCor = 'X' or f.FacEleEnvCor = '' or f.FacEleEnvCor is null) ";
            //  + " (f.FacEleEnvCor = 'W') ";
        } else {
            sql
                    += " AND Nitide like '%" + NitIde.getText() + "%' AND  ( "
                    + "        (e.facenvresp LIKE '%" + eError.getText() + "%') "
                    + "     OR (e.facenvresp IS NULL) "
                    + ") AND (f.FacEleStatus IS NULL OR f.FacEleStatus = '' OR f.FacEleStatus = 'K'  ) ";
        }
        sql += whereFacturaFinal + " "
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
            int contador = 0;
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

                if (sinpago.isSelected()) {
                    if (rs.getDouble("pago") < 1) {
                        contador += 1;
                        modelFactura.addRow(new Object[]{
                            rs.getString("Sec"),
                            sel, // checkbox
                            rs.getDate("Fecha"),
                            rs.getString("Numero"),
                            rs.getString("Nit"),
                            rs.getString("Cliente"),
                            rs.getString("Estado"),
                            rs.getDouble("pago"),
                            rs.getDouble("Total")
                        });
                    }

                } else {
                    if (conpago.isSelected()) {
                        if (rs.getDouble("pago") > 0) {
                            contador += 1;
                            modelFactura.addRow(new Object[]{
                                rs.getString("Sec"),
                                sel, // checkbox
                                rs.getDate("Fecha"),
                                rs.getString("Numero"),
                                rs.getString("Nit"),
                                rs.getString("Cliente"),
                                rs.getString("Estado"),
                                rs.getDouble("pago"),
                                rs.getDouble("Total")
                            });
                        }
                    } else {
                        contador += 1;
                        modelFactura.addRow(new Object[]{
                            rs.getString("Sec"),
                            sel, // checkbox
                            rs.getDate("Fecha"),
                            rs.getString("Numero"),
                            rs.getString("Nit"),
                            rs.getString("Cliente"),
                            rs.getString("Estado"),
                            rs.getDouble("pago"),
                            rs.getDouble("Total")
                        });
                    }

                }

            }
            titulo.setText(String.valueOf(contador));
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
        titulo = new javax.swing.JLabel();
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
        selsinerrores = new javax.swing.JCheckBox();
        eError = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        Filtrar = new javax.swing.JButton();
        EvioAsincrono = new javax.swing.JButton();
        ErrorCorreo = new javax.swing.JCheckBox();
        sinpago = new javax.swing.JCheckBox();
        conpago = new javax.swing.JCheckBox();
        NitIde = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        sinArticulo = new javax.swing.JCheckBox();
        EnvioCorreo = new javax.swing.JTextField();
        autoboton = new javax.swing.JToggleButton();
        copago = new javax.swing.JCheckBox();
        jScrollPane1 = new javax.swing.JScrollPane();
        TablaFactura = new javax.swing.JTable();
        configuracion = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        TXTresponse = new javax.swing.JTextArea();
        nroFac = new javax.swing.JLabel();
        configuracion1 = new javax.swing.JLabel();
        enviocorreo = new javax.swing.JLabel();
        barraprogreso = new javax.swing.JProgressBar();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel1.setBackground(new java.awt.Color(255, 255, 255));

        titulo.setFont(new java.awt.Font("Dialog", 0, 24)); // NOI18N
        titulo.setText("Envio facturas");

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

        ErrorCorreo.setText("etrroc");
        ErrorCorreo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ErrorCorreoActionPerformed(evt);
            }
        });

        sinpago.setText("sinformapago");
        sinpago.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sinpagoActionPerformed(evt);
            }
        });

        conpago.setText("conformapago");
        conpago.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                conpagoActionPerformed(evt);
            }
        });

        jLabel7.setText("Cliente");

        sinArticulo.setText("SinArticulo");
        sinArticulo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sinArticuloActionPerformed(evt);
            }
        });

        autoboton.setText("Auto");
        autoboton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                autobotonActionPerformed(evt);
            }
        });

        copago.setText("copago");
        copago.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                copagoActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
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
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(eFacNro, javax.swing.GroupLayout.PREFERRED_SIZE, 129, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(eError, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(jLabel4))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel7)
                            .addComponent(NitIde, javax.swing.GroupLayout.PREFERRED_SIZE, 129, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(488, 488, 488)
                        .addComponent(jLabel6)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(enviar, javax.swing.GroupLayout.PREFERRED_SIZE, 117, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(autoboton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(Filtrar, javax.swing.GroupLayout.PREFERRED_SIZE, 117, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(EvioAsincrono, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(EnvioCorreo, javax.swing.GroupLayout.PREFERRED_SIZE, 237, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 51, Short.MAX_VALUE)
                .addComponent(seltodos, javax.swing.GroupLayout.PREFERRED_SIZE, 108, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(ErrorCorreo, javax.swing.GroupLayout.PREFERRED_SIZE, 81, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(selsinerrores)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(conpago)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(sinpago)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(sinArticulo)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(copago)
                .addGap(7, 7, 7))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(seltodos)
                    .addComponent(ErrorCorreo)
                    .addComponent(selsinerrores)
                    .addComponent(conpago)
                    .addComponent(sinpago)
                    .addComponent(sinArticulo)
                    .addComponent(EnvioCorreo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(copago))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel5, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel3, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel2, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(Filtrar)
                                .addComponent(EvioAsincrono))
                            .addComponent(jLabel4, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel6, javax.swing.GroupLayout.Alignment.TRAILING))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel7)
                        .addGap(12, 12, 12)))
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(iFacFec, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(fFacFec, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(eTipEle, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(enviar)
                        .addComponent(eError, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(eFacNro, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(NitIde, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(autoboton)))
                .addContainerGap())
        );

        TablaFactura.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "FacSec", "Sel", "Fecha", "FacNro", "Nit", "Cliente", "Estado", "pago", "Total"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Object.class, java.lang.Boolean.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class
            };
            boolean[] canEdit = new boolean [] {
                true, true, false, true, false, true, true, true, true
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

        configuracion1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons8-subir-a-la-nube-20.png"))); // NOI18N
        configuracion1.setText("Tracking correos");
        configuracion1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                configuracion1MouseClicked(evt);
            }
        });

        enviocorreo.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons8-subir-a-la-nube-20.png"))); // NOI18N
        enviocorreo.setText("Envio correos");
        enviocorreo.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                enviocorreoMouseClicked(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel1Layout.createSequentialGroup()
                        .addGap(8, 8, 8)
                        .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(6, 6, 6))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jScrollPane1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel1Layout.createSequentialGroup()
                                .addGap(20, 20, 20)
                                .addComponent(titulo)
                                .addGap(32, 32, 32)
                                .addComponent(configuracion)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(configuracion1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(enviocorreo))
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel1Layout.createSequentialGroup()
                                .addGap(14, 14, 14)
                                .addComponent(barraprogreso, javax.swing.GroupLayout.PREFERRED_SIZE, 996, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 484, Short.MAX_VALUE)
                    .addComponent(nroFac))
                .addGap(15, 15, 15))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(21, 21, 21)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(titulo)
                    .addComponent(configuracion)
                    .addComponent(configuracion1)
                    .addComponent(enviocorreo))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(24, 24, 24)
                        .addComponent(jScrollPane2)
                        .addGap(24, 24, 24))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(nroFac)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(barraprogreso, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 586, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap())))
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

        /*   detener.setEnabled(true);
        Auto.setForeground(Color.decode("#FFFFFF"));
        Auto.setBackground(Color.decode("#288F22"));

        if (detener.isEnabled()) {
         //   timer = new Timer(); // ✅ SIEMPRE nuevo
            tarea = new TimerTask() {
                @Override
                public void run() {
                   
                }
            };
          //  timer.scheduleAtFixedRate(tarea, 1000, 1000);
        }
9*/
    }
public void iniciarScheduler() {

    if (scheduler != null && !scheduler.isShutdown()) {
        System.out.println("Scheduler ya está corriendo");
        return;
    }

    scheduler = Executors.newSingleThreadScheduledExecutor();

    scheduler.scheduleAtFixedRate(() -> {

        if (!ejecutando.compareAndSet(false, true)) {
            return;
        }


        try {
            LocalDate ayer = LocalDate.now().minusDays(1);
            Date fechaAyer = Date.from(
                    ayer.atStartOfDay(ZoneId.systemDefault()).toInstant()
            );

            Date hoy = new Date();
            iFacFec.setDate(fechaAyer);
            fFacFec.setDate(hoy);
   
            envioAuto();

        } catch (Exception ex) {
            System.out.println("Error: " + ex.getMessage());
        } finally {
            
        ejecutando.set(false); 
        }

    }, 0, 5, TimeUnit.MINUTES);
}







    public void iniciarSchedulerant() {

     /*   scheduler = Executors.newSingleThreadScheduledExecutor();

        scheduler.scheduleAtFixedRate(() -> {

            if (ejecutando) {
                return;
            }

            ejecutando = true;

            try {
                // Fecha actual
                Date hoy = new Date();
                // Asignar a los datepickers
                iFacFec.setDate(hoy);
                fFacFec.setDate(hoy);
                envioAuto();
            } catch (Exception ex) {
                System.out.println("Error: " + ex.getMessage());
            } finally {

                ejecutando = false;

            }

        }, 0, 5, TimeUnit.MINUTES); // inicia inmediatamente y luego cada 10 minutos*/
    }

    public void detenerScheduler() {

        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }

    }


    private void seltodosActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_seltodosActionPerformed

    }//GEN-LAST:event_seltodosActionPerformed

    private void selsinerroresActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selsinerroresActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_selsinerroresActionPerformed

    private void FiltrarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_FiltrarActionPerformed
        CargarFacturas();
    }//GEN-LAST:event_FiltrarActionPerformed

    private void ErrorCorreoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ErrorCorreoActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_ErrorCorreoActionPerformed

    private void sinpagoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sinpagoActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_sinpagoActionPerformed

    private void conpagoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_conpagoActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_conpagoActionPerformed

    private void sinArticuloActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sinArticuloActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_sinArticuloActionPerformed

    private void configuracion1MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_configuracion1MouseClicked

        mostrarDialogo("N");

    }//GEN-LAST:event_configuracion1MouseClicked

    private void autobotonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_autobotonActionPerformed
        if (autoboton.isSelected()) {

            titulo.setText("Auto Envío ACTIVO");
            iniciarScheduler();
            autoboton.setBackground(Color.GREEN);
            titulo.setText("Proceso automático iniciado");

        } else {

            titulo.setText("Auto Envío DETENIDO");

            detenerScheduler();
            autoboton.setBackground(Color.RED);
            titulo.setText("Proceso automático detenido");

        }

    }//GEN-LAST:event_autobotonActionPerformed

    private void enviocorreoMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_enviocorreoMouseClicked
        mostrarDialogoCor("N");
        // TODO add your handling code here:
    }//GEN-LAST:event_enviocorreoMouseClicked

    private void copagoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_copagoActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_copagoActionPerformed

    public void iniciarDialogTimer() {

        schedulerDialog = Executors.newSingleThreadScheduledExecutor();

        schedulerDialog.scheduleWithFixedDelay(() -> {

            mostrarDialogo("S");

        }, 1, 10, TimeUnit.MINUTES);

    }

    public void iniciarDialogcorTimer() {

        schedulerDialogenvcor = Executors.newSingleThreadScheduledExecutor();

        schedulerDialogenvcor.scheduleWithFixedDelay(() -> {

            mostrarDialogoCor("S");

        }, 1, 10, TimeUnit.MINUTES);

    }

    public void detenerDialogcorTimer() {

        if (schedulerDialogenvcor != null && !schedulerDialogenvcor.isShutdown()) {
            schedulerDialogenvcor.shutdown();
        }

    }

    public void detenerDialogTimer() {

        if (schedulerDialog != null && !schedulerDialog.isShutdown()) {
            schedulerDialog.shutdown();
        }

    }

    public void mostrarDialogoCor(String auto) {

        if (dialogProcesocor == null || !dialogProcesocor.isShowing()) {

            SwingUtilities.invokeLater(() -> {
                dialogProcesocor = new EnvioCorreos(this, false, auto); // tu dialog
                dialogProcesocor.setLocationRelativeTo(null); // Centra la ventana
                dialogProcesocor.setVisible(true);

            });

        }

    }

    public void mostrarDialogo(String auto) {

        if (dialogProceso == null || !dialogProceso.isShowing()) {

            SwingUtilities.invokeLater(() -> {
                dialogProceso = new ActualizarEstadoCorreo(this, false, auto); // tu dialog
                dialogProceso.setLocationRelativeTo(null); // Centra la ventana
                dialogProceso.setVisible(true);

            });

        }

    }



    
 
    
    
      
      
    
    public void envioAutanto() {
        barraprogreso.setIndeterminate(true);
        //barraprogreso.setString("Cargando facturas...");
        bloquearUI(true);
        seltodos.setSelected(true);
        CargarFacturas();
        DefaultTableModel model = (DefaultTableModel) TablaFactura.getModel();
        List<String[]> facturas = new ArrayList<>();
        detenerScheduler();
        // 2️⃣ Copiar datos de la tabla
        for (int i = 0; i < model.getRowCount(); i++) {

            Boolean seleccionado = (Boolean) model.getValueAt(i, 1);

            if (Boolean.TRUE.equals(seleccionado)) {

                String numeroFactura = model.getValueAt(i, 0).toString();
                String eFacnro = model.getValueAt(i, 3).toString();

                facturas.add(new String[]{numeroFactura, eFacnro});
            }
        }

        // 3️⃣ Worker
        SwingWorker<Void, Integer> worker = new SwingWorker<Void, Integer>() {

            @Override
            protected Void doInBackground() {
                SwingUtilities.invokeLater(() -> {
                    barraprogreso.setIndeterminate(false);
                    barraprogreso.setMaximum(facturas.size());
                    barraprogreso.setValue(0);
                    barraprogreso.setStringPainted(true);
                });

                int enviados = 0;

                for (String[] factura : facturas) {

                    if (xdetener) {
                        break;
                    }

                    String numeroFactura = factura[0];
                    String eFacnro = factura[1];

                    try {

                        SwingUtilities.invokeLater(()
                                -> nroFac.setText("Enviando: " + eFacnro)
                        );

                        enviarFacturaDian(numeroFactura, eFacnro);

                        enviados++;

                        publish(enviados);

                    } catch (Exception ex) {

                        SwingUtilities.invokeLater(()
                                -> TXTresponse.setText(
                                        "Error enviando factura "
                                        + numeroFactura + "\n" + ex.getMessage()
                                )
                        );
                    }
                }

                return null;
            }

            @Override
            protected void process(List<Integer> chunks) {

                int value = chunks.get(chunks.size() - 1);

                barraprogreso.setValue(value);

                nroFac.setText(
                        "Facturas enviadas: "
                        + value + " / " + barraprogreso.getMaximum()
                );
            }

            @Override
            protected void done() {

                bloquearUI(false);

                iniciarScheduler();

                nroFac.setText("Envío finalizado");
            }
        };

        worker.execute();
    }
public void envioAuto() {
    barraprogreso.setIndeterminate(true);
    bloquearUI(true);
    seltodos.setSelected(true);
    CargarFacturas();

    DefaultTableModel model = (DefaultTableModel) TablaFactura.getModel();
    List<String[]> facturas = new ArrayList<>();
    detenerScheduler();

    for (int i = 0; i < model.getRowCount(); i++) {
        Boolean seleccionado = (Boolean) model.getValueAt(i, 1);
        if (Boolean.TRUE.equals(seleccionado)) {
            String numeroFactura = model.getValueAt(i, 0).toString();
            String eFacnro = model.getValueAt(i, 3).toString();
            facturas.add(new String[]{numeroFactura, eFacnro});
        }
    }

    SwingWorker<Void, Integer> worker = new SwingWorker<Void, Integer>() {

        @Override
        protected Void doInBackground() {

            SwingUtilities.invokeLater(() -> {
                barraprogreso.setIndeterminate(false);
                barraprogreso.setMaximum(facturas.size());
                barraprogreso.setValue(0);
                barraprogreso.setStringPainted(true);
            });

            java.util.concurrent.ExecutorService pool =
                    java.util.concurrent.Executors.newFixedThreadPool(5); // ajusta si quieres

            java.util.concurrent.atomic.AtomicInteger enviados = new java.util.concurrent.atomic.AtomicInteger(0);

            for (String[] factura : facturas) {

                if (xdetener) break;

                pool.submit(() -> {
                    String numeroFactura = factura[0];
                    String eFacnro = factura[1];

                    try {
                        SwingUtilities.invokeLater(() ->
                                nroFac.setText("Enviando: " + eFacnro)
                        );

                        enviarFacturaDian(numeroFactura, eFacnro);

                        int total = enviados.incrementAndGet();
                        publish(total);

                    } catch (Exception ex) {
                        SwingUtilities.invokeLater(() ->
                                TXTresponse.setText(
                                        "Error enviando factura "
                                                + numeroFactura + "\n" + ex.getMessage()
                                )
                        );
                    }
                });
            }

            pool.shutdown();
            while (!pool.isTerminated()) {
                try { Thread.sleep(200); } catch (InterruptedException e) {}
            }

            return null;
        }

        @Override
        protected void process(List<Integer> chunks) {
            int value = chunks.get(chunks.size() - 1);
            barraprogreso.setValue(value);
            nroFac.setText(
                    "Facturas enviadas: "
                            + value + " / " + barraprogreso.getMaximum()
            );
        }

        @Override
        protected void done() {
            bloquearUI(false);
            iniciarScheduler();
            nroFac.setText("Envío finalizado");
        }
    };

    worker.execute();
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
        int HILOS = 100; // ajusta entre 5 y 10
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
                        SwingUtilities.invokeLater(()
                                -> nroFac.setText("Enviadas " + eFacnro + " " + count + " / " + total)
                        );

                    } catch (Exception ex) {
                        SwingUtilities.invokeLater(()
                                -> TXTresponse.append(
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

        String endpoint = "http://169.46.48.131:8087/MantisWebServices/rest/wsEnvioFacturaDianV2";
        URL url = new URL(endpoint);

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        // JSON Body
        String jsonBody = "{ \"nFacCliCorEle\": \"" + EnvioCorreo.getText() + "\", \"NumeroFactura\": \"" + numeroFactura + "\" }";

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
    private javax.swing.JTextField EnvioCorreo;
    private javax.swing.JCheckBox ErrorCorreo;
    private javax.swing.JButton EvioAsincrono;
    private javax.swing.JButton Filtrar;
    private javax.swing.JTextField NitIde;
    private javax.swing.JTextArea TXTresponse;
    private javax.swing.JTable TablaFactura;
    private javax.swing.JToggleButton autoboton;
    private javax.swing.JProgressBar barraprogreso;
    private javax.swing.JLabel configuracion;
    private javax.swing.JLabel configuracion1;
    private javax.swing.JCheckBox conpago;
    private javax.swing.JCheckBox copago;
    private javax.swing.JTextField eError;
    private javax.swing.JTextField eFacNro;
    private javax.swing.JComboBox<String> eTipEle;
    private javax.swing.JButton enviar;
    private javax.swing.JLabel enviocorreo;
    private com.toedter.calendar.JDateChooser fFacFec;
    private com.toedter.calendar.JDateChooser iFacFec;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JLabel nroFac;
    private javax.swing.JCheckBox selsinerrores;
    private javax.swing.JCheckBox seltodos;
    private javax.swing.JCheckBox sinArticulo;
    private javax.swing.JCheckBox sinpago;
    private javax.swing.JLabel titulo;
    // End of variables declaration//GEN-END:variables
}
