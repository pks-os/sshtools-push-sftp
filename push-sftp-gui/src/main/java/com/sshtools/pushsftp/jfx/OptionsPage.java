package com.sshtools.pushsftp.jfx;

import java.nio.file.Paths;
import java.util.ResourceBundle;

import com.sshtools.jajafx.AbstractTile;
import com.sshtools.jajafx.FXUtil;
import com.sshtools.jajafx.PrefBind;
import com.sshtools.jaul.Phase;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;

public class OptionsPage extends AbstractTile<PushSFTPUIApp> {

	final static ResourceBundle RESOURCES = ResourceBundle.getBundle(OptionsPage.class.getName());

	private PrefBind prefBind;

	@FXML
	TextField agentSocket;
	@FXML
	CheckBox verbose;
	@FXML
	CheckBox automaticUpdates;
	@FXML
	ComboBox<Phase> phase;

	private FileChooser agentSocketChooser;

	@Override
	protected void onConfigure() {
		phase.getItems().addAll(getContext().getContainer().getUpdateService().getPhases());
		prefBind = new PrefBind(getContext().getContainer().getAppPreferences());
		prefBind.bind(verbose, automaticUpdates);
		prefBind.bind(agentSocket);
		prefBind.bind(Phase.class, phase);
		agentSocket.setPromptText(System.getenv("SSH_AUTH_SOCK"));
		
	}
	@FXML
	private void back(ActionEvent evt) {
		getTiles().remove(this);
		
	}

	@FXML
	private void browseAgentSocket() {
		if (agentSocketChooser == null) {
			agentSocketChooser = new FileChooser();
			agentSocketChooser.setInitialDirectory(Paths.get(FXUtil.textOrPrompt(agentSocket)).getParent().toFile());
		}
		agentSocketChooser.setTitle(RESOURCES.getString("agentSocket.choose.title"));
		var selectedDirectory = agentSocketChooser.showOpenDialog(getScene().getWindow());
		if (selectedDirectory != null) {
			agentSocket.setText(selectedDirectory.getAbsolutePath());
		}
	}

	@Override
	public void close() {
		prefBind.close();
	}

}
