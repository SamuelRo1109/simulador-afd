package com.unbosque.afd.core.modelo;

import java.util.Objects;

public record Transicion(Estado estadoOrigen, char simbolo, Estado estadoDestino) {

    public Transicion {
        Objects.requireNonNull(estadoOrigen, "El estado origen no puede ser nulo");
        Objects.requireNonNull(estadoDestino, "El estado destino no puede ser nulo");
    }

    public ClaveTransicion clave() {
        return new ClaveTransicion(estadoOrigen, simbolo);
    }

    @Override
    public String toString() {
        return estadoOrigen.nombre() + " --" + simbolo + "--> " + estadoDestino.nombre();
    }
}
