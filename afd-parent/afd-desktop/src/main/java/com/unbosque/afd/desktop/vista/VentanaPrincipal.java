package com.unbosque.afd.desktop.vista;

import com.unbosque.afd.core.logica.ConstructorEjemplos;
import com.unbosque.afd.core.modelo.AutomataFinitoDeterminista;
import com.unbosque.afd.desktop.controlador.ControladorAutomata;
import com.unbosque.afd.desktop.controlador.ControladorSimulacion;
import com.unbosque.afd.desktop.render.Medidas;
import com.unbosque.afd.desktop.render.Tema;
import com.unbosque.afd.desktop.render.TipografiaApp;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.function.Supplier;

public class VentanaPrincipal extends JFrame {

    private static final String TITULO = "Plano Sintáctico — Simulador de AFD";

    private final ControladorAutomata controlador = new ControladorAutomata();
    private final ControladorSimulacion simulacion = new ControladorSimulacion(controlador);
    private final LienzoAutomata lienzo = new LienzoAutomata(controlador);
    private final PanelMatrizTransiciones panelMatriz = new PanelMatrizTransiciones(controlador);
    private final OverlayMatriz overlayMatriz;
    private final Inspector inspector = new Inspector(controlador);
    private final CajonEjecucion cajon;
    private final BarraSuperior barraSuperior;

    public VentanaPrincipal() {
        super(TITULO);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1440, 900);
        setMinimumSize(new Dimension(1100, 700));
        setLocationRelativeTo(null);

        overlayMatriz = new OverlayMatriz(panelMatriz, this::alternarMatriz);
        cajon = new CajonEjecucion(controlador, simulacion);
        barraSuperior = new BarraSuperior(controlador,
                inspector::abrirValidacion, this::alternarTema, this::exportarPNG, construirMenu());

        controlador.establecerCancelacionDeSimulacion(simulacion::cancelar);
        lienzo.establecerSimulacion(simulacion);
        lienzo.establecerAlternarMatriz(this::alternarMatriz);
        lienzo.establecerAlternarCajon(cajon::alternarColapso);
        panelMatriz.establecerSimulacion(simulacion);

        setContentPane(construirContenido());
        instalarAtajosGlobales();

        Tema.agregarObservador(this::alRefrescarTema);
        lienzo.agregarSuperposicion(overlayMatriz);
        lienzo.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent evento) {
                overlayMatriz.acomodarEnPadre();
            }
        });

        SwingUtilities.invokeLater(() -> {
            overlayMatriz.ubicarEnEsquina();
            lienzo.requestFocusInWindow();
        });
    }

    public ControladorAutomata controlador() {
        return controlador;
    }

    public ControladorSimulacion simulacion() {
        return simulacion;
    }

    public LienzoAutomata lienzo() {
        return lienzo;
    }

    public Inspector inspector() {
        return inspector;
    }

    public CajonEjecucion cajon() {
        return cajon;
    }

    public PanelMatrizTransiciones panelMatriz() {
        return panelMatriz;
    }

    public OverlayMatriz overlayMatriz() {
        return overlayMatriz;
    }

    private JComponent construirContenido() {
        JPanel raiz = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(Tema.actual().panelFondo());
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };

        JPanel centro = new JPanel(new BorderLayout());
        centro.setOpaque(false);
        centro.add(new RielHerramientas(controlador, lienzo), BorderLayout.WEST);
        centro.add(lienzo, BorderLayout.CENTER);
        centro.add(inspector, BorderLayout.EAST);

        raiz.add(barraSuperior, BorderLayout.NORTH);
        raiz.add(centro, BorderLayout.CENTER);
        raiz.add(cajon, BorderLayout.SOUTH);
        return raiz;
    }

    private void alternarMatriz() {
        overlayMatriz.setVisible(!overlayMatriz.isVisible());
        if (overlayMatriz.isVisible()) {
            overlayMatriz.acomodarEnPadre();
        }
        lienzo.repaint();
    }

    private void alternarTema() {
        Tema.alternar();
    }

    private void alRefrescarTema() {
        aplicarColoresDeMenu();
        Tema.refrescarArbol(getContentPane());
        inspector.refrescar();
        barraSuperior.refrescar();
        repaint();
    }

    private void aplicarColoresDeMenu() {
        Tema tema = Tema.actual();
        javax.swing.UIManager.put("MenuItem.background", tema.panelElevado());
        javax.swing.UIManager.put("MenuItem.foreground", tema.textoPrimario());
        javax.swing.UIManager.put("MenuItem.selectionBackground", Tema.mezclar(Tema.ACTIVO,
                tema.panelElevado(), 0.30));
        javax.swing.UIManager.put("MenuItem.selectionForeground", tema.textoPrimario());
        javax.swing.UIManager.put("PopupMenu.background", tema.panelElevado());
        javax.swing.UIManager.put("PopupMenu.foreground", tema.textoPrimario());
        javax.swing.UIManager.put("Menu.background", tema.panelFondo());
        javax.swing.UIManager.put("Menu.foreground", tema.textoSecundario());
        javax.swing.UIManager.put("Menu.selectionBackground", tema.sobrevuelo());
        javax.swing.UIManager.put("Menu.selectionForeground", tema.textoPrimario());
        javax.swing.UIManager.put("ToolTip.background", tema.panelElevado());
        javax.swing.UIManager.put("ToolTip.foreground", tema.textoPrimario());
        javax.swing.UIManager.put("OptionPane.background", tema.panelFondo());
        javax.swing.UIManager.put("OptionPane.messageForeground", tema.textoPrimario());
        javax.swing.UIManager.put("Panel.background", tema.panelFondo());
    }

    private JMenuBar construirMenu() {
        aplicarColoresDeMenu();
        JMenuBar barra = new JMenuBar();
        barra.setOpaque(false);
        barra.setBorder(BorderFactory.createEmptyBorder());

        JMenu archivo = menu("Archivo", KeyEvent.VK_A);
        archivo.add(elemento("Nuevo", evento -> nuevo()));
        archivo.add(elemento("Exportar PNG...", evento -> exportarPNG()));
        archivo.addSeparator();
        archivo.add(elemento("Salir", evento -> dispose()));
        barra.add(archivo);

        JMenu ejemplos = menu("Ejemplos", KeyEvent.VK_J);
        ejemplos.add(ejemplo("Cantidad par de ceros", ConstructorEjemplos::cantidadParDeCeros));
        ejemplos.add(ejemplo("Contiene la subcadena 00", ConstructorEjemplos::contieneSubcadena00));
        ejemplos.add(ejemplo("Cantidad par de unos", ConstructorEjemplos::cantidadParDeUnos));
        ejemplos.add(ejemplo("Termina en cero", ConstructorEjemplos::terminaEnCero));
        barra.add(ejemplos);

        JMenu vista = menu("Vista", KeyEvent.VK_V);
        vista.add(elemento("Ajustar a la vista (F)", evento -> lienzo.reencuadrar()));
        vista.add(elemento("Acercar", evento -> lienzo.acercar()));
        vista.add(elemento("Alejar", evento -> lienzo.alejar()));
        vista.addSeparator();
        vista.add(elemento("Matriz de transiciones (M)", evento -> alternarMatriz()));
        vista.add(elemento("Cajón de ejecución (Espacio)", evento -> cajon.alternarColapso()));
        vista.addSeparator();
        vista.add(elemento("Tema Grafito", evento -> Tema.establecer(Tema.GRAFITO)));
        vista.add(elemento("Tema Papel", evento -> Tema.establecer(Tema.PAPEL)));
        barra.add(vista);

        JMenu ayuda = menu("Ayuda", KeyEvent.VK_Y);
        ayuda.add(elemento("Atajos de teclado", evento -> mostrarAtajos()));
        ayuda.add(elemento("Acerca de", evento -> mostrarAcercaDe()));
        barra.add(ayuda);

        return barra;
    }

    private static JMenu menu(String texto, int mnemonico) {
        JMenu menu = new JMenu(texto);
        menu.setMnemonic(mnemonico);
        menu.setFont(TipografiaApp.ETIQUETA);
        menu.setForeground(Tema.actual().textoSecundario());
        menu.setOpaque(false);
        menu.setBorder(BorderFactory.createEmptyBorder(0, Medidas.paso(2), 0, Medidas.paso(2)));
        return menu;
    }

    private static JMenuItem elemento(String texto, java.awt.event.ActionListener accion) {
        JMenuItem item = new JMenuItem(texto);
        item.setFont(TipografiaApp.CUERPO);
        item.addActionListener(accion);
        return item;
    }

    private JMenuItem ejemplo(String texto, Supplier<AutomataFinitoDeterminista> proveedor) {
        JMenuItem item = new JMenuItem(texto);
        item.setFont(TipografiaApp.CUERPO);
        item.addActionListener(evento -> controlador.cargarEjemplo(proveedor.get()));
        return item;
    }

    private void instalarAtajosGlobales() {
        JComponent raiz = (JComponent) getContentPane();
        int atajo = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

        raiz.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_N, atajo), "nuevo");
        raiz.getActionMap().put("nuevo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent evento) {
                nuevo();
            }
        });

        raiz.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_E, atajo), "exportar");
        raiz.getActionMap().put("exportar", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent evento) {
                exportarPNG();
            }
        });
    }

    private void nuevo() {
        int respuesta = JOptionPane.showConfirmDialog(this,
                "Se descartará el autómata en edición. ¿Continuar?",
                "Nuevo autómata", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (respuesta == JOptionPane.OK_OPTION) {
            controlador.nuevo();
        }
    }

    private void exportarPNG() {
        JCheckBox altaResolucion = new JCheckBox("Alta resolución (2x) para el informe", true);
        JCheckBox forzarPapel = new JCheckBox("Forzar tema Papel (fondo claro)", Tema.actual().esOscuro());
        altaResolucion.setFont(TipografiaApp.CUERPO);
        forzarPapel.setFont(TipografiaApp.CUERPO);

        JPanel opciones = new JPanel();
        opciones.setLayout(new javax.swing.BoxLayout(opciones, javax.swing.BoxLayout.Y_AXIS));
        opciones.add(altaResolucion);
        opciones.add(forzarPapel);

        JFileChooser selector = new JFileChooser();
        selector.setDialogTitle("Exportar el diagrama a PNG");
        selector.setSelectedFile(new File("automata.png"));
        selector.setFileFilter(new FileNameExtensionFilter("Imagen PNG", "png"));
        selector.setAccessory(opciones);
        if (selector.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File destino = selector.getSelectedFile();
        if (!destino.getName().toLowerCase().endsWith(".png")) {
            destino = new File(destino.getParentFile(), destino.getName() + ".png");
        }
        try {
            lienzo.exportarPNG(destino, altaResolucion.isSelected() ? 2.0 : 1.0, forzarPapel.isSelected());
            JOptionPane.showMessageDialog(this, "Diagrama exportado en:\n" + destino.getAbsolutePath(),
                    "Exportar PNG", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException excepcion) {
            JOptionPane.showMessageDialog(this, "No se pudo exportar la imagen:\n" + excepcion.getMessage(),
                    "Exportar PNG", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void mostrarAtajos() {
        JOptionPane.showMessageDialog(this,
                """
                Herramientas
                  V  Seleccionar        E  Estado
                  A  Aceptación         I  Inicial
                  T  Transición         D  Borrar
                  H  Mano

                Vista
                  F         Ajustar a la vista
                  M         Mostrar u ocultar la matriz
                  Espacio   Contraer el cajón (sostenido: paneo)
                  Rueda     Zoom centrado en el cursor

                Edición
                  Ctrl+Z         Deshacer
                  Ctrl+Shift+Z   Rehacer
                  Supr           Borrar la selección""",
                "Atajos de teclado", JOptionPane.INFORMATION_MESSAGE);
    }

    private void mostrarAcercaDe() {
        JOptionPane.showMessageDialog(this,
                TITULO + "\n\nUniversidad El Bosque — Compiladores\n"
                        + "Edición directa sobre el lienzo, validación en vivo\n"
                        + "y simulación paso a paso sincronizada.",
                "Acerca de", JOptionPane.INFORMATION_MESSAGE);
    }
}
