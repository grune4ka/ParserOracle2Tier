package parseroracle2tier;

import javax.swing.*;

public class ErrorMsg {

    public static void show(Exception e) {
        String nameExc = e.toString();
        nameExc = nameExc.substring(nameExc.lastIndexOf('.') + 1);
        JOptionPane.showMessageDialog(null, e.getStackTrace(), nameExc, JOptionPane.ERROR_MESSAGE);
    }	
}
