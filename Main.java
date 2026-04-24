import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

import org.json.JSONArray;
import org.json.JSONObject;

public class Main {

    public static void main(String[] args) throws Exception {

        String regNo = "RA2311042020049"; 

        HttpClient client = HttpClient.newHttpClient();

        // To remove duplicates
        Set<String> uniqueEvents = new HashSet<>();

        // To store total scores
        Map<String, Integer> totalScores = new HashMap<>();

        // 🔁 Poll API 10 times
        for (int poll = 0; poll < 10; poll++) {

            String url = "https://devapigw.vidalhealthtpa.com/srm-quiz-task/quiz/messages"
                    + "?regNo=" + regNo + "&poll=" + poll;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());

            String body = response.body().trim();

            // ✅ Safety check
            if (!body.startsWith("{")) {
                System.out.println("Skipping invalid response for poll " + poll);
                continue;
            }

            JSONObject json = new JSONObject(body);
            JSONArray events = json.getJSONArray("events");

            for (int i = 0; i < events.length(); i++) {

                JSONObject event = events.getJSONObject(i);

                String roundId = event.getString("roundId").trim();
                String participant = event.getString("participant").trim();
                int score = event.getInt("score");

                // Unique key
                String key = roundId + "_" + participant;

                if (!uniqueEvents.contains(key)) {
                    uniqueEvents.add(key);

                    totalScores.put(participant,
                            totalScores.getOrDefault(participant, 0) + score);
                }
            }

            System.out.println("Poll " + poll + " done");

            Thread.sleep(5000); // ⏱ mandatory delay
        }

        // 🔽 Sort leaderboard (descending)
        List<Map.Entry<String, Integer>> leaderboardList =
                new ArrayList<>(totalScores.entrySet());

        leaderboardList.sort((a, b) -> b.getValue() - a.getValue());

        System.out.println("\nFINAL LEADERBOARD:");
        for (Map.Entry<String, Integer> entry : leaderboardList) {
            System.out.println(entry.getKey() + " : " + entry.getValue());
        }

        // 📦 Create JSON for submission
        JSONArray leaderboardArray = new JSONArray();

        for (Map.Entry<String, Integer> entry : leaderboardList) {
            JSONObject obj = new JSONObject();
            obj.put("participant", entry.getKey());
            obj.put("totalScore", entry.getValue());
            leaderboardArray.put(obj);
        }

        JSONObject finalPayload = new JSONObject();
        finalPayload.put("regNo", regNo);
        finalPayload.put("leaderboard", leaderboardArray);

        // 🚀 Submit result
        HttpRequest postRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://devapigw.vidalhealthtpa.com/srm-quiz-task/quiz/submit"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(finalPayload.toString()))
                .build();

        HttpResponse<String> postResponse =
                client.send(postRequest, HttpResponse.BodyHandlers.ofString());

        System.out.println("\nSERVER RESPONSE:");
        System.out.println(postResponse.body());
    }
}