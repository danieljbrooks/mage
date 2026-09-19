package org.mage.magezero;

import mage.cards.Card;
import mage.constants.WatcherScope;
import mage.game.Game;
import mage.game.events.GameEvent;
import mage.watchers.Watcher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Records the name of every card each player draws over a whole game, opening hand included.
 * Used for per-card "game in hand" (GIH) statistics comparable to 17lands. Unlike most watchers
 * it is never reset between turns.
 */
public class CardsDrawnWatcher extends Watcher {

    private final Map<UUID, List<String>> drawn = new HashMap<>();

    public CardsDrawnWatcher() {
        super(WatcherScope.GAME);
    }

    @Override
    public void watch(GameEvent event, Game game) {
        if (event.getType() != GameEvent.EventType.DREW_CARD) {
            return;
        }
        Card card = game.getCard(event.getTargetId());
        if (card != null) {
            drawn.computeIfAbsent(event.getPlayerId(), k -> new ArrayList<>()).add(card.getName());
        }
    }

    @Override
    public void reset() {
        // keep the whole game's history
    }

    public List<String> getDrawn(UUID playerId) {
        return drawn.getOrDefault(playerId, new ArrayList<>());
    }
}
