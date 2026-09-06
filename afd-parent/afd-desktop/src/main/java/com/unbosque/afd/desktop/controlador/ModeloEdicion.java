package com.unbosque.afd.desktop.controlador;

import com.unbosque.afd.core.modelo.Alfabeto;
import com.unbosque.afd.core.modelo.AutomataFinitoDeterminista;
import com.unbosque.afd.core.modelo.Estado;
import com.unbosque.afd.core.modelo.Transicion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class ModeloEdicion {

    public record ClaveEdicion(String estado, char simbolo) {
    }

    private final List<Character> simbolos = new ArrayList<>();
    private final List<String> estados = new ArrayList<>();
    private final Set<String> estadosAceptacion = new LinkedHashSet<>();
    private final Map<ClaveEdicion, String> transiciones = new LinkedHashMap<>();

    private String nombre = "Automata sin titulo";
    private String estadoInicial;

    public List<Character> simbolos() {
        return Collections.unmodifiableList(simbolos);
    }

    public List<String> estados() {
        return Collections.unmodifiableList(estados);
    }

    public String estadoInicial() {
        return estadoInicial;
    }

    public String nombre() {
        return nombre;
    }

    public boolean esAceptacion(String estado) {
        return estadosAceptacion.contains(estado);
    }

    public boolean esInicial(String estado) {
        return Objects.equals(estadoInicial, estado);
    }

    public String destino(String estado, char simbolo) {
        return transiciones.get(new ClaveEdicion(estado, simbolo));
    }

    public boolean estaVacio() {
        return simbolos.isEmpty() && estados.isEmpty();
    }

    public boolean agregarSimbolo(char simbolo) {
        if (simbolos.contains(simbolo)) {
            return false;
        }
        simbolos.add(simbolo);
        return true;
    }

    public void quitarSimbolo(char simbolo) {
        simbolos.remove(Character.valueOf(simbolo));
        transiciones.keySet().removeIf(clave -> clave.simbolo() == simbolo);
    }

    public boolean agregarEstado(String estado) {
        if (estado == null || estado.isBlank() || estados.contains(estado)) {
            return false;
        }
        estados.add(estado);
        if (estadoInicial == null) {
            estadoInicial = estado;
        }
        return true;
    }

    public void eliminarEstado(String estado) {
        if (!estados.remove(estado)) {
            return;
        }
        estadosAceptacion.remove(estado);
        transiciones.keySet().removeIf(clave -> clave.estado().equals(estado));
        transiciones.values().removeIf(destino -> destino.equals(estado));
        if (Objects.equals(estadoInicial, estado)) {
            estadoInicial = estados.isEmpty() ? null : estados.get(0);
        }
    }

    public void establecerInicial(String estado) {
        if (estados.contains(estado)) {
            estadoInicial = estado;
        }
    }

    public void establecerAceptacion(String estado, boolean aceptacion) {
        if (!estados.contains(estado)) {
            return;
        }
        if (aceptacion) {
            estadosAceptacion.add(estado);
        } else {
            estadosAceptacion.remove(estado);
        }
    }

    public void establecerTransicion(String estado, char simbolo, String destino) {
        ClaveEdicion clave = new ClaveEdicion(estado, simbolo);
        if (destino == null || !estados.contains(destino)) {
            transiciones.remove(clave);
        } else {
            transiciones.put(clave, destino);
        }
    }

    public void limpiar() {
        simbolos.clear();
        estados.clear();
        estadosAceptacion.clear();
        transiciones.clear();
        estadoInicial = null;
        nombre = "Automata sin titulo";
    }

    public void cargarDesde(AutomataFinitoDeterminista automata) {
        Objects.requireNonNull(automata, "El automata no puede ser nulo");
        limpiar();
        nombre = automata.nombre().isEmpty() ? nombre : automata.nombre();
        for (char simbolo : automata.alfabeto().simbolos()) {
            simbolos.add(simbolo);
        }
        for (Estado estado : automata.estados()) {
            estados.add(estado.nombre());
            if (estado.esAceptacion()) {
                estadosAceptacion.add(estado.nombre());
            }
        }
        estadoInicial = automata.tieneEstadoInicial() ? automata.estadoInicial().nombre() : null;
        for (Transicion transicion : automata.funcionTransicion().transiciones()) {
            transiciones.put(
                    new ClaveEdicion(transicion.estadoOrigen().nombre(), transicion.simbolo()),
                    transicion.estadoDestino().nombre());
        }
    }

    public AutomataFinitoDeterminista construirAutomata() {
        AutomataFinitoDeterminista.Constructor constructor = AutomataFinitoDeterminista.constructor()
                .nombre(nombre)
                .alfabeto(new Alfabeto(simbolos.stream().map(String::valueOf).toList()));

        for (String estado : estados) {
            constructor.agregarEstado(estado, esInicial(estado), esAceptacion(estado));
        }
        for (Map.Entry<ClaveEdicion, String> entrada : transiciones.entrySet()) {
            constructor.agregarTransicion(
                    entrada.getKey().estado(), entrada.getKey().simbolo(), entrada.getValue());
        }
        if (estadoInicial != null) {
            constructor.estadoInicial(estadoInicial);
        }
        return constructor.construir();
    }
}
