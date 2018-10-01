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
}
