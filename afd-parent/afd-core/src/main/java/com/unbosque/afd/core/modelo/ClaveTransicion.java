package com.unbosque.afd.core.modelo;

import java.util.Objects;

public record ClaveTransicion(Estado estado, char simbolo) {

    public ClaveTransicion {
        Objects.requireNonNull(estado, "El estado de la clave no puede ser nulo");
    }

    @Override
    public String toString() {
        return "(" + estado.nombre() + ", " + simbolo + ")";
    }
}
