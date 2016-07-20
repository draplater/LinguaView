package LinguaView.UIutils;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Some useful UI functions.
 * @author Chen Yufei
 * @since 16-5-25
 */
public class Utils {
    /**
     * open a fileSelection dialog. We use zenity to choose file in Linux.
     * @param savemode
     * @return
     */
    public static Logger logger = Logger.getLogger("linguaview");
    public static final boolean isDebug =
            java.lang.management.ManagementFactory.getRuntimeMXBean().
            getInputArguments().toString().indexOf("-agentlib:jdwp") > 0;

    static {
        if(isDebug)
            logger.setLevel(Level.INFO);
        else
            logger.setLevel(Level.WARNING);
    }
    public static String fileSelection(boolean savemode) {
        String os = System.getProperty("os.name");
        File input = null;
        String zenity = "zenity --file-selection --title=Open";
        String filestring;
        if ((os.contains("nix") || os.contains("nux")) &&
                new File("/usr/bin/zenity").exists()) {
            //Use native Linux file selection.
            try {
                if (savemode) {
                    zenity ="zenity --file-selection --title=Save --save";
                }
                Process p = Runtime.getRuntime().exec(zenity);
                BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
                StringBuffer sb = new StringBuffer();
                sb.append(br.readLine());
                filestring = sb.toString();
                if (filestring.equals("null")) {
                    return null;
                }
                return filestring;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Use java file chooser.
        final JFileChooser fc = new JFileChooser();
        fc.setCurrentDirectory(new File("."));
        int returnVal;
        if (savemode) {
            returnVal = fc.showSaveDialog(fc);
        } else {
            returnVal = fc.showOpenDialog(fc);
        }
        if(returnVal == 0)
            return fc.getSelectedFile().getAbsolutePath();
        return null;
    }

    /**
     * Implement of String.join in java 8
     * @param with
     * @param list
     * @return
     */
    public static String join(String with, Collection<String> list) {
        StringBuilder s = new StringBuilder();
        for(String j: list) {
            s.append(j);
            s.append(with);
        }
        if(s.length() != 0) {
            s.setLength(s.length() - with.length());
        }
        return s.toString();
    }
}
