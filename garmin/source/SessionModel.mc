import Toybox.Lang;

// One player of the open session, as the phone last reported it.
class PlayerCount {
    public var id as String;
    public var name as String;
    public var handsWon as Number;

    function initialize(id as String, name as String, handsWon as Number) {
        self.id = id;
        self.name = name;
        self.handsWon = handsWon;
    }
}

// The phone owns the counts; this only mirrors the latest state message:
// {"t": "state", "rec": Boolean, "hp": n, "p": [{"i": id, "n": name, "w": n}, ...]} or {"t": "idle"}.
class SessionModel {
    public var players as Array<PlayerCount> = [];
    public var handsPlayed as Number = 0;
    public var hasState as Boolean = false;
    public var isIdle as Boolean = false;
    public var phoneReachable as Boolean = true;
    private var mSelectedId as String? = null;

    // Returns true when a count changed, so the caller can confirm it with a buzz.
    function update(data as Dictionary) as Boolean {
        phoneReachable = true;
        hasState = true;
        var type = data["t"] as String?;
        if (type == null || type.equals("idle")) {
            isIdle = true;
            players = [];
            handsPlayed = 0;
            mSelectedId = null;
            return false;
        }
        isIdle = false;

        var list = data["p"];
        var fresh = [] as Array<PlayerCount>;
        if (list instanceof Array) {
            for (var i = 0; i < list.size(); i++) {
                var entry = list[i];
                if (entry instanceof Dictionary) {
                    fresh.add(new PlayerCount(entry["i"] as String, entry["n"] as String, entry["w"] as Number));
                }
            }
        }
        var changed = hasCountChanged(fresh);
        players = fresh;
        handsPlayed = data["hp"] != null ? data["hp"] as Number : 0;
        if (selectedIndex() < 0 && players.size() > 0) {
            mSelectedId = players[0].id;
        }
        return changed;
    }

    function selectedIndex() as Number {
        for (var i = 0; i < players.size(); i++) {
            if (players[i].id.equals(mSelectedId)) {
                return i;
            }
        }
        return -1;
    }

    function selectedPlayer() as PlayerCount? {
        var index = selectedIndex();
        return index < 0 ? null : players[index];
    }

    // Moves the selection, wrapping around at either end.
    function moveSelection(step as Number) as Void {
        if (players.size() == 0) {
            return;
        }
        var index = (selectedIndex() + step + players.size()) % players.size();
        mSelectedId = players[index].id;
    }

    // Only a change to a player already on the list counts; a new session or player is not a tap.
    private function hasCountChanged(fresh as Array<PlayerCount>) as Boolean {
        for (var i = 0; i < fresh.size(); i++) {
            for (var j = 0; j < players.size(); j++) {
                if (players[j].id.equals(fresh[i].id) && players[j].handsWon != fresh[i].handsWon) {
                    return true;
                }
            }
        }
        return false;
    }
}
