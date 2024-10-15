package com.brito.macena.boteco.interfaces;

import com.brito.macena.boteco.utils.Status;
import com.bueno.spi.model.GameIntel;
import com.bueno.spi.model.TrucoCard;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AnalyzerTest {

    @Nested
    @DisplayName("myHand method tests")
    class MyHandMethodTests {
        @Test
        @DisplayName("Returns EXCELLENT when no cards")
        void returnsExcellentWhenNoCards() {
            GameIntel intel = mock(GameIntel.class);
            when(intel.getCards()).thenReturn(List.of());

            Analyzer analyzer = new AnalyzerImpl();
            Status result = analyzer.myHand(intel);

            assertThat(result).isEqualTo(Status.EXCELLENT);
        }

        @Test
        @DisplayName("Returns threeCardsHandler when three cards")
        void returnsThreeCardsHandlerWhenThreeCards() {
            GameIntel intel = mock(GameIntel.class);
            List<TrucoCard> cards = List.of(mock(TrucoCard.class), mock(TrucoCard.class), mock(TrucoCard.class));
            when(intel.getCards()).thenReturn(cards);

            Analyzer analyzer = new AnalyzerImpl();
            Status result = analyzer.myHand(intel);

            assertThat(result).isEqualTo(Status.GOOD);
        }

        @Test
        @DisplayName("Returns twoCardsHandler when two cards")
        void returnsTwoCardsHandlerWhenTwoCards() {
            GameIntel intel = mock(GameIntel.class);
            List<TrucoCard> cards = List.of(mock(TrucoCard.class), mock(TrucoCard.class));
            when(intel.getCards()).thenReturn(cards);

            Analyzer analyzer = new AnalyzerImpl();
            Status result = analyzer.myHand(intel);

            assertThat(result).isEqualTo(Status.MEDIUM);
        }

        @Test
        @DisplayName("Returns oneCardHandler when one card")
        void returnsOneCardHandlerWhenOneCard() {
            GameIntel intel = mock(GameIntel.class);
            List<TrucoCard> cards = List.of(mock(TrucoCard.class));
            when(intel.getCards()).thenReturn(cards);

            Analyzer analyzer = new AnalyzerImpl();
            Status result = analyzer.myHand(intel);

            assertThat(result).isEqualTo(Status.BAD);
        }
    }

    private static class AnalyzerImpl extends Analyzer {
        @Override
        public Status threeCardsHandler() {
            return Status.GOOD;
        }

        @Override
        public Status twoCardsHandler() {
            return Status.MEDIUM;
        }

        @Override
        public Status oneCardHandler() {
            return Status.BAD;
        }
    }
}