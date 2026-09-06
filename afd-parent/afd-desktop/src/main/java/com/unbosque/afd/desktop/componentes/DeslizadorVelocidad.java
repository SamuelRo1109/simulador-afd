package com.unbosque.afd.desktop.componentes;

import com.unbosque.afd.desktop.render.Medidas;
import com.unbosque.afd.desktop.render.Tema;
import com.unbosque.afd.desktop.render.TipografiaApp;

import javax.swing.JSlider;
import javax.swing.plaf.basic.BasicSliderUI;
import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;

public class DeslizadorVelocidad extends JSlider implements Tema.Sensible {

    public static final int MINIMO = 200;
    public static final int MAXIMO = 2000;

    private static final int GROSOR_PISTA = 4;
    private static final int DIAMETRO_PULGAR = 14;
    private static final int ALTO = 34;

    public DeslizadorVelocidad(int inicial) {
        super(MINIMO, MAXIMO, Math.min(MAXIMO, Math.max(MINIMO, inicial)));
        setOpaque(false);
        setFocusable(false);
        setPaintTicks(false);
        setPaintLabels(false);
        setUI(new InterfazDeslizador(this));
        setToolTipText("Velocidad de reproducción");
    }

    @Override
    public void aplicarTema() {
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(160, ALTO);
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(110, ALTO);
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(220, ALTO);
    }

    private static final class InterfazDeslizador extends BasicSliderUI {

        private InterfazDeslizador(JSlider deslizador) {
            super(deslizador);
        }

        @Override
        protected Dimension getThumbSize() {
            return new Dimension(DIAMETRO_PULGAR, DIAMETRO_PULGAR);
        }

        @Override
        public void paintFocus(Graphics g) {
            // El deslizador no dibuja marco de foco: el pulgar ya comunica el estado.
        }

        @Override
        public void paintTrack(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                Medidas.calidad(g2);
                Tema tema = Tema.actual();
                Rectangle pista = trackRect;
                double y = pista.getCenterY() - GROSOR_PISTA / 2.0;

                RoundRectangle2D.Double fondo = new RoundRectangle2D.Double(
                        pista.x, y, pista.width, GROSOR_PISTA, GROSOR_PISTA, GROSOR_PISTA);
                g2.setColor(tema.panelBorde());
                g2.fill(fondo);

                double avance = thumbRect.getCenterX() - pista.x;
                if (avance > 0) {
                    g2.setColor(Tema.mezclar(Tema.ACTIVO, tema.panelBorde(), 0.85));
                    g2.fill(new RoundRectangle2D.Double(pista.x, y, avance, GROSOR_PISTA,
                            GROSOR_PISTA, GROSOR_PISTA));
                }

                g2.setFont(TipografiaApp.ETIQUETA);
                FontMetrics metrica = g2.getFontMetrics();
                g2.setColor(tema.textoSecundario());
                String texto = slider.getValue() + " ms";
                g2.drawString(texto, pista.x + (pista.width - metrica.stringWidth(texto)) / 2f,
                        pista.y + pista.height + metrica.getAscent() - 2);
            } finally {
                g2.dispose();
            }
        }

        @Override
        public void paintThumb(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                Medidas.calidad(g2);
                Tema tema = Tema.actual();
                Ellipse2D.Double pulgar = new Ellipse2D.Double(
                        thumbRect.getCenterX() - DIAMETRO_PULGAR / 2.0,
                        thumbRect.getCenterY() - DIAMETRO_PULGAR / 2.0,
                        DIAMETRO_PULGAR, DIAMETRO_PULGAR);
                g2.setColor(Tema.ACTIVO);
                g2.fill(pulgar);
                g2.setStroke(new BasicStroke(2f));
                g2.setColor(tema.panelElevado());
                g2.draw(pulgar);
            } finally {
                g2.dispose();
            }
        }
    }
}
