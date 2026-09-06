package com.unbosque.afd.core.modelo;

import java.util.List;
import java.util.Objects;

public record ResultadoValidacion(List<ErrorValidacion> hallazgos) {

    public ResultadoValidacion {
        hallazgos = List.copyOf(Objects.requireNonNull(hallazgos, "Los hallazgos no pueden ser nulos"));
    }

    public static ResultadoValidacion vacio() {
        return new ResultadoValidacion(List.of());
    }

    public boolean esValido() {
        return hallazgos.stream().noneMatch(ErrorValidacion::esError);
    }

    public List<ErrorValidacion> errores() {
        return hallazgos.stream().filter(ErrorValidacion::esError).toList();
    }

    public List<ErrorValidacion> advertencias() {
        return hallazgos.stream().filter(ErrorValidacion::esAdvertencia).toList();
    }

    public boolean tieneAdvertencias() {
        return !advertencias().isEmpty();
    }

    public boolean contieneCodigo(String codigo) {
        return hallazgos.stream().anyMatch(hallazgo -> hallazgo.codigo().equals(codigo));
    }
}
