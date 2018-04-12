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

package io.reactivex.internal.operators.completable;

import static org.junit.Assert.*;
import org.junit.Test;

import io.reactivex.*;
import io.reactivex.functions.Function;

public class CompletableOnErrorXTest {

    @Test
    public void normalReturn() {
        Completable.complete()
        .onErrorComplete()
        .test()
        .assertResult();
    }

    @Test
    public void normalResumeNext() {
        final int[] call = { 0 };
        Completable.complete()
        .onErrorResumeNext(new Function<Throwable, CompletableSource>() {
            @Override
            public CompletableSource apply(Throwable e) throws Exception {
                call[0]++;
                return Completable.complete();
            }
        })
        .test()
        .assertResult();

        assertEquals(0, call[0]);
    }
}
