package com.unbosque.afd.desktop;

import com.unbosque.afd.core.modelo.MotivoRechazo;
import com.unbosque.afd.desktop.controlador.ControladorAutomata;
import com.unbosque.afd.desktop.controlador.ControladorSimulacion;
import com.unbosque.afd.desktop.controlador.Herramienta;
import com.unbosque.afd.desktop.render.Tema;
import com.unbosque.afd.desktop.vista.LienzoAutomata;
import com.unbosque.afd.desktop.vista.PanelMatrizTransiciones;
import com.unbosque.afd.desktop.vista.SelectorSimbolos;
import com.unbosque.afd.desktop.vista.VentanaPrincipal;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FlujoCompletoTest {

    private static final Path SALIDA = Path.of("target", "capturas");

    private static VentanaPrincipal ventana;
    private static LienzoAutomata lienzo;
    private static ControladorAutomata controlador;
    private static ControladorSimulacion simulacion;
    private static PanelMatrizTransiciones matriz;

    @BeforeAll
    static void prepararVentana() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "La verificación visual necesita un entorno gráfico");
        Files.createDirectories(SALIDA);

        enEdt(() -> {
            App.prepararInterfaz();
            Tema.establecer(Tema.GRAFITO);
            ventana = new VentanaPrincipal();
            ventana.addNotify();
            ventana.validate();

            lienzo = ventana.lienzo();
            controlador = ventana.controlador();
            simulacion = ventana.simulacion();
            matriz = ventana.panelMatriz();
        });
        assertTrue(lienzo.getWidth() > 400, "El lienzo debe quedar dimensionado por el layout");
    }

    @AfterAll
    static void cerrarVentana() throws Exception {
        if (ventana != null) {
            enEdt(() -> ventana.dispose());
        }
    }

    @Test
    @Order(1)
    void insertaTresEstadosConLaHerramientaEstado() throws Exception {
        enEdt(() -> {
            controlador.agregarSimbolo("0");
            controlador.agregarSimbolo("1");
            controlador.establecerHerramienta(Herramienta.ESTADO);
            clic(260, 200);
            clic(520, 200);
            clic(390, 400);
        });

        assertEquals(List.of("q0", "q1", "q2"), controlador.modelo().estados());
        assertEquals(3, lienzo.nodos().size());
    }

    @Test
    @Order(2)
    void marcaInicialYAceptacionConSusHerramientas() throws Exception {
        enEdt(() -> {
            controlador.establecerHerramienta(Herramienta.INICIAL);
            clicEnEstado("q0");
            controlador.establecerHerramienta(Herramienta.ACEPTACION);
            clicEnEstado("q1");
        });

        assertEquals("q0", controlador.modelo().estadoInicial());
        assertTrue(controlador.modelo().esAceptacion("q1"));
        assertFalse(controlador.modelo().esAceptacion("q0"));
    }

    @Test
    @Order(3)
    void creaArcoYBucleArrastrandoConLaHerramientaTransicion() throws Exception {
        enEdt(() -> {
            controlador.establecerHerramienta(Herramienta.TRANSICION);
            arrastrarEntre("q0", "q1");
            aplicarSelector(Set.of('0'));
            arrastrarEntre("q1", "q1");
            aplicarSelector(Set.of('1'));
        });

        assertEquals("q1", controlador.modelo().destino("q0", '0'));
        assertEquals("q1", controlador.modelo().destino("q1", '1'));
        assertTrue(lienzo.aristas().stream().anyMatch(arista -> arista.conecta("q1", "q1")),
                "El bucle debe existir como arista");
    }

    @Test
    @Order(4)
    void avisaDelNoDeterminismoEnVezDePermitirloEnSilencio() throws Exception {
        AtomicReference<String> aviso = new AtomicReference<>();
        AtomicReference<Boolean> conflicto = new AtomicReference<>();

        enEdt(() -> {
            controlador.establecerHerramienta(Herramienta.TRANSICION);
            arrastrarEntre("q0", "q2");
            SelectorSimbolos selector = lienzo.selectorAbierto();
            assertNotNull(selector, "Soltar sobre un estado debe abrir el selector");
            selector.marcar('0', true);
            conflicto.set(selector.hayConflictos());
            aviso.set(selector.avisoActual());
        });

        assertTrue(conflicto.get(), "El selector debe detectar el conflicto de determinismo");
        assertTrue(aviso.get().contains("δ(q0,0) ya va a q1"),
                "El aviso debe nombrar el conflicto, y decía: " + aviso.get());
        assertEquals("q1", controlador.modelo().destino("q0", '0'),
                "δ(q0,0) no puede cambiar sin confirmación explícita");

        enEdt(() -> {
            assertEquals(List.of(new ControladorAutomata.ConflictoDeterminismo('0', "q1")),
                    controlador.conflictosDeterminismo("q0", "q2", Set.of('0')));
            lienzo.selectorAbierto().marcar('0', false);
            lienzo.selectorAbierto().aplicarSeleccion();
        });
    }

    @Test
    @Order(5)
    void completaCantidadParDeCerosYElAfdQuedaValido() throws Exception {
        enEdt(() -> {
            controlador.eliminarEstado("q2");
            controlador.establecerAceptacion("q1", false);
            controlador.establecerAceptacion("q0", true);
            controlador.renombrarEstado("q0", "qPar");
            controlador.renombrarEstado("q1", "qImpar");
            controlador.establecerTransicion("qPar", '0', "qImpar");
            controlador.establecerTransicion("qPar", '1', "qPar");
            controlador.establecerTransicion("qImpar", '0', "qPar");
            controlador.establecerTransicion("qImpar", '1', "qImpar");
            controlador.establecerNombre("Cantidad par de ceros");
        });

        assertTrue(controlador.validacionActual().esValido(),
                "Hallazgos: " + controlador.validacionActual().hallazgos());
        assertTrue(controlador.esValido());
        assertTrue(controlador.validacionActual().hallazgos().isEmpty(),
                "No deben quedar advertencias: " + controlador.validacionActual().hallazgos());
    }

    @Test
    @Order(6)
    void simula1001ConLosCuatroResaltadosSincronizados() throws Exception {
        enEdt(() -> simulacion.simular("1001"));
        assertEquals(4, simulacion.totalPasos());

        enEdt(() -> assertSame(lienzo.nodoActivo(), buscarNodo("qPar"),
                "En el paso -1 el resaltado se posa en el estado inicial"));

        String[] esperados = {"qPar", "qImpar", "qPar", "qPar"};
        for (int indice = 0; indice < esperados.length; indice++) {
            int paso = indice;
            enEdt(() -> {
                simulacion.siguiente();
                verificarSincronizacionCuadruple();
            });
            assertEquals(paso, simulacion.paso());
            assertEquals(esperados[paso], simulacion.estadoActual().nombre());
        }

        assertTrue(simulacion.hayVeredicto());
        assertTrue(simulacion.resultado().aceptada());
        assertEquals("Cadena ACEPTADA — terminó en qPar, que es de aceptación",
                simulacion.mensajeVeredicto());
        enEdt(() -> assertNotNull(lienzo.nodoResultado()));
    }

    @Test
    @Order(7)
    void reproduceEnModoAutomaticoHastaElFinalYSeDetieneSola() throws Exception {
        enEdt(() -> {
            simulacion.simular("1001");
            simulacion.establecerVelocidad(60);
            simulacion.reproducir();
        });

        long limite = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < limite && esperando(() -> simulacion.reproduciendo())) {
            Thread.sleep(30);
        }

        assertFalse(esperando(() -> simulacion.reproduciendo()), "La reproducción debe detenerse sola");
        assertEquals(simulacion.totalPasos() - 1, simulacion.paso());
        assertTrue(simulacion.hayVeredicto());
        enEdt(this::verificarSincronizacionCuadruple);
    }

    @Test
    @Order(8)
    void rechaza0PorEstadoNoAceptacionYAceptaLaCadenaVacia() throws Exception {
        enEdt(() -> simulacion.simular("0"));
        assertTrue(simulacion.hayResultado());
        enEdt(() -> simulacion.fin());

        assertFalse(simulacion.resultado().aceptada());
        assertEquals(MotivoRechazo.ESTADO_NO_ACEPTACION, simulacion.resultado().motivoRechazo());
        assertEquals("Cadena RECHAZADA — terminó en qImpar, que no es de aceptación",
                simulacion.mensajeVeredicto());

        enEdt(() -> simulacion.simular(""));
        assertEquals(0, simulacion.totalPasos());
        assertTrue(simulacion.hayVeredicto(), "La cadena vacía da veredicto de inmediato");
        assertTrue(simulacion.resultado().aceptada(), "λ termina en qPar, que es de aceptación");
        assertEquals("Cadena vacía (λ) — no se lee ningún símbolo", simulacion.descripcionPaso());
    }

    @Test
    @Order(9)
    void cancelaLaSimulacionCuandoSeEditaElAutomata() throws Exception {
        enEdt(() -> simulacion.simular("1001"));
        assertTrue(simulacion.hayResultado());

        enEdt(() -> controlador.alternarAceptacion("qImpar"));
        assertFalse(simulacion.hayResultado(), "Editar el autómata cancela la simulación");
        enEdt(() -> {
            assertNull(lienzo.nodoActivo());
            assertNull(lienzo.aristaActiva());
        });

        enEdt(() -> controlador.deshacer());
        assertFalse(controlador.modelo().esAceptacion("qImpar"));
    }

    @Test
    @Order(10)
    void deshaceYRehaceLasEdicionesDelLienzo() throws Exception {
        int estadosAntes = controlador.modelo().estados().size();
        enEdt(() -> {
            controlador.establecerHerramienta(Herramienta.ESTADO);
            clic(700, 500);
        });
        assertEquals(estadosAntes + 1, controlador.modelo().estados().size());

        enEdt(() -> controlador.deshacer());
        assertEquals(estadosAntes, controlador.modelo().estados().size());

        enEdt(() -> controlador.rehacer());
        assertEquals(estadosAntes + 1, controlador.modelo().estados().size());

        enEdt(() -> {
            for (int repeticion = 0; repeticion < 6; repeticion++) {
                controlador.deshacer();
            }
        });
        assertTrue(controlador.modelo().estados().size() <= estadosAntes,
                "Deshacer repetido debe seguir retrocediendo el historial");

        enEdt(() -> {
            while (controlador.pila().puedeRehacer()) {
                controlador.rehacer();
            }
        });
        assertEquals(estadosAntes + 1, controlador.modelo().estados().size(),
                "Rehacer hasta el final devuelve la edición más reciente");
    }

    @Test
    @Order(11)
    void exportaCapturasDeLosDosTemas() throws Exception {
        enEdt(() -> {
            controlador.cargarEjemplo(com.unbosque.afd.core.logica.ConstructorEjemplos.cantidadParDeCeros());
            simulacion.simular("1001");
            simulacion.irAPaso(1);
        });

        File grafito = SALIDA.resolve("grafito-simulacion.png").toFile();
        File papel = SALIDA.resolve("papel-simulacion.png").toFile();
        File grafito2x = SALIDA.resolve("grafito-2x.png").toFile();

        enEdt(() -> {
            try {
                Tema.establecer(Tema.GRAFITO);
                lienzo.exportarPNG(grafito, 1.0, false);
                lienzo.exportarPNG(grafito2x, 2.0, false);
                Tema.establecer(Tema.PAPEL);
                ventana.validate();
                lienzo.exportarPNG(papel, 1.0, false);
            } catch (Exception excepcion) {
                throw new IllegalStateException(excepcion);
            }
        });

        assertTrue(grafito.length() > 0 && papel.length() > 0 && grafito2x.length() > 0);
        assertTrue(grafito2x.length() > grafito.length(), "La captura 2x debe pesar más");

        enEdt(() -> {
            assertSame(Tema.PAPEL, Tema.actual());
            assertNotNull(lienzo.nodoActivo(), "El resaltado sobrevive al cambio de tema");
            Tema.establecer(Tema.GRAFITO);
        });
    }

    @Test
    @Order(12)
    void capturaLaVentanaCompletaEnLosDosTemas() throws Exception {
        enEdt(() -> {
            controlador.establecerHerramienta(Herramienta.SELECCIONAR);
            simulacion.simular("1001");
            simulacion.irAPaso(1);
        });

        capturarVentana("ventana-grafito.png", Tema.GRAFITO);
        capturarVentana("ventana-papel.png", Tema.PAPEL);

        enEdt(() -> simulacion.fin());
        assertTrue(simulacion.hayVeredicto());
        capturarVentana("ventana-veredicto.png", Tema.GRAFITO);

        enEdt(() -> {
            simulacion.simular("0");
            simulacion.fin();
        });
        capturarVentana("ventana-rechazada.png", Tema.GRAFITO);

        assertTrue(SALIDA.resolve("ventana-grafito.png").toFile().length() > 0);
        assertTrue(SALIDA.resolve("ventana-papel.png").toFile().length() > 0);
        assertTrue(SALIDA.resolve("ventana-veredicto.png").toFile().length() > 0);
        assertTrue(SALIDA.resolve("ventana-rechazada.png").toFile().length() > 0);
    }

    @Test
    @Order(13)
    void capturaLaPestanaDeValidacionConHallazgos() throws Exception {
        enEdt(() -> {
            controlador.nuevo();
            controlador.agregarSimbolo("0");
            controlador.agregarSimbolo("1");
            controlador.establecerHerramienta(Herramienta.ESTADO);
            clic(300, 250);
            clic(600, 250);
            ventana.inspector().abrirValidacion();
        });

        assertFalse(controlador.validacionActual().esValido(),
                "Sin inicial ni δ total el AFD debe reportar errores");
        assertTrue(controlador.validacionActual()
                .contieneCodigo("E01_SIN_ESTADO_INICIAL"), "Nunca se asigna un inicial automático");
        assertTrue(controlador.puedeCompletarConEstadoTrampa());

        capturarVentana("ventana-validacion.png", Tema.GRAFITO);
        assertTrue(SALIDA.resolve("ventana-validacion.png").toFile().length() > 0);
    }

    @Test
    @Order(14)
    void capturaUnaEdicionEnCursoConFantasmaYSelector() throws Exception {
        enEdt(() -> {
            controlador.establecerHerramienta(Herramienta.TRANSICION);
            Point desde = lienzo.puntoDePantalla("q0");
            Point hasta = lienzo.puntoDePantalla("q1");
            despachar(MouseEvent.MOUSE_PRESSED, desde.x, desde.y);
            despachar(MouseEvent.MOUSE_DRAGGED, (desde.x + hasta.x) / 2, desde.y - 60);
        });
        capturarVentana("ventana-arrastre.png", Tema.GRAFITO);

        enEdt(() -> {
            Point hasta = lienzo.puntoDePantalla("q1");
            despachar(MouseEvent.MOUSE_RELEASED, hasta.x, hasta.y);
        });
        capturarVentana("ventana-selector.png", Tema.GRAFITO);
        enEdt(() -> assertNotNull(lienzo.selectorAbierto()));

        enEdt(() -> {
            lienzo.selectorAbierto().marcar('0', true);
            lienzo.selectorAbierto().aplicarSeleccion();
            controlador.establecerHerramienta(Herramienta.ESTADO);
            despachar(MouseEvent.MOUSE_MOVED, 450, 480);
        });
        capturarVentana("ventana-fantasma.png", Tema.GRAFITO);

        assertEquals("q1", controlador.modelo().destino("q0", '0'));
        assertTrue(SALIDA.resolve("ventana-arrastre.png").toFile().length() > 0);
        assertTrue(SALIDA.resolve("ventana-fantasma.png").toFile().length() > 0);
    }

    private static void capturarVentana(String archivo, Tema tema) throws Exception {
        enEdt(() -> Tema.establecer(tema));
        enEdt(() -> {
            ventana.invalidate();
            ventana.validate();
        });
        enEdt(() -> {
            java.awt.image.BufferedImage imagen = new java.awt.image.BufferedImage(
                    ventana.getWidth(), ventana.getHeight(), java.awt.image.BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D g2 = imagen.createGraphics();
            try {
                ventana.getContentPane().printAll(g2);
            } finally {
                g2.dispose();
            }
            try {
                javax.imageio.ImageIO.write(imagen, "png", SALIDA.resolve(archivo).toFile());
            } catch (java.io.IOException excepcion) {
                throw new IllegalStateException(excepcion);
            }
        });
    }

    private void verificarSincronizacionCuadruple() {
        var paso = simulacion.pasoActual();
        assertNotNull(paso, "Debe haber un paso actual");

        assertSame(buscarNodo(simulacion.estadoActual().nombre()), lienzo.nodoActivo(),
                "1. El lienzo resalta el estado del paso actual");

        assertNotNull(lienzo.aristaActiva(), "2. El lienzo resalta la arista recorrida");
        assertTrue(lienzo.aristaActiva().conecta(paso.estadoOrigen().nombre(),
                        paso.estadoDestino().nombre())
                        && lienzo.aristaActiva().transporta(paso.simbolo()),
                "2. La arista resaltada debe ser la del paso");

        assertEquals(paso.indice(), simulacion.paso(),
                "3. La cinta se dibuja a partir del mismo índice de paso");

        int fila = controlador.modelo().estados().indexOf(paso.estadoOrigen().nombre());
        int columna = controlador.modelo().simbolos().indexOf(paso.simbolo()) + 1;
        assertTrue(matriz.esCeldaEnCurso(fila, columna),
                "4. La matriz marca la celda (" + paso.estadoOrigen() + ", " + paso.simbolo() + ")");

        String rotulo = simulacion.descripcionPaso();
        assertTrue(rotulo.startsWith("Paso " + (simulacion.paso() + 1) + " de " + simulacion.totalPasos()),
                "El rótulo debe ir al mismo paso, y decía: " + rotulo);
        assertTrue(rotulo.contains("leo '" + paso.simbolo() + "' en " + paso.estadoOrigen().nombre()),
                "El rótulo debe describir el paso, y decía: " + rotulo);
    }

    private static com.unbosque.afd.desktop.render.NodoGrafico buscarNodo(String nombre) {
        return lienzo.nodos().stream()
                .filter(nodo -> nodo.nombre().equals(nombre))
                .findFirst()
                .orElse(null);
    }

    private static void clic(int x, int y) {
        despachar(MouseEvent.MOUSE_PRESSED, x, y);
        despachar(MouseEvent.MOUSE_RELEASED, x, y);
    }

    private static void clicEnEstado(String estado) {
        Point punto = lienzo.puntoDePantalla(estado);
        assertNotNull(punto, "No se encontró el estado " + estado);
        clic(punto.x, punto.y);
    }

    private static void arrastrarEntre(String origen, String destino) {
        Point desde = lienzo.puntoDePantalla(origen);
        Point hasta = lienzo.puntoDePantalla(destino);
        assertNotNull(desde);
        assertNotNull(hasta);
        despachar(MouseEvent.MOUSE_PRESSED, desde.x, desde.y);
        despachar(MouseEvent.MOUSE_DRAGGED, (desde.x + hasta.x) / 2, (desde.y + hasta.y) / 2);
        despachar(MouseEvent.MOUSE_RELEASED, hasta.x, hasta.y);
    }

    private static void aplicarSelector(Set<Character> simbolos) {
        SelectorSimbolos selector = lienzo.selectorAbierto();
        assertNotNull(selector, "El selector de símbolos debe abrirse al soltar sobre un estado");
        for (char simbolo : controlador.modelo().simbolos()) {
            selector.marcar(simbolo, simbolos.contains(simbolo));
        }
        selector.aplicarSeleccion();
    }

    private static void despachar(int tipo, int x, int y) {
        lienzo.dispatchEvent(new MouseEvent(lienzo, tipo, System.currentTimeMillis(), 0,
                x, y, tipo == MouseEvent.MOUSE_PRESSED || tipo == MouseEvent.MOUSE_RELEASED ? 1 : 0,
                false, MouseEvent.BUTTON1));
    }

    private static boolean esperando(Callable<Boolean> condicion) {
        try {
            AtomicReference<Boolean> valor = new AtomicReference<>();
            SwingUtilities.invokeAndWait(() -> {
                try {
                    valor.set(condicion.call());
                } catch (Exception excepcion) {
                    throw new IllegalStateException(excepcion);
                }
            });
            return Boolean.TRUE.equals(valor.get());
        } catch (InterruptedException excepcion) {
            Thread.currentThread().interrupt();
            return false;
        } catch (InvocationTargetException excepcion) {
            throw new IllegalStateException(excepcion.getCause());
        }
    }

    private static void enEdt(Runnable cuerpo) throws Exception {
        try {
            SwingUtilities.invokeAndWait(cuerpo);
        } catch (InvocationTargetException excepcion) {
            if (excepcion.getCause() instanceof AssertionError error) {
                throw error;
            }
            throw new IllegalStateException(excepcion.getCause());
        }
    }

    private static void assertNull(Object valor) {
        org.junit.jupiter.api.Assertions.assertNull(valor);
    }
}
