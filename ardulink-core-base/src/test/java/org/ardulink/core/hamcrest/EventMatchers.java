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

package org.ardulink.core.hamcrest;

import static java.util.Comparator.comparing;
import static java.util.Comparator.nullsFirst;

import java.util.Comparator;

import org.ardulink.core.events.PinValueChangedEvent;

/**
 * [ardulinktitle] [ardulinkversion]
 * 
 * project Ardulink http://www.ardulink.org/
 * 
 * [adsense]
 *
 */
public final class EventMatchers {
	
	private EventMatchers() {
		super();
	}

	public static Comparator<PinValueChangedEvent> comparator() {
		Comparator<PinValueChangedEvent> c1 = nullsFirst(comparing(p -> p.getPin().getType()));
		Comparator<PinValueChangedEvent> c2 = comparing(p -> p.getPin().pinNum());
		return c1.thenComparing(c2);
	}

}
