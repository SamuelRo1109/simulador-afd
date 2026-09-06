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

    public record Datos(String nombre,
                        List<Character> simbolos,
                        List<String> estados,
                        Set<String> estadosAceptacion,
                        Map<ClaveEdicion, String> transiciones,
                        String estadoInicial) {
    }

    private static final String NOMBRE_POR_DEFECTO = "Autómata sin título";

    private final List<Character> simbolos = new ArrayList<>();
    private final List<String> estados = new ArrayList<>();
    private final Set<String> estadosAceptacion = new LinkedHashSet<>();
    private final Map<ClaveEdicion, String> transiciones = new LinkedHashMap<>();

    private String nombre = NOMBRE_POR_DEFECTO;
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

    public void establecerNombre(String nuevoNombre) {
        this.nombre = nuevoNombre == null || nuevoNombre.isBlank() ? NOMBRE_POR_DEFECTO : nuevoNombre.trim();
    }

    public boolean esAceptacion(String estado) {
        return estadosAceptacion.contains(estado);
    }

    public boolean esInicial(String estado) {
        return Objects.equals(estadoInicial, estado);
    }

    public boolean contieneEstado(String estado) {
        return estados.contains(estado);
    }

    public boolean contieneSimbolo(char simbolo) {
        return simbolos.contains(simbolo);
    }

    public String destino(String estado, char simbolo) {
        return transiciones.get(new ClaveEdicion(estado, simbolo));
    }

    public boolean estaVacio() {
        return simbolos.isEmpty() && estados.isEmpty();
    }

    public List<Character> simbolosEntre(String origen, String destino) {
        List<Character> encontrados = new ArrayList<>();
        for (char simbolo : simbolos) {
            if (Objects.equals(destino(origen, simbolo), destino)) {
                encontrados.add(simbolo);
            }
        }
        return List.copyOf(encontrados);
    }

    public List<Transicion> salientesDe(String estado) {
        List<Transicion> encontradas = new ArrayList<>();
        for (char simbolo : simbolos) {
            String destino = destino(estado, simbolo);
            if (destino != null) {
                encontradas.add(new Transicion(Estado.de(estado), simbolo, Estado.de(destino)));
            }
        }
        return List.copyOf(encontradas);
    }

    public List<Transicion> entrantesA(String estado) {
        List<Transicion> encontradas = new ArrayList<>();
        for (Map.Entry<ClaveEdicion, String> entrada : transiciones.entrySet()) {
            if (entrada.getValue().equals(estado)) {
                encontradas.add(new Transicion(
                        Estado.de(entrada.getKey().estado()), entrada.getKey().simbolo(), Estado.de(estado)));
            }
        }
        return List.copyOf(encontradas);
    }

    public int gradoDe(String estado) {
        return salientesDe(estado).size() + entrantesA(estado).size();
    }

    public String siguienteNombreLibre() {
        int indice = 0;
        while (estados.contains("q" + indice)) {
            indice++;
        }
        return "q" + indice;
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
            estadoInicial = null;
        }
    }

    public boolean renombrarEstado(String anterior, String nuevo) {
        if (nuevo == null || nuevo.isBlank() || !estados.contains(anterior)) {
            return false;
        }
        String limpio = nuevo.trim();
        if (limpio.equals(anterior)) {
            return true;
        }
        if (estados.contains(limpio)) {
            return false;
        }

        estados.set(estados.indexOf(anterior), limpio);
        if (estadosAceptacion.remove(anterior)) {
            estadosAceptacion.add(limpio);
        }
        if (Objects.equals(estadoInicial, anterior)) {
            estadoInicial = limpio;
        }

        Map<ClaveEdicion, String> renombradas = new LinkedHashMap<>();
        for (Map.Entry<ClaveEdicion, String> entrada : transiciones.entrySet()) {
            String origen = entrada.getKey().estado().equals(anterior) ? limpio : entrada.getKey().estado();
            String destino = entrada.getValue().equals(anterior) ? limpio : entrada.getValue();
            renombradas.put(new ClaveEdicion(origen, entrada.getKey().simbolo()), destino);
        }
        transiciones.clear();
        transiciones.putAll(renombradas);
        return true;
    }

    public void establecerInicial(String estado) {
        if (estado == null || estados.contains(estado)) {
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

    public void alternarAceptacion(String estado) {
        establecerAceptacion(estado, !esAceptacion(estado));
    }

    public void establecerTransicion(String estado, char simbolo, String destino) {
        ClaveEdicion clave = new ClaveEdicion(estado, simbolo);
        if (destino == null || !estados.contains(destino)) {
            transiciones.remove(clave);
        } else {
            transiciones.put(clave, destino);
        }
    }

    public void eliminarArista(String origen, String destino) {
        transiciones.entrySet().removeIf(entrada ->
                entrada.getKey().estado().equals(origen) && entrada.getValue().equals(destino));
    }

    public void limpiar() {
        simbolos.clear();
        estados.clear();
        estadosAceptacion.clear();
        transiciones.clear();
        estadoInicial = null;
        nombre = NOMBRE_POR_DEFECTO;
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

    public Datos instantanea() {
        return new Datos(nombre, List.copyOf(simbolos), List.copyOf(estados),
                new LinkedHashSet<>(estadosAceptacion), new LinkedHashMap<>(transiciones), estadoInicial);
    }

    public void restaurar(Datos datos) {
        Objects.requireNonNull(datos, "Los datos no pueden ser nulos");
        limpiar();
        nombre = datos.nombre();
        simbolos.addAll(datos.simbolos());
        estados.addAll(datos.estados());
        estadosAceptacion.addAll(datos.estadosAceptacion());
        transiciones.putAll(datos.transiciones());
        estadoInicial = datos.estadoInicial();
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
