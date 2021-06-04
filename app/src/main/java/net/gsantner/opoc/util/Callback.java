/*#######################################################
 *
 *   Maintained by Gregor Santner, 2018-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *     https://github.com/gsantner/opoc/#licensing
 *
#########################################################*/
package net.gsantner.opoc.util;

@SuppressWarnings("unused")
public class Callback {

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
}
