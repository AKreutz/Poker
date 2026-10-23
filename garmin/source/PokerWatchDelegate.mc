import Toybox.Lang;
import Toybox.WatchUi;

// UP and DOWN pick a player, START counts a hand won for them, and holding UP (the menu gesture) takes one back.
// BACK leaves the app.
class PokerWatchDelegate extends WatchUi.BehaviorDelegate {
    private var mModel as SessionModel;
    private var mLink as PhoneLink;

    function initialize(model as SessionModel, link as PhoneLink) {
        BehaviorDelegate.initialize();
        mModel = model;
        mLink = link;
    }

    function onSelect() as Boolean {
        return adjustSelected(1);
    }

    function onMenu() as Boolean {
        return adjustSelected(-1);
    }

    function onNextPage() as Boolean {
        return moveSelection(1);
    }

    function onPreviousPage() as Boolean {
        return moveSelection(-1);
    }

    private function adjustSelected(delta as Number) as Boolean {
        var player = mModel.selectedPlayer();
        if (player != null) {
            // The count changes when the phone confirms it, not here.
            mLink.adjust(player, delta);
        }
        return true;
    }

    private function moveSelection(step as Number) as Boolean {
        mModel.moveSelection(step);
        WatchUi.requestUpdate();
        return true;
    }
}
