import Toybox.Application;
import Toybox.Communications;
import Toybox.Lang;
import Toybox.Timer;
import Toybox.WatchUi;

// Remote control for the hands-won counters in the Android app: the phone owns the counts, this app shows them and
// sends taps.
class PokerWatchApp extends Application.AppBase {
    private var mModel as SessionModel = new SessionModel();
    private var mLink as PhoneLink = new PhoneLink(mModel);
    private var mRetryTimer as Timer.Timer? = null;

    function initialize() {
        AppBase.initialize();
    }

    function getInitialView() as [WatchUi.Views] or [WatchUi.Views, WatchUi.InputDelegates] {
        var view = new PokerWatchView(mModel);
        Communications.registerForPhoneAppMessages(view.method(:onPhoneMessage));
        // Ask for the current state; the phone also pushes every change by itself.
        mLink.sync();
        // The phone may not be listening yet (it starts once a session is open), so keep asking until it answers.
        var timer = new Timer.Timer();
        timer.start(method(:onRetryTick), 5000, true);
        mRetryTimer = timer;
        return [view, new PokerWatchDelegate(mModel, mLink)];
    }

    function onRetryTick() as Void {
        if (mModel.hasState) {
            return;
        }
        mLink.sync();
    }

    function onStop(state as Dictionary?) as Void {
        if (mRetryTimer != null) {
            mRetryTimer.stop();
            mRetryTimer = null;
        }
        Communications.registerForPhoneAppMessages(null);
    }
}
