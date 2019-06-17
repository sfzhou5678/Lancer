import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import gui.LancerMainToolWindow;
import service.MainToolWindowService;

public class LancerToolWindowFactory implements ToolWindowFactory {
    // Create the tool window content.
    public void createToolWindowContent(Project project, ToolWindow toolWindow) {
        LancerMainToolWindow lancerMainToolWindow = new LancerMainToolWindow(toolWindow);

        // get registered service, used for communicating between Action & Tool window
        MainToolWindowService mainToolWindowService = ServiceManager.getService(project, MainToolWindowService.class);
        mainToolWindowService.setToolWindow(lancerMainToolWindow);

        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        Content content = contentFactory.createContent(lancerMainToolWindow.getContent(), "", false);
        toolWindow.getContentManager().addContent(content);
    }
}
