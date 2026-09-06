package com.unbosque.afd.desktop.componentes;

import com.unbosque.afd.desktop.render.Medidas;
import com.unbosque.afd.desktop.render.Tema;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.RoundRectangle2D;

public final class BarraDesplazamiento extends BasicScrollBarUI {

    private static final int GROSOR = Medidas.GROSOR_BARRA_DESPLAZAMIENTO;

    public static JScrollPane envolver(JComponent contenido) {
        JScrollPane panel = new JScrollPane(new AjustadoAlAncho(contenido));
        panel.setBorder(null);
        panel.setViewportBorder(null);
        panel.setOpaque(false);
        panel.getViewport().setOpaque(false);
        panel.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        panel.getVerticalScrollBar().setUnitIncrement(16);
        aplicar(panel);
        return panel;
    }

    public static void aplicar(JScrollPane panel) {
        configurar(panel.getVerticalScrollBar(), false);
        configurar(panel.getHorizontalScrollBar(), true);
    }

    private static void configurar(JScrollBar barra, boolean horizontal) {
        if (barra == null) {
            return;
        }
        barra.setUI(new BarraDesplazamiento());
        barra.setOpaque(false);
        barra.setUnitIncrement(16);
        barra.setPreferredSize(horizontal
                ? new Dimension(0, GROSOR + Medidas.PASO)
                : new Dimension(GROSOR + Medidas.PASO, 0));
    }

    @Override
    protected JButton createDecreaseButton(int orientacion) {
        return botonInvisible();
    }

    @Override
    protected JButton createIncreaseButton(int orientacion) {
        return botonInvisible();
    }

    private static JButton botonInvisible() {
        JButton boton = new JButton();
        boton.setPreferredSize(new Dimension(0, 0));
        boton.setMinimumSize(new Dimension(0, 0));
        boton.setMaximumSize(new Dimension(0, 0));
        boton.setFocusable(false);
        return boton;
    }

    @Override
    protected void paintTrack(Graphics g, JComponent componente, Rectangle limites) {
        // La pista queda transparente: solo el pulgar es visible.
    }

    @Override
    protected void paintThumb(Graphics g, JComponent componente, Rectangle limites) {
        if (limites.isEmpty() || !scrollbar.isEnabled()) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            Medidas.calidad(g2);
            Color color = Tema.actual().pulgarDesplazamiento();
            boolean vertical = scrollbar.getOrientation() == JScrollBar.VERTICAL;
            double x = vertical ? limites.getCenterX() - GROSOR / 2.0 : limites.x;
            double y = vertical ? limites.y : limites.getCenterY() - GROSOR / 2.0;
            double ancho = vertical ? GROSOR : limites.width;
            double alto = vertical ? limites.height : GROSOR;

            g2.setColor(color);
            g2.fill(new RoundRectangle2D.Double(x, y, ancho, alto, GROSOR, GROSOR));
        } finally {
            g2.dispose();
        }
    }

    @Override
    protected Dimension getMinimumThumbSize() {
        return new Dimension(GROSOR * 4, GROSOR * 4);
    }

    private static final class AjustadoAlAncho extends JPanel implements Scrollable {

        private AjustadoAlAncho(JComponent contenido) {
            super(new BorderLayout());
            setOpaque(false);
            add(contenido, BorderLayout.NORTH);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visible, int orientacion, int direccion) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visible, int orientacion, int direccion) {
            return orientacion == SwingConstants.VERTICAL ? visible.height : visible.width;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }
}
