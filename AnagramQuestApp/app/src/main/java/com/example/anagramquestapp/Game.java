package com.example.anagramquestapp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class Game {
    private final ArrayList<ArrayList<String>> anagramSequence;
    private int currentAnagramToGuess;
    public Game(ArrayList<ArrayList<String>> anagramSequence){

        this.anagramSequence = anagramSequence;
        this.currentAnagramToGuess=anagramSequence.size()-1;
    }
    private String sortWord(String word){

        char wordArray[] = word.toCharArray();
        Arrays.sort(wordArray);
        String sortedWord = new String(wordArray);
        return sortedWord;

    }
    public boolean setNextAnagramToGuess(){
        this.currentAnagramToGuess--;
        if(currentAnagramToGuess==-1){
            /* The client has guessed the last anagram, do not continue the game */
            return false;
        }

        /* The client has not guessed the last anagram, continue the game */
        return true;
    }
    public String getCurrentAnagramToGuess(){
        return sortWord(anagramSequence.get(currentAnagramToGuess).get(0));
    }
    public boolean isGameStarted(){
        return !anagramSequence.isEmpty();
    }
    public boolean answerIsCorrect(String answer){
        /* If the sorted answer isn't equal to the anagram to guess, it is automatically not correct */
        if(anagramSequence.get(currentAnagramToGuess).contains(answer)){
            return true;
        }else{
            return false;
        }

    }
    public String getExampleWord(){
        return anagramSequence.get(currentAnagramToGuess).get(0);
    }
    public String printAnagramSequence(){
        return anagramSequence.toString();
    }
}
