package com.unbosque.afd.desktop.render;

import com.unbosque.afd.core.modelo.Estado;

import java.awt.Font;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.util.Objects;

public final class NodoGrafico {

    public static final Font FUENTE_BASE = new Font(Font.SANS_SERIF, Font.BOLD, 13);
    public static final double RADIO_MINIMO = 30;
    public static final double MARGEN_TEXTO = 10;
    public static final double SEPARACION_ACEPTACION = 6;

    private final Estado estado;
    private final double radio;
    private double x;
    private double y;

    public NodoGrafico(Estado estado, double x, double y) {
        this.estado = Objects.requireNonNull(estado, "El estado no puede ser nulo");
        this.x = x;
        this.y = y;
        this.radio = radioPara(estado.nombre());
    }

    public static double radioPara(String nombre) {
        return Math.max(RADIO_MINIMO, MetricasTexto.ancho(FUENTE_BASE, nombre) / 2 + MARGEN_TEXTO);
    }

    public Estado estado() {
        return estado;
    }

    public String nombre() {
        return estado.nombre();
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double radio() {
        return radio;
    }

    public double radioInterno() {
        return radio - SEPARACION_ACEPTACION;
    }

    public void mover(double nuevaX, double nuevaY) {
        this.x = nuevaX;
        this.y = nuevaY;
    }

    public Point2D.Double centro() {
        return new Point2D.Double(x, y);
    }

    public Ellipse2D.Double circulo() {
        return new Ellipse2D.Double(x - radio, y - radio, radio * 2, radio * 2);
    }

    public Ellipse2D.Double circuloInterno() {
        double interno = radioInterno();
        return new Ellipse2D.Double(x - interno, y - interno, interno * 2, interno * 2);
    }

    public boolean contiene(Point2D punto) {
        return punto != null && punto.distance(x, y) <= radio;
    }

    @Override
    public String toString() {
        return estado.nombre() + "(" + x + ", " + y + ")";
    }
}
