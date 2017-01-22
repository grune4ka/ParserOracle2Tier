
package parseroracle2tier;


import java.io.BufferedReader;
import java.io.File;

import java.io.FileWriter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import javax.swing.*;
import java.util.*;




public class Prs extends SwingWorker<Void, Void> {

    private ArrayList<File> path = null;
    private WindowProgress wind_progress = null;
    
    @Override
    protected Void doInBackground() {
        
        File result_txt = new File(path.get(path.size() - 1).getPath() + "\\SQL_Statement.txt");
        File method_txt = new File(path.get(path.size() - 1).getPath() + "\\Methods.java");
        try (BufferedReader reader_action = new BufferedReader(new InputStreamReader(new FileInputStream(path.get(0).getPath()), "windows-1251"));
                BufferedReader reader_log = new BufferedReader(new InputStreamReader(new FileInputStream(path.get(1).getPath()), "windows-1251"));
                BufferedReader reader_grd = new BufferedReader(new InputStreamReader(new FileInputStream(path.get(2).getPath()), "windows-1251"));
                FileWriter result_file = new FileWriter(result_txt);
                FileWriter method_file = new FileWriter(method_txt)) {
            String line;            
            int i = 0; 
            HashMap<String, String> quer = new HashMap<>();
            double procent = path.get(1).length(), lenght = 0.;
            while ((line = reader_log.readLine()) != null) {               
                if (line.contains("OCIStmtPrepare")) {                   
                    result_file.write(line.substring(110, line.length()));
                    while (!(line = reader_log.readLine()).contains("[OCI_SUCCESS]")) 
                        result_file.write(System.lineSeparator() + line);                    
                    result_file.write(System.lineSeparator() + System.lineSeparator() + ++i + ")" + System.lineSeparator());
                }
                lenght += (line.length() + 3);
                if(isCancelled())
                    return null;
                setProgress((int)((lenght / procent) * 100)); 
                Thread.sleep(1);
            }
            setProgress(100);
            Thread.sleep(1000);
        } catch (IOException e) {}
        catch(InterruptedException e) {}
        
        path.add(result_txt);
                            
        return null;
    }
                        
    @Override
    protected void done() {
        
        int i = path.size() - 1;
        wind_progress.succededWnpr(path.get(i));
        path.remove(i);
                            
    }
    
    public void execute(ArrayList<File> p, WindowProgress w) {
        path = p;
        wind_progress = w;
        execute();
    }
    
    
    
    public static ArrayList<File> checkDirectory(File path, ArrayList<JLabel> content) {
        ArrayList<File> array_paths = new ArrayList<>();
        String name_file = path.getName();
        
        String str_path = path.getPath().replace(name_file, "");
        String name_grd = str_path.substring(str_path.substring(0, str_path.length() - 2).lastIndexOf('\\') + 1, str_path.length() - 1) + ".grd";
              
        if (name_file.equals("Action.c") || name_file.equals("vuser_init.c") || name_file.equals("vuser_end.c")) {
            array_paths.add(path);
            array_paths.add(new File(str_path + "data\\RecordingLog.txt"));            
            array_paths.add(new File(str_path + name_grd));            
            for (int i = 1; i < array_paths.size(); i++) 
                if (!array_paths.get(i).exists())
                    return null;
            array_paths.add(new File(str_path + "ParseJavaFiles"));
            if (!array_paths.get(array_paths.size() - 1).exists()) 
                array_paths.get(array_paths.size() - 1).mkdir();            
            if (content.size() == 1)
                for (int i = 0; i < array_paths.size() - 1; i++) 
                    content.add(new JLabel(array_paths.get(i).getName()));
            if (!content.get(1).getText().equals(name_file)) {
                if (!content.get(3).getText().equals(name_grd)) 
                    content.get(3).setText(name_grd);
                content.get(1).setText(name_file);
            }
        }           
        else 
            return null;
        return array_paths;
    }   
}




