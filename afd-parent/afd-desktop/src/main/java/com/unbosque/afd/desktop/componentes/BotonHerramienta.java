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

public class BotonHerramienta extends JButton implements Tema.Sensible {

    private static final int LADO_ICONO = 24;
    private static final int GROSOR_INDICADOR = 3;

    private final PintorIcono pintor;

    private boolean activo;
    private boolean sobrevolado;

    public BotonHerramienta(PintorIcono pintor, String descripcion, String atajo) {
        this.pintor = Objects.requireNonNull(pintor, "El pintor de icono no puede ser nulo");
        setToolTipText(descripcion + "   (" + atajo + ")");
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

    public void establecerActivo(boolean nuevoActivo) {
        if (activo != nuevoActivo) {
            activo = nuevoActivo;
            repaint();
        }
    }

    public boolean estaActivo() {
        return activo;
    }

    @Override
    public void aplicarTema() {
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(Medidas.LADO_HERRAMIENTA, Medidas.LADO_HERRAMIENTA);
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

            if (activo) {
                g2.setColor(Tema.mezclar(Tema.ACTIVO, tema.panelFondo(), 0.18));
                g2.fill(marco);
            } else if (sobrevolado) {
                g2.setColor(tema.sobrevuelo());
                g2.fill(marco);
            }

            if (activo) {
                g2.setColor(Tema.ACTIVO);
                g2.fillRoundRect(0, Medidas.paso(2), GROSOR_INDICADOR,
                        getHeight() - Medidas.paso(4), GROSOR_INDICADOR, GROSOR_INDICADOR);
            }

            Color color;
            if (activo) {
                color = Tema.ACTIVO;
            } else if (sobrevolado) {
                color = tema.textoPrimario();
            } else {
                color = tema.textoSecundario();
            }

            double margen = (getWidth() - LADO_ICONO) / 2.0;
            pintor.pintar(g2, new Rectangle2D.Double(margen, margen, LADO_ICONO, LADO_ICONO), color);
        } finally {
            g2.dispose();
        }
    }
}
