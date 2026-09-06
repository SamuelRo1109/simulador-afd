package com.unbosque.afd.desktop.componentes;

import com.unbosque.afd.desktop.render.Medidas;
import com.unbosque.afd.desktop.render.Tema;
import com.unbosque.afd.desktop.render.TipografiaApp;

import javax.swing.JButton;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

public class BotonAccion extends JButton implements Tema.Sensible {

    public enum Estilo {
        PRIMARIO,
        SECUNDARIO,
        PLANO
    }

    private static final int ALTO = 32;
    private static final int RELLENO_HORIZONTAL = 16;

    private final Estilo estilo;
    private boolean sobrevolado;

    public BotonAccion(String texto, Estilo estilo) {
        super(texto);
        this.estilo = estilo;
        setFont(TipografiaApp.CUERPO_FUERTE);
        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent evento) {
                sobrevolado = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent evento) {
                sobrevolado = false;
                repaint();
            }
        });
    }

    @Override
    public void aplicarTema() {
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics metrica = getFontMetrics(getFont());
        return new Dimension(metrica.stringWidth(getText()) + RELLENO_HORIZONTAL * 2, ALTO);
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    public Dimension getMaximumSize() {
        return isMaximumSizeSet() ? super.getMaximumSize() : getPreferredSize();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            Medidas.calidad(g2);
            Tema tema = Tema.actual();
            boolean activo = isEnabled();
            RoundRectangle2D.Double marco = new RoundRectangle2D.Double(
                    0.5, 0.5, getWidth() - 1.0, getHeight() - 1.0,
                    Medidas.RADIO_BOTON, Medidas.RADIO_BOTON);

            Color texto;
            if (estilo == Estilo.PRIMARIO) {
                Color relleno = activo ? Tema.ACTIVO : tema.superficieDesactivada();
                if (sobrevolado && activo) {
                    relleno = Tema.aclarar(relleno, 0.12);
                }
                g2.setColor(relleno);
                g2.fill(marco);
                texto = activo ? Tema.TINTA_SOBRE_ACENTO : tema.desactivado(tema.textoSecundario());
            } else if (estilo == Estilo.SECUNDARIO) {
                if (sobrevolado && activo) {
                    g2.setColor(tema.sobrevuelo());
                    g2.fill(marco);
                }
                g2.setStroke(new BasicStroke(1f));
                g2.setColor(activo ? tema.panelBorde() : tema.superficieDesactivada());
                g2.draw(marco);
                texto = activo ? tema.textoPrimario() : tema.desactivado(tema.textoSecundario());
            } else {
                if (sobrevolado && activo) {
                    g2.setColor(tema.sobrevuelo());
                    g2.fill(marco);
                }
                texto = activo ? tema.textoSecundario() : tema.desactivado(tema.textoSecundario());
            }

            g2.setFont(getFont());
            FontMetrics metrica = g2.getFontMetrics();
            g2.setColor(texto);
            g2.drawString(getText(),
                    (getWidth() - metrica.stringWidth(getText())) / 2f,
                    (getHeight() + metrica.getAscent() - metrica.getDescent()) / 2f);
        } finally {
            g2.dispose();
        }
    }
}
