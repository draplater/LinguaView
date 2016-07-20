package LinguaView

import net.sf.epsgraphics.ColorMode
import net.sf.epsgraphics.EpsGraphics
import java.awt._
import javax.swing._
import java.awt.event._
import java.io._
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util._
import javax.swing.border.EmptyBorder
import javax.swing.event._
import javax.swing.undo.UndoManager
import javax.swing.undo.UndoableEdit
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.w3c.dom.Text
import org.xml.sax.InputSource
import _root_.LinguaView.syntax._
import _root_.LinguaView.UIutils._
import scala.util.control.Breaks._
import utils.UI._

/**
  * LinguaView is the top-most class in the whole class hierarchy
  * It constructs the top UI object and starts the whole program.
  *
  * @author shuoyang
  */
object LinguaView {
  private[LinguaView] var filename: String = null

  def main(args: Array[String]) {
    Locale.setDefault(Locale.ENGLISH)
    if (args.length >= 1) {
      if (args(0) == "export") {
        if (args.length < 3) {
          System.out.println("Usage: LinguaView.jar export input.xml output.eps/svg")
          System.exit(2)
        }
        filename = args(1)
        val frame: TabbedPaneFrame = new TabbedPaneFrame
        if (filename != null) {
          frame.importFromFile(filename)
        }
        val dim: Dimension = frame.LFGcomponent.getDimension
        try {
          if(args(2).endsWith(".eps")) {
            val g = new EpsGraphics("Title", new FileOutputStream(args(2)),
              0, 0, dim.getWidth.toInt + 2, dim.getHeight.toInt, ColorMode.COLOR_RGB)
            frame.LFGcomponent.paint(g)
            g.flush()
            g.close()
          }
        }
        catch {
          case e: IOException => e.printStackTrace()
        }
        System.exit(0)
      }
      else if (args(0) == "open") {
        filename = args(1)
      }
      // Read LFG default config from default config file
      else if (args(0) == "io") {
        // TODO: try with resources
        // Read all content from file.
        val scanner: Scanner = new Scanner(System.in)
        try {
          val input: String = scanner.useDelimiter("\\A").next
          val frame: TabbedPaneFrame = new TabbedPaneFrame
          frame.importFromString(input)
          try {
            val dim: Dimension = frame.LFGcomponent.getDimension
            val g: EpsGraphics = new EpsGraphics("Title", System.out, 0, 0, dim.getWidth.toInt + 2, dim.getHeight.toInt, ColorMode.COLOR_RGB)
            frame.LFGcomponent.paint(g)
            g.flush()
            g.close()
          }
          catch {
            case e: IOException => {
              e.printStackTrace()
            }
          }
          System.exit(0)
        } finally {
          if (scanner != null) scanner.close()
        }
      }
    }
    EventQueue.invokeLater(new Runnable() {
      def run {
        try {
          // Use system default style.
          UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName)

          // Use gtk style in Linux
          for (info <- UIManager.getInstalledLookAndFeels) {
            if ("com.sun.java.swing.plaf.gtk.GTKLookAndFeel" == info.getName) {
              UIManager.setLookAndFeel(info.getClassName)
              break //todo: break is not supported
            }
          }
        }
        catch {
          // If Nimbus is not available, you can set the GUI to
          // another look and feel.
          case e: Exception => e.printStackTrace()
        }
        val frame: TabbedPaneFrame = new TabbedPaneFrame
        if (filename != null) {
          frame.importFromFile(filename)
        }
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
        frame.setVisible(true)
      }
    })
  }
}

/**
  * TabbedPaneFrame is the top-most UI class in the class hierarchy.
  * It contains a toolbar, a status bar, a menubar, and a panel with multiple tabs to show different content.
  *
  * @author shuoyang
  */
@SuppressWarnings(Array("serial"))
class TabbedPaneFrame extends JFrame {
  /**
    * provides a panel that contains multiple tab to show different contents
    */
  private val tabbedPane: JTabbedPane = new JTabbedPane

  /**
    * contains the name of the input file that is currently opened by the viewer
    */
  private var filename: String = null

  /**
    * viewers for each syntax to show corresponding graphical representations
    */
  private[LinguaView] val constcomponent: ConstTreePanel = new ConstTreePanel
  private[LinguaView] val depcomponent: DepTreePanel = new DepTreePanel
  private[LinguaView] val deepdepcomponent: DepTreePanel = new DepTreePanel
  private[LinguaView] val CCGcomponent: CCGTreePanel = new CCGTreePanel
  private[LinguaView] val LFGcomponent: LFGStructPanel = new LFGStructPanel

  /**
    * constitute a text editor
    */
  private[LinguaView] val Textcomponent: JTextPane = new JTextPane

  /**
    * controls all the undo/redo actions in text editor
    */
  private[LinguaView] val undoManager: UndoManager = new UndoManager
  /**
    * a wrapper for Textcomponent to show the text in a no-wrap style
    */
  private[LinguaView] val TextNoWrapPanel: JPanel = new JPanel
  /**
    * wrappers for each component to make them scrollable
    */
  final private[LinguaView] val constScrollPane: JScrollPane = new JScrollPane(constcomponent)
  final private[LinguaView] val depScrollPane: JScrollPane = new JScrollPane(depcomponent)
  final private[LinguaView] val deepdepScrollPane: JScrollPane = new JScrollPane(deepdepcomponent)
  final private[LinguaView] val CCGScrollPane: JScrollPane = new JScrollPane(CCGcomponent)
  final private[LinguaView] val LFGScrollPane: JScrollPane = new JScrollPane(LFGcomponent)
  final private[LinguaView] val TextScrollPane: JScrollPane = new JScrollPane(TextNoWrapPanel)
  /**
    * default treebanks that are shown as default or when error occurs
    */
  private[LinguaView] var defaultConstTreebank: ConstTree = null
  private[LinguaView] var defaultDepTreebank: DepTree = null
  private[LinguaView] var defaultDeepdepTreebank: DepTree = null
  private[LinguaView] var defaultCCGTreebank: CCGNode = null
  private[LinguaView] var defaultLFGStructbank: Element = null
  /**
    * the menu bar lays on the top of the frame, while the popupMenu only appears when user fires
    * a right-click in the text editor
    */
  private[LinguaView] val jmenuBar: JMenuBar = new JMenuBar
  private[LinguaView] val popupMenu: JPopupMenu = new JPopupMenu
  /**
    * the tool bar lays on the left side of the frame, the user is able to drag it to attach it on
    * any side of the panel or leave it floating somewhere
    */
  private[LinguaView] val bar: JToolBar = new JToolBar
  private[LinguaView] val newButton, importButton, exportButton,
    zoomInButton, zoomOutButton, prevButton, nextButton,
    jumpButton, saveTextButton, reloadButton = new JButton
  /**
    * the status bar tells the user which sentence they are processing
    * it also shows warnings when something abnormal happens
    */
  private[LinguaView] val statusBar: StatusBar = new StatusBar
  private var meta: MetadataManager = null
  private var encoding: String = null
  /**
    * A flag indicate whether we should check LFG.
    */
  private var checkLFG: Boolean = true

  setTitle("LinguaView 1.3.5")
  // deal with the panel: size, font, layout, etc.
  val kit: Toolkit = Toolkit.getDefaultToolkit
  val screenSize: Dimension = kit.getScreenSize
  val screenHeight: Int = screenSize.height
  val screenWidth: Int = screenSize.width
  setSize(screenWidth / 2, screenHeight / 2)
  setMinimumSize(new Dimension(screenWidth / 2, screenHeight / 2))
  setLayout(new BorderLayout)
  try {
    val ge: GraphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment
    val Lucida: Font = Font.createFont(Font.TRUETYPE_FONT, classOf[TabbedPaneFrame].getResourceAsStream("/ui/LUCON.ttf"))
    val Ubuntu: Font = Font.createFont(Font.TRUETYPE_FONT, classOf[TabbedPaneFrame].getResourceAsStream("/ui/Ubuntu-R.ttf"))
    ge.registerFont(Lucida)
    ge.registerFont(Ubuntu)
    val font: Font = new Font(Ubuntu.getFontName, Font.PLAIN, 14)
    val codeFont: Font = new Font(Lucida.getFontName, Font.PLAIN, 14)
    UIManager.put("TabbedPane.font", font)
    UIManager.put("Menu.font", font)
    UIManager.put("MenuItem.font", font)
    UIManager.put("CheckBoxMenuItem.font", font)
    UIManager.put("Label.font", font)
    // SwingUtilities.updateComponentTreeUI(tabbedPane)
    //SwingUtilities.updateComponentTreeUI(jmenuBar)
    //SwingUtilities.updateComponentTreeUI(statusBar)
    Textcomponent.setFont(codeFont)
  }
  catch {
    case e1: FontFormatException => {
      e1.printStackTrace()
    }
    case e1: IOException => {
      e1.printStackTrace()
    }
  }
  TextNoWrapPanel.setLayout(new BorderLayout)
  TextNoWrapPanel.setBackground(Color.white)
  TextNoWrapPanel.add(Textcomponent)

  // deal with the menu bar
  setJMenuBar(jmenuBar)

  // the file menu enables users to open, save the xml files they're processing
  val FileMenu: JMenu = new JMenu("File")
  val newItem: JMenuItem = new JMenuItem("New XML File...")
  FileMenu.add(newItem)
  FileMenu.addSeparator
  val importItem: JMenuItem = new JMenuItem("Import From File...")
  FileMenu.add(importItem)
  val exportItem: JMenuItem = new JMenuItem("Export To File...")
  FileMenu.add(exportItem)
  FileMenu.addSeparator
  val saveItem: JMenuItem = new JMenuItem("Save XML File")
  FileMenu.add(saveItem)
  val saveAsItem: JMenuItem = new JMenuItem("Save XML File As...")
  FileMenu.add(saveAsItem)
  FileMenu.addSeparator
  val exitItem: JMenuItem = new JMenuItem("Exit")
  FileMenu.add(exitItem)
  jmenuBar.add(FileMenu)

  // the edit menu contains operations for editors, like copy and undo
  val EditMenu: JMenu = new JMenu("Edit")
  val cutItem: JMenuItem = new JMenuItem("Cut")
  EditMenu.add(cutItem)
  val copyItem: JMenuItem = new JMenuItem("Copy")
  EditMenu.add(copyItem)
  val pasteItem: JMenuItem = new JMenuItem("Paste")
  EditMenu.add(pasteItem)
  EditMenu.addSeparator
  val selectAllItem: JMenuItem = new JMenuItem("Select All")
  EditMenu.add(selectAllItem)
  EditMenu.addSeparator
  val undoItem: JMenuItem = new JMenuItem("Undo")
  EditMenu.add(undoItem)
  val redoItem: JMenuItem = new JMenuItem("Redo")
  EditMenu.add(redoItem)
  jmenuBar.add(EditMenu)

  // the tool menu controls the appearance of the tool bar
  // users may add or dispose buttons or the tool bar at their will
  val ToolMenu: JMenu = new JMenu("Tools")
  val NewFileToolItem: JMenuItem = new JCheckBoxMenuItem("New File Button", true)
  ToolMenu.add(NewFileToolItem)
  val FileToolkitItem: JMenuItem = new JCheckBoxMenuItem("Import/Export Button", true)
  ToolMenu.add(FileToolkitItem)
  val ZoomToolkitItem: JMenuItem = new JCheckBoxMenuItem("Zoom in/Zoom Out Button", true)
  ToolMenu.add(ZoomToolkitItem)
  val SentToolkitItem: JMenuItem = new JCheckBoxMenuItem("Prev/Next Button", true)
  ToolMenu.add(SentToolkitItem)
  val JumpToolItem: JMenuItem = new JCheckBoxMenuItem("Jump Button", true)
  ToolMenu.add(JumpToolItem)
  val SaveTextToolItem: JMenuItem = new JCheckBoxMenuItem("Save XML Button", true)
  ToolMenu.add(SaveTextToolItem)
  ToolMenu.addSeparator
  val ToolbarItem: JMenuItem = new JMenuItem("Show/Hide Toolbar")
  ToolMenu.add(ToolbarItem)
  jmenuBar.add(ToolMenu)

  // the layout menu controls the appearance of the viewers
  val LayoutMenu: JMenu = new JMenu("Layout")
  val zoomInItem: JMenuItem = new JMenuItem("Zoom In")
  LayoutMenu.add(zoomInItem)
  val zoomOutItem: JMenuItem = new JMenuItem("Zoom Out")
  LayoutMenu.add(zoomOutItem)
  LayoutMenu.addSeparator
  val prevSentItem: JMenuItem = new JMenuItem("Previous Sentence")
  LayoutMenu.add(prevSentItem)
  val nextSentItem: JMenuItem = new JMenuItem("Next Sentence")
  LayoutMenu.add(nextSentItem)
  val jumpSentItem: JMenuItem = new JMenuItem("Jump To Sentence...")
  LayoutMenu.add(jumpSentItem)
  LayoutMenu.addSeparator
  val prevTabItem: JMenuItem = new JMenuItem("Previous Tab")
  LayoutMenu.add(prevTabItem)
  val nextTabItem: JMenuItem = new JMenuItem("Next Tab")
  LayoutMenu.add(nextTabItem)
  LayoutMenu.addSeparator
  val skewItem: JMenuItem = new JMenuItem("Straight/Skew Constituent Lines")
  LayoutMenu.add(skewItem)
  val colorItem: JMenuItem = new JMenuItem("Color/BW LFG Correspondence Lines")
  LayoutMenu.add(colorItem)
  val showItem: JMenuItem = new JMenuItem("Show/Hide LFG Correspondence Lines")
  LayoutMenu.add(showItem)
  val commentItem: JMenuItem = new JMenuItem("Show/Hide Comments")
  LayoutMenu.add(commentItem)
  val lfgCheckerItem: JMenuItem = new JMenuItem("Enable/Disable LFG Checker")
  LayoutMenu.add(lfgCheckerItem)
  jmenuBar.add(LayoutMenu)

  // the help menu provides link to the online introduction to input format and access to the author
  val HelpMenu: JMenu = new JMenu("Help")
  val formatItem: JMenuItem = new JMenuItem("Input Format")
  HelpMenu.add(formatItem)
  val aboutItem: JMenuItem = new JMenuItem("About")
  HelpMenu.add(aboutItem)
  jmenuBar.add(HelpMenu)

  // the pop up menu contains the same items as the edit menu
  // it is just designed for the convenience of users
  val PopupCutItem: JMenuItem = new JMenuItem("Cut")
  popupMenu.add(PopupCutItem)
  val PopupCopyItem: JMenuItem = new JMenuItem("Copy")
  popupMenu.add(PopupCopyItem)
  val PopupPasteItem: JMenuItem = new JMenuItem("Paste")
  popupMenu.add(PopupPasteItem)
  val PopupSelAllItem: JMenuItem = new JMenuItem("Select All")
  popupMenu.add(PopupSelAllItem)
  Textcomponent.setComponentPopupMenu(popupMenu)

  // add listeners and accelerators to each item by menu
  // when the item is clicked, it is the listeners that carry out supposed operations
  // the accelerators enables users to fire an operation without mouse click
  newItem.addActionListener(new ImportItemListener)
  importItem.addActionListener(new ImportItemListener)
  exportItem.addActionListener(new ExportItemListener)
  saveItem.addActionListener{e: ActionEvent => saveTo(filename, true)}
  saveAsItem.addActionListener(new SaveTextAsListener)
  newItem.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_N, InputEvent.CTRL_MASK))
  importItem.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, InputEvent.CTRL_MASK))
  exportItem.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_P, InputEvent.CTRL_MASK))
  saveItem.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, InputEvent.CTRL_MASK))
  saveAsItem.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK))
  exitItem.addActionListener(new ActionListener() {
    def actionPerformed(event: ActionEvent) {
      System.exit(0)
    }
  })
  cutItem.addActionListener{e:ActionEvent => Textcomponent.cut()}
  copyItem.addActionListener{e:ActionEvent => Textcomponent.copy()}
  pasteItem.addActionListener{e:ActionEvent => Textcomponent.paste()}
  selectAllItem.addActionListener{e:ActionEvent => Textcomponent.selectAll()}
  undoItem.addActionListener(new ActionListener() {
    def actionPerformed(event: ActionEvent) {
      if (undoManager.canUndo) {
        undoManager.undo
      }
    }
  })
  redoItem.addActionListener(new ActionListener() {
    def actionPerformed(event: ActionEvent) {
      if (undoManager.canRedo) {
        undoManager.redo
      }
    }
  })
  cutItem.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_X, InputEvent.CTRL_MASK))
  copyItem.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C, InputEvent.CTRL_MASK))
  pasteItem.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_V, InputEvent.CTRL_MASK))
  selectAllItem.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_A, InputEvent.CTRL_MASK))
  undoItem.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Z, InputEvent.CTRL_MASK))
  redoItem.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Z, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK))
  NewFileToolItem.addActionListener(new ActionListener() {
    def actionPerformed(event: ActionEvent) {
      newButton.setVisible(!newButton.isVisible)
    }
  })
  FileToolkitItem.addActionListener(new ActionListener() {
    def actionPerformed(event: ActionEvent) {
      importButton.setVisible(!importButton.isVisible)
      exportButton.setVisible(!exportButton.isVisible)
    }
  })
  ZoomToolkitItem.addActionListener(new ActionListener() {
    def actionPerformed(event: ActionEvent) {
      zoomInButton.setVisible(!zoomInButton.isVisible)
      zoomOutButton.setVisible(!zoomOutButton.isVisible)
    }
  })
  SentToolkitItem.addActionListener(new ActionListener() {
    def actionPerformed(event: ActionEvent) {
      prevButton.setVisible(!prevButton.isVisible)
      nextButton.setVisible(!nextButton.isVisible)
    }
  })
  JumpToolItem.addActionListener(new ActionListener() {
    def actionPerformed(event: ActionEvent) {
      jumpButton.setVisible(!jumpButton.isVisible)
    }
  })
  SaveTextToolItem.addActionListener(new ActionListener() {
    def actionPerformed(event: ActionEvent) {
      saveTextButton.setVisible(!saveTextButton.isVisible)
    }
  })
  ToolbarItem.addActionListener(new ActionListener() {
    def actionPerformed(event: ActionEvent) {
      bar.setVisible(!bar.isVisible)
    }
  })
  zoomInItem.addActionListener{e:ActionEvent => zoomIn}
  zoomOutItem.addActionListener{e:ActionEvent => zoomOut()}
  prevSentItem.addActionListener({e: ActionEvent => prevSent()})
  nextSentItem.addActionListener({e: ActionEvent => nextSent()})
  jumpSentItem.addActionListener(new JumpSentListener)
  prevTabItem.addActionListener(new PrevTabListener)
  nextTabItem.addActionListener(new NextTabListener)
  skewItem.addActionListener(new SkewLineListener)
  colorItem.addActionListener(new ColorLineListener)
  showItem.addActionListener(new ShowLineListener)
  commentItem.addActionListener(new ActionListener() {
    def actionPerformed(actionEvent: ActionEvent) {
      LFGcomponent.toggleComment
    }
  })
  lfgCheckerItem.addActionListener(new ActionListener() {
    def actionPerformed(actionEvent: ActionEvent) {
      checkLFG = !checkLFG
      if (checkLFG) {
        statusBar.setMessage("LFG Checker is enabled.")
      }
      else {
        statusBar.setMessage("LFG Checker is disabled.")
      }
    }
  })
  zoomInItem.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_EQUALS, InputEvent.CTRL_MASK))
  zoomOutItem.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_MINUS, InputEvent.CTRL_MASK))
  prevSentItem.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_LEFT, InputEvent.CTRL_MASK))
  nextSentItem.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_RIGHT, InputEvent.CTRL_MASK))
  jumpSentItem.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_J, InputEvent.CTRL_MASK))
  prevTabItem.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_LEFT, InputEvent.SHIFT_MASK | InputEvent.CTRL_MASK))
  nextTabItem.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_RIGHT, InputEvent.SHIFT_MASK | InputEvent.CTRL_MASK))
  formatItem.addActionListener(new ActionListener() {
    def actionPerformed(arg0: ActionEvent) {
      try {
        val url: String = "https://github.com/shuoyangd/LinguaView/blob/master/Input_Format_1_x.md"
        java.awt.Desktop.getDesktop.browse(java.net.URI.create(url))
      }
      catch {
        case e: IOException => {
          System.out.println(e.getMessage)
        }
      }
    }
  })
  aboutItem.addActionListener(new ActionListener() {
    def actionPerformed(event: ActionEvent) {
      JOptionPane.showConfirmDialog(TabbedPaneFrame.this, "LinguaView 1.0.1 by Shuoyang Ding\n" + "Language Computing & Web Mining Group, Peking University\n" + "2013.11\n" + "dsy100@gmail.com\n\n" + "LinguaView is an light-weight graphical tool aiming to\naid manual construction of linguistically-deep corpuses.\n" + "To help make this tool better, if you find any problems or bugs,\ndo not hesitate to email me.\n\n" + "Special acknowledgement to Federico Sangati,\nthe original author of the constituent viewer part.\n" + "And also to Weiwei Sun and Chen Wang,\nauthors of the data structures that constitute an implementation of CCG.", "About", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE)
    }
  })
  PopupCutItem.addActionListener{e: ActionEvent => Textcomponent.cut()}
  PopupCopyItem.addActionListener{e: ActionEvent => Textcomponent.copy()}
  PopupPasteItem.addActionListener{e: ActionEvent => Textcomponent.paste()}
  PopupSelAllItem.addActionListener{e: ActionEvent => Textcomponent.selectAll()}
  PopupCutItem.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_X, InputEvent.CTRL_MASK))
  PopupCopyItem.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C, InputEvent.CTRL_MASK))
  PopupPasteItem.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_V, InputEvent.CTRL_MASK))
  PopupSelAllItem.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_A, InputEvent.CTRL_MASK))

  // deal with the tool bar
  val newIcon: Icon = new ImageIcon(classOf[TabbedPaneFrame].getResource("/ui/new.png"))
  val importIcon: Icon = new ImageIcon(classOf[TabbedPaneFrame].getResource("/ui/import.png"))
  val exportIcon: Icon = new ImageIcon(classOf[TabbedPaneFrame].getResource("/ui/export.png"))
  val reloadIcon: Icon = new ImageIcon(classOf[TabbedPaneFrame].getResource("/ui/refresh.png"))
  val zoomInIcon: Icon = new ImageIcon(classOf[TabbedPaneFrame].getResource("/ui/zoomin.png"))
  val zoomOutIcon: Icon = new ImageIcon(classOf[TabbedPaneFrame].getResource("/ui/zoomout.png"))
  val prevIcon: Icon = new ImageIcon(classOf[TabbedPaneFrame].getResource("/ui/prev.png"))
  val nextIcon: Icon = new ImageIcon(classOf[TabbedPaneFrame].getResource("/ui/next.png"))
  val jumpIcon: Icon = new ImageIcon(classOf[TabbedPaneFrame].getResource("/ui/jump.png"))
  val saveTextIcon: Icon = new ImageIcon(classOf[TabbedPaneFrame].getResource("/ui/ok.png"))
  newButton.setIcon(newIcon)
  importButton.setIcon(importIcon)
  exportButton.setIcon(exportIcon)
  reloadButton.setIcon(reloadIcon)
  zoomInButton.setIcon(zoomInIcon)
  zoomOutButton.setIcon(zoomOutIcon)
  prevButton.setIcon(prevIcon)
  nextButton.setIcon(nextIcon)
  jumpButton.setIcon(jumpIcon)
  saveTextButton.setIcon(saveTextIcon)
  newButton.addActionListener(new ImportItemListener)
  importButton.addActionListener(new ImportItemListener)
  exportButton.addActionListener(new ExportItemListener)
  reloadButton.addActionListener(new reloadListener)
  zoomInButton.addActionListener{e: ActionEvent => zoomIn}
  zoomOutButton.addActionListener{e: ActionEvent => zoomOut}
  prevButton.addActionListener{e: ActionEvent => prevSent()}
  nextButton.addActionListener{e: ActionEvent => nextSent()}
  jumpButton.addActionListener(new JumpSentListener)
  saveTextButton.addActionListener(new SaveTextListener)
  bar.add(newButton)
  bar.add(importButton)
  bar.add(exportButton)
  bar.add(reloadButton)
  bar.add(zoomInButton)
  bar.add(zoomOutButton)
  bar.add(prevButton)
  bar.add(nextButton)
  bar.add(jumpButton)
  bar.add(saveTextButton)
  bar.setOrientation(SwingConstants.VERTICAL)
  add(bar, "West")

  // deal with the tabs
  tabbedPane.addTab("Constituent Tree", null, null)
  tabbedPane.addTab("Dependency Tree", null, null)
  tabbedPane.addTab("Deep Dependency Graph", null, null)
  tabbedPane.addTab("Combinatorial Categorial Tree", null, null)
  tabbedPane.addTab("Lexical Functional Structure", null, null)
  tabbedPane.addTab("Text Editor", null, null)
  val constTreebank: ArrayList[ConstTree] = new ArrayList[ConstTree]

  // default constTree initialization
  val defaultConstStr: String = "( (S (NP-SBJ (NP (NNP Pierre) (NNP Vinken) ) (, ,)" + " (ADJP (NP (CD 61) (NNS years) ) (JJ old) ) (, ,) )" + " (VP (MD will) (VP (VB join) (NP (DT the) (NN board) )" + " (PP-CLR (IN as) (NP (DT a) (JJ nonexecutive) (NN director) ))" + " (NP-TMP (NNP Nov.) (CD 29) ))) (. .) ))"
  defaultConstTreebank = ConstTree.ConstTreeIO.ReadConstTree(defaultConstStr)
  constTreebank.add(defaultConstTreebank)
  constcomponent.loadTreebank(constTreebank)
  constcomponent.init()
  constcomponent.setBackground(Color.WHITE)

  // default dependencyTree initialization
  val DepTreebank: ArrayList[DepTree] = new ArrayList[DepTree]
  defaultDepTreebank = new DepTree
  defaultDepTreebank.loadTokens("Pierre Vinken , 61 years old , will join the board as " + "a nonexecutive director Nov. 29 .")
  defaultDepTreebank.loadEdges("(0, 1, _) (1, 7, _) (2, 1, _) (3, 4, _) (4, 5, _) (5, 1, _)" + "(6, 1, _) (7, -1, _) (8, 7, _) (9, 10, _) (10, 8, _) (11, 8, _)" + " (12, 14, _) (13, 14, _) (14, 11, _) (15, 8, _) (16, 15, _) (17, 7, _)")
  DepTreebank.add(defaultDepTreebank)
  depcomponent.loadTreebank(DepTreebank)
  depcomponent.init()
  depcomponent.setBackground(Color.WHITE)

  // default deep dependency tree initialization
  val DeepdepTreebank: ArrayList[DepTree] = new ArrayList[DepTree]
  defaultDeepdepTreebank = new DepTree
  defaultDeepdepTreebank.loadTokens("Pierre Vinken , 61 years old , will join the board as " + "a nonexecutive director Nov. 29 .")
  defaultDeepdepTreebank.loadEdges("(0, 1, _) (1, 7, _) (2, 1, _) (3, 4, _) (4, 5, _) (5, 1, _)" + "(6, 1, _) (7, -1, _) (8, 7, _) (9, 10, _) (10, 8, _) (11, 8, _)" + " (12, 14, _) (13, 14, _) (14, 11, _) (15, 8, _) (16, 15, _) (17, 7, _)")
  DeepdepTreebank.add(defaultDeepdepTreebank)
  deepdepcomponent.loadTreebank(DeepdepTreebank)
  deepdepcomponent.init()
  deepdepcomponent.setBackground(Color.WHITE)

  // default CCG parse tree initialization
  val CCGTreebank: ArrayList[CCGNode] = new ArrayList[CCGNode]
  val defaultCCGStr: String = "(<T S[dcl] 0 2> (<T S[dcl] 1 2> (<T NP 0 2> (<T NP 0 2> " + "(<T NP 0 2> (<T NP 0 1> (<T N 1 2> (<L N/N NNP NNP Pierre N_73/N_73>) " + "(<L N NNP NNP Vinken N>) ) ) (<L , , , , ,>) ) (<T NP\\NP 0 1> " + "(<T S[adj]\\NP 1 2> (<T NP 0 1> (<T N 1 2> (<L N/N CD CD 61 N_93/N_93>) " + "(<L N NNS NNS years N>) ) ) (<L (S[adj]\\NP)\\NP JJ JJ old (S[adj]\\NP_83)\\NP_84>) ) ) ) " + "(<L , , , , ,>) ) (<T S[dcl]\\NP 0 2> " + "(<L (S[dcl]\\NP)/(S[b]\\NP) MD MD will (S[dcl]\\NP_10)/(S[b]_11\\NP_10:B)_11>) " + "(<T S[b]\\NP 0 2> (<T S[b]\\NP 0 2> (<T (S[b]\\NP)/PP 0 2> " + "(<L ((S[b]\\NP)/PP)/NP VB VB join ((S[b]\\NP_20)/PP_21)/NP_22>) " + "(<T NP 1 2> (<L NP[nb]/N DT DT the NP[nb]_29/N_29>) (<L N NN NN board N>) ) ) " + "(<T PP 0 2> (<L PP/NP IN IN as PP/NP_34>) (<T NP 1 2> " + "(<L NP[nb]/N DT DT a NP[nb]_48/N_48>) (<T N 1 2> " + "(<L N/N JJ JJ nonexecutive N_43/N_43>) (<L N NN NN director N>) ) ) ) ) " + "(<T (S\\NP)\\(S\\NP) 0 2> (<L ((S\\NP)\\(S\\NP))/N[num] NNP NNP Nov. " + "((S_61\\NP_56)_61\\(S_61\\NP_56)_61)/N[num]_62>) " + "(<L N[num] CD CD 29 N[num]>) ) ) ) ) (<L . . . . .>) ) "
  defaultCCGTreebank = CCGNode.getCCGNodeFromString(defaultCCGStr)
  CCGTreebank.add(defaultCCGTreebank)
  CCGcomponent.loadTreebank(CCGTreebank)
  CCGcomponent.init()
  CCGcomponent.setBackground(Color.WHITE)

  // default LFG structure initialization
  val LFGStructbank: ArrayList[Element] = new ArrayList[Element]
  // Read LFG default config from default config file
  var defaultLFGStr: String = ""
  //try { // TODO: try with resources
    val scanner: Scanner = new Scanner(getClass.getResourceAsStream("/raw/default_lfg.xml"))
    try {
      // Read all content from file.
      defaultLFGStr = scanner.useDelimiter("\\A").next()
    } finally {
      if (scanner != null) scanner.close()
    }
  // }
  try {
    val stream: InputStream = new ByteArrayInputStream(defaultLFGStr.getBytes("UTF-8"))
    val factory: DocumentBuilderFactory = DocumentBuilderFactory.newInstance
    val builder: DocumentBuilder = factory.newDocumentBuilder
    val doc: Document = builder.parse(stream)
    defaultLFGStructbank = doc.getDocumentElement
    LFGStructbank.add(defaultLFGStructbank)
    LFGcomponent.loadTreebank(LFGStructbank)
    LFGcomponent.init()
    if (checkLFG) checkLFGValid()
    LFGcomponent.setBackground(Color.WHITE)
  }
  catch {
    case e: Exception => {
      e.printStackTrace()
    }
  }

  // default text editor content initialization
  Textcomponent.setText("<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n" + "<hint lang=\"English\">\n\tLoad a corpus\n</hint>\n" + "<hint lang=\"Francais\">\n\tCharger un corpus\n</hint>\n" + "<hint lang=\"Deutsch\">\n\tLaden ein Korpus\n</hint>" + "<hint lang=\"中文\">\n\t请载入语料\n</hint>")
  Textcomponent.setEditable(false)
  add(tabbedPane, "Center")

  // filling in the tabs with components
  tabbedPane.addChangeListener(new ChangeListener() {
    def stateChanged(event: ChangeEvent) {
      if (tabbedPane.getSelectedComponent == null) {
        val n: Int = tabbedPane.getSelectedIndex
        if (n == tabbedPane.indexOfTab("Constituent Tree")) {
          tabbedPane.setComponentAt(n, constScrollPane)
        }
        else if (n == tabbedPane.indexOfTab("Dependency Tree")) {
          tabbedPane.setComponentAt(n, depScrollPane)
        }
        else if (n == tabbedPane.indexOfTab("Deep Dependency Graph")) {
          tabbedPane.setComponentAt(n, deepdepScrollPane)
        }
        else if (n == tabbedPane.indexOfTab("Combinatorial Categorial Tree")) {
          tabbedPane.setComponentAt(n, CCGScrollPane)
        }
        else if (n == tabbedPane.indexOfTab("Lexical Functional Structure")) {
          tabbedPane.setComponentAt(n, LFGScrollPane)
        }
        else if (n == tabbedPane.indexOfTab("Text Editor")) {
          tabbedPane.setComponentAt(n, TextScrollPane)
        }
      }
    }
  })
  tabbedPane.setComponentAt(0, constScrollPane)
  tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT)
  add(statusBar, "South")

  /**
    * increase all the text (except text editor) by 1 in font size
    * other lines and spaces also augments by scale
    */
  def zoomIn {
    constcomponent.fontSize += 1
    depcomponent.fontSize += 1
    deepdepcomponent.fontSize += 1
    CCGcomponent.fontSize += 1
    LFGcomponent.fontSize += 1
    constcomponent.init()
    depcomponent.init()
    deepdepcomponent.init()
    CCGcomponent.init()
    LFGcomponent.init()
  }

  /**
    * decrease all the text (except text editor) by 1 in font size
    * other lines and spaces also shrinks by scale
    */
  def zoomOut() {
    if (constcomponent.fontSize > 0) {
      constcomponent.fontSize -= 1
    }
    if (depcomponent.fontSize > 0) {
      depcomponent.fontSize -= 1
    }
    if (deepdepcomponent.fontSize > 0) {
      deepdepcomponent.fontSize -= 1
    }
    if (CCGcomponent.fontSize > 0) {
      CCGcomponent.fontSize -= 1
    }
    if (LFGcomponent.fontSize > 0) {
      LFGcomponent.fontSize -= 1
    }
    constcomponent.init()
    depcomponent.init()
    deepdepcomponent.init()
    CCGcomponent.init()
    LFGcomponent.init()
  }

  /**
    * decrease sentenceNumber by 1 for each viewer
    * that switch the viewers to the graphical representations of the previous sentence
    */
  def prevSent() {
    var Success: Boolean = true
    if (constcomponent.sentenceNumber > 0) {
      constcomponent.sentenceNumber -= 1
      try {
        constcomponent.init()
      }
      catch {
        case e: Exception => {
          e.printStackTrace()
          Success = false
          statusBar.setMessage("Previous sentence load failed. " + "Please check for problems in constituent tree part.")
          constcomponent.replaceCurrentSentence(defaultConstTreebank)
          constcomponent.init()
        }
      }
    }
    if (depcomponent.sentenceNumber > 0) {
      depcomponent.sentenceNumber -= 1
      try {
        depcomponent.init()
      }
      catch {
        case e: Exception => {
          Success = false
          statusBar.setMessage("Please check for problems in dependency tree part.")
          for (tr <- e.getStackTrace) {
            if (tr.getMethodName.contains("loadTokens")) {
              statusBar.setMessage("Please check for problems in wordlist part.")
            }
          }
          depcomponent.replaceCurrentSentence(defaultDepTreebank)
          depcomponent.init()
        }
      }
    }
    if (deepdepcomponent.sentenceNumber > 0) {
      deepdepcomponent.sentenceNumber -= 1
      try {
        deepdepcomponent.init()
      }
      catch {
        case e: Exception => {
          Success = false
          statusBar.setMessage("Please check for problems in deep dependency tree part.")
          for (tr <- e.getStackTrace) {
            if (tr.getMethodName.contains("loadTokens")) {
              statusBar.setMessage("Please check for problems in wordlist part.")
            }
          }
          deepdepcomponent.replaceCurrentSentence(defaultDeepdepTreebank)
          deepdepcomponent.init()
        }
      }
    }
    if (CCGcomponent.sentenceNumber > 0) {
      CCGcomponent.sentenceNumber -= 1
      try {
        CCGcomponent.init()
      }
      catch {
        case e: Exception => {
          e.printStackTrace()
          Success = false
          statusBar.setMessage("Previous sentence load failed. " + "Please check for problems in CCG part.")
          CCGcomponent.replaceCurrentSentence(defaultCCGTreebank)
          CCGcomponent.init()
        }
      }
    }
    if (LFGcomponent.sentenceNumber > 0) {
      LFGcomponent.sentenceNumber -= 1
      try {
        LFGcomponent.init()
        if (checkLFG) checkLFGValid
      }
      catch {
        case e: Exception => {
          e.printStackTrace()
          Success = false
          statusBar.setMessage("Previous sentence load failed. " + "Please check for problems in LFG part.")
          LFGcomponent.replaceCurrentSentence(defaultLFGStructbank)
          LFGcomponent.init()
        }
      }
    }

    // note that the sentenceNumber starts from 0
    // but the display on the status bar starts from 1
    if (Success) {
      statusBar.setMessage("You are at sentence " + Integer.toString(constcomponent.sentenceNumber + 1) + "/" + Integer.toString(constcomponent.lastIndex + 1))
    }
  }

  /**
    * increase sentenceNumber by 1 for each viewer
    * that switch the viewers to the graphical representations of the next sentence
    */
  def nextSent() {
    var Success: Boolean = true
    if (constcomponent.sentenceNumber < constcomponent.lastIndex) {
      constcomponent.sentenceNumber += 1
      try {
        constcomponent.init()
      }
      catch {
        case e: Exception => {
          Success = false
          statusBar.setMessage("Please check for problems in constituent tree part.")
          constcomponent.replaceCurrentSentence(defaultConstTreebank)
          constcomponent.init()
        }
      }
    }
    if (depcomponent.sentenceNumber < depcomponent.lastIndex) {
      depcomponent.sentenceNumber += 1
      try {
        depcomponent.init()
      }
      catch {
        case e: Exception => {
          e.printStackTrace()
          Success = false
          statusBar.setMessage("Please check for problems in dependency tree part.")
          for (tr <- e.getStackTrace) {
            if (tr.getMethodName.contains("loadTokens")) {
              statusBar.setMessage("Please check for problems in wordlist part.")
            }
          }
          depcomponent.replaceCurrentSentence(defaultDepTreebank)
          depcomponent.init()
        }
      }
    }
    if (deepdepcomponent.sentenceNumber < deepdepcomponent.lastIndex) {
      deepdepcomponent.sentenceNumber += 1
      try {
        deepdepcomponent.init()
      }
      catch {
        case e: Exception => {
          Success = false
          statusBar.setMessage("Please check for problems in deep dependency tree part.")
          for (tr <- e.getStackTrace) {
            if (tr.getMethodName.contains("loadTokens")) {
              statusBar.setMessage("Please check for problems in wordlist part.")
            }
          }
          deepdepcomponent.replaceCurrentSentence(defaultDeepdepTreebank)
          deepdepcomponent.init()
        }
      }
    }
    if (CCGcomponent.sentenceNumber < CCGcomponent.lastIndex) {
      CCGcomponent.sentenceNumber += 1
      try {
        CCGcomponent.init()
      }
      catch {
        case e: Exception => {
          Success = false
          statusBar.setMessage("Please check for problems in CCG part.")
          CCGcomponent.replaceCurrentSentence(defaultCCGTreebank)
          CCGcomponent.init()
        }
      }
    }
    if (LFGcomponent.sentenceNumber < LFGcomponent.lastIndex) {
      LFGcomponent.sentenceNumber += 1
      try {
        LFGcomponent.init()
        if (checkLFG) checkLFGValid
      }
      catch {
        case e: Exception => {
          e.printStackTrace()
          Success = false
          statusBar.setMessage("Next sentence load failed. " + "Please check for problems in LFG part.")
          LFGcomponent.replaceCurrentSentence(defaultLFGStructbank)
          LFGcomponent.init()
        }
      }
    }

    // note that the sentenceNumber starts from 0
    // but the display on the status bar starts from 1
    if (Success) {
      statusBar.setMessage("You are at sentence " + Integer.toString(constcomponent.sentenceNumber + 1) + "/" + Integer.toString(constcomponent.lastIndex + 1))
    }
  }

  /**
    * set sentenceNumber to a certain value for each viewer
    * that switch the viewers to the graphical representations of a specified sentence
    */
  def jumpSent(origNewSentenceNumber: Int) {
    var Success: Boolean = true

    val newSentenceNumber = origNewSentenceNumber - 1
    if (newSentenceNumber <= constcomponent.lastIndex && newSentenceNumber >= 0) {
      constcomponent.sentenceNumber = newSentenceNumber
      try {
        constcomponent.init()
      }
      catch {
        case e: Exception => {
          Success = false
          statusBar.setMessage("Please check for problems in constituent tree part.")
        }
      }
    }
    if (newSentenceNumber <= depcomponent.lastIndex && newSentenceNumber >= 0) {
      depcomponent.sentenceNumber = newSentenceNumber
      try {
        depcomponent.init()
      }
      catch {
        case e: Exception => {
          Success = false
          statusBar.setMessage("Please check for problems in dependency tree part.")
          for (tr <- e.getStackTrace) {
            if (tr.getMethodName.contains("loadTokens")) {
              statusBar.setMessage("Please check for problems in wordlist part.")
            }
          }
        }
      }
    }
    if (newSentenceNumber <= deepdepcomponent.lastIndex && newSentenceNumber >= 0) {
      deepdepcomponent.sentenceNumber = newSentenceNumber
      try {
        deepdepcomponent.init()
      }
      catch {
        case e: Exception => {
          Success = false
          statusBar.setMessage("Please check for problems in deep dependency tree part.")
          for (tr <- e.getStackTrace) {
            if (tr.getMethodName.contains("loadTokens")) {
              statusBar.setMessage("Please check for problems in wordlist part.")
            }
          }
        }
      }
    }
    if (newSentenceNumber <= CCGcomponent.lastIndex && newSentenceNumber >= 0) {
      CCGcomponent.sentenceNumber = newSentenceNumber
      try {
        CCGcomponent.init()
      }
      catch {
        case e: Exception => {
          Success = false
          statusBar.setMessage("Please check for problems in CCG part.")
        }
      }
    }
    if (newSentenceNumber <= LFGcomponent.lastIndex && newSentenceNumber >= 0) {
      LFGcomponent.sentenceNumber = newSentenceNumber
      try {
        LFGcomponent.init()
        if (checkLFG) checkLFGValid
      }
      catch {
        case e: Exception => {
          e.printStackTrace()
          Success = false
          statusBar.setMessage("Cannot jump sentance. Please check for problems in LFG part.")
        }
      }
    }

    // note that the sentenceNumber starts from 0
    // but the display on the status bar starts from 1
    if (Success) {
      statusBar.setMessage("You are at sentence " + Integer.toString(constcomponent.sentenceNumber + 1) + "/" + Integer.toString(constcomponent.lastIndex + 1))
    }
  }

  /**
    * import data from parsed XML document "doc"
    *
    * @param doc
    */
  def importView(doc: Document) {
    val docRoot: Element = doc.getDocumentElement
    var children: NodeList = docRoot.getChildNodes
    // "Success" indicates whether the whole import operation has been successfully implemented
    val Sentences: ArrayList[Element] = new ArrayList[Element]
    var i: Int = 0
    while (i < children.getLength) {
      {
        val child: Node = children.item(i)
        if (child.getNodeName == "sentence" && (child.isInstanceOf[Element])) {
          Sentences.add(child.asInstanceOf[Element])
        }
      }
      ({
        i += 1; i - 1
      })
    }
    val constTreebank: ArrayList[ConstTree] = new ArrayList[ConstTree]
    val depTreebank: ArrayList[DepTree] = new ArrayList[DepTree]
    val deepdepTreebank: ArrayList[DepTree] = new ArrayList[DepTree]
    val CCGTreebank: ArrayList[CCGNode] = new ArrayList[CCGNode]
    val LFGStructbank: ArrayList[Element] = new ArrayList[Element]
    var Success: Boolean = true
    var sentNum: Int = 0
    sentNum = 0
    //iterate through all the sentences
    while (sentNum < Sentences.size) {
      {
        // indicate if each grammar tree is loaded for this sentence
        // if not, use a default tree to fill in
        val sent: Element = Sentences.get(sentNum)
        var constTreeAdded: Boolean = false
        var depTreeAdded: Boolean = false
        var deepdepTreeAdded: Boolean = false
        var CCGTreeAdded: Boolean = false
        var LFGStructAdded: Boolean = false

        // get the i-th sentence
        if (sent.isInstanceOf[Element]) {
          val sentChildren: NodeList = sent.getChildNodes
          var tokenls: Element = null
          var j: Int = 0
          // iterate through all the elements in a sentence
          while (j < sentChildren.getLength) {
            {
              val sentElement: Node = sentChildren.item(j)

              // extract element for wordlist
              // but it is only loaded when <deptree> tag is encountered
              if ((sentElement.isInstanceOf[Element]) && (sentElement.getNodeName eq "wordlist")) {
                tokenls = sentElement.asInstanceOf[Element]
              }
              // extract and load constituent tree
              else if ((sentElement.isInstanceOf[Element]) && (sentElement.getNodeName eq "constree")) {
                try {
                  children = sentElement.getChildNodes
                  var constStr: String = new String
                  var k: Int = 0
                  while (k < children.getLength) {
                    {
                      val constTextNode: Node = children.item(k)
                      if (constTextNode.isInstanceOf[Text]) {
                        constStr = (constTextNode.asInstanceOf[Text]).getTextContent
                      }
                    }
                    ({
                      k += 1; k - 1
                    })
                  }
                  if (!constStr.trim.isEmpty) {
                    val constTree: ConstTree = ConstTree.ConstTreeIO.ReadConstTree(constStr)
                    constTreebank.add(constTree)
                  }
                  else {
                    constTreebank.add(defaultConstTreebank)
                  }
                  constTreeAdded = true
                }
                catch {
                  case e: Exception => {
                    Success = false
                    statusBar.setMessage("Please check for problems in constituent tree part.")
                  }
                }
              }
              //extract and load dependency tree & wordlist
              else if ((sentElement.isInstanceOf[Element]) && (sentElement.getNodeName eq "deptree")) {
                try {
                  children = sentElement.getChildNodes
                  var depStr: String = new String
                  var k: Int = 0
                  while (k < children.getLength) {
                    {
                      val depTextNode: Node = children.item(k)
                      if (depTextNode.isInstanceOf[Text]) {
                        depStr = (depTextNode.asInstanceOf[Text]).getTextContent
                      }
                    }
                    ({
                      k += 1; k - 1
                    })
                  }
                  if (!depStr.trim.isEmpty) {
                    val depTree: DepTree = new DepTree
                    depTree.loadTokens(tokenls)
                    depTree.loadEdges(depStr.trim)
                    depTreebank.add(depTree)
                  }
                  else {
                    depTreebank.add(defaultDepTreebank)
                  }
                  depTreeAdded = true
                }
                catch {
                  case e: Exception => {
                    Success = false
                    statusBar.setMessage("Please check for problems in dependency tree part.")
                    for (tr <- e.getStackTrace) {
                      if (tr.getMethodName.contains("loadTokens")) {
                        statusBar.setMessage("Please check for problems in wordlist part.")
                      }
                    }
                  }
                }
              }
              //extract and load deep dependency graph & wordlist
              else if ((sentElement.isInstanceOf[Element]) && (sentElement.getNodeName eq "deepdep")) {
                try {
                  children = sentElement.getChildNodes
                  var depStr: String = new String
                  var k: Int = 0
                  while (k < children.getLength) {
                    {
                      val depTextNode: Node = children.item(k)
                      if (depTextNode.isInstanceOf[Text]) {
                        depStr = (depTextNode.asInstanceOf[Text]).getTextContent
                      }
                    }
                    ({
                      k += 1; k - 1
                    })
                  }
                  if (!depStr.trim.isEmpty) {
                    val depTree: DepTree = new DepTree
                    depTree.loadTokens(tokenls)
                    depTree.loadEdges(depStr.trim)
                    deepdepTreebank.add(depTree)
                  }
                  else {
                    deepdepTreebank.add(defaultDepTreebank)
                  }
                  deepdepTreeAdded = true
                }
                catch {
                  case e: Exception => {
                    Success = false
                    statusBar.setMessage("Please check for problems in deep dependency tree part.")
                    for (tr <- e.getStackTrace) {
                      if (tr.getMethodName.contains("loadTokens")) {
                        statusBar.setMessage("Please check for problems in wordlist part.")
                      }
                    }
                  }
                }
              }
              // extract LFG structure
              // it is loaded in LFGcomponent.init()
              else if ((sentElement.isInstanceOf[Element]) && (sentElement.getNodeName eq "lfg")) {
                try {
                  LFGStructbank.add(sentElement.asInstanceOf[Element])
                  LFGStructAdded = true
                }
                catch {
                  case e: Exception => {
                    e.printStackTrace()
                    Success = false
                    statusBar.setMessage("Can't load sentance from XML. " + "Please check for problems in LFG part.")
                  }
                }
              }
              // extract and load CCG
              else if ((sentElement.isInstanceOf[Element]) && (sentElement.getNodeName eq "ccg")) {
                try {
                  children = sentElement.getChildNodes
                  var CCGStr: String = new String
                  var k: Int = 0
                  while (k < children.getLength) {
                    {
                      val CCGTextNode: Node = children.item(k)
                      if (CCGTextNode.isInstanceOf[Text]) {
                        CCGStr = (CCGTextNode.asInstanceOf[Text]).getTextContent
                      }
                    }
                    ({
                      k += 1; k - 1
                    })
                  }
                  if (CCGStr.trim.matches("^\\( *\\)$")) {
                    val CCGTree: CCGNode = new CCGTerminalNode("<L _ _ _ _ _>", 0)
                    CCGTreebank.add(CCGTree)
                  }
                  else if (!CCGStr.trim.isEmpty) {
                    CCGStr = CCGStr.replace('{', '<')
                    CCGStr = CCGStr.replace('}', '>')
                    val CCGTree: CCGNode = CCGNode.getCCGNodeFromString(CCGStr.trim)
                    CCGTreebank.add(CCGTree)
                  }
                  else {
                    CCGTreebank.add(defaultCCGTreebank)
                  }
                  CCGTreeAdded = true
                }
                catch {
                  case e: Exception => {
                    Success = false
                    statusBar.setMessage("Please check for problems in CCG part.")
                    e.printStackTrace()
                  }
                }
              }
            }
            ({
              j += 1; j - 1
            })
          }
        }
        // if any grammar tree is not loaded for this sentence
        // use a default one to fill the position
        if (!constTreeAdded) {
          constTreebank.add(defaultConstTreebank)
        }
        if (!depTreeAdded) {
          depTreebank.add(defaultDepTreebank)
        }
        if (!deepdepTreeAdded) {
          deepdepTreebank.add(defaultDeepdepTreebank)
        }
        if (!CCGTreeAdded) {
          CCGTreebank.add(defaultCCGTreebank)
        }
        if (!LFGStructAdded) {
          JOptionPane.showMessageDialog(this, "No LFG part found.")
          LFGStructbank.add(defaultLFGStructbank)
        }
      }
      ({
        sentNum += 1; sentNum - 1
      })
    }
    constcomponent.sentenceNumber = 0
    depcomponent.sentenceNumber = 0
    deepdepcomponent.sentenceNumber = 0
    CCGcomponent.sentenceNumber = 0
    LFGcomponent.sentenceNumber = 0
    //load treebank and initiate the graphical representations
    if (!constTreebank.isEmpty) {
      try {
        constcomponent.loadTreebank(constTreebank)
        constcomponent.init()
      }
      catch {
        case e: Exception => {
          Success = false
          statusBar.setMessage("Please check for problems in constituent tree part.")
          constcomponent.replaceCurrentSentence(defaultConstTreebank)
          constcomponent.init()
        }
      }
    }
    if (!depTreebank.isEmpty) {
      try {
        depcomponent.loadTreebank(depTreebank)
        depcomponent.init()
      }
      catch {
        case e: Exception => {
          Success = false
          statusBar.setMessage("Please check for problems in dependency tree part.")
          depcomponent.replaceCurrentSentence(defaultDepTreebank)
          depcomponent.init()
        }
      }
    }
    if (!deepdepTreebank.isEmpty) {
      try {
        deepdepcomponent.loadTreebank(deepdepTreebank)
        deepdepcomponent.init()
      }
      catch {
        case e: Exception => {
          Success = false
          statusBar.setMessage("Please check for problems in deep dependency tree part.")
          deepdepcomponent.replaceCurrentSentence(defaultDeepdepTreebank)
          deepdepcomponent.init()
        }
      }
    }
    if (!CCGTreebank.isEmpty) {
      try {
        CCGcomponent.loadTreebank(CCGTreebank)
        CCGcomponent.init()
      }
      catch {
        case e: Exception => {
          Success = false
          statusBar.setMessage("Please check for problems in CCG part.")
          CCGcomponent.replaceCurrentSentence(defaultCCGTreebank)
          CCGcomponent.init()
        }
      }
    }
    if (!LFGStructbank.isEmpty) {
      try {
        LFGcomponent.loadTreebank(LFGStructbank)
        LFGcomponent.init()
        if (!LFGcomponent.isAllRefValid) {
          Success = false
          statusBar.setMessage("Invalid c-structure & f-structure correspondence detected.")
        }
        if (isVisible) if (checkLFG) checkLFGValid
      }
      catch {
        case e: Exception => {
          e.printStackTrace()
          Success = false
          statusBar.setMessage("Cannot load LFGStructbank. Please check for problems in LFG part.")
          LFGcomponent.replaceCurrentSentence(defaultLFGStructbank)
          LFGcomponent.init()
        }
      }
    }

    // note that the sentenceNumber starts from 0
    // but the display on the status bar starts from 1
    if (Success) {
      statusBar.setMessage("You are at sentence " + Integer.toString(constcomponent.sentenceNumber + 1) + "/" + Integer.toString(constcomponent.lastIndex + 1))
    }
  }

  /**
    * this function imports XML text into the text editor, configure the undoManager,
    * and then calls the importView() function to load data
    *
    * @param filename
    */
  def importFromFile(filename: String) {
    try {
      var res: Int = 0
      if ((new File(filename)).length > 1024000) {
        res = JOptionPane.showConfirmDialog(tabbedPane, "This file may results in " + "a long time wait due to its large size.\n" + "Would you like to continue anyway?", "Warning", JOptionPane.YES_NO_OPTION)
      }
      if (res == 0) {
        val factory: DocumentBuilderFactory = DocumentBuilderFactory.newInstance
        val builder: DocumentBuilder = factory.newDocumentBuilder
        val doc0: Document = builder.parse(new InputSource(new FileReader(filename)))
        encoding = doc0.getXmlEncoding
        new TextComponentLayout(Textcomponent, filename, encoding)
        Textcomponent.getDocument.addUndoableEditListener(new XMLUndoableEditListener)
        undoManager.discardAllEdits
        val doc: Document = builder.parse(new InputSource(new BufferedReader(new InputStreamReader(new FileInputStream(filename), encoding))))
        meta = new MetadataManager(filename, encoding)
        if (meta.isEmpty && isVisible) statusBar.setMessage("No metadata found.")
        importView(doc)
      }
    }
    catch {
      case e: Exception => {
        e.printStackTrace()
        statusBar.setMessage("Please check for problems in XML format.")
      }
    }
    this.filename = filename
  }

  /**
    * This function does not import text into editor
    * It is only called to set up the default view when the program starts
    *
    * @param input input string
    */
  def importFromString(input: String) {
    val factory: DocumentBuilderFactory = DocumentBuilderFactory.newInstance
    try {
      val builder: DocumentBuilder = factory.newDocumentBuilder
      val doc: Document = builder.parse(new InputSource(new ByteArrayInputStream(input.getBytes("utf-8"))))
      importView(doc)
      meta = new MetadataManager(input)
    }
    catch {
      case e: Exception => {
        e.printStackTrace()
      }
    }
  }

  /**
    * This listener is fired when importItem is clicked
    * The target directory will be fetched from a pop up dialog
    * If the file indicated by the directory exists, just open it up
    * Elsewise, the file would be created first and default content will be filled in
    *
    * @author shuoyang
    */
  private[LinguaView] class ImportItemListener extends ActionListener {
    def actionPerformed(event: ActionEvent) {
      filename = Utils.fileSelection(false)
      if (filename != null) {
        try {
          val f: File = new File(filename)
          if (!f.exists) {
            f.createNewFile
            val in: BufferedWriter = new BufferedWriter(new FileWriter(f))
            in.write("<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n")
            in.write("<viewer>\n")
            in.write("\t<sentence id=\"1\">\n")
            in.write("\t</sentence>\n")
            in.write("</viewer>\n")
            in.close()
          }
        }
        catch {
          case e: IOException => {
            e.printStackTrace()
          }
        }
        importFromFile(filename)
      }
    }
  }

  /**
    * This listener is fired when exportItem is clicked
    * It shows an ExportOptionPanel and leaves the rest work to it
    *
    * @author shuoyang
    */
  private[LinguaView] class ExportItemListener extends ActionListener {
    def actionPerformed(event: ActionEvent) {
      val exp: ExportOptionPanel = new ExportOptionPanel
      exp.showDialog(TabbedPaneFrame.this, "Export")
    }
  }

  /**
    * Reload the file.
    *
    * @author chen yufei
    */
  private[LinguaView] class reloadListener extends ActionListener {
    def actionPerformed(event: ActionEvent) {
      if (filename == null) {
        JOptionPane.showMessageDialog(TabbedPaneFrame.this, "No file imported.")
        return
      }
      // Jump to the same sentence
      val storedSentenceNumber: Int = constcomponent.sentenceNumber + 1
      importFromFile(filename)
      jumpSent(storedSentenceNumber)
    }
  }

  /**
    * This listener is fired when jumpSentItem is clicked
    * It shows a dialog to fetch the id of destination sentence and jumps to it
    *
    * @author shuoyang
    */
  private[LinguaView] class JumpSentListener extends ActionListener {
    def actionPerformed(event: ActionEvent) {
      val res: String = JOptionPane.showInputDialog(tabbedPane, "To which sentence do you what to jump?", "Jump To Sentence...", JOptionPane.PLAIN_MESSAGE)
      if (res != null && !res.trim.isEmpty) {
        jumpSent(res.toInt)
      }
    }
  }

  /**
    * This listener is fired when prevTabItem is clicked
    *
    * @author shuoyang
    */
  private[LinguaView] class PrevTabListener extends ActionListener {
    def actionPerformed(event: ActionEvent) {
      var n: Int = tabbedPane.getSelectedIndex
      if (n == tabbedPane.indexOfTab("Dependency Tree")) {
        n = tabbedPane.indexOfTab("Constituent Tree")
        tabbedPane.setSelectedIndex(n)
        tabbedPane.setComponentAt(n, constScrollPane)
      }
      else if (n == tabbedPane.indexOfTab("Deep Dependency Graph")) {
        n = tabbedPane.indexOfTab("Dependency Tree")
        tabbedPane.setSelectedIndex(n)
        tabbedPane.setComponentAt(n, depScrollPane)
      }
      else if (n == tabbedPane.indexOfTab("Combinatorial Categorial Tree")) {
        n = tabbedPane.indexOfTab("Deep Dependency Graph")
        tabbedPane.setSelectedIndex(n)
        tabbedPane.setComponentAt(n, deepdepScrollPane)
      }
      else if (n == tabbedPane.indexOfTab("Lexical Functional Structure")) {
        n = tabbedPane.indexOfTab("Combinatorial Categorial Tree")
        tabbedPane.setSelectedIndex(n)
        tabbedPane.setComponentAt(n, CCGScrollPane)
      }
      else if (n == tabbedPane.indexOfTab("Text Editor")) {
        n = tabbedPane.indexOfTab("Lexical Functional Structure")
        tabbedPane.setSelectedIndex(n)
        tabbedPane.setComponentAt(n, LFGScrollPane)
      }
    }
  }

  /**
    * This listener is fired when nextTabItem is clicked
    *
    * @author shuoyang
    */
  private[LinguaView] class NextTabListener extends ActionListener {
    def actionPerformed(event: ActionEvent) {
      var n: Int = tabbedPane.getSelectedIndex
      if (n == tabbedPane.indexOfTab("Constituent Tree")) {
        n = tabbedPane.indexOfTab("Dependency Tree")
        tabbedPane.setSelectedIndex(n)
        tabbedPane.setComponentAt(n, depScrollPane)
      }
      else if (n == tabbedPane.indexOfTab("Dependency Tree")) {
        n = tabbedPane.indexOfTab("Deep Dependency Graph")
        tabbedPane.setSelectedIndex(n)
        tabbedPane.setComponentAt(n, deepdepScrollPane)
      }
      else if (n == tabbedPane.indexOfTab("Deep Dependency Graph")) {
        n = tabbedPane.indexOfTab("Combinatorial Categorial Tree")
        tabbedPane.setSelectedIndex(n)
        tabbedPane.setComponentAt(n, CCGScrollPane)
      }
      else if (n == tabbedPane.indexOfTab("Combinatorial Categorial Tree")) {
        n = tabbedPane.indexOfTab("Lexical Functional Structure")
        tabbedPane.setSelectedIndex(n)
        tabbedPane.setComponentAt(n, LFGScrollPane)
      }
      else if (n == tabbedPane.indexOfTab("Lexical Functional Structure")) {
        n = tabbedPane.indexOfTab("Text Editor")
        tabbedPane.setSelectedIndex(n)
        tabbedPane.setComponentAt(n, TextScrollPane)
      }
    }
  }

  private[LinguaView] class SkewLineListener extends ActionListener {
    def actionPerformed(e: ActionEvent) {
      constcomponent.skewedLines = !constcomponent.skewedLines
      constcomponent.init()
    }
  }

  private[LinguaView] class ColorLineListener extends ActionListener {
    def actionPerformed(e: ActionEvent) {
      LFGcomponent.isColor = !LFGcomponent.isColor
      LFGcomponent.init()
    }
  }

  private[LinguaView] class ShowLineListener extends ActionListener {
    def actionPerformed(e: ActionEvent) {
      LFGcomponent.showCorrespondingLine = !LFGcomponent.showCorrespondingLine
      LFGcomponent.init()
    }
  }

  /**
    * This listener is fired when saveTextItem is clicked
    * It writes content in the editor to the linked destination and automatically reload from it
    *
    * @author shuoyang
    */
  private[LinguaView] class SaveTextListener extends ActionListener {
    def actionPerformed(event: ActionEvent) {
      saveTo(filename, true)
    }
  }

  /**
    * This listener almost the same as the previous one
    * The only difference is that a file dialog pops up to let you choose the destination
    *
    * @author shuoyang
    */
  private[LinguaView] class SaveTextAsListener extends ActionListener {
    def actionPerformed(event: ActionEvent) {
      filename = Utils.fileSelection(true)
      if (Files.exists(Paths.get(filename))) saveTo(filename, true)
      else saveTo(filename, false)
    }
  }

  /**
    * Save text to specify path.
    *
    * @param path
    */
  private def saveTo(path: String, backup: Boolean) {
    try {
      val text: String = Textcomponent.getText
      if (path != null && !path.trim.isEmpty) {
        if (backup) Files.copy(Paths.get(path), Paths.get(path + ".bak"), StandardCopyOption.REPLACE_EXISTING)
        val in: BufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path), encoding))
        in.write(text)
        in.close()
        importFromFile(path)
      }
    }
    catch {
      case e: IOException => {
        e.printStackTrace()
      }
    }
  }

  /**
    * the status bar tells the user which sentence they are processing
    * it also shows warnings when something abnormal happens
    *
    * @author shuoyang
    */
  private[LinguaView] class StatusBar extends JLabel {
    super.setPreferredSize(new Dimension(500, 32))
    setMessage("Default treebank displaying, load a corpus to begin.")

    def setMessage(message: String) {
      setText(message + " ")
      setHorizontalAlignment(SwingConstants.TRAILING)
      setVerticalTextPosition(SwingConstants.CENTER)
    }
  }

  /**
    * The ExportOptionPanel enables users to export a certain view of the current sentence to a EPS graph
    * It pops up a dialog first to let you choose which view and what location to export
    * After that it exports the graph using an EPSGraphics object
    *
    * @author shuoyang
    */
  private[LinguaView] class ExportOptionPanel extends JPanel {
    /**
      * the pop-up dialog
      */
    private[LinguaView] var dialog: JDialog = null
    /**
      * the file selected
      */
    private[LinguaView] var FileLoc: String = new String
    /**
      * the choice of view
      */
    private[LinguaView] val tabChoicePanel: ButtonPanel = new ButtonPanel("Tab Choice", "Constituent", "Dependency", "Deep Dependency", "CCG", "LFG")
    /**
      * the panel that enables you to select export location
      */
    private[LinguaView] val locPanel: FileLocPanel = new FileLocPanel
    /**
      * the "DO-IT" button
      */
    private[LinguaView] val ExportButton: JButton = new JButton("Export")
    /**
      * Sets the layout of the pop-up dialog
      */
    ExportButton.addActionListener(new ExportActionListener)
    setLayout(new BorderLayout)
    val kit: Toolkit = Toolkit.getDefaultToolkit
    val screenSize: Dimension = kit.getScreenSize
    val screenHeight: Int = screenSize.height
    val screenWidth: Int = screenSize.width
    setSize(screenWidth / 3, screenHeight / 4)
    setBorder(new EmptyBorder(10, 10, 10, 10))
    val origSize: Dimension = tabChoicePanel.getPreferredSize
    tabChoicePanel.setPreferredSize(new Dimension(screenWidth / 3, origSize.height))
    locPanel.setBorder(new EmptyBorder(5, 0, 5, 0))
    add(tabChoicePanel, "North")
    add(locPanel, "Center")
    val ExportButtonWrapperPanel: JPanel = new JPanel
    ExportButtonWrapperPanel.add(ExportButton)
    ExportButtonWrapperPanel.setPreferredSize(new Dimension(100, 40))
    add(ExportButtonWrapperPanel, "South")

    /**
      * To show the export option dialog, call this function
      *
      * @param parent
      * @param title
      */
    def showDialog(parent: Component, title: String) {
      var owner: Frame = null
      if (parent.isInstanceOf[Frame]) {
        owner = parent.asInstanceOf[Frame]
      }
      else {
        owner = SwingUtilities.getAncestorOfClass(classOf[Frame], parent).asInstanceOf[Frame]
      }
      if (dialog == null || (dialog.getOwner ne owner)) {
        dialog = new JDialog(owner, true)
        dialog.add(this)
        dialog.getRootPane.setDefaultButton(ExportButton)
        dialog.pack
      }
      dialog.setTitle(title)
      dialog.setVisible(true)
    }

    /**
      * This listener is attached to "ExportButton" and carries out the export operation
      *
      * @author shuoyang
      */
    private[LinguaView] class ExportActionListener extends ActionListener {
      def actionPerformed(event: ActionEvent) {
        FileLoc = locPanel.getLoc
        if (FileLoc != null && !FileLoc.isEmpty) {
          val sel: String = tabChoicePanel.getSelection
          try {
            if (sel eq "Constituent") {
              val dim: Dimension = constcomponent.getDimension
              val g: EpsGraphics = new EpsGraphics("Title", new FileOutputStream(FileLoc), 0, 0, dim.getWidth.toInt + 2, dim.getHeight.toInt, ColorMode.COLOR_RGB)
              constcomponent.paint(g)
              g.flush()
              g.close()
            }
            else if (sel eq "Dependency") {
              val dim: Dimension = depcomponent.getDimension
              val g: EpsGraphics = new EpsGraphics("Title", new FileOutputStream(FileLoc), 0, 0, dim.getWidth.toInt + 2, dim.getHeight.toInt, ColorMode.COLOR_RGB)
              depcomponent.paint(g)
              g.flush()
              g.close()
            }
            else if (sel eq "Deep Dependency") {
              val dim: Dimension = deepdepcomponent.getDimension
              val g: EpsGraphics = new EpsGraphics("Title", new FileOutputStream(FileLoc), 0, 0, dim.getWidth.toInt + 2, dim.getHeight.toInt, ColorMode.COLOR_RGB)
              deepdepcomponent.paint(g)
              g.flush()
              g.close()
            }
            else if (sel eq "LFG") {
              val dim: Dimension = LFGcomponent.getDimension
              val g: EpsGraphics = new EpsGraphics("Title", new FileOutputStream(FileLoc), 0, 0, dim.getWidth.toInt + 2, dim.getHeight.toInt, ColorMode.COLOR_RGB)
              LFGcomponent.paint(g)
              g.flush()
              g.close()
            }
            else if (sel eq "CCG") {
              val dim: Dimension = CCGcomponent.getDimension
              val g: EpsGraphics = new EpsGraphics("Title", new FileOutputStream(FileLoc), 0, 0, dim.getWidth.toInt + 2, dim.getHeight.toInt, ColorMode.COLOR_RGB)
              CCGcomponent.paint(g)
              g.flush()
              g.close()
            }
            JOptionPane.showMessageDialog(event.getSource.asInstanceOf[Component], "Export successful!")
          }
          catch {
            case e: FileNotFoundException =>
              JOptionPane.showMessageDialog(event.getSource.asInstanceOf[Component], "Invalid file directory, or you are not authorized to access.")
            case e: IOException =>
              JOptionPane.showMessageDialog(event.getSource.asInstanceOf[Component], "An I/O exception occurred.")
          }
        }
      }
    }

  }

  private def checkLFGValid() {
    val lackList: java.util.List[AttributeValueMatrix#FStructCheckResult] = LFGcomponent.isPredValid
    if (lackList != null) {
      var tipString: String = "Imcomplete governed grammatical function(s): "
      // join with ", "
      var j: Int = 0
      while (j < lackList.size) {
        {
          val item: AttributeValueMatrix#FStructCheckResult = lackList.get(j)
          tipString += String.format("%s of %s", item.reason, item.pred)
          if (j != lackList.size - 1) tipString += ", "
          else tipString += "."
        }
        ({
          j += 1; j - 1
        })
      }
      JOptionPane.showMessageDialog(this, tipString)
    }
    if (meta == null || meta.isEmpty) {
      return
    }
    val nonGovernable: java.util.List[AttributeValueMatrix#FStructCheckResult] = LFGcomponent.checkGovernable(meta)
    // join with ", "
    if (nonGovernable != null) {
      var tipString: String = "Non-governable grammatical function(s) is governed: "
      var j: Int = 0
      while (j < nonGovernable.size) {
        {
          val item: AttributeValueMatrix#FStructCheckResult = nonGovernable.get(j)
          tipString += String.format("%s of %s", item.reason, item.pred)
          if (j != nonGovernable.size - 1) tipString += ", "
          else tipString += "."
        }
        ({
          j += 1; j - 1
        })
      }
      JOptionPane.showMessageDialog(this, tipString)
    }
    val nonCoherence: java.util.List[String] = LFGcomponent.checkCoherence(meta)
    if (nonCoherence != null) {
      val tipString: String = "Redundant grammatical function(s): " + Utils.join(", ", nonCoherence) + "."
      JOptionPane.showMessageDialog(this, tipString)
    }
    val invalidGFName: java.util.List[String] = LFGcomponent.checkGramFuncName(meta)
    if (invalidGFName != null) {
      val tipString: String = "Undefined grammatical function(s): " + Utils.join(", ", invalidGFName) + "."
      JOptionPane.showMessageDialog(this, tipString)
    }
    val invalidFTName: java.util.List[String] = LFGcomponent.checkFeatureName(meta)
    if (invalidFTName != null) {
      val tipString: String = "Undefined feature(s): " + Utils.join(", ", invalidFTName) + "."
      JOptionPane.showMessageDialog(this, tipString)
    }
  }

  /**
    * This listener register edits to undoManager to enable undo/redo operation
    * Note that only insert/delete/text change operations are registered
    * style changes (coloring) are filtered
    *
    * @author shuoyang
    */
  private[LinguaView] class XMLUndoableEditListener extends UndoableEditListener {
    def undoableEditHappened(event: UndoableEditEvent) {
      if (!(event.getEdit.getPresentationName == UIManager.getString("AbstractDocument.styleChangeText"))) {
        val edit: UndoableEdit = event.getEdit
        undoManager.addEdit(edit)
      }
    }
  }

}