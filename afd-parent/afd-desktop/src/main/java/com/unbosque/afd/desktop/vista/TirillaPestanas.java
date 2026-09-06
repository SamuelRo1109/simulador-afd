package com.unbosque.afd.desktop.vista;

import com.unbosque.afd.desktop.render.Medidas;
import com.unbosque.afd.desktop.render.Tema;
import com.unbosque.afd.desktop.render.TipografiaApp;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.util.List;
import java.util.function.IntConsumer;

public class TirillaPestanas extends JComponent implements Tema.Sensible {

    private static final int ANCHO = 28;
    private static final int ALTO_PESTANA = 116;
    private static final int GROSOR_INDICADOR = 3;

    private final List<String> etiquetas;

    private int seleccionada;
    private int sobrevolada = -1;

    public TirillaPestanas(List<String> etiquetas, IntConsumer alSeleccionar) {
        this.etiquetas = List.copyOf(etiquetas);

        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        MouseAdapter interaccion = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent evento) {
                int indice = indiceEn(evento.getY());
                if (indice != sobrevolada) {
                    sobrevolada = indice;
                    repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent evento) {
                sobrevolada = -1;
                repaint();
            }

            @Override
            public void mousePressed(MouseEvent evento) {
                int indice = indiceEn(evento.getY());
                if (indice >= 0) {
                    seleccionar(indice);
                    if (alSeleccionar != null) {
                        alSeleccionar.accept(indice);
                    }
                }
            }
        };
        addMouseListener(interaccion);
        addMouseMotionListener(interaccion);
    }

    public void seleccionar(int indice) {
        if (indice < 0 || indice >= etiquetas.size()) {
            return;
        }
        seleccionada = indice;
        repaint();
    }

    public int seleccionada() {
        return seleccionada;
    }

    private int indiceEn(int y) {
        int indice = y / ALTO_PESTANA;
        return indice >= 0 && indice < etiquetas.size() ? indice : -1;
    }

    @Override
    public void aplicarTema() {
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(ANCHO, ALTO_PESTANA * etiquetas.size());
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(ANCHO, Integer.MAX_VALUE);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            Medidas.calidad(g2);
            Tema tema = Tema.actual();
            g2.setColor(tema.panelFondo());
            g2.fillRect(0, 0, getWidth(), getHeight());

            g2.setFont(TipografiaApp.ETIQUETA_FUERTE);
            FontMetrics metrica = g2.getFontMetrics();

            for (int indice = 0; indice < etiquetas.size(); indice++) {
                int y = indice * ALTO_PESTANA;
                boolean activa = indice == seleccionada;

                if (activa) {
                    g2.setColor(tema.panelElevado());
                    g2.fillRect(0, y, getWidth(), ALTO_PESTANA);
                    g2.setColor(Tema.ACTIVO);
                    g2.fillRect(getWidth() - GROSOR_INDICADOR, y + Medidas.paso(2),
                            GROSOR_INDICADOR, ALTO_PESTANA - Medidas.paso(4));
                } else if (indice == sobrevolada) {
                    g2.setColor(tema.sobrevuelo());
                    g2.fillRect(0, y, getWidth(), ALTO_PESTANA);
                }

                String texto = etiquetas.get(indice);
                Color color = activa ? Tema.ACTIVO : tema.textoSecundario();
                g2.setColor(color);

                AffineTransform original = g2.getTransform();
                g2.translate(getWidth() / 2.0 + metrica.getAscent() / 2.0 - 2,
                        y + (ALTO_PESTANA + metrica.stringWidth(texto)) / 2.0);
                g2.rotate(-Math.PI / 2);
                g2.drawString(texto, 0, 0);
                g2.setTransform(original);
            }

            g2.setColor(tema.panelBorde());
            g2.drawLine(getWidth() - 1, 0, getWidth() - 1, getHeight());
        } finally {
            g2.dispose();
        }
    }
}
