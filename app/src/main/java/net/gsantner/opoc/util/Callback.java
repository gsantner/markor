/*
 * ------------------------------------------------------------------------------
 * Lonami Exo <lonamiwebs.github.io> wrote this. You can do whatever you want
 * with it. If we meet some day, and you think it is worth it, you can buy me
 * a coke in return. Provided as is without any kind of warranty. Do not blame
 * or sue me if something goes wrong. No attribution required.
 *                                                             - Lonami Exo
 *
 * License: Creative Commons Zero (CC0 1.0)
 *  http://creativecommons.org/publicdomain/zero/1.0/
 * ----------------------------------------------------------------------------
 */
package net.gsantner.opoc.util;

// Simple callback interface which includes an object
public interface Callback<T> {
    void onCallback(T t);
}
