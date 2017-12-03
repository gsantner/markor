/*
 * ------------------------------------------------------------------------------
 * Gregor Santner <gsantner.net> wrote this. You can do whatever you want
 * with it. If we meet some day, and you think it is worth it, you can buy me a
 * coke in return. Provided as is without any kind of warranty. Do not blame or
 * sue me if something goes wrong. No attribution required.    - Gregor Santner
 *
 * License: Creative Commons Zero (CC0 1.0)
 *  http://creativecommons.org/publicdomain/zero/1.0/
 * ----------------------------------------------------------------------------
 */
package net.gsantner.opoc.util;

public class Callback {
    public static interface a1<A> {
        void callback(A arg1);
    }

    public static interface a2<A, B> {
        void callback(A arg1, B arg2);
    }

    public static interface a3<A, B, C> {
        void callback(A arg1, B arg2, C arg3);
    }

    public static interface a4<A, B, C, D> {
        void callback(A arg1, B arg2, C arg3, D arg4);
    }

    public static interface a5<A, B, C, D, E> {
        void callback(A arg1, B arg2, C arg3, D arg4, E arg5);
    }
}
