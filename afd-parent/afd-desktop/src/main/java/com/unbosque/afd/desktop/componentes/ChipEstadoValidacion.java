package com.unbosque.afd.desktop.componentes;

import com.unbosque.afd.core.modelo.ResultadoValidacion;
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

public class ChipEstadoValidacion extends JComponent implements Tema.Sensible {

    private static final int ALTO = 28;
    private static final int RELLENO = 12;
    private static final int DIAMETRO_PUNTO = 8;


    private String texto = "Sin autómata";
    private Color color = Tema.ADVERTENCIA;
    private boolean sobrevolado;

    public ChipEstadoValidacion(Runnable alPulsar) {

        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setToolTipText("Abrir la pestaña de validación");
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
            public void mouseClicked(MouseEvent evento) {
                if (alPulsar != null) {
                    alPulsar.run();
                }
            }
        });
    }

    public void actualizar(ResultadoValidacion validacion, boolean automataVacio) {
        int errores = validacion.errores().size();
        int advertencias = validacion.advertencias().size();

        if (automataVacio) {
            texto = "Lienzo vacío";
            color = Tema.actual().textoSecundario();
        } else if (errores > 0) {
            texto = errores + (errores == 1 ? " error" : " errores");
            color = Tema.RECHAZADA;
        } else if (advertencias > 0) {
            texto = advertencias + (advertencias == 1 ? " advertencia" : " advertencias");
            color = Tema.ADVERTENCIA;
        } else {
            texto = "AFD válido";
            color = Tema.ACEPTADA;
        }
        revalidate();
        repaint();
    }

    @Override
    public void aplicarTema() {
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics metrica = getFontMetrics(TipografiaApp.ETIQUETA_FUERTE);
        return new Dimension(RELLENO + DIAMETRO_PUNTO + Medidas.paso(2)
                + metrica.stringWidth(texto) + RELLENO, ALTO);
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
            RoundRectangle2D.Double pildora = new RoundRectangle2D.Double(
                    0.5, 0.5, getWidth() - 1.0, getHeight() - 1.0, ALTO, ALTO);

            g2.setColor(Tema.mezclar(color, tema.panelFondo(), sobrevolado ? 0.26 : 0.16));
            g2.fill(pildora);
            g2.setColor(Tema.mezclar(color, tema.panelFondo(), 0.55));
            g2.draw(pildora);

            g2.setColor(color);
            g2.fill(new Ellipse2D.Double(RELLENO, (getHeight() - DIAMETRO_PUNTO) / 2.0,
                    DIAMETRO_PUNTO, DIAMETRO_PUNTO));

            g2.setFont(TipografiaApp.ETIQUETA_FUERTE);
            FontMetrics metrica = g2.getFontMetrics();
            g2.setColor(tema.esOscuro() ? Tema.mezclar(color, tema.textoPrimario(), 0.45) : color.darker());
            g2.drawString(texto, RELLENO + DIAMETRO_PUNTO + Medidas.paso(2),
                    (getHeight() + metrica.getAscent() - metrica.getDescent()) / 2f);
        } finally {
            g2.dispose();
        }
    }
}
