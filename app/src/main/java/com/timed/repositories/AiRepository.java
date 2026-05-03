package com.timed.repositories;

import com.google.gson.Gson;
import com.timed.Features.AI.AiScheduleRequest;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import com.timed.BuildConfig;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AiRepository {
    private static final String GEMINI_API_KEY = BuildConfig.GEMINI_API_KEY;
    private final OkHttpClient client;
    private final Gson gson;

    private final String systemPrompt = "You are a scheduling assistant. Extract scheduling constraints from the user's text and return ONLY a raw JSON object. Do not include markdown formatting, backticks, or conversational text. " +
            "Use this exact schema: " +
            "{ \"title\": \"String (A short title)\", " +
            "\"durationMs\": \"Number (Duration in milliseconds. Default to 3600000)\", " +
            "\"dayKeyword\": \"String (Must be 'Today', 'Tomorrow', 'Next Monday', etc. Default to 'Tomorrow')\", " +
            "\"timeBound\": \"String (Must be 'Any time', 'Morning', 'Afternoon', 'Evening'. Default to 'Any time')\", " +
            "\"exactTime\": \"String (If an exact time is requested like '10:00 PM', output it in 24-hour HH:mm format like '22:00'. Otherwise null)\" }";

    public interface AiExtractionCallback {
        void onSuccess(AiScheduleRequest extractedData);
        void onError(Exception e);
    }

    public AiRepository() {
        this.client = new OkHttpClient();
        this.gson = new Gson();
    }

    public void extractScheduleFromText(String userText, AiExtractionCallback callback) {
        JSONObject payload = new JSONObject();
        try {
            JSONObject config = new JSONObject();
            config.put("responseMimeType", "application/json");
            payload.put("generationConfig", config);

            JSONObject systemInstruction = new JSONObject();
            JSONArray systemParts = new JSONArray();
            JSONObject systemPart = new JSONObject();
            systemPart.put("text", systemPrompt);
            systemParts.put(systemPart);
            systemInstruction.put("parts", systemParts);
            payload.put("systemInstruction", systemInstruction);

            JSONArray contents = new JSONArray();
            JSONObject contentObj = new JSONObject();
            JSONArray userParts = new JSONArray();
            JSONObject userPart = new JSONObject();
            userPart.put("text", userText);
            userParts.put(userPart);
            contentObj.put("parts", userParts);
            contents.put(contentObj);

            payload.put("contents", contents);
        } catch (Exception e) {
            callback.onError(e);
            return;
        }

        RequestBody body = RequestBody.create(payload.toString(), MediaType.get("application/json; charset=utf-8"));

        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + GEMINI_API_KEY;

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onError(new Exception("API Error: " + response.code()));
                    return;
                }

                try {
                    String responseData = response.body().string();
                    JSONObject jsonResponse = new JSONObject(responseData);

                    String aiMessage = jsonResponse.getJSONArray("candidates")
                            .getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONArray("parts")
                            .getJSONObject(0)
                            .getString("text");

                    AiScheduleRequest extractedData = gson.fromJson(aiMessage, AiScheduleRequest.class);
                    callback.onSuccess(extractedData);

                } catch (Exception e) {
                    callback.onError(new Exception("Failed to parse AI response", e));
                }
            }
        });
    }
}
