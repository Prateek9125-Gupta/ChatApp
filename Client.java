import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class Client implements ActionListener {
    /* GUI & NETWORK FIELDS */
    private final JTextField inputField;
    private final JPanel messageArea;
    private static final Box vertical=Box.createVerticalBox();
    private static final JFrame frame=new JFrame();

    private JButton sendBtn;
    private volatile boolean myTurn=false; // waits for server question

    private static DataOutputStream dout;

    public Client(){
        frame.setLayout(null);
        frame.setSize(450,700);
        frame.setLocation(800,30);
        frame.setUndecorated(true);
        frame.getContentPane().setBackground(Color.WHITE);

        frame.add(buildHeader());

        messageArea=new JPanel();
        messageArea.setBounds(5,75,440,570);
        frame.add(messageArea);

        inputField=new JTextField();
        inputField.setBounds(5,655,310,40);
        inputField.setFont(new Font("SAN_SERIF",Font.PLAIN,16));
        frame.add(inputField);

        sendBtn=new JButton("Send");
        sendBtn.setBounds(320,655,123,40);
        sendBtn.setBackground(new Color(7,94,84));
        sendBtn.setForeground(Color.WHITE);
        sendBtn.setFont(new Font("SAN_SERIF",Font.PLAIN,16));
        sendBtn.addActionListener(this);
        frame.add(sendBtn);

        updateTurnState();
        frame.setVisible(true);

        instance=this; // expose early
    }

    private JPanel buildHeader(){
        JPanel p1=new JPanel();
        p1.setBackground(new Color(7,94,84));
        p1.setBounds(0,0,450,70);
        p1.setLayout(null);

        JLabel back=new JLabel(scale("icons/3.png",25,25));
        back.setBounds(5,20,25,25);
        back.addMouseListener(new MouseAdapter(){public void mouseClicked(MouseEvent e){System.exit(0);}});
        p1.add(back);

        p1.add(buildIcon("icons/2.png",40,10,50,50));
        p1.add(buildIcon("icons/video.png",300,20,30,30));
        p1.add(buildIcon("icons/phone.png",360,20,30,30));
        p1.add(buildIcon("icons/3icon.png",410,20,15,30));

        JLabel name=new JLabel("Thor");
        name.setBounds(110,15,120,18);
        name.setFont(new Font("SAN_SERIF",Font.BOLD,18));
        name.setForeground(Color.WHITE);
        p1.add(name);

        JLabel status=new JLabel("Active now");
        status.setBounds(110,35,120,18);
        status.setFont(new Font("SAN_SERIF",Font.PLAIN,14));
        status.setForeground(Color.WHITE);
        p1.add(status);
        return p1;
    }

    private JLabel buildIcon(String path,int x,int y,int w,int h){
        JLabel lbl=new JLabel(scale(path,w,h));
        lbl.setBounds(x,y,w,h);
        return lbl;
    }
    private ImageIcon scale(String path,int w,int h){
        ImageIcon ic=new ImageIcon(ClassLoader.getSystemResource(path));
        Image img=ic.getImage().getScaledInstance(w,h,Image.SCALE_DEFAULT);
        return new ImageIcon(img);
    }

    /* SEND HANDLER */
    @Override public void actionPerformed(ActionEvent e){
        if(!myTurn) return;
        String msg=inputField.getText().trim();
        if(msg.isEmpty()) return;
        appendRight(format(msg));
        try{ dout.writeUTF(msg);}catch(IOException ex){ex.printStackTrace();}
        inputField.setText("");
        myTurn=false;
        updateTurnState();
    }

    /* UI HELPERS */
    private void appendRight(JPanel panel){
        messageArea.setLayout(new BorderLayout());
        JPanel right=new JPanel(new BorderLayout());
        right.add(panel,BorderLayout.LINE_END);
        vertical.add(right);
        vertical.add(Box.createVerticalStrut(15));
        messageArea.add(vertical,BorderLayout.PAGE_START);
        refresh();
    }
    private void appendLeft(JPanel panel){
        messageArea.setLayout(new BorderLayout());
        JPanel left=new JPanel(new BorderLayout());
        left.add(panel,BorderLayout.LINE_START);
        vertical.add(left);
        vertical.add(Box.createVerticalStrut(15));
        messageArea.add(vertical,BorderLayout.PAGE_START);
        refresh();
    }
    private void refresh(){
        frame.repaint();
        frame.invalidate();
        frame.validate();
    }
    private void updateTurnState(){
        inputField.setEditable(myTurn);
        sendBtn.setEnabled(myTurn);
    }

    /* MESSAGE FACTORY */
    public static JPanel format(String txt){
        JPanel pnl=new JPanel();
        pnl.setLayout(new BoxLayout(pnl,BoxLayout.Y_AXIS));
        JLabel out=new JLabel("<html><p style=\"width:150px\">"+txt+"</p></html>");
        out.setFont(new Font("Tahoma",Font.PLAIN,16));
        out.setOpaque(true);
        out.setBackground(new Color(37,211,102));
        out.setBorder(new EmptyBorder(15,15,15,50));
        pnl.add(out);
        pnl.add(new JLabel(new SimpleDateFormat("HH:mm").format(new Date())));
        return pnl;
    }

    /* STATIC DISPATCH FROM LISTENER */
    private static Client instance;
    private static void deliverQuestion(String msg){
        SwingUtilities.invokeLater(()->{
            instance.appendLeft(format(msg));
            instance.myTurn=true;
            instance.updateTurnState();
        });
    }

    public static void main(String[] args){
        new Client();
        try{
            Socket socket=new Socket("127.0.0.1",6001);
            DataInputStream din=new DataInputStream(socket.getInputStream());
            dout=new DataOutputStream(socket.getOutputStream());

            Thread listener=new Thread(()->{
                try{
                    while(true){
                        String msg=din.readUTF();
                        deliverQuestion(msg);
                    }
                }catch(IOException ex){ex.printStackTrace();}
            });
            listener.setDaemon(true);
            listener.start();

        }catch(IOException ex){ex.printStackTrace();}
    }
}
