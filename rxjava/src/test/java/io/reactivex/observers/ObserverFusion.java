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

package io.reactivex.observers;

import io.reactivex.Observable;
import io.reactivex.functions.*;
import io.reactivex.internal.fuseable.*;

/**
 * Utility methods that return functional interfaces to support assertions regarding fusion
 * in a TestObserver.
 * <p>Don't move this class as it needs package-private access to TestObserver's internals.
 */
public enum ObserverFusion {
    ;

    /**
     * Returns a function that takes a Flowable and returns a TestObserver that
     * is set up according to the parameters and is subscribed to the Flowable.
     * <p>
     * Use this as follows:
     * <pre>
     * source
     * .to(ObserverFusion.test(QueueFuseable.ANY, false))
     * .assertResult(0);
     * </pre>
     * @param <T> the value type
     * @param mode the fusion mode to request, see {@link QueueDisposable} constants.
     * @param cancelled should the TestObserver cancelled before the subscription even happens?
     * @return the new Function instance
     */
    public static <T> Function<Observable<T>, TestObserver<T>> test(
            final int mode, final boolean cancelled) {
        return new TestFunctionCallback<T>(mode, cancelled);
    }

    /**
     * Returns a Consumer that asserts on its TestObserver parameter that
     * the upstream is Fuseable (sent a QueueDisposable subclass in onSubscribe).
     * <p>
     * Use this as follows:
     * <pre>
     * source
     * .to(ObserverFusion.test(0, QueueFuseable.ANY, false))
     * .assertOf(ObserverFusion.assertFuseable());
     * </pre>
     * @param <T> the value type
     * @return the new Consumer instance
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T> Consumer<TestObserver<T>> assertFuseable() {
        return (Consumer)AssertFuseable.INSTANCE;
    }

    static final class AssertFusionConsumer<T> implements Consumer<TestObserver<T>> {
        private final int mode;

        AssertFusionConsumer(int mode) {
            this.mode = mode;
        }

        @Override
        public void accept(TestObserver<T> to) throws Exception {
            to.assertFusionMode(mode);
        }
    }

    static final class TestFunctionCallback<T> implements Function<Observable<T>, TestObserver<T>> {
        private final int mode;
        private final boolean cancelled;

        TestFunctionCallback(int mode, boolean cancelled) {
            this.mode = mode;
            this.cancelled = cancelled;
        }

        @Override
        public TestObserver<T> apply(Observable<T> t) throws Exception {
            TestObserver<T> to = new TestObserver<T>();
            to.setInitialFusionMode(mode);
            if (cancelled) {
                to.cancel();
            }
            t.subscribe(to);
            return to;
        }
    }

    enum AssertFuseable implements Consumer<TestObserver<Object>> {
        INSTANCE;
        @Override
        public void accept(TestObserver<Object> to) throws Exception {
            to.assertFuseable();
        }
    }

    /**
     * Returns a Consumer that asserts on its TestObserver parameter that
     * the upstream is not Fuseable (didn't sent a QueueDisposable subclass in onSubscribe).
     * <p>
     * Use this as follows:
     * <pre>
     * source
     * .to(ObserverFusion.test(0, QueueFuseable.ANY, false))
     * .assertOf(ObserverFusion.assertNotFuseable());
     * </pre>
     * @param <T> the value type
     * @return the new Consumer instance
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T> Consumer<TestObserver<T>> assertNotFuseable() {
        return (Consumer)AssertNotFuseable.INSTANCE;
    }

    enum AssertNotFuseable implements Consumer<TestObserver<Object>> {
        INSTANCE;
        @Override
        public void accept(TestObserver<Object> to) throws Exception {
            to.assertNotFuseable();
        }
    }

    /**
     * Returns a Consumer that asserts on its TestObserver parameter that
     * the upstream is Fuseable (sent a QueueDisposable subclass in onSubscribe)
     * and the established the given fusion mode.
     * <p>
     * Use this as follows:
     * <pre>
     * source
     * .to(ObserverFusion.test(0, QueueFuseable.ANY, false))
     * .assertOf(ObserverFusion.assertFusionMode(QueueFuseable.SYNC));
     * </pre>
     * @param <T> the value type
     * @param mode the expected established fusion mode, see {@link QueueFuseable} constants.
     * @return the new Consumer instance
     */
    public static <T> Consumer<TestObserver<T>> assertFusionMode(final int mode) {
        return new AssertFusionConsumer<T>(mode);
    }


    /**
     * Constructs a TestObserver with the given required fusion mode.
     * @param <T> the value type
     * @param mode the requested fusion mode, see {@link QueueFuseable} constants
     * @return the new TestSubscriber
     */
    public static <T> TestObserver<T> newTest(int mode) {
        TestObserver<T> to = new TestObserver<T>();
        to.setInitialFusionMode(mode);
        return to;
    }

    /**
     * Assert that the TestSubscriber received a fuseabe QueueFuseable.and
     * is in the given fusion mode.
     * @param <T> the value type
     * @param to the TestSubscriber instance
     * @param mode the expected mode
     * @return the TestSubscriber
     */
    public static <T> TestObserver<T> assertFusion(TestObserver<T> to, int mode) {
        return to.assertOf(ObserverFusion.<T>assertFuseable())
                .assertOf(ObserverFusion.<T>assertFusionMode(mode));
    }
}
