package com.unbosque.afd.desktop;

import com.unbosque.afd.core.logica.ConstructorEjemplos;
import com.unbosque.afd.desktop.controlador.ControladorAutomata;
import com.unbosque.afd.desktop.render.Tema;
import com.unbosque.afd.desktop.vista.LienzoAutomata;
import com.unbosque.afd.desktop.vista.VentanaPrincipal;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class EncuadreTest {

    private static final Path SALIDA = Path.of("target", "capturas");
    private static final int TOLERANCIA = 6;

    private static VentanaPrincipal ventana;
    private static LienzoAutomata lienzo;
    private static ControladorAutomata controlador;

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
        });
    }

    @AfterAll
    static void cerrarVentana() throws Exception {
        if (ventana != null) {
            enEdt(() -> ventana.dispose());
        }
    }

    @Test
    void centraElGrafoDeDosEstadosEnAmbosEjes() throws Exception {
        enEdt(() -> {
            controlador.cargarEjemplo(ConstructorEjemplos.cantidadParDeCeros());
            lienzo.reencuadrar();
        });

        File destino = SALIDA.resolve("encuadre-dos-estados.png").toFile();
        exportar(destino);
        BufferedImage imagen = ImageIO.read(destino);

        Margenes margenes = medir(imagen);
        int desbalanceHorizontal = Math.abs(margenes.izquierda - margenes.derecha);
        int desbalanceVertical = Math.abs(margenes.arriba - margenes.abajo);

        System.out.println("Encuadre 2 estados -> izq=" + margenes.izquierda
                + " der=" + margenes.derecha + " arr=" + margenes.arriba + " aba=" + margenes.abajo);

        assertTrue(desbalanceHorizontal <= TOLERANCIA,
                "Márgenes horizontales desiguales: izquierda=" + margenes.izquierda
                        + ", derecha=" + margenes.derecha);
        assertTrue(desbalanceVertical <= TOLERANCIA,
                "Márgenes verticales desiguales: arriba=" + margenes.arriba
                        + ", abajo=" + margenes.abajo);
    }

    @Test
    void reparteLosTresEstadosEnTrianguloConBaseHorizontal() throws Exception {
        enEdt(() -> {
            controlador.cargarEjemplo(ConstructorEjemplos.contieneSubcadena00());
            lienzo.reencuadrar();
        });

        File destino = SALIDA.resolve("encuadre-tres-estados.png").toFile();
        exportar(destino);
        Margenes margenes = medir(ImageIO.read(destino));

        System.out.println("Encuadre 3 estados -> izq=" + margenes.izquierda
                + " der=" + margenes.derecha + " arr=" + margenes.arriba + " aba=" + margenes.abajo);

        assertTrue(Math.abs(margenes.izquierda - margenes.derecha) <= TOLERANCIA,
                "Márgenes horizontales desiguales: izquierda=" + margenes.izquierda
                        + ", derecha=" + margenes.derecha);

        var posiciones = controlador.posiciones();
        double baseIzquierda = posiciones.get("qSinCero").y();
        double baseDerecha = posiciones.get("qEncontrado").y();
        double cima = posiciones.get("qUnCero").y();
        assertTrue(Math.abs(baseIzquierda - baseDerecha) < 0.5, "La base debe quedar horizontal");
        assertTrue(cima < baseIzquierda, "La cima debe quedar por encima de la base");
    }

    @Test
    void exportaElVeredictoComoAnilloSinTenirElNodo() throws Exception {
        enEdt(() -> {
            controlador.cargarEjemplo(ConstructorEjemplos.cantidadParDeCeros());
            lienzo.reencuadrar();
            ventana.simulacion().simular("1001");
            ventana.simulacion().fin();
        });
        assertTrue(ventana.simulacion().resultado().aceptada());

        for (Tema tema : new Tema[] {Tema.GRAFITO, Tema.PAPEL}) {
            String sufijo = tema.nombre().toLowerCase();
            exportarCon(SALIDA.resolve("veredicto-aceptada-" + sufijo + ".png").toFile(), tema);

            BufferedImage imagen = ImageIO.read(
                    SALIDA.resolve("veredicto-aceptada-" + sufijo + ".png").toFile());
            assertTrue(contiene(imagen, Tema.ACEPTADA), "Debe verse el anillo verde de aceptada");
            assertTrue(contiene(imagen, Tema.INICIAL), "La flecha de estado inicial debe seguir visible");
            assertTrue(contiene(imagen, Tema.ACEPTACION), "El doble círculo debe seguir visible");
            assertTrue(contiene(imagen, tema.estadoRelleno()),
                    "El relleno del nodo no puede quedar teñido por el veredicto");
        }

        enEdt(() -> {
            ventana.simulacion().simular("0");
            ventana.simulacion().fin();
        });
        for (Tema tema : new Tema[] {Tema.GRAFITO, Tema.PAPEL}) {
            String sufijo = tema.nombre().toLowerCase();
            exportarCon(SALIDA.resolve("veredicto-rechazada-" + sufijo + ".png").toFile(), tema);
            BufferedImage imagen = ImageIO.read(
                    SALIDA.resolve("veredicto-rechazada-" + sufijo + ".png").toFile());
            assertTrue(contiene(imagen, Tema.RECHAZADA), "Debe verse el anillo rojo de rechazada");
            assertTrue(contiene(imagen, Tema.INICIAL), "La flecha de estado inicial debe seguir visible");
        }
        enEdt(() -> Tema.establecer(Tema.GRAFITO));
    }

    private static boolean contiene(BufferedImage imagen, java.awt.Color color) {
        for (int y = 0; y < imagen.getHeight(); y++) {
            for (int x = 0; x < imagen.getWidth(); x++) {
                if (distancia(imagen.getRGB(x, y), color.getRGB()) <= 24) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void exportarCon(File destino, Tema tema) throws Exception {
        enEdt(() -> {
            Tema.establecer(tema);
            try {
                lienzo.exportarPNG(destino, 1.0, false);
            } catch (Exception excepcion) {
                throw new IllegalStateException(excepcion);
            }
        });
    }

    private record Margenes(int izquierda, int derecha, int arriba, int abajo) {
    }

    private static Margenes medir(BufferedImage imagen) {
        int fondo = Tema.GRAFITO.lienzoFondo().getRGB();
        int izquierda = imagen.getWidth();
        int derecha = imagen.getWidth();
        int arriba = imagen.getHeight();
        int abajo = imagen.getHeight();

        for (int x = 0; x < imagen.getWidth(); x++) {
            if (columnaConContenido(imagen, x, fondo)) {
                izquierda = x;
                break;
            }
        }
        for (int x = imagen.getWidth() - 1; x >= 0; x--) {
            if (columnaConContenido(imagen, x, fondo)) {
                derecha = imagen.getWidth() - 1 - x;
                break;
            }
        }
        for (int y = 0; y < imagen.getHeight(); y++) {
            if (filaConContenido(imagen, y, fondo)) {
                arriba = y;
                break;
            }
        }
        for (int y = imagen.getHeight() - 1; y >= 0; y--) {
            if (filaConContenido(imagen, y, fondo)) {
                abajo = imagen.getHeight() - 1 - y;
                break;
            }
        }
        return new Margenes(izquierda, derecha, arriba, abajo);
    }

    private static boolean columnaConContenido(BufferedImage imagen, int x, int fondo) {
        for (int y = 0; y < imagen.getHeight(); y++) {
            if (esContenido(imagen.getRGB(x, y), fondo)) {
                return true;
            }
        }
        return false;
    }

    private static boolean filaConContenido(BufferedImage imagen, int y, int fondo) {
        for (int x = 0; x < imagen.getWidth(); x++) {
            if (esContenido(imagen.getRGB(x, y), fondo)) {
                return true;
            }
        }
        return false;
    }

    private static boolean esContenido(int pixel, int fondo) {
        // La retícula del lienzo también es "fondo" para efectos de medir el encuadre.
        return distancia(pixel, fondo) > 40
                && distancia(pixel, Tema.GRAFITO.lienzoReticula().getRGB()) > 40;
    }

    private static int distancia(int pixel, int referencia) {
        return Math.abs((pixel >> 16 & 0xFF) - (referencia >> 16 & 0xFF))
                + Math.abs((pixel >> 8 & 0xFF) - (referencia >> 8 & 0xFF))
                + Math.abs((pixel & 0xFF) - (referencia & 0xFF));
    }

    private static void exportar(File destino) throws Exception {
        enEdt(() -> {
            try {
                lienzo.exportarPNG(destino, 1.0, false);
            } catch (Exception excepcion) {
                throw new IllegalStateException(excepcion);
            }
        });
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
}
