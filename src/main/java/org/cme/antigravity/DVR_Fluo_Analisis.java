/*
 * DVR Fluo Analisis
 *
 * PROCEDENCIA DE ESTE ARCHIVO: el codigo fuente original se perdio, y se
 * recupero descompilando el propio .jar del complemento. Desde entonces se
 * mantiene a mano: las correcciones y los comentarios que siguen se
 * escribieron sobre ese codigo recuperado, no salieron del descompilador.
 *
 * Por eso algunos nombres de variable son los que eligio la herramienta
 * (folderFile, opt, dup) en lugar de los originales. Se dejan como estan
 * para no tocar codigo que funciona y esta verificado contra el binario
 * que se distribuye.
 */
package org.cme.antigravity;

import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.Roi;
import ij.io.DirectoryChooser;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import ij.plugin.filter.Analyzer;
import ij.text.TextPanel;
import ij.text.TextWindow;
import java.awt.Button;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.LayoutManager;
import java.awt.Panel;
import java.awt.Window;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class DVR_Fluo_Analisis
implements PlugIn {
    // Las guias se abren con la ruta "blob" del repositorio, no con "raw":
    // con "raw" el navegador fuerza la descarga del PDF, con "blob" GitHub lo
    // muestra en su visor y el usuario lo lee ahi mismo.
    //
    // Estos enlaces viven dentro del binario, de modo que el complemento ya
    // instalado depende de que la ruta siga existiendo. Si alguna vez se mueve
    // el repositorio, hay que cambiarlos aca y publicar una version nueva: no
    // alcanza con mover los archivos.
    private static final String URL_GUIA = "https://github.com/ezegrondona/DVR-Fluo-Analisis/blob/main/docs/Gu%C3%ADa%20de%20Uso%20-%20DVR%20Fluo%20An%C3%A1lisis.pdf";
    private static final String URL_CALIBRACION = "https://github.com/ezegrondona/DVR-Fluo-Analisis/blob/main/docs/Gu%C3%ADa%20de%20Calibraci%C3%B3n%20FIJI-ImajeJ.pdf";
    private boolean ir_a_calibrar = false;

    // --- Estetica de los dialogos ------------------------------------------
    // Estas constantes y los tres metodos de interfaz que siguen
    // (estilizarDialogo, mostrarInfoPlugin y abrirLink) vivian en una clase
    // aparte, pensada para compartirse entre complementos. Ahora cada
    // complemento lleva las suyas y no depende de nadie.
    //
    // El motivo es concreto: dos .jar instalados en el mismo Fiji comparten
    // el classpath. Si los dos traen una clase con el mismo nombre y el mismo
    // paquete, la maquina virtual carga una sola -la primera que encuentra- y
    // el otro complemento corre con codigo ajeno, sin aviso y sin error. Con
    // todo adentro de un unico archivo por complemento eso no puede pasar.
    private static final Font FONT_SECCION = new Font("SansSerif", Font.BOLD, 13);
    private static final Font FONT_MENSAJE = new Font("SansSerif", Font.PLAIN, 12);
    private static final Font FONT_BOTON = new Font("SansSerif", Font.PLAIN, 10);
    private static final Font FONT_BOTON_BOLD = new Font("SansSerif", Font.BOLD, 10);
    private static final Color COLOR_CABECERA = new Color(25, 136, 255);
    private static final Color COLOR_TEXTO_SECCION = new Color(255, 255, 255);
    private static final Color COLOR_BOTON_INFO = new Color(0, 151, 161);
    private static final Color COLOR_BOTON_GRIS = new Color(157, 167, 159);
    private static final Color COLOR_BOTON_TEXTO = new Color(255, 255, 255);
    private static final Color COLOR_FONDO_GRIS = new Color(240, 245, 240);
    private static final Color COLOR_BLANCO = Color.WHITE;

    public void run(String arg) {
        block11: {
            while (true) {
                Opciones opt;
                if ((opt = this.mostrarVentanaConfiguracion()) == null) {
                    IJ.showMessage("Proceso finalizado", "PROCESO DETENIDO");
                    break block11;
                }
                if (this.ir_a_calibrar) {
                    GenericDialog gd = new GenericDialog("Calibrar imagen");
                    gd.addMessage("1. Abra una imagen y calibre desde SET SCALE.\n2. Cierre la imagen sin guardar (DON'T SAVE).\n3. Presione OK para continuar.");
                    estilizarDialogo(gd, false);
                    gd.setModal(false);
                    gd.showDialog();
                    while (!gd.wasOKed() && !gd.wasCanceled()) {
                        IJ.wait(100);
                    }
                    if (!gd.wasCanceled()) continue;
                    IJ.showMessage("Proceso finalizado", "PROCESO DETENIDO");
                    break block11;
                }
                DirectoryChooser dc = new DirectoryChooser("Selecciona la carpeta con las fotos a analizar");
                String baseDir = dc.getDirectory();
                if (baseDir == null) {
                    IJ.showMessage("Proceso finalizado", "PROCESO DETENIDO");
                    break block11;
                }
                File folderFile = new File(baseDir);
                this.renombrarArchivos(folderFile);
                LinkedHashMap<String, RowData> dataMap = new LinkedHashMap<String, RowData>();
                IJ.run("Set Measurements...", "area mean display redirect=None decimal=3");
                // CORRECCIÓN: limpiar tablas de análisis previos para evitar acumulación entre carpetas
                ResultsTable rt0 = ResultsTable.getResultsTable();
                if (rt0 != null) rt0.reset();
                DVR_Fluo_Analisis.cerrarVentanaTexto("Summary");
                int dapiCount = 0;
                int rojoCount = 0;
                int verdeCount = 0;
                if (opt.dapi) {
                    dapiCount = this.procesarDapi(folderFile, dataMap);
                }
                if (opt.verde) {
                    verdeCount = this.procesarCanal(folderFile, dataMap, "af488", opt.tipoArea, "Green");
                }
                if (opt.rojo) {
                    rojoCount = this.procesarCanal(folderFile, dataMap, "af594", opt.tipoArea, "Red");
                }
                this.guardarExcel(folderFile, dataMap, opt);
                StringBuilder msg = new StringBuilder("Proceso Finalizado en:\n" + baseDir + "\n");
                if (opt.dapi) {
                    msg.append("DAPI: ").append(dapiCount).append(" procesadas\n");
                }
                if (opt.verde) {
                    msg.append("AF488: ").append(verdeCount).append(" procesadas\n");
                }
                if (opt.rojo) {
                    msg.append("AF594: ").append(rojoCount).append(" procesadas\n");
                }
                IJ.showMessage("Exito", msg.toString());

                // Cerrar ventanas de resultados y resumen al finalizar
                for (String winTitle : new String[]{"Summary", "Results"}) {
                    DVR_Fluo_Analisis.cerrarVentanaTexto(winTitle);
                }
                GenericDialog gdFinal = new GenericDialog("Analizar otra carpeta");
                gdFinal.addMessage("Presiona OK para seleccionar otra carpeta o Cancelar para salir.");
                estilizarDialogo(gdFinal, false);
                gdFinal.showDialog();
                if (gdFinal.wasCanceled()) break;
            }
            // Salida normal: el usuario cancelo "Analizar otra carpeta" porque termino de
            // trabajar. No se muestra "PROCESO DETENIDO" aca a proposito: ese cartel es
            // para interrupciones, y todas ellas salen por "break block11", que saltea
            // este punto. Llegar hasta aca significa que el trabajo se completo bien.
        }
    }

    /**
     * Cierra una ventana de texto de ImageJ ("Results", "Summary") como corresponde.
     *
     * IMPORTANTE: hay que usar TextWindow.close(), NO dispose(). close() es lo unico
     * que llama a WindowManager.removeWindow() y, en el caso de "Results", a
     * IJ.setTextPanel(null). Con dispose() la ventana destruida sigue registrada:
     *
     *  - "Results": IJ conserva la referencia estatica al TextPanel muerto, asi que
     *    IJ.getTextPanel() nunca vuelve a crear la ventana y ninguna medicion
     *    posterior se muestra (ni la del plugin, ni un Measure manual) hasta
     *    reiniciar Fiji.
     *  - "Summary": tanto este plugin (WindowManager.getWindow) como el propio
     *    ParticleAnalyzer (WindowManager.getFrame) la encuentran por titulo y
     *    reutilizan su ResultsTable, leyendo datos de una ventana muerta.
     *
     * close(false) no abre ningun dialogo: para "Results" el parametro saltea
     * Analyzer.resetCounter(), y para "Summary" saveContents() solo pregunta si el
     * titulo contiene "Results".
     */
    private static void cerrarVentanaTexto(String titulo) {
        Window win = WindowManager.getWindow(titulo);
        if (win instanceof TextWindow) {
            if ("Results".equals(titulo)) {
                // Evita que quede un "Save N measurements?" pendiente para mas adelante.
                Analyzer.setUnsavedMeasurements(false);
            }
            ((TextWindow)win).close(false);
        } else if (win != null) {
            win.setVisible(false);
            win.dispose();
        }
    }

    private Opciones mostrarVentanaConfiguracion() {
        this.ir_a_calibrar = false;
        GenericDialog gd = new GenericDialog("Analizar INTENSIDAD de Fluorescencia");
        gd.addMessage("INFORMACIÓN");
        Panel panelInfo = new Panel(new GridLayout(1, 2, 5, 0));
        Button btnAcerca = new Button("ACERCA DEL PLUGIN");
        Button btnGuia = new Button("GUÍA DE USO");
        btnAcerca.addActionListener(e -> mostrarInfoPlugin());
        btnGuia.addActionListener(e -> abrirLink(URL_GUIA));
        panelInfo.add(btnAcerca);
        panelInfo.add(btnGuia);
        gd.addPanel(panelInfo);

        gd.addMessage("CALIBRACIÓN");
        Panel panelCalib = new Panel(new GridLayout(1, 2, 5, 0));
        Button btnGuiaCal = new Button("GUÍA DE CALIBRACIÓN");
        Button btnCalibrar = new Button("CALIBRAR");
        btnGuiaCal.addActionListener(e -> abrirLink(URL_CALIBRACION));
        btnCalibrar.addActionListener(e -> {
            this.ir_a_calibrar = true;
            try {
                Container parent;
                for (parent = ((Component)e.getSource()).getParent(); parent != null && !(parent instanceof GenericDialog); parent = parent.getParent()) {
                }
                if (parent != null) {
                    ((GenericDialog)parent).dispose();
                }
            }
            catch (Exception exception) {
                // empty catch block
            }
        });
        panelCalib.add(btnGuiaCal);
        panelCalib.add(btnCalibrar);
        gd.addPanel(panelCalib);

        gd.addMessage("DETERMINACIONES A ANALIZAR");
        gd.addCheckbox("DAPI", true);
        gd.addCheckbox("Rojo (AF594)", false);
        gd.addCheckbox("Verde (AF488)", false);

        gd.addMessage("MEDICIÓN DE ÁREA");
        gd.addRadioButtonGroup("", new String[]{"Área de Marca", "Área Total de foto"}, 1, 2, "Área de Marca");
        estilizarDialogo(gd, true);
        gd.showDialog();
        if (gd.wasCanceled()) {
            return null;
        }
        if (this.ir_a_calibrar) {
            return new Opciones(false, false, false, "");
        }
        return new Opciones(gd.getNextBoolean(), gd.getNextBoolean(), gd.getNextBoolean(), gd.getNextRadioButton());
    }

    private void estilizarDialogo(GenericDialog gd, boolean esPrincipal) {
        gd.setBackground(esPrincipal ? COLOR_FONDO_GRIS : COLOR_BLANCO);
        LayoutManager layout = gd.getLayout();
        for (Component c : gd.getComponents()) {
            if (c instanceof Label) {
                estilizarEtiqueta((Label) c, layout);
            }
            if (c instanceof Button) {
                estilizarBoton((Button) c);
            }
            if (!(c instanceof Panel)) continue;
            Panel p = (Panel) c;
            p.setBackground(esPrincipal ? COLOR_FONDO_GRIS : COLOR_BLANCO);
            estirarAlAncho(p, layout);
            for (Component sub : p.getComponents()) {
                if (sub instanceof Button) estilizarBoton((Button) sub);
            }
        }
    }

    private void estilizarEtiqueta(Label l, LayoutManager layout) {
        String txt = l.getText().toUpperCase();
        // Se prueban las dos escrituras, con tilde y sin ella: los titulos del
        // dialogo llevan tilde, pero dejar solo esa variante deja el estilo a
        // merced de como se escriba manana una seccion nueva.
        boolean esSeccion = txt.contains("INFORMACIÓN") || txt.contains("INFORMACION")
                || txt.contains("DETERMINACIONES")
                || txt.contains("MEDICIÓN") || txt.contains("MEDICION")
                || txt.contains("CALIBRACIÓN") || txt.contains("CALIBRACION")
                || txt.contains("ENTRENAMIENTO") || txt.contains("RESULTADOS");
        if (!esSeccion) {
            l.setFont(FONT_MENSAJE);
            return;
        }
        l.setFont(FONT_SECCION);
        l.setBackground(COLOR_CABECERA);
        l.setForeground(COLOR_TEXTO_SECCION);
        l.setAlignment(Label.CENTER);
        estirarAlAncho(l, layout);
    }

    // Las cabeceras de seccion y los paneles de botones se estiran de borde a
    // borde del dialogo, con el mismo margen lateral. Sin esto quedan al ancho
    // de su texto y el dialogo se ve desalineado.
    private void estirarAlAncho(Component c, LayoutManager layout) {
        if (!(layout instanceof GridBagLayout)) return;
        GridBagLayout gbl = (GridBagLayout) layout;
        GridBagConstraints gbc = gbl.getConstraints(c);
        if (gbc == null) return;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        if (gbc.insets != null) {
            gbc.insets.left = 10;
            gbc.insets.right = 10;
        } else {
            gbc.insets = new Insets(0, 10, 0, 10);
        }
        gbl.setConstraints(c, gbc);
    }

    // Los botones se reconocen por su etiqueta, no por el orden en que se
    // agregaron: asi agregar o mover un boton no descoloca los colores.
    private void estilizarBoton(Button b) {
        String lbl = b.getLabel().toUpperCase();
        b.setForeground(COLOR_BOTON_TEXTO);
        if (lbl.contains("GUÍA DE USO") || lbl.contains("GUIA DE USO")) {
            b.setFont(FONT_BOTON_BOLD);
            b.setBackground(COLOR_BOTON_INFO);
        } else if (lbl.contains("INFO") || lbl.contains("ACERCA")) {
            b.setFont(FONT_BOTON);
            b.setBackground(COLOR_BOTON_INFO);
        } else {
            b.setFont(FONT_BOTON);
            b.setBackground(COLOR_BOTON_GRIS);
        }
    }

    private void mostrarInfoPlugin() {
        GenericDialog gd = new GenericDialog("Informacion del Plugin");
        try {
            java.net.URL url = getClass().getResource("/DVR-Fluo-Analisis-logo.png");
            if (url != null) {
                java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(url);
                if (img != null) {
                    int w = 300;
                    int h = (int) (((double) img.getHeight() / img.getWidth()) * w);
                    java.awt.Image escalada = img.getScaledInstance(w, h, java.awt.Image.SCALE_SMOOTH);
                    gd.addImage(new ImagePlus("Logo", escalada));
                }
            }
        } catch (Exception e) {
            // Ignorar errores de carga de imagen para que el complemento siga funcionando
        }

        String infoTexto = "<html><center><br><h3>Analizar Intensidad de Fluorescencia</h3>"
                + "Analisis de Intensidad de Fluorescencia.<br><br>"
                + "<b>Creado por:</b><br><b>Dr. Ezequiel Grondona</b><br>"
                + "-UNC - INICSA - CONICET-<br>- 2026 -<br><br>"
                + "Lea atentamente la guia de uso antes de utilizar el plugin.<br>"
                + "Consultas y sugerencias:<br>ezequiel.grondona@unc.edu.ar<br></center></html>";
        Panel panel = new Panel();
        javax.swing.JLabel etiqueta = new javax.swing.JLabel(infoTexto);
        panel.add(etiqueta);
        gd.addPanel(panel);

        estilizarDialogo(gd, false);
        gd.showDialog();
    }

    private void abrirLink(String url) {
        try {
            Desktop.getDesktop().browse(new URI(url));
        } catch (Exception e) {
            IJ.showMessage("Error", "No se pudo abrir el enlace:" + System.lineSeparator() + e.getMessage());
        }
    }

    private void renombrarArchivos(File folderFile) {
        File[] subfolders = folderFile.listFiles(File::isDirectory);
        if (subfolders == null) {
            return;
        }
        for (File sub : subfolders) {
            File[] files = sub.listFiles();
            if (files == null) continue;
            for (File f : files) {
                String name = f.getName().toLowerCase();
                if (!name.endsWith(".tif") || f.getName().startsWith(sub.getName() + "_")) continue;
                f.renameTo(new File(sub, sub.getName() + "_" + f.getName()));
            }
        }
    }

    private int procesarDapi(File folderFile, Map<String, RowData> dataMap) {
        int count = 0;
        File[] subfolders = folderFile.listFiles(File::isDirectory);
        if (subfolders == null || subfolders.length == 0) {
            subfolders = new File[]{folderFile};
        }
        for (File sub : subfolders) {
            File[] files = sub.listFiles();
            if (files == null) continue;
            for (File f : files) {
                ImagePlus imp;
                String name = f.getName().toLowerCase();
                if (!name.endsWith(".tif") || !name.contains("dapi") || (imp = IJ.openImage(f.getAbsolutePath())) == null) continue;
                imp.show();
                IJ.run(imp, "8-bit", "");
                IJ.setAutoThreshold(imp, "Triangle dark");
                Prefs.blackBackground = true;
                IJ.run(imp, "Convert to Mask", "");
                IJ.run(imp, "Analyze Particles...", "size=10-Infinity show=Outlines exclude summarize");
                Window sumWin = WindowManager.getWindow("Summary");
                if (sumWin instanceof TextWindow) {
                    TextPanel tp = ((TextWindow)sumWin).getTextPanel();
                    String headingsStr = tp.getColumnHeadings();
                    String[] headings = headingsStr.split("\t");
                    String lastLineStr = tp.getLine(tp.getLineCount() - 1);
                    String[] lastLine = lastLineStr.split("\t");
                    RowData row = dataMap.get(sub.getName());
                    if (row == null) {
                        row = new RowData(sub.getName());
                        dataMap.put(sub.getName(), row);
                    }
                    for (int i = 0; i < Math.min(headings.length, lastLine.length); ++i) {
                        String h = headings[i].trim();
                        if (h.equals("Count")) {
                            row.cantidadCelulas = (int)Double.parseDouble(lastLine[i]);
                            continue;
                        }
                        if (!h.equals("Total Area")) continue;
                        row.areaTotalNucleos = Double.parseDouble(lastLine[i]);
                    }
                }
                ++count;
                imp.changes = false;
                imp.close();
                IJ.run("Close All");
            }
        }
        return count;
    }

    private int procesarCanal(File folderFile, Map<String, RowData> dataMap, String pattern, String tipoArea, String colorName) {
        int count = 0;
        Integer backgroundValue = null;
        ResultsTable rt = ResultsTable.getResultsTable();
        if (rt != null) rt.reset();

        File[] subfolders = folderFile.listFiles(File::isDirectory);
        if (subfolders == null || subfolders.length == 0) {
            subfolders = new File[]{folderFile};
        }
        for (File sub : subfolders) {
            File[] files = sub.listFiles();
            if (files == null) continue;
            for (File f : files) {
                ImagePlus imp;
                String name = f.getName().toLowerCase();
                if (!name.endsWith(".tif") || !name.contains(pattern) || (imp = IJ.openImage(f.getAbsolutePath())) == null) continue;
                imp.show();
                if (tipoArea.equals("\u00c1rea de Marca")) {
                    ImagePlus dup = imp.duplicate();
                    IJ.run(dup, "8-bit", "");
                    String thresh = pattern.contains("488") ? "Moments dark" : "Triangle dark";
                    IJ.setAutoThreshold(dup, thresh);
                    IJ.run(dup, "Create Selection", "");
                    Roi roi = dup.getRoi();
                    dup.close();
                    if (roi != null) {
                        imp.setRoi(roi);
                    } else {
                        IJ.run(imp, "Select None", "");
                    }
                } else {
                    IJ.run(imp, "Select None", "");
                }
                IJ.run(imp, "8-bit", "");
                if (!tipoArea.equals("\u00c1rea de Marca")) {
                    if (backgroundValue == null) {
                        // Aplicar LUT para facilitar visualización de la marca según el canal
                        if (pattern.contains("594")) {
                            IJ.run(imp, "Red", "");
                        } else if (pattern.contains("488")) {
                            IJ.run(imp, "Green", "");
                        }

                        if (rt != null) {
                            rt.reset(); 
                            rt.show("Results"); // Forzar visibilidad para feedback de tecla 'm'
                        }
                        GenericDialog gd = new GenericDialog("Determinar de Fondo");
                        gd.addMessage("1. Dibuje un rectangulo sobre el FONDO de la imagen.\n2. Presione la tecla 'm' para medir.\n3. Presione OK para capturar el valor.");
                        estilizarDialogo(gd, false);
                        gd.setModal(false);
                        gd.showDialog();
                        while (!gd.wasOKed() && !gd.wasCanceled()) {
                            IJ.wait(100);
                        }
                        if (gd.wasCanceled()) {
                            return 0;
                        }
                        
                        double val = 0;
                        if (rt != null && rt.size() > 0) {
                            val = rt.getValue("Mean", rt.size() - 1);
                        }
                        
                        GenericDialog gdConf = new GenericDialog("Confirmar Sustraccion");
                        if (val == 0) {
                            gdConf.addMessage("ADVERTENCIA: No se detectó ninguna medición.\nUse 'm' para medir el fondo antes de presionar OK.");
                        }
                        gdConf.addNumericField("Valor a restar de todas las " + colorName.toUpperCase() + ":", val, 0);
                        estilizarDialogo(gdConf, false);
                        gdConf.showDialog();
                        backgroundValue = gdConf.wasCanceled() ? 0 : (int)gdConf.getNextNumber();
                        
                        if (rt != null) rt.reset(); // Limpiar tabla tras capturar fondo para empezar de cero con las fotos
                    }
                    if (backgroundValue > 0) {
                        IJ.run(imp, "Select None", "");
                        IJ.run(imp, "Subtract...", ("value=" + backgroundValue));
                    }
                }
                IJ.run(imp, "Measure", "");
                rt = ResultsTable.getResultsTable();
                if (rt != null && rt.size() > 0) {
                    String[] headings;
                    RowData row = dataMap.get(sub.getName());
                    if (row == null) {
                        row = new RowData(sub.getName());
                        dataMap.put(sub.getName(), row);
                    }
                    for (String h : headings = rt.getHeadings()) {
                        double val;
                        if (h.equals("Mean")) {
                            val = rt.getValue(h, rt.size() - 1);
                            if (pattern.contains("594")) {
                                row.intensidadRojo = val;
                                continue;
                            }
                            row.intensidadVerde = val;
                            continue;
                        }
                        if (!h.equals("Area")) continue;
                        val = rt.getValue(h, rt.size() - 1);
                        if (pattern.contains("594")) {
                            row.areaRojo = val;
                            continue;
                        }
                        row.areaVerde = val;
                    }
                }
                ++count;
                imp.changes = false;
                imp.close();
                IJ.run("Close All");
            }
        }
        return count;
    }

    private void guardarExcel(File folderFile, Map<String, RowData> dataMap, Opciones opt) {
        if (dataMap.isEmpty()) {
            return;
        }
        try {
            XSSFWorkbook wb = new XSSFWorkbook();
            XSSFSheet sheet = wb.createSheet("Resultados");
            XSSFRow header = sheet.createRow(0);
            ArrayList<String> cols = new ArrayList<String>(Arrays.asList("Carpeta", "Cantidad de c\u00e9lulas", "\u00c1rea de total N\u00facleos"));
            if (opt.verde) {
                cols.add("Intensidad de Marca (AF488)");
                cols.add("\u00c1rea de Marca (AF488)");
            }
            if (opt.rojo) {
                cols.add("Intensidad de Marca (AF594)");
                cols.add("\u00c1rea de Marca (AF594)");
            }
            for (int i = 0; i < cols.size(); ++i) {
                header.createCell(i).setCellValue(cols.get(i));
            }
            int rowIndex = 1;
            ArrayList<String> sortedKeys = new ArrayList<String>(dataMap.keySet());
            Collections.sort(sortedKeys);
            for (String key : sortedKeys) {
                RowData data = dataMap.get(key);
                XSSFRow row = sheet.createRow(rowIndex++);
                int cIdx = 0;
                row.createCell(cIdx++).setCellValue(data.carpeta);
                row.createCell(cIdx++).setCellValue(data.cantidadCelulas);
                row.createCell(cIdx++).setCellValue(data.areaTotalNucleos);
                if (opt.verde) {
                    row.createCell(cIdx++).setCellValue(data.intensidadVerde);
                    row.createCell(cIdx++).setCellValue(data.areaVerde);
                }
                if (!opt.rojo) continue;
                row.createCell(cIdx++).setCellValue(data.intensidadRojo);
                row.createCell(cIdx++).setCellValue(data.areaRojo);
            }
            for (int i = 0; i < cols.size(); ++i) {
                sheet.autoSizeColumn(i);
            }
            String fileName = "Resultados - " + folderFile.getName() + ".xlsx";
            File outFile = new File(folderFile, fileName);
            try (FileOutputStream fos = new FileOutputStream(outFile);){
                wb.write(fos);
            }
            wb.close();
        }
        catch (IOException e) {
            IJ.error(("Error al guardar Excel: " + e.getMessage()));
        }
    }

    private static class Opciones {
        boolean dapi;
        boolean rojo;
        boolean verde;
        String tipoArea;

        Opciones(boolean d, boolean r, boolean v, String t) {
            this.dapi = d;
            this.rojo = r;
            this.verde = v;
            this.tipoArea = t;
        }
    }

    private static class RowData {
        String carpeta;
        int cantidadCelulas = 0;
        double areaTotalNucleos = 0.0;
        double intensidadRojo = 0.0;
        double areaRojo = 0.0;
        double intensidadVerde = 0.0;
        double areaVerde = 0.0;

        RowData(String c) {
            this.carpeta = c;
        }
    }
}
