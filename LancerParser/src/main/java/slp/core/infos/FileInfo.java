package slp.core.infos;

import java.util.List;

public class FileInfo {
    private String fileId;
    private String fileName;
    private String baseFolderPath;
    private String relativeFilePath;
    private String affiliatedProject = "";   // project name

    private List<String> tokens;   // the same as the old raw tokens
    private List<Integer> commentsTokenIndex;   // set tokens[ idx in commentsTokenIndex] ="" can remove comments
    private List<List<String>> lineTokens;


    public FileInfo(String fileName, String baseFolderPath, String relativeFilePath) {
        this.fileName = fileName;
        this.baseFolderPath = baseFolderPath;
        this.relativeFilePath = relativeFilePath;
    }

    public FileInfo(String fileId, String baseFolderPath, String relativeFilePath, String affiliatedProject) {
        this.fileId = fileId;
        this.baseFolderPath = baseFolderPath;
        this.relativeFilePath = relativeFilePath;

        this.affiliatedProject = affiliatedProject;
    }

    public FileInfo(String fileId, String fileName, String baseFolderPath, String relativeFilePath, String affiliatedProject,
                    List<String> tokens, List<Integer> commentsTokenIndex, List<List<String>> lineTokens) {
        this.fileId = fileId;
        this.fileName = fileName;
        this.baseFolderPath = baseFolderPath;
        this.relativeFilePath = relativeFilePath;
        this.affiliatedProject = affiliatedProject;
        this.tokens = tokens;
        this.commentsTokenIndex = commentsTokenIndex;
        this.lineTokens = lineTokens;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public void setRelativeFilePath(String relativeFilePath) {
        this.relativeFilePath = relativeFilePath;
    }

    public void setAffiliatedProject(String affiliatedProject) {
        this.affiliatedProject = affiliatedProject;
    }

    public void setTokens(List<String> tokens) {
        this.tokens = tokens;
    }

    public void setLineTokens(List<List<String>> lineTokens) {
        this.lineTokens = lineTokens;
    }

    public String getFileId() {
        return fileId;
    }

    public String getRelativeFilePath() {
        return relativeFilePath;
    }

    public String getAffiliatedProject() {
        return affiliatedProject;
    }

    public List<String> getTokens() {
        return tokens;
    }

    public List<List<String>> getLineTokens() {
        return lineTokens;
    }

    public List<Integer> getCommentsTokenIndex() {
        return commentsTokenIndex;
    }

    public void setCommentsTokenIndex(List<Integer> commentsTokenIndex) {
        this.commentsTokenIndex = commentsTokenIndex;
    }

    public String getBaseFolderPath() {
        return baseFolderPath;
    }

    public void setBaseFolderPath(String baseFolderPath) {
        this.baseFolderPath = baseFolderPath;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}
