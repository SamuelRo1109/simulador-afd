package com.unbosque.afd.core.logica;

import com.unbosque.afd.core.modelo.Alfabeto;
import com.unbosque.afd.core.modelo.AutomataFinitoDeterminista;
import com.unbosque.afd.core.modelo.ClaveTransicion;
import com.unbosque.afd.core.modelo.ErrorValidacion;
import com.unbosque.afd.core.modelo.Estado;
import com.unbosque.afd.core.modelo.ResultadoValidacion;
import com.unbosque.afd.core.modelo.Transicion;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class ValidadorAutomata {

    public static final String CODIGO_SIN_ESTADO_INICIAL = "E01_SIN_ESTADO_INICIAL";
    public static final String CODIGO_VARIOS_ESTADOS_INICIALES = "E02_VARIOS_ESTADOS_INICIALES";
    public static final String CODIGO_NOMBRE_VACIO = "E03_NOMBRE_VACIO";
    public static final String CODIGO_NOMBRE_DUPLICADO = "E04_NOMBRE_DUPLICADO";
    public static final String CODIGO_ALFABETO_VACIO = "E05_ALFABETO_VACIO";
    public static final String CODIGO_NO_DETERMINISTA = "E06_NO_DETERMINISTA";
    public static final String CODIGO_TRANSICION_FALTANTE = "E07_TRANSICION_FALTANTE";
    public static final String CODIGO_DESTINO_INEXISTENTE = "E08_DESTINO_INEXISTENTE";
    public static final String CODIGO_ESTADO_INALCANZABLE = "A01_ESTADO_INALCANZABLE";
    public static final String CODIGO_ACEPTACION_INALCANZABLE = "A02_ACEPTACION_INALCANZABLE";

    public static final String NOMBRE_ESTADO_TRAMPA = "qTrampa";

    private ValidadorAutomata() {
    }

    public static ResultadoValidacion validar(AutomataFinitoDeterminista automata) {
        Objects.requireNonNull(automata, "El autómata no puede ser nulo");
        List<ErrorValidacion> hallazgos = new ArrayList<>();

        validarEstadoInicial(automata, hallazgos);
        validarNombresDeEstado(automata, hallazgos);
        validarAlfabeto(automata, hallazgos);
        validarDeterminismo(automata, hallazgos);
        validarTotalidad(automata, hallazgos);
        validarDestinos(automata, hallazgos);
        validarAlcanzabilidad(automata, hallazgos);

        return new ResultadoValidacion(hallazgos);
    }

    public static AutomataFinitoDeterminista completarConEstadoTrampa(AutomataFinitoDeterminista automata) {
        Objects.requireNonNull(automata, "El autómata no puede ser nulo");

        Alfabeto alfabeto = automata.alfabeto();
        List<ClaveTransicion> faltantes = new ArrayList<>();
        for (Estado estado : automata.estados()) {
            for (char simbolo : alfabeto.simbolos()) {
                if (!automata.funcionTransicion().estaDefinida(estado, simbolo)) {
                    faltantes.add(new ClaveTransicion(estado, simbolo));
                }
            }
        }

        AutomataFinitoDeterminista.Constructor constructor =
                AutomataFinitoDeterminista.constructorDesde(automata);
        if (faltantes.isEmpty()) {
            return constructor.construir();
        }

        String nombreTrampa = nombreTrampaDisponible(automata);
        constructor.agregarEstado(nombreTrampa, false, false);
        for (ClaveTransicion clave : faltantes) {
            constructor.agregarTransicion(clave.estado().nombre(), clave.simbolo(), nombreTrampa);
        }
        for (char simbolo : alfabeto.simbolos()) {
            constructor.agregarTransicion(nombreTrampa, simbolo, nombreTrampa);
        }
        return constructor.construir();
    }

    private static String nombreTrampaDisponible(AutomataFinitoDeterminista automata) {
        if (automata.buscarEstado(NOMBRE_ESTADO_TRAMPA).isEmpty()) {
            return NOMBRE_ESTADO_TRAMPA;
        }
        int sufijo = 1;
        while (automata.buscarEstado(NOMBRE_ESTADO_TRAMPA + sufijo).isPresent()) {
            sufijo++;
        }
        return NOMBRE_ESTADO_TRAMPA + sufijo;
    }

    private static void validarEstadoInicial(AutomataFinitoDeterminista automata, List<ErrorValidacion> hallazgos) {
        List<Estado> iniciales = automata.estados().stream().filter(Estado::esInicial).toList();
        if (iniciales.isEmpty()) {
            hallazgos.add(ErrorValidacion.error(CODIGO_SIN_ESTADO_INICIAL,
                    "El autómata debe tener exactamente un estado inicial y no tiene ninguno"));
        } else if (iniciales.size() > 1) {
            hallazgos.add(ErrorValidacion.error(CODIGO_VARIOS_ESTADOS_INICIALES,
                    "El autómata debe tener exactamente un estado inicial y tiene " + iniciales.size()
                            + ": " + nombres(iniciales)));
        } else if (!automata.contieneEstado(automata.estadoInicial())) {
            hallazgos.add(ErrorValidacion.error(CODIGO_SIN_ESTADO_INICIAL,
                    "El estado inicial declarado no pertenece al conjunto de estados"));
        }
    }

    private static void validarNombresDeEstado(AutomataFinitoDeterminista automata, List<ErrorValidacion> hallazgos) {
        Set<String> vistos = new LinkedHashSet<>();
        Set<String> reportados = new LinkedHashSet<>();
        for (Estado estado : automata.estadosDeclarados()) {
            String nombre = estado.nombre();
            if (nombre.isBlank() && reportados.add("<vacio>")) {
                hallazgos.add(ErrorValidacion.error(CODIGO_NOMBRE_VACIO,
                        "Existe al menos un estado con nombre vacío"));
            }
            if (!vistos.add(nombre) && reportados.add(nombre)) {
                hallazgos.add(ErrorValidacion.error(CODIGO_NOMBRE_DUPLICADO,
                        "El nombre de estado '" + nombre + "' está duplicado"));
            }
        }
    }

    private static void validarAlfabeto(AutomataFinitoDeterminista automata, List<ErrorValidacion> hallazgos) {
        if (automata.alfabeto().estaVacio()) {
            hallazgos.add(ErrorValidacion.error(CODIGO_ALFABETO_VACIO, "El alfabeto no puede estar vacío"));
        }
    }

    private static void validarDeterminismo(AutomataFinitoDeterminista automata, List<ErrorValidacion> hallazgos) {
        Map<ClaveTransicion, Set<String>> destinosPorClave = new LinkedHashMap<>();
        for (Transicion transicion : automata.funcionTransicion().transiciones()) {
            destinosPorClave
                    .computeIfAbsent(transicion.clave(), clave -> new LinkedHashSet<>())
                    .add(transicion.estadoDestino().nombre());
        }
        for (Map.Entry<ClaveTransicion, Set<String>> entrada : destinosPorClave.entrySet()) {
            if (entrada.getValue().size() > 1) {
                ClaveTransicion clave = entrada.getKey();
                hallazgos.add(ErrorValidacion.error(CODIGO_NO_DETERMINISTA,
                        "El par (" + clave.estado().nombre() + ", " + clave.simbolo() + ") tiene "
                                + entrada.getValue().size() + " destinos: " + String.join(", ", entrada.getValue())));
            }
        }
    }

    private static void validarTotalidad(AutomataFinitoDeterminista automata, List<ErrorValidacion> hallazgos) {
        for (Estado estado : automata.estados()) {
            for (char simbolo : automata.alfabeto().simbolos()) {
                if (!automata.funcionTransicion().estaDefinida(estado, simbolo)) {
                    hallazgos.add(ErrorValidacion.error(CODIGO_TRANSICION_FALTANTE,
                            "No hay transición definida para (" + estado.nombre() + ", " + simbolo + ")"));
                }
            }
        }
    }

    private static void validarDestinos(AutomataFinitoDeterminista automata, List<ErrorValidacion> hallazgos) {
        Set<String> reportados = new LinkedHashSet<>();
        for (Transicion transicion : automata.funcionTransicion().transiciones()) {
            Estado destino = transicion.estadoDestino();
            if (!automata.contieneEstado(destino) && reportados.add(destino.nombre())) {
                hallazgos.add(ErrorValidacion.error(CODIGO_DESTINO_INEXISTENTE,
                        "El estado destino '" + destino.nombre() + "' no pertenece al conjunto de estados"));
            }
        }
    }

    private static void validarAlcanzabilidad(AutomataFinitoDeterminista automata, List<ErrorValidacion> hallazgos) {
        if (!automata.tieneEstadoInicial()) {
            return;
        }
        Set<Estado> alcanzables = alcanzables(automata);

        for (Estado estado : automata.estados()) {
            if (!alcanzables.contains(estado)) {
                hallazgos.add(ErrorValidacion.advertencia(CODIGO_ESTADO_INALCANZABLE,
                        "El estado '" + estado.nombre() + "' no es alcanzable desde el estado inicial"));
            }
        }
        for (Estado estado : automata.estadosAceptacion()) {
            if (!alcanzables.contains(estado)) {
                hallazgos.add(ErrorValidacion.advertencia(CODIGO_ACEPTACION_INALCANZABLE,
                        "El estado de aceptación '" + estado.nombre()
                                + "' no es alcanzable desde el estado inicial"));
            }
        }
    }

    public static Set<Estado> alcanzables(AutomataFinitoDeterminista automata) {
        Objects.requireNonNull(automata, "El autómata no puede ser nulo");
        Set<Estado> visitados = new LinkedHashSet<>();
        if (!automata.tieneEstadoInicial()) {
            return visitados;
        }

        Map<Estado, List<Estado>> sucesores = new LinkedHashMap<>();
        for (Transicion transicion : automata.funcionTransicion().transiciones()) {
            sucesores
                    .computeIfAbsent(transicion.estadoOrigen(), origen -> new ArrayList<>())
                    .add(transicion.estadoDestino());
        }

        Deque<Estado> cola = new ArrayDeque<>();
        visitados.add(automata.estadoInicial());
        cola.add(automata.estadoInicial());
        while (!cola.isEmpty()) {
            Estado actual = cola.removeFirst();
            for (Estado siguiente : sucesores.getOrDefault(actual, List.of())) {
                if (visitados.add(siguiente)) {
                    cola.addLast(siguiente);
                }
            }
        }
        return visitados;
    }

    private static String nombres(List<Estado> estados) {
        return estados.stream().map(Estado::nombre).reduce((uno, otro) -> uno + ", " + otro).orElse("");
    }
}
