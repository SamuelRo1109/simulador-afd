package com.unbosque.afd.desktop.vista;

import com.unbosque.afd.desktop.controlador.ControladorSimulacion;
import com.unbosque.afd.desktop.render.Medidas;
import com.unbosque.afd.desktop.render.Tema;
import com.unbosque.afd.desktop.render.TipografiaApp;

import javax.swing.JComponent;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Objects;

public class CintaCadena extends JComponent implements Tema.Sensible {

    private static final int ANCHO_CELDA = 40;
    private static final int ALTO_CELDA = 48;
    private static final int SEPARACION = 4;
    private static final int ALTO_CURSOR = 10;
    private static final int ALTO_TOTAL = ALTO_CELDA + ALTO_CURSOR + Medidas.paso(2);
    private static final int DURACION_TRANSICION = 120;
    private static final int PERIODO_TRANSICION = 16;

    private final ControladorSimulacion simulacion;
    private final Timer transicion;

    private double posicionCursor;
    private double objetivoCursor;
    private double origenCursor;
    private long inicioTransicion;
    private double desplazamiento;
    private String cadenaAnterior = "";

    public CintaCadena(ControladorSimulacion simulacion) {
        this.simulacion = Objects.requireNonNull(simulacion, "La simulación no puede ser nula");
        setOpaque(false);
        transicion = new Timer(PERIODO_TRANSICION, evento -> interpolar());
        simulacion.agregarObservador(this::sincronizar);
        sincronizar();
    }

    @Override
    public void aplicarTema() {
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        int celdas = Math.max(1, simulacion.cadena().length());
        return new Dimension(celdas * (ANCHO_CELDA + SEPARACION), ALTO_TOTAL);
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(ANCHO_CELDA * 2, ALTO_TOTAL);
    }

    private void sincronizar() {
        objetivoCursor = indiceResaltado();

        boolean cadenaNueva = !simulacion.cadena().equals(cadenaAnterior);
        cadenaAnterior = simulacion.cadena();
        if (cadenaNueva || !simulacion.hayResultado()) {
            transicion.stop();
            posicionCursor = objetivoCursor;
            desplazamiento = 0;
            repaint();
            return;
        }

        origenCursor = posicionCursor;
        inicioTransicion = System.currentTimeMillis();
        if (!transicion.isRunning()) {
            transicion.start();
        }
        repaint();
    }

    private void interpolar() {
        long transcurrido = System.currentTimeMillis() - inicioTransicion;
        double avance = Math.min(1.0, transcurrido / (double) DURACION_TRANSICION);
        double suavizado = avance * avance * (3 - 2 * avance);
        posicionCursor = origenCursor + (objetivoCursor - origenCursor) * suavizado;
        if (avance >= 1.0) {
            posicionCursor = objetivoCursor;
            transicion.stop();
        }
        repaint();
    }

    private int indiceResaltado() {
        return Math.max(0, simulacion.paso());
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            Medidas.calidad(g2);
            Tema tema = Tema.actual();
            String cadena = simulacion.cadena();

            if (!simulacion.hayResultado()) {
                pintarLeyenda(g2, tema, "Escribe una cadena y pulsa VALIDAR");
                return;
            }
            if (cadena.isEmpty()) {
                pintarCadenaVacia(g2, tema);
                return;
            }
            ajustarDesplazamiento(cadena.length());
            pintarCeldas(g2, tema, cadena);
            pintarCursor(g2);
        } finally {
            g2.dispose();
        }
    }

    private void ajustarDesplazamiento(int longitud) {
        int paso = ANCHO_CELDA + SEPARACION;
        int anchoTotal = longitud * paso;
        if (anchoTotal <= getWidth()) {
            desplazamiento = 0;
            return;
        }
        double centro = (indiceResaltado() + 0.5) * paso;
        desplazamiento = Math.max(0, Math.min(anchoTotal - getWidth(), centro - getWidth() / 2.0));
    }

    private void pintarCeldas(Graphics2D g2, Tema tema, String cadena) {
        int actual = simulacion.paso();
        g2.setFont(TipografiaApp.MONO.deriveFont(20f));
        FontMetrics metrica = g2.getFontMetrics();

        for (int indice = 0; indice < cadena.length(); indice++) {
            double x = indice * (ANCHO_CELDA + SEPARACION) - desplazamiento;
            if (x + ANCHO_CELDA < 0 || x > getWidth()) {
                continue;
            }
            RoundRectangle2D.Double celda = new RoundRectangle2D.Double(
                    x, 0, ANCHO_CELDA, ALTO_CELDA, Medidas.RADIO_BOTON, Medidas.RADIO_BOTON);

            Color texto;
            if (indice == actual) {
                g2.setColor(Tema.ACTIVO);
                g2.fill(celda);
                texto = Tema.TINTA_SOBRE_ACENTO;
            } else if (indice < actual) {
                g2.setColor(tema.panelFondo());
                g2.fill(celda);
                g2.setColor(tema.panelBorde());
                g2.draw(celda);
                texto = tema.textoSecundario();
            } else {
                g2.setColor(tema.panelFondo());
                g2.fill(celda);
                g2.setColor(tema.panelBorde());
                g2.draw(celda);
                texto = tema.textoPrimario();
            }

            String simbolo = String.valueOf(cadena.charAt(indice));
            g2.setColor(texto);
            g2.drawString(simbolo,
                    (float) (x + (ANCHO_CELDA - metrica.stringWidth(simbolo)) / 2.0),
                    (ALTO_CELDA + metrica.getAscent() - metrica.getDescent()) / 2f);
        }
    }

    private void pintarCursor(Graphics2D g2) {
        double centro = (posicionCursor + 0.5) * (ANCHO_CELDA + SEPARACION) - desplazamiento;
        if (centro < -ANCHO_CELDA || centro > getWidth() + ANCHO_CELDA) {
            return;
        }
        Path2D.Double triangulo = new Path2D.Double();
        triangulo.moveTo(centro, ALTO_CELDA + ALTO_CURSOR);
        triangulo.lineTo(centro - 7, ALTO_CELDA + 2);
        triangulo.lineTo(centro + 7, ALTO_CELDA + 2);
        triangulo.closePath();

        g2.setColor(Tema.ACTIVO);
        g2.fill(triangulo);
    }

    private void pintarCadenaVacia(Graphics2D g2, Tema tema) {
        g2.setFont(TipografiaApp.MONO.deriveFont(26f));
        FontMetrics metrica = g2.getFontMetrics();
        String lambda = "λ";
        double x = (getWidth() - metrica.stringWidth(lambda)) / 2.0;
        double y = (ALTO_CELDA + metrica.getAscent() - metrica.getDescent()) / 2.0;

        RoundRectangle2D.Double celda = new RoundRectangle2D.Double(
                x - Medidas.paso(4), 0, metrica.stringWidth(lambda) + Medidas.paso(8), ALTO_CELDA,
                Medidas.RADIO_BOTON, Medidas.RADIO_BOTON);
        g2.setColor(tema.panelFondo());
        g2.fill(celda);
        g2.setColor(tema.panelBorde());
        g2.draw(celda);

        g2.setColor(tema.textoPrimario());
        g2.drawString(lambda, (float) x, (float) y);

        pintarLeyenda(g2, tema, "cadena vacía");
    }

    private void pintarLeyenda(Graphics2D g2, Tema tema, String texto) {
        g2.setFont(TipografiaApp.ETIQUETA);
        FontMetrics metrica = g2.getFontMetrics();
        g2.setColor(tema.textoSecundario());
        g2.drawString(texto, (getWidth() - metrica.stringWidth(texto)) / 2f,
                ALTO_CELDA + ALTO_CURSOR + metrica.getAscent() - 2);
    }
}
