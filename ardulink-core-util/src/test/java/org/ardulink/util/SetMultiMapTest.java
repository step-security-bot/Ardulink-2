/**
Copyright 2013 project Ardulink http://www.ardulink.org/
 
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
 
    http://www.apache.org/licenses/LICENSE-2.0
 
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package org.ardulink.util;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * [ardulinktitle] [ardulinkversion]
 * 
 * project Ardulink http://www.ardulink.org/
 * 
 * [adsense]
 *
 */
class SetMultiMapTest {

	@Test
	void canPut() {
		SetMultiMap<Integer, String> s = new SetMultiMap<>();
		assertThat(s.put(1, "foo")).isEqualTo(TRUE);
		assertThat(s.asMap()).isEqualTo(buildMap(1, Collections.singleton("foo")));
	}

	@Test
	void canPutTwice() {
		SetMultiMap<Integer, String> s = new SetMultiMap<>();
		assertThat(s.put(1, "foo")).isEqualTo(TRUE);
		assertThat(s.put(1, "foo")).isEqualTo(FALSE);
		assertThat(s.asMap()).isEqualTo(buildMap(1, Collections.singleton("foo")));
	}

	@Test
	void canRemoveExistingValue() {
		SetMultiMap<Integer, String> s = new SetMultiMap<>();
		assertThat(s.put(1, "foo")).isEqualTo(TRUE);
		assertThat(s.remove(1, "foo")).isEqualTo(TRUE);
		assertThat(s.asMap()).isEqualTo(Collections.<Integer, Set<String>>emptyMap());
	}

	@Test
	void canHandleRemovesOfNonExistingValues() {
		SetMultiMap<Integer, String> s = new SetMultiMap<>();
		assertThat(s.put(1, "foo")).isEqualTo(TRUE);
		assertThat(s.remove(1, "bar")).isEqualTo(FALSE);
		assertThat(s.asMap()).isEqualTo(buildMap(1, Collections.singleton("foo")));
	}

	@Test
	void canHandleRemovesOfNonExistingKeys() {
		SetMultiMap<Integer, String> s = new SetMultiMap<>();
		assertThat(s.put(1, "foo")).isEqualTo(TRUE);
		assertThat(s.remove(2, "foo")).isEqualTo(FALSE);
		assertThat(s.asMap()).isEqualTo(buildMap(1, Collections.singleton("foo")));
	}

	private static Map<Integer, Set<String>> buildMap(Integer key, Set<String> value) {
		Map<Integer, Set<String>> m = new HashMap<>();
		m.put(key, value);
		return m;
	}

}
