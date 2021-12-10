/*
 * MadKitLanEdition (created by Jason MAHDJOUB (jason.mahdjoub@distri-mind.fr)) Copyright (c)
 * 2015 is a fork of MadKit and MadKitGroupExtension. 
 * 
 * Copyright or © or Copr. Jason Mahdjoub, Fabien Michel, Olivier Gutknecht, Jacques Ferber (1997)
 * 
 * jason.mahdjoub@distri-mind.fr
 * fmichel@lirmm.fr
 * olg@no-distance.net
 * ferber@lirmm.fr
 * 
 * This software is a computer program whose purpose is to
 * provide a lightweight Java library for designing and simulating Multi-Agent Systems (MAS).
 * This software is governed by the CeCILL-C license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL-C
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability.
 * 
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and,  more generally, to use and operate it in the
 * same conditions as regards security.
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-C license and that you accept its terms.
 */
package com.distrimind.madkit.kernel.network;

import java.util.Arrays;


/**
 * This class represents an average of bytes transferred during the last elapsed
 * duration. The time elapsed is the real time. So the statistic change with the
 * time. If no transfer has been done during the last elapsed duration, the
 * number of transferred bytes is equal to 0.
 * 
 * This class is thread safe.
 * 
 * @author Jason Mahdjoub
 * @version 1.0
 * @since MadkitLanEdition 1.0
 */
public class RealTimeTransferStat {
	private final long[] stats;
	private long totalBytes;
	private long totalBytesFromTheBeginning;
	private int cursor;
	private long previousUpdateTime;
	private final long segment;
	private final long duration;
	private volatile boolean one_cycle_done ;
	private final long timeUTC;

	/**
	 * Construct a metric that computes number of transferred bytes during the last
	 * elapsed duration, specified with the next arguments.
	 * 
	 * @param _duration
	 *            the duration in milliseconds. Must be greater or equal than 3.
	 * @param _segment
	 *            the duration segment. Must be lower than _duration. A lower
	 *            segment size gives statistics that are more precise, but takes
	 *            more memory place (array of
	 *            <code>(_duration-_duration%_segment)/_segment</code>).
	 */
	public RealTimeTransferStat(long _duration, long _segment) {
		if (_duration < _segment)
			throw new IllegalArgumentException("_duration must be greater or equal than _segment");
		if (_duration < 3)
			throw new IllegalArgumentException("_duration must be greater than 3 ms");

		_duration = _duration - _duration % _segment;
		cursor = 0;
		segment = _segment;
		duration = _duration;
		if (duration / segment > Integer.MAX_VALUE)
			throw new IllegalArgumentException("The value '_duration/_segment' must be lower than Integer.MAX_VALUE");
		if (duration / segment < 3)
			throw new IllegalArgumentException("The value '_duration/_segment' must be greater than 3");
		stats = new long[(int) (duration / segment)];
		reset();
		one_cycle_done = false;
		previousUpdateTime=System.currentTimeMillis();
		timeUTC=System.currentTimeMillis();
	}

	/**
	 * The duration in milliseconds used to computed the average of transferred bytes.
	 * 
	 * @return the duration in milliseconds used to computed the average of transferred bytes.
	 */
	public long getDurationMilli() {
		return duration;
	}

	private void reset() {
        Arrays.fill(stats,0);
		totalBytes = 0;
	}

	private void update() {
		long time=System.currentTimeMillis();
		long t = time-previousUpdateTime;

		if (t > duration) {
			reset();
			one_cycle_done=true;
			previousUpdateTime=time;
		} else {
            while (t > segment) {
                if (++cursor == stats.length) {
                    one_cycle_done=true;
                    cursor = 0;
                }
                totalBytes -= stats[cursor];
                stats[cursor] = 0;
                t -= segment;
                previousUpdateTime+=segment;
            }
        }

	}

	/**
	 * Inform that new bytes have been transferred
	 * 
	 * @param number
	 *            the number of transferred bytes
	 */
	public void newBytesIdentified(int number) {
		synchronized (this) {
			update();
			stats[cursor] += number;
			totalBytes += number;
			totalBytesFromTheBeginning+=number;
		}
	}

	/**
	 * 
	 * @return the average of bytes transferred during the given duration
	 */
	public long getNumberOfIdentifiedBytesDuringTheLastCycle() {
		synchronized (this) {
			update();
			return totalBytes;
		}
	}

	public long getNumberOfIdentifiedBytesFromCreationOfTheseStatistics()
	{
		synchronized (this)
		{
			return totalBytesFromTheBeginning;
		}
	}
	public long getDurationInMsFromCreationTimeOfTheseStatistics()
	{
		return System.currentTimeMillis()-timeUTC;
	}

	/**
	 * 
	 * @return true if sufficient bytes has been observed to give a correct metrics.
	 */
	public boolean isOneCycleDone() {
		return one_cycle_done;
	}

	public long getSegment() {
		return segment;
	}
}
