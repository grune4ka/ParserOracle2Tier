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
    private boolean is_open_step = false;
    private boolean is_open_uc = false;
    private boolean is_exec = true;
    
    @Override
    protected Void doInBackground() {
        
        File result_txt = new File(path.get(path.size() - 1).getPath() + "\\SQL_Statement.txt");
        File method_txt = new File(path.get(path.size() - 1).getPath() + "\\Methods.java");
        File busines_txt = new File(path.get(path.size() - 1).getPath() + "\\BusinesMethod.java");
        
        try (BufferedReader reader_action = new BufferedReader(new InputStreamReader(new FileInputStream(path.get(0).getPath()), "windows-1251"));
                BufferedReader reader_log = new BufferedReader(new InputStreamReader(new FileInputStream(path.get(1).getPath()), "windows-1251"));
                BufferedReader reader_grd = new BufferedReader(new InputStreamReader(new FileInputStream(path.get(2).getPath()), "windows-1251"));
                FileWriter result_file = new FileWriter(result_txt);
                FileWriter method_file = new FileWriter(method_txt);
                FileWriter busines_file = new FileWriter(busines_txt)) {
            String line;            
            int i = 0; 
            HashMap<String, StringBuilder> quer = new HashMap<>();
            HashMap<String, String> user_param = new HashMap<>();
            double procent = path.get(1).length(), lenght = 0.;            
            busines_file.write("\npublic class Uc {\n\n\tprivate final Step cst;\n\n" + 
                    "\tpublic Uc(Connection c) {\n\t\tcst  = new Step(c)\n\t}\n\n");
            method_file.write("\npublic class Step {\n\n" + 
                    "\tprivate final Connection connection;\n\n" + 
                    "\tpublic Step(Connection c) {\n\t\tconnection = c;\n\t}\n\n");
            while ((line = reader_log.readLine()) != null) {
                if (is_exec) {
                    parsAction(reader_action, busines_file, method_file, user_param);
                    is_exec = false;
                }
                after_empty_execute_statement:
                if (line.contains("OCIStmtPrepare")) {
                    StringBuilder acum_statem = new StringBuilder("\t\t\t\t\t" + line.substring(109, line.length()));                    
                    String key = line.substring(80, 98); 
                    result_file.write(line.substring(110, line.length()));
                    while (!(line = reader_log.readLine()).contains("[OCI_SUCCESS]")) {
                        acum_statem.append("\" + \r\n\t\t\t\t\t\"");
                        acum_statem.append(line);
                        result_file.write(System.lineSeparator() + line); 
                    }       
                    result_file.write(System.lineSeparator() + System.lineSeparator() + ++i + ")" + System.lineSeparator());
                    quer.put(key, acum_statem);                    
                }
                else if (line.contains("OCIStmtExecute")) {                    
                    is_exec = true;
                    int j = 0; 
                    String statement = quer.get(line.substring(90, 108)).toString().replaceAll("(?<=[^\\t\\t\\t])\"(?!( \\+))", "\\\\\"");
                    String name_variable_statement;
                    count_variable++;
                    if (statement.indexOf("begin") != -1) { 
                        name_variable_statement = "clSt_" + count_variable;
                        method_file.write("\t\tCallableStatement " + 
                                name_variable_statement + 
                                " = connection.prepareCall(\n" +
                                statement +
                                "\");\n"); 
                    }
                    else {
                        name_variable_statement = "prSt_" + count_variable;
                        method_file.write("\t\tPreparedStatement " + 
                                name_variable_statement + 
                                " = connection.prepareStatement(\n" +
                                statement +
                                "\");\n"); 
                    }
                    String pr_name = null;
                    String rs_name = null;
                    HashMap<String, String> tmp_prm = null;
                    short flag_result_set = 0;
                    while (!(line = reader_log.readLine()).contains("AFTER") && !line.contains("row(s) fetched")) {                        
                        if (line.isEmpty())
                            continue;
                        else if (line.contains("[LRD")) {                            
                            printExecuteAndFetch(flag_result_set, count_variable, name_variable_statement, rs_name, method_file);
                            break after_empty_execute_statement;
                        }
                        else if (++j > 3) {
                            if (tmp_prm == null) 
                                tmp_prm = new HashMap<String, String>();
                            
                            String tmp = ""; 
                            int index = 0;
                            pr_name = line.substring(3, line.indexOf('=')).trim();
                            while (pr_name.compareToIgnoreCase(tmp = statement.substring(index, index + pr_name.length())) != 0) 
                                index = statement.indexOf(":", index) + 1;                                                                            
                            pr_name = tmp; 
                            if (statement.matches("[\\s\\S]*" + pr_name + "\\s*:=[\\s\\S]*")) {
                                flag_result_set = 1;
                                if (statement.contains("select"))
                                    flag_result_set = 2;
                                rs_name = pr_name;
                                tmp_prm.put(pr_name, "\t\t" + name_variable_statement + ".registerOutParameter(\"" + pr_name + "\", OracleTypes.CURSOR);\r\n");
                                continue;
                            }
                            String value = line.substring(line.indexOf('=') + 1);
                            if (value.matches("^\\d*\\d$"))
                                tmp_prm.put(pr_name, "\t\t" + name_variable_statement + ".setLong(\"" + pr_name + "\", " + value + "L);\r\n");
                            else
                                tmp_prm.put(pr_name, "\t\t" + name_variable_statement + ".setString(\"" + pr_name + "\", \"" + value + "\");\r\n");
                        }                        
                    }
                    if (tmp_prm != null) {
                        if (tmp_prm.size() > 1) {
                            TreeMap<Integer, String> sort_param = new TreeMap<>();
                            for (Map.Entry<String, String> v : tmp_prm.entrySet()) 
                                sort_param.put(statement.indexOf(":" + v.getKey()), v.getKey());
                            for (Map.Entry<Integer, String> v : sort_param.entrySet())
                                method_file.write(tmp_prm.get(v.getValue()));
                        }
                        else 
                            method_file.write(tmp_prm.get(pr_name));
                    }
                    printExecuteAndFetch(flag_result_set, count_variable, name_variable_statement, rs_name, method_file);
                }                
                lenght += (line.length() + 3);
                if(isCancelled())
                    return null;
                setProgress((int)((lenght / procent) * 100));              
            }
            busines_file.write("\n\t}\n}");            
            method_file.write("\n\t}\n\n\tprivate void fetchResultSet(ArrayList<ParamsWithCoordinates> arr_prm, ResultSet result) throws SQLException {\n\n" +
                    "\t\t/* do something fetching */" +
                    "\n\t}\n\n\tprivate class ParamsWithCoordinates {\n\n" +
                    "\t\tpublic final String Name;\n" +
                    "\t\tpublic final int X;\n" +
                    "\t\tpublic final int Y;\n\n" +
                    "\t\tpublic ParamsWithCoordinates(String Name, int X, int Y) {\n" +
                    "\t\t\tthis.Name = Name;\n" +
                    "\t\t\tthis.X = X;\n" +
                    "\t\t\tthis.Y = Y;\n\t\t}\n\t}" +
                    "\n}");            
            setProgress(100);
            Thread.sleep(100);
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
    
    private void printExecuteAndFetch(short flag, int count_variable, String name_variable_statement, String rs_name, FileWriter method_file) {
        
        try {  
            method_file.write("\t\t" + name_variable_statement + ".execute();\r\n");
            if (flag > 0) {                
                method_file.write("\t\tResultSet resProcCur_" + count_variable + " = (ResultSet)" +
                        name_variable_statement +
                        ".getObject(\"" +
                        rs_name + "\");\r\n\t\tfetchResultSet(null, " + 
                        "resProcCur_" + count_variable +
                        ");\r\n");
            }
            if (flag != 1) {
                method_file.write("\t\tResultSet resSet_" +
                        count_variable + " = " +
                        name_variable_statement +
                        ".getResultSet();\r\n\t\tfetchResultSet(null, " + 
                        "resSet_" + count_variable +
                        ");\r\n");
            }            
            method_file.write("\r\n");          
        } catch (IOException e) { e.printStackTrace(); }
    }
    
    private boolean parsAction(BufferedReader reader_action, FileWriter busines_file, FileWriter method_file, HashMap<String, String> user_param) throws IOException {
        
        String line = null;
        String tmp;   
        
        try {
            while ((line = reader_action.readLine()) != null && !line.contains("lrd_ora8_exec(")) {
                if (line.contains("/*")) {                   
                    do {                        
                        if (line.matches(".*uc_\\d{1,3}.*")) { 
                            tmp = line.substring(line.indexOf("uc_")).trim();                            
                            if (tmp.endsWith("*/"))
                                tmp = tmp.substring(0 , tmp.length() - 3);
                            validateParam(tmp);
                            busines_file.write((is_open_uc ? "\t} \n\n\tpublic void" : "\tpublic void ") + tmp + "() throws SQLException  {");
                            if (!is_open_uc)
                                is_open_uc = true;
                        }
                        else if (line.matches(".*step_\\d{1,3}.*")) {
                            tmp = line.substring(line.indexOf("step_")).trim();                            
                            if (tmp.endsWith("*/"))
                                tmp = tmp.substring(0 , tmp.length() - 3);
                            validateParam(tmp);
                            busines_file.write("\n\n\t\tcst." + tmp + "();");
                            method_file.write((is_open_step ? "\t} \n\n\tpublic void " : "\tpublic void ") + tmp + "() throws SQLException {\n\n");
                            if (!is_open_step)
                                is_open_step = true;
                        }
                        else if (line.matches(".*name=.*")) {
                            String name;
                            String value;
                            if (!line.matches(".*value=.*"))
                                throw new Exception("Not valid user parametr" + line);                            
                            int index;
                            if ((index = line.indexOf(' ', line.indexOf("name="))) < 0)
                                throw new Exception("Not valid user parametr" + line);    
                            name = line.substring(line.indexOf("name=") + 5, index);
                            if (name.contains("value="))
                                throw new Exception("Not valid user parametr" + name); 
                            validateParam(name);                            
                            index = line.indexOf("value=") + 6;
                            value = line.substring(index);
                            if (value.contains("name="))
                                throw new Exception("Not valid user parametr" + value);
                            if (name.endsWith("*/"))
                                name = name.substring(0 , name.length() - 3);
                            if (value.endsWith("*/"))
                                value = value.substring(0 , value.length() - 3);                                                        
                            if (value.matches("^\\d*\\d$"))
                                method_file.write("\t\tLong " + name + " = " + value + "L;\n");
                            else
                                method_file.write("\t\tString " + name + " = \"" + value + "\";\n");                            
                        }
                        if (line.contains("*/"))
                            break;
                    } while ((line = reader_action.readLine()) != null); 
                }
            }
        } catch (Exception e) {e.printStackTrace();}
        
        return line != null ? true : false;
    }
    
    private void validateParam(String str) throws Exception {
        if (str.matches(".*\\W.*"))
            throw new Exception("Not valid user comment" + str);
    }
}