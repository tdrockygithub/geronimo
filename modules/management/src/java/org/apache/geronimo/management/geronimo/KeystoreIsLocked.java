/**
 *
 * Copyright 2005 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.geronimo.management.geronimo;

/**
 * Exception indicating that the keystore you tried to do something with is
 * locked.  It must be unlocked before it can be used in this way.
 *
 * @version $Rev$ $Date$
 */
public class KeystoreIsLocked extends Exception {
    public KeystoreIsLocked(String message) {
        super(message);
    }

    public KeystoreIsLocked(String message, Throwable cause) {
        super(message, cause);
    }
}
