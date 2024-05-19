/*#######################################################
 *
 * SPDX-FileCopyrightText: 2018-2024 Gregor Santner <gsantner AT mailbox DOT org>
 * SPDX-License-Identifier: Unlicense OR CC0-1.0
 *
 * Written 2018-2024 by Gregor Santner <gsantner AT mailbox DOT org>
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
#########################################################*/
package net.gsantner.opoc.wrapper;

@SuppressWarnings("unused")
public class GsCallback {

    public interface a0 {
        void callback();
    }

    public interface a1<A> {
        void callback(A arg1);
    }

    public interface a2<A, B> {
        void callback(A arg1, B arg2);
    }

    public interface a3<A, B, C> {
        void callback(A arg1, B arg2, C arg3);
    }

    public interface a4<A, B, C, D> {
        void callback(A arg1, B arg2, C arg3, D arg4);
    }

    public interface a5<A, B, C, D, E> {
        void callback(A arg1, B arg2, C arg3, D arg4, E arg5);
    }

    public interface b0 {
        boolean callback();
    }

    public interface b1<A> {
        boolean callback(A arg1);
    }

    public interface b2<A, B> {
        boolean callback(A arg1, B arg2);
    }

    public interface b3<A, B, C> {
        boolean callback(A arg1, B arg2, C arg3);
    }

    public interface b4<A, B, C, D> {
        boolean callback(A arg1, B arg2, C arg3, D arg4);
    }

    public interface b5<A, B, C, D, E> {
        boolean callback(A arg1, B arg2, C arg3, D arg4, E arg5);
    }

    public interface s0 {
        String callback();
    }

    public interface s1<A> {
        String callback(A arg1);
    }

    public interface s2<A, B> {
        String callback(A arg1, B arg2);
    }

    public interface s3<A, B, C> {
        String callback(A arg1, B arg2, C arg3);
    }

    public interface s4<A, B, C, D> {
        String callback(A arg1, B arg2, C arg3, D arg4);
    }

    public interface s5<A, B, C, D, E> {
        String callback(A arg1, B arg2, C arg3, D arg4, E arg5);
    }

    public interface r0<R> {
        R callback();
    }

    public interface r1<R, A> {
        R callback(A arg1);
    }

    public interface r2<R, A, B> {
        R callback(A arg1, B arg2);
    }

    public interface r3<R, A, B, C> {
        R callback(A arg1, B arg2, C arg3);
    }

    public interface r4<R, A, B, C, D> {
        R callback(A arg1, B arg2, C arg3, D arg4);
    }

    public interface r5<R, A, B, C, D, E> {
        R callback(A arg1, B arg2, C arg3, D arg4, E arg5);
    }
}
