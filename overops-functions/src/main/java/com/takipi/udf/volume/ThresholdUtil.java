package com.takipi.udf.volume;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.takipi.api.client.result.event.EventResult;

public class ThresholdUtil {
	public static void sortEventsByHitsDesc(List<EventResult> events) {

		Collections.sort(events, new Comparator<EventResult>() {
			@Override
			public int compare(EventResult o1, EventResult o2) {
				return (int) (getEventHits(o2) - getEventHits(o1));
			}
		});
	}

	public static long getEventHits(EventResult event) {

		if (event.stats == null) {
			return 0l;
		}

		return event.stats.hits;
	}

	public static long getEventsHits(Collection<EventResult> events) {

		long result = 0l;

		for (EventResult event : events) {
			result += getEventHits(event);
		}

		return result;
	}

	public static long getEventsInvocations(Collection<EventResult> events, long hitCount) {

		long invocations = 0;

		for (EventResult event : events) {

			if (event.stats == null) {
				continue;
			}

			System.out.println(event.id + ": " + event.summary + " - hits: " + event.stats.hits + " - inv: "
					+ event.stats.invocations);

			invocations += Math.max(event.stats.invocations, event.stats.hits);
		}

		long result = Math.max(invocations, hitCount);

		return result;
	}
}
