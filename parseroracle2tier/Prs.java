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
    private WindowProgress windProgress = null;
    private int count_variable = 0;
    private boolean is_open_step = false;
    private boolean is_open_uc = false;
    private boolean is_exec = true;
    
    @Override
    protected Void doInBackground() {
        
        File resultTxt = new File(path.get(path.size() - 1).getPath() + "\\SQL_Statement.txt");
        File methodTxt = new File(path.get(path.size() - 1).getPath() + "\\Methods.java");
        File businesTxt = new File(path.get(path.size() - 1).getPath() + "\\BusinesMethod.java");
        
        try (BufferedReader readerAction = new BufferedReader(new InputStreamReader(new FileInputStream(path.get(0).getPath()), "windows-1251"));
                BufferedReader readerLog = new BufferedReader(new InputStreamReader(new FileInputStream(path.get(1).getPath()), "windows-1251"));
                BufferedReader readeGrd = new BufferedReader(new InputStreamReader(new FileInputStream(path.get(2).getPath()), "windows-1251"));
                FileWriter resultFile = new FileWriter(resultTxt);
                FileWriter methodFile = new FileWriter(methodTxt);
                FileWriter businesFile = new FileWriter(businesTxt)) {
            String line;  
            
            int i = 0; 
            HashMap<String, StringBuilder> quer = new HashMap<>();
            HashMap<String, String> user_param = new HashMap<>();
            double procent = path.get(1).length(), lenght = 0.; 
            
            paste(businesFile, "##uc##");
            paste(methodFile, "##step##");
    
            while ((line = readerLog.readLine()) != null) {
                if (is_exec) {
                    parsAction(readerAction, businesFile, methodFile, user_param);
                    is_exec = false;
                }
                after_empty_execute_statement:
                if (line.contains("OCIStmtPrepare")) {
                    StringBuilder acum_statem = new StringBuilder("\t\t\t\t\t" + line.substring(109, line.length()));                    
                    String key = line.substring(80, 98); 
                    resultFile.write(line.substring(110, line.length()));
                    while (!(line = readerLog.readLine()).contains("[OCI_SUCCESS]")) {
                        acum_statem.append(" \" + \r\n\t\t\t\t\t\"");
                        acum_statem.append(line);
                        resultFile.write(System.lineSeparator() + line); 
                    }       
                    resultFile.write(System.lineSeparator() + System.lineSeparator() + ++i + ")" + System.lineSeparator());
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
                        methodFile.write("\t\tCallableStatement " + 
                                name_variable_statement + 
                                " = connection.prepareCall(\n" +
                                statement +
                                "\");\n"); 
                    }
                    else {
                        name_variable_statement = "prSt_" + count_variable;
                        methodFile.write("\t\tPreparedStatement " + 
                                name_variable_statement + 
                                " = connection.prepareStatement(\n" +
                                statement +
                                "\");\n"); 
                    }
                    String pr_name = null;
                    String rs_name = null;
                    HashMap<String, String> tmp_prm = null;
                    short flag_result_set = 0;
                    while (!(line = readerLog.readLine()).contains("AFTER") && !line.contains("row(s) fetched")) {                        
                        if (line.isEmpty())
                            continue;
                        else if (line.contains("[LRD")) {                            
                            printExecuteAndFetch(flag_result_set, count_variable, name_variable_statement, rs_name, methodFile);
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
                                methodFile.write(tmp_prm.get(v.getValue()));
                        }
                        else 
                            methodFile.write(tmp_prm.get(pr_name));
                    }
                    printExecuteAndFetch(flag_result_set, count_variable, name_variable_statement, rs_name, methodFile);
                }                
                lenght += (line.length() + 3);
                if(isCancelled())
                    return null;
                setProgress((int)((lenght / procent) * 100));              
            }
            businesFile.write("\n\t}\n}");            
            paste(methodFile, "##fetch##");            
            setProgress(100);
            Thread.sleep(100);
        } catch (IOException e) { ErrorMsg.show(e); }
        catch(InterruptedException e) { ErrorMsg.show(e); }
        catch(Exception e) { ErrorMsg.show(e); }
        path.add(resultTxt);
        return null;
    }
                        
    @Override
    protected void done() {
        int i = path.size() - 1;
        windProgress.succededWnpr(path.get(i));
        path.remove(i);                            
    }
    
    public void execute(ArrayList<File> p, WindowProgress w) {
        path = p;
        windProgress = w;
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
    
    private void printExecuteAndFetch(short flag, int count_variable, String name_variable_statement, String rs_name, FileWriter methodFile) {
        
        try {  
            methodFile.write("\t\t" + name_variable_statement + ".execute();\r\n");
            if (flag > 0) {                
                methodFile.write("\t\tResultSet resProcCur_" + count_variable + " = (ResultSet)" +
                        name_variable_statement +
                        ".getObject(\"" +
                        rs_name + "\");\r\n\t\tfetchResultSet(" + 
                        "resProcCur_" + count_variable +
                        ");\r\n");
            }
            if (flag != 1) {
                methodFile.write("\t\tResultSet resSet_" +
                        count_variable + " = " +
                        name_variable_statement +
                        ".getResultSet();\r\n\t\tfetchResultSet(" + 
                        "resSet_" + count_variable +
                        ");\r\n");
            }            
            methodFile.write("\r\n");          
        } catch (IOException e) { ErrorMsg.show(e); }
    }
    
    private boolean parsAction(BufferedReader readerAction, FileWriter businesFile, FileWriter methodFile, HashMap<String, String> user_param) throws IOException {
        
        String line = null;
        String tmp;   
        
        try {
            while ((line = readerAction.readLine()) != null && !line.contains("lrd_ora8_exec(")) {
                if (line.contains("/*")) {                   
                    do {                        
                        if (line.matches(".*uc_\\d{1,3}.*")) { 
                            tmp = line.substring(line.indexOf("uc_")).trim();                            
                            if (tmp.endsWith("*/"))
                                tmp = tmp.substring(0 , tmp.length() - 2);
                            validateParam(tmp);
                            businesFile.write((is_open_uc ? "\t}\n\n\tpublic void " : "\tpublic void ") + tmp + "() throws SQLException  {");
                            if (!is_open_uc)
                                is_open_uc = true;
                        }
                        else if (line.matches(".*step_\\d{1,3}.*")) {
                            tmp = line.substring(line.indexOf("step_")).trim();                            
                            if (tmp.endsWith("*/"))
                                tmp = tmp.substring(0 , tmp.length() - 2);
                            validateParam(tmp);
                            businesFile.write("\n\n\t\tcst." + tmp + "();\n");
                            methodFile.write((is_open_step ? "\t} \n\n\tpublic void " : "\tpublic void ") + tmp + "() throws SQLException {\n\n");
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
                                methodFile.write("\t\tLong " + name + " = " + value + "L;\n");
                            else
                                methodFile.write("\t\tString " + name + " = \"" + value + "\";\n");                            
                        }
                        if (line.contains("*/"))
                            break;
                    } while ((line = readerAction.readLine()) != null); 
                }
            }
        } catch (Exception e) { ErrorMsg.show(e); }
        
        return line != null;
    }
    
    private void validateParam(String str) throws Exception {
        if (str.matches(".*\\W.*"))
            throw new Exception("Not valid user comment " + str);
    }
    
    public void paste(FileWriter toFile, String whatPaste) throws IOException {
    	
    	BufferedReader resource = new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream("/res/forPaste.txt")));
    	String line;    	
    	while ((line = resource.readLine()) != null)  
            if (line.contains(whatPaste)) 
    		break;    	
        while ((line = resource.readLine())!= null && !line.equals("#########"))
            toFile.write(line + "\n");
        resource.close();		
    }
}