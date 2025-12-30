package com.wordscapes.helper;
import java.util.*;

public class Trie {
    class TrieNode {
        Map<Character, TrieNode> children = new HashMap<>();
        boolean isEndOfWord;
    }
    private final TrieNode root = new TrieNode();

    // Add a few test words immediately so we can test without downloading a massive file yet
    public Trie() {
        insert("CAT"); insert("ACT"); insert("DOG"); insert("GOD"); 
        insert("BAT"); insert("TAB"); insert("ATE"); insert("EAT");
        insert("TEA");
    }

    public void insert(String word) {
        TrieNode current = root;
        for (char l : word.toUpperCase().toCharArray()) {
            current = current.children.computeIfAbsent(l, k -> new TrieNode());
        }
        current.isEndOfWord = true;
    }

    public void findWords(String letters, String prefix, TrieNode node, boolean[] visited, Set<String> results) {
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
    
    // Wrapper to make calling easier
    public List<String> solve(String letters) {
        Set<String> results = new HashSet<>();
        findWords(letters, "", root, new boolean[letters.length()], results);
        return new ArrayList<>(results);
    }
}
