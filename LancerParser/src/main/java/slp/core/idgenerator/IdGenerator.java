package slp.core.idgenerator;

import slp.core.infos.FileInfo;
import slp.core.infos.MethodInfo;

public interface IdGenerator {
    public String getId(FileInfo fileInfo, MethodInfo methodInfo);
}
