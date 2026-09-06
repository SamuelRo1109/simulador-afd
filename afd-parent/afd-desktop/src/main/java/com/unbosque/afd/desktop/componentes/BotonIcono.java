package com.unbosque.afd.desktop.componentes;

import com.unbosque.afd.desktop.render.Medidas;
import com.unbosque.afd.desktop.render.PintorIcono;
import com.unbosque.afd.desktop.render.Tema;

import javax.swing.JButton;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Objects;

public class BotonIcono extends JButton implements Tema.Sensible {

    private final int lado;
    private final int ladoIcono;

    private PintorIcono pintor;
    private boolean sobrevolado;
    private boolean destacado;

    public BotonIcono(PintorIcono pintor, String descripcion, int lado, int ladoIcono) {
        this.pintor = Objects.requireNonNull(pintor, "El pintor de icono no puede ser nulo");
        this.lado = lado;
        this.ladoIcono = ladoIcono;
        setToolTipText(descripcion);
        getAccessibleContext().setAccessibleName(descripcion);
        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setFocusable(false);
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

    public void establecerPintor(PintorIcono nuevoPintor) {
        this.pintor = Objects.requireNonNull(nuevoPintor, "El pintor de icono no puede ser nulo");
        repaint();
    }

    public void establecerDestacado(boolean nuevoDestacado) {
        this.destacado = nuevoDestacado;
        repaint();
    }

    @Override
    public void aplicarTema() {
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(lado, lado);
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

            if (destacado) {
                g2.setColor(Tema.mezclar(Tema.ACTIVO, tema.panelElevado(), 0.22));
                g2.fill(marco);
            } else if (sobrevolado && isEnabled()) {
                g2.setColor(tema.sobrevuelo());
                g2.fill(marco);
            }

            Color color;
            if (!isEnabled()) {
                color = tema.desactivado(tema.textoSecundario());
            } else if (destacado) {
                color = Tema.ACTIVO;
            } else if (sobrevolado) {
                color = tema.textoPrimario();
            } else {
                color = tema.textoSecundario();
            }

            double desplazamiento = (getWidth() - ladoIcono) / 2.0;
            double desplazamientoY = (getHeight() - ladoIcono) / 2.0;
            pintor.pintar(g2, new Rectangle2D.Double(desplazamiento, desplazamientoY, ladoIcono, ladoIcono), color);
        } finally {
            g2.dispose();
        }
    }
}
