import Toybox.Attention;
import Toybox.Communications;
import Toybox.Graphics;
import Toybox.Lang;
import Toybox.WatchUi;

// A list of the session's players with their hands won, the selected one highlighted. Sized for the Forerunner 745's
// round 240x240 screen, so the list sits in the middle where the circle is widest.
class PokerWatchView extends WatchUi.View {
    private const VISIBLE_ROWS = 4;
    private var mModel as SessionModel;
    private var mFirstRow as Number = 0;

    function initialize(model as SessionModel) {
        View.initialize();
        mModel = model;
    }

    function onPhoneMessage(message as Communications.PhoneAppMessage) as Void {
        var data = message.data;
        if (!(data instanceof Dictionary)) {
            return;
        }
        if (mModel.update(data) && Attention has :vibrate) {
            // A short buzz confirms that the phone took the tap.
            Attention.vibrate([new Attention.VibeProfile(50, 80)]);
        }
        WatchUi.requestUpdate();
    }

    function onUpdate(dc as Graphics.Dc) as Void {
        dc.setColor(Graphics.COLOR_WHITE, Graphics.COLOR_BLACK);
        dc.clear();
        var w = dc.getWidth();
        var h = dc.getHeight();
        var center = Graphics.TEXT_JUSTIFY_CENTER | Graphics.TEXT_JUSTIFY_VCENTER;

        if (mModel.players.size() == 0) {
            var message = !mModel.phoneReachable ? Rez.Strings.NoPhone : (mModel.isIdle ? Rez.Strings.Idle : Rez.Strings.Waiting);
            dc.drawText(w / 2, h / 2, Graphics.FONT_SMALL, WatchUi.loadResource(message), center);
            return;
        }

        dc.setColor(Graphics.COLOR_LT_GRAY, Graphics.COLOR_TRANSPARENT);
        dc.drawText(w / 2, h * 0.11, Graphics.FONT_XTINY,
            WatchUi.loadResource(Rez.Strings.HandsPlayed) + ": " + mModel.handsPlayed, center);

        drawRows(dc, w, h);

        if (!mModel.phoneReachable) {
            dc.setColor(Graphics.COLOR_RED, Graphics.COLOR_TRANSPARENT);
            dc.drawText(w / 2, h * 0.93, Graphics.FONT_XTINY, WatchUi.loadResource(Rez.Strings.NoPhone), center);
        }
    }

    private function drawRows(dc as Graphics.Dc, w as Number, h as Number) as Void {
        var players = mModel.players;
        var selected = mModel.selectedIndex();
        // Scroll just far enough to keep the selection on screen.
        if (selected < mFirstRow) {
            mFirstRow = selected;
        } else if (selected >= mFirstRow + VISIBLE_ROWS) {
            mFirstRow = selected - VISIBLE_ROWS + 1;
        }
        mFirstRow = max(0, min(mFirstRow, players.size() - VISIBLE_ROWS));

        var top = h * 0.2;
        var rowHeight = h * 0.15;
        var left = w * 0.12;
        var right = w * 0.88;
        var last = min(players.size(), mFirstRow + VISIBLE_ROWS);
        for (var i = mFirstRow; i < last; i++) {
            var y = top + (i - mFirstRow) * rowHeight;
            var isSelected = i == selected;
            if (isSelected) {
                dc.setColor(Graphics.COLOR_DK_GREEN, Graphics.COLOR_TRANSPARENT);
                dc.fillRoundedRectangle(left - 6, y, right - left + 12, rowHeight - 3, 8);
            }
            dc.setColor(Graphics.COLOR_WHITE, Graphics.COLOR_TRANSPARENT);
            var middle = y + (rowHeight - 3) / 2;
            dc.drawText(left, middle, Graphics.FONT_SMALL, players[i].name,
                Graphics.TEXT_JUSTIFY_LEFT | Graphics.TEXT_JUSTIFY_VCENTER);
            dc.drawText(right, middle, Graphics.FONT_SMALL, players[i].handsWon.toString(),
                Graphics.TEXT_JUSTIFY_RIGHT | Graphics.TEXT_JUSTIFY_VCENTER);
        }

        // Arrows show that more players are off-screen.
        dc.setColor(Graphics.COLOR_LT_GRAY, Graphics.COLOR_TRANSPARENT);
        if (mFirstRow > 0) {
            dc.fillPolygon([[w / 2 - 6, top - 4], [w / 2 + 6, top - 4], [w / 2, top - 11]]);
        }
        if (last < players.size()) {
            var bottom = top + VISIBLE_ROWS * rowHeight;
            dc.fillPolygon([[w / 2 - 6, bottom], [w / 2 + 6, bottom], [w / 2, bottom + 7]]);
        }
    }

    private function min(a as Number, b as Number) as Number {
        return a < b ? a : b;
    }

    private function max(a as Number, b as Number) as Number {
        return a > b ? a : b;
    }
}
