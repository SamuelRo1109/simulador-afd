package com.unbosque.afd.desktop.componentes;

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
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;

public class ChipAlternable extends JComponent implements Tema.Sensible {

    private static final int LADO = 34;

    private final char simbolo;

    private boolean marcado;
    private boolean enConflicto;
    private boolean sobrevolado;

    public ChipAlternable(char simbolo, boolean marcado, Runnable alAlternar) {
        this.simbolo = simbolo;
        this.marcado = marcado;
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

            @Override
            public void mousePressed(MouseEvent evento) {
                ChipAlternable.this.marcado = !ChipAlternable.this.marcado;
                repaint();
                if (alAlternar != null) {
                    alAlternar.run();
                }
            }
        });
    }

    public char simbolo() {
        return simbolo;
    }

    public boolean marcado() {
        return marcado;
    }

    public void establecerMarcado(boolean nuevoMarcado) {
        if (marcado != nuevoMarcado) {
            marcado = nuevoMarcado;
            repaint();
        }
    }

    public boolean enConflicto() {
        return enConflicto;
    }

    public void establecerConflicto(boolean conflicto, String explicacion) {
        this.enConflicto = conflicto;
        setToolTipText(explicacion);
        repaint();
    }

    @Override
    public void aplicarTema() {
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(LADO, LADO);
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            Medidas.calidad(g2);
            Tema tema = Tema.actual();
            RoundRectangle2D.Double marco = new RoundRectangle2D.Double(
                    0.5, 0.5, getWidth() - 1.0, getHeight() - 1.0,
                    Medidas.RADIO_BOTON, Medidas.RADIO_BOTON);

            if (marcado) {
                g2.setColor(Tema.mezclar(Tema.ACTIVO, tema.panelElevado(), 0.30));
                g2.fill(marco);
                g2.setColor(Tema.ACTIVO);
            } else {
                g2.setColor(sobrevolado ? tema.sobrevuelo() : tema.panelFondo());
                g2.fill(marco);
                g2.setColor(tema.panelBorde());
            }
            g2.draw(marco);

            g2.setFont(TipografiaApp.MONO_FUERTE);
            FontMetrics metrica = g2.getFontMetrics();
            Color texto = marcado ? Tema.ACTIVO : tema.textoPrimario();
            g2.setColor(texto);
            g2.drawString(String.valueOf(simbolo),
                    (getWidth() - metrica.charWidth(simbolo)) / 2f,
                    (getHeight() + metrica.getAscent() - metrica.getDescent()) / 2f);

            if (enConflicto) {
                g2.setColor(Tema.ADVERTENCIA);
                g2.fill(new Ellipse2D.Double(getWidth() - 9, 3, 6, 6));
            }
        } finally {
            g2.dispose();
        }
    }
}
