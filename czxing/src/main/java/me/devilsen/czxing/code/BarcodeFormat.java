/*
 * Copyright 2007 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.devilsen.czxing.code;

import java.util.HashMap;

/**
 * Enumerates barcode formats known to this package.
 * Note that this should be keep synchronized with native (C++) side.
 */
public enum BarcodeFormat {

    /** Aztec 2D barcode format. */
    AZTEC(1),

    /** CODABAR 1D format. */
    CODABAR(1 << 1),

    /** Code 39 1D format. */
    CODE_39(1 << 2),

    /** Code 93 1D format. */
    CODE_93(1 << 3),

    /** Code 128 1D format. */
    CODE_128(1 << 4),

    /** Code DataBar 1D format. */
    DATA_BAR(1 << 5),

    /** RSS 14 */
    RSS_14(1 << 5),

    /** Code DataBarExpanded 1D format. */
    DATA_BAR_EXPANDED(1 << 6),

    /** RSS EXPANDED */
    RSS_EXPANDED(1 << 6),

    /** Data Matrix 2D barcode format. */
    DATA_MATRIX(1 << 7),

    /** EAN-8 1D format. */
    EAN_8(1 << 8),

    /** EAN-13 1D format. */
    EAN_13(1 << 9),

    /** ITF (Interleaved Two of Five) 1D format. */
    ITF(1 << 10),

    /** MaxiCode 2D barcode format. */
    MAXICODE(1 << 11),

    /** PDF417 format. */
    PDF_417(1 << 12),

    /** QR Code 2D barcode format. */
    QR_CODE(1 << 13),

    /** UPC-A 1D format. */
    UPC_A(1 << 14),

    /** UPC-E 1D format. */
    UPC_E(1 << 15);

    private static final HashMap<Integer, BarcodeFormat> FORMAT_CACHE = new HashMap<>(32);

    static {
        for (BarcodeFormat format : values()) {
            FORMAT_CACHE.put(format.value, format);
        }
    }

    public static BarcodeFormat valueOf(int value) {
        return FORMAT_CACHE.get(value);
    }

    private final int value;

    BarcodeFormat(int value) {
        this.value = value;
    }

    int getValue() {
        return value;
    }

}
