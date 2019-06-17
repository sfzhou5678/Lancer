package slp.core.idgenerator;

import db.DBManager;
import slp.core.infos.FileInfo;
import slp.core.infos.MethodInfo;
import org.apache.commons.codec.digest.DigestUtils;

import java.sql.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BCBIdGenerator implements IdGenerator {

    private DBManager db;

    public BCBIdGenerator(DBManager db) {
        this.db = db;
    }

    private String regex = "(default|selected|sample)";

    private String getLastMatchedStr(String inputString, String regex) {
        String returnString = "";
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(inputString);
        while (m.find()) {
            returnString = m.group();
        }
        return returnString;
    }

    /**
     * Select (fileName, type, project, startLine, endLine) from db to locate a specific snippet
     * if failed, return a uuid
     *
     * @param fileInfo
     * @return
     */
    @Override
    public String getId(FileInfo fileInfo, MethodInfo methodInfo) {
        try {
            String fileName = fileInfo.getFileName();
            String type = getLastMatchedStr(fileInfo.getBaseFolderPath() + "/" + fileInfo.getRelativeFilePath(), this.regex);


            String sql = String.format("select id from functions\n" +
                    "where name='%s' and type='%s' and startline=%d and endline=%d;", fileName, type, methodInfo.getStartLine(), methodInfo.getEndLine());

            ResultSet res = this.db.executeQuery(sql);

            if (res.next()) {
                String id = res.getString("id");
                return id;
            }
        } catch (Exception ignored) {
        }
        String encodeStr = DigestUtils.md5Hex(
                fileInfo.getRelativeFilePath() + fileInfo.getAffiliatedProject() +
                        methodInfo.getClassName() + methodInfo.getMethodName());
        return encodeStr;
    }
}
