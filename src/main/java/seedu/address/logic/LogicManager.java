package seedu.address.logic;

import java.nio.file.Path;
import java.util.logging.Logger;

import javafx.collections.ObservableList;
import seedu.address.commons.core.GuiSettings;
import seedu.address.commons.core.LogsCenter;
import seedu.address.commons.exceptions.CommandHistoryException;
import seedu.address.commons.exceptions.DataLoadingException;
import seedu.address.logic.commands.Command;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.util.AddressBookParser;
import seedu.address.logic.util.exceptions.ParseException;
import seedu.address.model.Model;
import seedu.address.model.person.Person;
import seedu.address.storage.Storage;

/**
 * The main LogicManager of the app.
 */
public class LogicManager implements Logic {

    private final Logger logger = LogsCenter.getLogger(LogicManager.class);

    private final Model model;
    private final Storage storage;

    private final CommandHistory commandHistory = new CommandHistory();

    /**
     * Constructs a {@code LogicManager} with the given {@code Model} and {@code Storage}.
     */
    public LogicManager(Model model, Storage storage) {
        this.model = model;
        this.storage = storage;
    }

    @Override
    public String execute(String commandText) throws CommandException, ParseException, DataLoadingException {
        logger.info("----------------[USER COMMAND][" + commandText + "]");

        Command command = AddressBookParser.parseCommand(commandText);
        String commandResult = command.execute(model);
        storage.saveAddressBook(model.getAddressBook());

        // keep track of valid commands
        commandHistory.add(commandText);
        logger.info("\"" + commandText + "\" added to commandHistory");

        return commandResult;
    }

    @Override
    public String getPreviousCommandText() throws CommandHistoryException {
        return commandHistory.getPrevious();
    }

    @Override
    public String getNextCommandText() throws CommandHistoryException {
        return commandHistory.getNext();
    }

    @Override
    public ObservableList<Person> getFilteredPersonList() {
        return model.getFilteredPersonList();
    }

    @Override
    public Path getAddressBookFilePath() {
        return model.getAddressBookFilePath();
    }

    @Override
    public GuiSettings getGuiSettings() {
        return model.getGuiSettings();
    }

    @Override
    public void setGuiSettings(GuiSettings guiSettings) {
        model.setGuiSettings(guiSettings);
    }

}
