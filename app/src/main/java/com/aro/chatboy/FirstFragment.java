package com.aro.chatboy;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceManager;

import com.aro.chatboy.databinding.FragmentFirstBinding;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FirstFragment extends Fragment {

    private FragmentFirstBinding binding;

    StringBuilder builder = new StringBuilder();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentFirstBinding.inflate(inflater, container, false);

        binding.sendButton.setOnClickListener(this::sendMessage);

        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

//        binding.buttonFirst.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                NavHostFragment.findNavController(FirstFragment.this)
//                        .navigate(R.id.action_FirstFragment_to_SecondFragment);
//            }
//        });

        FragmentActivity activity = getActivity();
        if (activity == null) return;
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity);
        String chat_content = sharedPreferences.getString("chat_content", "");
        binding.displayBox.setText(builder.toString());
    }

    @Override
    public void onDestroyView() {

        FragmentActivity activity = getActivity();
        if (activity == null) return;
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("chat_content", builder.toString());
        editor.apply();


        super.onDestroyView();
        binding = null;
    }

    public void sendMessage(View view) {
        // 获取输入内容，并清空输入框
        String msg = binding.inputBox.getText().toString();
        if (msg.isEmpty()) return;

        binding.inputBox.setText("");
        binding.sendButton.setEnabled(false);

        // 把输入内容显示到输出框
        builder.append(msg).append("\n");
        binding.displayBox.setText(builder.toString());

        FragmentActivity activity = getActivity();
        if (activity == null) return;
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity);
        String api_key = sharedPreferences.getString("signature", "");

        // 启动后台获取任务
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        executorService.execute(new HttpAsyncTask(msg, api_key));
        executorService.shutdown();
    }


    private class HttpAsyncTask implements Runnable {
        private final String msg;
        private final String api_key;

        public HttpAsyncTask(String msg_, String api_key) {
            msg = msg_;
            this.api_key = api_key;
        }

        @Override
        public void run() {
            StringBuilder responseBuilder = new StringBuilder();
            try {
                ChatMessage chatMessage = new ChatMessage();
                chatMessage.setRole("user");
                chatMessage.setContent(msg);
                List<ChatMessage> dataList = new ArrayList<>();
                dataList.add(chatMessage);

                ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder().messages(dataList).model("gpt-3.5-turbo").build();

                OpenAiService service = new OpenAiService(api_key, Duration.ofSeconds(60));
                ChatCompletionResult chatCompletion = service.createChatCompletion(chatCompletionRequest);
                chatCompletion.getChoices().forEach(choice -> responseBuilder.append(choice.getMessage().getContent()).append("\n"));

            } catch (Exception e) {
                responseBuilder.append(e).append("\n");
            }

            String result = responseBuilder.toString();
            if (!result.isEmpty()) {
                builder.append(result).append("\n");
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(() -> {
                    binding.displayBox.setText(builder.toString());
                    binding.sendButton.setEnabled(true);
                });
            }
        }
    }
}