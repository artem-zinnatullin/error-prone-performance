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

package io.reactivex.internal.operators.flowable;

import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.Test;
import org.reactivestreams.Subscriber;

import io.reactivex.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.Function;
import io.reactivex.processors.PublishProcessor;

public class FlowableHideTest {
    @Test
    public void testHiding() {
        PublishProcessor<Integer> src = PublishProcessor.create();

        Flowable<Integer> dst = src.hide();

        assertFalse(dst instanceof PublishProcessor);

        Subscriber<Object> o = TestHelper.mockSubscriber();

        dst.subscribe(o);

        src.onNext(1);
        src.onComplete();

        verify(o).onNext(1);
        verify(o).onComplete();
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void testHidingError() {
        PublishProcessor<Integer> src = PublishProcessor.create();

        Flowable<Integer> dst = src.hide();

        assertFalse(dst instanceof PublishProcessor);

        Subscriber<Object> o = TestHelper.mockSubscriber();

        dst.subscribe(o);

        src.onError(new TestException());

        verify(o, never()).onNext(any());
        verify(o, never()).onComplete();
        verify(o).onError(any(TestException.class));
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeFlowable(new Function<Flowable<Object>, Flowable<Object>>() {
            @Override
            public Flowable<Object> apply(Flowable<Object> o)
                    throws Exception {
                return o.hide();
            }
        });
    }

    @Test
    public void disposed() {
        TestHelper.checkDisposed(PublishProcessor.create().hide());
    }
}
