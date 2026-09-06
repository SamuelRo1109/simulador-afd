package com.unbosque.afd.desktop.render;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

public final class MetricasTexto {

    private static final Graphics2D CONTEXTO = crearContexto();

    private MetricasTexto() {
    }

    public static FontMetrics metricas(Font fuente) {
        return CONTEXTO.getFontMetrics(fuente);
    }

    public static double ancho(Font fuente, String texto) {
        return metricas(fuente).stringWidth(texto);
    }

    public static double alto(Font fuente) {
        FontMetrics metrica = metricas(fuente);
        return metrica.getAscent() + metrica.getDescent();
    }

    private static Graphics2D crearContexto() {
        Graphics2D graficos = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics();
        graficos.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        return graficos;
    }
}
