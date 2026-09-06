package com.unbosque.afd.desktop.render;

import com.unbosque.afd.core.modelo.AutomataFinitoDeterminista;
import com.unbosque.afd.core.modelo.Transicion;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.QuadCurve2D;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class CalculadoraGeometria {

    public static final double LARGO_PUNTA = 14;
    public static final double APERTURA_PUNTA_GRADOS = 25;
    public static final double DESPLAZAMIENTO_CURVA_MINIMO = 45;
    public static final double FACTOR_DESPLAZAMIENTO_CURVA = 0.6;
    public static final double DESPLAZAMIENTO_ETIQUETA = 16;
    public static final double LARGO_FLECHA_INICIAL = 35;

    private static final double APERTURA_BUCLE_GRADOS = 32;
    private static final double APERTURA_CONTROL_BUCLE_GRADOS = 50;
    private static final double ALCANCE_BUCLE = 4.0;
    private static final int ITERACIONES_BISECCION = 60;
    private static final double ANGULO_ARRIBA = -Math.PI / 2;
    private static final double EPSILON = 1e-9;

    private record Par(String origen, String destino) {
    }

    private CalculadoraGeometria() {
    }

    public static List<AristaGrafica> calcularAristas(AutomataFinitoDeterminista automata,
                                                     List<NodoGrafico> nodos) {
        Objects.requireNonNull(automata, "El autómata no puede ser nulo");
        Objects.requireNonNull(nodos, "Los nodos no pueden ser nulos");

        Map<String, NodoGrafico> nodosPorNombre = new LinkedHashMap<>();
        for (NodoGrafico nodo : nodos) {
            nodosPorNombre.put(nodo.nombre(), nodo);
        }

        Map<Par, List<Character>> agrupadas = agrupar(automata, nodosPorNombre);

        List<AristaGrafica> aristas = new ArrayList<>(agrupadas.size());
        for (Map.Entry<Par, List<Character>> entrada : agrupadas.entrySet()) {
            Par par = entrada.getKey();
            NodoGrafico origen = nodosPorNombre.get(par.origen());
            NodoGrafico destino = nodosPorNombre.get(par.destino());

            if (par.origen().equals(par.destino())) {
                aristas.add(construirBucle(origen, entrada.getValue(), nodos));
            } else if (agrupadas.containsKey(new Par(par.destino(), par.origen()))) {
                aristas.add(construirCurva(origen, destino, entrada.getValue()));
            } else {
                aristas.add(construirRecta(origen, destino, entrada.getValue()));
            }
        }
        return List.copyOf(aristas);
    }

    private static Map<Par, List<Character>> agrupar(AutomataFinitoDeterminista automata,
                                                     Map<String, NodoGrafico> nodosPorNombre) {
        Map<Par, List<Character>> agrupadas = new LinkedHashMap<>();
        for (Transicion transicion : automata.funcionTransicion().transiciones()) {
            String origen = transicion.estadoOrigen().nombre();
            String destino = transicion.estadoDestino().nombre();
            if (!nodosPorNombre.containsKey(origen) || !nodosPorNombre.containsKey(destino)) {
                continue;
            }
            List<Character> simbolos = agrupadas.computeIfAbsent(new Par(origen, destino), par -> new ArrayList<>());
            if (!simbolos.contains(transicion.simbolo())) {
                simbolos.add(transicion.simbolo());
            }
        }
        return agrupadas;
    }

    private static AristaGrafica construirRecta(NodoGrafico origen, NodoGrafico destino, List<Character> simbolos) {
        double dx = destino.x() - origen.x();
        double dy = destino.y() - origen.y();
        double distancia = Math.hypot(dx, dy);
        if (distancia < EPSILON) {
            return construirCurva(origen, destino, simbolos);
        }

        double ux = dx / distancia;
        double uy = dy / distancia;
        Point2D.Double inicio = new Point2D.Double(
                origen.x() + ux * origen.radio(), origen.y() + uy * origen.radio());
        Point2D.Double fin = new Point2D.Double(
                destino.x() - ux * destino.radio(), destino.y() - uy * destino.radio());

        Shape forma = new Line2D.Double(inicio, fin);
        Shape punta = puntaDeFlecha(fin, Math.atan2(uy, ux));

        Point2D.Double medio = new Point2D.Double((inicio.x + fin.x) / 2, (inicio.y + fin.y) / 2);
        Point2D etiqueta = desplazarSobreNormal(medio, ux, uy, DESPLAZAMIENTO_ETIQUETA);

        return new AristaGrafica(origen, destino, simbolos, TipoArista.RECTA, forma, punta, etiqueta);
    }

    private static AristaGrafica construirCurva(NodoGrafico origen, NodoGrafico destino, List<Character> simbolos) {
        double dx = destino.x() - origen.x();
        double dy = destino.y() - origen.y();
        double distancia = Math.hypot(dx, dy);
        double ux = distancia < EPSILON ? 1 : dx / distancia;
        double uy = distancia < EPSILON ? 0 : dy / distancia;

        Point2D.Double medioRecto = new Point2D.Double((origen.x() + destino.x()) / 2, (origen.y() + destino.y()) / 2);
        Point2D control = desplazarSobreNormal(medioRecto, ux, uy, desplazamientoCurva(origen, destino));

        QuadCurve2D.Double completa = new QuadCurve2D.Double(
                origen.x(), origen.y(), control.getX(), control.getY(), destino.x(), destino.y());

        QuadCurve2D recortada = recortarContraNodos(completa, origen, destino);

        Point2D fin = new Point2D.Double(recortada.getX2(), recortada.getY2());
        double tangente = Math.atan2(recortada.getY2() - recortada.getCtrlY(), recortada.getX2() - recortada.getCtrlX());
        Shape punta = puntaDeFlecha(fin, tangente);

        Point2D.Double medioCurva = puntoEnCurva(recortada, 0.5);
        Point2D etiqueta = desplazarSobreNormal(medioCurva, ux, uy, DESPLAZAMIENTO_ETIQUETA);

        return new AristaGrafica(origen, destino, simbolos, TipoArista.CURVA, recortada, punta, etiqueta);
    }

    private static AristaGrafica construirBucle(NodoGrafico nodo, List<Character> simbolos, List<NodoGrafico> nodos) {
        double apertura = Math.toRadians(APERTURA_BUCLE_GRADOS);
        double aperturaControl = Math.toRadians(APERTURA_CONTROL_BUCLE_GRADOS);
        double alcance = nodo.radio() * ALCANCE_BUCLE;

        Point2D.Double inicio = enPolar(ANGULO_ARRIBA - apertura, nodo.radio());
        Point2D.Double fin = enPolar(ANGULO_ARRIBA + apertura, nodo.radio());
        Point2D.Double control1 = enPolar(ANGULO_ARRIBA - aperturaControl, alcance);
        Point2D.Double control2 = enPolar(ANGULO_ARRIBA + aperturaControl, alcance);

        Path2D.Double formaLocal = new Path2D.Double();
        formaLocal.moveTo(inicio.x, inicio.y);
        formaLocal.curveTo(control1.x, control1.y, control2.x, control2.y, fin.x, fin.y);

        Shape puntaLocal = puntaDeFlecha(fin, Math.atan2(fin.y - control2.y, fin.x - control2.x));

        Point2D.Double cima = new Point2D.Double(
                (inicio.x + 3 * control1.x + 3 * control2.x + fin.x) / 8,
                (inicio.y + 3 * control1.y + 3 * control2.y + fin.y) / 8);
        Point2D.Double etiquetaLocal = new Point2D.Double(cima.x, cima.y - DESPLAZAMIENTO_ETIQUETA);

        double[] direccion = direccionOpuestaAlCentroide(nodo, nodos);
        AffineTransform orientacion = AffineTransform.getTranslateInstance(nodo.x(), nodo.y());
        orientacion.rotate(Math.atan2(direccion[1], direccion[0]) - ANGULO_ARRIBA);

        return new AristaGrafica(nodo, nodo, simbolos, TipoArista.BUCLE,
                orientacion.createTransformedShape(formaLocal),
                orientacion.createTransformedShape(puntaLocal),
                orientacion.transform(etiquetaLocal, null));
    }

    public static double desplazamientoCurva(NodoGrafico origen, NodoGrafico destino) {
        return Math.max(DESPLAZAMIENTO_CURVA_MINIMO,
                FACTOR_DESPLAZAMIENTO_CURVA * (origen.radio() + destino.radio()) / 2);
    }

    public static Shape puntaDeFlecha(Point2D punta, double anguloTangente) {
        Objects.requireNonNull(punta, "El punto de la punta no puede ser nulo");
        double desplazamiento = LARGO_PUNTA * Math.tan(Math.toRadians(APERTURA_PUNTA_GRADOS / 2));

        Path2D.Double triangulo = new Path2D.Double();
        triangulo.moveTo(0, 0);
        triangulo.lineTo(-LARGO_PUNTA, -desplazamiento);
        triangulo.lineTo(-LARGO_PUNTA, desplazamiento);
        triangulo.closePath();

        AffineTransform transformacion = AffineTransform.getTranslateInstance(punta.getX(), punta.getY());
        transformacion.rotate(anguloTangente);
        return transformacion.createTransformedShape(triangulo);
    }

    public static Line2D lineaEstadoInicial(NodoGrafico nodo) {
        return lineaEstadoInicial(nodo, 0);
    }

    public static Line2D lineaEstadoInicial(NodoGrafico nodo, double desplazamiento) {
        Objects.requireNonNull(nodo, "El nodo no puede ser nulo");
        double borde = nodo.x() - nodo.radio() - desplazamiento;
        return new Line2D.Double(borde - LARGO_FLECHA_INICIAL, nodo.y(), borde, nodo.y());
    }

    public static Shape puntaEstadoInicial(NodoGrafico nodo) {
        return puntaEstadoInicial(nodo, 0);
    }

    public static Shape puntaEstadoInicial(NodoGrafico nodo, double desplazamiento) {
        Objects.requireNonNull(nodo, "El nodo no puede ser nulo");
        return puntaDeFlecha(
                new Point2D.Double(nodo.x() - nodo.radio() - desplazamiento, nodo.y()), 0);
    }

    private static double[] direccionOpuestaAlCentroide(NodoGrafico nodo, List<NodoGrafico> nodos) {
        double sumaX = 0;
        double sumaY = 0;
        int cantidad = 0;
        for (NodoGrafico otro : nodos) {
            if (otro != nodo && !otro.nombre().equals(nodo.nombre())) {
                sumaX += otro.x();
                sumaY += otro.y();
                cantidad++;
            }
        }
        if (cantidad == 0) {
            return new double[] {0, -1};
        }
        double dx = nodo.x() - sumaX / cantidad;
        double dy = nodo.y() - sumaY / cantidad;
        double norma = Math.hypot(dx, dy);
        if (norma < EPSILON) {
            return new double[] {0, -1};
        }
        // El bucle de un estado inicial no puede caer sobre su flecha de entrada, que llega
        // siempre por la izquierda: en ese caso lo mandamos hacia arriba.
        if (nodo.estado().esInicial() && dx < 0) {
            return new double[] {0, -1};
        }
        return new double[] {dx / norma, dy / norma};
    }

    private static Point2D.Double enPolar(double angulo, double distancia) {
        return new Point2D.Double(distancia * Math.cos(angulo), distancia * Math.sin(angulo));
    }

    private static Point2D desplazarSobreNormal(Point2D punto, double ux, double uy, double distancia) {
        return new Point2D.Double(punto.getX() - uy * distancia, punto.getY() + ux * distancia);
    }

    private static QuadCurve2D recortarContraNodos(QuadCurve2D curva, NodoGrafico origen, NodoGrafico destino) {
        double separacion = origen.centro().distance(destino.centro());
        if (separacion <= origen.radio() + destino.radio()) {
            return curva;
        }
        double inicio = parametroEnCircunferencia(curva, origen.centro(), origen.radio(), 0, 1);
        double fin = parametroEnCircunferencia(curva, destino.centro(), destino.radio(), 1, 0);
        if (fin - inicio < EPSILON) {
            return curva;
        }
        return recortarCurva(curva, inicio, fin);
    }

    private static double parametroEnCircunferencia(QuadCurve2D curva, Point2D centro, double radio,
                                                    double parametroDentro, double parametroFuera) {
        double dentro = parametroDentro;
        double fuera = parametroFuera;
        for (int iteracion = 0; iteracion < ITERACIONES_BISECCION; iteracion++) {
            double medio = (dentro + fuera) / 2;
            if (puntoEnCurva(curva, medio).distance(centro) < radio) {
                dentro = medio;
            } else {
                fuera = medio;
            }
        }
        return (dentro + fuera) / 2;
    }

    private static QuadCurve2D recortarCurva(QuadCurve2D curva, double inicio, double fin) {
        Point2D.Double desde = puntoEnCurva(curva, inicio);
        Point2D.Double hasta = puntoEnCurva(curva, fin);
        double pesoOrigen = (1 - inicio) * (1 - fin);
        double pesoControl = inicio * (1 - fin) + fin * (1 - inicio);
        double pesoDestino = inicio * fin;
        double controlX = pesoOrigen * curva.getX1() + pesoControl * curva.getCtrlX() + pesoDestino * curva.getX2();
        double controlY = pesoOrigen * curva.getY1() + pesoControl * curva.getCtrlY() + pesoDestino * curva.getY2();
        return new QuadCurve2D.Double(desde.x, desde.y, controlX, controlY, hasta.x, hasta.y);
    }

    private static Point2D.Double puntoEnCurva(QuadCurve2D curva, double parametro) {
        double complemento = 1 - parametro;
        double x = complemento * complemento * curva.getX1()
                + 2 * complemento * parametro * curva.getCtrlX()
                + parametro * parametro * curva.getX2();
        double y = complemento * complemento * curva.getY1()
                + 2 * complemento * parametro * curva.getCtrlY()
                + parametro * parametro * curva.getY2();
        return new Point2D.Double(x, y);
    }
}
