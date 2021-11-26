/*
 *  Copyright (C) 2021 Lucas B. R. de Oliveira - IFSP/SCL
 *  Contact: lucas <dot> oliveira <at> ifsp <dot> edu <dot> br
 *
 *  This file is part of CTruco (Truco game for didactic purpose).
 *
 *  CTruco is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  CTruco is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with CTruco.  If not, see <https://www.gnu.org/licenses/>
 */

package com.bueno.application.cli;

import com.bueno.domain.entities.deck.Card;
import com.bueno.domain.entities.game.GameRuleViolationException;
import com.bueno.domain.entities.hand.HandResult;
import com.bueno.domain.entities.hand.HandScore;
import com.bueno.domain.entities.hand.Intel;
import com.bueno.domain.entities.player.mineirobot.MineiroBot;
import com.bueno.domain.entities.player.util.Player;
import com.bueno.domain.entities.round.Round;
import com.bueno.domain.usecases.game.PlayGameUseCase;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.logging.LogManager;
import java.util.stream.Collectors;

public class PlayerCLI extends Player {

    public static void main(String[] args) {
        LogManager.getLogManager().reset();

        Scanner scanner = new Scanner(System.in);

        System.out.println("====== LET'S TRUCO ======");

        System.out.print("Nome do jogador 1 > ");
        String name1 = scanner.nextLine();
        PlayerCLI playerCLI = new PlayerCLI(name1);

        playerCLI.startGame();

        scanner.close();
    }

    public PlayerCLI(String name) {
        super(name);
    }

    public void startGame() {
        cls();
        PlayGameUseCase gameUseCase = new PlayGameUseCase(this, new MineiroBot());

        while (true) {
            final Intel intel = gameUseCase.playNewHand();
            if (intel == null)
                break;
            printGameIntel(intel, 3000);
        }
    }

    @Override
    public Card playCard() {
        Card cardToPlay = null;

        while (cardToPlay == null) {
            cls();
            printGameIntel(getIntel(), 1000);
            Scanner scanner = new Scanner(System.in);

            System.out.print("Carta a jogar [índice] > ");
            try {
                final int cardIndex = Integer.parseInt(scanner.nextLine()) - 1;

                if (cardIndex < 0 || cardIndex > cards.size() - 1) {
                    printErrorMessage("Valor inválido!");
                    continue;
                }

                System.out.print("Descartar [s, n] > ");
                final String choice = scanner.nextLine();
                if (isValidChoice(choice, "s", "n")) {
                    printErrorMessage("Valor inválido!");
                    continue;
                }

                if (choice.equalsIgnoreCase("n"))
                    cardToPlay = cards.remove(cardIndex);
                else
                    cardToPlay = discard(cards.get(cardIndex));
            } catch (Exception e) {
                printErrorMessage("Valor inválido!");
            }
        }
        return cardToPlay;
    }

    private boolean isValidChoice(String choice, String... options) {
        return Arrays.stream(options).noneMatch(choice::equalsIgnoreCase);
    }

    @Override
    public boolean requestTruco() {
        if (getIntel().getHandScore().get() == 12)
            return false;

        cls();
        while (true) {
            printGameIntel(getIntel(), 1000);
            Scanner scanner = new Scanner(System.in);
            System.out.print("Pedir " + getNextHandValueAsString() + " [s, n]: ");
            final String choice = scanner.nextLine().toLowerCase();

            if (isValidChoice(choice, "s", "n")) {
                printErrorMessage("Valor inválido!");
                continue;
            }
            return choice.equalsIgnoreCase("s");
        }
    }

    @Override
    public int getTrucoResponse(HandScore newHandScore) {
        cls();
        while (true) {
            Scanner scanner = new Scanner(System.in);
            System.out.print(getIntel().getOpponentId(this) + " está pedindo "
                    + getNextHandValueAsString() + ". Escolha uma opção [(T)opa, (C)orre, (A)umenta]: ");

            final String choice = scanner.nextLine();

            if (isValidChoice(choice, "t", "c", "a")) {
                printErrorMessage("Valor inválido!");
                continue;
            }

            printGameIntel(getIntel(), 1000);
            return toIntChoice(choice);
        }
    }

    private int toIntChoice(String choice) {
        if (choice.equalsIgnoreCase("c"))
            return -1;
        if (choice.equalsIgnoreCase("a"))
            return 1;
        return 0;
    }

    private String getNextHandValueAsString() {
        return switch (getIntel().getHandScore().get()) {
            case 1 -> "truco";
            case 3 -> "seis";
            case 6 -> "nove";
            case 9 -> "doze";
            default -> throw new GameRuleViolationException("Invalid hand value!");
        };
    }

    @Override
    public boolean getMaoDeOnzeResponse() {
        cls();
        while (true) {
            printGameIntel(getIntel(), 1000);
            Scanner scanner = new Scanner(System.in);
            System.out.print("O jogo está em mão de onze. Você aceita [s, n]: ");
            final String choice = scanner.nextLine().toLowerCase();

            if (isValidChoice(choice, "s", "n")) {
                printErrorMessage("Valor inválido!");
                continue;
            }
            return choice.equalsIgnoreCase("s");
        }
    }

    private void printGameIntel(Intel intel, int delayInMilliseconds) {
        System.out.println("+=======================================+");
        printGameMainInfo(intel);
        printRounds(intel);
        printCardsOpenInTable(intel);
        printVira(intel.getVira());
        printOpponentCardIfAvailable(intel);
        printOwnedCards();
        printResultIfAvailable();
        System.out.println("+=======================================+\n");

        try {
            Thread.sleep(delayInMilliseconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void printGameMainInfo(Intel intel) {
        System.out.println(" Vez do: " + getUsername());
        System.out.println(" Ponto da mão: " + intel.getHandScore().get());
        System.out.println(" Placar: " + getUsername() + " " + getScore() + " x "
                + intel.getOpponentScore(this) + " " + intel.getOpponentId(this));
    }

    private void printRounds(Intel intel) {
        final List<Round> roundsPlayed = intel.getRoundsPlayed();
        if (roundsPlayed.size() > 0) {
            final String roundResults = roundsPlayed.stream()
                    .map(Round::getWinner)
                    .map(possibleWinner -> possibleWinner.orElse(null))
                    .map(winner -> winner != null ? winner.getUsername() : "Empate")
                    .collect(Collectors.joining(" | ", "[ ", " ] "));
            System.out.println("Ganhadores das Rodadas: " + roundResults);
        }
    }

    private void printCardsOpenInTable(Intel intel) {
        final List<Card> openCards = intel.getOpenCards();
        if (openCards.size() > 0) {
            System.out.print(" Cartas na mesa: ");
            openCards.forEach(card -> System.out.print(card + " "));
            System.out.print("\n");
        }
    }

    private void printVira(Card vira) {
        System.out.println(" Vira: " + vira);
    }

    private void printOpponentCardIfAvailable(Intel intel) {
        final Optional<Card> cardToPlayAgainst = intel.getCardToPlayAgainst();
        cardToPlayAgainst.ifPresent(card -> System.out.println(" Carta do Oponente: " + card));
    }

    private void printOwnedCards() {
        System.out.print(" Cartas na mão: ");
        for (int i = 0; i < cards.size(); i++) {
            System.out.print((i + 1) + ") " + cards.get(i) + "\t");
        }
        System.out.print("\n");
    }

    private void printResultIfAvailable() {
        final Optional<HandResult> potentialResult = getIntel().getResult();
        if (potentialResult.isPresent()) {
            final String resultString = potentialResult.get().getWinner()
                    .map(winner -> winner.getUsername().concat(" VENCEU!").toUpperCase())
                    .orElse("EMPATE.");
            System.out.println(" RESULTADO: " + resultString);
        }
    }

    private void printErrorMessage(String message) {
        Scanner scanner = new Scanner(System.in);
        cls();
        System.out.println(message);
        scanner.nextLine();
        cls();
    }

    private static void cls() {
        for (int i = 0; i < 15; ++i)
            System.out.println();
    }
}

