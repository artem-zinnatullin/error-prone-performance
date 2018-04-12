/**
 * Copyright (c) 2016-present, RxJava Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */
package io.reactivex.internal.util;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import io.reactivex.*;
import io.reactivex.disposables.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.observers.TestObserver;

public class HalfSerializerObserverTest {

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void reentrantOnNextOnNext() {
        final AtomicInteger wip = new AtomicInteger();
        final AtomicThrowable error = new AtomicThrowable();

        final Observer[] a = { null };

        final TestObserver ts = new TestObserver();

        Observer s = new Observer() {
            @Override
            public void onSubscribe(Disposable s) {
                ts.onSubscribe(s);
            }

            @Override
            public void onNext(Object t) {
                if (t.equals(1)) {
                    HalfSerializer.onNext(a[0], 2, wip, error);
                }
                ts.onNext(t);
            }

            @Override
            public void onError(Throwable t) {
                ts.onError(t);
            }

            @Override
            public void onComplete() {
                ts.onComplete();
            }
        };

        a[0] = s;

        s.onSubscribe(Disposables.empty());

        HalfSerializer.onNext(s, 1, wip, error);

        ts.assertValue(1).assertNoErrors().assertNotComplete();
    }

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void reentrantOnNextOnError() {
        final AtomicInteger wip = new AtomicInteger();
        final AtomicThrowable error = new AtomicThrowable();

        final Observer[] a = { null };

        final TestObserver ts = new TestObserver();

        Observer s = new Observer() {
            @Override
            public void onSubscribe(Disposable s) {
                ts.onSubscribe(s);
            }

            @Override
            public void onNext(Object t) {
                if (t.equals(1)) {
                    HalfSerializer.onError(a[0], new TestException(), wip, error);
                }
                ts.onNext(t);
            }

            @Override
            public void onError(Throwable t) {
                ts.onError(t);
            }

            @Override
            public void onComplete() {
                ts.onComplete();
            }
        };

        a[0] = s;

        s.onSubscribe(Disposables.empty());

        HalfSerializer.onNext(s, 1, wip, error);

        ts.assertFailure(TestException.class, 1);
    }

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void reentrantOnNextOnComplete() {
        final AtomicInteger wip = new AtomicInteger();
        final AtomicThrowable error = new AtomicThrowable();

        final Observer[] a = { null };

        final TestObserver ts = new TestObserver();

        Observer s = new Observer() {
            @Override
            public void onSubscribe(Disposable s) {
                ts.onSubscribe(s);
            }

            @Override
            public void onNext(Object t) {
                if (t.equals(1)) {
                    HalfSerializer.onComplete(a[0], wip, error);
                }
                ts.onNext(t);
            }

            @Override
            public void onError(Throwable t) {
                ts.onError(t);
            }

            @Override
            public void onComplete() {
                ts.onComplete();
            }
        };

        a[0] = s;

        s.onSubscribe(Disposables.empty());

        HalfSerializer.onNext(s, 1, wip, error);

        ts.assertResult(1);
    }

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void reentrantErrorOnError() {
        final AtomicInteger wip = new AtomicInteger();
        final AtomicThrowable error = new AtomicThrowable();

        final Observer[] a = { null };

        final TestObserver ts = new TestObserver();

        Observer s = new Observer() {
            @Override
            public void onSubscribe(Disposable s) {
                ts.onSubscribe(s);
            }

            @Override
            public void onNext(Object t) {
                ts.onNext(t);
            }

            @Override
            public void onError(Throwable t) {
                ts.onError(t);
                HalfSerializer.onError(a[0], new IOException(), wip, error);
            }

            @Override
            public void onComplete() {
                ts.onComplete();
            }
        };

        a[0] = s;

        s.onSubscribe(Disposables.empty());

        HalfSerializer.onError(s, new TestException(), wip, error);

        ts.assertFailure(TestException.class);
    }

    @Test
    public void onNextOnCompleteRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {

            final AtomicInteger wip = new AtomicInteger();
            final AtomicThrowable error = new AtomicThrowable();

            final TestObserver<Integer> to = new TestObserver<Integer>();
            to.onSubscribe(Disposables.empty());

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    HalfSerializer.onNext(to, 1, wip, error);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    HalfSerializer.onComplete(to, wip, error);
                }
            };

            TestHelper.race(r1, r2);

            to.assertComplete().assertNoErrors();

            assertTrue(to.valueCount() <= 1);
        }
    }

    @Test
    public void onErrorOnCompleteRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {

            final AtomicInteger wip = new AtomicInteger();
            final AtomicThrowable error = new AtomicThrowable();

            final TestObserver<Integer> to = new TestObserver<Integer>();

            to.onSubscribe(Disposables.empty());

            final TestException ex = new TestException();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    HalfSerializer.onError(to, ex, wip, error);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    HalfSerializer.onComplete(to, wip, error);
                }
            };

            TestHelper.race(r1, r2);

            if (to.completions() != 0) {
                to.assertResult();
            } else {
                to.assertFailure(TestException.class);
            }
        }
    }

}
