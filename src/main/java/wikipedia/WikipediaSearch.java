package wikipedia;

import wikipedia.models.SearchResult;
import java.net.URI;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;

public class WikipediaSearch {
    private WikipediaAPI wikipediaAPI;
    private Scanner scanner;

    public WikipediaSearch() {
        wikipediaAPI = new WikipediaAPI();
        scanner = new Scanner(System.in);
    }

    public void start() {
        System.out.println("Программа поиска в Википедии");
        System.out.println("Для выхода введите 'exit'");
        System.out.println();

        while (true) {
            System.out.print("Введите запрос: ");

            try {
                String query = scanner.nextLine();

                if (query.equalsIgnoreCase("exit")) {
                    break;
                }

                if (query.trim().isEmpty()) {
                    System.out.println("Введите непустой запрос");
                    continue;
                }

                List<SearchResult> results;
                try {
                    results = wikipediaAPI.search(query);
                } catch (IllegalArgumentException e) {
                    System.out.println("Ошибка: " + e.getMessage());
                    continue;
                } catch (java.net.UnknownHostException e) {
                    System.out.println("Ошибка сети: не удается найти сервер");
                    continue;
                } catch (java.net.SocketTimeoutException e) {
                    System.out.println("Таймаут подключения. Проверьте интернет");
                    continue;
                } catch (java.net.ConnectException e) {
                    System.out.println("Не удается подключиться к серверу");
                    continue;
                } catch (java.io.IOException e) {
                    System.out.println("Ошибка ввода-вывода: " + e.getMessage());
                    continue;
                } catch (java.lang.Exception e) {
                    System.out.println("Ошибка: " + e.getMessage());
                    continue;
                }

                if (results.isEmpty()) {
                    System.out.println("Ничего не найдено");
                    continue;
                }

                showResults(results);

                if (results.size() > 1) {
                    selectArticle(results);
                } else {
                    openArticle(results.get(0));
                }

            } catch (java.util.NoSuchElementException e) {
                System.out.println("Ошибка ввода");
                break;
            } catch (java.lang.IllegalStateException e) {
                System.out.println("Ошибка сканера");
                break;
            }
        }

        scanner.close();
        System.out.println("Программа завершена");
    }

    private void showResults(List<SearchResult> results) {
        System.out.println();
        System.out.println("Найдено " + results.size() + " результатов:");

        for (int i = 0; i < results.size(); i++) {
            SearchResult result = results.get(i);
            String title = result.getTitle();
            String snippet = cleanText(result.getSnippet());

            System.out.println((i + 1) + ". " + title);
            if (!snippet.isEmpty()) {
                System.out.println("   " + snippet.substring(0, Math.min(snippet.length(), 100)));
                if (snippet.length() > 100) {
                    System.out.println("   ...");
                }
            }
            System.out.println();
        }
    }

    private String cleanText(String text) {
        return text.replaceAll("<[^>]+>", "")
                .replaceAll("&[a-z]+;", "")
                .trim();
    }

    private void selectArticle(List<SearchResult> results) {
        System.out.print("Выберите номер статьи (1-" + results.size() + "): ");

        try {
            String input = scanner.nextLine();
            int choice = Integer.parseInt(input);

            if (choice >= 1 && choice <= results.size()) {
                openArticle(results.get(choice - 1));
            } else {
                System.out.println("Неверный номер");
            }
        } catch (NumberFormatException e) {
            System.out.println("Введите число");
        } catch (InputMismatchException e) {
            System.out.println("Неверный формат ввода");
        }
    }

    private void openArticle(SearchResult result) {
        try {
            String url = "https://ru.wikipedia.org/w/index.php?curid=" + result.getPageid();
            System.out.println("Открывается: " + result.getTitle());

            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().browse(new URI(url));
            } else {
                System.out.println("Ссылка: " + url);
            }
        } catch (java.net.URISyntaxException e) {
            System.out.println("Ошибка формата URL");
        } catch (java.io.IOException e) {
            System.out.println("Ошибка при открытии браузера");
        } catch (java.lang.UnsupportedOperationException e) {
            System.out.println("Операция не поддерживается");
        }
    }

    public static void main(String[] args) {
        WikipediaSearch search = new WikipediaSearch();
        search.start();
    }
}