package action;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.TypedActionHandler;
import com.intellij.openapi.project.Project;
import config.SettingConfig;
import gui.LancerMainToolWindow;
import handler.RecommendSnippetHandler;
import org.jetbrains.annotations.NotNull;
import service.MainToolWindowService;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class LAMPTypedHandler implements TypedActionHandler {
    private SettingConfig config = SettingConfig.getInstance();
    private TypedActionHandler oldHandler;
    private RecommendSnippetHandler recommendSnippetHandler = new RecommendSnippetHandler();

    //    public static final Character[] triggerChars = {
//            '\n', ' ', '(', ')', '{', '}', '.', ';', '@', '+', '[', ']', '=', ',', '=', ':', '<', '>', '!'
//    };
    public static final Character[] triggerChars = {
            '{', ';', '\n'
    };

    public static final Set<Character> TRIGGER_SET = new HashSet<>(Arrays.asList(triggerChars));

    @Override
    public void execute(@NotNull Editor editor, char charTyped, @NotNull DataContext dataContext) {
        if (oldHandler != null)
            oldHandler.execute(editor, charTyped, dataContext);
        if (!TRIGGER_SET.contains(charTyped)) {
            return;
        }
        if (config.isAUTO_TRIGGER()) {
            new Thread(() -> {
                Project project = editor.getProject();
                MainToolWindowService mainToolWindowService = ServiceManager.getService(project, MainToolWindowService.class);
                LancerMainToolWindow toolWindow = mainToolWindowService.getToolWindow();

                final Document doc = editor.getDocument();
                recommendSnippetHandler.execute(toolWindow, editor, doc);
            }).start();
        }
    }

    public void setOldHandler(TypedActionHandler oldHandler) {
        this.oldHandler = oldHandler;
    }
}
