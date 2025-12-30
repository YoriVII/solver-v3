package com.wordscapes.helper;
import android.content.Context;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public class Trie {
    class TrieNode {
        Map<Character, TrieNode> children = new HashMap<>();
        boolean isEndOfWord;
    }
    private final TrieNode root = new TrieNode();

    // New method: Load words from the words.txt file
    public void loadDictionary(Context context) {
        try {
            InputStream is = context.getResources().openRawResource(R.raw.words);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = reader.readLine()) != null) {
                insert(line.trim());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void insert(String word) {
        if (word.length() < 3) return; // Wordscapes ignores tiny words
        TrieNode current = root;
        for (char l : word.toUpperCase().toCharArray()) {
            current = current.children.computeIfAbsent(l, k -> new TrieNode());
        }
        current.isEndOfWord = true;
    }

    public List<String> solve(String letters) {
        Set<String> results = new HashSet<>();
        findWords(letters, "", root, new boolean[letters.length()], results);
        List<String> sorted = new ArrayList<>(results);
        // Sort by length (longest words first)
        Collections.sort(sorted, (a, b) -> b.length() - a.length());
        return sorted;
    }

    private void findWords(String letters, String prefix, TrieNode node, boolean[] visited, Set<String> results) {
        if (node.isEndOfWord && prefix.length() >= 3) results.add(prefix);

        for (int i = 0; i < letters.length(); i++) {
            if (visited[i]) continue;
            char c = letters.toUpperCase().charAt(i);
            if (node.children.containsKey(c)) {
                visited[i] = true;
                findWords(letters, prefix + c, node.children.get(c), visited, results);
                visited[i] = false;
            }
        }
    }
}
