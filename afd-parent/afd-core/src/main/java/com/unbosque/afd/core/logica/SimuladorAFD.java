package com.unbosque.afd.core.logica;

import com.unbosque.afd.core.modelo.AutomataFinitoDeterminista;
import com.unbosque.afd.core.modelo.Estado;
import com.unbosque.afd.core.modelo.MotivoRechazo;
import com.unbosque.afd.core.modelo.PasoEjecucion;
import com.unbosque.afd.core.modelo.ResultadoSimulacion;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class SimuladorAFD {

    private SimuladorAFD() {
    }

    public static ResultadoSimulacion simular(AutomataFinitoDeterminista automata, String cadena) {
        Objects.requireNonNull(automata, "El automata no puede ser nulo");
        Objects.requireNonNull(cadena, "La cadena no puede ser nula");

        List<PasoEjecucion> pasos = new ArrayList<>(cadena.length());
        Estado actual = automata.estadoInicial();
        if (actual == null) {
            return new ResultadoSimulacion(false, null, pasos, MotivoRechazo.TRANSICION_NO_DEFINIDA);
        }

        for (int indice = 0; indice < cadena.length(); indice++) {
            char simbolo = cadena.charAt(indice);
            String leida = cadena.substring(0, indice + 1);
            String restante = cadena.substring(indice + 1);

            if (!automata.alfabeto().contiene(simbolo)) {
                pasos.add(new PasoEjecucion(indice, actual, simbolo, null, leida, restante, true));
                return new ResultadoSimulacion(false, actual, pasos, MotivoRechazo.SIMBOLO_FUERA_ALFABETO);
            }

            Optional<Estado> destino = automata.transitar(actual, simbolo);
            if (destino.isEmpty()) {
                pasos.add(new PasoEjecucion(indice, actual, simbolo, null, leida, restante, true));
                return new ResultadoSimulacion(false, actual, pasos, MotivoRechazo.TRANSICION_NO_DEFINIDA);
            }

            boolean ultimo = indice == cadena.length() - 1;
            pasos.add(new PasoEjecucion(indice, actual, simbolo, destino.get(), leida, restante, ultimo));
            actual = destino.get();
        }

        boolean aceptada = automata.esDeAceptacion(actual);
        MotivoRechazo motivo = aceptada ? MotivoRechazo.NINGUNO : MotivoRechazo.ESTADO_NO_ACEPTACION;
        return new ResultadoSimulacion(aceptada, actual, pasos, motivo);
    }

    public static boolean acepta(AutomataFinitoDeterminista automata, String cadena) {
        return simular(automata, cadena).aceptada();
    }
}
