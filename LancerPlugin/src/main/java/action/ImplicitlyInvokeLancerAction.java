package action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.editor.actionSystem.EditorActionManager;
import com.intellij.openapi.editor.actionSystem.TypedAction;
import com.intellij.openapi.editor.actionSystem.TypedActionHandler;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class ImplicitlyInvokeLancerAction extends AnAction {
    static {
        final EditorActionManager actionManager = EditorActionManager.getInstance();
//        final TypedAction typedAction = actionManager.getTypedAction();
//        typedAction.setupHandler(new LAMPTypedHandler());

        // ref: https://blog.csdn.net/huachao1001/article/details/53885981
        final TypedAction typedAction = actionManager.getTypedAction();
        LAMPTypedHandler handler = new LAMPTypedHandler();
        TypedActionHandler oldHandler = typedAction.setupHandler(handler);  // use oldHandler to do the original task.
        handler.setOldHandler(oldHandler);
    }

    @Override
    public void actionPerformed(AnActionEvent anActionEvent) {
        final Editor editor = anActionEvent.getRequiredData(CommonDataKeys.EDITOR);
        EditorActionManager actionManager = EditorActionManager.getInstance();
        EditorActionHandler actionHandler = actionManager.getActionHandler(IdeActions.ACTION_EDITOR_CLONE_CARET_BELOW);
        actionHandler.execute(editor, editor.getCaretModel().getCurrentCaret(), anActionEvent.getDataContext());
    }

    @Override
    public void update(@NotNull final AnActionEvent anActionEvent) {
        final Project project = anActionEvent.getData(CommonDataKeys.PROJECT);
        final Editor editor = anActionEvent.getData(CommonDataKeys.EDITOR);
        anActionEvent.getPresentation().setVisible((project != null && editor != null && editor.getCaretModel().getCaretCount() > 0));
    }
}
