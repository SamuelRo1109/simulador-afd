package com.unbosque.afd.desktop.componentes;

import com.unbosque.afd.desktop.render.Medidas;
import com.unbosque.afd.desktop.render.Tema;
import com.unbosque.afd.desktop.render.TipografiaApp;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;

public class PanelTarjeta extends JPanel implements Tema.Sensible {

    private final String titulo;
    private boolean elevado = true;

    public PanelTarjeta() {
        this(null);
    }

    public PanelTarjeta(String titulo) {
        this.titulo = titulo;
        setOpaque(false);
        int superior = titulo == null ? Medidas.paso(3) : Medidas.paso(8);
        setBorder(BorderFactory.createEmptyBorder(superior, Medidas.paso(3), Medidas.paso(3), Medidas.paso(3)));
    }

    public void establecerElevado(boolean nuevoElevado) {
        this.elevado = nuevoElevado;
        repaint();
    }

    @Override
    public void aplicarTema() {
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            Medidas.calidad(g2);
            Tema tema = Tema.actual();
            double radio = Medidas.RADIO_TARJETA;
            RoundRectangle2D.Double marco = new RoundRectangle2D.Double(
                    0.5, 0.5, getWidth() - 1.0, getHeight() - 1.0, radio, radio);

            g2.setColor(elevado ? tema.panelElevado() : tema.panelFondo());
            g2.fill(marco);

            g2.setStroke(new BasicStroke(1f));
            g2.setColor(tema.panelBorde());
            g2.draw(marco);

            if (elevado) {
                g2.setColor(tema.luzSuperior());
                g2.drawLine((int) radio, 1, (int) (getWidth() - radio), 1);
                g2.setColor(tema.sombraInferior());
                g2.drawLine((int) radio, getHeight() - 1, (int) (getWidth() - radio), getHeight() - 1);
            }

            if (titulo != null) {
                g2.setFont(TipografiaApp.ETIQUETA_FUERTE);
                g2.setColor(tema.textoSecundario());
                g2.drawString(titulo.toUpperCase(), Medidas.paso(3), Medidas.paso(5));
            }
        } finally {
            g2.dispose();
        }
        super.paintComponent(g);
    }
}
