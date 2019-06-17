package slp.core.infos;

import java.util.List;

public class InfoCollector {
    private FileInfo fileInfo;
    private List<MethodInfo> methodInfoList;

    public InfoCollector(FileInfo fileInfo, List<MethodInfo> methodInfoList) {
        this.fileInfo = fileInfo;
        this.methodInfoList = methodInfoList;
    }

    public FileInfo getFileInfo() {
        return fileInfo;
    }

    public void setFileInfo(FileInfo fileInfo) {
        this.fileInfo = fileInfo;
    }

    public List<MethodInfo> getMethodInfoList() {
        return methodInfoList;
    }

    public void setMethodInfoList(List<MethodInfo> methodInfoList) {
        this.methodInfoList = methodInfoList;
    }
}
