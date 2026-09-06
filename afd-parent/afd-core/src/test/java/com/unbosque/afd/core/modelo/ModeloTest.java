package com.unbosque.afd.core.modelo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModeloTest {

    @Nested
    @DisplayName("Alfabeto")
    class AlfabetoTest {

        @Test
        void conservaElOrdenDeInsercionYReconoceSimbolos() {
            Alfabeto alfabeto = Alfabeto.de("1", "0", "a");

            assertEquals(3, alfabeto.tamano());
            assertEquals(List.of('1', '0', 'a'), List.copyOf(alfabeto.simbolos()));
            assertTrue(alfabeto.contiene('a'));
            assertFalse(alfabeto.contiene('b'));
            assertFalse(alfabeto.estaVacio());
        }

        @Test
        void rechazaDuplicadosVaciosYMultiCaracter() {
            assertThrows(IllegalArgumentException.class, () -> Alfabeto.de("0", "0"));
            assertThrows(IllegalArgumentException.class, () -> Alfabeto.de("0", ""));
            assertThrows(IllegalArgumentException.class, () -> Alfabeto.de("0", "01"));
            assertThrows(NullPointerException.class, () -> new Alfabeto(null));
        }

        @Test
        void elConjuntoDeSimbolosEsInmutable() {
            Alfabeto alfabeto = Alfabeto.de("0", "1");
            assertThrows(UnsupportedOperationException.class, () -> alfabeto.simbolos().add('2'));
        }
    }

    @Nested
    @DisplayName("Estado")
    class EstadoTest {

        @Test
        void laIgualdadEsPorNombre() {
            Estado uno = new Estado("q0", true, false);
            Estado otro = new Estado("q0", false, true);

            assertEquals(uno, otro);
            assertEquals(uno.hashCode(), otro.hashCode());
            assertFalse(uno.equals(Estado.de("q1")));
        }

        @Test
        void lasBanderasSeCopianSinMutar() {
            Estado original = Estado.de("q0");
            Estado inicial = original.conInicial(true);

            assertFalse(original.esInicial());
            assertTrue(inicial.esInicial());
            assertTrue(Estado.inicialYAceptacion("q1").esAceptacion());
            assertThrows(NullPointerException.class, () -> Estado.de(null));
        }
    }

    @Nested
    @DisplayName("FuncionTransicion")
    class FuncionTransicionTest {

        private final Estado q0 = new Estado("q0", true, false);
        private final Estado q1 = Estado.aceptacion("q1");

        @Test
        void transitarDevuelveElDestinoOVacio() {
            FuncionTransicion delta = new FuncionTransicion(List.of(new Transicion(q0, '0', q1)));

            assertEquals(q1, delta.transitar(q0, '0').orElseThrow());
            assertTrue(delta.transitar(q0, '1').isEmpty());
            assertTrue(delta.transitar(q1, '0').isEmpty());
            assertTrue(delta.transitar(null, '0').isEmpty());
            assertTrue(delta.estaDefinida(q0, '0'));
            assertFalse(delta.estaDefinida(q0, '1'));
        }

        @Test
        void conservaLosDestinosMultiplesParaDetectarNoDeterminismo() {
            FuncionTransicion delta = new FuncionTransicion(List.of(
                    new Transicion(q0, '0', q0),
                    new Transicion(q0, '0', q1)));

            assertEquals(2, delta.destinos(q0, '0').size());
            assertEquals(q0, delta.transitar(q0, '0').orElseThrow());
            assertEquals(2, delta.cantidad());
            assertEquals(1, delta.mapa().size());
        }

        @Test
        void esVaciaPorDefectoYNoAdmiteNulos() {
            assertEquals(0, FuncionTransicion.vacia().cantidad());
            assertThrows(NullPointerException.class, () -> new Transicion(null, '0', q1));
            assertThrows(NullPointerException.class, () -> new Transicion(q0, '0', null));
        }
    }

    @Nested
    @DisplayName("AutomataFinitoDeterminista")
    class AutomataTest {

        @Test
        void elConstructorArmaLosConjuntosDerivados() {
            AutomataFinitoDeterminista automata = AutomataFinitoDeterminista.constructor()
                    .nombre("Ejemplo")
                    .alfabeto("0", "1")
                    .agregarEstado("q0", false, false)
                    .agregarEstado("q1", false, false)
                    .estadoInicial("q0")
                    .agregarEstadoAceptacion("q1")
                    .agregarTransicion("q0", '0', "q1")
                    .agregarTransicion("q0", '1', "q0")
                    .agregarTransicion("q1", '0', "q0")
                    .agregarTransicion("q1", '1', "q1")
                    .construir();

            assertEquals("Ejemplo", automata.nombre());
            assertEquals(2, automata.estados().size());
            assertTrue(automata.tieneEstadoInicial());
            assertEquals("q0", automata.estadoInicial().nombre());
            assertTrue(automata.estadoInicial().esInicial());
            assertEquals(1, automata.estadosAceptacion().size());
            assertTrue(automata.esDeAceptacion(Estado.de("q1")));
            assertEquals(4, automata.funcionTransicion().cantidad());
            assertEquals("q1", automata.transitar(automata.estadoInicial(), '0').orElseThrow().nombre());
        }

        @Test
        void losConjuntosExpuestosSonInmutables() {
            AutomataFinitoDeterminista automata = AutomataFinitoDeterminista.constructor()
                    .alfabeto("0")
                    .agregarEstado("q0", true, true)
                    .agregarTransicion("q0", '0', "q0")
                    .construir();

            assertThrows(UnsupportedOperationException.class, () -> automata.estados().add(Estado.de("x")));
            assertThrows(UnsupportedOperationException.class, () -> automata.estadosAceptacion().clear());
            assertThrows(UnsupportedOperationException.class,
                    () -> automata.funcionTransicion().transiciones().clear());
            assertTrue(automata.buscarEstado("inexistente").isEmpty());
        }

        @Test
        void constructorDesdeCopiaLaDefinicionCompleta() {
            AutomataFinitoDeterminista original = AutomataFinitoDeterminista.constructor()
                    .nombre("Original")
                    .alfabeto("0", "1")
                    .agregarEstado("q0", true, true)
                    .agregarEstado("q1", false, false)
                    .agregarTransicion("q0", '0', "q1")
                    .agregarTransicion("q0", '1', "q0")
                    .agregarTransicion("q1", '0', "q0")
                    .agregarTransicion("q1", '1', "q1")
                    .construir();

            AutomataFinitoDeterminista copia = AutomataFinitoDeterminista.constructorDesde(original).construir();

            assertEquals(original.nombre(), copia.nombre());
            assertEquals(original.alfabeto(), copia.alfabeto());
            assertEquals(original.estados(), copia.estados());
            assertEquals(original.estadosAceptacion(), copia.estadosAceptacion());
            assertEquals(original.estadoInicial(), copia.estadoInicial());
            assertEquals(original.funcionTransicion().cantidad(), copia.funcionTransicion().cantidad());
        }
    }

    @Nested
    @DisplayName("Resultados")
    class ResultadosTest {

        @Test
        void elResultadoDeValidacionDistingueErroresDeAdvertencias() {
            ResultadoValidacion soloAdvertencias = new ResultadoValidacion(List.of(
                    ErrorValidacion.advertencia("A01", "estado aislado")));
            ResultadoValidacion conError = new ResultadoValidacion(List.of(
                    ErrorValidacion.advertencia("A01", "estado aislado"),
                    ErrorValidacion.error("E01", "sin estado inicial")));

            assertTrue(ResultadoValidacion.vacio().esValido());
            assertTrue(soloAdvertencias.esValido());
            assertTrue(soloAdvertencias.tieneAdvertencias());
            assertFalse(conError.esValido());
            assertEquals(1, conError.errores().size());
            assertEquals(1, conError.advertencias().size());
            assertTrue(conError.contieneCodigo("E01"));
            assertEquals(Severidad.ERROR, conError.errores().get(0).severidad());
        }

        @Test
        void elResultadoDeSimulacionCopiaLaTraza() {
            Estado q0 = new Estado("q0", true, true);
            List<PasoEjecucion> pasos = new java.util.ArrayList<>();
            pasos.add(new PasoEjecucion(0, q0, '0', q0, "0", "", true));

            ResultadoSimulacion resultado = new ResultadoSimulacion(true, q0, pasos, MotivoRechazo.NINGUNO);
            pasos.clear();

            assertEquals(1, resultado.cantidadPasos());
            assertFalse(resultado.rechazada());
            assertFalse(resultado.pasos().get(0).fallido());
            assertThrows(NullPointerException.class,
                    () -> new ResultadoSimulacion(false, q0, null, MotivoRechazo.NINGUNO));
        }
    }
}
