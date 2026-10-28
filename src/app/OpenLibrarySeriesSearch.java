package app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Looks up a series creator while keeping the user's series title intact. */
final class OpenLibrarySeriesSearch {
    private static final String SEARCH_URL = "https://openlibrary.org/search.json";

    private OpenLibrarySeriesSearch() { }

    static SearchResult search(String seriesTitle) throws IOException {
        String query = URLEncoder.encode(seriesTitle, StandardCharsets.UTF_8.name());
        URL url = new URL(SEARCH_URL + "?title=" + query + "&fields=author_name,subject&limit=10");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(8000);
        connection.setReadTimeout(8000);
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("User-Agent", "FieldnoteLibrary/1.0");

        try {
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("Open Library returned HTTP " + responseCode + ".");
            }
            return firstResultFromDocs(readAll(connection.getInputStream()));
        } finally {
            connection.disconnect();
        }
    }

    private static String readAll(InputStream input) throws IOException {
        StringBuilder text = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) text.append(line);
        }
        return text.toString();
    }

    private static SearchResult firstResultFromDocs(String json) throws IOException {
        int docsKey = json.indexOf("\"docs\"");
        int docsStart = docsKey < 0 ? -1 : json.indexOf('[', docsKey);
        if (docsStart < 0) return null;

        int position = docsStart + 1;
        while (position < json.length()) {
            char current = json.charAt(position);
            if (Character.isWhitespace(current) || current == ',') {
                position++;
                continue;
            }
            if (current == ']') return null;
            if (current != '{') {
                position++;
                continue;
            }

            int objectStart = position;
            int depth = 0;
            boolean inString = false;
            boolean escaped = false;
            while (position < json.length()) {
                char value = json.charAt(position++);
                if (inString) {
                    if (escaped) escaped = false;
                    else if (value == '\\') escaped = true;
                    else if (value == '"') inString = false;
                } else if (value == '"') {
                    inString = true;
                } else if (value == '{') {
                    depth++;
                } else if (value == '}' && --depth == 0) {
                    String object = json.substring(objectStart, position);
                    String author = firstArrayString(object, "author_name");
                    if (author != null && !author.trim().isEmpty()) {
                        return new SearchResult(author.trim(), arrayStrings(object, "subject"));
                    }
                    break;
                }
            }
        }
        return null;
    }

    private static String firstArrayString(String object, String field) throws IOException {
        List<String> values = arrayStrings(object, field);
        return values.isEmpty() ? null : values.get(0);
    }

    private static List<String> arrayStrings(String object, String field) throws IOException {
        int key = object.indexOf("\"" + field + "\"");
        if (key < 0) return Collections.emptyList();
        int arrayStart = object.indexOf('[', key);
        if (arrayStart < 0) return Collections.emptyList();
        List<String> values = new ArrayList<>();
        int position = arrayStart + 1;
        while (position < object.length()) {
            char current = object.charAt(position);
            if (Character.isWhitespace(current) || current == ',') {
                position++;
            } else if (current == ']') {
                return values;
            } else if (current == '"') {
                String value = readJsonString(object, position);
                values.add(value);
                position = skipJsonString(object, position);
            } else {
                position++;
            }
        }
        return values;
    }

    private static int skipJsonString(String json, int start) throws IOException {
        boolean escaped = false;
        for (int position = start + 1; position < json.length(); position++) {
            char current = json.charAt(position);
            if (escaped) escaped = false;
            else if (current == '\\') escaped = true;
            else if (current == '"') return position + 1;
        }
        throw new IOException("Unterminated string in Open Library response.");
    }

    private static String readJsonString(String json, int start) throws IOException {
        StringBuilder value = new StringBuilder();
        for (int position = start + 1; position < json.length(); position++) {
            char current = json.charAt(position);
            if (current == '"') return value.toString();
            if (current != '\\') {
                value.append(current);
                continue;
            }
            if (++position >= json.length()) break;
            char escaped = json.charAt(position);
            switch (escaped) {
                case '"': value.append('"'); break;
                case '\\': value.append('\\'); break;
                case '/': value.append('/'); break;
                case 'b': value.append('\b'); break;
                case 'f': value.append('\f'); break;
                case 'n': value.append('\n'); break;
                case 'r': value.append('\r'); break;
                case 't': value.append('\t'); break;
                case 'u':
                    if (position + 4 >= json.length()) throw new IOException("Invalid Unicode escape in response.");
                    try {
                        value.append((char) Integer.parseInt(json.substring(position + 1, position + 5), 16));
                    } catch (NumberFormatException e) {
                        throw new IOException("Invalid Unicode escape in response.", e);
                    }
                    position += 4;
                    break;
                default: throw new IOException("Invalid escape in Open Library response.");
            }
        }
        throw new IOException("Unterminated string in Open Library response.");
    }

    static final class SearchResult {
        private final String author;
        private final List<String> subjects;

        SearchResult(String author, List<String> subjects) {
            this.author = author;
            this.subjects = subjects;
        }

        String getAuthor() { return author; }
        List<String> getSubjects() { return subjects; }
    }
}