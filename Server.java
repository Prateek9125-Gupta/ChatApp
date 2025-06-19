import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class Server implements ActionListener {
    /* ----------- GUI & NETWORK FIELDS ----------- */
    private final JTextField inputField;
    private final JPanel messageArea;
    private static final Box vertical = Box.createVerticalBox();
    private static final JFrame frame = new JFrame();

    private JButton sendBtn;
    private volatile boolean myTurn = true; // Server starts by asking

    /* Socket streams made static so listener thread can access */
    private static DataOutputStream dout;

    /* ----------- CONSTRUCTOR ----------- */
    public Server() {
        /* ——— Frame base ——— */
        frame.setLayout(null);
        frame.setSize(450, 700);
        frame.setLocation(200, 30);
        frame.setUndecorated(true);
        frame.getContentPane().setBackground(Color.WHITE);

        /* ——— Header bar ——— */
        JPanel header = buildHeader();
        frame.add(header);

        /* ——— Message panel ——— */
        messageArea = new JPanel();
        messageArea.setBounds(5, 75, 440, 570);
        frame.add(messageArea);

        /* ——— Input field ——— */
        inputField = new JTextField();
        inputField.setBounds(5, 655, 310, 40);
        inputField.setFont(new Font("SAN_SERIF", Font.PLAIN, 16));
        frame.add(inputField);

        /* ——— Send button ——— */
        sendBtn = new JButton("Send");
        sendBtn.setBounds(320, 655, 123, 40);
        sendBtn.setBackground(new Color(7, 94, 84));
        sendBtn.setForeground(Color.WHITE);
        sendBtn.setFont(new Font("SAN_SERIF", Font.PLAIN, 16));
        sendBtn.addActionListener(this);
        frame.add(sendBtn);

        updateTurnState();
        frame.setVisible(true);

        /* Expose this instance for static callbacks before socket thread starts */
        instance = this;
    }

    /* ----------- HEADER BUILDER ----------- */
    private JPanel buildHeader() {
        JPanel p1 = new JPanel();
        p1.setBackground(new Color(7, 94, 84));
        p1.setBounds(0, 0, 450, 70);
        p1.setLayout(null);

        JLabel back = new JLabel(scale("icons/3.png", 25, 25));
        back.setBounds(5, 20, 25, 25);
        back.addMouseListener(new MouseAdapter() { public void mouseClicked(MouseEvent e){ System.exit(0);} });
        p1.add(back);

        p1.add(buildIcon("icons/1.png", 40, 10, 50, 50));
        p1.add(buildIcon("icons/video.png", 300, 20, 30, 30));
        p1.add(buildIcon("icons/phone.png", 360, 20, 30, 30));
        p1.add(buildIcon("icons/3icon.png", 410, 20, 15, 30));

        JLabel name = new JLabel("Iron‑man");
        name.setBounds(110, 15, 120, 18);
        name.setFont(new Font("SAN_SERIF", Font.BOLD, 18));
        name.setForeground(Color.WHITE);
        p1.add(name);

        JLabel status = new JLabel("Active now");
        status.setBounds(110, 35, 120, 18);
        status.setFont(new Font("SAN_SERIF", Font.PLAIN, 14));
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

    /* ----------- SEND BUTTON HANDLER ----------- */
    @Override public void actionPerformed(ActionEvent e){
        if(!myTurn) return;
        String msg=inputField.getText().trim();
        if(msg.isEmpty()) return;
        appendRight(format(msg));
        try{ dout.writeUTF(msg); }catch(IOException ex){ ex.printStackTrace(); }
        inputField.setText("");
        myTurn=false;
        updateTurnState();
    }

    /* ----------- UI HELPERS ----------- */
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

    /* ----------- MESSAGE FACTORY ----------- */
    public static JPanel format(String txt){
        JPanel pnl=new JPanel();
        pnl.setLayout(new BoxLayout(pnl,BoxLayout.Y_AXIS));
        JLabel out=new JLabel("<html><p style=\"width:150px\">"+txt+"</p></html>");
        out.setFont(new Font("Tahoma",Font.PLAIN,16));
        out.setOpaque(true);
        out.setBackground(new Color(37,211,102));
        out.setBorder(new EmptyBorder(15,15,15,50));
        pnl.add(out);
        JLabel time=new JLabel(new SimpleDateFormat("HH:mm").format(new Date()));
        pnl.add(time);
        return pnl;
    }

    /* ----------- STATIC DISPATCH FROM LISTENER ----------- */
    private static Server instance;
    private static void deliverFromClient(String msg){
        SwingUtilities.invokeLater(()->{
            instance.appendLeft(format(msg));
            instance.myTurn=true;
            instance.updateTurnState();
        });
    }

    /* ----------- MAIN ----------- */
    public static void main(String[] args){
        new Server();
        try(ServerSocket ss=new ServerSocket(6001)){
            Socket socket=ss.accept();
            DataInputStream din=new DataInputStream(socket.getInputStream());
            dout=new DataOutputStream(socket.getOutputStream());

            // listener thread keeps GUI responsive
            Thread t=new Thread(()->{
                try{
                    while(true){
                        String msg=din.readUTF();
                        deliverFromClient(msg);
                    }
                }catch(IOException ex){ ex.printStackTrace(); }
            });
            t.setDaemon(true);
            t.start();

        }catch(IOException ex){ ex.printStackTrace(); }
    }
}
