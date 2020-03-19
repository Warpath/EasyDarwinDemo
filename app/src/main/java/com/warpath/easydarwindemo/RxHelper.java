package com.warpath.easydarwindemo;

import android.support.annotation.NonNull;

import org.easydarwin.util.AbstractSubscriber;
import org.reactivestreams.Publisher;

import io.reactivex.Single;
import io.reactivex.subjects.PublishSubject;

/**
 * Created by warpath on 2020/3/19.
 */
public class RxHelper {
    static boolean IGNORE_ERROR = false;

    public static <T> Single<T> single(@NonNull Publisher<T> t, @NonNull T defaultValueIfNotNull) {
        if (defaultValueIfNotNull != null) return Single.just(defaultValueIfNotNull);
        final PublishSubject sub = PublishSubject.create();
        t.subscribe(new AbstractSubscriber<T>() {
            @Override
            public void onNext(T t) {
                super.onNext(t);
                sub.onNext(t);
            }

            @Override
            public void onError(Throwable t) {
                if (IGNORE_ERROR) {
                    super.onError(t);
                    sub.onComplete();
                } else {
                    sub.onError(t);
                }
            }

            @Override
            public void onComplete() {
                super.onComplete();
                sub.onComplete();
            }
        });
        return sub.firstOrError();
    }
}
