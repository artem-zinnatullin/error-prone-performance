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

import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

import org.junit.Test;
import org.mockito.Mockito;

import io.reactivex.*;
import io.reactivex.disposables.*;
import io.reactivex.functions.Function;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.Schedulers;

public class ObservableOnExceptionResumeNextViaObservableTest {

    @Test
    public void testResumeNextWithException() {
        // Trigger failure on second element
        TestObservable f = new TestObservable("one", "EXCEPTION", "two", "three");
        Observable<String> w = Observable.unsafeCreate(f);
        Observable<String> resume = Observable.just("twoResume", "threeResume");
        Observable<String> observable = w.onExceptionResumeNext(resume);

        Observer<String> observer = TestHelper.mockObserver();
        observable.subscribe(observer);

        try {
            f.t.join();
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }

        verify(observer).onSubscribe((Disposable)any());
        verify(observer, times(1)).onNext("one");
        verify(observer, Mockito.never()).onNext("two");
        verify(observer, Mockito.never()).onNext("three");
        verify(observer, times(1)).onNext("twoResume");
        verify(observer, times(1)).onNext("threeResume");
        verify(observer, Mockito.never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
        verifyNoMoreInteractions(observer);
    }

    @Test
    public void testResumeNextWithRuntimeException() {
        // Trigger failure on second element
        TestObservable f = new TestObservable("one", "RUNTIMEEXCEPTION", "two", "three");
        Observable<String> w = Observable.unsafeCreate(f);
        Observable<String> resume = Observable.just("twoResume", "threeResume");
        Observable<String> observable = w.onExceptionResumeNext(resume);

        Observer<String> observer = TestHelper.mockObserver();
        observable.subscribe(observer);

        try {
            f.t.join();
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }

        verify(observer).onSubscribe((Disposable)any());
        verify(observer, times(1)).onNext("one");
        verify(observer, Mockito.never()).onNext("two");
        verify(observer, Mockito.never()).onNext("three");
        verify(observer, times(1)).onNext("twoResume");
        verify(observer, times(1)).onNext("threeResume");
        verify(observer, Mockito.never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
        verifyNoMoreInteractions(observer);
    }

    @Test
    public void testThrowablePassesThru() {
        // Trigger failure on second element
        TestObservable f = new TestObservable("one", "THROWABLE", "two", "three");
        Observable<String> w = Observable.unsafeCreate(f);
        Observable<String> resume = Observable.just("twoResume", "threeResume");
        Observable<String> observable = w.onExceptionResumeNext(resume);

        Observer<String> observer = TestHelper.mockObserver();
        observable.subscribe(observer);

        try {
            f.t.join();
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }

        verify(observer).onSubscribe((Disposable)any());
        verify(observer, times(1)).onNext("one");
        verify(observer, never()).onNext("two");
        verify(observer, never()).onNext("three");
        verify(observer, never()).onNext("twoResume");
        verify(observer, never()).onNext("threeResume");
        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onComplete();
        verifyNoMoreInteractions(observer);
    }

    @Test
    public void testErrorPassesThru() {
        // Trigger failure on second element
        TestObservable f = new TestObservable("one", "ERROR", "two", "three");
        Observable<String> w = Observable.unsafeCreate(f);
        Observable<String> resume = Observable.just("twoResume", "threeResume");
        Observable<String> observable = w.onExceptionResumeNext(resume);

        Observer<String> observer = TestHelper.mockObserver();
        observable.subscribe(observer);

        try {
            f.t.join();
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }

        verify(observer).onSubscribe((Disposable)any());
        verify(observer, times(1)).onNext("one");
        verify(observer, never()).onNext("two");
        verify(observer, never()).onNext("three");
        verify(observer, never()).onNext("twoResume");
        verify(observer, never()).onNext("threeResume");
        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onComplete();
        verifyNoMoreInteractions(observer);
    }

    @Test
    public void testMapResumeAsyncNext() {
        // Trigger multiple failures
        Observable<String> w = Observable.just("one", "fail", "two", "three", "fail");
        // Resume Observable is async
        TestObservable f = new TestObservable("twoResume", "threeResume");
        Observable<String> resume = Observable.unsafeCreate(f);

        // Introduce map function that fails intermittently (Map does not prevent this when the Observer is a
        //  rx.operator incl onErrorResumeNextViaObservable)
        w = w.map(new Function<String, String>() {
            @Override
            public String apply(String s) {
                if ("fail".equals(s)) {
                    throw new RuntimeException("Forced Failure");
                }
                System.out.println("BadMapper:" + s);
                return s;
            }
        });

        Observable<String> observable = w.onExceptionResumeNext(resume);

        Observer<String> observer = TestHelper.mockObserver();
        observable.subscribe(observer);

        try {
            // if the thread gets started (which it shouldn't if it's working correctly)
            if (f.t != null) {
                f.t.join();
            }
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }

        verify(observer, times(1)).onNext("one");
        verify(observer, never()).onNext("two");
        verify(observer, never()).onNext("three");
        verify(observer, times(1)).onNext("twoResume");
        verify(observer, times(1)).onNext("threeResume");
        verify(observer, Mockito.never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }


    @Test
    public void testBackpressure() {
        TestObserver<Integer> to = new TestObserver<Integer>();
        Observable.range(0, 100000)
                .onExceptionResumeNext(Observable.just(1))
                .observeOn(Schedulers.computation())
                .map(new Function<Integer, Integer>() {
                    int c;

                    @Override
                    public Integer apply(Integer t1) {
                        if (c++ <= 1) {
                            // slow
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        return t1;
                    }

                })
                .subscribe(to);
        to.awaitTerminalEvent();
        to.assertNoErrors();
    }


    private static class TestObservable implements ObservableSource<String> {

        final String[] values;
        Thread t;

        TestObservable(String... values) {
            this.values = values;
        }

        @Override
        public void subscribe(final Observer<? super String> observer) {
            observer.onSubscribe(Disposables.empty());
            System.out.println("TestObservable subscribed to ...");
            t = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        System.out.println("running TestObservable thread");
                        for (String s : values) {
                            if ("EXCEPTION".equals(s)) {
                                throw new Exception("Forced Exception");
                            } else if ("RUNTIMEEXCEPTION".equals(s)) {
                                throw new RuntimeException("Forced RuntimeException");
                            } else if ("ERROR".equals(s)) {
                                throw new Error("Forced Error");
                            } else if ("THROWABLE".equals(s)) {
                                throw new Throwable("Forced Throwable");
                            }
                            System.out.println("TestObservable onNext: " + s);
                            observer.onNext(s);
                        }
                        System.out.println("TestObservable onComplete");
                        observer.onComplete();
                    } catch (Throwable e) {
                        System.out.println("TestObservable onError: " + e);
                        observer.onError(e);
                    }
                }

            });
            System.out.println("starting TestObservable thread");
            t.start();
            System.out.println("done starting TestObservable thread");
        }
    }
}
