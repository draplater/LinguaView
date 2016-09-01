package LinguaView;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.logging.Level;

class FileHandler implements HttpHandler {
    private final String path;
    private FileNameMap fileNameMap = URLConnection.getFileNameMap();

    public FileHandler(String path) {
        this.path = path;
    }

    public void handle(HttpExchange t) throws IOException {
        String targetPath = t.getRequestURI().getPath();

        // Check if file exists
        File fileFolder = new File(getClass().getResource(this.path).getFile());
        File targetFile = new File(fileFolder, targetPath.replace('/', File.separatorChar));

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy.MM.dd-hh:mm:ss z", Locale.getDefault());

        if (targetFile.exists()) {
            // If it exists and it's a file, serve it
            int bufLen = 10000 * 1024;
            byte[] buf = new byte[bufLen];
            int len = 0;
            Headers responseHeaders = t.getResponseHeaders();

            String mimeType = fileNameMap.getContentTypeFor(targetFile.toURI().toURL().toString());

            if (targetFile.isDirectory() || targetFile.getName().endsWith(".html") || targetFile.getName().endsWith(".htm")) {
                mimeType = "text/html; charset=UTF-8";
            } else {
                mimeType = "text/plain; charset=UTF-8";
            }

            responseHeaders.set("Content-Type", mimeType);
            // logger.log(Level.ALL, "Server Directory Listing:" + targetFile.getAbsolutePath());
            // accessLogger.log(Level.ALL, "Server Directory Listing:" + targetFile.getAbsolutePath());

            if (targetFile.isFile()) {
                t.sendResponseHeaders(200, targetFile.length());
                FileInputStream fileIn = new FileInputStream(targetFile);
                OutputStream out = t.getResponseBody();

                while ((len = fileIn.read(buf, 0, bufLen)) != -1) {
                    out.write(buf, 0, len);
                }

                out.close();
                fileIn.close();
            } else if (targetFile.isDirectory()) {
                File files[] = targetFile.listFiles();
                StringBuffer sb = new StringBuffer();
                sb.append("\n<html>");
                sb.append("\n<head>");
                sb.append("\n<style>");
                sb.append("\n</style>");
                sb.append("\n<title>List of files/dirs under /scratch/mseelam/view_storage/mseelam_otd1/otd_test/./work</title>");
                sb.append("\n</head>");
                sb.append("\n<body>");
                sb.append("\n<div class=\"datagrid\">");
                sb.append("\n<table>");
                sb.append("\n<caption>Directory Listing</caption>");
                sb.append("\n<thead>");
                sb.append("\n	<tr>");
                sb.append("\n		<th>File</th>");
                sb.append("\n		<th>Dir ?</th>");
                sb.append("\n		<th>Size</th>");
                sb.append("\n		<th>Date</th>");
                sb.append("\n	</tr>");
                sb.append("\n</thead>");
                sb.append("\n<tfoot>");
                sb.append("\n	<tr>");
                sb.append("\n		<th>File</th>");
                sb.append("\n		<th>Dir ?</th>");
                sb.append("\n		<th>Size</th>");
                sb.append("\n		<th>Date</th>");
                sb.append("\n	</tr>");
                sb.append("\n</tfoot>");
                sb.append("\n<tbody>");

                int numberOfFiles = files.length;

                for (int i = 0; i < numberOfFiles; i++) {
                    //System.out.println("In Work:" + f.getAbsolutePath());
                    if (i % 2 == 0) sb.append("\n\t<tr class='alt'>");
                    else sb.append("\n\t<tr>");
                    if (files[i].isDirectory()) sb.append("\n\t\t<td><a href='" + targetPath + files[i].getName() + "/'>" + files[i].getName() + "</a></td>" +
                            "<td>Y</td>" + "<td>" + files[i].length() +
                            "</td>" + "<td>" + formatter.format(new Date(files[i].lastModified())) + "</td>\n\t</tr>");
                    else sb.append("\n\t\t<td><a href='" + targetPath + files[i].getName() + "'>" + files[i].getName() + "</a></td>" +
                            "<td> </td>" + "<td>" + files[i].length() +
                            "</td>" + "<td>" + formatter.format(new Date(files[i].lastModified())) + "</td>\n\t</tr>");
                }
                sb.append("\n</tbody>");
                sb.append("\n</table>");
                sb.append("\n</div>");
                sb.append("\n</body>");
                sb.append("\n</html>");

                t.sendResponseHeaders(200, sb.length());
                OutputStream out = t.getResponseBody();
                out.write(sb.toString().getBytes());
                out.close();
            }
        } else {
            // If it doesn't exist, send error
            String message = "404 Not Found " + targetFile.getAbsolutePath();
            t.sendResponseHeaders(404, 0);
            OutputStream out = t.getResponseBody();
            out.write(message.getBytes());
            out.close();
        }
    }
}
