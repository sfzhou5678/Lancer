package action;

import gui.LancerMainToolWindow;
import slp.core.infos.MethodInfo;

import java.util.List;

public interface ShowSnippetsCallBack {
    public void showSnippets(List<MethodInfo> methodInfoList, LancerMainToolWindow toolWindow);
}
