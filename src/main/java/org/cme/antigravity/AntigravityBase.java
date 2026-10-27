/*
 * AntigravityBase - utilidades de interfaz compartidas por los complementos.
 *
 * PROCEDENCIA DE ESTE ARCHIVO: igual que DVR_Fluo_Analisis.java, se recupero
 * descompilando el .jar tras perderse el fuente original, y desde entonces se
 * mantiene a mano.
 */
package org.cme.antigravity;

import ij.IJ;
import ij.gui.GenericDialog;
import java.awt.Button;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.LayoutManager;
import java.awt.Panel;
import java.net.URI;

public class AntigravityBase {
    public static final Font FONT_SECCION = new Font("SansSerif", 1, 13);
    public static final Color COLOR_CABECERA = new Color(25, 136, 255);
    public static final Color COLOR_TEXTO_SECCION = new Color(255, 255, 255);
    public static final Font FONT_BOTON = new Font("SansSerif", 0, 10);
    public static final Font FONT_BOTON_BOLD = new Font("SansSerif", 1, 10);
    public static final Color COLOR_BOTON_INFO = new Color(0, 151, 161);
    public static final Color COLOR_BOTON_GRIS = new Color(157, 167, 159);
    public static final Color COLOR_BOTON_TEXTO = new Color(255, 255, 255);
    public static final Color COLOR_FONDO_GRIS = new Color(240, 245, 240);
    public static final Color COLOR_BLANCO = Color.WHITE;

    public static void estilizarDialogo(GenericDialog gd, boolean esPrincipal) {
        if (esPrincipal) {
            gd.setBackground(COLOR_FONDO_GRIS);
        } else {
            gd.setBackground(COLOR_BLANCO);
        }
        Component[] components = gd.getComponents();
        LayoutManager layout = gd.getLayout();
        for (Component c : components) {
            if (c instanceof Label) {
                Label l = (Label)c;
                String txt = l.getText().toUpperCase();
                if (txt.contains("INFORMACIÓN") || txt.contains("INFORMACION") || txt.contains("DETERMINACIONES") || txt.contains("MEDICIÓN") || txt.contains("MEDICION") || txt.contains("CALIBRACIÓN") || txt.contains("CALIBRACION") || txt.contains("ENTRENAMIENTO") || txt.contains("RESULTADOS")) {
                    l.setFont(FONT_SECCION);
                    l.setBackground(COLOR_CABECERA);
                    l.setForeground(COLOR_TEXTO_SECCION);
                    l.setAlignment(1);
                    if (layout instanceof GridBagLayout) {
                        GridBagLayout gbl = (GridBagLayout)layout;
                        GridBagConstraints gbc = gbl.getConstraints(l);
                        gbc.fill = 2;
                        if (gbc.insets != null) {
                            gbc.insets.left = 10;
                            gbc.insets.right = 10;
                        } else {
                            gbc.insets = new Insets(0, 10, 0, 10);
                        }
                        gbl.setConstraints(l, gbc);
                    }
                } else {
                    l.setFont(new Font("SansSerif", 0, 12));
                }
            }
            if (c instanceof Button) {
                Button b = (Button)c;
                String lbl = b.getLabel().toUpperCase();
                b.setForeground(COLOR_BOTON_TEXTO);
                
                boolean isGuideUse = lbl.contains("GUÍA DE USO") || lbl.contains("GUIA DE USO");
                boolean isCalibGroup = lbl.contains("CALIBR");
                boolean isInfoAcerca = lbl.contains("INFO") || lbl.contains("ACERCA");

                if (isGuideUse) {
                    b.setFont(FONT_BOTON_BOLD);
                    b.setBackground(COLOR_BOTON_INFO);
                } else if (isInfoAcerca) {
                    b.setFont(FONT_BOTON);
                    b.setBackground(COLOR_BOTON_INFO);
                } else if (isCalibGroup) {
                    b.setFont(FONT_BOTON);
                    b.setBackground(COLOR_BOTON_GRIS);
                } else {
                    b.setFont(FONT_BOTON);
                    b.setBackground(COLOR_BOTON_GRIS);
                }
            }
            if (!(c instanceof Panel)) continue;
            Panel p = (Panel)c;
            p.setBackground(esPrincipal ? COLOR_FONDO_GRIS : COLOR_BLANCO);
            if (layout instanceof GridBagLayout) {
                GridBagLayout gbl = (GridBagLayout)layout;
                GridBagConstraints gbc = gbl.getConstraints(p);
                gbc.fill = 2;
                if (gbc.insets != null) {
                    gbc.insets.left = 10;
                    gbc.insets.right = 10;
                } else {
                    gbc.insets = new Insets(0, 10, 0, 10);
                }
                gbl.setConstraints(p, gbc);
            }
            for (Component sub : p.getComponents()) {
                if (!(sub instanceof Button)) continue;
                sub.setForeground(COLOR_BOTON_TEXTO);
                String sl = ((Button)sub).getLabel().toUpperCase();
                
                boolean isGuideUseP = sl.contains("GUÍA DE USO") || sl.contains("GUIA DE USO");
                boolean isCalibGroupP = sl.contains("CALIBR");
                boolean isInfoAcercaP = sl.contains("INFO") || sl.contains("ACERCA");

                if (isGuideUseP) {
                    sub.setFont(FONT_BOTON_BOLD);
                    sub.setBackground(COLOR_BOTON_INFO);
                } else if (isInfoAcercaP) {
                    sub.setFont(FONT_BOTON);
                    sub.setBackground(COLOR_BOTON_INFO);
                } else if (isCalibGroupP) {
                    sub.setFont(FONT_BOTON);
                    sub.setBackground(COLOR_BOTON_GRIS);
                } else {
                    sub.setFont(FONT_BOTON);
                    sub.setBackground(COLOR_BOTON_GRIS);
                }
            }
        }
    }

    public static void abrirLink(String url) {
        try {
            Desktop.getDesktop().browse(new URI(url));
        }
        catch (Exception ex) {
            IJ.error("No se pudo abrir el link.");
        }
    }
        public static void mostrarAcercaDe(String titulo, String autor, String institucion, String mail) {
        try {
            // El logo viaja como recurso dentro del .jar. Antes estaba incrustado
            // en este mismo archivo como una cadena base64 de 95 KB, que habia
            // que decodificar en cada apertura; el recurso deja el fuente
            // legible y es una sola copia.
            //
            // Se sigue volcando a un archivo temporal porque el cartel se arma
            // con HTML y la etiqueta <img> necesita una URL que el renderizador
            // de ImageJ sepa abrir. Una URL "jar:" directa al recurso evitaria
            // el rodeo, pero no esta verificada: el temporal si funciona.
            byte[] imgBytes;
            java.io.InputStream in = AntigravityBase.class.getResourceAsStream("/dvr-fluo-analisis-logo.png");
            if (in == null) throw new java.io.IOException("No se encontro el logo dentro del complemento.");
            try {
                java.io.ByteArrayOutputStream buf = new java.io.ByteArrayOutputStream();
                byte[] trozo = new byte[8192];
                int leidos;
                while ((leidos = in.read(trozo)) != -1) buf.write(trozo, 0, leidos);
                imgBytes = buf.toByteArray();
            } finally {
                in.close();
            }
            java.io.File tmpFile = java.io.File.createTempFile("dvr_logo", ".png");
            tmpFile.deleteOnExit();
            java.io.FileOutputStream fos = new java.io.FileOutputStream(tmpFile);
            try {
                fos.write(imgBytes);
            } finally {
                fos.close();
            }
            String imgUrl = tmpFile.toURI().toString();
            StringBuilder sb = new StringBuilder();
            sb.append("<html><center><img src='");
            sb.append(imgUrl);
            sb.append("' width='300'><br><br><b><font size='5'>");
            sb.append(titulo);
            sb.append("</font></b><br><br>Analisis de Intensidad de Fluorescencia.<br><br>");
            sb.append("<b>Creado por:</b><br><b>");
            sb.append(autor);
            sb.append("</b><br>");
            sb.append(institucion);
            sb.append("<br>- 2026 -<br><br>Lea atentamente la guia de uso antes de utilizar el plugin.<br>Consultas y sugerencias:<br>");
            sb.append(mail);
            sb.append("</center></html>");
            IJ.showMessage("Informacion del Plugin", sb.toString());
            tmpFile.delete();
        } catch (Exception e) {
            IJ.error("No se pudo mostrar la informacion del plugin: " + e.getMessage());
        }
    }
}