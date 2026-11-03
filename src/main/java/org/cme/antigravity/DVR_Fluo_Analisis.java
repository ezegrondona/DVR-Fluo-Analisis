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
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
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
                    verdeCount = this.procesarCanal(folderFile, dataMap, "af488", opt.tipoArea, "Green", opt.fondoAutomatico);
                }
                if (opt.rojo) {
                    rojoCount = this.procesarCanal(folderFile, dataMap, "af594", opt.tipoArea, "Red", opt.fondoAutomatico);
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

        // El titulo lleva "MEDICIÓN" a proposito: estilizarDialogo reconoce
        // esa palabra y lo pinta como cabecera de seccion, igual que las
        // demas.
        gd.addMessage("MEDICIÓN DEL FONDO");
        gd.addCheckbox("Automática (SMO)", true);

        estilizarDialogo(gd, true);
        gd.showDialog();
        if (gd.wasCanceled()) {
            return null;
        }
        if (this.ir_a_calibrar) {
            return new Opciones(false, false, false, "", true);
        }
        // Se leen en el orden en que se agregaron: las casillas por un lado y
        // el grupo de opciones por otro.
        boolean dapi = gd.getNextBoolean();
        boolean rojo = gd.getNextBoolean();
        boolean verde = gd.getNextBoolean();
        boolean automatico = gd.getNextBoolean();
        String area = gd.getNextRadioButton();
        return new Opciones(dapi, rojo, verde, area, automatico);
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

    // ====================================================================
    //  Estimacion automatica del fondo - Silver Mountain Operator (SMO)
    //
    //  Metodo de Mauro Silberberg y Hernan E. Grecco:
    //    "Robust and unbiased estimation of the background distribution
    //     for automated quantitative imaging"
    //    Journal of the Optical Society of America A (2023).
    //    doi:10.1364/JOSAA.477468
    //  Implementacion de referencia en Python, con licencia MIT:
    //    https://github.com/maurosilber/SMO
    //  Lo que sigue es una reimplementacion propia del mismo metodo.
    //
    //  La idea: donde no hay estructura, la DIRECCION del gradiente
    //  fluctua al azar, y el promedio local de los gradientes unitarios se
    //  cancela (longitud cercana a 0). Donde hay un objeto, las
    //  direcciones son coherentes y el promedio conserva su longitud
    //  (cercana a 1). Los pixeles de valor bajo son, entonces, fondo; la
    //  mediana de su intensidad es el fondo de esa imagen.
    //
    //  Importa que sea POR IMAGEN: en el material de prueba el fondo varia
    //  entre fotos de un mismo canal hasta el doble, de modo que un unico
    //  valor medido una sola vez no puede representarlas a todas.
    // ====================================================================

    /** Lado de la ventana de promediado, en pixeles.
     *
     *  Tiene que ser menor que las estructuras que se quieren conservar:
     *  una ventana mas grande que los objetos los difumina y termina
     *  tomandolos por fondo. 7 es el valor que usan los autores. */
    private static final int SMO_VENTANA = 7;

    /** Percentil de la distribucion nula que separa fondo de estructura. */
    private static final double SMO_PERCENTIL = 5.0;

    /** El umbral depende solo de la ventana, no de la imagen, asi que se
     *  calcula una vez por sesion. Se comprobo que entre 256x256 y
     *  2056x2464 varia menos de un 0,3%. */
    private static Double umbralSmoCache = null;

    /** Indice reflejado en los bordes, sin repetir el borde mismo. Es el
     *  modo "mirror" de la implementacion de referencia. */
    private static int espejo(int i, int n) {
        if (n == 1) return 0;
        int periodo = 2 * (n - 1);
        i = Math.abs(i) % periodo;
        return (i < n) ? i : periodo - i;
    }

    /** Promedio en una ventana cuadrada, aplicado por filas y despues por
     *  columnas: el resultado es el mismo que en dos dimensiones y cuesta
     *  mucho menos. */
    private static float[] filtroMedia(float[] origen, int w, int h, int lado) {
        int r = lado / 2;
        float[] paso = new float[w * h];
        for (int y = 0; y < h; y++) {
            int fila = y * w;
            for (int x = 0; x < w; x++) {
                double suma = 0;
                for (int k = -r; k <= r; k++) suma += origen[fila + espejo(x + k, w)];
                paso[fila + x] = (float) (suma / lado);
            }
        }
        float[] salida = new float[w * h];
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                double suma = 0;
                for (int k = -r; k <= r; k++) suma += paso[espejo(y + k, h) * w + x];
                salida[y * w + x] = (float) (suma / lado);
            }
        }
        return salida;
    }

    /** Aplica el operador: devuelve, para cada pixel, la longitud del
     *  promedio local de los gradientes unitarios. Valores entre 0 y 1. */
    private static float[] smo(float[] px, int w, int h) {
        int n = w * h;
        float[] gx = new float[n];
        float[] gy = new float[n];

        // Gradiente por diferencias centradas, unilaterales en los bordes.
        for (int y = 0; y < h; y++) {
            int fila = y * w;
            int ym = (y == 0) ? 0 : y - 1;
            int yp = (y == h - 1) ? h - 1 : y + 1;
            int dy = yp - ym;
            for (int x = 0; x < w; x++) {
                int xm = (x == 0) ? 0 : x - 1;
                int xp = (x == w - 1) ? w - 1 : x + 1;
                int dx = xp - xm;
                gx[fila + x] = (dx == 0) ? 0f : (px[fila + xp] - px[fila + xm]) / dx;
                gy[fila + x] = (dy == 0) ? 0f : (px[yp * w + x] - px[ym * w + x]) / dy;
            }
        }

        // Normalizar a longitud 1: se conserva la direccion y se descarta
        // la magnitud. Esto es lo que vuelve al metodo no parametrico.
        for (int i = 0; i < n; i++) {
            double largo = Math.sqrt((double) gx[i] * gx[i] + (double) gy[i] * gy[i]);
            if (largo > 0) {
                gx[i] = (float) (gx[i] / largo);
                gy[i] = (float) (gy[i] / largo);
            }
        }

        float[] px_ = filtroMedia(gx, w, h, SMO_VENTANA);
        float[] py_ = filtroMedia(gy, w, h, SMO_VENTANA);
        float[] salida = new float[n];
        for (int i = 0; i < n; i++) {
            salida[i] = (float) Math.sqrt((double) px_[i] * px_[i] + (double) py_[i] * py_[i]);
        }
        return salida;
    }

    /** Umbral bajo la hipotesis nula: que los pixeles no esten
     *  correlacionados. Se obtiene aplicando el operador a ruido uniforme
     *  y quedandose con el percentil indicado. */
    private static double umbralSmo() {
        if (umbralSmoCache != null) return umbralSmoCache.doubleValue();
        int lado = 512;
        float[] ruido = new float[lado * lado];
        java.util.Random azar = new java.util.Random(42L);
        for (int i = 0; i < ruido.length; i++) ruido[i] = azar.nextFloat();
        float[] s = smo(ruido, lado, lado);
        java.util.Arrays.sort(s);
        int k = (int) Math.round(SMO_PERCENTIL / 100.0 * (s.length - 1));
        umbralSmoCache = Double.valueOf(s[k]);
        return umbralSmoCache.doubleValue();
    }

    /** Fondo de la imagen: mediana de la intensidad de los pixeles que el
     *  operador clasifica como fondo. Devuelve 0 si no encuentra ninguno. */
    private static double fondoAutomatico(ImagePlus imp) {
        ImageProcessor ip = imp.getProcessor();
        int w = ip.getWidth();
        int h = ip.getHeight();
        float[] px = (float[]) ip.convertToFloat().getPixels();

        // Los pixeles saturados no informan sobre el fondo y ademas
        // distorsionan el gradiente: se excluyen, como indican los autores.
        double saturado = Double.MAX_VALUE;
        if (ip instanceof ByteProcessor) saturado = 255.0;
        else if (ip instanceof ShortProcessor) saturado = 65535.0;

        float[] s = smo(px, w, h);
        double umbral = umbralSmo();

        int cuantos = 0;
        for (int i = 0; i < s.length; i++) {
            if (s[i] < umbral && px[i] < saturado) cuantos++;
        }
        if (cuantos == 0) return 0.0;

        float[] fondo = new float[cuantos];
        int j = 0;
        for (int i = 0; i < s.length; i++) {
            if (s[i] < umbral && px[i] < saturado) fondo[j++] = px[i];
        }
        java.util.Arrays.sort(fondo);
        int m = fondo.length;
        return (m % 2 == 1) ? fondo[m / 2] : 0.5 * (fondo[m / 2 - 1] + fondo[m / 2]);
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
                imp.setTitle(sub.getName() + "_" + f.getName());
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

    private int procesarCanal(File folderFile, Map<String, RowData> dataMap, String pattern, String tipoArea, String colorName, boolean fondoAuto) {
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

                // El titulo es lo que identifica a cada fila en la tabla de
                // resultados. Antes eso se conseguia renombrando el archivo en
                // el disco; cambiar el titulo de la imagen ya abierta cumple
                // la misma funcion y no toca el material del usuario.
                imp.setTitle(sub.getName() + "_" + f.getName());

                // La seleccion se sigue calculando sobre un duplicado de 8
                // bits. Los umbrales automaticos de ImageJ estan pensados para
                // esa profundidad, y calcularlos sobre los 16 bits moveria la
                // segmentacion. Lo que cambia en esta version es la escala en
                // que se MIDE, no la forma en que se segmenta.
                Roi roi = null;
                if (tipoArea.equals("\u00c1rea de Marca")) {
                    ImagePlus dup = imp.duplicate();
                    IJ.run(dup, "8-bit", "");
                    String thresh = pattern.contains("488") ? "Moments dark" : "Triangle dark";
                    IJ.setAutoThreshold(dup, thresh);
                    IJ.run(dup, "Create Selection", "");
                    roi = dup.getRoi();
                    dup.close();
                }

                double fondo = 0.0;
                if (fondoAuto) {
                    // Se estima en cada imagen. El fondo no es el mismo en
                    // todas las fotos de un canal: en el material de prueba
                    // llego a variar al doble dentro de la misma tanda.
                    fondo = fondoAutomatico(imp);
                } else if (backgroundValue == null) {
                    if (pattern.contains("594")) {
                        IJ.run(imp, "Red", "");
                    } else if (pattern.contains("488")) {
                        IJ.run(imp, "Green", "");
                    }
                    if (rt != null) {
                        rt.reset();
                        rt.show("Results"); // Forzar visibilidad para feedback de tecla 'm'
                    }
                    double val;
                    while (true) {
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
                        if (rt != null && rt.size() > 0) {
                            val = rt.getValue("Mean", rt.size() - 1);
                            break;
                        }
                        GenericDialog aviso = new GenericDialog("Falta medir el fondo");
                        aviso.addMessage("ADVERTENCIA: No se detect\u00f3 ninguna medici\u00f3n.\nUse \"m\" para medir fondo.");
                        estilizarDialogo(aviso, false);
                        aviso.showDialog();
                        if (aviso.wasCanceled()) {
                            return 0;
                        }
                    }
                    GenericDialog gdConf = new GenericDialog("Confirmar Sustraccion");
                    gdConf.addNumericField("Valor a restar de todas las " + colorName.toUpperCase() + ":", val, 0);
                    estilizarDialogo(gdConf, false);
                    gdConf.showDialog();
                    backgroundValue = Integer.valueOf(gdConf.wasCanceled() ? 0 : (int)gdConf.getNextNumber());
                    if (rt != null) rt.reset(); // Limpiar tabla tras capturar fondo para empezar de cero con las fotos
                    fondo = backgroundValue.doubleValue();
                } else {
                    fondo = backgroundValue.doubleValue();
                }

                // "Subtract" solo afecta a la seleccion activa, de modo que se
                // resta con la seleccion apagada y recien despues se aplica la
                // marca para medir dentro de ella.
                IJ.run(imp, "Select None", "");
                if (fondo > 0.0) {
                    IJ.run(imp, "Subtract...", "value=" + Math.round(fondo));
                }
                if (roi != null) {
                    imp.setRoi(roi);
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
                    if (pattern.contains("594")) {
                        row.fondoRojo = fondo;
                    } else {
                        row.fondoVerde = fondo;
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
                cols.add("Fondo restado (AF488)");
            }
            if (opt.rojo) {
                cols.add("Intensidad de Marca (AF594)");
                cols.add("\u00c1rea de Marca (AF594)");
                cols.add("Fondo restado (AF594)");
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
                    row.createCell(cIdx++).setCellValue(data.fondoVerde);
                }
                if (!opt.rojo) continue;
                row.createCell(cIdx++).setCellValue(data.intensidadRojo);
                row.createCell(cIdx++).setCellValue(data.areaRojo);
                row.createCell(cIdx++).setCellValue(data.fondoRojo);
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
        boolean fondoAutomatico;

        Opciones(boolean d, boolean r, boolean v, String t, boolean fa) {
            this.dapi = d;
            this.rojo = r;
            this.verde = v;
            this.tipoArea = t;
            this.fondoAutomatico = fa;
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
        // Fondo que efectivamente se resto a cada imagen. Queda en la
        // planilla para que el resultado sea trazable: sin esto no hay modo
        // de saber despues con que se midio.
        double fondoRojo = 0.0;
        double fondoVerde = 0.0;

        RowData(String c) {
            this.carpeta = c;
        }
    }
}
