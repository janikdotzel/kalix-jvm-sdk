/*
 * Copyright 2021 Lightbend Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kalix.javasdk;

import kalix.javasdk.impl.effect.SideEffectImpl;

/** A side effect. */
public interface SideEffect {

  /** The service call that is executed as this effect. */
  DeferredCall<?, ?> call();

  /** Whether this effect should be executed synchronously or not. */
  boolean synchronous();

  /**
   * Create a side effect of the given service call.
   *
   * @param serviceCall The service call to effect.
   * @param synchronous Whether this effect should be executed synchronously.
   * @return The side effect.
   */
  static SideEffect of(DeferredCall serviceCall, boolean synchronous) {
    return new SideEffectImpl(serviceCall, synchronous);
  }

  /**
   * Create a side effect of the given service call.
   *
   * @param serviceCall The service call to effect.
   * @return The side effect.
   */
  static SideEffect of(DeferredCall serviceCall) {
    return new SideEffectImpl(serviceCall, false);
  }
}
