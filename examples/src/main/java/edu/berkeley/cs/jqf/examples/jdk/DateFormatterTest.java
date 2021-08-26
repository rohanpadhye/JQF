/*
 * Copyright (c) 2017-2018 The Regents of the University of California
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package edu.berkeley.cs.jqf.examples.jdk;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;

import com.pholser.junit.quickcheck.generator.InRange;
import edu.berkeley.cs.jqf.fuzz.Fuzz;
import edu.berkeley.cs.jqf.fuzz.JQF;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.runner.RunWith;

/**
 * @author Rohan Padhye
 */
@RunWith(JQF.class)
public class DateFormatterTest {

    @Fuzz
    public void fuzzSimple(Date date, String format) throws IllegalArgumentException {
        // Create a formatter using the input format
        DateFormat df = new SimpleDateFormat(format);
        // Serialize a Date object to a String
        try {
            df.format(date);
        } catch (ArrayIndexOutOfBoundsException e) {
            Assume.assumeNoException(e);
        }
    }

    @Fuzz
    public void fuzzLocalDateTime(String date, String pattern) throws IllegalArgumentException, DateTimeParseException {
        LocalDateTime.parse(date, DateTimeFormatter.ofPattern(pattern));
    }

    @Fuzz
    public void testLocalDateTimeSerialization(@InRange(minInt=0) int year,
                                               @InRange(minInt=1, maxInt=12) int month,
                                               @InRange(minInt=1, maxInt=31) int dayOfMonth,
                                               @InRange(minInt=0, maxInt=23) int hour,
                                               @InRange(minInt=0, maxInt=59) int minute,
                                               @InRange(minInt=0, maxInt=59) int second) {
        LocalDateTime ldt1 = null, ldt2;
        String s1, s2;
        try {
            ldt1 = LocalDateTime.of(year, month, dayOfMonth, hour, minute, second);
        } catch (DateTimeException e) {
            Assume.assumeNoException(e);
        }

        s1 = ldt1.format(DateTimeFormatter.ISO_DATE_TIME);
        ldt2 = LocalDateTime.parse(s1);
        s2 = ldt2.format(DateTimeFormatter.ISO_DATE_TIME);

        Assert.assertEquals(s1, s2);

    }

    @Fuzz
    public void testLocalDateTimeSerialization2(String s) {
        LocalDateTime ldt1 = null, ldt2;
        String s1, s2;
        try {
            ldt1 = LocalDateTime.parse(s);
        } catch (DateTimeException e) {
            return;
        }

        s1 = ldt1.format(DateTimeFormatter.ISO_DATE_TIME);
        ldt2 = LocalDateTime.parse(s1);
        s2 = ldt2.format(DateTimeFormatter.ISO_DATE_TIME);

        Assert.assertEquals(s1, s2);

    }
}
