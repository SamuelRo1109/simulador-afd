package com.unbosque.afd.desktop.render;

import com.unbosque.afd.core.logica.DistribuidorEstados;
import com.unbosque.afd.core.modelo.Punto;

import java.util.List;

public final class DistribuidorLienzo {

    public static final double SEPARACION_HORIZONTAL = 260;

    private static final double ALTURA_TRIANGULO = SEPARACION_HORIZONTAL * Math.sqrt(3) / 2;

    private DistribuidorLienzo() {
    }

    public static List<Punto> distribuir(int cantidad, double centroX, double centroY, double radio) {
        if (cantidad < 0) {
            throw new IllegalArgumentException("La cantidad de estados no puede ser negativa");
        }
        return switch (cantidad) {
            case 0 -> List.of();
            case 1 -> List.of(new Punto(centroX, centroY));
            case 2 -> enLinea(centroX, centroY);
            case 3 -> enTriangulo(centroX, centroY);
            default -> DistribuidorEstados.circular(cantidad, centroX, centroY, radio);
        };
    }

    private static List<Punto> enLinea(double centroX, double centroY) {
        double mitad = SEPARACION_HORIZONTAL / 2;
        return List.of(
                new Punto(centroX - mitad, centroY),
                new Punto(centroX + mitad, centroY));
    }

    private static List<Punto> enTriangulo(double centroX, double centroY) {
        double mitad = SEPARACION_HORIZONTAL / 2;
        double cima = ALTURA_TRIANGULO * 2 / 3;
        double base = ALTURA_TRIANGULO / 3;
        return List.of(
                new Punto(centroX - mitad, centroY + base),
                new Punto(centroX, centroY - cima),
                new Punto(centroX + mitad, centroY + base));
    }
}
