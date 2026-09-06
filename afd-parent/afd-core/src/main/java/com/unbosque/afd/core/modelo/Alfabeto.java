package com.unbosque.afd.core.modelo;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class Alfabeto {

    private final Set<Character> simbolos;

    public Alfabeto(Collection<String> simbolos) {
        Objects.requireNonNull(simbolos, "El alfabeto no puede ser nulo");
        Set<Character> acumulado = new LinkedHashSet<>();
        for (String simbolo : simbolos) {
            if (simbolo == null) {
                throw new IllegalArgumentException("El alfabeto no admite símbolos nulos");
            }
            if (simbolo.isEmpty()) {
                throw new IllegalArgumentException("El alfabeto no admite cadenas vacías");
            }
            if (simbolo.length() > 1) {
                throw new IllegalArgumentException(
                        "Cada símbolo debe tener exactamente un carácter: '" + simbolo + "'");
            }
            if (!acumulado.add(simbolo.charAt(0))) {
                throw new IllegalArgumentException("Símbolo duplicado en el alfabeto: '" + simbolo + "'");
            }
        }
        this.simbolos = Collections.unmodifiableSet(acumulado);
    }

    public static Alfabeto de(String... simbolos) {
        Objects.requireNonNull(simbolos, "El alfabeto no puede ser nulo");
        return new Alfabeto(List.of(simbolos));
    }

    public static Alfabeto deCaracteres(char... simbolos) {
        Objects.requireNonNull(simbolos, "El alfabeto no puede ser nulo");
        List<String> lista = new java.util.ArrayList<>(simbolos.length);
        for (char simbolo : simbolos) {
            lista.add(String.valueOf(simbolo));
        }
        return new Alfabeto(lista);
    }

    public Set<Character> simbolos() {
        return simbolos;
    }

    public boolean contiene(char simbolo) {
        return simbolos.contains(simbolo);
    }

    public boolean estaVacio() {
        return simbolos.isEmpty();
    }

    public int tamano() {
        return simbolos.size();
    }

    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        return otro instanceof Alfabeto alfabeto && simbolos.equals(alfabeto.simbolos);
    }

    @Override
    public int hashCode() {
        return simbolos.hashCode();
    }

    @Override
    public String toString() {
        return simbolos.stream().map(String::valueOf).collect(Collectors.joining(", ", "{", "}"));
    }
}
