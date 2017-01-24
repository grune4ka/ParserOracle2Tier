
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
    private int count_variable = 0;
    
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
            HashMap<String, StringBuilder> quer = new HashMap<>();
            HashMap<String, String> params_execute = new HashMap<>();
            double procent = path.get(1).length(), lenght = 0.;            
            
            while ((line = reader_log.readLine()) != null) {
                after_empty_execute_statement:
                if (line.contains("OCIStmtPrepare")) {
                    StringBuilder acum_statem = new StringBuilder("\t\t\t" + line.substring(109, line.length()));                    
                    String key = line.substring(80, 98); 
                    result_file.write(line.substring(110, line.length()));
                    while (!(line = reader_log.readLine()).contains("[OCI_SUCCESS]")) {
                        acum_statem.append("\" + \r\n\t\t\t\"");
                        acum_statem.append(line);
                        result_file.write(System.lineSeparator() + line); 
                    }       
                    result_file.write(System.lineSeparator() + System.lineSeparator() + ++i + ")" + System.lineSeparator());
                    quer.put(key, acum_statem);
                    
                }
                else if (line.contains("OCIStmtExecute")) {                    
                    
                    int j = 0; 
                    String statement = quer.get(line.substring(90, 108)).toString().replaceAll("(?<=[^\\t\\t\\t])\"(?!( \\+))", "\\\\\"");
                    String name_variable_statement;
                    count_variable++;
                    if (statement.indexOf("begin") != -1) { 
                        name_variable_statement = "clSt_" + count_variable;
                        method_file.write("CallableStatement " + 
                                name_variable_statement + 
                                " = connection.prepareCall(\n" +
                                statement +
                                "\");\n"); 
                    }
                    else {
                        name_variable_statement = "prSt_" + count_variable;
                        method_file.write("PreparedStatement " + 
                                name_variable_statement + 
                                " = connection.prepareStatement(\n" +
                                statement +
                                "\");\n"); 
                    }
                    String pr_name = null;
                    String rs_name = null;
                    boolean flag_result_set = false;
                    while (!(line = reader_log.readLine()).contains("AFTER") && !line.contains("row(s) fetched")) {                        
                        if (line.isEmpty())
                            continue;
                        else if (line.contains("[LRD")) {
                            if (flag_result_set)
                                printExecuteAndFetch(flag_result_set, count_variable, name_variable_statement, rs_name, method_file);
                            break after_empty_execute_statement;
                        }
                        else if (++j > 3) {                            
                            pr_name = line.substring(3, line.indexOf('=')).trim().toLowerCase();
                            if (statement.matches("[\\s\\S]*" + pr_name + "\\s*:=[\\s\\S]*")) {
                                flag_result_set = true;
                                rs_name = new String(pr_name);
                                method_file.write(name_variable_statement + ".registerOutParameter(\"" + pr_name + "\", OracleTypes.CURSOR);\r\n");
                                continue;
                            }
                            String value = line.substring(line.indexOf('=') + 1);
                            if (value.matches("^\\d*\\d$"))
                                method_file.write(name_variable_statement + ".setLong(\"" + pr_name + "\", " + value + "L);\r\n");
                            else
                                method_file.write(name_variable_statement + ".setString(\"" + pr_name + "\", \"" + value + "\");\r\n");
                        }                        
                    }
                    printExecuteAndFetch(flag_result_set, count_variable, name_variable_statement, rs_name, method_file);
                }                
                lenght += (line.length() + 3);
                if(isCancelled())
                    return null;
                setProgress((int)((lenght / procent) * 100)); 
                //Thread.sleep(1);
            }
            setProgress(100);
            Thread.sleep(1000);
        } catch (IOException e) {e.printStackTrace();}
        catch(InterruptedException e) {e.printStackTrace();}
        catch(Exception e) {e.printStackTrace();}
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
    
    private void printExecuteAndFetch(boolean flag, int count_variable, String name_variable_statement, String rs_name, FileWriter method_file) {
        
        try {
            method_file.write(name_variable_statement + ".execute();\r\nResultSet rsPrCr_" + count_variable);
            if (flag)
                method_file.write(" = (ResultSet)" +
                        name_variable_statement +
                        ".getObject(\"" +
                        rs_name + "\");\r\n\r\n\r\n");
            else
                method_file.write(name_variable_statement + ".execute();\r\n\r\n\r\n");
            
        } catch (IOException e) { e.printStackTrace(); }
    }
}




