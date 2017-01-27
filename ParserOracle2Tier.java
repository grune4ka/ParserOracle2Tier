package parseroracle2tier;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.*;

public class ParserOracle2Tier {

    private final JButton main_btn = new JButton();
    private final JButton chan_btn = new JButton();
    private final JButton open_act = new JButton();      
    private ArrayList<File> selected_file = null;    
    private final JFrame root = new JFrame("Parser Oracle 2Tier");
    private final JPanel panel = new JPanel();   
    private final ArrayList<JLabel> content = new ArrayList<>();
    private short condt = 1;
    private JFileChooser wind = new JFileChooser("Open Action.c file in folder script");
    
    public ParserOracle2Tier() {
        panel.setLayout(null);
        chan_btn.setText("Change Action.c file");
        main_btn.setText("Open Action.c file");
        chan_btn.setBounds(190, 180, 140, 30);      
        main_btn.setBounds(340, 180, 140, 30);        
        content.add(new JLabel("Status: on start"));
        content.get(0).setFont(new Font("Arial", Font.PLAIN, 16));      
        content.get(0).setBounds(15, 180, 250, 30);               
        panel.add(main_btn);
        panel.add(content.get(0));
        
        open_act.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                try {
                    ProcessBuilder proc = new ProcessBuilder("notepad.exe", selected_file.get(0).getPath());
                    proc.start();
                } catch (IOException e) { /* сделать вывод ошибки в окне */ }
            }
        });
        
        chan_btn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                File tmp;
                ArrayList<File> tmp_array_path;
                if (wind.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) 
                    return;
                tmp = wind.getSelectedFile();
                if ((tmp_array_path = Prs.checkDirectory(tmp, content)) == null) {
                    if (panel.getBackground() != Color.LIGHT_GRAY) {                    
                        content.get(0).setText("Status: erorr open file");
                        panel.setBackground(Color.LIGHT_GRAY);                    
                        main_btn.setText("Start parse old file");
                    }
                    return;                        
                }
                if (panel.getBackground() == Color.LIGHT_GRAY) {
                    content.get(0).setText("Status: ready parse file");
                    panel.setBackground(Color.GREEN);
                    main_btn.setText("Start parse");
                }
                selected_file = null;
                selected_file = tmp_array_path; 
            }
        });
        
         main_btn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {          
            switch (condt) {
                case 1: case 3:
                    File tmp;
                    ArrayList<File> tmp_array_path;
                    if (wind.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) 
                        return;
                    tmp = wind.getSelectedFile();
                    if ((tmp_array_path = Prs.checkDirectory(tmp, content)) == null) {
                        if (panel.getBackground() != Color.LIGHT_GRAY) {                    
                            content.get(0).setText("Status: erorr open file");
                            panel.setBackground(Color.LIGHT_GRAY); 
                            if (condt == 1)
                                main_btn.setText("Change Action.c file");
                            else {
                                panel.add(chan_btn);
                                if (main_btn.getText().equals("Open new Action.c file")) {
                                    main_btn.setText("   Start parse old file   ");
                                    condt = 2;
                                }
                            }                            
                        }
                        break;                        
                    }                    
                    content.get(0).setText("Status: ready parse file");
                    panel.setBackground(Color.GREEN);
                    main_btn.setText("Start parse");
                    selected_file = null;
                    selected_file = tmp_array_path;                    
                    if (!chan_btn.isShowing());
                        panel.add(chan_btn);
                    if (condt == 1) {                        
                        for (int i = 1; i < content.size(); i++) {
                            content.get(i).setFont(new Font("Arial", Font.PLAIN, 12));
                            content.get(i).setBounds(15, 15 * i, 180, 10); 
                            
                            panel.add(content.get(i));
                        }                    

                        open_act.setText("Open faile to NotePad");
                        open_act.setBounds(200, 10, 160, 30);
                        panel.add(open_act);
                        panel.setBackground(Color.GREEN); 
                    }
                    condt = 2;
                    break;
                    
                case 2:                    
                    main_btn.setText("Open new Action.c file");
                    content.get(0).setText("Status: Was start parse, ready for new task");
                    panel.setBackground(Color.WHITE);
                    final Prs pars = new Prs(); 
                    final WindowProgress wind_progress = new WindowProgress(pars);                    
                    pars.addPropertyChangeListener(wind_progress);
                    pars.execute(selected_file, wind_progress);
                    panel.remove(chan_btn);                
                    condt = 3;
                    break;                
                }
            }
        });
         root.add(panel);
         root.setLocation(400, 300);
         root.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
         root.setSize(500, 250);
         root.setResizable(false);
         root.setVisible(true);
    }

    public static void main(String[] args) {
        new ParserOracle2Tier();
    }
}