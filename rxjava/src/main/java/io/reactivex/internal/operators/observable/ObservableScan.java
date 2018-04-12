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

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.BiFunction;
import io.reactivex.internal.disposables.DisposableHelper;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.plugins.RxJavaPlugins;

public final class ObservableScan<T> extends AbstractObservableWithUpstream<T, T> {
    final BiFunction<T, T, T> accumulator;
    public ObservableScan(ObservableSource<T> source, BiFunction<T, T, T> accumulator) {
        super(source);
        this.accumulator = accumulator;
    }

    @Override
    public void subscribeActual(Observer<? super T> t) {
        source.subscribe(new ScanObserver<T>(t, accumulator));
    }

    static final class ScanObserver<T> implements Observer<T>, Disposable {
        final Observer<? super T> actual;
        final BiFunction<T, T, T> accumulator;

        Disposable s;

        T value;

        boolean done;

        ScanObserver(Observer<? super T> actual, BiFunction<T, T, T> accumulator) {
            this.actual = actual;
            this.accumulator = accumulator;
        }

        @Override
        public void onSubscribe(Disposable s) {
            if (DisposableHelper.validate(this.s, s)) {
                this.s = s;
                actual.onSubscribe(this);
            }
        }


        @Override
        public void dispose() {
            s.dispose();
        }

        @Override
        public boolean isDisposed() {
            return s.isDisposed();
        }


        @Override
        public void onNext(T t) {
            if (done) {
                return;
            }
            final Observer<? super T> a = actual;
            T v = value;
            if (v == null) {
                value = t;
                a.onNext(t);
            } else {
                T u;

                try {
                    u = ObjectHelper.requireNonNull(accumulator.apply(v, t), "The value returned by the accumulator is null");
                } catch (Throwable e) {
                    Exceptions.throwIfFatal(e);
                    s.dispose();
                    onError(e);
                    return;
                }

                value = u;
                a.onNext(u);
            }
        }

        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
                return;
            }
            done = true;
            actual.onError(t);
        }

        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;
            actual.onComplete();
        }
    }
}
