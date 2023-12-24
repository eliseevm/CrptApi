import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Deque;

class CrptApi {
    private String key = "ac2b8ce6-68e0-4d8c-88a7-fdfcab8b5e5e"; // Ключ авторизации.
    private Gson gson = new Gson();
    private final int requestLimit;
    private final Duration timeUnit;
    private final Deque<Instant> requestTimes;

    public CrptApi(Duration timeUnit, int requestLimit) {
        this.requestLimit = requestLimit;
        this.timeUnit = timeUnit;
        this.requestTimes = new ArrayDeque<>();
    }

    public synchronized void makeRequest() throws InterruptedException {
        Instant now = Instant.now();

        // Удаление устаревшего времени запроса
        while (!requestTimes.isEmpty() && requestTimes.peekFirst().isBefore(now.minus(timeUnit))) {
            requestTimes.removeFirst();
        }

        // Если лимит превышен, ожидаем следующего интервала
        while (requestTimes.size() >= requestLimit) {
            waiting(Duration.between(now, requestTimes.peekFirst().plus(timeUnit)).toMillis());
            now = Instant.now(); // Обновление текущего времени
        }

        requestTimes.addLast(now);


        // Вызываем метод клиента для сериализации данных и взаимодействия с API
        introductionCirculation(new Product(), key);

    }

    private synchronized void waiting(long durationMillis) throws InterruptedException {
        wait(durationMillis);
    }

    private synchronized void introductionCirculation(Object o, String key) {
        DocClient docClient = new DocClient("https://ismp.crpt.ru/api/v3/auth/cert/");
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.setPrettyPrinting();
        Gson gson = gsonBuilder.create();
        String doc = gson.toJson(o);
        System.out.println("Выводим в консоль Объект в виде json: " + doc);
        docClient.put(doc, key); // Вызываем метод клиента для отправки данных серверу
    }

    class DocClient {

        private String url;
        private URI uri;
        private String key;
        private HttpResponse.BodyHandler<String> handler = HttpResponse.BodyHandlers.ofString();
        private HttpClient client;


        public DocClient(String url) {
            client = HttpClient.newHttpClient();
            this.url = url;
        }

        public synchronized void put(String json, String key) {
            this.key = key;
            try {
                uri = URI.create(url + key);
                HttpRequest.Builder rb = HttpRequest.newBuilder();
                HttpRequest request = rb // создаем объект описывающий запрос
                        .uri(uri)
                        .version(HttpClient.Version.HTTP_1_1)
                        .header("Authorization", "Bearer <token>")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();
                HttpResponse<String> response = client.send(request, handler);
                System.out.println("Код добавления продукта произведенного в РФ, по ключу " + key + " - "
                        + response.statusCode());
            } catch (IOException | InterruptedException ex) {
                System.out.println(ex.getMessage());
            }
        }
    }


    class Product {
        String doc_id = "string";
        String doc_status = "string";
        String doc_type = "string";
        Boolean importRequest = true;
        String owner_inn = "string";
        String participant_inn = "string";
        String producer_inn = "string";
        String production_date = "2020-01-23";
        String production_type = "string";
        Products products = new Products();
        String reg_date = "2020-01-23";
        String reg_number = "string";
    }

    class Products {
        String certificate_document = "string";
        String certificate_document_date = "2020-01-23";
        String certificate_document_number = "string";
        String owner_inn = "string";
        String producer_inn = "string";
        String production_date = "2020-01-23";
        String tnved_code = "string";
        String uit_code = "string";
        String uitu_code = "string";
    }


    public static class Main {
        public static void main(String[] args) {
            CrptApi crptApi = new CrptApi(Duration.ofSeconds(1), 3);

            Runnable requestTask = () -> {
                try {
                    crptApi.makeRequest();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            };

            Thread thread1 = new Thread(requestTask);
            Thread thread2 = new Thread(requestTask);
            Thread thread3 = new Thread(requestTask);
            Thread thread4 = new Thread(requestTask);
            Thread thread5 = new Thread(requestTask);
            Thread thread6 = new Thread(requestTask);

            thread1.start();
            thread2.start();
            thread3.start();
            thread4.start();
            thread5.start();
            thread6.start();
        }
    }
}
