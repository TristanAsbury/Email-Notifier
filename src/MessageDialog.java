import javax.mail.*;
import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;

public class MessageDialog extends JDialog implements ActionListener {
    GroupLayout inputLayout;

    JPanel messagePanel;
    JPanel buttonPanel;

    JButton okayButton;

    Folder mailFolder;
    JLabel newMessagesLabel;
    JLabel fromLabel;
    JLabel subjectLabel;
    JLabel messageLabel;
    int newMessages;

    boolean isAlive;

    public MessageDialog(Folder mailFolder, int newMessages){ //For adding
        isAlive = true;
        this.mailFolder = mailFolder;
        this.newMessages = newMessages;
        createGUI();
        setUp();
    }
    
    //UI Stuff, nothing special
    private void createGUI(){
        try {
            Message recentMessage = mailFolder.getMessage(mailFolder.getMessageCount());
            newMessagesLabel = new JLabel("NEW MESSAGES: " + newMessages);
            fromLabel = new JLabel("From: " + recentMessage.getFrom()[0].toString());
            subjectLabel = new JLabel("Subject: " + recentMessage.getSubject());
            messageLabel = new JLabel("Message: " + recentMessage.getContent().toString());

            messagePanel = new JPanel();
            buttonPanel = new JPanel();

            inputLayout = new GroupLayout(messagePanel);
            messagePanel.setLayout(inputLayout);
            inputLayout.setAutoCreateGaps(true);
            inputLayout.setAutoCreateContainerGaps(true);

            GroupLayout.SequentialGroup hGroup = inputLayout.createSequentialGroup();
            hGroup.addGroup(inputLayout.createParallelGroup().
                        addComponent(newMessagesLabel)
                        .addComponent(fromLabel)
                        .addComponent(subjectLabel)
                        .addComponent(messageLabel));
            inputLayout.setHorizontalGroup(hGroup);
            GroupLayout.SequentialGroup vGroup = inputLayout.createSequentialGroup();
            vGroup.addGroup(inputLayout.createParallelGroup(Alignment.BASELINE).
                        addComponent(newMessagesLabel));
            vGroup.addGroup(inputLayout.createParallelGroup(Alignment.BASELINE).
                        addComponent(fromLabel));
            vGroup.addGroup(inputLayout.createParallelGroup(Alignment.BASELINE).
                        addComponent(subjectLabel));
            vGroup.addGroup(inputLayout.createParallelGroup(Alignment.BASELINE).
                        addComponent(messageLabel));
            inputLayout.setVerticalGroup(vGroup);

            okayButton = new JButton("Okay!");
            okayButton.addActionListener(this);
            
            
            buttonPanel.add(okayButton);
            add(messagePanel, BorderLayout.CENTER);
            add(buttonPanel, BorderLayout.SOUTH);

        } catch (MessagingException me){
            System.out.println("Problem reading mail!");
        } catch (IOException io){
            System.out.println("Problem accessing mail");
        }
    }
    
    public void actionPerformed(ActionEvent e){
        if(e.getSource().equals(okayButton)){
            dispose();                              //Close the window
            isAlive = false;
        }
    }

    private void setUp(){
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);  //This is kinda cheap, but having the only way to close the popup being the okay button allows the mailfolder to close
        Toolkit tk = Toolkit.getDefaultToolkit();
        Dimension d = tk.getScreenSize();
        setSize((int)d.getWidth()/5, (int)d.getHeight()/5);
        setLocation((int)d.getWidth()/5, (int)d.getHeight()/5);
        setTitle("Properties");
        setVisible(true);
    }
}
