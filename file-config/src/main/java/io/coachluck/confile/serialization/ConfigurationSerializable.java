/*
 *   Project: Confile
 *   File: ConfigurationSerializable.java
 *   Last Modified: 1/17/21, 5:37 PM
 *
 *    Copyright 2021 AJ Romaniello
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

package io.coachluck.confile.serialization;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

public interface ConfigurationSerializable {
    @NotNull
    Map<String, Object> serialize();
}
