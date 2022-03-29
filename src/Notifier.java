import javax.mail.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Notifier implements ActionListener {
    Timer timer;
    
    Session session;        //Session of mail
    Authenticator auth;

    Store store;            //Store
    Folder inboxFolder;     //Inbox folder object
    Message[] messages;     //Messages array
    MessageDialog newMailDialog;

    int newMessageCount;
    boolean playSound;

    SystemTray tray;
    PopupMenu trayPopup;
    MenuItem exitItem, settingsItem, toggleSoundItem;

    int intervalMinutes;
    Properties props;

    public Notifier(){
        newMessageCount = 0;
        System.out.println("Creating tray!");
        if(SystemTray.isSupported()){               //If there is a system tray that is supported.
            tray = SystemTray.getSystemTray();      //Then we will make a pointer to that tray
            
            trayPopup = new PopupMenu();            //Create a new popup menu for the tray item

            exitItem = new MenuItem("Exit");        //Add the menu items
            exitItem.addActionListener(this);
            trayPopup.add(exitItem);                     //Add the menu items to the popup

            settingsItem = new MenuItem("Settings");
            settingsItem.addActionListener(this);
            trayPopup.add(settingsItem);

            toggleSoundItem = new MenuItem("Sound");
            toggleSoundItem.addActionListener(this);
            trayPopup.add(toggleSoundItem);
            
            setupProps();
            setupTray();                                    //Setup the tray
        } else {
            System.out.println("Failed to find system tray!");
        }
    }

    private void setupProps(){
        //Create new properties object
        props = new Properties();
        //Try to find properties file
        try {
            props.load(new FileInputStream("src\\resources\\props.properties")); //If there is already a properties
        } catch (IOException e){
            PropertiesDialog propertiesDialog = new PropertiesDialog(props, true);
            System.out.println("Failed loading props file!");
        }
        
        if(props.getProperty("notisound") == "true"){       //Update the notification sound value
            toggleSoundItem.setLabel("Sound on");
        } else {
            toggleSoundItem.setLabel("Sound off");
        }
        
        //Try connecting to the mail server with properties
        try {                                       
            auth = null;
            session = Session.getInstance(props, auth);
            store = session.getStore("imaps");
            store.connect(props.getProperty("host"), props.getProperty("username"), props.getProperty("password"));
            inboxFolder = store.getFolder("INBOX");

            inboxFolder.open(Folder.READ_WRITE);    //Open folder and see how many new messages
            if(inboxFolder.getNewMessageCount() > 0){
                JOptionPane.showMessageDialog(null, "You gained " + inboxFolder.getNewMessageCount() + " new messages since you last checked your mail!");  //Display if we have new messages
            }
            inboxFolder.close(false);
            inboxFolder.open(Folder.READ_WRITE);    //Start regular loop
        } catch (NoSuchProviderException np){      
            System.out.println("No such provider: " + props.getProperty("protocolProvider"));
            PropertiesDialog propertiesDialog = new PropertiesDialog(props, false);
            setupProps();
        } catch (MessagingException me) {
            System.out.println("Messaging Exception!");
            PropertiesDialog propertiesDialog = new PropertiesDialog(props, false);
            setupProps();
        }
    }


    //Opens the connection to the mail server
    private void openConnection(){
        try {
            auth = null;
            session = Session.getInstance(props, auth);
            store = session.getStore("imaps");
            store.connect(props.getProperty("host"), props.getProperty("username"), props.getProperty("password"));
            inboxFolder = store.getFolder("INBOX");
            inboxFolder.open(Folder.READ_WRITE);    //Open folder and see how many new messages
        } catch (NoSuchProviderException nsp){      //If there was a problem with the settings, open them back up again
            openSettingsDialog();
        } catch (MessagingException me){            //If there was a problem with the settings, open them back up again
            openSettingsDialog();
        }
    }

    //Closes connection to the mail server
    private void closeConnection(){
        try {
            if(inboxFolder.isOpen()){
                inboxFolder.close(false);
            }
            if(store.isConnected()){
                store.close();
            }
        } catch (MessagingException me) {
            System.out.println("Error closing mail folder!");
        }
    }

    //Opens settings dialog
    private void openSettingsDialog(){
        //This is needed for the first check
        timer.stop();       //Stops the timer that may be currently running
        closeConnection();  //Close the connection
        PropertiesDialog propertiesDialog = new PropertiesDialog(props, false);  //Open settings dialog so user can edit the properties
        playSound = Boolean.valueOf(props.getProperty("notisound"));
        updateNotiSoundItem();
        openConnection();                                               //Attempt to open the connection after the user closed the dialog
        timer.start();                                                  //Start the timer so we can check for mail
    }


    private void setupTray(){
        TrayIcon trayIcon = new TrayIcon(Toolkit.getDefaultToolkit().getImage("src\\resources\\icon.png"), "Mail Notifier");
        trayIcon.setImageAutoSize(true);
        trayIcon.setPopupMenu(trayPopup);
        
        try{
            tray.add(trayIcon);
        } catch (AWTException e){
            System.out.println("Failed to add icon!");
            return;     //If the setup fails, return to the constructor, and pretty much end the program.
        }
        
        timer = new Timer(Integer.parseInt(props.getProperty("interval")) * 60000, this);
        timer.setActionCommand("CHECK");
        timer.setRepeats(true);
        timer.start();
        
    }

    public void actionPerformed(ActionEvent e) {
        if(e.getActionCommand().equals("CHECK")){
            System.out.println("Checking mail!");
            openConnection();
            checkFolder(inboxFolder);
            closeConnection();
        }

        if(e.getSource().equals(exitItem)){
            closeConnection();
            System.exit(0);
        }

        if(e.getSource().equals(settingsItem)){
            openSettingsDialog();
        }

        if(e.getSource().equals(toggleSoundItem)){
            playSound = !playSound;
            updateNotiSoundItem();
        }
    }

    private void updateNotiSoundItem(){
        if(playSound){
            toggleSoundItem.setLabel("Sound on");
        } else {
            toggleSoundItem.setLabel("Sound off");
        }
        
    }

    private void checkFolder(Folder mailFolder){
        //Get new messages
        try {
            if(mailFolder.hasNewMessages()){    //If there is a new message
                if(newMailDialog == null){
                    newMessageCount = mailFolder.getNewMessageCount();
                    newMailDialog = new MessageDialog(mailFolder, newMessageCount);
                } else {              
                    if(newMailDialog.isAlive){              //If there is a dialog present and the new mail count is different
                        newMailDialog.dispose();            //Dispose of the dialog
                        newMessageCount += mailFolder.getNewMessageCount();
                        newMailDialog = new MessageDialog(mailFolder, newMessageCount);     //Create a new one with the updated count
                    } else {
                        newMailDialog.dispose();                                        //If there isn't a mailDialog present, dispose in case
                        newMessageCount = mailFolder.getNewMessageCount();              //Get new message count
                        newMailDialog = new MessageDialog(mailFolder, newMessageCount); //Create a new dialog with the new message count
                    }
                }

                if(playSound){
                    new PlaySound("src\\resources\\uh_oh.wav").start();
                }

                inboxFolder.close(false);
                store.close();

            } else {
                System.out.println("No new messages");
            }
        } catch (MessagingException me){
            System.out.println("Error accessing new mail!");
        }
    }
    
}