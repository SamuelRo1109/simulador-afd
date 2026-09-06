package com.unbosque.afd.desktop.componentes;

import com.unbosque.afd.desktop.render.Medidas;
import com.unbosque.afd.desktop.render.Tema;
import com.unbosque.afd.desktop.render.TipografiaApp;

import javax.swing.BorderFactory;
import javax.swing.JTextField;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.geom.RoundRectangle2D;

public class CampoTexto extends JTextField implements Tema.Sensible {

    private static final int ALTO = 32;

    private final String marcador;

    private Color colorAviso;

    public CampoTexto(String marcador, int columnas) {
        super(columnas);
        this.marcador = marcador == null ? "" : marcador;
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(0, Medidas.paso(3), 0, Medidas.paso(3)));
        setFont(TipografiaApp.CUERPO);
        setCaretColor(Tema.ACTIVO);
        aplicarTema();
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent evento) {
                repaint();
            }

            @Override
            public void focusLost(FocusEvent evento) {
                repaint();
            }
        });
    }

    public void establecerAviso(Color color) {
        this.colorAviso = color;
        repaint();
    }

    @Override
    public void aplicarTema() {
        Tema tema = Tema.actual();
        setForeground(tema.textoPrimario());
        setSelectionColor(Tema.mezclar(Tema.ACTIVO, tema.panelFondo(), 0.35));
        setSelectedTextColor(tema.textoPrimario());
        setDisabledTextColor(tema.textoSecundario());
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension preferido = super.getPreferredSize();
        return new Dimension(preferido.width, ALTO);
    }

    @Override
    public Dimension getMaximumSize() {
        return isMaximumSizeSet() ? super.getMaximumSize() : new Dimension(Integer.MAX_VALUE, ALTO);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            Medidas.calidad(g2);
            Tema tema = Tema.actual();
            RoundRectangle2D.Double marco = new RoundRectangle2D.Double(
                    0.75, 0.75, getWidth() - 1.5, getHeight() - 1.5,
                    Medidas.RADIO_BOTON, Medidas.RADIO_BOTON);

            g2.setColor(isEnabled() ? tema.panelFondo() : Tema.mezclar(tema.panelFondo(), tema.panelElevado(), 0.5));
            g2.fill(marco);

            Color borde;
            if (colorAviso != null) {
                borde = colorAviso;
            } else if (isFocusOwner()) {
                borde = Tema.ACTIVO;
            } else {
                borde = tema.panelBorde();
            }
            g2.setStroke(new BasicStroke(isFocusOwner() || colorAviso != null ? 1.6f : 1f));
            g2.setColor(borde);
            g2.draw(marco);

            if (getText().isEmpty() && !marcador.isEmpty()) {
                g2.setFont(getFont());
                FontMetrics metrica = g2.getFontMetrics();
                g2.setColor(Tema.mezclar(tema.textoSecundario(), tema.panelFondo(), 0.75));
                g2.drawString(marcador, getInsets().left,
                        (getHeight() + metrica.getAscent() - metrica.getDescent()) / 2f);
            }
        } finally {
            g2.dispose();
        }
        super.paintComponent(g);
    }
}
