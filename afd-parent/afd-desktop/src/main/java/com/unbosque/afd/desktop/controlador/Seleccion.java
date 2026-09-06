package com.unbosque.afd.desktop.controlador;

import java.util.Objects;

public record Seleccion(Tipo tipo, String estado, String origen, String destino) {

    public enum Tipo {
        NINGUNA,
        ESTADO,
        ARISTA
    }

    public static final Seleccion NINGUNA = new Seleccion(Tipo.NINGUNA, null, null, null);

    public static Seleccion deEstado(String estado) {
        return new Seleccion(Tipo.ESTADO, Objects.requireNonNull(estado), null, null);
    }

    public static Seleccion deArista(String origen, String destino) {
        return new Seleccion(Tipo.ARISTA, null,
                Objects.requireNonNull(origen), Objects.requireNonNull(destino));
    }

    public boolean esEstado(String nombre) {
        return tipo == Tipo.ESTADO && estado.equals(nombre);
    }

    public boolean esArista(String nombreOrigen, String nombreDestino) {
        return tipo == Tipo.ARISTA && origen.equals(nombreOrigen) && destino.equals(nombreDestino);
    }

    public boolean vacia() {
        return tipo == Tipo.NINGUNA;
    }
}
