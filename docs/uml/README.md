# Diagramas UML — Plano Sintáctico

Documentación UML del simulador de Autómatas Finitos Deterministas.
Universidad El Bosque — Compiladores.

Todos los diagramas están **derivados del código fuente real**: nombres de
clases, atributos, métodos, multiplicidades y flujos corresponden a lo que
hay en `afd-parent/`, no a una idealización del diseño.

## Los siete diagramas

| Archivo | Qué muestra |
|---|---|
| [`clases-core.puml`](clases-core.puml) | Modelo formal del AFD: la quíntupla M = (Q, Σ, δ, q₀, F), sus records inmutables y las cuatro clases utilitarias de lógica. |
| [`clases-desktop.puml`](clases-desktop.puml) | Interfaz Swing organizada en MVC, con el patrón Comando del deshacer y las dependencias hacia `afd-core`. |
| [`paquetes.puml`](paquetes.puml) | Los dos módulos Maven y la dependencia en un solo sentido: `afd-desktop → afd-core`, nunca al revés. |
| [`casos-de-uso.puml`](casos-de-uso.puml) | Lo que el usuario puede hacer, agrupado en definición, validación, simulación, vista y salida. |
| [`secuencia-validar-cadena.puml`](secuencia-validar-cadena.puml) | Flujo completo de simular una cadena: cálculo único de la traza y sincronización cuádruple lienzo–cinta–matriz–rótulo. |
| [`secuencia-crear-transicion.puml`](secuencia-crear-transicion.puml) | Arrastrar una transición y la verificación de determinismo que impide crear el conflicto en silencio. |
| [`secuencia-insertar-estado.puml`](secuencia-insertar-estado.puml) | Insertar un estado con la herramienta ESTADO: vista previa fantasma, nombre autogenerado y edición en el sitio. |

Las imágenes ya renderizadas están en [`png/`](png/).

## Cómo renderizarlos

### Opción 1 — Visual Studio Code

Instala la extensión **PlantUML** (jebbs.plantuml) y pulsa `Alt+D` sobre
cualquier `.puml` para la vista previa.

### Opción 2 — Línea de comandos

El proyecto ya usa Maven, así que la forma más simple de obtener PlantUML es
pedírselo a Maven y ejecutar el jar:

```bash
mvn dependency:get -Dartifact=net.sourceforge.plantuml:plantuml:1.2024.7

java -jar ~/.m2/repository/net/sourceforge/plantuml/plantuml/1.2024.7/plantuml-1.2024.7.jar \
     -tpng -o png docs/uml/*.puml
```

Para SVG (mejor calidad de impresión en el informe) cambia `-tpng` por `-tsvg`.

### Opción 3 — Servidor web

Pega el contenido de cualquier `.puml` en <https://www.plantuml.com/plantuml>.

> **Graphviz no es necesario.** PlantUML 1.2024.7 resuelve el trazado de los
> diagramas de clases, paquetes y casos de uso con su motor interno *Smetana*
> cuando no encuentra `dot`. Los siete archivos se verificaron sin Graphviz
> instalado.

## Convenciones usadas

- Nombres de clases, métodos y atributos **exactamente** como en el código,
  en español.
- `<<record>>` marca los `record` de Java; `<<utility>>` las clases `final` con
  constructor privado y solo miembros estáticos.
- Rombo lleno (composición) donde el contenedor define el ciclo de vida del
  contenido; rombo hueco o flecha simple (agregación/asociación) donde el
  objeto se comparte o puede ser nulo.
- Fondo blanco y paleta neutra: pensados para imprimirse en el informe.

## Nota sobre las multiplicidades del núcleo

`AutomataFinitoDeterminista` admite **cero** estados y **cero o un** estado
inicial. No es un descuido del diagrama: la aplicación arranca precisamente con
un autómata vacío, y `ValidadorAutomata` es quien reporta `E01_SIN_ESTADO_INICIAL`
y `E05_ALFABETO_VACIO` mientras el usuario lo construye. Por eso las
multiplicidades son `0..*` y `0..1`, y no `1..*` y `1`.
