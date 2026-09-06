package com.unbosque.afd.core.logica;

import com.unbosque.afd.core.modelo.Punto;

import java.util.ArrayList;
import java.util.List;

public final class DistribuidorEstados {

    private static final double ANGULO_INICIAL = -Math.PI / 2;

    private DistribuidorEstados() {
    }

    public static List<Punto> circular(int cantidad, double centroX, double centroY, double radio) {
        if (cantidad < 0) {
            throw new IllegalArgumentException("La cantidad de estados no puede ser negativa");
        }
        if (radio < 0) {
            throw new IllegalArgumentException("El radio no puede ser negativo");
        }
        if (cantidad == 0) {
            return List.of();
        }
        if (cantidad == 1) {
            return List.of(new Punto(centroX, centroY));
        }

        List<Punto> puntos = new ArrayList<>(cantidad);
        double paso = 2 * Math.PI / cantidad;
        for (int indice = 0; indice < cantidad; indice++) {
            double angulo = ANGULO_INICIAL + paso * indice;
            puntos.add(new Punto(centroX + radio * Math.cos(angulo), centroY + radio * Math.sin(angulo)));
        }
        return List.copyOf(puntos);
    }
}
