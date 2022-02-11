package net.bdavies.tomcat.server;

import lombok.extern.slf4j.Slf4j;
import rx.subjects.PublishSubject;

/**
 * @author ben.davies
 */
@Slf4j
public class ShutdownHandle {
    private final PublishSubject<Void> subject = PublishSubject.create();

    public void subscribe(Runnable runnable) {
        subject.subscribe(a -> runnable.run());
    }

    public void runShutdownHooks() {
        subject.onNext(null);
    }
}
