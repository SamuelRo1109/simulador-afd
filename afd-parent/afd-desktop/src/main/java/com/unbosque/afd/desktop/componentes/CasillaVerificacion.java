package com.unbosque.afd.desktop.componentes;

import com.unbosque.afd.desktop.render.Medidas;
import com.unbosque.afd.desktop.render.Tema;
import com.unbosque.afd.desktop.render.TipografiaApp;

import javax.swing.JComponent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.function.Consumer;

public class CasillaVerificacion extends JComponent implements Tema.Sensible {

    private static final int LADO = 16;
    private static final int ALTO = 24;

    private final String etiqueta;
    private final Consumer<Boolean> alCambiar;

    private boolean marcada;
    private boolean sobrevolada;
    private Color acento = Tema.ACTIVO;

    public CasillaVerificacion(String etiqueta, boolean marcada, Consumer<Boolean> alCambiar) {
        this.etiqueta = etiqueta;
        this.marcada = marcada;
        this.alCambiar = alCambiar;
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent evento) {
                sobrevolada = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent evento) {
                sobrevolada = false;
                repaint();
            }

            @Override
            public void mousePressed(MouseEvent evento) {
                if (!isEnabled()) {
                    return;
                }
                alternar();
            }
        });
    }

    public void establecerAcento(Color nuevoAcento) {
        this.acento = nuevoAcento;
        repaint();
    }

    public void alternar() {
        marcada = !marcada;
        repaint();
        if (alCambiar != null) {
            alCambiar.accept(marcada);
        }
    }

    public boolean marcada() {
        return marcada;
    }

    @Override
    public void aplicarTema() {
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics metrica = getFontMetrics(TipografiaApp.CUERPO);
        return new Dimension(LADO + Medidas.paso(2) + metrica.stringWidth(etiqueta), ALTO);
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, ALTO);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            Medidas.calidad(g2);
            Tema tema = Tema.actual();
            double y = (getHeight() - LADO) / 2.0;
            RoundRectangle2D.Double caja = new RoundRectangle2D.Double(0.5, y, LADO, LADO, 4, 4);
            Color acento = isEnabled() ? this.acento : tema.desactivado(this.acento);

            if (marcada) {
                g2.setColor(acento);
                g2.fill(caja);
                g2.setColor(tema.lienzoFondo());
                Path2D.Double marca = new Path2D.Double();
                marca.moveTo(4, y + 8.5);
                marca.lineTo(7, y + 11.5);
                marca.lineTo(12.5, y + 5);
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(marca);
            } else {
                g2.setColor(sobrevolada ? tema.sobrevuelo() : tema.panelFondo());
                g2.fill(caja);
                g2.setStroke(new BasicStroke(1.2f));
                g2.setColor(sobrevolada ? acento : tema.panelBorde());
                g2.draw(caja);
            }

            g2.setFont(TipografiaApp.CUERPO);
            FontMetrics metrica = g2.getFontMetrics();
            g2.setColor(isEnabled() ? tema.textoPrimario() : tema.desactivado(tema.textoSecundario()));
            g2.drawString(etiqueta, LADO + Medidas.paso(2),
                    (getHeight() + metrica.getAscent() - metrica.getDescent()) / 2f);
        } finally {
            g2.dispose();
        }
    }
}
