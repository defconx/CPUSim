/**
 * File: DesktopController
 * @author: Ben Borchard
 * Modified: 6/4/13
 */

/**
 * File: DesktopController
 * Author: Pratap Luitel, Scott Franchi, Stephen Webel
 * Date: 10/27/13
 *
 * Fields removed:
 *      private SimpleBooleanProperty machineDirty
 *      private File machineFile
 *      private SimpleStringProperty machineDirtyString
 *      private String currentMachineDirectory
 *
 * Methods added:
 *      public ArrayDeque<String> getReopenMachineFiles()
 *      public void setReopenMachineFiles()
 *      public ConsoleManager getConsoleManager()
 *
 * Methods removed:
 *      public void machineChanged()
 *      public SimpleStringProperty getMachineDirtyProperty()
 *      private void addMachineStateListeners()
 *      private void saveMachine()
 *      private void saveAsMachine()
 *      public void newMachine()
 *      public void openMachine(File fileToOpen)
 *
 * Methods modified:
 *      protected void handleNewMachine(ActionEvent event)
 *      protected void handleOpenMachine(ActionEvent event)
 *      public void updateReopenMachineMenu()
 *      public void updateReopenMachineFiles()
 *      private boolean confirmClosing()
 *      public void clearTables()
 *      public void loadPreferences()
 *      public void storePreferences()
 *      protected void handleSaveMachine(ActionEvent event)
 *      protected void handleSaveAsMachine(ActionEvent event)
 *      public void initFileChooser(FileChooser fileChooser, String title, boolean text)
 *
 */

/**
 * Michael Goldenberg, Jinghui Yu, and Ben Borchard modified this file on 11/7/13
 * with the following changes:
 *
 * 1.) added capability for the register and ram tables to handle the Unsigned Decimal 
 * and Ascii bases except in the ram address column
 *
 * on 11/25:
 *
 * 1.) Changed saveAsHTMLMachine() method so that the fileChooser dialog has the .html
 * file extenstion
 * filter
 * 2.) Changed saveAs() method so that the fileChooser dialog has the .a file
 * extenstion filter
 * extension
 *
 */
package cpusim.gui.desktop;

import com.sun.javafx.scene.control.behavior.TextInputControlBehavior;
import com.sun.javafx.scene.control.skin.TextInputControlSkin;
import cpusim.*;
import cpusim.assembler.Token;
import cpusim.gui.about.AboutController;
import cpusim.gui.desktop.editorpane.CodePaneController;
import cpusim.gui.desktop.editorpane.CodePaneTab;
import cpusim.gui.desktop.editorpane.LineNumAndBreakpointFactory;
import cpusim.gui.desktop.editorpane.StyleInfo;
import cpusim.gui.editmachineinstruction.EditMachineInstructionController;
import cpusim.gui.editmicroinstruction.EditMicroinstructionsController;
import cpusim.gui.editmodules.EditModulesController;
import cpusim.gui.equs.EQUsController;
import cpusim.gui.fetchsequence.EditFetchSequenceController;
import cpusim.gui.find.FindReplaceController;
import cpusim.gui.help.HelpController;
import cpusim.gui.options.OptionsController;
import cpusim.gui.preferences.PreferencesController;
import cpusim.microinstruction.IO;
import cpusim.module.RAM;
import cpusim.module.Register;
import cpusim.module.RegisterArray;
import cpusim.util.*;
import cpusim.xml.MachineHTMLWriter;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.print.PrinterJob;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.*;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.*;
import javafx.stage.FileChooser.ExtensionFilter;
import org.controlsfx.control.action.Action;
import org.controlsfx.dialog.Dialog;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.InlineStyleTextArea;
import org.fxmisc.richtext.Paragraph;
import org.fxmisc.richtext.StyledTextArea;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.prefs.Preferences;

/**
 * @author Ben Borchard
 */
public class DesktopController implements Initializable {

    @FXML
    private VBox mainPane;

    @FXML
    private TabPane textTabPane;

    @FXML
    private ChoiceBox<String> registerDataDisplayCB;
    @FXML
    private ChoiceBox<String> ramAddressDisplayCB;
    @FXML
    private ChoiceBox<String> ramDataDisplayCB;

    @FXML
    protected Menu fileMenu;
    @FXML
    protected Menu editMenu;
    @FXML
    protected Menu modifyMenu;
    @FXML
    protected Menu executeMenu;
    @FXML
    protected Menu helpMenu;

    @FXML
    private Menu reopenMachineMenu;
    @FXML
    private Menu reopenTextMenu;
    @FXML
    private Menu openRamMenu;
    @FXML
    private Menu saveRamMenu;

    @FXML
    private Label noRAMLabel;

    @FXML
    private VBox ramVbox;
    @FXML
    private VBox regVbox;

    @FXML
    private SplitPane regSplitPane;
    @FXML
    private SplitPane ramSplitPane;

    @FXML
    private ToolBar ramToolBar;

    @FXML
    private StackPane ioConsolePane;

    private StyledTextArea ioConsole;

    // System.getProperty("line.separator") doesn't work
    // on PCs. TextArea class may just use "\n".
    static final String NEWLINE = "\n";

    private String currentTextDirectory;
    private SimpleStringProperty tableStyle;

    private FontData textFontData;
    private FontData tableFontData;
    private HashMap<String, String> backgroundSetting;
    private OtherSettings otherSettings;

    private ArrayDeque<String> reopenTextFiles;
    private ArrayDeque<String> reopenMachineFiles;

    private ObservableList<RamTableController> ramControllers;
    private ObservableList<RegisterTableController> registerControllers;
    private DebugToolBarController debugToolBarController;
    private CodePaneController codePaneController;

    private MachineHTMLWriter htmlWriter; //HTML descriptions of the machine

    private Stage stage;

    private ObservableList<String> keyBindings;
    private ObservableList<ObjectProperty<KeyCodeCombination>> keyCodeCombinations;

    private Mediator mediator;

    private String regDataBase;
    private String ramAddressBase;
    private String ramDataBase;

    private HighlightManager highlightManager;
    private UpdateDisplayManager updateDisplayManager;
    private ConsoleManager consoleManager;

    private SimpleBooleanProperty inDebugMode;
    private SimpleBooleanProperty inRunningMode;
    private SimpleBooleanProperty inDebugOrRunningMode;
    private SimpleBooleanProperty noTabSelected;
    private SimpleBooleanProperty canUndoProperty;
    private SimpleBooleanProperty canRedoProperty;
    private SimpleBooleanProperty anchorEqualsCarret;
    private SimpleBooleanProperty codeStoreIsNull;

    private HelpController helpController;
    private FindReplaceController findReplaceController;

    private PrinterJob printController;

    public static final String SHORTCUT = System.getProperty("os.name").startsWith
            ("Windows") ? "Ctrl" : "Cmd";
    public static final String[] DEFAULT_KEY_BINDINGS = {
            SHORTCUT + "-N",               // new text file
            SHORTCUT + "-O",               // open text file
            SHORTCUT + "-W",               // close file
            SHORTCUT + "-S",               // save file
            SHORTCUT + "-Shift-S",         // save file as
            SHORTCUT + "-Shift-N",         // save machine
            SHORTCUT + "-Shift-O",         // open machine  
            SHORTCUT + "-B",               // save machine 
            SHORTCUT + "-Shift-B",         // save machine as
            SHORTCUT + "-Alt-B",           // save machine as html
            SHORTCUT + "-Alt-P",           // print preview
            SHORTCUT + "-Shift-P",         // print setup
            SHORTCUT + "-P",               // print 
            /* quit, undo, redo, cut, 
            copy, paste, delete, and 
            select all are not editable */
            SHORTCUT + "-Slash",           // toggle comment
            SHORTCUT + "-F",               // find
            SHORTCUT + "-Comma",           // preferences
            SHORTCUT + "-M",               // edit machine instructions
            SHORTCUT + "-Shift-M",         // edit microinstructions
            SHORTCUT + "-K",               // edit hardware modules
            SHORTCUT + "-E",               // edit EQUs 
            SHORTCUT + "-Y",               // edit fetch sequence
            SHORTCUT + "-D",               // toggle debug mode
            SHORTCUT + "-1",               // assemble
            SHORTCUT + "-2",               // assemble and load
            SHORTCUT + "-3",               // assemble, load and run
            SHORTCUT + "-G",               // clear, assemble, load, and run
            SHORTCUT + "-R",               // run   
            SHORTCUT + "-Period",          // stop
            SHORTCUT + "-Shift-R",         // reset everything
            SHORTCUT + "-L",               // clear console 
            SHORTCUT + "-I",               // edit options
            SHORTCUT + "-Shift-H",         // open general cpusim help
            SHORTCUT + "-Shift-A"          // open about cpusim
    };

    /**
     * constructor method that takes in a mediator and a stage
     *
     * @param mediator handles communication between the modules, assembler etc
     *                 and the desktop controller
     * @param stage    the stage used to display the desktop
     */
    public DesktopController(Mediator mediator, Stage stage) {
        mediator.setDesktopController(this);
        this.stage = stage;
        this.mediator = mediator;
        this.ioConsole = new CodeArea();

        highlightManager = new HighlightManager(mediator, this);
        updateDisplayManager = new UpdateDisplayManager(mediator, this);
        debugToolBarController = new DebugToolBarController(mediator, this);
        codePaneController = new CodePaneController(mediator);
    }

    /**
     * Initializes the desktop controller field
     *
     * @param url unused
     * @param rb  unused
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {

        // add the ioConsole to the ioConsolePane
        ioConsolePane.getChildren().add(ioConsole);

        // create the consoleManager
        consoleManager = new ConsoleManager(ioConsole);

        //initialize the list of controllers
        ramControllers = FXCollections.observableArrayList();
        registerControllers = FXCollections.observableArrayList();

        //initialize the html writer
        htmlWriter = new MachineHTMLWriter();

        tableStyle = new SimpleStringProperty("");

        //add listener for the stage for closing
        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent t) {
                boolean close = confirmClosing();
                if (close) {
                    if (helpController != null) {
                        helpController.getStage().close();
                    }
                    if (findReplaceController != null) {
                        findReplaceController.getStage().close();
                    }
                    storePreferences();
                }
                else {
                    t.consume();
                }

            }
        });

        //set up reopen queues
        reopenTextFiles = new ArrayDeque<String>();
        reopenMachineFiles = new ArrayDeque<String>();

        //initialize table and text data
        textFontData = new FontData();
        tableFontData = new FontData();

        backgroundSetting = new HashMap<String, String>() {{
            put("WHITE", "cpusim/gui/css/DefaultBackground.css");
            put("BLACK", "cpusim/gui/css/CustomerBackground1.css");
            put("AZURE", "cpusim/gui/css/CustomerBackground2.css");
            put("PURPLE", "cpusim/gui/css/CustomerBackground3.css");
            put("ORANGE", "cpusim/gui/css/CustomerBackground4.css");
            put("GREEN", "cpusim/gui/css/CustomerBackground5.css");
            put("MAGENTA", "cpusim/gui/css/CustomerBackground6.css");
        }};

        otherSettings = new OtherSettings();

        //init key bindings
        keyBindings = FXCollections.observableArrayList();
        keyCodeCombinations = FXCollections.observableArrayList();
        for (int i = 0; i < DEFAULT_KEY_BINDINGS.length; i++) {
            keyCodeCombinations.add(new SimpleObjectProperty<KeyCodeCombination>(null));
        }

        //find the screen width
        double screenwidth = Screen.getPrimary().getBounds().getWidth();
        double screenheight = Screen.getPrimary().getBounds().getHeight();

        //fit main pane to the screen (roughly)
        if (mainPane.getPrefWidth() > screenwidth) {
            mainPane.setPrefWidth(screenwidth - 75);
        }

        if (mainPane.getPrefHeight() > screenheight) {
            mainPane.setPrefHeight(screenheight - 40);
        }

        //load preferences
        loadPreferences();

        //initialize key bindings
        createKeyCodes();
        bindKeys();

        //initialize the values of the choice boxes
        registerDataDisplayCB.setValue(regDataBase);
        ramAddressDisplayCB.setValue(ramAddressBase);
        ramDataDisplayCB.setValue(ramDataBase);

        //add listeners to the choice button
        addBaseChangeListener(registerDataDisplayCB, "registerData");
        addBaseChangeListener(ramAddressDisplayCB, "ramAddress");
        addBaseChangeListener(ramDataDisplayCB, "ramData");

        // For disabling/enabling
        noTabSelected = new SimpleBooleanProperty();
        textTabPane.getSelectionModel().selectedItemProperty().addListener(
                new ChangeListener<Tab>() {
                    @Override
                    public void changed(ObservableValue<? extends Tab> arg0, Tab
                            oldTab, Tab newTab) {
                        noTabSelected.set(newTab == null);
                        if (newTab != null) {
                            final Node node = newTab.getContent();
                            Platform.runLater(new Runnable() {
                                @Override
                                public void run() {
                                    node.requestFocus();
                                }
                            });
                        }
                    }
                });

        // initialize simpleBooleanProperties and disables
        inDebugMode = new SimpleBooleanProperty(false);
        inRunningMode = new SimpleBooleanProperty(false);
        inDebugOrRunningMode = new SimpleBooleanProperty(false);
        inDebugOrRunningMode.bind(inDebugMode.or(inRunningMode));
        canUndoProperty = new SimpleBooleanProperty(false);
        canRedoProperty = new SimpleBooleanProperty(false);
        anchorEqualsCarret = new SimpleBooleanProperty(false);
        codeStoreIsNull = new SimpleBooleanProperty(true);
        bindItemDisablesToSimpleBooleanProperties();

        // Set up channels
        ((DialogChannel) (((BufferedChannel) (CPUSimConstants.DIALOG_CHANNEL))
                .getChannel())).setStage(stage);
        ((ConsoleChannel) (((BufferedChannel) (CPUSimConstants.CONSOLE_CHANNEL))
                .getChannel())).setMediator(mediator);

        // whenever a new tab in the code text area is selected,
        // set the line numbers according to the settings
        this.textTabPane.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldTab, newTab) -> {
                    if( newTab == null ) return; // there are no tabs left
                    StyledTextArea codeArea = (StyledTextArea) newTab.getContent();
                    LineNumAndBreakpointFactory lFactory =
                            (LineNumAndBreakpointFactory) codeArea.getParagraphGraphicFactory();
                    if (otherSettings.showLineNumbers.get()) {
                        lFactory.setFormat(digits ->  "%" + digits + "d");
                    }

                    else {
                        lFactory.setFormat(digits -> "");
                    }
                });
    }

    //================ handlers for FILE menu ==================================

    /**
     * Adds a new untitled empty tab to the text tab pane,
     * with new title.
     *
     * @param event action event that is unused
     */
    @FXML
    protected void handleNewText(ActionEvent event) {
        ObservableList<Tab> tabs = textTabPane.getTabs();
        ArrayList<String> titles = new ArrayList<String>();
        for (Tab tab : tabs) {
            titles.add(tab.getText().trim());
        }

        String s = "Untitled";
        int i = 0;
        while (titles.contains(s)) {
            i++;
            s = "Untitled " + i;
        }
        addTab("", s, null);
    }

    /**
     * Opens user specified text from computer memory.
     *
     * @param event action event that is unused
     */
    @FXML
    protected void handleOpenText(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
                /*fileChooser.getExtensionFilters().add(new ExtensionFilter("Assembly
                Language File (.a)",
                        "*.a"));*/
        initFileChooser(fileChooser, "Open Text", true);

        File fileToOpen = fileChooser.showOpenDialog(stage);
        if (fileToOpen == null) {
            return;
        }

        open(fileToOpen);
    }

    /**
     * Nothing needs to be done here.
     */
    @FXML
    protected void handleReopenText(ActionEvent event) {
    }

    /**
     * Closes whichever tab is selected.
     *
     * @param event unused action event
     */
    @FXML
    protected void handleCloseText(ActionEvent event) {
        Tab tab = textTabPane.getSelectionModel().getSelectedItem();
        closeTab(tab, true);
    }

    /**
     * Saves whichever tag is selected.
     * opens a file chooser if there is not already
     * a file associated with the tab.
     *
     * @param event unused action event
     */
    @FXML
    protected void handleSaveText(ActionEvent event) {
        Tab tab = textTabPane.getSelectionModel().getSelectedItem();
        save(tab);
    }

    /**
     * Allows the user to specify a file name and directory
     * to save the selected tab in.
     *
     * @param event unused action event
     */
    @FXML
    protected void handleSaveAsText(ActionEvent event) {
        Tab selectedTab = textTabPane.getSelectionModel().getSelectedItem();
        saveAs(selectedTab);
    }

    /**
     * Creates and opens a new machine.
     *
     * @param event unused action event
     */
    @FXML
    protected void handleNewMachine(ActionEvent event) {
        //CHANGE: mediator is polled for machineDirty
        if (mediator.isMachineDirty()) {

            Action response = CPUSimConstants.dialog.
                    owner(stage).
                    masthead("Save Machine").
                    message("The machine you are currently working on is unsaved.  " +
                            "Would you "
                            + "like to save it before you open a new machine?").
                    showConfirm();

            if (response == Dialog.ACTION_YES) {
                handleSaveMachine(event);
            }
            else if (response == Dialog.ACTION_CANCEL || response == Dialog
                    .ACTION_CLOSE) {
                return;
            }
        }

        Machine machine = new Machine("New");

        //CHANGE: changes to our machineFile and machineDirty are
        //changed in the mediator rather than in a DesktopController field
        mediator.setMachine(machine);
        mediator.setMachineFile(null);
        mediator.setMachineDirty(true);
        clearTables();
        setUpTables();
    }

    /**
     * opens a machine from a file
     * if the current machine is not saved, it will confirm the opening of a new
     * machine
     *
     * @param event unused
     */
    @FXML
    protected void handleOpenMachine(ActionEvent event) {
        //CHANGE: mediator is polled for machineDirty
        if (mediator.isMachineDirty()) {
            Action response = CPUSimConstants.dialog.
                    owner(stage).
                    masthead("Save Machine").
                    message("The machine you are currently working on is unsaved.  " +
                            "Would you "
                            + "like to save it before you open a new machine?").
                    showConfirm();
            if (response == Dialog.ACTION_YES) {
                handleSaveMachine(event);
            }
            else if (response == Dialog.ACTION_CANCEL || response == Dialog
                    .ACTION_CLOSE) {
                return;
            }
        }

        FileChooser fileChooser = new FileChooser();
                /*fileChooser.getExtensionFilters().add(new ExtensionFilter("Machine
                File (.cpu)",
                        "*.cpu"));*/

        initFileChooser(fileChooser, "Open Machine", false);


        //in the case that the tab is already open for the
        File fileToOpen = fileChooser.showOpenDialog(stage);
        if (fileToOpen == null) {
            return;
        }

        //CHANGE: mediator is asked to open the machine
        mediator.openMachine(fileToOpen);

    }

    /**
     * Nothing needs to be done here.
     *
     * @param event unused
     */
    @FXML
    protected void handleReopenMachine(ActionEvent event) {
    }

    /**
     * Saves the current Machine.
     *
     * @param event unused
     */
    @FXML
    protected void handleSaveMachine(ActionEvent event) {
        //CHANGE: mediator responsible for saving
        mediator.saveMachine();
    }

    /**
     * Save As for current machine.
     *
     * @param event unused
     */
    @FXML
    protected void handleSaveAsMachine(ActionEvent event) {
        //CHANGE: mediator responsible for saving
        mediator.saveAsMachine();
    }

    /**
     * Saves the Machine as HTML.
     *
     * @param event unused
     */
    @FXML
    protected void handleSaveAsHTMLMachine(ActionEvent event) throws
            FileNotFoundException {
        FileChooser fileChooser = new FileChooser();
        initFileChooser(fileChooser, "Save Machine", false);
        fileChooser.getExtensionFilters().add(new ExtensionFilter("Hyper Text Markup " +
                "File file (.html)",
                "*.html"));

        File fileToSave = fileChooser.showSaveDialog(stage);
        if (fileToSave == null) {
            return;
        }

        if (fileToSave.getAbsolutePath().lastIndexOf(".html") !=
                fileToSave.getAbsolutePath().length() - 5) {
            fileToSave = new File(fileToSave.getAbsolutePath() + ".html");
        }

        fileToSave = new File(fileToSave.getAbsolutePath() + ".html");

        PrintWriter printWriter = new PrintWriter(fileToSave);

        htmlWriter.writeMachineInHTML(mediator.getMachine(), printWriter);
    }

    /**
     * Not implemented yet...
     *
     * @param event
     */
    @FXML
    protected void handlePrintPreview(ActionEvent event) {
        tempPrintDialog();
    }

    /**
     * Not implemented yet...
     *
     * @param event
     */
    @FXML
    protected void handlePrintSetup(ActionEvent event) {

        // make sure there is a printController
        if (printController != null) {
            // show the printsetup dialog
            boolean success = printController.showPageSetupDialog(stage);
            // if they cancelled the dialog
            if (!success) {
                // Not sure what do do here
            }
        }
        else /* computer has no printers associated with it */ {
            // not sure what to do here...
        }
    }

    /**
     * Not implemented yet...
     *
     * @param event
     */
    @FXML
    protected void handlePrint(ActionEvent event) {

        // make sure there is a printController
        if (printController != null) {
            // show the print dialog
            boolean success = printController.showPrintDialog(stage);
            // if it wasn't cancelled the print the current tabs text area
            if (success) {
                printController.printPage(
                        textTabPane.getSelectionModel().getSelectedItem().getContent());
            }
        }
    }

    /**
     * Exits the program
     *
     * @param event the action event causing the quit request
     */
    @FXML
    protected void handleQuit(ActionEvent event) {
        boolean close = confirmClosing();
        if (close) {
            storePreferences();
            if (helpController != null) {
                helpController.getStage().close();
            }
            if (findReplaceController != null) {
                findReplaceController.getStage().close();
            }
            System.exit(0);
        }
    }


    //================= handlers for EDIT menu =================================

    /**
     * Undoes the last thing done in the current
     * text tab.
     */
    @FXML
    protected void handleUndo(ActionEvent event) {
        InlineStyleTextArea codeArea =
                (InlineStyleTextArea) textTabPane.getSelectionModel().getSelectedItem().getContent();
        TextInputControlBehavior<?> behavior =
                (((TextInputControlSkin<?, ?>) codeArea.getSkin()).getBehavior());
        behavior.callAction("Undo");
    }

    /**
     * Re-does the last thing that was un-done in the
     * current text tab.
     */
    @FXML
    protected void handleRedo(ActionEvent event) {
        InlineStyleTextArea codeArea =
                (InlineStyleTextArea) textTabPane.getSelectionModel().getSelectedItem().getContent();
        TextInputControlBehavior<?> behavior =
                (((TextInputControlSkin<?, ?>) codeArea.getSkin()).getBehavior());
        behavior.callAction("Redo");
    }

    /**
     * Cuts selected text in the selected tab
     *
     * @param event unused action event
     */
    @FXML
    protected void handleCut(ActionEvent event) {
        InlineStyleTextArea codeArea =
                (InlineStyleTextArea) textTabPane.getSelectionModel().getSelectedItem().getContent();
        TextInputControlBehavior<?> behavior =
                (((TextInputControlSkin<?, ?>) codeArea.getSkin()).getBehavior());
        behavior.callAction("Cut");
    }

    /**
     * copies selected text in the selected tab
     *
     * @param event unused action event
     */
    @FXML
    protected void handleCopy(ActionEvent event) {
        InlineStyleTextArea codeArea =
                (InlineStyleTextArea) textTabPane.getSelectionModel().getSelectedItem().getContent();
        TextInputControlBehavior<?> behavior =
                (((TextInputControlSkin<?, ?>) codeArea.getSkin()).getBehavior());
        behavior.callAction("Copy");
    }

    /**
     * Pastes text that was cut or copied into the selected tab.
     *
     * @param event unused action event
     */
    @FXML
    protected void handlePaste(ActionEvent event) {
        InlineStyleTextArea codeArea =
                (InlineStyleTextArea) textTabPane.getSelectionModel().getSelectedItem().getContent();
        TextInputControlBehavior<?> behavior =
                ((TextInputControlSkin<?, ?>) codeArea.getSkin()).getBehavior();
        behavior.callAction("Paste");
    }

    /**
     * Deletes selected text.
     *
     * @param event unused action event
     */
    @FXML
    protected void handleDelete(ActionEvent event) {
        InlineStyleTextArea codeArea =
                (InlineStyleTextArea) textTabPane.getSelectionModel().getSelectedItem().getContent();
        TextInputControlBehavior<?> behavior =
                ((TextInputControlSkin<?, ?>) codeArea.getSkin()).getBehavior();
        behavior.callAction("DeleteSelection");
    }

    /**
     * Selects all the text in the selected tab.
     *
     * @param event unused action event
     */
    @FXML
    protected void handleSelectAll(ActionEvent event) {
        InlineStyleTextArea codeArea =
                (InlineStyleTextArea) textTabPane.getSelectionModel().getSelectedItem().getContent();
        TextInputControlBehavior<?> behavior =
                ((TextInputControlSkin<?, ?>) codeArea.getSkin()).getBehavior();
        behavior.callAction("SelectAll");
    }

    /**
     * Toggles the selected text's lines to be commented
     * or uncommented.
     *
     * @param event unused action event
     */
    @FXML
    protected void handleToggleComment(ActionEvent event) {
        Tab currTab = textTabPane.getSelectionModel().getSelectedItem();
        InlineStyleTextArea ta = (InlineStyleTextArea) currTab.getContent();

        int lower = Math.min(ta.getCaretPosition(), ta.getAnchor());
        int upper = Math.max(ta.getCaretPosition(), ta.getAnchor());

        String text = ta.getText();

        //handle the case if there is a line at the end with nothing in it
        //give it something so that it will have its own line in the array
        if (text.endsWith(NEWLINE)) {
            text = text + " ";
        }

        String[] splitArray = text.split(NEWLINE);

        //undo the change we did before we split the array
        if (text.endsWith(NEWLINE)) {
            splitArray[splitArray.length - 1] = "";
        }


        if (text.length() == 0) {
            return;
        }

        int lineStart = -1;
        int lineEnd = -1;

        //index of the line on which the highlighting begins
        int F = 0;

        //index of the line on which the highlighting ends
        int L = 0;


        //initialize F and L
        for (int i = 0; i < splitArray.length; i++) {
            String line = splitArray[i];
            lineStart = lineEnd + 1;
            lineEnd = lineStart + line.length();

            if (lineStart <= lower && lower <= lineEnd) {
                F = i;
            }
            if (lineStart <= upper && upper <= lineEnd) {
                L = i;
            }
        }

        //get the current comment character
        String commentChar = String.valueOf(mediator.getMachine().getCommentChar());

        //the new text after to be put in the text area after after
        //comment characters are added/removed as appropriate
        String newText = "";

        //if F is not within the bounds of the array then get out of this method
        if (!(0 <= F && F < splitArray.length)) {
            return;
        }

        //determine whether we will be commenting or uncommenting by checking
        //the first character of the place where highlighting begins
        boolean commenting = false;
        for (int i = 0; i < splitArray.length; i++) {
            if (F <= i && i <= L) {
                commenting |= !splitArray[i].startsWith(commentChar);
            }
        }

        int numIncreasedChars = 0;

        //the line with comment approriately added or removed
        String editedLine = "";


        for (int i = 0; i < splitArray.length; i++) {
            String origLine = splitArray[i];
            if (F <= i && i <= L) {
                //Modification: changed the content of the if-else loop
                //				to have the toggle work properly
                if (commenting) {
                    editedLine = (commentChar + origLine);
                }
                //do not delete the first character if it isn't the comment
                //character
                else if (origLine.startsWith(commentChar)) {
                    editedLine = (origLine.substring(1));
                }
                else {
                    editedLine = origLine;
                }
                newText += editedLine;
                numIncreasedChars += editedLine.length() - origLine.length();
            }

            else {
                newText += origLine;
            }
            //add a newline character unless we are on the last line
            if (i != splitArray.length - 1) {
                newText += NEWLINE;
            }
        }

        // Note that the below implementation is a major hack.
        // We select the text we want to replace, paste the
        // contents in to replace, then return the Clipboard to
        // its original state. This way the actions can be un-done
        // and re-done.
        ta.selectAll();
        TextInputControlBehavior<?> ticb = (TextInputControlBehavior<?>)
                (((TextInputControlSkin<?, ?>) ta.getSkin()).getBehavior());
        Clipboard clipboard = Clipboard.getSystemClipboard();

        boolean setBack = true;
        DataFormat df = null;
        Object oldVal = null;
        try {
            df = (DataFormat) (clipboard.getContentTypes().toArray()[0]);
            oldVal = (clipboard.getContent(df));
        } catch (Exception e) {
            setBack = false;
        }

        ClipboardContent content = new ClipboardContent();
        content.putString(newText);
        clipboard.setContent(content);
        ticb.callAction("Paste");

        if (setBack) {
            ClipboardContent oldContent = new ClipboardContent();
            oldContent.put(df, oldVal);
            clipboard.setContent(oldContent);
        }

        if (commenting) {
            ta.selectRange(lower + 1, upper + numIncreasedChars);
        }
        else {
            ta.selectRange(lower - 1, upper + numIncreasedChars);
        }
    }

    /**
     * Opens the find/replace dialog.
     *
     * @param event unused action event
     */
    @FXML
    protected void handleFind(ActionEvent event) {
        if (findReplaceController == null) {
            findReplaceController = FindReplaceController.openFindReplaceDialog
                    (mediator, ioConsole.isFocused());
        }
        else {
            findReplaceController.setUseConsole(ioConsole.isFocused());
            findReplaceController.getStage().toFront();
            findReplaceController.getStage().requestFocus();
        }
    }

    /**
     * Opens the preferences dialog.
     *
     * @param event unused action event
     */
    @FXML
    protected void handlePreferences(ActionEvent event) {
        openModalDialog("Preferences", "gui/preferences/Preferences.fxml",
                new PreferencesController(mediator, this));
    }

    //============== handlers for MODIFY menu ==================================

    /**
     * Opens the machine instructions dialog.
     *
     * @param event
     */
    @FXML
    protected void handleMachineInstructions(ActionEvent event) {
        EditMachineInstructionController controller = new
                EditMachineInstructionController(mediator);
        openModalDialog("Edit Machine Instructions",
                "gui/editmachineinstruction/editMachineInstruction.fxml", controller);
    }

    /**
     * Opens the microinstructions dialog.
     *
     * @param event unused action event
     */
    @FXML
    protected void handleMicroinstructions(ActionEvent event) {
        EditMicroinstructionsController controller = new
                EditMicroinstructionsController(mediator);
        openModalDialog("Edit Microinstructions",
                "gui/editmicroinstruction/EditMicroinstructions.fxml", controller);
    }

    /**
     * Opens the hardware modules dialog.
     *
     * @param event unused action event
     */
    @FXML
    protected void handleHardwareModules(ActionEvent event) {
        openHardwareModulesDialog(0);
    }

    /**
     * Opens the global EQUs dialog.
     *
     * @param event unused action event
     */
    @FXML
    protected void handleEQUs(ActionEvent event) {
        openModalDialog("EQUs", "gui/equs/EQUs.fxml",
                new EQUsController(mediator));
    }

    /**
     * Opens the Fetch Sequence dialog.
     *
     * @param event unused action event
     */
    @FXML
    protected void handleFetchSequence(ActionEvent event) {
        EditFetchSequenceController controller = new EditFetchSequenceController
                (mediator);
        openModalDialog("Edit Fetch Sequence",
                "gui/fetchsequence/editFetchSequence.fxml", controller);
    }


    //================= handlers for the EXECUTE menu ==========================

    /**
     * Method called when user clicks "Debug"
     * within the Execute drop-down menu.
     *
     * @param event - unused event.
     */
    @FXML
    protected void handleDebug(ActionEvent event) {
        setInDebugMode(!getInDebugMode());
    }

    /**
     * Method called when user clicks "Assemble"
     * within the Execute drop-down menu.
     *
     * @param event - unused event.
     */
    @FXML
    protected void handleAssemble(ActionEvent event) {
        File currFile = getFileToAssemble();
        if (currFile != null) {
            mediator.Assemble(currFile.getPath());
        }
    }

    /**
     * Method called when user clicks "Assemble
     * & Load" from the Execute drop-down menu.
     *
     * @param event - unused event.
     */
    @FXML
    protected void handleAssembleLoad(ActionEvent event) {
        File currFile = getFileToAssemble();
        if (currFile != null) {
            mediator.AssembleLoad(currFile.getPath());
        }
    }

    /**
     * Method called when user clicks "Assemble,
     * Load, & Run" from the Execute drop-down menu.
     *
     * @param event - unused event.
     */
    @FXML
    protected void handleAssembleLoadRun(ActionEvent event) {
        File currFile = getFileToAssemble();
        if (currFile != null) {
            mediator.AssembleLoadRun(currFile.getPath());
        }
    }

    /**
     * Method called when user clicks "Clear, Assemble,
     * Load, & Run" from the Execute drop-down menu.
     *
     * @param event - unused event.
     */
    @FXML
    protected void handleClearAssembleLoadRun(ActionEvent event) {
        File currFile = getFileToAssemble();
        if (currFile != null) {
            mediator.ClearAssembleLoadRun(currFile.getPath());
        }
    }

    /**
     * Runs the current program through the mediator.
     *
     * @param event - unused event.
     */
    @FXML
    protected void handleRun(ActionEvent event) {
        File currFile = getFileToAssemble();
        if (currFile != null) {
            mediator.Run();
        }
    }

    /**
     * Stops the currently running program
     * through the mediator.
     *
     * @param event - unused event.
     */
    @FXML
    protected void handleStop(ActionEvent event) {
        mediator.Stop();
    }

    /**
     * Resets everything, all RAM and RAM arrays.
     * Done through the mediator.
     *
     * @param event - unused event.
     */
    @FXML
    protected void handleResetEverything(ActionEvent event) {
        mediator.ResetEverything();
    }

    /**
     * Resets everything, all RAM and RAM arrays.
     * Done through the mediator.
     *
     * @param event - unused event.
     */
    @FXML
    protected void handleClearConsole(ActionEvent event) {
        ioConsole.clear();
    }

    /**
     * Opens the Options dialog.
     *
     * @param event - unused event.
     */
    @FXML
    protected void handleOptions(ActionEvent event) {
        openOptionsDialog(0);
    }

    //================= handler for HELP menu ==================================

    /**
     * Opens the help dialog.
     *
     * @param event unused action event
     */
    @FXML
    protected void handleGeneralCPUSimHelp(ActionEvent event) {
        if (helpController == null) {
            helpController = HelpController.openHelpDialog(this);
        }
        else {
            helpController.selectTreeItem("Introduction");
            helpController.getStage().toFront();
        }
    }

    /**
     * Opens the about dialog.
     *
     * @param event unused action event
     */
    @FXML
    protected void handleAboutCPUSim(ActionEvent event) {
//        openModalDialog("About CPU Sim", "gui/about/AboutFXML.fxml",
//                new AboutController());
        Tab tab = textTabPane.getSelectionModel().getSelectedItem();
        StyledTextArea codeArea = (StyledTextArea) tab.getContent();
        System.out.println(((LineNumAndBreakpointFactory) codeArea
                .getParagraphGraphicFactory()).getBreakPointLines());

    }

    //======================= auxiliary methods ================================

    /**
     * If the user has unsaved content in a tab, this handles
     * asking the user if he would like to save it before closing.
     *
     * @param event unused action event
     */
    @FXML
    protected void handleTabClosed(Event event) {
        Tab closingTab = (Tab) event.getSource();
        closeTab(closingTab, false);
    }

    /**
     * Closes a tab, when close = true. In some cases
     * the closing is already set in progress and we
     * only need do a few other things to keep everything
     * up to date. Use this method with close = false for
     * this occasion.
     *
     * @param tab   - The tab to close.
     * @param close - boolean to indicate whether we
     *              should also close the tab here. If not, it is assumed
     *              it is done elsewhere.
     */
    private void closeTab(Tab tab, boolean close) {
        if (((CodePaneTab) tab).getDirty()) {
            Action response = CPUSimConstants.dialog.
                    owner(stage).
                    masthead("Save File").
                    message("Would you like to save your work before you close this " +
                            "tab?").
                    showConfirm();
            System.out.println(response);
            if (response == Dialog.ACTION_YES) {
                if (save(tab) && close) {
                    textTabPane.getTabs().remove(tab);
                }
            }
            else if (response == Dialog.ACTION_NO) {
                if (close) {
                    textTabPane.getTabs().remove(tab);
                }
            }
            else {
                if (!close) {
                    textTabPane.getTabs().add(tab);
                    textTabPane.getSelectionModel().selectLast();
                }
            }
        }
        else {
            if (close) {
                textTabPane.getTabs().remove(tab);
            }
        }
    }


    /**
     * get the File to be assembled.
     * It is the file associated with the currently-selected tab in the desktop.
     * If there are no tabs, null is returned.  If there is a tab, but there is
     * no associated file or if the tab is dirty, a dialog is opened to save the
     * tab to a file.  If the user cancels, null is returned.
     *
     * @return the File to be assembled.
     */
    public File getFileToAssemble() {
        CodePaneTab currTab = (CodePaneTab) textTabPane.getSelectionModel()
                .getSelectedItem();
        if (currTab.getFile() != null && !currTab.getDirty()) {
            return currTab.getFile();
        }
        else if (otherSettings.autoSave) {
            boolean savedSuccessfully = save(currTab);
            if (savedSuccessfully) {
                return currTab.getFile();
            }
        }
        else {  //there is no file or there is a file but the tab is dirty.
            Action response = CPUSimConstants.dialog.
                    owner(stage).
                    actions(Dialog.ACTION_CANCEL, Dialog.ACTION_OK).
                    masthead("Save File?").
                    message("Current Tab is not saved. It needs to be saved"
                            + " before assembly. Save and continue?").
                    showConfirm();
            if (response == Dialog.ACTION_OK) {
                boolean savedSuccessfully = save(currTab);
                if (savedSuccessfully) {
                    return currTab.getFile();
                }
            }
        }
        return null;
    }

    /**
     * adds a new tab to the text tab pane
     *
     * @param content the text that is in the file
     * @param title   the title of the file
     * @param file    the file object to be associated with this tab (null for unsaved
     *                files)
     */
    public void addTab(String content, String title, File file) {
        // create the new tab and text area
        final CodePaneTab newTab = new CodePaneTab();
        final File f = file;
        InlineStyleTextArea<StyleInfo> codeArea =
                new InlineStyleTextArea<>(new StyleInfo(), StyleInfo::toCss);
        codeArea.setParagraphGraphicFactory(LineNumAndBreakpointFactory.get(codeArea,
                otherSettings.showLineNumbers.get() ? (digits -> "%" + digits + "d") :
                                                      (digits -> "")));
        newTab.setContent(codeArea);

        // whenever the text is changed, recompute the highlighting and set it dirty
        codeArea.textProperty().addListener((obs, oldText, newText) -> {
            codeArea.setStyleSpans(0, codePaneController.computeHighlighting(newText));
            newTab.setDirty(true);
        });

        // add the content, set what to do when closed, and set the tooltip
        codeArea.replaceText(0, 0, content);
        newTab.setDirty(false); // not initially dirty
        newTab.setOnClosed(this::handleTabClosed);
        if (file != null) {
            newTab.setTooltip(new Tooltip(file.getAbsolutePath()));
        }
        else {
            newTab.setTooltip(new Tooltip("File has not been saved."));
        }

        // set the file, title, and context menu
        newTab.setFile(file);
        newTab.setText(title);
        addContextMenu(newTab);

        textTabPane.getTabs().add(newTab);
        textTabPane.getSelectionModel().selectLast();
    }

    /**
     * creates and adds a context menu for the new Tab
     * @param newTab the Tab that gets the context menu
     */
    private void addContextMenu(CodePaneTab newTab) {
        //set up the context menu
        MenuItem close = new MenuItem("Close");
        close.setOnAction(e -> closeTab(newTab, true));

        MenuItem closeAll = new MenuItem("Close All");
        closeAll.setOnAction(e -> {
            ArrayList<Tab> tabs = new ArrayList<Tab>();
            for (Tab tab : textTabPane.getTabs()) {
                tabs.add(tab);
            }
            for (Tab tab : tabs) {
                closeTab(tab, true);
            }
        });

        MenuItem closeOthers = new MenuItem("Close Others");
        closeOthers.setOnAction(e -> {
            ArrayList<Tab> tabs = new ArrayList<Tab>();
            for (Tab tab : textTabPane.getTabs()) {
                tabs.add(tab);
            }
            for (Tab tab : tabs) {
                if (!tab.equals(newTab)) {
                    closeTab(tab, true);
                }
            }
        });

        MenuItem copyPath = new MenuItem("Copy Path Name");
        copyPath.setOnAction(e -> {
            if (newTab.getFile() != null) {
                Clipboard clipboard = Clipboard.getSystemClipboard();
                ClipboardContent content1 = new ClipboardContent();
                content1.putString(newTab.getFile().getAbsolutePath());
                clipboard.setContent(content1);
            }
        });
        copyPath.disableProperty().bind(
                newTab.tooltipProperty().get().textProperty().isEqualTo("File has not " +
                        "been saved."));

        ContextMenu cm = new ContextMenu();
        cm.getItems().addAll(close, closeAll, closeOthers, copyPath);
        newTab.setContextMenu(cm);
    }

    /**
     * Returns the tab for the given assembly text file.
     * If the file is not already open, it is opened and the new tab is returned.
     *
     * @param file the file contains the assembly program
     * @return the tab
     */
    public Tab getTabForFile(File file) {
        assert file != null : "Null passed as parameter to getTabForFile";
        Optional<Tab> existingTab = textTabPane.getTabs().stream()
                .filter(t -> file.equals(((CodePaneTab) t).getFile()))
                .findFirst();
        if (existingTab.isPresent()) {
            Tab currentTab = existingTab.get();
            textTabPane.getSelectionModel().select(currentTab);
            return currentTab;
        }
        else {
            open(file);
            return textTabPane.getTabs().get(textTabPane.getTabs().size() - 1);
        }
    }

    /**
     * displays a message listing all the halt bits that are set.
     */
    public void displayHaltBitsThatAreSet() {
        Vector setHaltedBits = mediator.getMachine().haltBitsThatAreSet();
        if (setHaltedBits.size() > 0) {
            String message = "The following halt condition bits are set:  ";
            for (int i = 0; i < setHaltedBits.size(); i++)
                message += setHaltedBits.elementAt(i) + "  ";
            consoleManager.printlnToConsole(message);
        }
    }

    /**
     * returns the stage
     *
     * @return the stage
     */
    public Stage getStage() {
        return stage;
    }

    /**
     * Returns the pane of tabs.
     *
     * @return the pane of tabs.
     */
    public TabPane getTextTabPane() {
        return textTabPane;
    }

    /**
     * Returns the ioConsole TextArea.
     *
     * @return the ioConosle TextArea.
     */
    public StyledTextArea getIOConsole() {
        return ioConsole;
    }

    /**
     * returns the hightlightManager.
     *
     * @return the hightlightManager.
     */
    public HighlightManager getHighlightManager() {
        return this.highlightManager;
    }

    /**
     * returns the updateDisplayManager.
     *
     * @return the updateDisplayManager.
     */
    public UpdateDisplayManager getUpdateDisplayManager() {
        return this.updateDisplayManager;
    }

    /**
     * Getter for the help controller.
     * Null if there is no window open right now.
     *
     * @return The current HelpController.
     */
    public HelpController getHelpController() {
        return helpController;
    }

    /**
     * Sets the current help controller.
     * Done when opening a new help window.
     *
     * @param hc The HelpController reference.
     */
    public void setHelpController(HelpController hc) {
        helpController = hc;
    }

    /**
     * Gives the current FindReplaceController.
     *
     * @return the current FindReplaceController.
     */
    public FindReplaceController getFindReplaceController() {
        return findReplaceController;
    }

    /**
     * Sets the current FindReplaceController.
     *
     * @param frc the current FindReplaceController.
     */
    public void setFindReplaceController(FindReplaceController frc) {
        this.findReplaceController = frc;
    }
//------------------------------------------------------------------------------
//Added methods by Pratap, Scott, and Stephen

    /**
     * Gives the current reopenMachineFiles value.
     *
     * @return the current reopenMachineFiles.
     */
    public ArrayDeque<String> getReopenMachineFiles() {
        return reopenMachineFiles;
    }

    /**
     * Sets the current reopenMachineFiles.
     *
     * @param s the string to set the current reopenMachineFiles to.
     */
    public void setReopenMachineFiles(ArrayDeque<String> s) {
        this.reopenMachineFiles = s;
    }

    /**
     * returns the consoleManager.
     *
     * @return the consoleManager.
     */
    public ConsoleManager getConsoleManager() {
        return this.consoleManager;
    }
//------------------------------------------------------------------------------

    /**
     * Sets the desktop into debug mode
     * if b is true, into regular mode if false.
     *
     * @param inDebug true for debug mode, false for
     *                normal mode
     */
    private void setInDebugMode(boolean inDebug) {
        inDebugMode.set(inDebug);
        if (inDebug) {
            mediator.getMachine().getControlUnit().reset();
            mediator.getMachine().resetAllChannels();

            debugToolBarController.updateDisplay(true, false, mediator);
            //debugToolBarController = new DebugToolBarController(mediator, this);
            mainPane.getChildren().add(1, debugToolBarController);
            debugToolBarController.prefWidthProperty().bind(mainPane.widthProperty());
        }
        else {
            mainPane.getChildren().remove(1);
            debugToolBarController.clearAllOutlines();
            mediator.getBackupManager().flushBackups();
            mediator.getMachine().getControlUnit().setMicroIndex(0);
        }
        RAM codeStore = mediator.getMachine().getCodeStore();
        if (codeStore != null) {
            codeStore.setHaltAtBreaks(inDebug);
        }
        mediator.getBackupManager().setListening(inDebug);
        ((CheckMenuItem) (executeMenu.getItems().get(0))).setSelected(inDebug);
    }

    /**
     * Returns the boolean describing whether or not
     * the desktop is currently in debug mode.
     *
     * @return boolean describing whether or not
     * the desktop is currently in debug mode.
     */
    public boolean getInDebugMode() {
        return inDebugMode.get();
    }

    /**
     * Gives the SimpleBooleanProperty describing
     * whether or not we are in debug mode.
     *
     * @return The SimpleBooleanProperty describing
     * whether or not we are in debug mode.
     */
    public SimpleBooleanProperty inDebugModeProperty() {
        return inDebugMode;
    }

    /**
     * Notifies the desktop that the machine is in
     * running mode.
     */
    public void setInRunningMode(boolean irm) {
        inRunningMode.set(irm);
    }

    /**
     * Returns a boolean describing whether
     * or not we are currently in running mode.
     */
    public boolean getInRunningMode() {
        return inRunningMode.get();
    }

    /**
     * Returns the SimpleBooleanProperty describing whether
     * or not we are currently in running mode.
     */
    public SimpleBooleanProperty inRunningModeProperty() {
        return inRunningMode;
    }

    /**
     * Returns the SimpleBooleanProperty describing whether
     * or not we are currently in running mode or currently
     * in debugging mode.
     */
    public SimpleBooleanProperty inDebugOrRunningModeProperty() {
        return inDebugOrRunningMode;
    }

    /**
     * Binds all the menu items to the appropriate
     * SimpleBooleanProperties so that the are
     * enabled/disabled appropriately.
     */
    public void bindItemDisablesToSimpleBooleanProperties() {

        // File Menu
        fileMenu.setOnMenuValidation(new EventHandler<Event>() {
            @Override
            public void handle(Event event) {
                for (int i = 7; i < 9; i++) {
                    fileMenu.getItems().get(i).setDisable(ramControllers.isEmpty());
                }
            }
        });
        // Close Text
        fileMenu.getItems().get(3).disableProperty().bind(noTabSelected);
        // Save Text
        fileMenu.getItems().get(4).disableProperty().bind(noTabSelected);
        // Save Text As
        fileMenu.getItems().get(5).disableProperty().bind(noTabSelected);
        // New Machine
        fileMenu.getItems().get(10).disableProperty().bind(inRunningMode);
        // Open Machine
        fileMenu.getItems().get(11).disableProperty().bind(inRunningMode);
        // Reopen Machine
        fileMenu.getItems().get(12).disableProperty().bind(inRunningMode);
        // Print Preview
        fileMenu.getItems().get(18).disableProperty().bind(noTabSelected);
        // Print
        fileMenu.getItems().get(20).disableProperty().bind(noTabSelected);


        // Edit Menu
        editMenu.setOnMenuValidation(event -> {
//            boolean canUndo = false;
//            boolean canRedo = false;
//            boolean ancEqCar = false;
//            if (!noTabSelected.get()) {
//                Tab currTab = textTabPane.getSelectionModel().getSelectedItem();
//                InlineStyleTextArea codeArea = (InlineStyleTextArea) currTab.getContent();
//                Object o = codeArea.getSkin();
//                System.out.println(o.getClass());
//                TextInputControlBehavior<?> behavior =
//                        ((TextInputControlSkin<?, ?>) codeArea.getSkin()).getBehavior();
//                canUndo = behavior.canUndo();
//                canRedo = behavior.canRedo();
//                ancEqCar = (codeArea.getAnchor() == codeArea.getCaretPosition());
//            }
//            canUndoProperty.set(canUndo);
//            canRedoProperty.set(canRedo);
//            anchorEqualsCarret.set(ancEqCar);
        });
        // Undo
        editMenu.getItems().get(0).disableProperty().bind(noTabSelected.or
                (canUndoProperty.not()));
        // Redo
        editMenu.getItems().get(1).disableProperty().bind(noTabSelected.or
                (canRedoProperty.not()));
        // Cut
        editMenu.getItems().get(3).disableProperty().bind(noTabSelected.or
                (anchorEqualsCarret));
        // Copy
        editMenu.getItems().get(4).disableProperty().bind(noTabSelected.or
                (anchorEqualsCarret));
        // Paste
        editMenu.getItems().get(5).disableProperty().bind(noTabSelected);
        // Delete
        editMenu.getItems().get(6).disableProperty().bind(noTabSelected.or
                (anchorEqualsCarret));
        // Select All
        editMenu.getItems().get(7).disableProperty().bind(noTabSelected);
        // Toggle Comment
        editMenu.getItems().get(9).disableProperty().bind(noTabSelected);
        // Find
        editMenu.getItems().get(10).disableProperty().bind(noTabSelected);

        // Modify Menu
        modifyMenu.disableProperty().bind(inDebugOrRunningMode);
        // All sub-items disabled at same time.
        for (int i = 0; i < 5; i++) {
            modifyMenu.getItems().get(i).disableProperty().bind(inDebugOrRunningMode);
        }

        // Execute Menu
        // Debug Mode
        executeMenu.getItems().get(0).disableProperty().bind((inRunningMode.or
                (noTabSelected)).or(
                codeStoreIsNull));
        // Assemble
        executeMenu.getItems().get(2).disableProperty().bind(noTabSelected.or(
                codeStoreIsNull));
        // Assemble & Load
        executeMenu.getItems().get(3).disableProperty().bind(noTabSelected.or(
                codeStoreIsNull));
        // Assemble Load & Run
        executeMenu.getItems().get(4).disableProperty().bind((inDebugOrRunningMode
                .or(noTabSelected)).or(codeStoreIsNull));
        // Clear, assemble, load & run
        executeMenu.getItems().get(5).disableProperty().bind((inDebugOrRunningMode
                .or(noTabSelected)).or(codeStoreIsNull));
        // Run
        executeMenu.getItems().get(6).disableProperty().bind((inDebugOrRunningMode
                .or(noTabSelected)).or(codeStoreIsNull));
        // Stop
        executeMenu.getItems().get(7).disableProperty().bind(inRunningMode.not());
        // Reset Everything
        executeMenu.getItems().get(8).disableProperty().bind(inRunningMode);
        // IO Options
        executeMenu.getItems().get(10).disableProperty().bind(inDebugOrRunningMode.or(
                codeStoreIsNull));
        // Update codeStoreIsNull
        executeMenu.setOnMenuValidation(new EventHandler<Event>() {
            @Override
            public void handle(Event event) {
                codeStoreIsNull.set(mediator.getMachine().getCodeStore() == null);
            }
        });

        inRunningMode.addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> arg0,
                                Boolean oldVal, Boolean newVal) {
                boolean inRunMode = newVal.booleanValue();
                ObservableList<Microinstruction> ios = mediator.getMachine().getMicros
                        ("io");
                boolean consoleIsInputOrOutputChannel = false;
                for (Microinstruction micro : ios) {
                    IO io = (IO) micro;
                    if (io.getConnection().equals(CPUSimConstants.CONSOLE_CHANNEL)) {
                        consoleIsInputOrOutputChannel = true;
                    }
                }
                if (consoleIsInputOrOutputChannel) {
                    if (inRunMode) {
                        ioConsole.setStyle("-fx-background-color: yellow");
                        ioConsole.requestFocus();
                    }
                    else {
                        ioConsole.setStyle("-fx-background-color: white");
                    }
                }
            }
        });
    }

    /**
     * adds a change listener to a choice box so that it can keep track of which
     * choice is selected and do things cased on that
     *
     * @param choiceBox The ChoiceBox who is getting the listener added
     * @param type      a String indicating the type of TableView that the ChoiceBox
     *                  affects ("registerData", "ramData", "ramAddress")
     */
    public void addBaseChangeListener(ChoiceBox<String> choiceBox, String type) {
        final String finalType = type;
        choiceBox.getSelectionModel().selectedIndexProperty().addListener(
                new ChangeListener<Number>() {
                    @Override
                    public void changed(ObservableValue ov, Number value, Number
                            new_value) {
                        if (finalType.equals("registerData")) {
                            if (new_value.equals(0)) {
                                regDataBase = "Dec";
                            }
                            else if (new_value.equals(1)) {
                                regDataBase = "Bin";
                            }
                            else if (new_value.equals(2)) {
                                regDataBase = "Hex";
                            }
                            else if (new_value.equals(3)) {
                                regDataBase = "Unsigned Dec";
                            }
                            else {
                                regDataBase = "Ascii";
                            }
                            for (RegisterTableController registerTableController
                                    : registerControllers) {
                                registerTableController.setDataBase(regDataBase);
                            }
                        }
                        else if (finalType.equals("ramAddress")) {
                            if (new_value.equals(0)) {
                                ramAddressBase = "Dec";
                            }
                            else if (new_value.equals(1)) {
                                ramAddressBase = "Bin";
                            }
                            else {
                                ramAddressBase = "Hex";
                            }
                            for (RamTableController ramTableController
                                    : ramControllers) {
                                ramTableController.setAddressBase(ramAddressBase);
                            }
                        }
                        else {  //type == "ramData"
                            if (new_value.equals(0)) {
                                ramDataBase = "Dec";
                            }
                            else if (new_value.equals(1)) {
                                ramDataBase = "Bin";
                            }
                            else if (new_value.equals(2)) {
                                ramDataBase = "Hex";
                            }
                            else if (new_value.equals(3)) {
                                ramDataBase = "Unsigned Dec";
                            }
                            else {
                                ramDataBase = "Ascii";
                            }
                            for (RamTableController ramTableController
                                    : ramControllers) {
                                ramTableController.setDataBase(ramDataBase);
                            }
                        }
                    }
                });
    }

    /**
     * gets the register controllers in an observable list.
     *
     * @return the observable list of registercontrollers
     */
    public ObservableList<RegisterTableController> getRegisterController() {
        return registerControllers;
    }

    /**
     * gets the ram controller in an observable list.
     *
     * @return the observable list of ramcontrollers.
     */
    public ObservableList<RamTableController> getRAMController() {
        return ramControllers;
    }

    /**
     * gets the DebugToolbarController.
     *
     * @return the DebugToolbarController.
     */
    public DebugToolBarController getDebugToolBarController() {
        return debugToolBarController;
    }

    /**
     * Opens the hardware modules dialog with the
     * specified selected section index.
     *
     * @param initialSection
     */
    public void openHardwareModulesDialog(int initialSection) {
        if (0 <= initialSection && initialSection <= 3) {
            EditModulesController controller = new EditModulesController(mediator, this);
            openModalDialog("Edit Modules",
                    "gui/editmodules/EditModules.fxml", controller);
            controller.selectSection(initialSection);
        }
        else {
            openHardwareModulesDialog(0);
        }
    }

    /**
     * Opens the hardware modules dialog with the
     * specified selected section index.
     *
     * @param initialSection
     */
    public void openOptionsDialog(int initialSection) {
        if (0 <= initialSection && initialSection <= 3) {
            OptionsController controller = new OptionsController(mediator);
            openModalDialog("Options", "gui/options/OptionsFXML.fxml",
                    controller);
            controller.selectTab(initialSection);
        }
        else {
            openOptionsDialog(0);
        }
    }

    /**
     * Opens a dialog modal to the main window.
     *
     * @param title    - desired title for the window to be created.
     * @param fxmlPath - path to the fxml file that contains the
     *                 formatting for the window to be opened.
     */
    public void openModalDialog(String title, String fxmlPath) {
        openModalDialog(title, fxmlPath, null);
    }

    /**
     * Opens a dialog modal to the main window.
     *
     * @param title      - desired title for the window to be created.
     * @param fxmlPath   - path to the fxml file that contains the
     *                   formatting for the window to be opened.
     * @param controller - The controller of the FXML.
     */
    public void openModalDialog(String title, String fxmlPath, Object controller) {

        openModalDialog(title, fxmlPath, controller, -1, -1);
    }

    /**
     * Opens a dialog modal to the main window.
     *
     * @param title      - desired title for the window to be created.
     * @param fxmlPath   - path to the fxml file that contains the
     *                   formatting for the window to be opened.
     * @param controller - The controller of the FXML.
     * @param x          - the horizonal distance from the left edge of the
     *                   desktop window to the left edge of the new window.
     * @param y          - the vertical distance from the top edge of the
     *                   desktop window to the top edge of the new window.
     */
    public void openModalDialog(String title, String fxmlPath, Object controller, int
            x, int y) {
        openDialog(title, fxmlPath, controller, x, y, Modality.WINDOW_MODAL);
    }

    /**
     * Opens a dialog with no modality.
     *
     * @param title    - desired title for the window to be created.
     * @param fxmlPath - path to the fxml file that contains the
     *                 formatting for the window to be opened.
     */
    public void openNonModalDialog(String title, String fxmlPath) {
        openNonModalDialog(title, fxmlPath, null);
    }

    /**
     * Opens a dialog with no modality.
     *
     * @param title      - desired title for the window to be created.
     * @param fxmlPath   - path to the fxml file that contains the
     *                   formatting for the window to be opened.
     * @param controller - The controller of the FXML.
     */
    public void openNonModalDialog(String title, String fxmlPath, Object controller) {
        openNonModalDialog(title, fxmlPath, controller, -1, -1);
    }

    /**
     * Opens a dialog with no modality.
     *
     * @param title      - desired title for the window to be created.
     * @param fxmlPath   - path to the fxml file that contains the
     *                   formatting for the window to be opened.
     * @param controller - The controller of the FXML.
     * @param x          - the horizonal distance from the left edge of the
     *                   desktop window to the left edge of the new window.
     * @param y          - the vertical distance from the top edge of the
     *                   desktop window to the top edge of the new window.
     */
    public void openNonModalDialog(String title, String fxmlPath, Object controller,
                                   int x, int y) {
        openDialog(title, fxmlPath, controller, x, y, Modality.NONE);
    }

    /**
     * Private generic method to open a new window.
     *
     * @param title      - desired title for the window to be created.
     * @param fxmlPath   - path to the fxml file that contains the
     *                   formatting for the window to be opened.
     * @param controller - The controller of the FXML.
     * @param x          - the horizonal distance from the left edge of the
     *                   desktop window to the left edge of the new window.
     * @param y          - the vertical distance from the top edge of the
     *                   desktop window to the top edge of the new window.
     * @param modality   - The modality of the new window.
     */
    private void openDialog(String title, String fxmlPath,
                            Object controller, int x, int y, Modality modality) {
        FXMLLoader fxmlLoader = new FXMLLoader(mediator.getClass().getResource(fxmlPath));
        if (controller != null) {
            fxmlLoader.setController(controller);
        }
        final Stage dialogStage = new Stage();
        // Load in icon for the new dialog
        URL url = getClass().getResource("/cpusim/gui/about/cpusim_icon.jpg");
        Image icon = new Image(url.toExternalForm());
        dialogStage.getIcons().add(icon);
        if (controller instanceof PreferencesController) {
            dialogStage.setResizable(false);
        }
        Pane dialogRoot = null;

        try {
            dialogRoot = (Pane) fxmlLoader.load();
        } catch (IOException e) {
            //TODO: something better...
            System.out.println(e.getMessage());
        }
        Scene dialogScene = new Scene(dialogRoot);
        dialogStage.setScene(dialogScene);
        dialogStage.initOwner(stage);
        dialogStage.initModality(modality);
        dialogStage.setTitle(title);
        if (x >= 0 && y >= 0) {
            dialogStage.setX(stage.getX() + x);
            dialogStage.setY(stage.getY() + y);
        }
        dialogScene.addEventFilter(
                KeyEvent.KEY_RELEASED, new EventHandler<KeyEvent>() {
                    @Override
                    public void handle(KeyEvent event) {
                        if (event.getCode().equals(KeyCode.ESCAPE)) {
                            if (dialogStage.isFocused()) {
                                dialogStage.close();
                            }
                        }
                    }
                });
        dialogStage.show();
    }

    /**
     * gives a fileChooser object certain properties
     *
     * @param fileChooser fileChooser to be modified
     * @param title       title of fileChooser window
     */
    public void initFileChooser(FileChooser fileChooser, String title, boolean text) {
        fileChooser.setTitle(title);
        if (text) {
            fileChooser.setInitialDirectory(new File(currentTextDirectory));
        }
        else {
            //CHANGE: current machine directory is now stored in the mediator
            fileChooser.setInitialDirectory(new File(mediator
                    .getCurrentMachineDirectory()));
        }
    }

    /**
     * extracts all the text from a file including new lines
     *
     * @param fileToOpen file to extract text from
     * @return the text contained in the file as a string
     */
    private String extractTextFromFile(File fileToOpen) {
        try {
            String content = "";
            FileReader freader = new FileReader(fileToOpen);
            BufferedReader breader = new BufferedReader(freader);
            while (true) {
                String line = breader.readLine();
                if (line == null) {
                    break;
                }
                content += line + NEWLINE;
            }

            freader.close();
            breader.close();

            return content;
        } catch (IOException ioe) {
            //TODO: something...
            System.out.println("IO fail");

        }
        return null;


    }

    /**
     * saves text to a file.  If unable to save, a dialog appears
     * indicating the problem.
     *
     * @param fileToSave file to be saved
     * @param text       text to be put in the file to be saved
     * @return true if the save was successful.
     */
    private boolean saveTextFile(File fileToSave, String text) {
        try {
            FileWriter fwriter = new FileWriter(fileToSave);
            BufferedWriter bwriter = new BufferedWriter(fwriter);

            fwriter.write(text);

            fwriter.close();
            bwriter.close();
            return true;
        } catch (IOException ioe) {
            CPUSimConstants.dialog.
                    owner(stage).
                    masthead("Error").
                    message("Unable to save the text to a file.").
                    showError();
            return false;
        }
    }

    /**
     * saves the contents of a tab to a file if the current contents of that
     * tab have not already been saved.
     *
     * @param tab tab whose contents needs to be saved
     * @return true if the tab was successfully saved to a file.
     */
    private boolean save(Tab tab) {
        CodePaneTab theTab = (CodePaneTab) tab;
        if (theTab.getFile() == null) {
            return saveAs(theTab);
        }

        InlineStyleTextArea textToSave = (InlineStyleTextArea) theTab.getContent();
        if (theTab.getDirty()) {
            boolean successfulSave = saveTextFile(theTab.getFile(), textToSave.getText
                    ());

            if (successfulSave) {
                theTab.setDirty(false);
                return true;
            }
            return false;
        }
        else {
            return true;
        }
    }

    /**
     * saves the content of a tab in a new file
     *
     * @param tab tab whose content needs to be saved
     * @return true if the tab was successfully saved to a file
     */
    private boolean saveAs(Tab tab) {
        FileChooser fileChooser = new FileChooser();
        initFileChooser(fileChooser, "Save Text", true);
                /*fileChooser.getExtensionFilters().add(new ExtensionFilter("Assembly
                Language File (.a)",
                        "*.a"));*/

        File fileToSave = fileChooser.showSaveDialog(stage);
        final File finalFileToSave;

        if (fileToSave == null) {
            finalFileToSave = null;
        }
                /*else if (fileToSave.getAbsolutePath().lastIndexOf(".a") != 
                        fileToSave.getAbsolutePath().length() - 2) {
                    finalFileToSave = new File(fileToSave.getAbsolutePath() + ".a");
                }*/
        else {
            finalFileToSave = new File(fileToSave.getAbsolutePath());
        }

        if (finalFileToSave != null) {

            InlineStyleTextArea textToSave = (InlineStyleTextArea) tab.getContent();

            saveTextFile(finalFileToSave, textToSave.getText());

            ((CodePaneTab) tab).setFile(finalFileToSave);
            tab.getTooltip().setText(finalFileToSave.getAbsolutePath());

            // Update Menu
            MenuItem copyPath = new MenuItem("Copy Path Name ");
            copyPath.setOnAction(e -> {
                if (finalFileToSave != null) {
                    Clipboard clipboard = Clipboard.getSystemClipboard();
                    ClipboardContent content = new ClipboardContent();
                    content.putString(finalFileToSave.getAbsolutePath());
                    clipboard.setContent(content);
                }
            });
            ObservableList<MenuItem> mi = tab.getContextMenu().getItems();
            mi.remove(3);
            mi.add(copyPath);
        }
        return finalFileToSave != null;
    }

    /**
     * opens a text file
     *
     * @param fileToOpen file to be opened
     */
    public void open(File fileToOpen) {
        currentTextDirectory = fileToOpen.getParent();
        String content = extractTextFromFile(fileToOpen);
        if (content == null) {
            CPUSimConstants.dialog.
                    owner(stage).
                    masthead("Nonexistant File").
                    message("There is no longer a file at the path " +
                            fileToOpen.getAbsolutePath()).
                    showError();
            if (reopenTextFiles.contains(fileToOpen.getAbsolutePath())) {
                reopenTextFiles.remove(fileToOpen.getAbsolutePath());
            }
            updateReopenTextMenu();
            return;
        }
        //update the reopen menu
        updateReopenTextFiles(fileToOpen);

        //if text is already open, just select the proper tab else open a new tab
        Optional<Tab> existingTab = textTabPane.getTabs().stream()
                .filter(t -> ((CodePaneTab) t).getFile() != null)
                .filter(t -> fileToOpen.getAbsolutePath().equals(
                        ((CodePaneTab) t).getFile().getAbsolutePath()))
                .findFirst();
        if (existingTab.isPresent()) {
            textTabPane.getSelectionModel().select(existingTab.get());
            currentTextDirectory = fileToOpen.getParent();
        }
        else {
            addTab(content, fileToOpen.getName(), fileToOpen);
        }
    }

    /**
     * puts the input file at the front of the reopenTextFiles queue and removes
     * the last entry if the queue has more than 10 elements
     *
     * @param fileToOpen input file
     */
    private void updateReopenTextFiles(File fileToOpen) {
        if (reopenTextFiles.contains(fileToOpen.getAbsolutePath())) {
            reopenTextFiles.remove(fileToOpen.getAbsolutePath());
        }
        reopenTextFiles.addFirst(fileToOpen.getAbsolutePath());
        if (reopenTextFiles.size() > 10) {
            reopenTextFiles.removeLast();
        }

        updateReopenTextMenu();
    }

    /**
     * Updates the reopenTextMenu so that it contains the proper sublist of
     * files as dictated by the reopenTextFiles queue
     */
    private void updateReopenTextMenu() {
        reopenTextMenu.getItems().clear();

        for (String filePath : reopenTextFiles) {
            //this is a workaround that may need to be changed...
            final File finalFile = new File(filePath);
            MenuItem menuItem = new MenuItem(filePath);
            menuItem.setOnAction(e -> open(finalFile));
            reopenTextMenu.getItems().add(menuItem);
        }
    }

    /**
     * puts the input file at the front of the reopenMachineFiles queue and removes
     * the last entry if the queue has more than 10 elements
     *
     * @param fileToOpen input file
     */
    public void updateReopenMachineFiles(File fileToOpen) {
        if (reopenMachineFiles.contains(fileToOpen.getAbsolutePath())) {
            reopenMachineFiles.remove(fileToOpen.getAbsolutePath());
        }
        reopenMachineFiles.addFirst(fileToOpen.getAbsolutePath());
        if (reopenMachineFiles.size() > 10) {
            reopenMachineFiles.removeLast();
        }
        updateReopenMachineMenu();
    }

    /**
     * Updates the reopenMachineMenu so that it contains the proper sublist of
     * files as dictated by the reopenMachineFiles queue
     */
    public void updateReopenMachineMenu() {
        reopenMachineMenu.getItems().clear();

        for (String filePath : reopenMachineFiles) {
            //this is a workaround that may need to be changed...
            final File finalFile = new File(filePath);
            MenuItem menuItem = new MenuItem(filePath);
            menuItem.setOnAction(e -> {
                if (mediator.isMachineDirty()) {
                    Action response = CPUSimConstants.dialog.
                            owner(stage).
                            masthead("Save Machine").
                            message("The machine you are currently "
                                    + "working on is unsaved.  Would "
                                    + "you like to save it before "
                                    + "you open a new machine?").
                            showConfirm();
                    if (response == Dialog.ACTION_YES) {
                        handleSaveMachine(e);
                    }
                    else if (response == Dialog.ACTION_CANCEL) {
                        return;
                    }
                }
                mediator.openMachine(finalFile);
            });
            reopenMachineMenu.getItems().add(menuItem);
        }
    }

    /**
     * Adds a tab with the default settings.
     */
    public void addDefaultTab() {
        addTab("", "Untitled", null);
    }

    /**
     * updates the Save and Load RAM menus so that the the contain a menu item
     * for each RAM in the current machine
     */
    private void updateRamMenus() {
        ObservableList<RAM> rams = (ObservableList<RAM>)
                mediator.getMachine().getModule("rams");
        saveRamMenu.getItems().clear();
        openRamMenu.getItems().clear();

        for (RAM ram : rams) {
            final RAM finalRam = ram;
            MenuItem saveMenuItem = new MenuItem("from " + ram.getName());
            saveMenuItem.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent e) {
                    saveRam(finalRam);
                }
            });
            saveRamMenu.getItems().add(saveMenuItem);

            MenuItem openMenuItem = new MenuItem("into " + ram.getName());
            openMenuItem.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent e) {
                    openRam(finalRam);
                }
            });
            openRamMenu.getItems().add(openMenuItem);
        }
    }

    /**
     * opens data from a mif or hex file chosen by the user into a certain RAM
     *
     * @param ram
     */
    private void openRam(RAM ram) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(new File(currentTextDirectory));

        fileChooser.setTitle("Open RAM");
        fileChooser.getExtensionFilters().add(
                new ExtensionFilter("Memory Text Files (.mif), (.hex)", "*.mif", "*" +
                        ".hex"));

        File fileToOpen = fileChooser.showOpenDialog(stage);

        if (fileToOpen.getName().lastIndexOf(".mif") == fileToOpen.getName().length() -
                4) {
            try {
                mediator.parseMIFFile(extractTextFromFile(fileToOpen), ram,
                        fileToOpen.getAbsolutePath());
            } catch (MIFReaderException e) {
                CPUSimConstants.dialog.
                        owner(stage).
                        masthead("MIF Parse Error").
                        message(e.getMessage()).
                        showError();
            }
        }

        else {
            mediator.parseIntelHexFile(extractTextFromFile(fileToOpen), ram,
                    fileToOpen.getAbsolutePath());
        }

        for (RamTableController rc : ramControllers) {
            rc.updateTable();
        }

    }

    /**
     * saves the contents of a particular ram to an mif or hex file (as dictated by the
     * user)
     *
     * @param ram
     */
    private void saveRam(RAM ram) {

        List<String> choices = new ArrayList<String>();

        choices.add("Machine Instruction File (.mif)");
        choices.add("Intel Hex Format (.hex)");


        // NOTE: This choicebox dialog does not have a default choice, which
        // is not ideal. The problem is that if there is a default choice,
        // and the user is fine with that default and just clicks okay without
        // choosing something else the fileFormat string that will be returned
        // will be null.  This would be fine except that we can tell that the
        // user canceled the dialog only by checking if the string is null
        Optional<String> fileFormat = CPUSimConstants.dialog.
                owner(stage).
                masthead("File Format Choice").
                message("In what file format should your ram information be saved?").
                showChoices(choices);

        if (!fileFormat.isPresent()) {
            return;
        }

        ExtensionFilter extensionFilter;
        boolean asMIF;
        if (fileFormat.equals("Machine Instruction File (.mif)")) {
            extensionFilter = new ExtensionFilter(
                    "Machine Instruction Files (.mif)", "*.mif");
            asMIF = true;
        }
        else {
            extensionFilter = new ExtensionFilter(
                    "Intel Hex Format (.hex)", "*.hex");
            asMIF = false;
        }


        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(new File(currentTextDirectory));

        fileChooser.setTitle("Save RAM");
        fileChooser.getExtensionFilters().add(extensionFilter);

        File fileToSave = fileChooser.showSaveDialog(stage);

        if (fileToSave == null) {
            return;
        }


        if (asMIF) {

            if (fileToSave.getAbsolutePath().lastIndexOf(".mif") !=
                    fileToSave.getAbsolutePath().length() - 4) {
                fileToSave = new File(fileToSave.getAbsolutePath() + ".mif");
            }

            try {
                FileWriter fwriter = new FileWriter(fileToSave);
                BufferedWriter bwriter = new BufferedWriter(fwriter);

                fwriter.write(mediator.ramToMIF(ram));

                fwriter.close();
                bwriter.close();
            } catch (IOException ioe) {
                CPUSimConstants.dialog.
                        owner(stage).
                        masthead("Error").
                        message("Unable to save the ram to a file.").
                        showError();
            }

        }
        else {
            if (fileToSave.getAbsolutePath().lastIndexOf(".hex") !=
                    fileToSave.getAbsolutePath().length() - 4) {
                fileToSave = new File(fileToSave.getAbsolutePath() + ".hex");
            }

            try {
                FileWriter fwriter = new FileWriter(fileToSave);
                BufferedWriter bwriter = new BufferedWriter(fwriter);

                fwriter.write(mediator.ramToIntelHex(ram));

                fwriter.close();
                bwriter.close();
            } catch (IOException ioe) {
                CPUSimConstants.dialog.
                        owner(stage).
                        masthead("Error").
                        message("Unable to save the ram to a file.").
                        showError();
            }
        }


    }

    /**
     * sets the value of the ram, register, and register array tables
     */
    public void setUpTables() {
        registerControllers.clear();
        ramControllers.clear();

        updateStyleOfTables();

        ObservableList<Register> registers =
                (ObservableList<Register>) mediator.getMachine().getModule("registers");

        RegisterTableController registerTableController =
                new RegisterTableController(this, registers, "Registers");
        FXMLLoader registerFxmlLoader = new FXMLLoader(
                mediator.getClass().getResource("gui/desktop/RegisterTable.fxml"));
        registerFxmlLoader.setController(registerTableController);
        registerControllers.add(registerTableController);


        Pane registerTableRoot = null;

        try {
            registerTableRoot = (Pane) registerFxmlLoader.load();
        } catch (IOException e) {
            //TODO: something more meaningful
            System.out.println("IOException: " + e.getMessage());
        }
        registerTableController.setDataBase(regDataBase);

        regVbox.setVgrow(regSplitPane, Priority.ALWAYS);

        regSplitPane.getItems().add(registerTableRoot);


        ObservableList<RegisterArray> registerArrays = (ObservableList<RegisterArray>)
                mediator.getMachine().getModule("registerArrays");

        Pane registerArrayTableRoot = null;

        if (!registerArrays.isEmpty()) {
            for (int i = 0; i < registerArrays.size(); i++) {
                FXMLLoader registerArrayFxmlLoader = new FXMLLoader(
                        mediator.getClass().getResource("gui/desktop/RegisterTable" +
                                ".fxml"));

                RegisterTableController registerArrayTableController = new
                        RegisterTableController(
                        this,
                        registerArrays.get(i).registers(),
                        registerArrays.get(i).getName());
                registerArrayFxmlLoader.setController(registerArrayTableController);

                registerControllers.add(registerArrayTableController);


                try {
                    registerArrayTableRoot = (Pane) registerArrayFxmlLoader.load();
                } catch (IOException e) {
                    //TODO: something...
                }
                registerArrayTableController.setDataBase(regDataBase);

                regSplitPane.getItems().add(registerArrayTableRoot);
            }
        }

        double numRegSplitPanes = regSplitPane.getItems().size();
        double regdpos = 0;
        for (int i = 0; i < numRegSplitPanes - 1; i++) {
            regdpos += (1.0 / numRegSplitPanes);
            regSplitPane.setDividerPosition(i, regdpos);
        }

        ObservableList<RAM> rams =
                (ObservableList<RAM>) mediator.getMachine().getModule("rams");

        if (!rams.isEmpty()) {
            ramVbox.getChildren().remove(noRAMLabel);
            ramToolBar.setDisable(false);

            Pane ramTableRoot = null;
            RamTableController ramTableController;

            for (int i = 0; i < rams.size(); i++) {
                FXMLLoader ramFxmlLoader = new FXMLLoader(
                        mediator.getClass().getResource("gui/desktop/RamTable.fxml"));
                ramTableController = new RamTableController(
                        this,
                        rams.get(i),
                        rams.get(i).getName());
                ramFxmlLoader.setController(ramTableController);

                ramControllers.add(ramTableController);


                try {
                    ramTableRoot = (Pane) ramFxmlLoader.load();
                } catch (IOException e) {
                    //TODO: something...
                }

                ramTableController.setDataBase(ramDataBase);
                ramTableController.setAddressBase(ramAddressBase);

                ramVbox.setVgrow(ramSplitPane, Priority.ALWAYS);
                ramSplitPane.getItems().add(ramTableRoot);

            }

            for (int i = 0; i < ramSplitPane.getDividers().size(); i++) {
                ramSplitPane.setDividerPosition(i,
                        1.0 / (ramSplitPane.getDividers().size() + 1) * (i + 1));
            }

            updateRamMenus();
            ramVbox.getChildren().addAll();
        }
        else {
            if (!ramVbox.getChildren().contains(noRAMLabel)) {
                ramVbox.getChildren().add(1, noRAMLabel);
            }
            ramToolBar.setDisable(true);
        }

        double numRamSplitPanes = ramSplitPane.getItems().size();
        double ramdpos = 0;
        for (int i = 0; i < numRamSplitPanes - 1; i++) {
            ramdpos += (1.0 / numRamSplitPanes);
            ramSplitPane.setDividerPosition(i, ramdpos);
        }


    }

    /**
     * gets rid of all register and ram tables
     */
    //CHANGE: made public for access by mediator
    public void clearTables() {
        ramSplitPane.getItems().clear();
        regSplitPane.getItems().clear();
    }

    public void adjustTablesForNewModules() {
        if (regSplitPane.getItems().size() > 1) {
            regSplitPane.getItems().remove(1, regSplitPane.getItems().size());
        }
        registerControllers.remove(1, registerControllers.size());

        ObservableList<RegisterArray> registerArrays = (ObservableList<RegisterArray>)
                mediator.getMachine().getModule("registerArrays");

        Pane registerArrayTableRoot = null;

        if (!registerArrays.isEmpty()) {
            for (int i = 0; i < registerArrays.size(); i++) {
                FXMLLoader registerArrayFxmlLoader = new FXMLLoader(
                        mediator.getClass().getResource("gui/desktop/RegisterTable" +
                                ".fxml"));

                RegisterTableController registerArrayTableController = new
                        RegisterTableController(
                        this,
                        registerArrays.get(i).registers(),
                        registerArrays.get(i).getName());
                registerArrayFxmlLoader.setController(registerArrayTableController);

                registerControllers.add(registerArrayTableController);


                try {
                    registerArrayTableRoot = (Pane) registerArrayFxmlLoader.load();
                } catch (IOException e) {
                    //TODO: something...
                }
                registerArrayTableController.setDataBase(regDataBase);

                regSplitPane.getItems().add(registerArrayTableRoot);
            }
        }

        double numRegSplitPanes = regSplitPane.getItems().size();
        double regdpos = 0;
        for (int i = 0; i < numRegSplitPanes - 1; i++) {
            regdpos += (1.0 / numRegSplitPanes);
            regSplitPane.setDividerPosition(i, regdpos);
        }

        ramSplitPane.getItems().clear();
        ObservableList<RAM> rams =
                (ObservableList<RAM>) mediator.getMachine().getModule("rams");
        ramControllers.clear();

        if (!rams.isEmpty()) {
            ramVbox.getChildren().remove(noRAMLabel);
            ramToolBar.setDisable(false);

            Pane ramTableRoot = null;
            RamTableController ramTableController;

            for (int i = 0; i < rams.size(); i++) {
                FXMLLoader ramFxmlLoader = new FXMLLoader(
                        mediator.getClass().getResource("gui/desktop/RamTable.fxml"));
                ramTableController = new RamTableController(
                        this,
                        rams.get(i),
                        rams.get(i).getName());
                ramFxmlLoader.setController(ramTableController);

                ramControllers.add(ramTableController);


                try {
                    ramTableRoot = (Pane) ramFxmlLoader.load();
                } catch (IOException e) {
                    //TODO: something...
                }

                ramTableController.setDataBase(ramDataBase);
                ramTableController.setAddressBase(ramAddressBase);

                ramVbox.setVgrow(ramSplitPane, Priority.ALWAYS);
                ramSplitPane.getItems().add(ramTableRoot);
            }

            updateRamMenus();
            ramVbox.getChildren().addAll();
        }
        else {
            if (!ramVbox.getChildren().contains(noRAMLabel)) {
                ramVbox.getChildren().add(0, noRAMLabel);
            }
            ramToolBar.setDisable(true);

        }

        double numRamSplitPanes = ramSplitPane.getItems().size();
        double ramdpos = 0;
        for (int i = 0; i < numRamSplitPanes - 1; i++) {
            ramdpos += (1.0 / numRamSplitPanes);
            ramSplitPane.setDividerPosition(i, ramdpos);
        }

        for (RamTableController rtc : ramControllers) {
            rtc.updateTable();
        }
        for (RegisterTableController rtc : registerControllers) {
            rtc.updateTable();
        }

    }

    /**
     * Looks for all unsaved work and asks the user if he would like to save any of
     * the work before closing
     *
     * @return whether or not the window should be closed
     */
    private boolean confirmClosing() {
        if (inRunningMode.get()) {
            Action response = CPUSimConstants.dialog.
                    owner(stage).
                    masthead("Running Program").
                    message("There is a program running. " +
                            "Closing the application will also quit the program. " +
                            "Do you want to quit the running program?").
                    showConfirm();
            if (response == Dialog.ACTION_CANCEL ||
                    response == Dialog.ACTION_NO) {
                return false;
            }
        }
        //CHANGE: mediator is asked is the machine is dirty
        if (mediator.isMachineDirty()) {
            Action response = CPUSimConstants.dialog.
                    owner(stage).
                    masthead("Save Machine").
                    message("The machine you are currently working on "
                            + "is unsaved.  Would you like to save it"
                            + " before you close?").
                    showConfirm();
            if (response == Dialog.ACTION_YES) {
                //CHANGE: mediator is told to save current machine
                mediator.saveMachine();
            }
            else if (response == Dialog.ACTION_CANCEL) {
                return false;
            }
        }
        for (Tab tab : textTabPane.getTabs()) {
            if (((CodePaneTab) tab).getDirty()) {
                Action response = CPUSimConstants.dialog.
                        owner(stage).
                        masthead("Save Text").
                        message("Would you like to save your work before you "
                                + "close " + tab.getText().substring(1) + "?").
                        showConfirm();
                if (response == Dialog.ACTION_YES) {
                    save(tab);
                }
                else if (response == Dialog.ACTION_CANCEL) {
                    return false;
                }
            }
        }
        return true;
    }


    /**
     * stores certain preferences and other things that reflect a specific user's
     * experience with cpusim
     */
    public void storePreferences() {
        Preferences prefs = Preferences.userNodeForPackage(getClass());

        //save current text and machine directories
        //CHANGE: current machine directory is not stored in mediator
        prefs.put("machineDirectory", mediator.getCurrentMachineDirectory());
        prefs.put("textDirectory", currentTextDirectory);

        //save recently opened text files
        int i = 0;
        for (String filePath : reopenTextFiles) {
            prefs.put("reopenTextFile" + i, filePath);
            i++;
        }
        prefs.putInt("numTextFiles", reopenTextFiles.size());

        i = 0;
        for (String filePath : reopenMachineFiles) {
            prefs.put("reopenMachineFile" + i, filePath);
            i++;
        }
        prefs.putInt("numMachineFiles", reopenMachineFiles.size());

        prefs.put("regDataBase", regDataBase);
        prefs.put("ramAddressBase", ramAddressBase);
        prefs.put("ramDataBase", ramDataBase);

        prefs.put("textFontSize", textFontData.fontSize);
        prefs.put("textFont", textFontData.font);
        prefs.putBoolean("textBold", textFontData.bold);
        prefs.putBoolean("textItalic", textFontData.italic);
        prefs.put("textForground", textFontData.foreground);
        prefs.put("textBackground", textFontData.background);

        prefs.put("tableFontSize", tableFontData.fontSize);
        prefs.put("tableFont", tableFontData.font);
        prefs.putBoolean("tableBold", tableFontData.bold);
        prefs.putBoolean("tableItalic", tableFontData.italic);
        prefs.put("tableForground", tableFontData.foreground);
        prefs.put("tableBackground", tableFontData.background);
        prefs.put("tableBorder", tableFontData.border);

        i = 0;
        for (String binding : keyBindings) {
            prefs.put("keyBinding" + i, binding);
            i++;
        }

        prefs.putBoolean("autoSave", otherSettings.autoSave);
        prefs.putBoolean("showLineNumbers", otherSettings.showLineNumbers.get());
        prefs.putBoolean("clearConsoleOnRun", otherSettings.clearConsoleOnRun);
    }

    /**
     * Loads preferences
     */
    public void loadPreferences() {
        Preferences prefs = Preferences.userNodeForPackage(getClass());
        //CHANGE: machine Directory is now contained in mediator
        mediator.setCurrentMachineDirectory(System.getProperty("user.dir"));
        currentTextDirectory = System.getProperty("user.dir");
        // the next two lines sometimes cause problems (exceptions to be thrown)
        //        currentMachineDirectory = prefs.get("machineDirectory", System
        // .getProperty("user.dir"));
        //        currentTextDirectory = prefs.get("textDirectory", System.getProperty
        // ("user.dir"));


        int numTextFiles = prefs.getInt("numTextFiles", 0);
        for (int i = 0; i < numTextFiles; i++) {
            reopenTextFiles.offer(prefs.get("reopenTextFile" + i, ""));
        }

        updateReopenTextMenu();

        int numMachineFiles = prefs.getInt("numMachineFiles", 0);
        for (int i = 0; i < numMachineFiles; i++) {
            reopenMachineFiles.offer(prefs.get("reopenMachineFile" + i, ""));
        }

        updateReopenMachineMenu();

        regDataBase = prefs.get("regDataBase", "Dec");
        ramAddressBase = prefs.get("ramAddressBase", "Dec");
        ramDataBase = prefs.get("ramDataBase", "Decimal");

        textFontData.fontSize = prefs.get("textFontSize", "12");
        textFontData.font = prefs.get("textFont", "\"Courier New\"");
        textFontData.bold = prefs.getBoolean("textBold", false);
        textFontData.italic = prefs.getBoolean("textItalic", false);
        textFontData.foreground = prefs.get("textForground", "#000000");
        textFontData.background = prefs.get("textBackground", "#FFFFFF");

        tableFontData.fontSize = prefs.get("tableFontSize", "12");
        tableFontData.font = prefs.get("tableFont", "\"Courier New\"");
        tableFontData.bold = prefs.getBoolean("tableBold", false);
        tableFontData.italic = prefs.getBoolean("tableItalic", false);
        tableFontData.foreground = prefs.get("tableForground", "#000000");
        tableFontData.background = prefs.get("tableBackground", "WHITE");
        tableFontData.border = prefs.get("tableBorder", "#D3D3D3");

        for (int i = 0; i < DEFAULT_KEY_BINDINGS.length; i++) {
            String keyString = prefs.get("keyBinding" + i, DEFAULT_KEY_BINDINGS[i]);
            keyBindings.add(keyString);
        }

        otherSettings.autoSave = prefs.getBoolean("autoSave", false);
        otherSettings.showLineNumbers.set(prefs.getBoolean("showLineNumbers", true));
        otherSettings.clearConsoleOnRun = prefs.getBoolean("clearConsoleOnRun", true);
    }

    /**
     * sets the style of the text in the text area;
     * TODO:  Fix updateStyleOfTabs for CodeAreas
     */
    public void updateStyleOfTabs() {
//        String boldString = textFontData.bold ? "bold" : "normal";
//        String italicString = textFontData.italic ? "italic" : "normal";
//        for (Tab tab : tabFiles.keySet()) {
//            EditorPaneController controller = tabEditorControllers.get(tab);
//            TextArea ta = controller.getTextArea();
//            ta.setStyle("-fx-font-size:" + textFontData.fontSize + "; "
//                    + "-fx-font-family:" + textFontData.font + "; -fx-font-style:" +
//                    italicString + "; "
//                    + "-fx-font-weight:" + boldString + "; -fx-background-color:" +
//                    textFontData.background +
//                    "; -fx-text-fill:" + textFontData.foreground + ";");
//            controller.setRowHeights(Integer.parseInt(textFontData.fontSize));
//        }
    }

    /**
     * sets the style of the text in the tables area;
     */
    public void updateStyleOfTables() {
        String boldString = textFontData.bold ? "bold" : "normal";
        String italicString = textFontData.italic ? "italic" : "normal";
        tableStyle.set("-fx-font-size:" + tableFontData.fontSize + "; "
                + "-fx-font-family:" + tableFontData.font + "; -fx-font-style:" +
                italicString + "; "
                + "-fx-font-weight:" + boldString +
                "; -fx-text-fill:" + tableFontData.foreground +
                "; -fx-border-color:" + tableFontData.border + ";");

        if (mainPane.getStyleClass().size() > 1) {
            mainPane.getStyleClass().remove(1);
        }

        if (!backgroundSetting.keySet().contains(tableFontData.background)) {
            tableFontData.background = "WHITE";
        }

        mainPane.getStylesheets().add(backgroundSetting.get(tableFontData.background));

        for (RegisterTableController rtc : registerControllers) {
            rtc.setColor(tableStyle.get());
        }
        for (RamTableController rtc : ramControllers) {
            rtc.setColor(tableStyle.get());
        }
    }

    /**
     * returns the table style string
     *
     * @return table style string
     */
    public SimpleStringProperty getTableStyle() {
        return tableStyle;
    }

    /**
     * Returns the text font data object
     *
     * @return the text font data object
     */
    public FontData getTextFontData() {
        return textFontData;
    }

    /**
     * Returns the table font data object
     *
     * @return the table font data object
     */
    public FontData getTableFontData() {
        return tableFontData;
    }

    /**
     * Returns the other settings of preference
     *
     * @return the other settings of preference
     */
    public OtherSettings getOtherSettings() {
        return otherSettings;
    }

    /**
     * Binds the proper menu items to the correct key combinations based on the strings
     * in the keyBindings data structure.
     */
    private void createKeyCodes() {

        KeyCode key = null;

        int i = 0;
        for (String keyBinding : keyBindings) {
            KeyCodeCombination.ModifierValue shift = KeyCodeCombination.ModifierValue.UP;
            KeyCodeCombination.ModifierValue ctrl = KeyCodeCombination.ModifierValue.UP;
            KeyCodeCombination.ModifierValue alt = KeyCodeCombination.ModifierValue.UP;
            KeyCodeCombination.ModifierValue meta = KeyCodeCombination.ModifierValue.UP;
            KeyCodeCombination.ModifierValue shortcut = KeyCodeCombination
                    .ModifierValue.UP;
            String[] keys = keyBinding.split("-");
            key = KeyCode.getKeyCode(keys[keys.length - 1]);
            if (key == null) {
                key = Convert.charToKeyCode(keys[keys.length - 1]);
            }
            keys[keys.length - 1] = null;
            if (keys.length > 1) {
                for (String mod : keys) {
                    if (mod != null) {
                        switch (mod) {
                            case "Shift":
                                shift = KeyCodeCombination.ModifierValue.DOWN;
                                break;
                            case "Ctrl":
                                if (!System.getProperty("os.name").startsWith
                                        ("Windows")) {
                                    ctrl = KeyCodeCombination.ModifierValue.DOWN;
                                }
                                else {
                                    shortcut = KeyCodeCombination.ModifierValue.DOWN;
                                }
                                break;
                            case "Alt":
                                alt = KeyCodeCombination.ModifierValue.DOWN;
                                break;
                            case "Meta":
                                meta = KeyCodeCombination.ModifierValue.DOWN;
                                break;
                            case "Cmd":
                                shortcut = KeyCodeCombination.ModifierValue.DOWN;
                                break;
                            default:
                                break;
                        }
                    }
                }
            }
            keyCodeCombinations.get(i).set(null);
            if (key != null) {
                keyCodeCombinations.get(i).set(new KeyCodeCombination(key, shift, ctrl,
                        alt,
                        meta, shortcut));
            }
            i++;
        }

    }

    /**
     * Uses the key code combinations in the keyCodeCombinations list to bind the proper
     * menu items to the proper key combinations.
     */
    private void bindKeys() {

        //List of menu items to give keycodes
        ArrayList<MenuItem> menuItems = new ArrayList<>();

        //put appropriate menu items from the filemenu into the array
        for (MenuItem menuItem : fileMenu.getItems()) {
            if (menuItem.getText() == null) {
                continue;
            }
            if (menuItem.getText().equals("Reopen text") ||
                    menuItem.getText().equals("Reopen machine") ||
                    menuItem.getText().equals("Open RAM...") ||
                    menuItem.getText().equals("Save RAM...") ||
                    menuItem.getText().equals("Quit")) {
                continue;
            }
            menuItems.add(menuItem);
        }

        //put appropriate menu items from the filemenu into the array and
        //give appropriate menu items their default (final) value
        for (MenuItem menuItem : editMenu.getItems()) {
            if (menuItem.getText() == null) {
                continue;
            }
            // Delete: DELETE
            if (menuItem.getText().equals("Delete")) {
                menuItem.setAccelerator(new KeyCodeCombination(KeyCode.DELETE,
                        KeyCodeCombination.
                                ModifierValue.UP, KeyCodeCombination.ModifierValue.UP,
                        KeyCodeCombination.
                                ModifierValue.UP, KeyCodeCombination.ModifierValue.UP,
                        KeyCodeCombination.
                                ModifierValue.UP));
                continue;
            }
            // Undo: SHORTCUT-Z
            if (menuItem.getText().equals("Undo")) {
                menuItem.setAccelerator(new KeyCodeCombination(KeyCode.Z,
                        KeyCodeCombination.
                                ModifierValue.UP, KeyCodeCombination.ModifierValue.UP,
                        KeyCodeCombination.
                                ModifierValue.UP, KeyCodeCombination.ModifierValue.UP,
                        KeyCodeCombination.
                                ModifierValue.DOWN));
                continue;
            }
            // Redo: SHORTCUT-Shift-Z
            if (menuItem.getText().equals("Redo")) {
                menuItem.setAccelerator(new KeyCodeCombination(KeyCode.Z,
                        KeyCodeCombination.
                                ModifierValue.DOWN, KeyCodeCombination.ModifierValue.UP,
                        KeyCodeCombination.
                                ModifierValue.UP, KeyCodeCombination.ModifierValue.UP,
                        KeyCodeCombination.
                                ModifierValue.DOWN));
                continue;
            }
            // Cut: SHORTCUT-X
            if (menuItem.getText().equals("Cut")) {
                menuItem.setAccelerator(new KeyCodeCombination(KeyCode.X,
                        KeyCodeCombination.
                                ModifierValue.UP, KeyCodeCombination.ModifierValue.UP,
                        KeyCodeCombination.
                                ModifierValue.UP, KeyCodeCombination.ModifierValue.UP,
                        KeyCodeCombination.
                                ModifierValue.DOWN));
                continue;
            }
            // Copy: SHORTCUT-C
            if (menuItem.getText().equals("Copy")) {
                menuItem.setAccelerator(new KeyCodeCombination(KeyCode.C,
                        KeyCodeCombination.
                                ModifierValue.UP, KeyCodeCombination.ModifierValue.UP,
                        KeyCodeCombination.
                                ModifierValue.UP, KeyCodeCombination.ModifierValue.UP,
                        KeyCodeCombination.
                                ModifierValue.DOWN));
                continue;
            }
            // Paste: SHORTCUT-Y
            if (menuItem.getText().equals("Paste")) {
                menuItem.setAccelerator(new KeyCodeCombination(KeyCode.V,
                        KeyCodeCombination.
                                ModifierValue.UP, KeyCodeCombination.ModifierValue.UP,
                        KeyCodeCombination.
                                ModifierValue.UP, KeyCodeCombination.ModifierValue.UP,
                        KeyCodeCombination.
                                ModifierValue.DOWN));
                continue;
            }
            if (menuItem.getText().equals("Select All")) {
                menuItem.setAccelerator(new KeyCodeCombination(KeyCode.A,
                        KeyCodeCombination.
                                ModifierValue.UP, KeyCodeCombination.ModifierValue.UP,
                        KeyCodeCombination.
                                ModifierValue.UP, KeyCodeCombination.ModifierValue.UP,
                        KeyCodeCombination.
                                ModifierValue.DOWN));
                continue;
            }
            menuItems.add(menuItem);
        }

        //get appropriate menu items (all fo them) from the rest of the menu items
        for (MenuItem menuItem : modifyMenu.getItems()) {
            if (menuItem.getText() == null) {
                continue;
            }
            menuItems.add(menuItem);
        }

        for (MenuItem menuItem : executeMenu.getItems()) {
            if (menuItem.getText() == null) {
                continue;
            }
            menuItems.add(menuItem);
        }

        for (MenuItem menuItem : helpMenu.getItems()) {
            menuItems.add(menuItem);
        }

        //bind the key proper keycode to the every menu item
        int i = 0;
        for (MenuItem menuItem : menuItems) {
            menuItem.acceleratorProperty().bind(keyCodeCombinations.get(i));
            i++;
        }
    }


    /**
     * returns the current key bindings for the menu items
     *
     * @returns the current key bindings for the menu items
     */
    public ObservableList<String> getKeyBindings() {
        return keyBindings;
    }

    /**
     * Sets the key bindings of the menu items and binds those keys to the menu items
     * (this doesn't seem to work unless it is being done upon loading the program)
     *
     * @param keyBindings The new key bindings for the menu items
     */
    public void setKeyBindings(ObservableList<String> keyBindings) {
        this.keyBindings = keyBindings;
        createKeyCodes();
    }

    /**
     * shows an info dialog telling the user that the print features have yet to
     * be implemented
     */
    private void tempPrintDialog() {
        CPUSimConstants.dialog.
                owner(stage).
                masthead("Not Implemented").
                message("Not yet implemented (waiting for JavaFX 8)").
                showInformation();
    }

    /**
     * highlights the token in the text pane for the file containing the token.
     * If there is no tab for that file, then a new tab is created.
     *
     * @param token the token to be highlighted
     */
    public void highlightToken(Token token) {
        File file = new File(token.filename);
        if (!file.canRead()) {
            CPUSimConstants.dialog.
                    owner(stage).
                    masthead("IO Error").
                    message("CPU Sim could not find the file to open and "
                            + "highlight: " + file.getAbsolutePath()).
                    showInformation();
            return;
        }
        InlineStyleTextArea textArea = (InlineStyleTextArea) getTabForFile(file).getContent();
        textArea.selectRange(token.offset, token.offset + token.contents.length());
    }

    public void printlnToConsole(String s) {
        consoleManager.printlnToConsole(s);
    }

    /**
     * Just a class to hold all the data for the font
     * so that it can be passed around different object
     * and retain proper modifications
     */
    public class FontData {
        public String font;
        public String fontSize;
        public boolean bold;
        public boolean italic;
        public String foreground;
        public String background;
        public String border;
    }

    /**
     * A class to hold all other preference settings
     */
    public class OtherSettings {
        public boolean autoSave;
        public SimpleBooleanProperty showLineNumbers;
        public boolean clearConsoleOnRun;

        public OtherSettings() {
            showLineNumbers = new SimpleBooleanProperty(true);
            // add a listener that changes the line numbers for the selected tab
            // The line numbers for other tabs are not changed until they are selected.
            showLineNumbers.addListener(new ChangeListener<Boolean>() {
                @Override
                public void changed(ObservableValue<? extends Boolean> arg0,
                                    Boolean oldVal, Boolean newVal) {
                    Tab t = textTabPane.getSelectionModel().getSelectedItem();
                    if (t == null) {
                        return;
                    }
                    StyledTextArea codeArea = (StyledTextArea) t.getContent();
                    LineNumAndBreakpointFactory lFactory =
                            (LineNumAndBreakpointFactory) codeArea.getParagraphGraphicFactory();

                    if (newVal) { // show line numbers
                        lFactory.setFormat(digits -> "%" + digits + "d");
                    }
                    else { // hide line numbers
                        lFactory.setFormat(digits -> "");
                    }
                }
            });
        }
    }

}
