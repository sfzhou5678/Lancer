package utils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class FileUtil {
    public static List<String> readLines(String filePath) {
        List<String> lines = new ArrayList<>();

        File file = new File(filePath);
        BufferedReader bufferedReader;
        try {
            bufferedReader = new BufferedReader(new FileReader(file));
            String str = null;
            while ((str = bufferedReader.readLine()) != null) {
                lines.add(str);
            }
            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lines;
    }

    public static boolean writeFileByLines(List<String> lines, String outputPath) {
        try {
            FileWriter filew = new FileWriter(outputPath);
            for (String line : lines) {
                filew.write(line + '\n');
            }
            filew.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}
