package com.unbosque.afd.core.logica;

import com.unbosque.afd.core.modelo.AutomataFinitoDeterminista;
import com.unbosque.afd.core.modelo.MotivoRechazo;
import com.unbosque.afd.core.modelo.ResultadoSimulacion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConstructorEjemplosTest {

    private static void assertAcepta(AutomataFinitoDeterminista automata, String cadena) {
        ResultadoSimulacion resultado = SimuladorAFD.simular(automata, cadena);
        assertTrue(resultado.aceptada(),
                () -> automata.nombre() + " deberia aceptar \"" + cadena + "\" y la rechazo por "
                        + resultado.motivoRechazo());
        assertEquals(MotivoRechazo.NINGUNO, resultado.motivoRechazo());
    }

    private static void assertRechaza(AutomataFinitoDeterminista automata, String cadena) {
        ResultadoSimulacion resultado = SimuladorAFD.simular(automata, cadena);
        assertFalse(resultado.aceptada(),
                () -> automata.nombre() + " deberia rechazar \"" + cadena + "\"");
        assertEquals(MotivoRechazo.ESTADO_NO_ACEPTACION, resultado.motivoRechazo());
    }

    @Test
    @DisplayName("Los cuatro ejemplos son automatas validos")
    void losEjemplosSonValidos() {
        for (AutomataFinitoDeterminista automata : ConstructorEjemplos.todos()) {
            assertTrue(ValidadorAutomata.validar(automata).esValido(),
                    () -> "El ejemplo " + automata.nombre() + " deberia ser valido");
            assertEquals(2, automata.alfabeto().tamano());
            assertTrue(automata.alfabeto().contiene('0') && automata.alfabeto().contiene('1'));
        }
    }

    @Nested
    @DisplayName("Cantidad par de ceros")
    class CantidadParDeCeros {

        private final AutomataFinitoDeterminista automata = ConstructorEjemplos.cantidadParDeCeros();

        // "100" tiene dos ceros, es decir una cantidad par, por lo tanto se acepta.
        @ParameterizedTest
        @ValueSource(strings = {"", "1001", "100", "00", "11", "0110"})
        void acepta(String cadena) {
            assertAcepta(automata, cadena);
        }

        @ParameterizedTest
        @ValueSource(strings = {"0", "10", "1000", "011"})
        void rechaza(String cadena) {
            assertRechaza(automata, cadena);
        }
    }

    @Nested
    @DisplayName("Contiene la subcadena 00")
    class ContieneSubcadena00 {

        private final AutomataFinitoDeterminista automata = ConstructorEjemplos.contieneSubcadena00();

        @ParameterizedTest
        @ValueSource(strings = {"1001", "00", "000", "1100", "0011"})
        void acepta(String cadena) {
            assertAcepta(automata, cadena);
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "0101", "1", "0", "010"})
        void rechaza(String cadena) {
            assertRechaza(automata, cadena);
        }
    }

    @Nested
    @DisplayName("Cantidad par de unos")
    class CantidadParDeUnos {

        private final AutomataFinitoDeterminista automata = ConstructorEjemplos.cantidadParDeUnos();

        @ParameterizedTest
        @ValueSource(strings = {"", "11", "0110", "00", "1010"})
        void acepta(String cadena) {
            assertAcepta(automata, cadena);
        }

        @ParameterizedTest
        @ValueSource(strings = {"1", "01", "111", "0010"})
        void rechaza(String cadena) {
            assertRechaza(automata, cadena);
        }
    }

    @Nested
    @DisplayName("Termina en cero")
    class TerminaEnCero {

        private final AutomataFinitoDeterminista automata = ConstructorEjemplos.terminaEnCero();

        @ParameterizedTest
        @ValueSource(strings = {"0", "10", "1110", "00", "0110"})
        void acepta(String cadena) {
            assertAcepta(automata, cadena);
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "1", "01", "0011"})
        void rechaza(String cadena) {
            assertRechaza(automata, cadena);
        }
    }
}
