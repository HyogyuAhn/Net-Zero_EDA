package insa.eda.services;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.ai.openai.models.ChatChoice;
import com.azure.ai.openai.models.ChatCompletions;
import com.azure.ai.openai.models.ChatCompletionsOptions;
import com.azure.ai.openai.models.ChatMessage;
import com.azure.ai.openai.models.ChatRole;

import com.azure.core.credential.AzureKeyCredential;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AzureOpenAIService {
    private static final String TAG = "AzureOpenAIService";
    private final String endpoint = "https://insciaifoundary.openai.azure.com";
    private final String apiKey = "2ks4QkppnKmI29BD6xgqVgboOsuGud3vaxqz1bjqRU1lrPz1VegfJQQJ99BFACNns7RXJ3w3AAAAACOGfxS0";
    private final String deploymentName = "gpt-4o";

    private final OpenAIClient client;
    private final Executor executor;
    private final Handler mainHandler;

    public AzureOpenAIService() {
        this.client = new OpenAIClientBuilder()
                .endpoint(endpoint)
                .credential(new AzureKeyCredential(apiKey))
                .buildClient();
        
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public interface TipCallback {
        void onTipGenerated(String tip);
        void onError(String errorMessage);
    }

    public void generateEcoDrivingTip(TipCallback callback) {
        executor.execute(() -> {
            try {
                List<ChatMessage> messages = new ArrayList<>();
                messages.add(new ChatMessage(ChatRole.SYSTEM, 
                        "당신은 에코 드라이빙 어시스턴트 앱을 위한 동기 부여 메시지를 만드는 AI입니다. " +
                        "사용자는 안전하고 부드러운 운전을 통해 환경 보호에 기여하고 있습니다. " +
                        "매번 새롭고 창의적인 격려 문장을 생성해주세요. " +
                        "문장은 짧고 긍정적이어야 합니다. " +
                        "출력은 반드시 아래 JSON 형식이어야 합니다:\n\n" +
                        "{\n  \"message\": \"생성된 격려 문장\"\n}"
                ));
                String[] seasons = {"따뜻한 봄", "활기찬 여름", "상쾌한 가을", "포근한 겨울"};
                String currentSeason = seasons[new java.util.Random().nextInt(seasons.length)];
                String userMessage = String.format("%s 날씨에 어울리는, 운전자를 위한 창의적인 격려 문장을 만들어줘.", currentSeason);

                messages.add(new ChatMessage(ChatRole.USER, userMessage));

                ChatCompletionsOptions options = new ChatCompletionsOptions(messages)
                        .setMaxTokens(100)
                        .setTemperature(1.0)
                        .setTopP(1.0)
                        .setPresencePenalty(0.8);

                ChatCompletions chatCompletions = client.getChatCompletions(deploymentName, options);
                
                for (ChatChoice choice : chatCompletions.getChoices()) {
                    ChatMessage message = choice.getMessage();
                    String content = message.getContent();
                    
                    try {
                        JSONObject jsonResponse = new JSONObject(content);
                        String tip = jsonResponse.getString("message");
                        mainHandler.post(() -> callback.onTipGenerated(tip));
                        return;
                    } catch (JSONException e) {
                        mainHandler.post(() -> callback.onTipGenerated(content));
                    }
                }
                mainHandler.post(() -> callback.onError("No response from AI service"));
                
            } catch (Exception e) {
                Log.e(TAG, "Error generating tip", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }
}
