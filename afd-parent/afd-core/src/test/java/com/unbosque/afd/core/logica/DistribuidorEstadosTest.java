package com.unbosque.afd.core.logica;

import com.unbosque.afd.core.modelo.Punto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DistribuidorEstadosTest {

    private static final double TOLERANCIA = 1e-9;

    @Test
    @DisplayName("El layout circular ubica cada estado sobre la circunferencia")
    void distribucionSobreLaCircunferencia() {
        List<Punto> puntos = DistribuidorEstados.circular(6, 100, 50, 40);

        assertEquals(6, puntos.size());
        for (Punto punto : puntos) {
            double distancia = Math.hypot(punto.x() - 100, punto.y() - 50);
            assertEquals(40, distancia, TOLERANCIA);
        }
    }

    @Test
    @DisplayName("El primer punto queda en la parte superior y el reparto es uniforme")
    void primerPuntoArribaYRepartoUniforme() {
        List<Punto> puntos = DistribuidorEstados.circular(4, 0, 0, 10);

        assertEquals(0, puntos.get(0).x(), TOLERANCIA);
        assertEquals(-10, puntos.get(0).y(), TOLERANCIA);
        assertEquals(10, puntos.get(1).x(), TOLERANCIA);
        assertEquals(0, puntos.get(1).y(), TOLERANCIA);
        assertEquals(0, puntos.get(2).x(), TOLERANCIA);
        assertEquals(10, puntos.get(2).y(), TOLERANCIA);
        assertEquals(-10, puntos.get(3).x(), TOLERANCIA);
        assertEquals(0, puntos.get(3).y(), TOLERANCIA);
    }

    @Test
    @DisplayName("Casos borde: cero estados, un estado y argumentos negativos")
    void casosBorde() {
        assertTrue(DistribuidorEstados.circular(0, 5, 5, 10).isEmpty());

        List<Punto> unico = DistribuidorEstados.circular(1, 5, 7, 10);
        assertEquals(1, unico.size());
        assertEquals(new Punto(5, 7), unico.get(0));

        assertThrows(IllegalArgumentException.class, () -> DistribuidorEstados.circular(-1, 0, 0, 10));
        assertThrows(IllegalArgumentException.class, () -> DistribuidorEstados.circular(3, 0, 0, -10));
    }

    @Test
    @DisplayName("La lista devuelta es inmutable")
    void listaInmutable() {
        List<Punto> puntos = DistribuidorEstados.circular(3, 0, 0, 1);
        assertThrows(UnsupportedOperationException.class, () -> puntos.add(new Punto(0, 0)));
    }
}
