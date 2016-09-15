package codes.ai.intention;

import codes.ai.websocket.WsClient;
import com.google.gson.Gson;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.event.CaretEvent;
import com.intellij.openapi.editor.event.CaretListener;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.util.Collection;
import java.util.stream.Collectors;

/** @author xuy. Copyright (c) Ai.codes */
public class IntentionCaretListener implements CaretListener {
  private WsClient wsWsClient = WsClient.getInstance();
  private Gson gson = new Gson();

  @Override
  public void caretPositionChanged(CaretEvent caretEvent) {
    Project project = caretEvent.getEditor().getProject();
    /// Skip parsing event if caret stays in the same line.
    if (caretEvent.getOldPosition().line == caretEvent.getNewPosition().line) {
      return;
    }
    if (project != null) {
      PsiDocumentManager manager = PsiDocumentManager.getInstance(project);
      PsiFile file = manager.getPsiFile(caretEvent.getEditor().getDocument());
      Caret c = caretEvent.getCaret();
      if (c != null && file != null) {
        PsiElement element = file.findElementAt(c.getOffset());
        PsiMethod method = PsiTreeUtil.getParentOfType(element, PsiMethod.class);
        if (method != null) {
          IntentionPayload payload = new IntentionPayload();
          payload.methodName = method.getName();

          Collection<PsiComment> comments =
              PsiTreeUtil.findChildrenOfType(method, PsiComment.class);
          payload.intentions.addAll(
              comments
                  .stream()
                  .filter(comment -> comment.getText().startsWith("///"))
                  .map(comment -> comment.getText().substring(3).trim())
                  .collect(Collectors.toList()));
          wsWsClient.sendMessage(gson.toJson(payload));
        }
      }
    }
  }

  @Override
  public void caretAdded(CaretEvent caretEvent) {
    System.out.println("Added");
  }

  @Override
  public void caretRemoved(CaretEvent caretEvent) {
    // do nothing
    System.out.println("Removed");
  }

  // Poor man's websocket
  // TODO: get this to JSON and send to express via websocket.
  private void sendMessage(String message) {
    CloseableHttpClient httpClient = HttpClients.createDefault();
    HttpGet get = new HttpGet("http://127.0.0.1:26337/ping/" + message);
    get.setConfig(
        RequestConfig.custom()
            .setConnectTimeout(10)
            .setConnectionRequestTimeout(10)
            .setSocketTimeout(10)
            .build());
    CloseableHttpResponse response = null;
    try {
      response = httpClient.execute(get);
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      try {
        if (response != null) response.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
}
