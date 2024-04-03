package seedu.address.ui;

import java.util.logging.Logger;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputControl;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import seedu.address.commons.core.GuiSettings;
import seedu.address.commons.core.LogsCenter;
import seedu.address.commons.exceptions.CommandHistoryException;
import seedu.address.logic.Logic;
import seedu.address.logic.Messages;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.util.exceptions.ParseException;
import seedu.address.storage.exceptions.StorageException;

/**
 * The Main Window. Provides the basic application layout containing
 * a menu bar and space where other JavaFX elements can be placed.
 */
public class MainWindow extends UiPart<Stage> {

    private static final String FXML = "MainWindow.fxml";

    private final Logger logger = LogsCenter.getLogger(getClass());

    private final Stage primaryStage;
    private final Logic logic;

    // Independent Ui parts residing in this Ui container
    private final PersonListPanel personListPanel;
    private final ResultDisplay resultDisplay = new ResultDisplay();
    private final HelpWindow helpWindow = new HelpWindow();
    private final String iconPath = "/images/icon.png";

    @FXML
    private StackPane commandBoxPlaceholder;

    @FXML
    private MenuItem helpMenuItem;

    @FXML
    private StackPane personListPanelPlaceholder;

    @FXML
    private StackPane resultDisplayPlaceholder;

    @FXML
    private StackPane statusbarPlaceholder;

    @FXML
    private ImageView iconImageView;

    /**
     * Creates a {@code MainWindow} with the given {@code Stage} and {@code Logic}.
     */
    public MainWindow(Stage primaryStage, Logic logic) {
        super(FXML, primaryStage);

        // Set dependencies
        this.primaryStage = primaryStage;
        this.logic = logic;
        this.personListPanel = new PersonListPanel(logic.getFilteredPersonList());

        // Configure the UI
        setWindowDefaultSize(logic.getGuiSettings());

        setAccelerators();

        primaryStage.show(); // This should be called before creating other UI parts
        primaryStage.requestFocus();

        fillInnerParts();
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    private void setAccelerators() {
        setAccelerator(helpMenuItem, KeyCombination.valueOf("F1"));
    }

    /**
     * Sets the accelerator of a MenuItem.
     * @param keyCombination the KeyCombination value of the accelerator
     */
    private void setAccelerator(MenuItem menuItem, KeyCombination keyCombination) {
        menuItem.setAccelerator(keyCombination);

        /*
         * TODO: the code below can be removed once the bug reported here
         * https://bugs.openjdk.java.net/browse/JDK-8131666
         * is fixed in later version of SDK.
         *
         * According to the bug report, TextInputControl (TextField, TextArea) will
         * consume function-key events. Because CommandBox contains a TextField, and
         * ResultDisplay contains a TextArea, thus some accelerators (e.g F1) will
         * not work when the focus is in them because the key event is consumed by
         * the TextInputControl(s).
         *
         * For now, we add following event filter to capture such key events and open
         * help window purposely so to support accelerators even when focus is
         * in CommandBox or ResultDisplay.
         */
        getRoot().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getTarget() instanceof TextInputControl && keyCombination.match(event)) {
                menuItem.getOnAction().handle(new ActionEvent());
                event.consume();
            }
        });
    }

    /**
     * Fills up all the placeholders of this window.
     */
    private void fillInnerParts() {
        personListPanelPlaceholder.getChildren().add(personListPanel.getRoot());

        resultDisplayPlaceholder.getChildren().add(resultDisplay.getRoot());

        StatusBarFooter statusBarFooter = new StatusBarFooter(logic.getAddressBookFilePath());
        statusbarPlaceholder.getChildren().add(statusBarFooter.getRoot());

        CommandBox commandBox = new CommandBox(new RecordedCommandExecutor());
        commandBoxPlaceholder.getChildren().add(commandBox.getRoot());

        Image iconImage = new Image(iconPath);
        iconImageView.setImage(iconImage);
    }

    /**
     * Sets the default size based on {@code guiSettings}.
     */
    private void setWindowDefaultSize(GuiSettings guiSettings) {
        primaryStage.setHeight(guiSettings.getWindowHeight());
        primaryStage.setWidth(guiSettings.getWindowWidth());

        if (guiSettings.getIsMaximized()) {
            primaryStage.setMaximized(true);
        }

        if (guiSettings.getWindowCoordinates() != null) {
            primaryStage.setX(guiSettings.getWindowCoordinates().getX());
            primaryStage.setY(guiSettings.getWindowCoordinates().getY());
        }
    }

    /**
     * Opens the help window or focuses on it if it's already opened.
     */
    @FXML
    public void handleHelp() {
        if (!helpWindow.isShowing()) {
            helpWindow.show();
        } else {
            helpWindow.focus();
        }
    }

    /**
     * Closes the application.
     */
    @FXML
    private void handleExit() {
        GuiSettings guiSettings = new GuiSettings(primaryStage.getWidth(), primaryStage.getHeight(),
                (int) primaryStage.getX(), (int) primaryStage.getY(), primaryStage.isMaximized());
        logic.setGuiSettings(guiSettings);
        helpWindow.hide();
        primaryStage.hide();
    }

    /**
     * Copies string to the clipboard
     */
    @FXML
    private void handleCopy(String detailsToCopy) {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(detailsToCopy);
        clipboard.setContent(content);
    }

    public void showMessage(String msg) {
        resultDisplay.setFeedbackToUser(msg);
    }

    private class RecordedCommandExecutor implements CommandExecutor {

        @Override
        public String execute(String commandText) throws CommandException, ParseException, StorageException {
            String commandResult;
            try {
                commandResult = logic.execute(commandText);
                logger.info("Result: " + commandResult);
                showMessage(commandResult);
            } catch (CommandException | ParseException | StorageException e) {
                logger.info("An error occurred while executing command: " + commandText);
                showMessage(e.getMessage());
                throw e;
            }

            // == used to prevent an edge case where a command may somehow return this exact string,
            // but is not actually a help or exit command.
            if (commandResult == Messages.MESSAGE_SHOWING_HELP) {
                handleHelp();
            }
            if (commandResult == Messages.MESSAGE_EXITING) {
                handleExit();
            }
            if (commandResult.startsWith(Messages.MESSAGE_COPIED.substring(0, Messages.MESSAGE_COPIED_LEN + 1))) {
                handleCopy(commandResult.substring(Messages.MESSAGE_COPIED_LEN).trim());
            }

            return commandResult;
        }

        @Override
        public String getPreviousCommandText() throws CommandHistoryException {
            return logic.getPreviousCommandText();
        }

        @Override
        public String getNextCommandText() throws CommandHistoryException {
            return logic.getNextCommandText();
        }

    }

}
