package com.aro.chatboy;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.aro.chatboy.databinding.FragmentFirstBinding;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FirstFragment extends Fragment {

    private FragmentFirstBinding binding;

    StringBuilder builder = new StringBuilder();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentFirstBinding.inflate(inflater, container, false);


        binding.sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessage(view);
            }
        });

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
    }

    @Override
    public void onDestroyView() {
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

        // 启动后台获取任务
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        executorService.execute(new HttpAsyncTask(msg));
    }


    private class HttpAsyncTask implements Runnable {
        String msg;

        public HttpAsyncTask(String msg_) {
            msg = msg_;
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


                OpenAiService service = new OpenAiService("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");
                service.createChatCompletion(chatCompletionRequest).getChoices().forEach(choice -> responseBuilder.append(choice.getMessage().getContent()).append("\n"));

            } catch (Exception e) {
                responseBuilder.append(e).append("\n");
            }

            String result = responseBuilder.toString();
            if (!result.isEmpty()) {
                builder.append(result).append("\n");
                binding.displayBox.setText(builder.toString());

                binding.sendButton.setEnabled(true);
            }
        }
    }
}