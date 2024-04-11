package com.mrli.openai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.mrli.openai.completion.CompletionRequest;
import com.mrli.openai.completion.CompletionResult;
import com.mrli.openai.completion.chat.ChatCompletionRequest;
import com.mrli.openai.completion.chat.ChatCompletionResult;
import com.mrli.openai.config.ChatGptConfig;
import com.mrli.openai.edit.EditRequest;
import com.mrli.openai.edit.EditResult;
import com.mrli.openai.embedding.EmbeddingRequest;
import com.mrli.openai.embedding.EmbeddingResult;
import com.mrli.openai.file.File;
import com.mrli.openai.finetune.FineTuneEvent;
import com.mrli.openai.finetune.FineTuneRequest;
import com.mrli.openai.finetune.FineTuneResult;
import com.mrli.openai.image.CreateImageEditRequest;
import com.mrli.openai.image.CreateImageRequest;
import com.mrli.openai.image.CreateImageVariationRequest;
import com.mrli.openai.image.ImageResult;
import com.mrli.openai.model.Model;
import com.mrli.openai.moderation.ModerationRequest;
import com.mrli.openai.moderation.ModerationResult;
import io.reactivex.Single;
import okhttp3.*;
import retrofit2.HttpException;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class OpenAiService {

    private static final String BASE_URL = "https://api.openai.com/";
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);
    private static final ObjectMapper errorMapper = defaultObjectMapper();

    private final OpenAiApi api;

    /**
     * 创建一个自定义的X509TrustManager，用于管理X509证书的信任。
     * 这个实现对客户端和服务器的证书信任检查进行空实现，即不进行任何信任验证。
     * 它返回一个空的接受发行者数组。
     *
     * @return 实现了X509TrustManager接口的匿名类实例，该实例不对证书进行信任验证。
     */
    private static X509TrustManager x509TrustManager() {
        return new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {}

            @Override
            public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {}

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        };
    }
    /**
     * 创建一个自定义的 SSLSocketFactory。
     * 这个工厂方法用于初始化 SSL 上下文并返回一个 SSLSocketFactory 实例，该实例可用于创建 SSL/TLS 连接。
     *
     * @return 初始化好的 SSLSocketFactory 实例，如果初始化失败则返回 null。
     */
    private static SSLSocketFactory sslSocketFactory() {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[] {x509TrustManager()}, new SecureRandom());
            return sslContext.getSocketFactory();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }
        return null;
    }
    /**
     * 构造函数：通过配置对象初始化OpenAiService。
     *
     * @param config ChatGptConfig对象，包含访问OpenAI所需的配置信息，如token和timeout。
     */
    public OpenAiService(ChatGptConfig config) {
        this(config.getToken(), Duration.ofSeconds(config.getTimeout()));
    }

    /**
     * 构造函数：使用token和超时时间初始化OpenAiService。
     *
     * @param token 访问OpenAI API所需的令牌。
     * @param timeout 请求OpenAI API的超时时间。
     */
    public OpenAiService(final String token, final Duration timeout) {
        this(buildApi(token, timeout));
    }

    /**
     * OpenAiService的构造函数。
     * 用于初始化一个OpenAiService实例，将传入的OpenAiApi实例赋值给内部的api变量。
     *
     * @param api OpenAiApi类型的参数，代表OpenAI的API接口实例。
     */
    public OpenAiService(final OpenAiApi api) {
        this.api = api;
    }

    public List<Model> listModels() {
        return execute(api.listModels()).data;
    }

    public Model getModel(String modelId) {
        return execute(api.getModel(modelId));
    }

    public CompletionResult createCompletion(CompletionRequest request) {
        return execute(api.createCompletion(request));
    }

    public ChatCompletionResult createChatCompletion(ChatCompletionRequest request) {
        return execute(api.createChatCompletion(request));
    }

    public EditResult createEdit(EditRequest request) {
        return execute(api.createEdit(request));
    }

    public EmbeddingResult createEmbeddings(EmbeddingRequest request) {
        return execute(api.createEmbeddings(request));
    }

    public List<File> listFiles() {
        return execute(api.listFiles()).data;
    }
    /**
     * 上传文件到指定目的。
     *
     * @param purpose 上传文件的目的描述。
     * @param filepath 要上传的文件的本地路径。
     * @return 返回上传结果的文件对象。
     */
    public File uploadFile(String purpose, String filepath) {
        // 创建本地文件对象
        java.io.File file = new java.io.File(filepath);
        // 创建目的描述的请求体
        RequestBody purposeBody = RequestBody.create(okhttp3.MultipartBody.FORM, purpose);
        // 将文件封装成请求体，这里将文件类型解析为文本类型，实际应根据文件类型正确设置
        RequestBody fileBody = RequestBody.create(MediaType.parse("text"), file);
        // 创建multipart/form-data类型的请求体，用于上传文件
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", filepath, fileBody);
        // 执行上传操作并返回结果
        return execute(api.uploadFile(purposeBody, body));
    }

    public DeleteResult deleteFile(String fileId) {
        return execute(api.deleteFile(fileId));
    }

    public File retrieveFile(String fileId) {
        return execute(api.retrieveFile(fileId));
    }

    public FineTuneResult createFineTune(FineTuneRequest request) {
        return execute(api.createFineTune(request));
    }

    public CompletionResult createFineTuneCompletion(CompletionRequest request) {
        return execute(api.createFineTuneCompletion(request));
    }

    public List<FineTuneResult> listFineTunes() {
        return execute(api.listFineTunes()).data;
    }

    public FineTuneResult retrieveFineTune(String fineTuneId) {
        return execute(api.retrieveFineTune(fineTuneId));
    }

    public FineTuneResult cancelFineTune(String fineTuneId) {
        return execute(api.cancelFineTune(fineTuneId));
    }

    public List<FineTuneEvent> listFineTuneEvents(String fineTuneId) {
        return execute(api.listFineTuneEvents(fineTuneId)).data;
    }

    public DeleteResult deleteFineTune(String fineTuneId) {
        return execute(api.deleteFineTune(fineTuneId));
    }

    public ImageResult createImage(CreateImageRequest request) {
        return execute(api.createImage(request));
    }

    public ImageResult createImageEdit(CreateImageEditRequest request, String imagePath, String maskPath) {
        java.io.File image = new java.io.File(imagePath);
        java.io.File mask = null;
        if (maskPath != null) {
            mask = new java.io.File(maskPath);
        }
        return createImageEdit(request, image, mask);
    }

    public ImageResult createImageEdit(CreateImageEditRequest request, java.io.File image, java.io.File mask) {
        RequestBody imageBody = RequestBody.create(MediaType.parse("image"), image);

        MultipartBody.Builder builder = new MultipartBody.Builder()
                .setType(MediaType.get("multipart/form-data"))
                .addFormDataPart("prompt", request.getPrompt())
                .addFormDataPart("size", request.getSize())
                .addFormDataPart("response_format", request.getResponseFormat())
                .addFormDataPart("image", "image", imageBody);

        if (request.getN() != null) {
            builder.addFormDataPart("n", request.getN().toString());
        }

        if (mask != null) {
            RequestBody maskBody = RequestBody.create(MediaType.parse("image"), mask);
            builder.addFormDataPart("mask", "mask", maskBody);
        }

        return execute(api.createImageEdit(builder.build()));
    }

    public ImageResult createImageVariation(CreateImageVariationRequest request, String imagePath) {
        java.io.File image = new java.io.File(imagePath);
        return createImageVariation(request, image);
    }

    public ImageResult createImageVariation(CreateImageVariationRequest request, java.io.File image) {
        RequestBody imageBody = RequestBody.create(MediaType.parse("image"), image);

        MultipartBody.Builder builder = new MultipartBody.Builder()
                .setType(MediaType.get("multipart/form-data"))
                .addFormDataPart("size", request.getSize())
                .addFormDataPart("response_format", request.getResponseFormat())
                .addFormDataPart("image", "image", imageBody);

        if (request.getN() != null) {
            builder.addFormDataPart("n", request.getN().toString());
        }

        return execute(api.createImageVariation(builder.build()));
    }

    public ModerationResult createModeration(ModerationRequest request) {
        return execute(api.createModeration(request));
    }

    /**
     * Calls the Open AI api, returns the response, and parses error messages if the request fails
     */
    public static <T> T execute(Single<T> apiCall) {
        try {
            return apiCall.blockingGet();
        } catch (HttpException e) {
            try {
                if (e.response() == null || e.response().errorBody() == null) {
                    throw e;
                }
                String errorBody = e.response().errorBody().string();

                OpenAiError error = errorMapper.readValue(errorBody, OpenAiError.class);
                throw new OpenAiHttpException(error, e, e.code());
            } catch (IOException ex) {
                // couldn't parse OpenAI error
                throw e;
            }
        }
    }

    public static OpenAiApi buildApi(String token, Duration timeout) {
        ObjectMapper mapper = defaultObjectMapper();
        OkHttpClient client = defaultClient(token, timeout);
        Retrofit retrofit = defaultRetrofit(client, mapper);

        return retrofit.create(OpenAiApi.class);
    }

    public static ObjectMapper defaultObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
        return mapper;
    }

    public static OkHttpClient defaultClient(String token, Duration timeout) {
        Authenticator authenticator = ChatGptConfig.authenticator;
        return new OkHttpClient.Builder()
                .addInterceptor(new AuthenticationInterceptor(token))
                .retryOnConnectionFailure(false)
                .connectionPool(new ConnectionPool(5, 1, TimeUnit.SECONDS))
                .readTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
                .proxy(ChatGptConfig.proxy)
                .sslSocketFactory(sslSocketFactory(), x509TrustManager())
                .proxyAuthenticator(authenticator)
                .authenticator(authenticator)
                .build();
    }

    public static Retrofit defaultRetrofit(OkHttpClient client, ObjectMapper mapper) {
        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(JacksonConverterFactory.create(mapper))
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build();
    }
}
