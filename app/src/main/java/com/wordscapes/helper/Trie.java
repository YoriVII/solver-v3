package com.wordscapes.helper;
import java.util.*;
class TrieNode {
    Map<Character, TrieNode> children = new HashMap<>();
    boolean isEndOfWord;
}
public class Trie {
    private final TrieNode root = new TrieNode();
    public void insert(String word) {
        TrieNode current = root;
        for (char l : word.toUpperCase().toCharArray()) {
            current = current.children.computeIfAbsent(l, k -> new TrieNode());
        }
        current.isEndOfWord = true;
    }
}
