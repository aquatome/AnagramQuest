package com.example.starter;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class Game {

    private final ArrayList<String> anagramSequence;
    private final ArrayList<String> dictionary;
    private final Map<String, Set<String>> anagramicClasses;

    public Game(int finalLength, String dictPath){

        this.dictionary = loadDictionary(dictPath);

        this.anagramicClasses = createAnagramicClasses(dictionary);

        this.anagramSequence = findAnagramSequence(finalLength);

    }

    public boolean isAnagramSequenceEmpty(){

      if(anagramSequence==null){
        return true;
      }
      return anagramSequence.isEmpty();
    }
    private String sortWord(String word){

        char[] wordArray = word.toCharArray();
        Arrays.sort(wordArray);
        return new String(wordArray);

    }
    private Map<String, Set<String>> createAnagramicClasses(ArrayList<String> dictionary){

        Map<String, Set<String>> anagramicClasses = new HashMap<String, Set<String>>();

        for(String word : dictionary){

            String sortedWord = sortWord(word);


            Set<String> newSet;
            if(anagramicClasses.containsKey(sortedWord)){
                newSet = anagramicClasses.get(sortedWord);
            }else{

                newSet = new HashSet<>();
            }
            newSet.add(word);
            anagramicClasses.put(sortedWord, newSet);

        }

        return anagramicClasses;
    }
    public JsonArray getAnagramSequenceAsJSON(){

      JsonArray anagramSequence = new JsonArray();
      for(String anagram : this.anagramSequence){

        JsonObject wordsJO = new JsonObject();
        wordsJO.put("words", getAnagramsFromAnagramicClasses(anagram));

        anagramSequence.add(wordsJO);
      }

      return anagramSequence;
    }
    private String getAnagramsFromAnagramicClasses(String sortedWord){

      String words = "";
      for(String word : anagramicClasses.get(sortedWord)){
        words = words + word + ",";
      }
      words = words.substring(0, words.length()-1);
      return words;
    }
    private ArrayList<String> loadDictionary(String path){

        ArrayList<String> dictionary = new ArrayList<>();

        try {
            dictionary = (ArrayList<String>) Files.readAllLines(Paths.get(path));
        } catch (IOException e) {
            System.err.println("Could not load the dictionary");
        }

        return dictionary;
    }
    private ArrayList<String> findAnagramSequence(int maxLength){

        if(dictionary.size()==0){

            System.err.println("Dictionary is empty -> cannot create anagram sequence");
            return anagramSequence;
        }

        ArrayList<String> potentialLastWords = new ArrayList<>();

        /* Checking if there is a word of specified max length */
        for(String word : dictionary){
            if(word.length() == maxLength){
                potentialLastWords.add(word);
            }
        }

        if(potentialLastWords.isEmpty()){
            System.err.println("Could not find a word with a max length of " + maxLength + " -> cannot create anagram sequence");
            return anagramSequence;
        }

        /* Choosing one of the last words randomly */
        int randomLastWord = ThreadLocalRandom.current().nextInt(0, potentialLastWords.size());

        String lastWord = potentialLastWords.get(randomLastWord);

        System.out.println("Last word : " + lastWord);


        ArrayList<String> finalAnagramSequence = findAnagrams(lastWord);

        /* Delete last string if it is of length 1, it is useless */
        if(finalAnagramSequence.get(finalAnagramSequence.size()-1).length()==1){
            finalAnagramSequence.remove(finalAnagramSequence.size()-1);
        }

        String sortLastWord = sortWord(finalAnagramSequence.get(0));
        finalAnagramSequence.set(0,sortLastWord);

        return finalAnagramSequence;

    }
    private ArrayList<String> findAnagrams(String initialWord){

        /* Finding all anagram with 1 less letter */
        ArrayList<String> anagramsWith1LessLetter = new ArrayList<>();

        for(var entry : anagramicClasses.entrySet()){

            if(!(entry.getKey().length() == initialWord.length()-1)){
                continue;
            }

            /* Getting all characters into a list */
            char[] initialWordAsArray = initialWord.toCharArray();
            List<Character> initialWordAsList = new ArrayList<>();
            for(char c : initialWordAsArray){
                initialWordAsList.add(c);
            }

            /* Deleting every letter in initial word from the key's letter */
            for(int i=0; i<entry.getKey().length(); i++){
                if(initialWordAsList.contains(entry.getKey().charAt(i))){
                    for(int j=0; j<initialWordAsList.size(); j++){
                        if(initialWordAsList.get(j)==entry.getKey().charAt(i)){
                            initialWordAsList.remove(j);
                            break;
                        }
                    }
                }
            }

            /* If only one letter is remaining we can use for the sequence */
            if(initialWordAsList.size()==1){
                anagramsWith1LessLetter.add(entry.getKey());
            }

        }

        /* If no anagrams with one less letters are found, return the initial word */
        if(anagramsWith1LessLetter.isEmpty()){
            ArrayList<String> retWord = new ArrayList<>();
            retWord.add(initialWord);

            return retWord;
        }

        ArrayList<ArrayList<String>> potentialSequences = new ArrayList<>();

        /* Get sequences from all potential anagrams */
        for(String anagram : anagramsWith1LessLetter){

            potentialSequences.add(findAnagrams(anagram));

        }

        /* Decide which sequence is the best by choosing the longest one */
        int bestSequence = 0;
        for(int i=1; i<potentialSequences.size(); i++){

            /* If both sequences are equal choose randomly one of the two */
            if(potentialSequences.get(i).size() == potentialSequences.get(bestSequence).size()){
                if(ThreadLocalRandom.current().nextInt(0, 2)==1){
                    bestSequence=i;
                }
            }

            if(potentialSequences.get(i).size() > potentialSequences.get(bestSequence).size()){
                bestSequence = i;
            }

        }

        ArrayList<String> sequence = new ArrayList<>();
        sequence.add(initialWord);
        sequence.addAll(potentialSequences.get(bestSequence));

        return sequence;

    }
}
