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

package io.reactivex.internal.operators.observable;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import io.reactivex.*;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.*;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;

public class ObservableRepeatTest {

    @Test(timeout = 2000)
    public void testRepetition() {
        int num = 10;
        final AtomicInteger count = new AtomicInteger();
        int value = Observable.unsafeCreate(new ObservableSource<Integer>() {

            @Override
            public void subscribe(final Observer<? super Integer> o) {
                o.onNext(count.incrementAndGet());
                o.onComplete();
            }
        }).repeat().subscribeOn(Schedulers.computation())
        .take(num).blockingLast();

        assertEquals(num, value);
    }

    @Test(timeout = 2000)
    public void testRepeatTake() {
        Observable<Integer> xs = Observable.just(1, 2);
        Object[] ys = xs.repeat().subscribeOn(Schedulers.newThread()).take(4).toList().blockingGet().toArray();
        assertArrayEquals(new Object[] { 1, 2, 1, 2 }, ys);
    }

    @Test(timeout = 20000)
    public void testNoStackOverFlow() {
        Observable.just(1).repeat().subscribeOn(Schedulers.newThread()).take(100000).blockingLast();
    }

    @Test
    public void testRepeatTakeWithSubscribeOn() throws InterruptedException {

        final AtomicInteger counter = new AtomicInteger();
        Observable<Integer> oi = Observable.unsafeCreate(new ObservableSource<Integer>() {

            @Override
            public void subscribe(Observer<? super Integer> sub) {
                sub.onSubscribe(Disposables.empty());
                counter.incrementAndGet();
                sub.onNext(1);
                sub.onNext(2);
                sub.onComplete();
            }
        }).subscribeOn(Schedulers.newThread());

        Object[] ys = oi.repeat().subscribeOn(Schedulers.newThread()).map(new Function<Integer, Integer>() {

            @Override
            public Integer apply(Integer t1) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return t1;
            }

        }).take(4).toList().blockingGet().toArray();

        assertEquals(2, counter.get());
        assertArrayEquals(new Object[] { 1, 2, 1, 2 }, ys);
    }

    @Test(timeout = 2000)
    public void testRepeatAndTake() {
        Observer<Object> o = TestHelper.mockObserver();

        Observable.just(1).repeat().take(10).subscribe(o);

        verify(o, times(10)).onNext(1);
        verify(o).onComplete();
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test(timeout = 2000)
    public void testRepeatLimited() {
        Observer<Object> o = TestHelper.mockObserver();

        Observable.just(1).repeat(10).subscribe(o);

        verify(o, times(10)).onNext(1);
        verify(o).onComplete();
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test(timeout = 2000)
    public void testRepeatError() {
        Observer<Object> o = TestHelper.mockObserver();

        Observable.error(new TestException()).repeat(10).subscribe(o);

        verify(o).onError(any(TestException.class));
        verify(o, never()).onNext(any());
        verify(o, never()).onComplete();

    }

    @Test(timeout = 2000)
    public void testRepeatZero() {
        Observer<Object> o = TestHelper.mockObserver();

        Observable.just(1).repeat(0).subscribe(o);

        verify(o).onComplete();
        verify(o, never()).onNext(any());
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test(timeout = 2000)
    public void testRepeatOne() {
        Observer<Object> o = TestHelper.mockObserver();

        Observable.just(1).repeat(1).subscribe(o);

        verify(o).onComplete();
        verify(o, times(1)).onNext(any());
        verify(o, never()).onError(any(Throwable.class));
    }

    /** Issue #2587. */
    @Test
    public void testRepeatAndDistinctUnbounded() {
        Observable<Integer> src = Observable.fromIterable(Arrays.asList(1, 2, 3, 4, 5))
                .take(3)
                .repeat(3)
                .distinct();

        TestObserver<Integer> to = new TestObserver<Integer>();

        src.subscribe(to);

        to.assertNoErrors();
        to.assertTerminated();
        to.assertValues(1, 2, 3);
    }

    /** Issue #2844: wrong target of request. */
    @Test(timeout = 3000)
    public void testRepeatRetarget() {
        final List<Integer> concatBase = new ArrayList<Integer>();
        TestObserver<Integer> to = new TestObserver<Integer>();
        Observable.just(1, 2)
        .repeat(5)
        .concatMap(new Function<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> apply(Integer x) {
                System.out.println("testRepeatRetarget -> " + x);
                concatBase.add(x);
                return Observable.<Integer>empty()
                        .delay(200, TimeUnit.MILLISECONDS);
            }
        })
        .subscribe(to);

        to.awaitTerminalEvent();
        to.assertNoErrors();
        to.assertNoValues();

        assertEquals(Arrays.asList(1, 2, 1, 2, 1, 2, 1, 2, 1, 2), concatBase);
    }

    @Test
    public void repeatUntil() {
        Observable.just(1)
        .repeatUntil(new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() throws Exception {
                return false;
            }
        })
        .take(5)
        .test()
        .assertResult(1, 1, 1, 1, 1);
    }

    @Test
    public void repeatLongPredicateInvalid() {
        try {
            Observable.just(1).repeat(-99);
            fail("Should have thrown");
        } catch (IllegalArgumentException ex) {
            assertEquals("times >= 0 required but it was -99", ex.getMessage());
        }
    }

    @Test
    public void repeatUntilError() {
        Observable.error(new TestException())
        .repeatUntil(new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() throws Exception {
                return true;
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void repeatUntilFalse() {
        Observable.just(1)
        .repeatUntil(new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() throws Exception {
                return true;
            }
        })
        .test()
        .assertResult(1);
    }

    @Test
    public void repeatUntilSupplierCrash() {
        Observable.just(1)
        .repeatUntil(new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() throws Exception {
                throw new TestException();
            }
        })
        .test()
        .assertFailure(TestException.class, 1);
    }

    @Test
    public void shouldDisposeInnerObservable() {
      final PublishSubject<Object> subject = PublishSubject.create();
      final Disposable disposable = Observable.just("Leak")
          .repeatWhen(new Function<Observable<Object>, ObservableSource<Object>>() {
            @Override
            public ObservableSource<Object> apply(Observable<Object> completions) throws Exception {
                return completions.switchMap(new Function<Object, ObservableSource<Object>>() {
                    @Override
                    public ObservableSource<Object> apply(Object ignore) throws Exception {
                        return subject;
                    }
                });
            }
        })
          .subscribe();

      assertTrue(subject.hasObservers());
      disposable.dispose();
      assertFalse(subject.hasObservers());
    }

    @Test
    public void testRepeatWhen() {
        Observable.error(new TestException())
        .repeatWhen(new Function<Observable<Object>, ObservableSource<Object>>() {
            @Override
            public ObservableSource<Object> apply(Observable<Object> v) throws Exception {
                return v.delay(10, TimeUnit.SECONDS);
            }
        })
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
    }

    @Test
    public void whenTake() {
        Observable.range(1, 3).repeatWhen(new Function<Observable<Object>, ObservableSource<Object>>() {
            @Override
            public ObservableSource<Object> apply(Observable<Object> handler) throws Exception {
                return handler.take(2);
            }
        })
        .test()
        .assertResult(1, 2, 3, 1, 2, 3);
    }

    @Test
    public void handlerError() {
        Observable.range(1, 3)
        .repeatWhen(new Function<Observable<Object>, ObservableSource<Object>>() {
            @Override
            public ObservableSource<Object> apply(Observable<Object> v) throws Exception {
                return v.map(new Function<Object, Object>() {
                    @Override
                    public Object apply(Object w) throws Exception {
                        throw new TestException();
                    }
                });
            }
        })
        .test()
        .assertFailure(TestException.class, 1, 2, 3);
    }
}
