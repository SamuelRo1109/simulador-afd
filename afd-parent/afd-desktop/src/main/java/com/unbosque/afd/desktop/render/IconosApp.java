package com.unbosque.afd.desktop.render;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

public final class IconosApp {

    private static final double LADO = 24;

    private IconosApp() {
    }

    public static void seleccionar(Graphics2D g2, Rectangle2D area, Color color) {
        Path2D.Double flecha = new Path2D.Double();
        flecha.moveTo(7, 4);
        flecha.lineTo(18, 12.4);
        flecha.lineTo(12.6, 13.2);
        flecha.lineTo(15.6, 19.2);
        flecha.lineTo(13.1, 20.4);
        flecha.lineTo(10.2, 14.4);
        flecha.lineTo(6.6, 18.2);
        flecha.closePath();
        rellenar(g2, area, color, flecha);
    }

    public static void estado(Graphics2D g2, Rectangle2D area, Color color) {
        trazar(g2, area, color, 1.8, new Ellipse2D.Double(4.5, 4.5, 15, 15));
    }

    public static void aceptacion(Graphics2D g2, Rectangle2D area, Color color) {
        Path2D.Double doble = new Path2D.Double();
        doble.append(new Ellipse2D.Double(3.5, 3.5, 17, 17), false);
        doble.append(new Ellipse2D.Double(6.5, 6.5, 11, 11), false);
        trazar(g2, area, color, 1.8, doble);
    }

    public static void inicial(Graphics2D g2, Rectangle2D area, Color color) {
        Path2D.Double chevron = new Path2D.Double();
        chevron.moveTo(3, 12);
        chevron.lineTo(13, 12);
        chevron.moveTo(9, 6.5);
        chevron.lineTo(15, 12);
        chevron.lineTo(9, 17.5);
        chevron.append(new Ellipse2D.Double(15.5, 8.5, 7, 7), false);
        trazar(g2, area, color, 1.8, chevron);
    }

    public static void transicion(Graphics2D g2, Rectangle2D area, Color color) {
        Path2D.Double curva = new Path2D.Double();
        curva.moveTo(3.5, 17);
        curva.curveTo(7, 6, 15, 5, 19.5, 9.5);
        trazar(g2, area, color, 1.8, curva);
        Path2D.Double punta = new Path2D.Double();
        punta.moveTo(20.8, 10.8);
        punta.lineTo(14.4, 9.8);
        punta.lineTo(18.4, 4.8);
        punta.closePath();
        rellenar(g2, area, color, punta);
    }

    public static void borrar(Graphics2D g2, Rectangle2D area, Color color) {
        Path2D.Double aspa = new Path2D.Double();
        aspa.moveTo(6, 6);
        aspa.lineTo(18, 18);
        aspa.moveTo(18, 6);
        aspa.lineTo(6, 18);
        trazar(g2, area, color, 2.2, aspa);
    }

    public static void mano(Graphics2D g2, Rectangle2D area, Color color) {
        Path2D.Double palma = new Path2D.Double();
        palma.moveTo(8, 13);
        palma.lineTo(8, 6.5);
        palma.moveTo(11.3, 12);
        palma.lineTo(11.3, 5);
        palma.moveTo(14.6, 12);
        palma.lineTo(14.6, 6);
        palma.moveTo(17.6, 12.5);
        palma.lineTo(17.6, 8.5);
        palma.moveTo(6.2, 12.6);
        palma.curveTo(4.6, 14.4, 5.4, 16.6, 7.2, 18);
        palma.lineTo(11, 20.4);
        palma.lineTo(15.4, 20.4);
        palma.curveTo(17.4, 19.6, 17.9, 17.6, 17.9, 15);
        trazar(g2, area, color, 1.7, palma);
    }

    public static void ajustar(Graphics2D g2, Rectangle2D area, Color color) {
        Path2D.Double esquinas = new Path2D.Double();
        esquinas.moveTo(4, 9);
        esquinas.lineTo(4, 4);
        esquinas.lineTo(9, 4);
        esquinas.moveTo(15, 4);
        esquinas.lineTo(20, 4);
        esquinas.lineTo(20, 9);
        esquinas.moveTo(20, 15);
        esquinas.lineTo(20, 20);
        esquinas.lineTo(15, 20);
        esquinas.moveTo(9, 20);
        esquinas.lineTo(4, 20);
        esquinas.lineTo(4, 15);
        trazar(g2, area, color, 1.9, esquinas);
        trazar(g2, area, color, 1.5, new Ellipse2D.Double(9.5, 9.5, 5, 5));
    }

    public static void acercar(Graphics2D g2, Rectangle2D area, Color color) {
        lupa(g2, area, color, true);
    }

    public static void alejar(Graphics2D g2, Rectangle2D area, Color color) {
        lupa(g2, area, color, false);
    }

    private static void lupa(Graphics2D g2, Rectangle2D area, Color color, boolean mas) {
        Path2D.Double forma = new Path2D.Double();
        forma.append(new Ellipse2D.Double(4, 4, 13, 13), false);
        forma.moveTo(16.2, 16.2);
        forma.lineTo(20.5, 20.5);
        forma.moveTo(7.5, 10.5);
        forma.lineTo(13.5, 10.5);
        if (mas) {
            forma.moveTo(10.5, 7.5);
            forma.lineTo(10.5, 13.5);
        }
        trazar(g2, area, color, 1.9, forma);
    }

    public static void reorganizar(Graphics2D g2, Rectangle2D area, Color color) {
        trazar(g2, area, color, 1.4, new Ellipse2D.Double(5, 5, 14, 14));
        Path2D.Double puntos = new Path2D.Double();
        puntos.append(new Ellipse2D.Double(9.6, 2.4, 4.8, 4.8), false);
        puntos.append(new Ellipse2D.Double(16.8, 14.4, 4.8, 4.8), false);
        puntos.append(new Ellipse2D.Double(2.4, 14.4, 4.8, 4.8), false);
        rellenar(g2, area, color, puntos);
    }

    public static void matriz(Graphics2D g2, Rectangle2D area, Color color) {
        Path2D.Double reja = new Path2D.Double();
        reja.append(new RoundRectangle2D.Double(3.5, 3.5, 17, 17, 3, 3), false);
        reja.moveTo(3.5, 9.2);
        reja.lineTo(20.5, 9.2);
        reja.moveTo(3.5, 14.8);
        reja.lineTo(20.5, 14.8);
        reja.moveTo(9.2, 3.5);
        reja.lineTo(9.2, 20.5);
        reja.moveTo(14.8, 3.5);
        reja.lineTo(14.8, 20.5);
        trazar(g2, area, color, 1.5, reja);
    }

    public static void exportar(Graphics2D g2, Rectangle2D area, Color color) {
        Path2D.Double forma = new Path2D.Double();
        forma.moveTo(12, 3.5);
        forma.lineTo(12, 14.8);
        forma.moveTo(7.5, 10.2);
        forma.lineTo(12, 14.8);
        forma.lineTo(16.5, 10.2);
        forma.moveTo(4.5, 17);
        forma.lineTo(4.5, 20.5);
        forma.lineTo(19.5, 20.5);
        forma.lineTo(19.5, 17);
        trazar(g2, area, color, 1.9, forma);
    }

    public static void tema(Graphics2D g2, Rectangle2D area, Color color) {
        trazar(g2, area, color, 1.8, new Ellipse2D.Double(4.5, 4.5, 15, 15));
        Path2D.Double mitad = new Path2D.Double();
        mitad.moveTo(12, 4.5);
        mitad.curveTo(16.2, 4.5, 19.5, 7.8, 19.5, 12);
        mitad.curveTo(19.5, 16.2, 16.2, 19.5, 12, 19.5);
        mitad.closePath();
        rellenar(g2, area, color, mitad);
    }

    public static void inicio(Graphics2D g2, Rectangle2D area, Color color) {
        rellenar(g2, area, color, new Rectangle2D.Double(6, 6, 2.2, 12));
        Path2D.Double triangulo = new Path2D.Double();
        triangulo.moveTo(18.5, 6);
        triangulo.lineTo(18.5, 18);
        triangulo.lineTo(9.6, 12);
        triangulo.closePath();
        rellenar(g2, area, color, triangulo);
    }

    public static void anterior(Graphics2D g2, Rectangle2D area, Color color) {
        Path2D.Double chevron = new Path2D.Double();
        chevron.moveTo(15, 5.5);
        chevron.lineTo(8.5, 12);
        chevron.lineTo(15, 18.5);
        trazar(g2, area, color, 2.2, chevron);
    }

    public static void siguiente(Graphics2D g2, Rectangle2D area, Color color) {
        Path2D.Double chevron = new Path2D.Double();
        chevron.moveTo(9, 5.5);
        chevron.lineTo(15.5, 12);
        chevron.lineTo(9, 18.5);
        trazar(g2, area, color, 2.2, chevron);
    }

    public static void fin(Graphics2D g2, Rectangle2D area, Color color) {
        Path2D.Double triangulo = new Path2D.Double();
        triangulo.moveTo(5.5, 6);
        triangulo.lineTo(5.5, 18);
        triangulo.lineTo(14.4, 12);
        triangulo.closePath();
        rellenar(g2, area, color, triangulo);
        rellenar(g2, area, color, new Rectangle2D.Double(15.8, 6, 2.2, 12));
    }

    public static void reproducir(Graphics2D g2, Rectangle2D area, Color color) {
        Path2D.Double triangulo = new Path2D.Double();
        triangulo.moveTo(7.5, 4.8);
        triangulo.lineTo(7.5, 19.2);
        triangulo.lineTo(19, 12);
        triangulo.closePath();
        rellenar(g2, area, color, triangulo);
    }

    public static void pausar(Graphics2D g2, Rectangle2D area, Color color) {
        Path2D.Double barras = new Path2D.Double();
        barras.append(new Rectangle2D.Double(7.2, 5, 3.4, 14), false);
        barras.append(new Rectangle2D.Double(13.4, 5, 3.4, 14), false);
        rellenar(g2, area, color, barras);
    }

    public static void reiniciar(Graphics2D g2, Rectangle2D area, Color color) {
        Path2D.Double arco = new Path2D.Double();
        arco.moveTo(19, 8.5);
        arco.curveTo(17, 4.6, 12.4, 3, 8.6, 4.9);
        arco.curveTo(4.2, 7.1, 2.8, 12.7, 5.6, 16.8);
        arco.curveTo(8.3, 20.8, 14, 21.4, 17.6, 18.2);
        trazar(g2, area, color, 2.0, arco);
        Path2D.Double punta = new Path2D.Double();
        punta.moveTo(20.4, 10.4);
        punta.lineTo(13.8, 9.4);
        punta.lineTo(18.4, 4.4);
        punta.closePath();
        rellenar(g2, area, color, punta);
    }

    public static void cerrar(Graphics2D g2, Rectangle2D area, Color color) {
        Path2D.Double aspa = new Path2D.Double();
        aspa.moveTo(7.5, 7.5);
        aspa.lineTo(16.5, 16.5);
        aspa.moveTo(16.5, 7.5);
        aspa.lineTo(7.5, 16.5);
        trazar(g2, area, color, 1.9, aspa);
    }

    public static void minimizar(Graphics2D g2, Rectangle2D area, Color color) {
        Path2D.Double raya = new Path2D.Double();
        raya.moveTo(7, 12);
        raya.lineTo(17, 12);
        trazar(g2, area, color, 1.9, raya);
    }

    public static void basura(Graphics2D g2, Rectangle2D area, Color color) {
        Path2D.Double forma = new Path2D.Double();
        forma.moveTo(4.5, 7);
        forma.lineTo(19.5, 7);
        forma.moveTo(9.5, 7);
        forma.lineTo(9.5, 4.5);
        forma.lineTo(14.5, 4.5);
        forma.lineTo(14.5, 7);
        forma.moveTo(6.5, 7);
        forma.lineTo(7.6, 20);
        forma.lineTo(16.4, 20);
        forma.lineTo(17.5, 7);
        forma.moveTo(10.4, 10.5);
        forma.lineTo(10.8, 16.8);
        forma.moveTo(13.6, 10.5);
        forma.lineTo(13.2, 16.8);
        trazar(g2, area, color, 1.6, forma);
    }

    public static void copiar(Graphics2D g2, Rectangle2D area, Color color) {
        Path2D.Double forma = new Path2D.Double();
        forma.append(new RoundRectangle2D.Double(4, 4, 12, 12, 2.5, 2.5), false);
        forma.append(new RoundRectangle2D.Double(8, 8, 12, 12, 2.5, 2.5), false);
        trazar(g2, area, color, 1.6, forma);
    }

    public static void trampa(Graphics2D g2, Rectangle2D area, Color color) {
        Path2D.Double forma = new Path2D.Double();
        forma.append(new Ellipse2D.Double(5, 5, 14, 14), false);
        forma.moveTo(12, 8.5);
        forma.lineTo(12, 15.5);
        forma.moveTo(8.5, 12);
        forma.lineTo(15.5, 12);
        trazar(g2, area, color, 1.8, forma);
    }

    private static void trazar(Graphics2D g2, Rectangle2D area, Color color, double grosor, Shape forma) {
        Graphics2D local = preparar(g2, area, color);
        try {
            local.setStroke(new BasicStroke((float) grosor, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            local.draw(forma);
        } finally {
            local.dispose();
        }
    }

    private static void rellenar(Graphics2D g2, Rectangle2D area, Color color, Shape forma) {
        Graphics2D local = preparar(g2, area, color);
        try {
            local.fill(forma);
        } finally {
            local.dispose();
        }
    }

    private static Graphics2D preparar(Graphics2D g2, Rectangle2D area, Color color) {
        Graphics2D local = (Graphics2D) g2.create();
        local.setColor(color);
        local.transform(new AffineTransform(
                area.getWidth() / LADO, 0, 0, area.getHeight() / LADO, area.getX(), area.getY()));
        return local;
    }
}
