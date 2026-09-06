package com.unbosque.afd.desktop.componentes;

import com.unbosque.afd.core.modelo.ErrorValidacion;
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
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TarjetaHallazgo extends JComponent implements Tema.Sensible {

    private static final int RELLENO = 10;
    private static final int ANCHO_CODIGO = 34;

    private final ErrorValidacion hallazgo;
    private final String codigoCorto;
    private final boolean navegable;

    private boolean sobrevolado;
    private int altoCalculado = 48;

    public TarjetaHallazgo(ErrorValidacion hallazgo, boolean navegable, Runnable alPulsar) {
        this.hallazgo = Objects.requireNonNull(hallazgo, "El hallazgo no puede ser nulo");
        this.codigoCorto = codigoCorto(hallazgo);
        this.navegable = navegable;
        setToolTipText(navegable
                ? "Clic para seleccionar el elemento afectado en el lienzo"
                : hallazgo.mensaje());
        setCursor(Cursor.getPredefinedCursor(navegable ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
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
                if (navegable && alPulsar != null) {
                    alPulsar.run();
                }
            }
        });
    }

    public ErrorValidacion hallazgo() {
        return hallazgo;
    }

    public static String codigoCorto(ErrorValidacion hallazgo) {
        String codigo = hallazgo.codigo();
        int separador = codigo.indexOf('_');
        return separador < 0 ? codigo : codigo.substring(0, separador);
    }

    @Override
    public void aplicarTema() {
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(0, altoCalculado);
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, altoCalculado);
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(0, altoCalculado);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            Medidas.calidad(g2);
            Tema tema = Tema.actual();
            Color acento = hallazgo.esError() ? Tema.RECHAZADA : Tema.ADVERTENCIA;

            g2.setFont(TipografiaApp.ETIQUETA);
            FontMetrics metrica = g2.getFontMetrics();
            int anchoTexto = Math.max(40, getWidth() - RELLENO * 2 - ANCHO_CODIGO);
            List<String> lineas = repartir(hallazgo.mensaje(), metrica, anchoTexto);

            int alto = RELLENO * 2 + lineas.size() * metrica.getHeight();
            if (alto != altoCalculado) {
                altoCalculado = alto;
                revalidate();
            }

            RoundRectangle2D.Double marco = new RoundRectangle2D.Double(
                    0.5, 0.5, getWidth() - 1.0, alto - 1.0,
                    Medidas.RADIO_BOTON, Medidas.RADIO_BOTON);
            g2.setColor(Tema.mezclar(acento, tema.panelFondo(), sobrevolado && navegable ? 0.18 : 0.09));
            g2.fill(marco);
            g2.setColor(Tema.mezclar(acento, tema.panelFondo(), 0.40));
            g2.draw(marco);

            g2.setColor(acento);
            g2.fillRoundRect(0, 4, 3, alto - 8, 3, 3);

            g2.setFont(TipografiaApp.ETIQUETA_FUERTE);
            g2.setColor(acento);
            g2.drawString(codigoCorto, RELLENO, RELLENO + metrica.getAscent());

            g2.setFont(TipografiaApp.ETIQUETA);
            g2.setColor(tema.textoPrimario());
            int y = RELLENO + metrica.getAscent();
            for (String linea : lineas) {
                g2.drawString(linea, RELLENO + ANCHO_CODIGO, y);
                y += metrica.getHeight();
            }
        } finally {
            g2.dispose();
        }
    }

    private static List<String> repartir(String texto, FontMetrics metrica, int anchoMaximo) {
        List<String> lineas = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        for (String palabra : texto.split(" ")) {
            String candidato = actual.isEmpty() ? palabra : actual + " " + palabra;
            if (metrica.stringWidth(candidato) > anchoMaximo && !actual.isEmpty()) {
                lineas.add(actual.toString());
                actual = new StringBuilder(palabra);
            } else {
                actual = new StringBuilder(candidato);
            }
        }
        if (!actual.isEmpty()) {
            lineas.add(actual.toString());
        }
        return lineas.isEmpty() ? List.of(texto) : lineas;
    }
}
