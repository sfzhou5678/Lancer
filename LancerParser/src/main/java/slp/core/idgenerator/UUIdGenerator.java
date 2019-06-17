package slp.core.idgenerator;

import slp.core.infos.FileInfo;
import slp.core.infos.MethodInfo;

import java.util.UUID;

public class UUIdGenerator implements IdGenerator {
    @Override
    public String getId(FileInfo fileInfo, MethodInfo info) {
        return UUID.randomUUID().toString();
    }
}
