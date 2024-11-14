/*
 *  Copyright (C) 2024 Matheus Sabatini Pacífico - IFSP/SCL
 *  Copyright (C) 2024 Dylan Tomaz Petrucelli - IFSP/SCL
 *
 *  Contact: matheus <dot> pacifico <at> aluno <dot> ifsp <dot> edu <dot> br
 *  Contact: dylan <dot> petrucelli <at> aluno <dot> ifsp <dot> edu <dot> br
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

package com.matheus.dylan.superidolbot;

import com.bueno.spi.model.CardToPlay;
import com.bueno.spi.model.GameIntel;
import com.bueno.spi.model.TrucoCard;
import com.bueno.spi.service.BotServiceProvider;

import java.util.Comparator;
import java.util.Optional;

public class SuperIdolBot implements BotServiceProvider {
    private static final int GOOD_HAND_STRENGTH_THRESHOLD = 21;
    private static final int AVERAGE_HAND_STRENGTH_THRESHOLD = 14;
    private static final int BAD_HAND_STRENGTH_THRESHOLD = 9;
    private static final int STRONG_CARD_THRESHOLD = 9;

    @Override
    public boolean getMaoDeOnzeResponse(GameIntel intel) {
        int manilhas = countManilhas(intel);
        int strongCards = countStrongCard(intel);

        //if the opponent would win if we forfeit, we play the hand;
        if (intel.getOpponentScore() == 11) {
            return true;
        }
        //if the opponent wins if we play and lose, we only play if we have a good hand;
        if (intel.getOpponentScore() >= 9) {
            if (manilhas >= 2) return true;
            if (manilhas > 0 && strongCards > 0) return true;
            if (strongCards == 3) return true;
        }

        //if we won't lose the game by losing this hand, we play if we have a good hand overall;
        return totalHandValue(intel) > GOOD_HAND_STRENGTH_THRESHOLD;
    }

    @Override
    public boolean decideIfRaises(GameIntel intel) {
        int roundCount = intel.getRoundResults().size();
        int manilhas = countManilhas(intel);
        int strongCards = countStrongCard(intel);

        //we never raise on the first round
        if (roundCount == 0) return false;

        //on the second round, raise if we won the first round or if both remaining cards are strong;
        if (roundCount == 1){
            if (wonFirstRound(intel)) return true;
            if (manilhas == 2) return true;
            if (manilhas > 0  && strongCards > 0) return true;
        }

        //if the opponent played their card first and our last card is stronger, we raise;
        if (roundCount == 2){
            Optional<TrucoCard> opponentCard = intel.getOpponentCard();
            if (opponentCard.isPresent()){
                TrucoCard myCard = intel.getCards().get(0);
                return myCard.relativeValue(intel.getVira()) > opponentCard.get().relativeValue(intel.getVira());
            }
            //if our last card is strong or a manilha, we raise;
            if (strongCards > 0) return true;
            return manilhas > 0;
        }

        return false;
    }

    @Override
    public CardToPlay chooseCard(GameIntel intel) {
        int roundCount = intel.getRoundResults().size();
        int manilhas = countManilhas(intel);

        if (roundCount == 0) { //first round
            Optional<TrucoCard> opponentCard = intel.getOpponentCard();
            //if the opponent played the first card:
            if (opponentCard.isPresent()) {
                // current optimal card = weakest card that beats the opponent card or else the WEAKEST from hand;
                TrucoCard optimalCard = intel.getCards().stream()
                        .filter(card -> card.relativeValue(intel.getVira()) > opponentCard.get().relativeValue(intel.getVira()))
                        .min(Comparator.comparingInt(card -> card.relativeValue(intel.getVira())))
                        .orElse(weakestCard(intel));

                return CardToPlay.of(optimalCard);
            }
            //if the hand is really bad, play the worst card;
            if (totalHandValue(intel) <= BAD_HAND_STRENGTH_THRESHOLD) return CardToPlay.of(weakestCard(intel));
            //otherwise, play the best card;
            return CardToPlay.of(strongestCard(intel));
        }

        if (roundCount == 1) { //round 2
            Optional<TrucoCard> opponentCard = intel.getOpponentCard(); //if the opponent plays first, it means they won first round;
            if (opponentCard.isPresent()){
                // current optimal card = weakest card that beats the opponent card or else the STRONGEST from hand;
                TrucoCard optimalCard = intel.getCards().stream()
                        .filter(card -> card.relativeValue(intel.getVira()) >  opponentCard.get().relativeValue(intel.getVira()))
                        .min(Comparator.comparingInt(card -> card.relativeValue(intel.getVira())))
                        .orElse(strongestCard(intel));

                return CardToPlay.of(optimalCard);
            }

            // if we have a good card AND a manilha, this will save a manilha for the last round
            if (totalHandValue(intel) <= AVERAGE_HAND_STRENGTH_THRESHOLD && manilhas > 0) return CardToPlay.of(weakestCard(intel));

            return CardToPlay.of(strongestCard(intel)); //otherwise, play the strongest card;
        }
        return CardToPlay.of(intel.getCards().get(0)); //only runs on last round, plays the only remaining card (duh);
    }

    @Override
    public int getRaiseResponse(GameIntel intel) {
        int roundCount = intel.getRoundResults().size();
        int manilhas = countManilhas(intel);
        int strongCards = countStrongCard(intel);

        if (roundCount == 0) { // first round
            if (manilhas == 2) return 1; // 2 manilhas = raise;
            if (manilhas > 0 && strongCards > 0) return 1; //good hand overall = raise;

            if (manilhas > 0) return 0; // 1 manilha = accept truco;
            if (totalHandValue(intel) > GOOD_HAND_STRENGTH_THRESHOLD) return 0; // if the overall hand value is good, accept truco;

            return -1; // otherwise, run;
        }

        if (roundCount == 1){ // second round has the exact same logic, except for the last "if"
            if (manilhas == 2) return 1;
            if (manilhas > 0 && strongCards > 0) return 1;
            if (wonFirstRound(intel) && strongCards > 0) return 1;

            if (manilhas > 0) return 0;
            if (totalHandValue(intel) > AVERAGE_HAND_STRENGTH_THRESHOLD) return 0;
            //the hand value required to accept truco is lowered from good to average

            return -1;
        }

        if (roundCount == 2){
            if (manilhas > 0) return 1; // if we have a manilha, raise
            if (wonFirstRound(intel) && totalHandValue(intel) >= BAD_HAND_STRENGTH_THRESHOLD) return 1;
            //if we won the first round (tiebreaker) and our hand isnt bad, raise

            if (totalHandValue(intel) >= BAD_HAND_STRENGTH_THRESHOLD) return 0;
            //if we didn't win first round and our hand isn't bad, check;

            return -1; //run
        }

        return -1;
    }

    @Override
    public String getName() {
        return "SuperIdol的笑容Bot";
    }

    private boolean wonFirstRound(GameIntel intel) {
        return intel.getRoundResults().get(0) == GameIntel.RoundResult.WON;
    }

    private int countManilhas(GameIntel intel) {
        return (int) intel.getCards().stream()
                .filter(card -> card.isManilha(intel.getVira()))
                .count();
    }

    private int countStrongCard(GameIntel intel){
        return (int) intel.getCards().stream()
                .filter(card -> card.relativeValue(intel.getVira()) >= STRONG_CARD_THRESHOLD)
                .count();
    }

    private int totalHandValue(GameIntel intel) {
        return intel.getCards().stream().mapToInt(card -> card.relativeValue(intel.getVira()))
                .sum();
    }

    private TrucoCard strongestCard(GameIntel intel) {
        return intel.getCards().stream()
                .max(Comparator.comparingInt(card -> card.relativeValue(intel.getVira())))
                .orElse(intel.getCards().get(0));
    }

    private TrucoCard weakestCard(GameIntel intel) {
        return intel.getCards().stream()
                .min(Comparator.comparingInt(card -> card.relativeValue(intel.getVira())))
                .orElse(intel.getCards().get(0));
    }
}


