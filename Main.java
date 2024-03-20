import okhttp3.*;
import com.alibaba.fastjson.JSON;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Main {
    public static final String NAME = "姓名";
    public static final String BOOK = "课程名称";
    private static final String URL = "抢课链接";
    private static final String COOKIE_HEADER = Constant.COOKIE;
    private static int count;

    public static void main(String[] args) {
        String fileName = generateFileName();

        try (FileOutputStream fos = new FileOutputStream(fileName);
             PrintStream ps = new PrintStream(fos)) {
            System.setOut(new DoublePrintStream(System.out, ps));
            OkHttpClient client = new OkHttpClient.Builder().build();

            for (int i = 0; i < 1; i++) {
                makeRequest(client);
            }

            System.out.println("Output has been written to the file: " + fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String generateFileName() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        return formatter.format(now) + ".txt";
    }

    private static void makeRequest(OkHttpClient client) {
        try {
            Thread.sleep(Constant.SLEEP);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        System.out.println("正在进行第" + ++count + "次抢课");
        Request request = new Request.Builder()
                .url(URL)
                .addHeader("Cookie", COOKIE_HEADER)
                .build();

        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                makeRequest(client);
                System.out.println("第" + count + "次抢课，发送请求失败，错误原因：" + e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();
                System.out.println("第" + count + "次抢课响应成功，响应内容：" + responseBody);
                ResponseBean courseSelectionResponse = JSON.parseObject(responseBody, ResponseBean.class);

                if (courseSelectionResponse.isSuccess() == 2) {
                    System.out.println("第" + count + "次抢课成功！");
                    PushPlusHelper.pushMessage(NAME + " 抢课成功~", "第" + count + "次抢课 " + BOOK + " 成功~\n" + courseSelectionResponse.getMessage());
                    try {
                        Thread.sleep(Constant.SLEEP);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    System.exit(0);
                } else if (courseSelectionResponse.isSuccess() == 1) {
                    System.out.println("第" + count + "次抢课结束！");
                    PushPlusHelper.pushMessage(NAME + " 抢课失败~", "第" + count + "次抢课 " + BOOK + " 抢课失败~\n" + courseSelectionResponse.getMessage());
                    try {
                        Thread.sleep(Constant.SLEEP);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    System.exit(0);
                } else {
                    System.out.println("第" + count + "次抢课失败");
                    makeRequest(client);
                }
                response.close();
            }
        });
    }

    private static class DoublePrintStream extends PrintStream {
        private final PrintStream second;

        public DoublePrintStream(PrintStream first, PrintStream second) {
            super(first);
            this.second = second;
        }

        @Override
        public void println(String x) {
            super.println(x);
            second.println(x);
        }
    }
}
