package com.unbosque.afd.core.modelo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class FuncionTransicion {

    private final List<Transicion> transiciones;
    private final Map<ClaveTransicion, Estado> delta;

    public FuncionTransicion(Collection<Transicion> transiciones) {
        Objects.requireNonNull(transiciones, "Las transiciones no pueden ser nulas");
        List<Transicion> copia = new ArrayList<>();
        Map<ClaveTransicion, Estado> mapa = new LinkedHashMap<>();
        for (Transicion transicion : transiciones) {
            Objects.requireNonNull(transicion, "Una transicion no puede ser nula");
            copia.add(transicion);
            mapa.putIfAbsent(transicion.clave(), transicion.estadoDestino());
        }
        this.transiciones = Collections.unmodifiableList(copia);
        this.delta = Collections.unmodifiableMap(mapa);
    }

    public static FuncionTransicion vacia() {
        return new FuncionTransicion(List.of());
    }

    public Optional<Estado> transitar(Estado estado, char simbolo) {
        if (estado == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(delta.get(new ClaveTransicion(estado, simbolo)));
    }

    public List<Estado> destinos(Estado estado, char simbolo) {
        if (estado == null) {
            return List.of();
        }
        List<Estado> encontrados = new ArrayList<>();
        for (Transicion transicion : transiciones) {
            if (transicion.estadoOrigen().equals(estado) && transicion.simbolo() == simbolo
                    && !encontrados.contains(transicion.estadoDestino())) {
                encontrados.add(transicion.estadoDestino());
            }
        }
        return Collections.unmodifiableList(encontrados);
    }

    public boolean estaDefinida(Estado estado, char simbolo) {
        return estado != null && delta.containsKey(new ClaveTransicion(estado, simbolo));
    }

    public List<Transicion> transiciones() {
        return transiciones;
    }

    public Map<ClaveTransicion, Estado> mapa() {
        return delta;
    }

    public int cantidad() {
        return transiciones.size();
    }

    @Override
    public String toString() {
        return transiciones.toString();
    }
}
