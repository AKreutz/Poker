import Toybox.Communications;
import Toybox.Lang;
import Toybox.Time;
import Toybox.WatchUi;

// Sends commands to the phone and notices when one could not be delivered.
class PhoneLink extends Communications.ConnectionListener {
    private var mModel as SessionModel;
    private var mCounter as Number = 0;
    private var mIdBase as Long;

    function initialize(model as SessionModel) {
        ConnectionListener.initialize();
        mModel = model;
        // The phone drops a message id it has already seen, so ids must not repeat across app runs.
        mIdBase = Time.now().value().toLong() * 1000l;
    }

    function sync() as Void {
        transmit({ "t" => "sync" });
    }

    function adjust(player as PlayerCount, delta as Number) as Void {
        mCounter++;
        transmit({ "t" => "adj", "i" => player.id, "d" => delta, "m" => mIdBase + mCounter });
    }

    function onComplete() as Void {
        mModel.phoneReachable = true;
    }

    function onError() as Void {
        mModel.phoneReachable = false;
        WatchUi.requestUpdate();
    }

    private function transmit(message as Communications.TransmitType) as Void {
        Communications.transmit(message, null, self);
    }
}
