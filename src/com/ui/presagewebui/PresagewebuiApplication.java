package com.ui.presagewebui;



import com.vaadin.Application;
import com.vaadin.ui.*;


@SuppressWarnings("serial")
public class PresagewebuiApplication extends Application {
	/**
	 * 
	 */

	@Override
	public void init(){
		Window mainWindow = new Window("Presagewebui Application");
		mainWindow.setImmediate(true);
		AppViewController appLayout = new AppViewController();
		mainWindow.setContent(appLayout);
		setMainWindow(mainWindow);
	}

}
