package com.unbosque.afd.desktop.render;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Locale;

public final class TipografiaApp {

    private static final String FAMILIA_INTERFAZ = resolverFamiliaInterfaz();

    public static final Font TITULO = new Font(FAMILIA_INTERFAZ, Font.BOLD, 15);
    public static final Font CUERPO = new Font(FAMILIA_INTERFAZ, Font.PLAIN, 13);
    public static final Font CUERPO_FUERTE = new Font(FAMILIA_INTERFAZ, Font.BOLD, 13);
    public static final Font ETIQUETA = new Font(FAMILIA_INTERFAZ, Font.PLAIN, 11);
    public static final Font ETIQUETA_FUERTE = new Font(FAMILIA_INTERFAZ, Font.BOLD, 11);
    public static final Font MONO = new Font(Font.MONOSPACED, Font.PLAIN, 13);
    public static final Font MONO_FUERTE = new Font(Font.MONOSPACED, Font.BOLD, 13);
    public static final Font MONO_GRANDE = new Font(Font.MONOSPACED, Font.BOLD, 18);
    public static final Font MARCA = new Font(FAMILIA_INTERFAZ, Font.BOLD, 14);
    public static final Font MARCA_SECUNDARIA = new Font(FAMILIA_INTERFAZ, Font.PLAIN, 10);

    private TipografiaApp() {
    }

    public static String familiaInterfaz() {
        return FAMILIA_INTERFAZ;
    }

    public static Font interfaz(int estilo, float tamano) {
        return new Font(FAMILIA_INTERFAZ, estilo, 12).deriveFont(estilo, tamano);
    }

    public static Font mono(int estilo, float tamano) {
        return MONO.deriveFont(estilo, tamano);
    }

    private static String resolverFamiliaInterfaz() {
        try {
            String[] disponibles = GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getAvailableFontFamilyNames(Locale.ROOT);
            for (String familia : disponibles) {
                if (familia.equalsIgnoreCase("Segoe UI")) {
                    return familia;
                }
            }
        } catch (RuntimeException | Error ignorado) {
            return Font.SANS_SERIF;
        }
        return Font.SANS_SERIF;
    }
}
