package com.unbosque.afd.core.modelo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class AutomataFinitoDeterminista {

    private final String nombre;
    private final Alfabeto alfabeto;
    private final Set<Estado> estados;
    private final List<Estado> estadosDeclarados;
    private final FuncionTransicion funcionTransicion;
    private final Estado estadoInicial;
    private final Set<Estado> estadosAceptacion;

    private AutomataFinitoDeterminista(String nombre,
                                       Alfabeto alfabeto,
                                       List<Estado> estadosDeclarados,
                                       Set<Estado> estados,
                                       FuncionTransicion funcionTransicion,
                                       Estado estadoInicial,
                                       Set<Estado> estadosAceptacion) {
        this.nombre = nombre;
        this.alfabeto = alfabeto;
        this.estadosDeclarados = Collections.unmodifiableList(estadosDeclarados);
        this.estados = Collections.unmodifiableSet(estados);
        this.funcionTransicion = funcionTransicion;
        this.estadoInicial = estadoInicial;
        this.estadosAceptacion = Collections.unmodifiableSet(estadosAceptacion);
    }

    public static Constructor constructor() {
        return new Constructor();
    }

    public static Constructor constructorDesde(AutomataFinitoDeterminista automata) {
        Objects.requireNonNull(automata, "El autómata origen no puede ser nulo");
        Constructor constructor = new Constructor()
                .nombre(automata.nombre())
                .alfabeto(automata.alfabeto())
                .estados(automata.estadosDeclarados())
                .transiciones(automata.funcionTransicion().transiciones());
        if (automata.estadoInicial() != null) {
            constructor.estadoInicial(automata.estadoInicial().nombre());
        }
        for (Estado estado : automata.estadosAceptacion()) {
            constructor.agregarEstadoAceptacion(estado.nombre());
        }
        return constructor;
    }

    public String nombre() {
        return nombre;
    }

    public Alfabeto alfabeto() {
        return alfabeto;
    }

    public Set<Estado> estados() {
        return estados;
    }

    public List<Estado> estadosDeclarados() {
        return estadosDeclarados;
    }

    public FuncionTransicion funcionTransicion() {
        return funcionTransicion;
    }

    public Estado estadoInicial() {
        return estadoInicial;
    }

    public Set<Estado> estadosAceptacion() {
        return estadosAceptacion;
    }

    public boolean tieneEstadoInicial() {
        return estadoInicial != null;
    }

    public boolean esDeAceptacion(Estado estado) {
        return estado != null && estadosAceptacion.contains(estado);
    }

    public boolean contieneEstado(Estado estado) {
        return estado != null && estados.contains(estado);
    }

    public Optional<Estado> buscarEstado(String nombreEstado) {
        if (nombreEstado == null) {
            return Optional.empty();
        }
        return estados.stream().filter(estado -> estado.nombre().equals(nombreEstado)).findFirst();
    }

    public Optional<Estado> transitar(Estado estado, char simbolo) {
        return funcionTransicion.transitar(estado, simbolo);
    }

    @Override
    public String toString() {
        return (nombre.isEmpty() ? "AFD" : nombre)
                + " Q=" + estados
                + " Sigma=" + alfabeto
                + " q0=" + estadoInicial
                + " F=" + estadosAceptacion;
    }

    public static final class Constructor {

        private record DefinicionTransicion(String origen, char simbolo, String destino) {
        }

        private String nombre = "";
        private Alfabeto alfabeto;
        private final List<Estado> estados = new ArrayList<>();
        private final List<DefinicionTransicion> definiciones = new ArrayList<>();
        private String nombreEstadoInicial;
        private final Set<String> nombresAceptacion = new LinkedHashSet<>();

        private Constructor() {
        }

        public Constructor nombre(String nombre) {
            this.nombre = nombre == null ? "" : nombre;
            return this;
        }

        public Constructor alfabeto(Alfabeto alfabeto) {
            this.alfabeto = alfabeto;
            return this;
        }

        public Constructor alfabeto(String... simbolos) {
            this.alfabeto = Alfabeto.de(simbolos);
            return this;
        }

        public Constructor agregarEstado(Estado estado) {
            estados.add(Objects.requireNonNull(estado, "El estado no puede ser nulo"));
            return this;
        }

        public Constructor agregarEstado(String nombreEstado, boolean esInicial, boolean esAceptacion) {
            return agregarEstado(new Estado(nombreEstado, esInicial, esAceptacion));
        }

        public Constructor estados(Collection<Estado> nuevosEstados) {
            Objects.requireNonNull(nuevosEstados, "Los estados no pueden ser nulos").forEach(this::agregarEstado);
            return this;
        }

        public Constructor agregarTransicion(Transicion transicion) {
            Objects.requireNonNull(transicion, "La transición no puede ser nula");
            definiciones.add(new DefinicionTransicion(
                    transicion.estadoOrigen().nombre(), transicion.simbolo(), transicion.estadoDestino().nombre()));
            return this;
        }

        public Constructor agregarTransicion(String origen, char simbolo, String destino) {
            definiciones.add(new DefinicionTransicion(
                    Objects.requireNonNull(origen, "El origen no puede ser nulo"),
                    simbolo,
                    Objects.requireNonNull(destino, "El destino no puede ser nulo")));
            return this;
        }

        public Constructor transiciones(Collection<Transicion> nuevasTransiciones) {
            Objects.requireNonNull(nuevasTransiciones, "Las transiciones no pueden ser nulas")
                    .forEach(this::agregarTransicion);
            return this;
        }

        public Constructor estadoInicial(String nombreEstado) {
            this.nombreEstadoInicial = nombreEstado;
            return this;
        }

        public Constructor agregarEstadoAceptacion(String nombreEstado) {
            nombresAceptacion.add(Objects.requireNonNull(nombreEstado, "El estado de aceptación no puede ser nulo"));
            return this;
        }

        public AutomataFinitoDeterminista construir() {
            Alfabeto alfabetoFinal = alfabeto == null ? new Alfabeto(List.of()) : alfabeto;

            List<Estado> declarados = new ArrayList<>(estados.size());
            for (Estado estado : estados) {
                declarados.add(aplicarBanderas(estado));
            }

            Set<Estado> conjunto = new LinkedHashSet<>(declarados);

            List<Transicion> resueltas = new ArrayList<>(definiciones.size());
            for (DefinicionTransicion definicion : definiciones) {
                resueltas.add(new Transicion(
                        resolver(conjunto, definicion.origen()),
                        definicion.simbolo(),
                        resolver(conjunto, definicion.destino())));
            }

            Estado inicial = determinarEstadoInicial(conjunto);

            Set<Estado> aceptacion = new LinkedHashSet<>();
            for (Estado estado : conjunto) {
                if (estado.esAceptacion()) {
                    aceptacion.add(estado);
                }
            }

            return new AutomataFinitoDeterminista(nombre, alfabetoFinal, declarados, conjunto,
                    new FuncionTransicion(resueltas), inicial, aceptacion);
        }

        private Estado aplicarBanderas(Estado estado) {
            boolean inicial = estado.esInicial() || estado.nombre().equals(nombreEstadoInicial);
            boolean aceptacion = estado.esAceptacion() || nombresAceptacion.contains(estado.nombre());
            if (inicial == estado.esInicial() && aceptacion == estado.esAceptacion()) {
                return estado;
            }
            return new Estado(estado.nombre(), inicial, aceptacion);
        }

        private Estado determinarEstadoInicial(Set<Estado> conjunto) {
            if (nombreEstadoInicial != null) {
                return resolver(conjunto, nombreEstadoInicial);
            }
            for (Estado estado : conjunto) {
                if (estado.esInicial()) {
                    return estado;
                }
            }
            return null;
        }

        private Estado resolver(Set<Estado> conjunto, String nombreEstado) {
            for (Estado estado : conjunto) {
                if (estado.nombre().equals(nombreEstado)) {
                    return estado;
                }
            }
            return Estado.de(nombreEstado);
        }
    }
}
