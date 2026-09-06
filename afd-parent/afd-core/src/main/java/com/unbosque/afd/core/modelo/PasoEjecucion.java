package com.unbosque.afd.core.modelo;

import java.util.Objects;

public record PasoEjecucion(
        int indice,
        Estado estadoOrigen,
        char simbolo,
        Estado estadoDestino,
        String cadenaLeida,
        String cadenaRestante,
        boolean esUltimo) {

    public PasoEjecucion {
        Objects.requireNonNull(estadoOrigen, "El estado origen del paso no puede ser nulo");
        Objects.requireNonNull(cadenaLeida, "La cadena leida no puede ser nula");
        Objects.requireNonNull(cadenaRestante, "La cadena restante no puede ser nula");
    }

    public boolean fallido() {
        return estadoDestino == null;
    }

    @Override
    public String toString() {
        String destino = estadoDestino == null ? "-" : estadoDestino.nombre();
        return indice + ": " + estadoOrigen.nombre() + " --" + simbolo + "--> " + destino;
    }
}
