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

import java.util.Collection;
import java.util.Optional;

/** The principals associated with a request. */
public interface Principals {
  /** Whether this request was from the internet. */
  boolean isInternet();

  /** Whether this is a self request. */
  boolean isSelf();

  /** Whether this request is a backoffice request. */
  boolean isBackoffice();

  /**
   * Whether this request was from a service in the local project.
   *
   * @param name The name of the service.
   */
  boolean isLocalService(String name);
  /** Whether this request was from any service in the local project. */
  boolean isAnyLocalService();
  /** Get the service that invoked this call, if any. */
  Optional<String> getLocalService();

  /** Get the principals associated with this request. */
  Collection<Principal> get();
}
