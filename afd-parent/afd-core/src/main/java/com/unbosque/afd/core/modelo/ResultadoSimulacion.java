package com.unbosque.afd.core.modelo;

import java.util.List;
import java.util.Objects;

public record ResultadoSimulacion(
        boolean aceptada,
        Estado estadoFinal,
        List<PasoEjecucion> pasos,
        MotivoRechazo motivoRechazo) {

    public ResultadoSimulacion {
        Objects.requireNonNull(motivoRechazo, "El motivo de rechazo no puede ser nulo");
        pasos = List.copyOf(Objects.requireNonNull(pasos, "La traza no puede ser nula"));
    }

    public int cantidadPasos() {
        return pasos.size();
    }

    public boolean rechazada() {
        return !aceptada;
    }
}
