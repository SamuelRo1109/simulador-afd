package com.unbosque.afd.core.modelo;

import java.util.Objects;

public record ErrorValidacion(Severidad severidad, String codigo, String mensaje) {

    public ErrorValidacion {
        Objects.requireNonNull(severidad, "La severidad no puede ser nula");
        Objects.requireNonNull(codigo, "El código no puede ser nulo");
        Objects.requireNonNull(mensaje, "El mensaje no puede ser nulo");
    }

    public static ErrorValidacion error(String codigo, String mensaje) {
        return new ErrorValidacion(Severidad.ERROR, codigo, mensaje);
    }

    public static ErrorValidacion advertencia(String codigo, String mensaje) {
        return new ErrorValidacion(Severidad.ADVERTENCIA, codigo, mensaje);
    }

    public boolean esError() {
        return severidad == Severidad.ERROR;
    }

    public boolean esAdvertencia() {
        return severidad == Severidad.ADVERTENCIA;
    }

    @Override
    public String toString() {
        return "[" + severidad + "] " + codigo + ": " + mensaje;
    }
}
