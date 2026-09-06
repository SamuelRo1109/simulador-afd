# Plano Sintáctico — Simulador de Autómatas Finitos Deterministas

Editor visual y simulador de AFD desarrollado para la materia Compiladores
de la Universidad El Bosque.

## Características

- Construcción de autómatas mediante edición directa sobre el lienzo
- Renderizado propio en Java2D: sin librerías de grafos ni de autómatas
- Validación de la quíntupla M = (Q, Σ, δ, q0, F), incluyendo determinismo
  y totalidad de la función de transición
- Simulación de cadenas paso a paso con visualización sincronizada
- Exportación del autómata a PNG y de la traza a texto

## Estructura

- `afd-core` — lógica del autómata en Java puro, sin dependencias de interfaz
- `afd-desktop` — interfaz Swing / Java2D

## Ejecución

    mvn clean package
    java -jar afd-parent/afd-desktop/target/afd-desktop-1.0.0.jar

Requiere JDK 17 o superior.

## Autor

Samuel Julián Rodríguez Chávez