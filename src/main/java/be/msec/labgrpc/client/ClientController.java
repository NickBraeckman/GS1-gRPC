package be.msec.labgrpc.client;

import be.msec.labgrpc.UserNotFoundException;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.effect.Lighting;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.io.IOException;

public class ClientController {
    private ChatClient client;
    /* ----------------------------- @FXML ----------------------------- */
    @FXML
    private TextField userField;
    @FXML
    private TextField serverField;
    @FXML
    private TextField portField;
    @FXML
    private TextField msgField;
    @FXML
    private ListView chatPane;
    @FXML
    private ListView userPane;
    /* ----------------------------- ERROR MESSAGES ----------------------------- */
    static final String ERROR_EMPTY_MESSAGE = "Cannot send empty message !";
    static final String ERROR_EMPTY_USER = "Required !";
    static final String ERROR_EMPTY_SERVER = "Required ! !";
    static final String ERROR_EMPTY_PORT = "Required ! ! !";
    static final String ERROR_FORMAT_PORT = "Only numbers allowed !";

    /* ----------------------------- METHODS ----------------------------- */
    public void connectButtonAction() throws IOException {
        String userNameString = this.userField.getText();
        String serverString = serverField.getText();
        String portString = portField.getText();
        boolean correct = true;

        if (userNameString.isEmpty()) {
            this.userField.setText(ERROR_EMPTY_USER);
            flashTextField(this.userField);
            correct = false;
        }

        if (serverString.isEmpty()) {
            this.serverField.setText(ERROR_EMPTY_SERVER);
            flashTextField(this.serverField);
            correct = false;
        }

        if (portString.isEmpty()) {
            this.portField.setText(ERROR_EMPTY_PORT);
            flashTextField(this.portField);
            correct = false;
        } else if (!isInteger(portString)) {
            this.portField.setText(ERROR_FORMAT_PORT);
            flashTextField(this.portField);
            correct = false;
        }

        if (correct) {
            int port = Integer.parseInt(portString);
            connectToServer(userNameString, serverString, port);
        }
    }

    private void connectToServer(String userName, String serverName, int portNumber) {
        client = new ChatClient(serverName, portNumber);
        chatPane.setItems(client.getMessages());
        client.connectUser(userName);
        client.sync();
    }

    public void sendButtonAction() throws UserNotFoundException {
        String text = msgField.getText();
        if (!text.isEmpty()) {
            client.sendBroadcastMsg(text);
            msgField.clear();
        } else {
            msgField.setText(ERROR_EMPTY_MESSAGE);
            flashTextField(this.msgField);
        }
    }

    public void exit() throws InterruptedException {
        if (client != null) {
            client.leave();
        }
        Platform.exit();
        System.exit(0);
    }

    /* ----------------------------- METHODS ----------------------------- */
    public static boolean isInteger(String s) {
        try {
            Integer.parseInt(s);
        } catch (NumberFormatException | NullPointerException e) {
            return false;
        }
        // only got here if we didn't return false
        return true;
    }

    /* ----------------------------- KEY PRESSED ----------------------------- */
    public void keyPressed(KeyEvent ke) throws UserNotFoundException {
        if (ke.getCode().equals(KeyCode.ENTER)) sendButtonAction();
    }

    /* ----------------------------- FIELD PRESSED ----------------------------- */
    public void userNameClicked() {
        userField.clear();
        userField.setEffect(null);
    }

    public void serverClicked() {
        serverField.clear();
        serverField.setEffect(null);
    }

    public void portClicked() {
        portField.clear();
        portField.setEffect(null);
    }

    public void messageFieldClicked() {
        msgField.setEffect(null);
    }

    /* ----------------------------- VISUAL EFFECTS ----------------------------- */
    public void flashTextField(TextField t) {
        Lighting errorLighting = new Lighting();
        t.setEffect(errorLighting);
    }


/*
    public void setUserList(Message msg) {
        System.out.println("setUserList() method Enter");
        Platform.runLater(() -> {
            ObservableList<User> users = FXCollections.observableList(msg.getUsers());
            userList.setItems(users);
            userList.setCellFactory(new CellRenderer());
            setOnlineLabel(String.valueOf(msg.getUserlist().size()));
        });
        System.out.println("setUserList() method Exit");
    }*/

}
