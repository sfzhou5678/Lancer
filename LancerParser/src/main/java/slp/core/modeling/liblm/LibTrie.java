package slp.core.modeling.liblm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

class LibTrieNode {
    public Map<String, LibTrieNode> children = null;
    public boolean isLeaf = false;
    public String val;

    public LibTrieNode() {
    }

    public LibTrieNode(String token) {
        this.val = token;
    }

    public void identifyLibs(List<List<String>> libTokens) {
        ArrayList<String> path = new ArrayList<>();
        identifyLibs(libTokens, path, 1);
    }


    public void identifyLibs(List<List<String>> libTokens, ArrayList<String> path, int depth) {
        if (val != null) {
            if (Character.isUpperCase(val.charAt(0))) {
                return;
            } else if (val.matches("com[0-9]")) {
                return;
            }
            path.add(val);
        }
        if (depth >= 4 || children == null) {
            if (path.size() > 1)
                libTokens.add((List<String>) path.clone());
        } else {
            boolean haveUpper = false;
            for (Map.Entry<String, LibTrieNode> entry : children.entrySet()) {
                if (Character.isUpperCase(entry.getKey().charAt(0))) {
                    haveUpper = true;
                    break;
                }
            }
            if (haveUpper) {
                if (path.size() > 1)
                    libTokens.add((List<String>) path.clone());
            } else {
                for (Map.Entry<String, LibTrieNode> entry : children.entrySet()) {
                    entry.getValue().identifyLibs(libTokens, path, depth + 1);
                }
            }
        }
        if (val != null) {
            path.remove(path.size() - 1);
        }
    }
}

public class LibTrie {
    private LibTrieNode root;

    public LibTrie() {
        root = new LibTrieNode();
    }

    public LibTrie(List<List<String>> libTokens) {
        root = new LibTrieNode();
        insertLibs(libTokens);
    }

    private boolean isStopToken(String token) {
        String regEx = "[/./*]";
        Pattern pattern = Pattern.compile(regEx);

        Matcher matcher = pattern.matcher(token);
        return matcher.matches();
    }

    public void insertLibs(List<List<String>> libTokens) {
        for (List<String> tokens : libTokens) {
            insert(tokens);
        }
    }

    public void insert(List<String> libToken) {
        LibTrieNode node = root;
        for (String token : libToken) {
            if (isStopToken(token)) {
                continue;
            }
            if (node.children == null)
                node.children = new HashMap<String, LibTrieNode>();
            if (!node.children.containsKey(token)) {
                node.children.put(token, new LibTrieNode(token));
                node.isLeaf = false;
            }
            node = node.children.get(token);
        }
        node.isLeaf = true;
    }

    public List<String> search(List<String> libToken) {
        List<String> path = new ArrayList<>();
        LibTrieNode node = root;
        for (String token : libToken) {
            if (isStopToken(token)) {
                continue;
            }
            if (node.children == null || !node.children.containsKey(token)) {
                break;
            }
            node = node.children.get(token);
            path.add(node.val);
        }
        if (node.isLeaf) {
            return path;
        } else {
            return null;
        }
    }

    public List<List<String>> identifyLibs() {
        List<List<String>> libTokens = new ArrayList<>();
        if (root != null && root.children != null) {
            for (Map.Entry<String, LibTrieNode> entry : root.children.entrySet()) {
                entry.getValue().identifyLibs(libTokens);
            }
        }
        return libTokens;
    }
}


