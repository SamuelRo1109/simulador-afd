package com.unbosque.afd.desktop.controlador;

import com.unbosque.afd.core.logica.ValidadorAutomata;
import com.unbosque.afd.core.modelo.AutomataFinitoDeterminista;
import com.unbosque.afd.core.modelo.ErrorValidacion;
import com.unbosque.afd.core.modelo.Estado;
import com.unbosque.afd.core.modelo.Punto;
import com.unbosque.afd.core.modelo.ResultadoValidacion;
import com.unbosque.afd.desktop.controlador.comandos.Comando;
import com.unbosque.afd.desktop.controlador.comandos.ComandoDirecto;
import com.unbosque.afd.desktop.controlador.comandos.ComandoInstantanea;
import com.unbosque.afd.desktop.controlador.comandos.PilaComandos;
import com.unbosque.afd.desktop.render.DistribuidorLienzo;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class ControladorAutomata {

    public record Instantanea(ModeloEdicion.Datos datos, Map<String, Punto> posiciones) {
    }

    public record ConflictoDeterminismo(char simbolo, String destinoActual) {
    }

    private static final double RADIO_DISTRIBUCION = 0.34;
    private static final Dimension LIENZO_POR_DEFECTO = new Dimension(900, 620);

    private final ModeloEdicion modelo = new ModeloEdicion();
    private final Map<String, Punto> posiciones = new LinkedHashMap<>();
    private final PilaComandos pila = new PilaComandos();
    private final List<Runnable> observadores = new ArrayList<>();

    private AutomataFinitoDeterminista automataActual;
    private ResultadoValidacion validacionActual = ResultadoValidacion.vacio();
    private Herramienta herramienta = Herramienta.SELECCIONAR;
    private Seleccion seleccion = Seleccion.NINGUNA;
    private Dimension tamanoLienzo = LIENZO_POR_DEFECTO;
    private Runnable alCancelarSimulacion;
    private Runnable alReencuadrar;
    private Instantanea movimientoEnCurso;

    public ControladorAutomata() {
        reconstruir();
    }

    public ModeloEdicion modelo() {
        return modelo;
    }

    public AutomataFinitoDeterminista automataActual() {
        return automataActual;
    }

    public ResultadoValidacion validacionActual() {
        return validacionActual;
    }

    public Map<String, Punto> posiciones() {
        return Map.copyOf(posiciones);
    }

    public PilaComandos pila() {
        return pila;
    }

    public void agregarObservador(Runnable observador) {
        observadores.add(Objects.requireNonNull(observador, "El observador no puede ser nulo"));
    }

    public void establecerCancelacionDeSimulacion(Runnable accion) {
        this.alCancelarSimulacion = accion;
    }

    public void establecerReencuadre(Runnable accion) {
        this.alReencuadrar = accion;
    }

    public void establecerTamanoLienzo(Dimension tamano) {
        if (tamano != null && tamano.width > 0 && tamano.height > 0) {
            this.tamanoLienzo = new Dimension(tamano);
        }
    }

    public Herramienta herramienta() {
        return herramienta;
    }

    public void establecerHerramienta(Herramienta nueva) {
        Objects.requireNonNull(nueva, "La herramienta no puede ser nula");
        if (herramienta == nueva) {
            return;
        }
        herramienta = nueva;
        if (nueva != Herramienta.SELECCIONAR) {
            seleccion = Seleccion.NINGUNA;
        }
        notificar();
    }

    public Seleccion seleccion() {
        return seleccion;
    }

    public void seleccionar(Seleccion nueva) {
        Seleccion candidata = nueva == null ? Seleccion.NINGUNA : nueva;
        if (candidata.equals(seleccion)) {
            return;
        }
        seleccion = candidata;
        notificar();
    }

    public boolean esValido() {
        return validacionActual.esValido() && !modelo.estados().isEmpty();
    }

    public boolean puedeCompletarConEstadoTrampa() {
        return validacionActual.contieneCodigo(ValidadorAutomata.CODIGO_TRANSICION_FALTANTE);
    }

    public String insertarEstado(double x, double y, boolean aceptacion) {
        String nombre = modelo.siguienteNombreLibre();
        ejecutar(aceptacion ? "Insertar estado de aceptación " + nombre : "Insertar estado " + nombre, () -> {
            modelo.agregarEstado(nombre);
            modelo.establecerAceptacion(nombre, aceptacion);
            posiciones.put(nombre, new Punto(x, y));
        });
        seleccionar(Seleccion.deEstado(nombre));
        return nombre;
    }

    public void alternarAceptacion(String estado) {
        if (!modelo.contieneEstado(estado)) {
            return;
        }
        boolean nuevo = !modelo.esAceptacion(estado);
        ejecutar((nuevo ? "Marcar " : "Desmarcar ") + estado + " como aceptación",
                () -> modelo.establecerAceptacion(estado, nuevo));
    }

    public void establecerAceptacion(String estado, boolean aceptacion) {
        if (!modelo.contieneEstado(estado) || modelo.esAceptacion(estado) == aceptacion) {
            return;
        }
        ejecutar((aceptacion ? "Marcar " : "Desmarcar ") + estado + " como aceptación",
                () -> modelo.establecerAceptacion(estado, aceptacion));
    }

    public void marcarInicial(String estado) {
        if (!modelo.contieneEstado(estado) || modelo.esInicial(estado)) {
            return;
        }
        ejecutar("Marcar " + estado + " como inicial", () -> modelo.establecerInicial(estado));
    }

    public void eliminarEstado(String estado) {
        if (!modelo.contieneEstado(estado)) {
            return;
        }
        ejecutar("Eliminar el estado " + estado, () -> {
            modelo.eliminarEstado(estado);
            posiciones.remove(estado);
        });
        if (seleccion.esEstado(estado)) {
            seleccionar(Seleccion.NINGUNA);
        }
    }

    public boolean renombrarEstado(String anterior, String nuevo) {
        if (!modelo.contieneEstado(anterior)) {
            return false;
        }
        String limpio = nuevo == null ? "" : nuevo.trim();
        if (limpio.equals(anterior)) {
            return true;
        }
        if (limpio.isEmpty() || modelo.contieneEstado(limpio)) {
            return false;
        }
        ejecutar("Renombrar " + anterior + " como " + limpio, () -> {
            modelo.renombrarEstado(anterior, limpio);
            Punto punto = posiciones.remove(anterior);
            if (punto != null) {
                posiciones.put(limpio, punto);
            }
        });
        if (seleccion.esEstado(anterior)) {
            seleccionar(Seleccion.deEstado(limpio));
        }
        return true;
    }

    public void eliminarArista(String origen, String destino) {
        if (modelo.simbolosEntre(origen, destino).isEmpty()) {
            return;
        }
        ejecutar("Eliminar la transición " + origen + " → " + destino,
                () -> modelo.eliminarArista(origen, destino));
        if (seleccion.esArista(origen, destino)) {
            seleccionar(Seleccion.NINGUNA);
        }
    }

    public List<ConflictoDeterminismo> conflictosDeterminismo(String origen, String destino,
                                                             Collection<Character> simbolos) {
        List<ConflictoDeterminismo> conflictos = new ArrayList<>();
        for (char simbolo : simbolos) {
            String actual = modelo.destino(origen, simbolo);
            if (actual != null && !actual.equals(destino)) {
                conflictos.add(new ConflictoDeterminismo(simbolo, actual));
            }
        }
        return List.copyOf(conflictos);
    }

    public void establecerSimbolosArista(String origen, String destino, Set<Character> simbolos) {
        if (!modelo.contieneEstado(origen) || !modelo.contieneEstado(destino)) {
            return;
        }
        ejecutar("Definir δ(" + origen + ", ·) → " + destino, () -> {
            for (char simbolo : modelo.simbolos()) {
                boolean elegido = simbolos.contains(simbolo);
                if (elegido) {
                    modelo.establecerTransicion(origen, simbolo, destino);
                } else if (destino.equals(modelo.destino(origen, simbolo))) {
                    modelo.establecerTransicion(origen, simbolo, null);
                }
            }
        });
        if (simbolos.isEmpty()) {
            if (seleccion.esArista(origen, destino)) {
                seleccionar(Seleccion.NINGUNA);
            }
        } else {
            seleccionar(Seleccion.deArista(origen, destino));
        }
    }

    public void establecerTransicion(String estado, char simbolo, String destino) {
        if (Objects.equals(modelo.destino(estado, simbolo), destino)) {
            return;
        }
        String texto = destino == null
                ? "Quitar δ(" + estado + ", " + simbolo + ")"
                : "Definir δ(" + estado + ", " + simbolo + ") = " + destino;
        ejecutar(texto, () -> modelo.establecerTransicion(estado, simbolo, destino));
    }

    public boolean agregarSimbolo(String texto) {
        String limpio = texto == null ? "" : texto.trim();
        if (limpio.length() != 1 || modelo.contieneSimbolo(limpio.charAt(0))) {
            return false;
        }
        char simbolo = limpio.charAt(0);
        ejecutar("Agregar el símbolo '" + simbolo + "' a Σ", () -> modelo.agregarSimbolo(simbolo));
        return true;
    }

    public void quitarSimbolo(char simbolo) {
        if (!modelo.contieneSimbolo(simbolo)) {
            return;
        }
        ejecutar("Quitar el símbolo '" + simbolo + "' de Σ", () -> modelo.quitarSimbolo(simbolo));
    }

    public void establecerNombre(String nombre) {
        if (Objects.equals(modelo.nombre(), nombre)) {
            return;
        }
        ejecutar("Renombrar el autómata", () -> modelo.establecerNombre(nombre));
    }

    public void iniciarMovimiento() {
        movimientoEnCurso = capturar();
    }

    public void actualizarPosicion(String estado, double x, double y) {
        if (modelo.contieneEstado(estado)) {
            posiciones.put(estado, new Punto(x, y));
        }
    }

    public void finalizarMovimiento(String estado) {
        if (movimientoEnCurso == null) {
            return;
        }
        Instantanea antes = movimientoEnCurso;
        movimientoEnCurso = null;
        Instantanea despues = capturar();
        if (antes.posiciones().equals(despues.posiciones())) {
            return;
        }
        registrar(new ComandoDirecto<>("Mover " + estado, antes, despues, this::restaurar));
    }

    public void reorganizarEnCirculo() {
        List<String> estados = List.copyOf(modelo.estados());
        if (estados.isEmpty()) {
            return;
        }
        List<Punto> destino = distribucionDeReserva(estados.size());
        ejecutar("Reorganizar en círculo", () -> {
            for (int indice = 0; indice < estados.size(); indice++) {
                posiciones.put(estados.get(indice), destino.get(indice));
            }
        });
        reencuadrar();
    }

    public void nuevo() {
        ejecutar("Nuevo autómata", () -> {
            modelo.limpiar();
            posiciones.clear();
        });
        seleccionar(Seleccion.NINGUNA);
        reencuadrar();
    }

    public void cargarEjemplo(AutomataFinitoDeterminista ejemplo) {
        Objects.requireNonNull(ejemplo, "El ejemplo no puede ser nulo");
        ejecutar("Cargar " + ejemplo.nombre(), () -> {
            modelo.cargarDesde(ejemplo);
            posiciones.clear();
            asignarPosicionesFaltantes();
        });
        seleccionar(Seleccion.NINGUNA);
        reencuadrar();
    }

    public void completarConEstadoTrampa() {
        if (automataActual == null || !puedeCompletarConEstadoTrampa()) {
            return;
        }
        AutomataFinitoDeterminista completado =
                ValidadorAutomata.completarConEstadoTrampa(automataActual);
        ejecutar("Completar con estado trampa", () -> {
            modelo.cargarDesde(completado);
            asignarPosicionesFaltantes();
        });
    }

    public boolean deshacer() {
        String descripcion = pila.deshacer();
        if (descripcion == null) {
            return false;
        }
        reconstruir();
        notificar();
        return true;
    }

    public boolean rehacer() {
        String descripcion = pila.rehacer();
        if (descripcion == null) {
            return false;
        }
        reconstruir();
        notificar();
        return true;
    }

    public Optional<String> estadoCulpable(ErrorValidacion hallazgo) {
        String mensaje = hallazgo.mensaje();

        int comillaInicio = mensaje.indexOf('\'');
        if (comillaInicio >= 0) {
            int comillaFin = mensaje.indexOf('\'', comillaInicio + 1);
            if (comillaFin > comillaInicio) {
                String candidato = mensaje.substring(comillaInicio + 1, comillaFin);
                if (modelo.contieneEstado(candidato)) {
                    return Optional.of(candidato);
                }
            }
        }

        int parentesis = mensaje.indexOf('(');
        if (parentesis >= 0) {
            int cierre = mensaje.indexOf(')', parentesis + 1);
            int coma = mensaje.indexOf(',', parentesis + 1);
            if (cierre > parentesis && coma > parentesis && coma < cierre) {
                String candidato = mensaje.substring(parentesis + 1, coma).trim();
                if (modelo.contieneEstado(candidato)) {
                    return Optional.of(candidato);
                }
            }
        }
        return Optional.empty();
    }

    public void sincronizar() {
        reconstruir();
        notificar();
    }

    private void ejecutar(String descripcion, Runnable mutacion) {
        registrar(new ComandoInstantanea<>(descripcion, this::capturar, this::restaurar, mutacion));
    }

    private void registrar(Comando comando) {
        pila.ejecutar(comando);
        reconstruir();
        notificar();
    }

    private Instantanea capturar() {
        return new Instantanea(modelo.instantanea(), new LinkedHashMap<>(posiciones));
    }

    private void restaurar(Instantanea instantanea) {
        modelo.restaurar(instantanea.datos());
        posiciones.clear();
        posiciones.putAll(instantanea.posiciones());
    }

    private void reconstruir() {
        automataActual = modelo.construirAutomata();
        validacionActual = ValidadorAutomata.validar(automataActual);
        asignarPosicionesFaltantes();
        posiciones.keySet().retainAll(modelo.estados());
        if (alCancelarSimulacion != null) {
            alCancelarSimulacion.run();
        }
    }

    private void reencuadrar() {
        if (alReencuadrar != null) {
            alReencuadrar.run();
        }
    }

    private void asignarPosicionesFaltantes() {
        List<String> faltantes = new ArrayList<>();
        for (String estado : modelo.estados()) {
            if (!posiciones.containsKey(estado)) {
                faltantes.add(estado);
            }
        }
        if (faltantes.isEmpty()) {
            return;
        }
        List<Punto> reserva = distribucionDeReserva(Math.max(faltantes.size(), modelo.estados().size()));
        int cursor = posiciones.size();
        for (String estado : faltantes) {
            posiciones.put(estado, reserva.get(Math.min(cursor, reserva.size() - 1)));
            cursor++;
        }
    }

    private List<Punto> distribucionDeReserva(int cantidad) {
        double ancho = tamanoLienzo.width;
        double alto = tamanoLienzo.height;
        return DistribuidorLienzo.distribuir(Math.max(cantidad, 1), ancho / 2, alto / 2,
                Math.min(ancho, alto) * RADIO_DISTRIBUCION);
    }

    private void notificar() {
        for (Runnable observador : new ArrayList<>(observadores)) {
            observador.run();
        }
    }

    public List<Estado> estadosDelAutomata() {
        return automataActual == null ? List.of() : List.copyOf(automataActual.estados());
    }
}
