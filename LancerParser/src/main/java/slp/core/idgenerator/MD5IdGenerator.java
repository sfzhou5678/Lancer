package slp.core.idgenerator;

import slp.core.infos.FileInfo;
import slp.core.infos.MethodInfo;
import org.apache.commons.codec.digest.DigestUtils;

public class MD5IdGenerator implements IdGenerator {
    @Override
    public String getId(FileInfo fileInfo, MethodInfo methodInfo) {
        String encodeStr = DigestUtils.md5Hex(
                fileInfo.getRelativeFilePath() + fileInfo.getAffiliatedProject() +
                        methodInfo.getClassName() + methodInfo.getMethodName());
        return encodeStr;
    }
}
