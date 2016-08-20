/*
 * SerialCommView.java
 */

package serialcomm;

import org.jdesktop.application.Action;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.SingleFrameApplication;
import org.jdesktop.application.FrameView;
import org.jdesktop.application.TaskMonitor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
//import javax.swing.Timer;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import java.util.Timer;
import java.util.TimerTask;
import java.io.*;
import java.util.ArrayList;
import javax.swing.JFileChooser;
import java.lang.Object;
import java.util.Calendar;


/**
 * The automated sequence generator.
 */
class SequenceGenerator extends java.util.TimerTask {
  static int trials=0;

  SequenceGenerator()
  {
  }

  SequenceGenerator(boolean x)
  {
    just_cs=x;
  }	
	public void run() {
               if(just_cs) Send_codingschemes();
		
	       else if(activeNode < Constants.testbedSize) { //running experiment
			TestSerial serial = SerialCommApp.getApplication().getSerial(activeNode);
			System.out.println("Generating sequence for node " + activeNode);
			serial.startBeacon();
			activeNode++;
			if(activeNode == Constants.testbedSize)
				pollingDone = true;
		} else if(readyToWrite == true) { //experiment done. ready to save file
			//Save file
			//SerialCommApp.getApplication().getView().getPanel().build_tree();
			Calendar thisMoment = Calendar.getInstance();
			int month = thisMoment.get(Calendar.MONTH) + 1; //Calendar returns 0 for JANUARY :( :( :(
			try
			{
				FileOutputStream fos = new FileOutputStream(thisMoment.get(Calendar.YEAR) + "_" + 
					month + "_" + thisMoment.get(Calendar.DAY_OF_MONTH) + "_" +
					thisMoment.get(Calendar.HOUR_OF_DAY) + ":" + thisMoment.get(Calendar.MINUTE) + ":" +
					thisMoment.get(Calendar.SECOND) + ".top");
				ObjectOutputStream oos = new ObjectOutputStream(fos);
				oos.writeObject(SerialCommApp.getApplication().getView().getPanel().getnodeArray());
				oos.close();
				System.out.println("Auto Writing done");
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
			//Send coding schemes
                        Send_codingschemes();                        

			//Reset
			pollingNode = 0;
			activeNode = 0;
			pollingDone = false;
			readyToWrite = false;
		for(int i=0; i<Constants.testbedSize; i++) {
			SerialCommApp.getApplication().getSerial(i).resetStatus();
		} 
			trials++;
                        if(trials==10) this.cancel();
		} else if(pollingDone == true) { //receiving neighbor table
			TestSerial serial = SerialCommApp.getApplication().getSerial(pollingNode);
			System.out.println("Retrieving NbrTbl from node " + pollingNode);
			serial.dumpNbrTbl();
			pollingNode++;
			if(pollingNode == Constants.testbedSize) {
				readyToWrite = true;
			}
		}
	}
 
        public void Send_codingschemes()
        {
          /* Build the tree first */
	  SerialCommApp.getApplication().getView().getPanel().build_tree();
          int TOS_SOURCE = 8;
          int source_id=-1;
          int node=0,nodeID;
		while(node < Constants.testbedSize) { //running experiment
                    try{
                     Thread.sleep(1000);
                       }
                    catch(InterruptedException e)
                     {
                      System.out.println("Interr");
                     }
                     nodeID=SerialCommApp.getApplication().getView().getPanel().getNodeId(node);
                     if(nodeID==-1) break;    
                     if(nodeID==TOS_SOURCE) source_id=node;
			TestSerial serial = SerialCommApp.getApplication().getSerial(nodeID);
			System.out.println("Sending cs for node " + node);
			serial.sendCS(SerialCommApp.getApplication().getView().getPanel().coding_scheme(node), SerialCommApp.getApplication().getView().getPanel().hopcount(node));
			node++;
                 }
           if(source_id!=-1)
                 {
			TestSerial serial = SerialCommApp.getApplication().getSerial(source_id);
			System.out.println("Sending cs for node " + source_id);
			serial.sendCS((short)1,(short)1);
         }
        }  
	
	static boolean pollingDone=false;
	static boolean readyToWrite=false;
	static int activeNode=0;
	static int pollingNode=0;
        static boolean just_cs=false;
}

/**
 * The application's main frame.
 */
public class SerialCommView extends FrameView {

    public SerialCommView(SingleFrameApplication app) {
        super(app);

        initComponents();

        // status bar initialization - message timeout, idle icon and busy animation, etc
        ResourceMap resourceMap = getResourceMap();
        int messageTimeout = resourceMap.getInteger("StatusBar.messageTimeout");
        messageTimer = new javax.swing.Timer(messageTimeout, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                statusMessageLabel.setText("");
            }
        });
        messageTimer.setRepeats(false);
        int busyAnimationRate = resourceMap.getInteger("StatusBar.busyAnimationRate");
        for (int i = 0; i < busyIcons.length; i++) {
            busyIcons[i] = resourceMap.getIcon("StatusBar.busyIcons[" + i + "]");
        }
        busyIconTimer = new javax.swing.Timer(busyAnimationRate, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                busyIconIndex = (busyIconIndex + 1) % busyIcons.length;
                statusAnimationLabel.setIcon(busyIcons[busyIconIndex]);
            }
        });
        idleIcon = resourceMap.getIcon("StatusBar.idleIcon");
        statusAnimationLabel.setIcon(idleIcon);
        progressBar.setVisible(false);

        // connecting action tasks to status bar via TaskMonitor
        TaskMonitor taskMonitor = new TaskMonitor(getApplication().getContext());
        taskMonitor.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                String propertyName = evt.getPropertyName();
                if ("started".equals(propertyName)) {
                    if (!busyIconTimer.isRunning()) {
                        statusAnimationLabel.setIcon(busyIcons[0]);
                        busyIconIndex = 0;
                        busyIconTimer.start();
                    }
                    progressBar.setVisible(true);
                    progressBar.setIndeterminate(true);
                } else if ("done".equals(propertyName)) {
                    busyIconTimer.stop();
                    statusAnimationLabel.setIcon(idleIcon);
                    progressBar.setVisible(false);
                    progressBar.setValue(0);
                } else if ("message".equals(propertyName)) {
                    String text = (String)(evt.getNewValue());
                    statusMessageLabel.setText((text == null) ? "" : text);
                    messageTimer.restart();
                } else if ("progress".equals(propertyName)) {
                    int value = (Integer)(evt.getNewValue());
                    progressBar.setVisible(true);
                    progressBar.setIndeterminate(false);
                    progressBar.setValue(value);
                }
            }
        });
    }

    @Action
    public void showAboutBox() {
        if (aboutBox == null) {
            JFrame mainFrame = SerialCommApp.getApplication().getMainFrame();
            aboutBox = new SerialCommAboutBox(mainFrame);
            aboutBox.setLocationRelativeTo(mainFrame);
        }
        SerialCommApp.getApplication().show(aboutBox);
    }
	
	@Action
	public void open() {
		System.out.println("Entered open");
		JFileChooser chooser = new JFileChooser();
		int returnVal = chooser.showOpenDialog(null);
		if(returnVal == JFileChooser.APPROVE_OPTION) {
			System.out.println("You chose to open file: " + chooser.getSelectedFile().getName());
			try
			{
				FileInputStream fis = new FileInputStream(chooser.getSelectedFile().getName());
				ObjectInputStream ois = new ObjectInputStream(fis);
				ArrayList nodeArrayFromFile = (ArrayList) ois.readObject();
				SerialCommApp.getApplication().getView().getPanel().setnodeArray(nodeArrayFromFile);
				ois.close();
				System.out.println("Reading done");
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
		else {
			System.out.println("No file chosen to open");
		}
	}
	
	@Action
	public void save() {
		System.out.println("Entered save");
		JFileChooser chooser = new JFileChooser();
		int returnVal = chooser.showSaveDialog(null);
		if(returnVal == JFileChooser.APPROVE_OPTION) {
			System.out.println("You chose to save file: " + chooser.getSelectedFile().getName());
			try
			{
				FileOutputStream fos = new FileOutputStream(chooser.getSelectedFile().getName());
				ObjectOutputStream oos = new ObjectOutputStream(fos);
				oos.writeObject(SerialCommApp.getApplication().getView().getPanel().getnodeArray());
				oos.close();
				System.out.println("Writing done");
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
		else {
			System.out.println("No file chosen to save");
		}
		
	}

    public MyPanel getPanel() {
        return (MyPanel) mainPanel;
    }

    public JButton getButton(int index) {
        switch (index) {
            case 0:
                return jButton1;
            case 1:
                return jButton2;
            case 2:
                return jButton3;
            case 3:
                return jButton4;
            case 4:
                return jButton5;
            case 5:
                return jButton6;
            case 6:
                return jButton7;
            case 7:
                return jButton8;
            case 8:
                return jButton9;
            case 9:
                return jButton10;
            case 10:
                return jButton11;
            case 11:
                return jButton12;
            case 12:
                return jButton13;
            case 13:
                return jButton14;
            case 14:
                return jButton15;
            case 15:
                return jButton16;
            case 16:
                return jButton17;
            case 17:
                return jButton18;
            case 18:
                return jButton19;
            case 19:
                return jButton20;
            case 20:
                return jButton21;
            case 21:
                return jButton22;
            case 22:
                return jButton23;
            case 23:
                return jButton24;
            case 24:
                return jButton25;
            case 25:
                return jButton26;
            case 26:
                return jButton27;
            case 27:
                return jButton28;
            case 28:
                return jButton29;
            case 29:
                return jButton30;
            case 30:
                return jButton31;
            case 31:
                return jButton32;
            case 32:
                return jButton33;
            case 33:
                return jButton34;
            case 34:
                return jButton35;
            case 35:
                return jButton36;
            case 36:
                return jButton37;
            case 37:
                return jButton38;
            case 38:
                return jButton39;
            case 39:
                return jButton40;
            case 40:
                return jButton41;
			case 41:
				return jButton42;
            default:
                return null;
        }
    }
	
	public double getThreshold() {
		return jSlider1.getValue()/100.0;
	}

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        mainPanel = new MyPanel();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
        jButton4 = new javax.swing.JButton();
        jButton5 = new javax.swing.JButton();
        jButton6 = new javax.swing.JButton();
        jButton7 = new javax.swing.JButton();
        jButton8 = new javax.swing.JButton();
        jButton9 = new javax.swing.JButton();
        jButton10 = new javax.swing.JButton();
        jButton11 = new javax.swing.JButton();
        jButton12 = new javax.swing.JButton();
        jButton13 = new javax.swing.JButton();
        jButton14 = new javax.swing.JButton();
        jButton15 = new javax.swing.JButton();
        jButton16 = new javax.swing.JButton();
        jButton17 = new javax.swing.JButton();
        jButton18 = new javax.swing.JButton();
        jButton19 = new javax.swing.JButton();
        jButton20 = new javax.swing.JButton();
        jButton21 = new javax.swing.JButton();
        jButton22 = new javax.swing.JButton();
        jButton23 = new javax.swing.JButton();
        jButton24 = new javax.swing.JButton();
        jButton25 = new javax.swing.JButton();
        jButton26 = new javax.swing.JButton();
        jButton27 = new javax.swing.JButton();
        jButton28 = new javax.swing.JButton();
        jButton29 = new javax.swing.JButton();
        jButton30 = new javax.swing.JButton();
        jButton31 = new javax.swing.JButton();
        jButton32 = new javax.swing.JButton();
        jButton33 = new javax.swing.JButton();
        jButton34 = new javax.swing.JButton();
        jButton35 = new javax.swing.JButton();
        jButton36 = new javax.swing.JButton();
        jButton37 = new javax.swing.JButton();
        jButton38 = new javax.swing.JButton();
        jButton39 = new javax.swing.JButton();
        jButton40 = new javax.swing.JButton();
        jButton41 = new javax.swing.JButton();
        jButton42 = new javax.swing.JButton();
        menuBar = new javax.swing.JMenuBar();
        javax.swing.JMenu fileMenu = new javax.swing.JMenu();
        javax.swing.JMenuItem openMenuItem = new javax.swing.JMenuItem();
        javax.swing.JMenuItem saveMenuItem = new javax.swing.JMenuItem();
        javax.swing.JMenuItem exitMenuItem = new javax.swing.JMenuItem();
        javax.swing.JMenu helpMenu = new javax.swing.JMenu();
        javax.swing.JMenuItem aboutMenuItem = new javax.swing.JMenuItem();
        statusPanel = new javax.swing.JPanel();
        javax.swing.JSeparator statusPanelSeparator = new javax.swing.JSeparator();
        statusMessageLabel = new javax.swing.JLabel();
        statusAnimationLabel = new javax.swing.JLabel();
        progressBar = new javax.swing.JProgressBar();
        jButton44 = new javax.swing.JButton();
        jButton43 = new javax.swing.JButton();
        jButton45 = new javax.swing.JButton();
        jSlider1 = new javax.swing.JSlider();
        buttonGroup1 = new javax.swing.ButtonGroup();
        buttonGroup2 = new javax.swing.ButtonGroup();

        mainPanel.setName("mainPanel");

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(serialcomm.SerialCommApp.class).getContext().getResourceMap(SerialCommView.class);
        jButton1.setText(resourceMap.getString("jButton1.text")); // NOI18N
        jButton1.setBorderPainted(false);
        jButton1.setContentAreaFilled(false);
        jButton1.setDefaultCapable(false);
        jButton1.setFocusPainted(false);
        jButton1.setFocusable(false);
        jButton1.setMargin(new java.awt.Insets(5, 10, 5, 10));
        jButton1.setName("jButton1"); // NOI18N
        jButton1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton1MouseClicked(evt);
            }
        });

        jButton2.setText(resourceMap.getString("jButton2.text")); // NOI18N
        jButton2.setBorderPainted(false);
        jButton2.setContentAreaFilled(false);
        jButton2.setDefaultCapable(false);
        jButton2.setFocusPainted(false);
        jButton2.setFocusable(false);
        jButton2.setMargin(new java.awt.Insets(5, 10, 5, 10));
        jButton2.setName("jButton2"); // NOI18N
        jButton2.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton2MouseClicked(evt);
            }
        });

        jButton3.setText(resourceMap.getString("jButton3.text")); // NOI18N
        jButton3.setBorderPainted(false);
        jButton3.setContentAreaFilled(false);
        jButton3.setDefaultCapable(false);
        jButton3.setFocusPainted(false);
        jButton3.setFocusable(false);
        jButton3.setMargin(new java.awt.Insets(5, 10, 5, 10));
        jButton3.setName("jButton3"); // NOI18N
        jButton3.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton3MouseClicked(evt);
            }
        });

        jButton4.setText(resourceMap.getString("jButton4.text")); // NOI18N
        jButton4.setBorderPainted(false);
        jButton4.setContentAreaFilled(false);
        jButton4.setDefaultCapable(false);
        jButton4.setFocusPainted(false);
        jButton4.setFocusable(false);
        jButton4.setMargin(new java.awt.Insets(5, 10, 5, 10));
        jButton4.setName("jButton4"); // NOI18N
        jButton4.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton4MouseClicked(evt);
            }
        });

        jButton5.setText(resourceMap.getString("jButton5.text")); // NOI18N
        jButton5.setBorderPainted(false);
        jButton5.setContentAreaFilled(false);
        jButton5.setDefaultCapable(false);
        jButton5.setFocusPainted(false);
        jButton5.setFocusable(false);
        jButton5.setMargin(new java.awt.Insets(5, 10, 5, 10));
        jButton5.setName("jButton5"); // NOI18N
        jButton5.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton5MouseClicked(evt);
            }
        });

        jButton6.setText(resourceMap.getString("jButton6.text")); // NOI18N
        jButton6.setBorderPainted(false);
        jButton6.setContentAreaFilled(false);
        jButton6.setDefaultCapable(false);
        jButton6.setFocusPainted(false);
        jButton6.setFocusable(false);
        jButton6.setMargin(new java.awt.Insets(5, 10, 5, 10));
        jButton6.setName("jButton6"); // NOI18N
        jButton6.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton6MouseClicked(evt);
            }
        });

        jButton7.setText(resourceMap.getString("jButton7.text")); // NOI18N
        jButton7.setBorderPainted(false);
        jButton7.setContentAreaFilled(false);
        jButton7.setDefaultCapable(false);
        jButton7.setFocusPainted(false);
        jButton7.setFocusable(false);
        jButton7.setMargin(new java.awt.Insets(5, 10, 5, 10));
        jButton7.setName("jButton7"); // NOI18N
        jButton7.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton7MouseClicked(evt);
            }
        });

        jButton8.setText(resourceMap.getString("jButton8.text")); // NOI18N
        jButton8.setBorderPainted(false);
        jButton8.setContentAreaFilled(false);
        jButton8.setDefaultCapable(false);
        jButton8.setFocusPainted(false);
        jButton8.setFocusable(false);
        jButton8.setMargin(new java.awt.Insets(5, 10, 5, 10));
        jButton8.setName("jButton8"); // NOI18N
        jButton8.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton8MouseClicked(evt);
            }
        });

        jButton9.setText(resourceMap.getString("jButton9.text")); // NOI18N
        jButton9.setBorderPainted(false);
        jButton9.setContentAreaFilled(false);
        jButton9.setDefaultCapable(false);
        jButton9.setFocusPainted(false);
        jButton9.setFocusable(false);
        jButton9.setMargin(new java.awt.Insets(5, 10, 5, 10));
        jButton9.setName("jButton9"); // NOI18N
        jButton9.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton9MouseClicked(evt);
            }
        });

        jButton10.setText(resourceMap.getString("jButton10.text")); // NOI18N
        jButton10.setBorderPainted(false);
        jButton10.setContentAreaFilled(false);
        jButton10.setDefaultCapable(false);
        jButton10.setFocusPainted(false);
        jButton10.setFocusable(false);
        jButton10.setMargin(new java.awt.Insets(5, 10, 5, 10));
        jButton10.setName("jButton10"); // NOI18N
        jButton10.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton10MouseClicked(evt);
            }
        });

        jButton11.setText(resourceMap.getString("jButton11.text")); // NOI18N
        jButton11.setBorderPainted(false);
        jButton11.setContentAreaFilled(false);
        jButton11.setDefaultCapable(false);
        jButton11.setFocusPainted(false);
        jButton11.setFocusable(false);
        jButton11.setMargin(new java.awt.Insets(5, 10, 5, 10));
        jButton11.setName("jButton11"); // NOI18N
        jButton11.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton11MouseClicked(evt);
            }
        });

        jButton12.setText(resourceMap.getString("jButton12.text")); // NOI18N
        jButton12.setBorderPainted(false);
        jButton12.setContentAreaFilled(false);
        jButton12.setDefaultCapable(false);
        jButton12.setFocusPainted(false);
        jButton12.setFocusable(false);
        jButton12.setMargin(new java.awt.Insets(5, 10, 5, 10));
        jButton12.setName("jButton12"); // NOI18N
        jButton12.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton12MouseClicked(evt);
            }
        });

        jButton13.setText(resourceMap.getString("jButton13.text")); // NOI18N
        jButton13.setBorderPainted(false);
        jButton13.setContentAreaFilled(false);
        jButton13.setDefaultCapable(false);
        jButton13.setFocusPainted(false);
        jButton13.setFocusable(false);
        jButton13.setMargin(new java.awt.Insets(5, 10, 5, 10));
        jButton13.setName("jButton13"); // NOI18N
        jButton13.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton13MouseClicked(evt);
            }
        });

        jButton14.setText(resourceMap.getString("jButton14.text")); // NOI18N
        jButton14.setBorderPainted(false);
        jButton14.setContentAreaFilled(false);
        jButton14.setDefaultCapable(false);
        jButton14.setFocusPainted(false);
        jButton14.setFocusable(false);
        jButton14.setMargin(new java.awt.Insets(5, 10, 5, 10));
        jButton14.setName("jButton14"); // NOI18N
        jButton14.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton14MouseClicked(evt);
            }
        });

        jButton15.setText(resourceMap.getString("jButton15.text")); // NOI18N
        jButton15.setBorderPainted(false);
        jButton15.setContentAreaFilled(false);
        jButton15.setDefaultCapable(false);
        jButton15.setFocusPainted(false);
        jButton15.setFocusable(false);
        jButton15.setMargin(new java.awt.Insets(5, 10, 5, 10));
        jButton15.setName("jButton15"); // NOI18N
        jButton15.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton15MouseClicked(evt);
            }
        });

        jButton16.setText(resourceMap.getString("jButton16.text")); // NOI18N
        jButton16.setBorderPainted(false);
        jButton16.setContentAreaFilled(false);
        jButton16.setDefaultCapable(false);
        jButton16.setFocusPainted(false);
        jButton16.setFocusable(false);
        jButton16.setMargin(new java.awt.Insets(5, 10, 5, 10));
        jButton16.setName("jButton16"); // NOI18N
        jButton16.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton16MouseClicked(evt);
            }
        });

        jButton17.setText(resourceMap.getString("jButton17.text")); // NOI18N
        jButton17.setBorderPainted(false);
        jButton17.setContentAreaFilled(false);
        jButton17.setDefaultCapable(false);
        jButton17.setFocusPainted(false);
        jButton17.setFocusable(false);
        jButton17.setMargin(new java.awt.Insets(5, 10, 5, 10));
        jButton17.setName("jButton17"); // NOI18N
        jButton17.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton17MouseClicked(evt);
            }
        });

        jButton18.setText(resourceMap.getString("jButton18.text")); // NOI18N
        jButton18.setBorderPainted(false);
        jButton18.setContentAreaFilled(false);
        jButton18.setDefaultCapable(false);
        jButton18.setFocusPainted(false);
        jButton18.setFocusable(false);
        jButton18.setMargin(new java.awt.Insets(5, 10, 5, 10));
        jButton18.setName("jButton18"); // NOI18N
        jButton18.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton18MouseClicked(evt);
            }
        });

        jButton19.setText(resourceMap.getString("jButton19.text")); // NOI18N
        jButton19.setBorderPainted(false);
        jButton19.setContentAreaFilled(false);
        jButton19.setDefaultCapable(false);
        jButton19.setFocusPainted(false);
        jButton19.setFocusable(false);
        jButton19.setMargin(new java.awt.Insets(5, 10, 5, 10));
        jButton19.setName("jButton19"); // NOI18N
        jButton19.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton19MouseClicked(evt);
            }
        });

        jButton20.setText(resourceMap.getString("jButton20.text")); // NOI18N
        jButton20.setBorderPainted(false);
        jButton20.setContentAreaFilled(false);
        jButton20.setDefaultCapable(false);
        jButton20.setFocusPainted(false);
        jButton20.setFocusable(false);
        jButton20.setMargin(new java.awt.Insets(5, 10, 5, 10));
        jButton20.setName("jButton20"); // NOI18N
        jButton20.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton20MouseClicked(evt);
            }
        });

        jButton21.setText(resourceMap.getString("jButton21.text")); // NOI18N
        jButton21.setBorderPainted(false);
        jButton21.setContentAreaFilled(false);
        jButton21.setDefaultCapable(false);
        jButton21.setFocusPainted(false);
        jButton21.setFocusable(false);
        jButton21.setMargin(new java.awt.Insets(5, 10, 5, 10));
        jButton21.setName("jButton21"); // NOI18N
        jButton21.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton21MouseClicked(evt);
            }
        });

        jButton22.setText(resourceMap.getString("jButton22.text")); // NOI18N
        jButton22.setBorderPainted(false);
        jButton22.setContentAreaFilled(false);
        jButton22.setDefaultCapable(false);
        jButton22.setFocusPainted(false);
        jButton22.setFocusable(false);
        jButton22.setName("jButton22"); // NOI18N
        jButton22.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton22MouseClicked(evt);
            }
        });

        jButton23.setText(resourceMap.getString("jButton23.text")); // NOI18N
        jButton23.setBorderPainted(false);
        jButton23.setContentAreaFilled(false);
        jButton23.setDefaultCapable(false);
        jButton23.setFocusPainted(false);
        jButton23.setFocusable(false);
        jButton23.setName("jButton23"); // NOI18N
        jButton23.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton23MouseClicked(evt);
            }
        });

        jButton24.setText(resourceMap.getString("jButton24.text")); // NOI18N
        jButton24.setBorderPainted(false);
        jButton24.setContentAreaFilled(false);
        jButton24.setDefaultCapable(false);
        jButton24.setFocusPainted(false);
        jButton24.setFocusable(false);
        jButton24.setName("jButton24"); // NOI18N
        jButton24.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton24MouseClicked(evt);
            }
        });

        jButton25.setText(resourceMap.getString("jButton25.text")); // NOI18N
        jButton25.setBorderPainted(false);
        jButton25.setContentAreaFilled(false);
        jButton25.setDefaultCapable(false);
        jButton25.setFocusPainted(false);
        jButton25.setFocusable(false);
        jButton25.setName("jButton25"); // NOI18N
        jButton25.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton25MouseClicked(evt);
            }
        });

        jButton26.setText(resourceMap.getString("jButton26.text")); // NOI18N
        jButton26.setBorderPainted(false);
        jButton26.setContentAreaFilled(false);
        jButton26.setDefaultCapable(false);
        jButton26.setFocusPainted(false);
        jButton26.setFocusable(false);
        jButton26.setName("jButton26"); // NOI18N
        jButton26.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton26MouseClicked(evt);
            }
        });

        jButton27.setText(resourceMap.getString("jButton27.text")); // NOI18N
        jButton27.setBorderPainted(false);
        jButton27.setContentAreaFilled(false);
        jButton27.setDefaultCapable(false);
        jButton27.setFocusPainted(false);
        jButton27.setFocusable(false);
        jButton27.setName("jButton27"); // NOI18N
        jButton27.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton27MouseClicked(evt);
            }
        });

        jButton28.setText(resourceMap.getString("jButton28.text")); // NOI18N
        jButton28.setBorderPainted(false);
        jButton28.setContentAreaFilled(false);
        jButton28.setDefaultCapable(false);
        jButton28.setFocusPainted(false);
        jButton28.setFocusable(false);
        jButton28.setName("jButton28"); // NOI18N
        jButton28.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton28MouseClicked(evt);
            }
        });

        jButton29.setText(resourceMap.getString("jButton29.text")); // NOI18N
        jButton29.setBorderPainted(false);
        jButton29.setContentAreaFilled(false);
        jButton29.setDefaultCapable(false);
        jButton29.setFocusPainted(false);
        jButton29.setFocusable(false);
        jButton29.setName("jButton29"); // NOI18N
        jButton29.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton29MouseClicked(evt);
            }
        });

        jButton30.setText(resourceMap.getString("jButton30.text")); // NOI18N
        jButton30.setBorderPainted(false);
        jButton30.setContentAreaFilled(false);
        jButton30.setDefaultCapable(false);
        jButton30.setFocusPainted(false);
        jButton30.setFocusable(false);
        jButton30.setName("jButton30"); // NOI18N
        jButton30.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton30MouseClicked(evt);
            }
        });

        jButton31.setText(resourceMap.getString("jButton31.text")); // NOI18N
        jButton31.setBorderPainted(false);
        jButton31.setContentAreaFilled(false);
        jButton31.setDefaultCapable(false);
        jButton31.setFocusPainted(false);
        jButton31.setFocusable(false);
        jButton31.setName("jButton31"); // NOI18N
        jButton31.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton31MouseClicked(evt);
            }
        });

        jButton32.setText(resourceMap.getString("jButton32.text")); // NOI18N
        jButton32.setBorderPainted(false);
        jButton32.setContentAreaFilled(false);
        jButton32.setDefaultCapable(false);
        jButton32.setFocusPainted(false);
        jButton32.setFocusable(false);
        jButton32.setName("jButton32"); // NOI18N
        jButton32.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton32MouseClicked(evt);
            }
        });

        jButton33.setText(resourceMap.getString("jButton33.text")); // NOI18N
        jButton33.setBorderPainted(false);
        jButton33.setContentAreaFilled(false);
        jButton33.setDefaultCapable(false);
        jButton33.setFocusPainted(false);
        jButton33.setFocusable(false);
        jButton33.setName("jButton33"); // NOI18N
        jButton33.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton33MouseClicked(evt);
            }
        });

        jButton34.setText(resourceMap.getString("jButton34.text")); // NOI18N
        jButton34.setBorderPainted(false);
        jButton34.setContentAreaFilled(false);
        jButton34.setDefaultCapable(false);
        jButton34.setFocusPainted(false);
        jButton34.setFocusable(false);
        jButton34.setName("jButton34"); // NOI18N
        jButton34.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton34MouseClicked(evt);
            }
        });

        jButton35.setText(resourceMap.getString("jButton35.text")); // NOI18N
        jButton35.setBorderPainted(false);
        jButton35.setContentAreaFilled(false);
        jButton35.setDefaultCapable(false);
        jButton35.setFocusPainted(false);
        jButton35.setFocusable(false);
        jButton35.setName("jButton35"); // NOI18N
        jButton35.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton35MouseClicked(evt);
            }
        });

        jButton36.setText(resourceMap.getString("jButton36.text")); // NOI18N
        jButton36.setBorderPainted(false);
        jButton36.setContentAreaFilled(false);
        jButton36.setDefaultCapable(false);
        jButton36.setFocusPainted(false);
        jButton36.setFocusable(false);
        jButton36.setName("jButton36"); // NOI18N
        jButton36.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton36MouseClicked(evt);
            }
        });

        jButton37.setText(resourceMap.getString("jButton37.text")); // NOI18N
        jButton37.setBorderPainted(false);
        jButton37.setContentAreaFilled(false);
        jButton37.setDefaultCapable(false);
        jButton37.setFocusPainted(false);
        jButton37.setFocusable(false);
        jButton37.setName("jButton37"); // NOI18N
        jButton37.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton37MouseClicked(evt);
            }
        });

        jButton38.setText(resourceMap.getString("jButton38.text")); // NOI18N
        jButton38.setBorderPainted(false);
        jButton38.setContentAreaFilled(false);
        jButton38.setDefaultCapable(false);
        jButton38.setFocusPainted(false);
        jButton38.setFocusable(false);
        jButton38.setName("jButton38"); // NOI18N
        jButton38.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton38MouseClicked(evt);
            }
        });

        jButton39.setText(resourceMap.getString("jButton39.text")); // NOI18N
        jButton39.setBorderPainted(false);
        jButton39.setContentAreaFilled(false);
        jButton39.setDefaultCapable(false);
        jButton39.setFocusPainted(false);
        jButton39.setFocusable(false);
        jButton39.setName("jButton39"); // NOI18N
        jButton39.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton39MouseClicked(evt);
            }
        });

        jButton40.setText(resourceMap.getString("jButton40.text")); // NOI18N
        jButton40.setBorderPainted(false);
        jButton40.setContentAreaFilled(false);
        jButton40.setDefaultCapable(false);
        jButton40.setFocusPainted(false);
        jButton40.setFocusable(false);
        jButton40.setName("jButton40"); // NOI18N
        jButton40.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton40MouseClicked(evt);
            }
        });

        jButton41.setText(resourceMap.getString("jButton41.text")); // NOI18N
        jButton41.setBorderPainted(false);
        jButton41.setContentAreaFilled(false);
        jButton41.setDefaultCapable(false);
        jButton41.setFocusPainted(false);
        jButton41.setFocusable(false);
        jButton41.setName("jButton41"); // NOI18N
        jButton41.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton41MouseClicked(evt);
            }
        });

        jButton42.setText(resourceMap.getString("jButton42.text")); // NOI18N
        jButton42.setBorderPainted(false);
        jButton42.setContentAreaFilled(false);
        jButton42.setDefaultCapable(false);
        jButton42.setFocusPainted(false);
        jButton42.setFocusable(false);
        jButton42.setName("jButton42"); // NOI18N
        jButton42.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton42MouseClicked(evt);
            }
        });

        javax.swing.GroupLayout mainPanelLayout = new javax.swing.GroupLayout(mainPanel);
        mainPanel.setLayout(mainPanelLayout);
        mainPanelLayout.setHorizontalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mainPanelLayout.createSequentialGroup()
                .addGap(861, 861, 861)
                .addComponent(jButton26)
                .addContainerGap(961, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, mainPanelLayout.createSequentialGroup()
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(mainPanelLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jButton28))
                    .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                        .addGroup(mainPanelLayout.createSequentialGroup()
                            .addContainerGap()
                            .addComponent(jButton24))
                        .addGroup(mainPanelLayout.createSequentialGroup()
                            .addGap(42, 42, 42)
                            .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jButton16)
                                .addComponent(jButton15)
                                .addComponent(jButton11)
                                .addComponent(jButton18))
                            .addGap(34, 34, 34)
                            .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, mainPanelLayout.createSequentialGroup()
                                    .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jButton10)
                                        .addComponent(jButton19)
                                        .addGroup(mainPanelLayout.createSequentialGroup()
                                            .addComponent(jButton14)
                                            .addGap(61, 61, 61)
                                            .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addComponent(jButton20)
                                                .addComponent(jButton21))
                                            .addGap(148, 148, 148)
                                            .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addComponent(jButton1)
                                                .addComponent(jButton3)
                                                .addGroup(mainPanelLayout.createSequentialGroup()
                                                    .addGap(12, 12, 12)
                                                    .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(jButton23, javax.swing.GroupLayout.Alignment.TRAILING)
                                                        .addComponent(jButton32, javax.swing.GroupLayout.Alignment.TRAILING)
                                                        .addComponent(jButton39, javax.swing.GroupLayout.Alignment.TRAILING))))))
                                    .addGap(86, 86, 86)
                                    .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(mainPanelLayout.createSequentialGroup()
                                            .addComponent(jButton8)
                                            .addGap(72, 72, 72)
                                            .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(mainPanelLayout.createSequentialGroup()
                                                    .addComponent(jButton4)
                                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 229, Short.MAX_VALUE)
                                                    .addComponent(jButton25))
                                                .addGroup(mainPanelLayout.createSequentialGroup()
                                                    .addGap(20, 20, 20)
                                                    .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                                        .addComponent(jButton27)
                                                        .addComponent(jButton35)
                                                        .addComponent(jButton42)))
                                                .addComponent(jButton6)
                                                .addComponent(jButton12)
                                                .addComponent(jButton7)))
                                        .addGroup(mainPanelLayout.createSequentialGroup()
                                            .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addComponent(jButton22)
                                                .addComponent(jButton31)
                                                .addComponent(jButton38)
                                                .addComponent(jButton13)
                                                .addComponent(jButton2))
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED))))
                                .addGroup(mainPanelLayout.createSequentialGroup()
                                    .addGap(188, 188, 188)
                                    .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                        .addComponent(jButton33)
                                        .addComponent(jButton40)
                                        .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(jButton9)
                                            .addComponent(jButton17)
                                            .addComponent(jButton5)))
                                    .addGap(141, 141, 141)
                                    .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                        .addComponent(jButton34)
                                        .addComponent(jButton41))
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED))))))
                .addGap(886, 886, 886))
            .addGroup(mainPanelLayout.createSequentialGroup()
                .addGap(630, 630, 630)
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(mainPanelLayout.createSequentialGroup()
                        .addComponent(jButton36)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jButton37))
                    .addGroup(mainPanelLayout.createSequentialGroup()
                        .addComponent(jButton29)
                        .addGap(119, 119, 119)
                        .addComponent(jButton30)))
                .addContainerGap(1045, Short.MAX_VALUE))
        );
        mainPanelLayout.setVerticalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mainPanelLayout.createSequentialGroup()
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(mainPanelLayout.createSequentialGroup()
                        .addGap(37, 37, 37)
                        .addComponent(jButton23)
                        .addGap(144, 144, 144)
                        .addComponent(jButton26)
                        .addGap(27, 27, 27)
                        .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jButton36)
                            .addComponent(jButton37)
                            .addComponent(jButton40)
                            .addComponent(jButton41))
                        .addGap(13, 13, 13)
                        .addComponent(jButton28)
                        .addGap(48, 48, 48)
                        .addComponent(jButton24)
                        .addGap(24, 24, 24)
                        .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(mainPanelLayout.createSequentialGroup()
                                .addGap(72, 72, 72)
                                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jButton16)
                                    .addComponent(jButton4))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 67, Short.MAX_VALUE)
                                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jButton12)
                                    .addComponent(jButton1)
                                    .addComponent(jButton21)
                                    .addComponent(jButton11))
                                .addGap(48, 48, 48))
                            .addGroup(mainPanelLayout.createSequentialGroup()
                                .addGap(46, 46, 46)
                                .addComponent(jButton25)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 6, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, mainPanelLayout.createSequentialGroup()
                        .addGap(60, 60, 60)
                        .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jButton22)
                            .addComponent(jButton27))
                        .addGap(57, 57, 57)
                        .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jButton38)
                            .addComponent(jButton39)
                            .addComponent(jButton42))
                        .addGap(168, 168, 168)
                        .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jButton31)
                            .addComponent(jButton32)
                            .addComponent(jButton35))
                        .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(mainPanelLayout.createSequentialGroup()
                                .addGap(57, 57, 57)
                                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jButton29)
                                    .addComponent(jButton30)
                                    .addComponent(jButton33)
                                    .addComponent(jButton34))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 52, Short.MAX_VALUE)
                                .addComponent(jButton17)
                                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(mainPanelLayout.createSequentialGroup()
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jButton10)
                                        .addGap(68, 68, 68)
                                        .addComponent(jButton19))
                                    .addGroup(mainPanelLayout.createSequentialGroup()
                                        .addGap(113, 113, 113)
                                        .addComponent(jButton9)))
                                .addGap(8, 8, 8))
                            .addGroup(mainPanelLayout.createSequentialGroup()
                                .addGap(183, 183, 183)
                                .addComponent(jButton13)
                                .addGap(70, 70, 70)
                                .addComponent(jButton2)
                                .addGap(6, 6, 6)))))
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(mainPanelLayout.createSequentialGroup()
                        .addGap(18, 18, 18)
                        .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jButton15)
                            .addComponent(jButton20)
                            .addComponent(jButton3)
                            .addComponent(jButton6))
                        .addGap(53, 53, 53)
                        .addComponent(jButton18))
                    .addGroup(mainPanelLayout.createSequentialGroup()
                        .addGap(53, 53, 53)
                        .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jButton14)
                            .addComponent(jButton8))
                        .addGap(18, 18, 18)
                        .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jButton5)
                            .addComponent(jButton7))))
                .addGap(72, 72, 72))
        );

        menuBar.setName("menuBar"); // NOI18N

        fileMenu.setText(resourceMap.getString("fileMenu.text")); // NOI18N
        fileMenu.setName("fileMenu"); // NOI18N

        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(serialcomm.SerialCommApp.class).getContext().getActionMap(SerialCommView.class, this);
        openMenuItem.setAction(actionMap.get("open")); // NOI18N
        openMenuItem.setName("openMenuItem"); // NOI18N
        fileMenu.add(openMenuItem);

        saveMenuItem.setAction(actionMap.get("save")); // NOI18N
        saveMenuItem.setName("saveMenuItem"); // NOI18N
        fileMenu.add(saveMenuItem);

        exitMenuItem.setAction(actionMap.get("quit")); // NOI18N
        exitMenuItem.setName("exitMenuItem"); // NOI18N
        fileMenu.add(exitMenuItem);

        menuBar.add(fileMenu);

        helpMenu.setText(resourceMap.getString("helpMenu.text")); // NOI18N
        helpMenu.setName("helpMenu"); // NOI18N

        aboutMenuItem.setAction(actionMap.get("showAboutBox")); // NOI18N
        aboutMenuItem.setName("aboutMenuItem"); // NOI18N
        helpMenu.add(aboutMenuItem);

        menuBar.add(helpMenu);

        statusPanel.setName("statusPanel"); // NOI18N

        statusPanelSeparator.setName("statusPanelSeparator"); // NOI18N

        statusMessageLabel.setName("statusMessageLabel"); // NOI18N

        statusAnimationLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        statusAnimationLabel.setName("statusAnimationLabel"); // NOI18N

        progressBar.setName("progressBar"); // NOI18N

        jButton44.setText(resourceMap.getString("jButton44.text")); // NOI18N
        jButton44.setName("jButton44"); // NOI18N
        jButton44.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton44MouseClicked(evt);
            }
        });

        jButton43.setText(resourceMap.getString("jButton43.text")); // NOI18N
        jButton43.setName("jButton43"); // NOI18N
        jButton43.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton43MouseClicked(evt);
            }
        });

        jButton45.setText(resourceMap.getString("jButton45.text")); // NOI18N
        jButton45.setName("jButton45"); // NOI18N
        jButton45.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton45MouseClicked(evt);
            }
        });

        jSlider1.setName("jSlider1"); // NOI18N
        jSlider1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                jSlider1MouseReleased(evt);
            }
        });

        javax.swing.GroupLayout statusPanelLayout = new javax.swing.GroupLayout(statusPanel);
        statusPanel.setLayout(statusPanelLayout);
        statusPanelLayout.setHorizontalGroup(
            statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(statusPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(statusMessageLabel)
                .addGap(62, 62, 62)
                .addComponent(jButton43, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(32, 32, 32)
                .addComponent(jSlider1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(53, 53, 53)
                .addComponent(jButton44, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(53, 53, 53)
                .addComponent(jButton45, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(178, 178, 178)
                .addGroup(statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(statusPanelSeparator, javax.swing.GroupLayout.DEFAULT_SIZE, 1137, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, statusPanelLayout.createSequentialGroup()
                        .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(statusAnimationLabel)
                        .addContainerGap())))
        );
        statusPanelLayout.setVerticalGroup(
            statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(statusPanelLayout.createSequentialGroup()
                .addComponent(statusPanelSeparator, javax.swing.GroupLayout.PREFERRED_SIZE, 2, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 41, Short.MAX_VALUE)
                .addGroup(statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(statusMessageLabel)
                    .addComponent(statusAnimationLabel)
                    .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(3, 3, 3))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, statusPanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jSlider1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton44)
                    .addComponent(jButton45)
                    .addComponent(jButton43, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        setComponent(mainPanel);
        setMenuBar(menuBar);
        setStatusBar(statusPanel);
    }// </editor-fold>//GEN-END:initComponents

    private void jButton44MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton44MouseClicked
        // TODO add your handling code here:
		((MyPanel) mainPanel).toggleShowState();
}//GEN-LAST:event_jButton44MouseClicked

    private void jButton45MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton45MouseClicked
                if(Inprocess==1) return;
                Inprocess=1;         
		/* SerialCommApp.getApplication().InitializeSerial(); */
		
		//Reset status 
		for(int i=0; i<Constants.testbedSize; i++) {
			SerialCommApp.getApplication().getSerial(i).resetStatus();
		} 
                /* Send a message for reset */
		
		java.util.Timer sequencetimer = new java.util.Timer();
		sequencetimer.schedule(new SequenceGenerator(), 0, 15000);
                Inprocess=0;         
   }

    private void jButton43MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton43MouseClicked
        // TODO add your handling code here:
		/** START THE ENTIRE EXPERIMENT SEQUENCE HERE **/
		//Create serial instances
                if(Inprocess==1) return;
                Inprocess=1;         
		SerialCommApp.getApplication().InitializeSerial();
		
		//Reset status 
		for(int i=0; i<Constants.testbedSize; i++) {
			SerialCommApp.getApplication().getSerial(i).resetStatus();
		}
		
		java.util.Timer sequencetimer = new java.util.Timer();
		sequencetimer.schedule(new SequenceGenerator(), 0, 15000);
                Inprocess=0;         
}//GEN-LAST:event_jButton43MouseClicked

    private void jButton1MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton1MouseClicked
        // TODO add your handling code here:
        ((MyPanel) mainPanel).nodeClicked((short) 0);
    }//GEN-LAST:event_jButton1MouseClicked

    private void jButton3MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton3MouseClicked
        // TODO add your handling code here:
        ((MyPanel) mainPanel).nodeClicked((short) 2);
    }//GEN-LAST:event_jButton3MouseClicked

    private void jButton2MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton2MouseClicked
        // TODO add your handling code here:
        ((MyPanel) mainPanel).nodeClicked((short) 1);
    }//GEN-LAST:event_jButton2MouseClicked

    private void jButton4MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton4MouseClicked
        // TODO add your handling code here:
        ((MyPanel) mainPanel).nodeClicked((short) 3);
    }//GEN-LAST:event_jButton4MouseClicked

    private void jButton5MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton5MouseClicked
        // TODO add your handling code here:
        ((MyPanel) mainPanel).nodeClicked((short) 4);
    }//GEN-LAST:event_jButton5MouseClicked

    private void jButton6MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton6MouseClicked
        // TODO add your handling code here:
        ((MyPanel) mainPanel).nodeClicked((short) 5);
    }//GEN-LAST:event_jButton6MouseClicked

    private void jButton7MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton7MouseClicked
        // TODO add your handling code here:
        ((MyPanel) mainPanel).nodeClicked((short) 6);
    }//GEN-LAST:event_jButton7MouseClicked

    private void jButton8MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton8MouseClicked
        // TODO add your handling code here:
        ((MyPanel) mainPanel).nodeClicked((short) 7);
    }//GEN-LAST:event_jButton8MouseClicked

    private void jButton9MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton9MouseClicked
        // TODO add your handling code here:
        ((MyPanel) mainPanel).nodeClicked((short) 8);
    }//GEN-LAST:event_jButton9MouseClicked

    private void jButton10MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton10MouseClicked
        // TODO add your handling code here:
        ((MyPanel) mainPanel).nodeClicked((short) 9);
    }//GEN-LAST:event_jButton10MouseClicked

    private void jButton11MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton11MouseClicked
        // TODO add your handling code here:
        ((MyPanel) mainPanel).nodeClicked((short) 10);
    }//GEN-LAST:event_jButton11MouseClicked

    private void jButton12MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton12MouseClicked
        // TODO add your handling code here:
        ((MyPanel) mainPanel).nodeClicked((short) 11);
    }//GEN-LAST:event_jButton12MouseClicked

    private void jButton13MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton13MouseClicked
        // TODO add your handling code here:
        ((MyPanel) mainPanel).nodeClicked((short) 12);
    }//GEN-LAST:event_jButton13MouseClicked

    private void jButton14MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton14MouseClicked
        // TODO add your handling code here:
        ((MyPanel) mainPanel).nodeClicked((short) 13);
    }//GEN-LAST:event_jButton14MouseClicked

    private void jButton15MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton15MouseClicked
        // TODO add your handling code here:
        ((MyPanel) mainPanel).nodeClicked((short) 14);
    }//GEN-LAST:event_jButton15MouseClicked

    private void jButton16MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton16MouseClicked
        // TODO add your handling code here:
        ((MyPanel) mainPanel).nodeClicked((short) 15);
    }//GEN-LAST:event_jButton16MouseClicked

    private void jButton17MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton17MouseClicked
        // TODO add your handling code here:
        ((MyPanel) mainPanel).nodeClicked((short) 16);
    }//GEN-LAST:event_jButton17MouseClicked

    private void jButton18MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton18MouseClicked
        // TODO add your handling code here:
        ((MyPanel) mainPanel).nodeClicked((short) 17);
    }//GEN-LAST:event_jButton18MouseClicked

    private void jButton19MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton19MouseClicked
        // TODO add your handling code here:
        ((MyPanel) mainPanel).nodeClicked((short) 18);
    }//GEN-LAST:event_jButton19MouseClicked

    private void jButton20MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton20MouseClicked
        // TODO add your handling code here:
        ((MyPanel) mainPanel).nodeClicked((short) 19);
    }//GEN-LAST:event_jButton20MouseClicked

    private void jButton21MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton21MouseClicked
        // TODO add your handling code here:
        ((MyPanel) mainPanel).nodeClicked((short) 20);
    }//GEN-LAST:event_jButton21MouseClicked

    private void jButton22MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton22MouseClicked
        // TODO add your handling code here:
        ((MyPanel) mainPanel).nodeClicked((short) 21);
    }//GEN-LAST:event_jButton22MouseClicked

    private void jButton23MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton23MouseClicked
        // TODO add your handling code here:
        ((MyPanel) mainPanel).nodeClicked((short) 22);
    }//GEN-LAST:event_jButton23MouseClicked

    private void jButton24MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton24MouseClicked
        // TODO add your handling code here:
        ((MyPanel) mainPanel).nodeClicked((short) 23);
    }//GEN-LAST:event_jButton24MouseClicked

    private void jButton25MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton25MouseClicked
        // TODO add your handling code here:
        ((MyPanel) mainPanel).nodeClicked((short) 24);
    }//GEN-LAST:event_jButton25MouseClicked

    private void jButton26MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton26MouseClicked
        // TODO add your handling code here:
        ((MyPanel) mainPanel).nodeClicked((short) 25);
    }//GEN-LAST:event_jButton26MouseClicked

    private void jButton27MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton27MouseClicked
        // TODO add your handling code here:
        ((MyPanel) mainPanel).nodeClicked((short) 26);
    }//GEN-LAST:event_jButton27MouseClicked

    private void jButton28MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton28MouseClicked
        // TODO add your handling code here:
        ((MyPanel) mainPanel).nodeClicked((short) 27);
    }//GEN-LAST:event_jButton28MouseClicked

    private void jButton29MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton29MouseClicked
        // TODO add your handling code here:
        ((MyPanel) mainPanel).nodeClicked((short) 28);
    }//GEN-LAST:event_jButton29MouseClicked

    private void jButton30MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton30MouseClicked
        // TODO add your handling code here:
        ((MyPanel) mainPanel).nodeClicked((short) 29);
    }//GEN-LAST:event_jButton30MouseClicked

    private void jButton31MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton31MouseClicked
        // TODO add your handling code here:
        ((MyPanel) mainPanel).nodeClicked((short) 30);
    }//GEN-LAST:event_jButton31MouseClicked

    private void jButton32MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton32MouseClicked
        // TODO add your handling code here:
        ((MyPanel) mainPanel).nodeClicked((short) 31);
    }//GEN-LAST:event_jButton32MouseClicked

    private void jButton33MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton33MouseClicked
        // TODO add your handling code here:
        ((MyPanel) mainPanel).nodeClicked((short) 32);
    }//GEN-LAST:event_jButton33MouseClicked

    private void jButton34MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton34MouseClicked
        // TODO add your handling code here:
        ((MyPanel) mainPanel).nodeClicked((short) 33);
    }//GEN-LAST:event_jButton34MouseClicked

    private void jButton35MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton35MouseClicked
        // TODO add your handling code here:
        ((MyPanel) mainPanel).nodeClicked((short) 34);
    }//GEN-LAST:event_jButton35MouseClicked

    private void jButton36MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton36MouseClicked
        // TODO add your handling code here:
        ((MyPanel) mainPanel).nodeClicked((short) 35);
    }//GEN-LAST:event_jButton36MouseClicked

    private void jButton37MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton37MouseClicked
        // TODO add your handling code here:
        ((MyPanel) mainPanel).nodeClicked((short) 36);
    }//GEN-LAST:event_jButton37MouseClicked

    private void jButton38MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton38MouseClicked
        // TODO add your handling code here:
        ((MyPanel) mainPanel).nodeClicked((short) 37);
    }//GEN-LAST:event_jButton38MouseClicked

    private void jButton39MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton39MouseClicked
        // TODO add your handling code here:
        ((MyPanel) mainPanel).nodeClicked((short) 38);
    }//GEN-LAST:event_jButton39MouseClicked

    private void jButton40MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton40MouseClicked
        // TODO add your handling code here:
        ((MyPanel) mainPanel).nodeClicked((short) 39);
    }//GEN-LAST:event_jButton40MouseClicked

    private void jButton41MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton41MouseClicked
        // TODO add your handling code here:
        ((MyPanel) mainPanel).nodeClicked((short) 40);
    }//GEN-LAST:event_jButton41MouseClicked

    private void jButton42MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton42MouseClicked
        // TODO add your handling code here:
        ((MyPanel) mainPanel).nodeClicked((short) 41);
    }//GEN-LAST:event_jButton42MouseClicked

    private void jSlider1MouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jSlider1MouseReleased
        // TODO add your handling code here:
        ((MyPanel) mainPanel).OnThresholdChange();
    }//GEN-LAST:event_jSlider1MouseReleased

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.ButtonGroup buttonGroup2;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton10;
    private javax.swing.JButton jButton11;
    private javax.swing.JButton jButton12;
    private javax.swing.JButton jButton13;
    private javax.swing.JButton jButton14;
    private javax.swing.JButton jButton15;
    private javax.swing.JButton jButton16;
    private javax.swing.JButton jButton17;
    private javax.swing.JButton jButton18;
    private javax.swing.JButton jButton19;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton20;
    private javax.swing.JButton jButton21;
    private javax.swing.JButton jButton22;
    private javax.swing.JButton jButton23;
    private javax.swing.JButton jButton24;
    private javax.swing.JButton jButton25;
    private javax.swing.JButton jButton26;
    private javax.swing.JButton jButton27;
    private javax.swing.JButton jButton28;
    private javax.swing.JButton jButton29;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton30;
    private javax.swing.JButton jButton31;
    private javax.swing.JButton jButton32;
    private javax.swing.JButton jButton33;
    private javax.swing.JButton jButton34;
    private javax.swing.JButton jButton35;
    private javax.swing.JButton jButton36;
    private javax.swing.JButton jButton37;
    private javax.swing.JButton jButton38;
    private javax.swing.JButton jButton39;
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButton40;
    private javax.swing.JButton jButton41;
    private javax.swing.JButton jButton42;
    private javax.swing.JButton jButton43;
    private javax.swing.JButton jButton45;
    private javax.swing.JButton jButton44;
    private javax.swing.JButton jButton5;
    private javax.swing.JButton jButton6;
    private javax.swing.JButton jButton7;
    private javax.swing.JButton jButton8;
    private javax.swing.JButton jButton9;
    private javax.swing.JSlider jSlider1;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JLabel statusAnimationLabel;
    private javax.swing.JLabel statusMessageLabel;
    private javax.swing.JPanel statusPanel;
    private static int Inprocess=0;
    // End of variables declaration//GEN-END:variables

    private final javax.swing.Timer messageTimer;
    private final javax.swing.Timer busyIconTimer;
    private final Icon idleIcon;
    private final Icon[] busyIcons = new Icon[15];
    private int busyIconIndex = 0;
	private File file = new File("firstfile.txt");

    private JDialog aboutBox;
}
