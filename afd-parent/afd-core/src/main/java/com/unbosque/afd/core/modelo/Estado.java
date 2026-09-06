package com.unbosque.afd.core.modelo;

import java.util.Objects;

public final class Estado {

    private final String nombre;
    private final boolean esInicial;
    private final boolean esAceptacion;

    public Estado(String nombre, boolean esInicial, boolean esAceptacion) {
        this.nombre = Objects.requireNonNull(nombre, "El nombre del estado no puede ser nulo");
        this.esInicial = esInicial;
        this.esAceptacion = esAceptacion;
    }

    public static Estado de(String nombre) {
        return new Estado(nombre, false, false);
    }

    public static Estado inicial(String nombre) {
        return new Estado(nombre, true, false);
    }

    public static Estado aceptacion(String nombre) {
        return new Estado(nombre, false, true);
    }

    public static Estado inicialYAceptacion(String nombre) {
        return new Estado(nombre, true, true);
    }

    public String nombre() {
        return nombre;
    }

    public boolean esInicial() {
        return esInicial;
    }

    public boolean esAceptacion() {
        return esAceptacion;
    }

    public Estado conInicial(boolean nuevoValor) {
        return new Estado(nombre, nuevoValor, esAceptacion);
    }

    public Estado conAceptacion(boolean nuevoValor) {
        return new Estado(nombre, esInicial, nuevoValor);
    }

    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        return otro instanceof Estado estado && nombre.equals(estado.nombre);
    }

    @Override
    public int hashCode() {
        return nombre.hashCode();
    }

    @Override
    public String toString() {
        return nombre;
    }
}
