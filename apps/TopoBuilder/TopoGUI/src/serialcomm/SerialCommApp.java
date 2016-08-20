/*
 * SerialCommApp.java
 */

package serialcomm;

import net.tinyos.message.MoteIF;
import net.tinyos.packet.BuildSource;
import net.tinyos.packet.PhoenixSource;
import net.tinyos.util.PrintStreamMessenger;
import org.jdesktop.application.Application;
import org.jdesktop.application.SingleFrameApplication;

/**
 * The main class of the application.
 */
public class SerialCommApp extends SingleFrameApplication {

    private static SerialCommView view;
	private static TestSerial[] serialArray;

    /**
     * At startup create and show the main frame of the application.
     */
    @Override protected void startup() {
        view = new SerialCommView(this);
        show(view);
    }

    /**
     * This method is to initialize the specified window by injecting resources.
     * Windows shown in our application come fully initialized from the GUI
     * builder, so this additional configuration is not needed.
     */
    @Override protected void configureWindow(java.awt.Window root) {
    }

    /**
     * A convenient static getter for the application instance.
     * @return the instance of SerialCommApp
     */
    public static SerialCommApp getApplication() {
        return Application.getInstance(SerialCommApp.class);
    }

    public SerialCommView getView() {
        return view;
    }

    public TestSerial getSerial(int parseInt) {
		return serialArray[parseInt];
    }

	public void InitializeSerial()
	{
		serialArray = new TestSerial[Constants.testbedSize];
		for(int i=0; i<Constants.testbedSize; i++) {
			serialArray[i] = new TestSerial(new MoteIF(BuildSource.makePhoenix("serial@/dev/ttyUSB" + i + 
												":115200", PrintStreamMessenger.err)));
		}
	}
	
    /**
     * Main method launching the application.
     */
    public static void main(String[] args) {

        launch(SerialCommApp.class, args);
 
    }
}
