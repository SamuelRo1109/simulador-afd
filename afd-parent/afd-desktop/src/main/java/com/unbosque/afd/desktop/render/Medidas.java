package com.unbosque.afd.desktop.render;

import java.awt.Graphics2D;
import java.awt.RenderingHints;

public final class Medidas {

    public static final int PASO = 4;
    public static final int RADIO_TARJETA = 8;
    public static final int RADIO_BOTON = 6;
    public static final int ALTO_BARRA_SUPERIOR = 56;
    public static final int ANCHO_RIEL = 64;
    public static final int LADO_HERRAMIENTA = 48;
    public static final int ANCHO_INSPECTOR = 320;
    public static final int ALTO_CAJON = 140;
    public static final int PASO_RETICULA = 24;
    public static final int GROSOR_BARRA_DESPLAZAMIENTO = 6;

    private Medidas() {
    }

    public static int paso(int multiplo) {
        return PASO * multiplo;
    }

    public static void calidad(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }
}
