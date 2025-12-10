package wikipedia;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import wikipedia.models.SearchResult;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class WikipediaAPI {
    private static final String BASE_URL = "https://ru.wikipedia.org/w/api.php";
    private static final JsonParser jsonParser = new JsonParser();

    public List<SearchResult> search(String query) throws IOException {
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("Запрос пустой");
        }

        String encodedQuery;
        try {
            encodedQuery = URLEncoder.encode(query.trim(), "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            throw new IOException("Ошибка кодирования запроса", e);
        }

        String urlString = BASE_URL + "?action=query&list=search&format=json&srsearch=" + encodedQuery;

        HttpURLConnection connection = null;
        BufferedReader reader = null;

        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.setRequestProperty("User-Agent", "WikiSearchBot/1.0");
            connection.setRequestProperty("Accept", "application/json");

            int responseCode = connection.getResponseCode();

            if (responseCode != HttpURLConnection.HTTP_OK) {
                String errorMessage = getResponseError(connection);
                throw new IOException("HTTP ошибка: " + responseCode + " - " + errorMessage);
            }

            reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));

            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            return parseResponse(response.toString());

        } catch (MalformedURLException e) {
            throw new IOException("Некорректный URL", e);
        } catch (UnknownHostException e) {
            throw new IOException("Не удается найти сервер Википедии", e);
        } catch (SocketTimeoutException e) {
            throw new IOException("Таймаут подключения", e);
        } catch (java.net.ConnectException e) {
            throw new IOException("Не удается подключиться к серверу", e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String getResponseError(HttpURLConnection connection) {
        try {
            BufferedReader errorReader = new BufferedReader(
                    new InputStreamReader(connection.getErrorStream(), "UTF-8")
            );
            StringBuilder errorResponse = new StringBuilder();
            String line;
            while ((line = errorReader.readLine()) != null) {
                errorResponse.append(line);
            }
            errorReader.close();
            return errorResponse.toString();
        } catch (Exception e) {
            return "Не удалось получить сообщение об ошибке";
        }
    }

    private List<SearchResult> parseResponse(String json) throws IOException {
        try {
            JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();

            if (!jsonObject.has("query")) {
                return new ArrayList<>();
            }

            JsonObject queryObj = jsonObject.getAsJsonObject("query");
            if (!queryObj.has("search")) {
                return new ArrayList<>();
            }

            JsonArray searchArray = queryObj.getAsJsonArray("search");
            List<SearchResult> results = new ArrayList<>();

            for (JsonElement element : searchArray) {
                JsonObject obj = element.getAsJsonObject();

                SearchResult result = new SearchResult() {
                    private final String title = getString(obj, "title");
                    private final String snippet = getString(obj, "snippet");
                    private final int pageid = getInt(obj, "pageid");

                    @Override
                    public String getTitle() {
                        return title;
                    }

                    @Override
                    public String getSnippet() {
                        return snippet;
                    }

                    @Override
                    public int getPageid() {
                        return pageid;
                    }
                };

                results.add(result);
            }
            return results;

        } catch (JsonSyntaxException e) {
            throw new IOException("Ошибка формата JSON", e);
        } catch (IllegalStateException e) {
            throw new IOException("Некорректный ответ сервера", e);
        } catch (NullPointerException e) {
            throw new IOException("Пустой ответ от сервера", e);
        }
    }

    private String getString(JsonObject obj, String key) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            return obj.get(key).getAsString();
        }
        return "";
    }

    private int getInt(JsonObject obj, String key) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            return obj.get(key).getAsInt();
        }
        return 0;
    }
}