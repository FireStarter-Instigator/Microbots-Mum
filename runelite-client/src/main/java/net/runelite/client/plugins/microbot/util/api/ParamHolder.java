/*
 * Copyright (c) 2020 Abex
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.microbot.util.api;

/**
 * A composition that can hold `param` keys. This lets Jagex attach arbitrary constant
 * data to certain items, objects, npcs, or structs for use in cs2
 *
 * @see net.runelite.api.ParamID
 */
public interface ParamHolder
{
    // FIXED: Use Microbot Node to satisfy IterableHashTable<T extends Node> bounds
    IterableHashTable<net.runelite.client.plugins.microbot.util.api.Node> getParams();

    // FIXED: Use Microbot Node here as well for consistency
    void setParams(IterableHashTable<net.runelite.client.plugins.microbot.util.api.Node> params);

    /**
     * Gets the value of a given {@link net.runelite.api.ParamID}, or its default if it is unset
     */
    int getIntValue(int paramID);

    /**
     * Sets the value of a given {@link net.runelite.api.ParamID}
     */
    void setValue(int paramID, int value);

    /**
     * Gets the value of a given {@link net.runelite.api.ParamID}, or its default if it is unset
     */
    String getStringValue(int paramID);

    /**
     * Sets the value of a given {@link ParamID}
     */
    void setValue(int paramID, String value);
}