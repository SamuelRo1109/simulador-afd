package com.unbosque.afd.desktop.componentes;

import com.unbosque.afd.desktop.render.IconosApp;
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
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.function.Consumer;

public class ChipSimbolo extends JComponent implements Tema.Sensible {

    private static final int ALTO = 26;
    private static final int LADO_ASPA = 14;
    private static final int RELLENO = 10;

    private final char simbolo;

    private boolean sobreAspa;

    public ChipSimbolo(char simbolo, Consumer<Character> alQuitar) {
        this.simbolo = simbolo;

        setToolTipText("Quitar el símbolo '" + simbolo + "' de Σ y sus transiciones");
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        MouseAdapter interaccion = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent evento) {
                boolean nuevo = evento.getX() >= getWidth() - RELLENO - LADO_ASPA;
                if (nuevo != sobreAspa) {
                    sobreAspa = nuevo;
                    repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent evento) {
                sobreAspa = false;
                repaint();
            }

            @Override
            public void mouseClicked(MouseEvent evento) {
                if (alQuitar != null) {
                    alQuitar.accept(simbolo);
                }
            }
        };
        addMouseListener(interaccion);
        addMouseMotionListener(interaccion);
    }

    public char simbolo() {
        return simbolo;
    }

    @Override
    public void aplicarTema() {
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics metrica = getFontMetrics(TipografiaApp.MONO_FUERTE);
        int ancho = RELLENO + metrica.charWidth(simbolo) + Medidas.paso(2) + LADO_ASPA + RELLENO;
        return new Dimension(ancho, ALTO);
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            Medidas.calidad(g2);
            Tema tema = Tema.actual();
            RoundRectangle2D.Double pildora = new RoundRectangle2D.Double(
                    0.5, 0.5, getWidth() - 1.0, getHeight() - 1.0, ALTO, ALTO);

            g2.setColor(tema.panelFondo());
            g2.fill(pildora);
            g2.setColor(tema.panelBorde());
            g2.draw(pildora);

            g2.setFont(TipografiaApp.MONO_FUERTE);
            FontMetrics metrica = g2.getFontMetrics();
            g2.setColor(tema.textoPrimario());
            g2.drawString(String.valueOf(simbolo), RELLENO,
                    (getHeight() + metrica.getAscent() - metrica.getDescent()) / 2f);

            Color colorAspa = sobreAspa ? Tema.RECHAZADA : tema.textoSecundario();
            IconosApp.cerrar(g2, new Rectangle2D.Double(
                    getWidth() - RELLENO - LADO_ASPA, (getHeight() - LADO_ASPA) / 2.0,
                    LADO_ASPA, LADO_ASPA), colorAspa);
        } finally {
            g2.dispose();
        }
    }
}
