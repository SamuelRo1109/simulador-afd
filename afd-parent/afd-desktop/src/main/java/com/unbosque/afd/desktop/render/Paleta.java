package com.unbosque.afd.desktop.render;

import java.awt.Color;

public final class Paleta {

    public static final Color FONDO = new Color(0xFAFAF9);
    public static final Color ESTADO_RELLENO = new Color(0xFFFFFF);
    public static final Color ESTADO_BORDE = new Color(0x3F3F46);
    public static final Color INICIAL = new Color(0x2E9E5B);
    public static final Color ACEPTACION = new Color(0xB5179E);
    public static final Color ACTIVO = new Color(0xF97316);
    public static final Color ARISTA = new Color(0x71717A);
    public static final Color ACEPTADA = new Color(0x22A06B);
    public static final Color RECHAZADA = new Color(0xD93025);

    public static final Color BORDE_SUAVE = mezclar(ARISTA, FONDO, 0.28);
    public static final Color TEXTO_TENUE = mezclar(ESTADO_BORDE, FONDO, 0.60);
    public static final Color CELDA_SIN_DEFINIR = mezclar(RECHAZADA, ESTADO_RELLENO, 0.15);

    private Paleta() {
    }

    public static Color mezclar(Color frente, Color fondo, double opacidad) {
        double peso = Math.min(1, Math.max(0, opacidad));
        return new Color(
                (int) Math.round(frente.getRed() * peso + fondo.getRed() * (1 - peso)),
                (int) Math.round(frente.getGreen() * peso + fondo.getGreen() * (1 - peso)),
                (int) Math.round(frente.getBlue() * peso + fondo.getBlue() * (1 - peso)));
    }
}
