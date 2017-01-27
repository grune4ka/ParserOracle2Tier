package parseroracle2tier;

import java.awt.Color;
import java.awt.Font;
import java.beans.*;
import javax.swing.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;

public class WindowProgress implements PropertyChangeListener {
    
    private final JProgressBar pb = new JProgressBar();
    private File result_txt = null; 
    private static int pos = 1;
    private final JPanel panel = new JPanel();
    private final JFrame root = new JFrame();
    private final JButton manag_btn = new JButton();  
    private final JLabel procent = new JLabel("Progress 0%");
    
    public WindowProgress(final SwingWorker<Void, Void> pars_thread) {
        
        ++pos;
        panel.setLayout(null);
        manag_btn.setText("Stop");
        manag_btn.setBounds(380, 80, 100, 30); 
        
        manag_btn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {                 
                if(pb.isShowing()) {                    
                    pars_thread.cancel(true); 
                    root.setVisible(false);
                }
                else {
                    try {                        
                        ProcessBuilder proc = new ProcessBuilder("explorer", result_txt.getPath().substring(0, result_txt.getPath().length() - 18));
                        proc.start();
                    } catch (IOException e) { /* сделать вывод ошибки в окне */ }
                }                
            }            
        });
        pb.setBounds(25, 30, 450, 30);  
        procent.setBounds(25, 80, 150, 30);  
        panel.add(pb);
        panel.add(procent);
        panel.add(manag_btn);
        panel.setBackground(Color.CYAN);
        root.setSize(500, 150);
        root.setTitle("Execute parse");
        root.add(panel);
        if (pos > 10)
            pos = 1;
        root.setLocation(50 * pos, 50 * pos);
        root.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);        
        root.setResizable(false);
        root.setVisible(true);	    
    }
    
    public void succededWnpr(File r_txt) {
        
        result_txt = r_txt;
        pb.setValue(100);        
        for (int i = 0; i < 2; i++) {
            JLabel succes = new JLabel();
            if (i == 0) 
                succes.setText("Parse successful");
            else
                succes.setText("Create file in directory <Folder_Sctipt>\\ParseJavaFiles");
            succes.setBounds(20, 55 + 20 * i, 340, 30);             
            succes.setFont(new Font("Arial", Font.PLAIN, 12));
            panel.add(succes);
        }       
        manag_btn.setText("Open folder");
        panel.remove(pb);
        panel.remove(procent);
        panel.updateUI();
        root.setTitle("End parse");
    }    

    @Override
    public void propertyChange(PropertyChangeEvent e) {
        
        if ("progress".equals(e.getPropertyName())) {
            int val = (int) e.getNewValue();
            pb.setValue(val);
            procent.setText("Progress " + val + "%");
        }
    }
}