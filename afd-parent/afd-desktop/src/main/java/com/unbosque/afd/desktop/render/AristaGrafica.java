package com.unbosque.afd.desktop.render;

import java.awt.Shape;
import java.awt.geom.Point2D;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class AristaGrafica {

    private final NodoGrafico nodoOrigen;
    private final NodoGrafico nodoDestino;
    private final List<Character> simbolos;
    private final TipoArista tipo;
    private final Shape forma;
    private final Shape punta;
    private final Point2D puntoEtiqueta;

    public AristaGrafica(NodoGrafico nodoOrigen,
                         NodoGrafico nodoDestino,
                         List<Character> simbolos,
                         TipoArista tipo,
                         Shape forma,
                         Shape punta,
                         Point2D puntoEtiqueta) {
        this.nodoOrigen = Objects.requireNonNull(nodoOrigen, "El nodo origen no puede ser nulo");
        this.nodoDestino = Objects.requireNonNull(nodoDestino, "El nodo destino no puede ser nulo");
        this.simbolos = List.copyOf(Objects.requireNonNull(simbolos, "Los simbolos no pueden ser nulos"));
        this.tipo = Objects.requireNonNull(tipo, "El tipo de arista no puede ser nulo");
        this.forma = Objects.requireNonNull(forma, "La forma no puede ser nula");
        this.punta = Objects.requireNonNull(punta, "La punta de flecha no puede ser nula");
        this.puntoEtiqueta = Objects.requireNonNull(puntoEtiqueta, "El punto de la etiqueta no puede ser nulo");
    }

    public NodoGrafico nodoOrigen() {
        return nodoOrigen;
    }

    public NodoGrafico nodoDestino() {
        return nodoDestino;
    }

    public List<Character> simbolos() {
        return simbolos;
    }

    public TipoArista tipo() {
        return tipo;
    }

    public Shape forma() {
        return forma;
    }

    public Shape punta() {
        return punta;
    }

    public Point2D puntoEtiqueta() {
        return puntoEtiqueta;
    }

    public String etiqueta() {
        return simbolos.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    public boolean conecta(String nombreOrigen, String nombreDestino) {
        return nodoOrigen.nombre().equals(nombreOrigen) && nodoDestino.nombre().equals(nombreDestino);
    }

    public boolean transporta(char simbolo) {
        return simbolos.contains(simbolo);
    }

    @Override
    public String toString() {
        return nodoOrigen.nombre() + " --" + etiqueta() + "--> " + nodoDestino.nombre() + " [" + tipo + "]";
    }
}
