package org.example;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.*;

public class Serveur_UDP {

    public static final int PORT = 20000;
    public static final String INTERFACE = "localhost";
    private static ArrayList<Game> gameInstances = new ArrayList<>();
    private static ArrayList<ArrayList<String>> datagramToCommandList(DatagramPacket packet){

        /* Converting data to String */
        String dataString = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);

        dataString = dataString.trim();

        /*
        Converting the data to an array of commands as Strings
        each command must be separated with commas as such : "cmd1,cmd2,cmd3"
        */
        var commands = dataString.split(",");

        /*
        Separate each command into a command with a value if it is provided
        the command and value must be separated by ":"
        for example the command "start:10" will become an ArrayList with "start" and "10"
        */
        ArrayList<ArrayList<String>> commandList = new ArrayList<ArrayList<String>>();

        for(int i=0; i<commands.length;i++){

            String[] tempCommand = commands[i].split(":");
            ArrayList<String> tempCommandArray = new ArrayList<>(Arrays.asList(tempCommand));
            commandList.add(tempCommandArray);
        }

        return commandList;
    }
    private static boolean isGameAlreadyStarted(InetAddress addr, int port){

        for (Game game : gameInstances){
            if((game.getUserAddr()==addr) && (game.getUserPort()==port)){
                return true;
            }
        }
        return false;
    }

    private static void sendErrorPacket(DatagramSocket socket, InetAddress destAddr, int destPort, String errorMsg){

        byte[] errorMsgBytes = ("error:" + errorMsg).getBytes(StandardCharsets.UTF_8);

        var errorMsgPacket = new DatagramPacket(errorMsgBytes, 0, errorMsgBytes.length);

        errorMsgPacket.setAddress(destAddr);
        errorMsgPacket.setPort(destPort);

        try {
            socket.send(errorMsgPacket);
        } catch (IOException e) {
            System.err.println("Could not send packet");
        }
    }

    private static void printErrorMsg(String errorMsg, DatagramPacket receivedPacket){
        System.err.println("Received start command by : " + receivedPacket.getAddress() + "/" + receivedPacket.getPort() + " : " + errorMsg);
    }
    private static int getGameInstanceOfClient(DatagramPacket receivedPacket){
        int gameId = -1;
        for(int i=0; i<gameInstances.size(); i++){
            if(gameInstances.get(i).getUserAddr()==receivedPacket.getAddress() && gameInstances.get(i).getUserPort()==receivedPacket.getPort()){
                gameId = i;
                break;
            }
        }
        return gameId;
    }

    public static boolean parseStart(ArrayList<ArrayList<String>> commandList, DatagramPacket receivedPacket, DatagramSocket socket){

        String errorMsg;

        for(ArrayList<String> command : commandList){

            if(command.get(0).equals("start")){

                /* Checking if user did not send a value of a max length for the anagram */
                if(command.size()==1){

                    errorMsg = "noLengthSpecified";

                    /* printing error in console and sending error message to user's client */

                    printErrorMsg(errorMsg, receivedPacket);
                    sendErrorPacket(socket,receivedPacket.getAddress(), receivedPacket.getPort(), errorMsg);

                    return false;
                }

                /* Checking if more than one value has been sent */
                if(command.size()>2){
                    System.out.println("tooManyValuesGiven");
                }

                /* initializing final length of an anagram */
                int finalLength;

                /* Checking if the value sent is a number */
                try{
                    finalLength = Integer.parseInt(command.get(1));
                }catch (NumberFormatException e){

                    errorMsg = "notANumber";

                    /* printing error in console and sending error message to user's client */
                    printErrorMsg(errorMsg, receivedPacket);
                    sendErrorPacket(socket,receivedPacket.getAddress(), receivedPacket.getPort(), errorMsg);

                    return false;
                }

                /* Check if game is already started for the user*/
                if(isGameAlreadyStarted(receivedPacket.getAddress(),receivedPacket.getPort())){

                    errorMsg = "gameAlreadyStarted";

                    /* printing error in console and sending error message to user's client */
                    printErrorMsg(errorMsg, receivedPacket);
                    sendErrorPacket(socket,receivedPacket.getAddress(), receivedPacket.getPort(), errorMsg);

                    return false;
                }

                if(finalLength==1){
                    errorMsg = "lengthOf1";

                    /* printing error in console and sending error message to user's client */
                    printErrorMsg(errorMsg, receivedPacket);
                    sendErrorPacket(socket,receivedPacket.getAddress(), receivedPacket.getPort(), errorMsg);

                    return false;
                }

                /* Check if client specified a dictionary */


                Game game = new Game(receivedPacket.getAddress(), receivedPacket.getPort(), finalLength, "src/main/resources/dictionaries/english-scowl-80.txt");

                /* Checking if we could generate a anagram sequence for the specified max length of a word */
                if(game.getAnagramSequence().isEmpty()){

                    errorMsg = "noAnagramSequenceFound";

                    /* printing error in console and sending error message to user's client */
                    printErrorMsg(errorMsg, receivedPacket);

                    sendErrorPacket(socket,receivedPacket.getAddress(), receivedPacket.getPort(), errorMsg);

                    return false;
                }

                /* All other checks are valid, we add the game to instances for the player and do not check other commands */
                gameInstances.add(game);

                /* Send a packet to inform the client of the first anagram to guess */

                String anagramToGuess = "guess:"+game.getCurrentAnagramToGuess();
                byte[] anagramToGuessBytes = anagramToGuess.getBytes(StandardCharsets.UTF_8);

                DatagramPacket initialGuessPacket = new DatagramPacket(anagramToGuessBytes, anagramToGuessBytes.length);
                initialGuessPacket.setAddress(game.getUserAddr());
                initialGuessPacket.setPort(game.getUserPort());

                try {
                    socket.send(initialGuessPacket);
                } catch (IOException e) {
                    System.err.println("Could not send initial guess packet");
                }

                return true;
            }
        }
        /* No start command in command list, we do nothing, and check for other commands*/
        return false;
    }
    public static boolean parseAnswer(ArrayList<ArrayList<String>> commandList, DatagramPacket receivedPacket, DatagramSocket socket){

        String errorMsg;

        for(ArrayList<String> command : commandList){
            if(command.get(0).equals("answer")){
                if((command.size()==1)){

                    errorMsg = "noAnswerGiven";

                    /* printing error in console and sending error message to user's client */
                    printErrorMsg(errorMsg, receivedPacket);
                    sendErrorPacket(socket,receivedPacket.getAddress(), receivedPacket.getPort(), errorMsg);

                    return false;
                }

                if(command.size()>2){
                    System.out.println("multipleAnswersGiven");
                }

                /* Searching if a game is started for this client */
                int gameId = getGameInstanceOfClient(receivedPacket);

                /* If gameId is still -1, no games were found for this client */
                if(gameId==-1){
                    errorMsg = "noGameFound";
                    printErrorMsg(errorMsg, receivedPacket);
                    sendErrorPacket(socket, receivedPacket.getAddress(), receivedPacket.getPort(), errorMsg);
                    return true;
                }

                boolean win = false;

                String answer = command.get(1);
                DatagramPacket sentPacket = new DatagramPacket(new byte[0], 0);
                if(gameInstances.get(gameId).answerIsCorrect(answer)){
                    /* The client has guessed a correct anagram */

                    if(!gameInstances.get(gameId).setNextAnagramToGuess()){
                        String winMsg = "win";
                        byte[] winMsgBytes = winMsg.getBytes(StandardCharsets.UTF_8);
                        sentPacket.setData(winMsgBytes);
                        win = true;

                    }else{

                        String nextAnagramToGuessMsg = "guess:" + gameInstances.get(gameId).getCurrentAnagramToGuess();
                        byte[] nextAnagramToGuessMsgBytes = nextAnagramToGuessMsg.getBytes(StandardCharsets.UTF_8);
                        sentPacket.setData(nextAnagramToGuessMsgBytes);

                    }

                }else{
                    /* The client has guessed a wrong answer */
                    String wrongAnswer = "wrong";
                    byte[] wrongAnswerBytes = wrongAnswer.getBytes(StandardCharsets.UTF_8);
                    sentPacket.setData(wrongAnswerBytes);
                }

                sentPacket.setAddress(gameInstances.get(gameId).getUserAddr());
                sentPacket.setPort(gameInstances.get(gameId).getUserPort());

                try {
                    socket.send(sentPacket);
                } catch (IOException e) {
                    System.err.println("Could not send packet");
                }

                /* Delete game Instance if win */
                if(win){
                    gameInstances.remove(gameId);
                }

                return true;
            }
        }
        return false;
    }

    public static boolean parseAbandon(ArrayList<ArrayList<String>> commandList, DatagramPacket receivedPacket, DatagramSocket socket){
        String errorMsg;

        for(ArrayList<String> command : commandList){
            if(command.get(0).equals("abandon")){

                int gameId = getGameInstanceOfClient(receivedPacket);

                /* If gameId is still -1, no games were found for this client */
                if(gameId==-1){
                    errorMsg = "noGameFound";
                    printErrorMsg(errorMsg, receivedPacket);
                    sendErrorPacket(socket, receivedPacket.getAddress(), receivedPacket.getPort(), errorMsg);
                    return true;
                }

                String msg = "gameCancelled:"+gameInstances.get(gameId).getExampleWord();
                gameInstances.remove(gameId);

                byte[] msgByte = msg.getBytes(StandardCharsets.UTF_8);
                DatagramPacket sentPacket = new DatagramPacket(msgByte, msgByte.length);
                sentPacket.setAddress(receivedPacket.getAddress());
                sentPacket.setPort(receivedPacket.getPort());

                try {
                    socket.send(sentPacket);
                } catch (IOException e) {
                    System.err.println("Could not send packet");
                }

                return true;

            }
        }
        return false;
    }

    public static void main(String[] args) throws SocketException {

        ///, InetAddress.getByName(INTERFACE)

        /* Initialization of a socket */
        var socket = new DatagramSocket(PORT);

        /* Setting the timeout of the socket to 10s */
        socket.setSoTimeout(10000);

        /* Buffer for our received packets */
        var buffer = new byte[128];

        /* Initialization of our receiving packet */
        var receivedPacket = new DatagramPacket(buffer, buffer.length);

        /* Initialization of our sending packet */
        var sentPacket = new DatagramPacket(new byte[0], 0);

        /* Main loop */
        while(true){

            /* Testing if we have received a packet */
            try {
                socket.receive(receivedPacket);
            } catch (IOException e) {
                System.err.println("Packet not received");
                continue;
            }

            String dataString = new String(receivedPacket.getData(), 0, receivedPacket.getLength(), StandardCharsets.UTF_8);
            System.out.println("Received Message from : " + receivedPacket.getAddress() + "/" + receivedPacket.getPort() + ": " + dataString);

            /* Converting the data in the received packet to a List of commands */
            ArrayList<ArrayList<String>> commandList = datagramToCommandList(receivedPacket);

            /* Checking if user sent start command */
            if(parseStart(commandList, receivedPacket, socket)){
                continue;
            }

            if(parseAnswer(commandList, receivedPacket, socket)){
                continue;
            }

            if(parseAbandon(commandList, receivedPacket, socket)){
                continue;
            }

            System.out.println("Game Instances :");
            if(gameInstances.size()!=0){
                for(Game game : gameInstances){
                    System.out.println("ID : " + game.getUserAddr() + "/" + game.getUserPort());
                    System.out.println(game.getAnagramSequence());
                }
            }
        }
    }
}
