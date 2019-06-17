package action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import gui.LancerMainToolWindow;
import handler.RecommendSnippetHandler;
import org.jetbrains.annotations.NotNull;
import service.MainToolWindowService;

public class InvokeLancerAction extends AnAction {
    private RecommendSnippetHandler handler = new RecommendSnippetHandler();


    @Override
    public void actionPerformed(AnActionEvent e) {
        new Thread(() -> {
            // get get registered service
            Project project = e.getData(PlatformDataKeys.PROJECT);
            MainToolWindowService mainToolWindowService = ServiceManager.getService(project, MainToolWindowService.class);
            LancerMainToolWindow toolWindow = mainToolWindowService.getToolWindow();

            Editor editor = e.getData(PlatformDataKeys.EDITOR);
            final Document doc = editor.getDocument();

            handler.execute(toolWindow, editor, doc);
        }).start();
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Editor editor = e.getData(PlatformDataKeys.EDITOR);
        if (editor != null)
            e.getPresentation().setEnabled(true);
        else
            e.getPresentation().setEnabled(false);
    }
}
