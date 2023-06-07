package com.sshtools.pushsftp.jfx;

import java.util.Optional;
import java.util.ResourceBundle;

import org.controlsfx.control.NotificationPane;

import com.sshtools.jajafx.AboutPage;
import com.sshtools.jajafx.AbstractTile;
import com.sshtools.jajafx.FXUtil;
import com.sshtools.jajafx.PageTransition;
import com.sshtools.jajafx.ScrollStack;
import com.sshtools.pushsftp.jfx.Target.TargetBuilder;

import eu.hansolo.medusa.Gauge;
import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;

public class DropPage extends AbstractTile<PushSFTPUIApp> {

	final static ResourceBundle RESOURCES = ResourceBundle.getBundle(DropPage.class.getName());

	@FXML
	private Label text;
	@FXML
	private HBox progressContainer;

	@FXML
	private ScrollStack scrollStack;

	@FXML
	private BorderPane dropTop;
	
	@FXML
	private Gauge progressGauge;
	
	@FXML
	private Gauge speedGauge;
	
	@FXML
	private Hyperlink scrollPrevious;
	
	@FXML
	private Hyperlink scrollNext;

	
	NotificationPane  notificationPane;

	@Override
	protected void onConfigure() {
		var service = getContext().getService();
		var targets = service.getTargets();
		
		targets.forEach(t -> scrollStack.add(new DropTarget().setup(t, getContext())));
		targets.addListener((ListChangeListener.Change<? extends Target> c) -> {
			while (c.next()) {
				if(c.wasReplaced()) {
					// A bit brute force, look for better way
					scrollStack.clear();
					targets.forEach(t -> scrollStack.add(new DropTarget().setup(t, getContext())));
				}
				else {
					for (var t : c.getAddedSubList()) {
						scrollStack.add(new DropTarget().setup(t, getContext()));
					}
					for(var t : c.getRemoved()) {
						scrollStack.remove(findDropTarget(t));
					}
				}
			}
		});
		
		FXUtil.clipChildren(scrollStack, 0);
		
		progressGauge.setMaxValue(100);
		service.busyProperty().addListener((c,o,n) ->{
			if(n) {
				notificationPane.hide();			
				resetGauges(); 
			}
		});
		service.summaryProperty().addListener((c,o,n) -> updateGauges());
		
		scrollPrevious.visibleProperty().bind(Bindings.not(scrollStack.showingFirstProperty()));
		scrollNext.visibleProperty().bind(Bindings.not(scrollStack.showingLastProperty()));
		
		progressContainer.disableProperty().bind(Bindings.not(service.busyProperty()));
		
		var dropTopParent = dropTop.getParent();
		notificationPane = new NotificationPane(dropTop);
		
		((AnchorPane)dropTopParent).getChildren().setAll(notificationPane);
		AnchorPane.setBottomAnchor(notificationPane, 0d);
		AnchorPane.setTopAnchor(notificationPane, 0d);
		AnchorPane.setLeftAnchor(notificationPane, 0d);
		AnchorPane.setRightAnchor(notificationPane, 0d);
		
		resetGauges();
		updateGauges();
	}

	private void resetGauges() {
		speedGauge.setBarColor(Color.valueOf("#0078d7"));
		progressGauge.setTitle("");
		progressGauge.setBarColor( Color.valueOf("#0078d7"));
		speedGauge.setMaxValue(100);
		speedGauge.setValue(0);
		progressGauge.setValue(0);
	}
	
	private void updateGauges() {
		var summary = getContext().getService().summaryProperty().get();
		progressGauge.setValue(summary.percentage());
		var speed = summary.bytesPerSecond() / 1024D;
		if(speed > speedGauge.getMaxValue())
			speedGauge.setMaxValue(speed);
		speedGauge.setValue(speed);
		if(summary.percentage() == 100) {
			speedGauge.setBarColor(Color.GREEN.darker());
			progressGauge.setBarColor(Color.GREEN.darker());
		}
		progressGauge.setTitle(summary.size() == 0 ? "" :  summary.timeRemainingString());
	}
	
	private DropTarget findDropTarget(Target target) {
		for(var child : scrollStack.getNodes()) {
			if(target.equals(((DropTarget)child).getTarget())) {
				return (DropTarget)child;
			}
		}
		throw new IllegalArgumentException("No such panel for target. " + target);
	}

	@FXML
	void addTarget(ActionEvent evt) {
		getTiles().popup(EditTargetPage.class, PageTransition.FROM_RIGHT).setTarget(TargetBuilder.builder().build(),
				(newTarget) -> getContext().getService().getTargets().add(newTarget), Optional.empty());
	}

	@FXML
	void next(ActionEvent evt) {
		scrollStack.next();
	}

	@FXML
	void previous(ActionEvent evt) {
		scrollStack.previous();
	}

	@FXML
	void about(ActionEvent evt) {
		getTiles().popup(AboutPage.class, PageTransition.FROM_LEFT);
	}

	@FXML
	void queue(ActionEvent evt) {
		getTiles().popup(QueuePage.class, PageTransition.FROM_LEFT);
	}

	@FXML
	void options(ActionEvent evt) {
		getTiles().popup(OptionsPage.class, PageTransition.FROM_LEFT);
	}

}
